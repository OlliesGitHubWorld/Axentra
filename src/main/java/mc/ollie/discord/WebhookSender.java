package mc.ollie.discord;

import mc.ollie.App;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Posts to Discord through webhooks (channels.&lt;key&gt;.webhook) for channels without buttons or
 * replies: join-leave, alerts, chat, console, staffchat and weekly-report. This is plain HTTP, so it
 * works without the bot, keeps posting while the bot reconnects, and doesn't share the bot's rate
 * limits. Every webhook has its own queue that sends in order and waits when Discord asks it to.
 */
public class WebhookSender {

    /** Channels that can post through a webhook. The rest need the bot for buttons, edits or threads. */
    public static final List<String> CHANNEL_KEYS = List.of(
            "join-leave", "alerts", "chat", "console", "staffchat", "weekly-report");

    private static final Pattern URL = Pattern.compile(
            "https://(?:ptb\\.|canary\\.)?discord(?:app)?\\.com/api/(?:v\\d+/)?webhooks/(\\d+)/([\\w-]+)");
    /** Discord refuses webhook usernames containing these words. */
    private static final Pattern BLOCKED_NAME = Pattern.compile("(?i)discord|clyde");
    /** Messages waiting per webhook; more are dropped so a slow Discord can't fill up memory. */
    private static final int MAX_PENDING = 100;

    private final App plugin;
    private final DiscordManager discord;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, ExecutorService> queues = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> pending = new ConcurrentHashMap<>();
    /** Webhooks Discord says no longer exist (deleted or reset), skipped until the next reload. */
    private final Set<String> dead = ConcurrentHashMap.newKeySet();
    private volatile boolean shutdown;

    public WebhookSender(App plugin, DiscordManager discord) {
        this.plugin = plugin;
        this.discord = discord;
    }

    /** A webhook message. Without a username/avatar the name and picture of the webhook itself are used. */
    public record Message(String content, String username, String avatarUrl, List<MessageEmbed> embeds) {
        public static Message text(String content) {
            return new Message(content, null, null, List.of());
        }

        public static Message embed(MessageEmbed embed) {
            return new Message(null, null, null, List.of(embed));
        }

        /** Posts as a player: their name and head instead of the webhook's own. */
        public static Message as(String username, String avatarUrl, String content) {
            return new Message(content, username, avatarUrl, List.of());
        }
    }

    // ── Config ──────────────────────────────────────────────

    private String configured(String key) {
        if (!CHANNEL_KEYS.contains(key)) return "";
        return discord.config().getString("channels." + key + ".webhook", "").trim();
    }

    /** The webhook URL without query or trailing slash, or null when it isn't a Discord webhook URL. */
    private static String normalize(String raw) {
        int query = raw.indexOf('?');
        if (query >= 0) raw = raw.substring(0, query);
        while (raw.endsWith("/")) raw = raw.substring(0, raw.length() - 1);
        return URL.matcher(raw).matches() ? raw : null;
    }

    /** The webhook to post to for a channel, or null when it's off, not set, invalid or deleted. */
    public String url(String key) {
        if (!discord.config().getBoolean("channels." + key + ".enabled", false)) return null;
        String raw = configured(key);
        if (raw.isEmpty()) return null;
        String url = normalize(raw);
        return url != null && !dead.contains(url) ? url : null;
    }

    /** Why a configured webhook isn't used, or null when it's fine or not set. */
    public String problem(String key) {
        String raw = configured(key);
        if (raw.isEmpty()) return null;
        String url = normalize(raw);
        if (url == null) return "invalid webhook URL";
        if (dead.contains(url)) return "webhook was deleted in Discord";
        return null;
    }

    /** Called on start and /axentra reload: retries deleted webhooks and warns about invalid URLs. */
    public void reload() {
        dead.clear();
        for (String key : CHANNEL_KEYS) {
            if (!discord.config().getBoolean("channels." + key + ".enabled", false)) continue;
            String problem = problem(key);
            if (problem != null) {
                plugin.getLogger().warning("Discord channel '" + key + "': " + problem
                        + ". Copy it from Channel settings > Integrations > Webhooks.");
            }
        }
    }

    /** Whether a log line contains the secret part of a configured webhook URL. */
    public boolean revealsWebhook(String line) {
        for (String key : CHANNEL_KEYS) {
            String url = normalize(configured(key));
            if (url == null) continue;
            Matcher m = URL.matcher(url);
            if (m.matches() && line.contains(m.group(2))) return true;
        }
        return false;
    }

    /** Messages still waiting to be sent for a channel, so batch senders can back off. */
    public int pending(String key) {
        String url = url(key);
        AtomicInteger count = url != null ? pending.get(url) : null;
        return count != null ? count.get() : 0;
    }

    // ── Sending ─────────────────────────────────────────────

    /** Queues a message. Completes with true when Discord accepted it. Never blocks. */
    public CompletableFuture<Boolean> send(String key, Message message) {
        String url = url(key);
        if (url == null || shutdown) return CompletableFuture.completedFuture(false);
        AtomicInteger count = pending.computeIfAbsent(url, u -> new AtomicInteger());
        if (count.incrementAndGet() > MAX_PENDING) {
            count.decrementAndGet();
            return CompletableFuture.completedFuture(false);
        }
        String body = toJson(message);
        ExecutorService queue = queues.computeIfAbsent(url,
                u -> Executors.newSingleThreadExecutor(Thread.ofVirtual().name("Axentra-Webhook").factory()));
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return post(key, url, body);
                } finally {
                    count.decrementAndGet();
                }
            }, queue);
        } catch (RejectedExecutionException e) {
            count.decrementAndGet();
            return CompletableFuture.completedFuture(false);
        }
    }

    private static String toJson(Message message) {
        DataObject json = DataObject.empty();
        String content = message.content();
        String username = message.username();
        if (username != null && BLOCKED_NAME.matcher(username).find()) {
            // Discord refuses the whole message for such a name, so show it in the text instead
            if (content != null) content = "**" + EmbedFactory.escape(username) + "**: " + content;
            username = null;
        }
        if (content != null) json.put("content", EmbedFactory.truncate(content, 2000));
        if (username != null) json.put("username", EmbedFactory.truncate(username, 80));
        if (message.avatarUrl() != null && username != null) json.put("avatar_url", message.avatarUrl());
        if (!message.embeds().isEmpty()) {
            json.put("embeds", DataArray.fromCollection(message.embeds().stream().map(MessageEmbed::toData).toList()));
        }
        // Text from Minecraft must never ping anyone
        json.put("allowed_mentions", DataObject.empty().put("parse", DataArray.empty()));
        return json.toString();
    }

    /** Runs on the webhook's own queue. Retries on rate limits and Discord outages, at most 4 tries. */
    private boolean post(String key, String url, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Axentra/" + plugin.getDescription().getVersion())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        for (int attempt = 0; attempt < 4; attempt++) {
            HttpResponse<String> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (IOException e) {
                plugin.logInfo("Could not reach the Discord webhook for '" + key + "': " + e.getMessage());
                return false;
            }

            int status = response.statusCode();
            if (status == 429) {
                sleep(retryAfter(response));
                continue;
            }
            if (status == 401 || status == 404) {
                if (dead.add(url)) {
                    plugin.getLogger().warning("The webhook for Discord channel '" + key + "' no longer exists. "
                            + "Set a new URL in discord.yml and use /axentra reload.");
                }
                return false;
            }
            if (status >= 500) {
                sleep(1000L * (attempt + 1));
                continue;
            }
            // Last request in this rate limit window: wait so the next message isn't refused
            if ("0".equals(response.headers().firstValue("X-RateLimit-Remaining").orElse(null))) {
                sleep(seconds(response.headers().firstValue("X-RateLimit-Reset-After").orElse(null), 1));
            }
            if (status >= 400) {
                plugin.logInfo("Discord refused a webhook message for '" + key + "' (HTTP " + status + "): "
                        + EmbedFactory.truncate(response.body(), 200));
                return false;
            }
            return true;
        }
        return false;
    }

    /** Milliseconds to wait after a 429, from the body (retry_after) or the Retry-After header. */
    private static long retryAfter(HttpResponse<String> response) {
        try {
            DataObject data = DataObject.fromJson(response.body());
            if (data.hasKey("retry_after")) return Math.min(60_000L, (long) (data.getDouble("retry_after") * 1000) + 50);
        } catch (Exception ignored) { }
        return seconds(response.headers().firstValue("Retry-After").orElse(null), 2);
    }

    private static long seconds(String value, double fallback) {
        double s = fallback;
        if (value != null) {
            try { s = Double.parseDouble(value); } catch (NumberFormatException ignored) { }
        }
        return Math.min(60_000L, (long) (s * 1000) + 50);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** On disable: sends what is still queued, waiting at most a few seconds. */
    public void shutdown() {
        shutdown = true;
        for (ExecutorService queue : queues.values()) queue.shutdown();
        long deadline = System.currentTimeMillis() + 3000;
        for (ExecutorService queue : queues.values()) {
            try {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0 || !queue.awaitTermination(left, TimeUnit.MILLISECONDS)) queue.shutdownNow();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                queue.shutdownNow();
            }
        }
    }
}
