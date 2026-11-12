package mc.ollie.discord;

import mc.ollie.App;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.CloseCode;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Owns the JDA connection: start, stop and reload, intent detection, channel checks and the
 * bot status. Connecting happens on an async thread so the server never waits for Discord.
 * Features register their JDA listeners and ready hooks here, so they survive a reconnect.
 */
public class DiscordManager {

    /** Channel keys in discord.yml under channels.* */
    public static final List<String> CHANNEL_KEYS = List.of(
            "cases", "reports", "staffchat", "alerts", "join-leave", "appeals", "weekly-report", "chat", "console");

    private static final List<GatewayIntent> PRIVILEGED = List.of(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT);

    private final App plugin;
    private final List<Object> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<JDA>> readyHooks = new CopyOnWriteArrayList<>();

    private volatile JDA jda;
    private volatile boolean connecting;
    private volatile Set<GatewayIntent> missingIntents = EnumSet.noneOf(GatewayIntent.class);
    private volatile Map<String, String> channelProblems = new LinkedHashMap<>();
    private volatile String lastError;

    private String activeToken = "";
    private String activeGuildId = "";
    private BukkitTask statusTask;

    private final WebhookSender webhooks;

    public DiscordManager(App plugin) {
        this.plugin = plugin;
        this.webhooks = new WebhookSender(plugin, this);
    }

    // ── Config helpers ──────────────────────────────────────

    public FileConfiguration config() {
        return plugin.getFileManager().getDiscord();
    }

    public boolean isEnabledInConfig() {
        return config().getBoolean("enabled", false);
    }

    /** A message from discord.yml (messages.*) with {placeholder} values: text(key, "id", "5", ...). */
    public String text(String key, String... placeholders) {
        String msg = config().getString("messages." + key, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1] == null ? "" : placeholders[i + 1]);
        }
        return msg;
    }

    // ── Lifecycle ───────────────────────────────────────────

    /** Starts the bot if it is enabled. Never blocks the calling thread. */
    public void start() {
        webhooks.reload();
        if (!isEnabledInConfig()) {
            plugin.logInfo("Discord bot is disabled in discord.yml.");
            return;
        }
        String token = config().getString("token", "").trim();
        String guildId = config().getString("guild-id", "").trim();
        if (token.isEmpty()) {
            lastError = "No bot token set in discord.yml.";
            plugin.getLogger().warning("Discord is enabled but no bot token is set in discord.yml. The bot stays offline.");
            return;
        }
        if (guildId.isEmpty()) {
            lastError = "No guild-id set in discord.yml.";
            plugin.getLogger().warning("Discord is enabled but guild-id is empty in discord.yml. The bot stays offline.");
            return;
        }
        if (connecting || jda != null) return;

        connecting = true;
        activeToken = token;
        activeGuildId = guildId;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                connect(token, guildId);
            } finally {
                connecting = false;
            }
        });
    }

    /**
     * Logs in, retrying without privileged intents that aren't enabled in the Developer Portal,
     * so a missing intent turns off one feature instead of the whole bot.
     */
    private void connect(String token, String guildId) {
        // Never ping anyone from text that comes from Minecraft (@everyone, @here, roles, users).
        net.dv8tion.jda.api.utils.messages.MessageRequest.setDefaultMentions(
                EnumSet.noneOf(net.dv8tion.jda.api.entities.Message.MentionType.class));

        List<Set<GatewayIntent>> attempts = List.of(
                EnumSet.copyOf(PRIVILEGED),
                EnumSet.of(GatewayIntent.GUILD_MEMBERS),
                EnumSet.of(GatewayIntent.MESSAGE_CONTENT),
                EnumSet.noneOf(GatewayIntent.class));

        for (Set<GatewayIntent> intents : attempts) {
            AtomicReference<CloseCode> closeCode = new AtomicReference<>();
            JDA built;
            try {
                JDABuilder builder = JDABuilder.createDefault(token)
                        .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                        .disableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EXPRESSIONS,
                                GatewayIntent.SCHEDULED_EVENTS)
                        .setEnableShutdownHook(false)
                        .setActivity(buildActivity())
                        .addEventListeners(new ListenerAdapter() {
                            @Override
                            public void onShutdown(@NotNull ShutdownEvent event) {
                                closeCode.set(event.getCloseCode());
                            }
                        });
                if (!intents.isEmpty()) builder.enableIntents(intents);
                for (Object listener : listeners) builder.addEventListeners(listener);
                built = builder.build();
            } catch (IllegalArgumentException e) {
                // Thrown for a token with an invalid format — never print the token itself.
                fail("The bot token in discord.yml is not valid.");
                return;
            } catch (Exception e) {
                fail("Could not start the Discord bot: " + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? " — " + e.getMessage() : ""));
                return;
            }

            try {
                built.awaitReady();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                built.shutdownNow();
                return;
            } catch (RuntimeException e) {
                // awaitReady fails as soon as JDA starts shutting down, but the ShutdownEvent with the
                // close code arrives a moment later. Wait for the shutdown so the reason is known.
                built.shutdownNow();
                try {
                    built.awaitShutdown(Duration.ofSeconds(10));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                CloseCode code = closeCode.get();
                if (code == CloseCode.DISALLOWED_INTENTS) continue; // try with fewer privileged intents
                if (code == CloseCode.AUTHENTICATION_FAILED) {
                    fail("The bot token in discord.yml is invalid or was reset.");
                } else if (code != null) {
                    fail("Discord closed the connection: " + code.name() + " (" + code.getMeaning() + ").");
                } else {
                    String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    if (e.getCause() != null && e.getCause().getMessage() != null) reason += " — " + e.getCause().getMessage();
                    fail("Could not connect to Discord: " + reason);
                }
                return;
            }

            Set<GatewayIntent> missing = EnumSet.noneOf(GatewayIntent.class);
            for (GatewayIntent intent : PRIVILEGED) {
                if (!intents.contains(intent)) missing.add(intent);
            }
            missingIntents = missing;
            lastError = null;
            jda = built;
            onReady(built, guildId);
            return;
        }
    }

    private void fail(String message) {
        lastError = message;
        plugin.getLogger().warning(message + " The rest of Axentra keeps running.");
    }

    private void onReady(JDA jda, String guildId) {
        plugin.getLogger().info("Connected to Discord as " + jda.getSelfUser().getName() + ".");

        if (missingIntents.contains(GatewayIntent.GUILD_MEMBERS)) {
            plugin.getLogger().warning("The SERVER MEMBERS intent is off in the Discord Developer Portal. "
                    + "Role changes made in Discord are only picked up by the periodic role sync.");
        }
        if (missingIntents.contains(GatewayIntent.MESSAGE_CONTENT)) {
            plugin.getLogger().warning("The MESSAGE CONTENT intent is off in the Discord Developer Portal. "
                    + "Staff chat from Discord to Minecraft is turned off.");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning("The bot is not in the Discord server with guild-id " + guildId
                    + ". Invite it with the link from the Developer Portal (see the top of discord.yml).");
        }
        checkChannels(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (statusTask != null) statusTask.cancel();
            statusTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateStatus, 20L, 20L * 60);
        });

        for (Consumer<JDA> hook : readyHooks) {
            try {
                hook.accept(jda);
            } catch (Exception e) {
                plugin.getLogger().warning("Discord feature failed to start: " + e.getMessage());
            }
        }
    }

    /** Stops the bot and waits briefly for a clean disconnect. */
    public void stop() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        JDA current = jda;
        jda = null;
        if (current == null) return;
        current.shutdown();
        try {
            if (!current.awaitShutdown(Duration.ofSeconds(5))) current.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            current.shutdownNow();
        }
    }

    /**
     * Called after /axentra reload. Reconnects only when enabled, token or guild changed;
     * otherwise re-applies the status and re-checks the channels.
     */
    public void reload() {
        boolean enabled = isEnabledInConfig();
        String token = config().getString("token", "").trim();
        String guildId = config().getString("guild-id", "").trim();
        boolean running = jda != null;

        boolean changed = enabled != running || !token.equals(activeToken) || !guildId.equals(activeGuildId);
        if (changed) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                stop();
                if (enabled) {
                    channelProblems = new LinkedHashMap<>();
                    Bukkit.getScheduler().runTask(plugin, this::start);
                } else {
                    plugin.logInfo("Discord bot stopped.");
                }
            });
        } else if (running) {
            updateStatus();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> checkChannels(true));
            for (Runnable hook : reloadHooks) hook.run();
        }
        webhooks.reload();
        for (Runnable hook : configReloadHooks) hook.run();
    }

    private final List<Runnable> reloadHooks = new CopyOnWriteArrayList<>();
    private final List<Runnable> configReloadHooks = new CopyOnWriteArrayList<>();

    /** Runs on the main thread after /axentra reload when the bot stays connected (timers, schedules). */
    public void onReload(Runnable hook) {
        reloadHooks.add(hook);
    }

    /** Runs on the main thread after every /axentra reload, also without the bot (webhook features). */
    public void onConfigReload(Runnable hook) {
        configReloadHooks.add(hook);
    }

    // ── Status & checks ─────────────────────────────────────

    private Activity buildActivity() {
        String type = config().getString("status.type", "WATCHING").toUpperCase(Locale.ROOT);
        String text = config().getString("status.text", "{online}/{max} players")
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()));
        return switch (type) {
            case "PLAYING" -> Activity.playing(text);
            case "LISTENING" -> Activity.listening(text);
            default -> Activity.watching(text);
        };
    }

    /** Runs on the main thread; setActivity is queued by JDA and doesn't block. */
    private void updateStatus() {
        JDA current = jda;
        if (current != null) current.getPresence().setActivity(buildActivity());
    }

    /** Checks that each enabled channel exists and the bot can write in it. */
    private void checkChannels(boolean log) {
        Map<String, String> problems = new LinkedHashMap<>();
        Guild guild = getGuild();
        for (String key : CHANNEL_KEYS) {
            if (!isChannelEnabled(key)) continue;
            String id = config().getString("channels." + key + ".id", "").trim();
            if (guild == null) {
                problems.put(key, "bot not in guild");
                continue;
            }
            TextChannel channel = guild.getTextChannelById(id);
            if (channel == null) {
                problems.put(key, "channel " + id + " not found");
                continue;
            }
            Member self = guild.getSelfMember();
            if (!self.hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                problems.put(key, "missing View/Send/Embed permission in #" + channel.getName());
            }
        }
        channelProblems = problems;
        if (log) {
            for (Map.Entry<String, String> p : problems.entrySet()) {
                plugin.getLogger().warning("Discord channel '" + p.getKey() + "': " + p.getValue() + ".");
            }
        }
    }

    // ── Accessors used by features ──────────────────────────

    /** Registers a JDA event listener. Added to the current and every future connection. */
    public void addListener(Object listener) {
        listeners.add(listener);
        JDA current = jda;
        if (current != null) current.addEventListener(listener);
    }

    /** Runs when the bot is connected (also after a reconnect). Runs on a JDA/async thread. */
    public void onReady(Consumer<JDA> hook) {
        readyHooks.add(hook);
        JDA current = jda;
        if (current != null) hook.accept(current);
    }

    public JDA getJda() { return jda; }

    public boolean isConnected() {
        JDA current = jda;
        return current != null && current.getStatus() == JDA.Status.CONNECTED;
    }

    public Guild getGuild() {
        JDA current = jda;
        if (current == null || activeGuildId.isEmpty()) return null;
        return current.getGuildById(activeGuildId);
    }

    public boolean isChannelEnabled(String key) {
        return config().getBoolean("channels." + key + ".enabled", false)
                && !config().getString("channels." + key + ".id", "").trim().isEmpty();
    }

    /** The text channel for a key, or null when it's disabled, missing or not writable. */
    public TextChannel getChannel(String key) {
        if (!isChannelEnabled(key) || channelProblems.containsKey(key)) return null;
        Guild guild = getGuild();
        if (guild == null) return null;
        return guild.getTextChannelById(config().getString("channels." + key + ".id", "").trim());
    }

    public WebhookSender webhooks() { return webhooks; }

    /** Whether a channel posts through its webhook (channels.&lt;key&gt;.webhook) instead of the bot. */
    public boolean usesWebhook(String key) {
        return webhooks.url(key) != null;
    }

    /** Whether something can be posted to a channel right now: through its webhook or the connected bot. */
    public boolean canPost(String key) {
        return usesWebhook(key) || (isConnected() && getChannel(key) != null);
    }

    public boolean hasIntent(GatewayIntent intent) {
        return jda != null && !missingIntents.contains(intent);
    }

    public Set<GatewayIntent> getMissingIntents() { return missingIntents; }
    public Map<String, String> getChannelProblems() { return channelProblems; }
    public String getLastError() { return lastError; }
    public boolean isConnecting() { return connecting; }

    // ── /axentra discord ────────────────────────────────────

    private java.util.function.Consumer<org.bukkit.command.CommandSender> weeklyReportSender;

    /** Set by the weekly report feature so /axentra discord report can send it right away. */
    public void setWeeklyReportSender(java.util.function.Consumer<org.bukkit.command.CommandSender> sender) {
        this.weeklyReportSender = sender;
    }

    public void sendWeeklyReportNow(org.bukkit.command.CommandSender sender) {
        if (weeklyReportSender == null || (!isConnected() && !usesWebhook("weekly-report"))) {
            sender.sendMessage(ingame("discord-status-disconnected"));
            return;
        }
        weeklyReportSender.accept(sender);
    }

    /** A colored in-game message from messages.yml (not discord.yml). */
    public String ingame(String path) {
        String message = plugin.getFileManager().getMessages().getString(path, path);
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
    }

    /** /axentra discord status: connection, guild, channels and intents. */
    public void sendStatus(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(ingame("discord-status-header"));
        if (isConnected()) {
            sender.sendMessage(ingame("discord-status-connected").replace("%ping%", String.valueOf(jda.getGatewayPing())));
        } else if (connecting) {
            sender.sendMessage(ingame("discord-status-connecting"));
        } else if (!isEnabledInConfig()) {
            sender.sendMessage(ingame("discord-status-disabled"));
            // Webhook channels keep working without the bot
            for (String key : WebhookSender.CHANNEL_KEYS) {
                if (!config().getBoolean("channels." + key + ".enabled", false)) continue;
                if (usesWebhook(key)) {
                    sender.sendMessage(ingame("discord-status-channel-webhook").replace("%channel%", key));
                } else if (webhooks.problem(key) != null) {
                    sender.sendMessage(ingame("discord-status-channel-problem").replace("%channel%", key)
                            .replace("%problem%", webhooks.problem(key)));
                }
            }
            return;
        } else {
            sender.sendMessage(ingame("discord-status-disconnected"));
        }
        if (lastError != null) {
            sender.sendMessage(ingame("discord-status-error").replace("%error%", lastError));
        }
        if (!isConnected()) return;

        Guild guild = getGuild();
        if (guild == null) {
            sender.sendMessage(ingame("discord-status-guild-missing"));
        } else {
            sender.sendMessage(ingame("discord-status-guild").replace("%guild%", guild.getName()));
        }

        for (String key : CHANNEL_KEYS) {
            if (webhooks.problem(key) != null && config().getBoolean("channels." + key + ".enabled", false)) {
                sender.sendMessage(ingame("discord-status-channel-problem").replace("%channel%", key)
                        .replace("%problem%", webhooks.problem(key)));
            } else if (usesWebhook(key)) {
                sender.sendMessage(ingame("discord-status-channel-webhook").replace("%channel%", key));
            } else if (!isChannelEnabled(key)) {
                sender.sendMessage(ingame("discord-status-channel-off").replace("%channel%", key));
            } else if (channelProblems.containsKey(key)) {
                sender.sendMessage(ingame("discord-status-channel-problem").replace("%channel%", key)
                        .replace("%problem%", channelProblems.get(key)));
            } else {
                sender.sendMessage(ingame("discord-status-channel-ok").replace("%channel%", key));
            }
        }

        if (missingIntents.isEmpty()) sender.sendMessage(ingame("discord-status-intents-ok"));
        for (GatewayIntent intent : missingIntents) {
            String feature = intent == GatewayIntent.GUILD_MEMBERS
                    ? "instant role updates from Discord" : "staff chat from Discord";
            sender.sendMessage(ingame("discord-status-intent-missing")
                    .replace("%intent%", intent.name()).replace("%feature%", feature));
        }
    }

    /** Whether a guild member has one of the staff roles from discord.yml. */
    public boolean isStaff(Member member) {
        if (member == null) return false;
        List<String> roles = config().getStringList("staff-roles");
        return member.getRoles().stream().anyMatch(r -> roles.contains(r.getId()));
    }
}
