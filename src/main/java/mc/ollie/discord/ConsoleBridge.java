package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * The server console in a Discord channel (channels.console). Log lines are collected and sent in
 * batches every few seconds as code blocks, with IP addresses hidden by default.
 *
 * <p>Running commands from Discord is off by default (console.allow-commands). When on, only
 * members with a role from console.roles can run them, commands in console.blocked-commands are
 * refused, and every command is written to the audit log.
 */
public class ConsoleBridge extends ListenerAdapter {

    private static final Pattern ANSI = Pattern.compile("\u001B\\[[;\\d]*[ -/]*[@-~]");
    private static final Pattern MC_COLOR = Pattern.compile("(?i)§[0-9A-FK-ORX]");
    private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern IPV6 = Pattern.compile("\\b(?:[0-9a-fA-F]{1,4}:){3,7}[0-9a-fA-F]{1,4}\\b");
    /** Lines kept while Discord is slow; older lines are dropped and counted. */
    private static final int MAX_QUEUE = 2000;
    /** Messages per flush, so a log flood can't hit Discord's rate limits. */
    private static final int MAX_MESSAGES_PER_FLUSH = 4;

    private final App plugin;
    private final DiscordManager discord;
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queued = new AtomicInteger();
    private final AtomicInteger dropped = new AtomicInteger();
    private AbstractAppender appender;
    private BukkitTask task;

    public ConsoleBridge(App plugin, DiscordManager discord) {
        this.plugin = plugin;
        this.discord = discord;
        discord.addListener(this);
        discord.onReady(jda -> Bukkit.getScheduler().runTask(plugin, this::start));
        discord.onConfigReload(this::start);
        // A console webhook works without the bot, so don't wait for it to connect
        Bukkit.getScheduler().runTask(plugin, this::start);
    }

    /** Hooks into the server log when the console channel is on. Safe to call again (reload). */
    public synchronized void start() {
        if (task != null) task.cancel();
        boolean viaBot = discord.isEnabledInConfig() && discord.isChannelEnabled("console");
        if (!viaBot && !discord.usesWebhook("console")) {
            stop();
            return;
        }
        if (appender == null && !installAppender()) return;
        long seconds = Math.max(1, discord.config().getInt("console.flush-seconds", 3));
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flush, 20L * seconds, 20L * seconds);
    }

    /** Adds the log4j appender. Returns false when the server doesn't use log4j. */
    private boolean installAppender() {
        try {
            SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
            appender = new AbstractAppender("AxentraDiscordConsole", null, null, true, Property.EMPTY_ARRAY) {
                @Override
                public void append(LogEvent event) {
                    String logger = event.getLoggerName() != null ? event.getLoggerName() : "";
                    // Never relay JDA's own logging, or a Discord error would loop back into Discord
                    if (logger.startsWith("net.dv8tion")) return;
                    String message = event.getMessage() != null ? event.getMessage().getFormattedMessage() : "";
                    String line = "[" + time.format(new Date(event.getTimeMillis())) + " " + event.getLevel() + "] " + message;
                    // Errors carry their stack trace separately; add the top of it so the cause is visible
                    if (event.getThrown() != null) line += "\n" + stackTrace(event.getThrown());
                    if (queued.incrementAndGet() > MAX_QUEUE) {
                        queued.decrementAndGet();
                        dropped.incrementAndGet();
                        return;
                    }
                    queue.add(line);
                }
            };
            appender.start();
            ((Logger) LogManager.getRootLogger()).addAppender(appender);
            return true;
        } catch (LinkageError | ClassCastException e) {
            appender = null;
            plugin.getLogger().warning("The Discord console channel needs Log4j, which this server doesn't use. It stays off.");
            return false;
        }
    }

    /** Removes the log hook. Called on disable and when the channel is turned off. */
    public synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (appender != null) {
            ((Logger) LogManager.getRootLogger()).removeAppender(appender);
            appender.stop();
            appender = null;
        }
        queue.clear();
        queued.set(0);
    }

    /** The first lines of a stack trace plus every "Caused by" line, at most 20 lines. */
    private static String stackTrace(Throwable thrown) {
        java.io.StringWriter out = new java.io.StringWriter();
        thrown.printStackTrace(new java.io.PrintWriter(out));
        StringBuilder sb = new StringBuilder();
        int lines = 0;
        int sinceCause = 0;
        for (String l : out.toString().split("\\R")) {
            boolean cause = l.startsWith("Caused by:");
            if (cause) sinceCause = 0;
            // Keep the exception line, the first frames of every cause, and skip the rest
            if (cause || sinceCause < 6) {
                sb.append(l).append('\n');
                if (++lines >= 20) break;
            }
            sinceCause++;
        }
        return sb.toString().stripTrailing();
    }

    private String clean(String line) {
        line = ANSI.matcher(line).replaceAll("");
        line = MC_COLOR.matcher(line).replaceAll("");
        if (discord.config().getBoolean("console.hide-ips", true)) {
            line = IPV4.matcher(line).replaceAll("x.x.x.x");
            line = IPV6.matcher(line).replaceAll("x:x:x:x");
        }
        String token = discord.config().getString("token", "");
        if (!token.isBlank() && line.contains(token)) return "[line hidden: contained the bot token]";
        if (discord.webhooks().revealsWebhook(line)) return "[line hidden: contained a webhook URL]";
        return line.replace("```", "'''");
    }

    /** Async: sends the collected lines as code blocks of at most 2000 characters. */
    private void flush() {
        boolean webhook = discord.usesWebhook("console");
        TextChannel channel = webhook ? null : discord.getChannel("console");
        if (!webhook && (channel == null || !discord.isConnected())) return;
        // Webhooks allow fewer messages per minute: wait until the previous batch is out, so lines
        // collect here (where floods are counted and skipped) instead of piling up in the send queue
        if (webhook && discord.webhooks().pending("console") > 0) return;

        List<String> messages = new ArrayList<>();
        StringBuilder block = new StringBuilder();
        String line;
        while ((line = queue.poll()) != null) {
            queued.decrementAndGet();
            line = EmbedFactory.truncate(clean(line), 1900);
            if (block.length() + line.length() + 1 > 1990 - 8) {
                messages.add(block.toString());
                block.setLength(0);
                if (messages.size() >= MAX_MESSAGES_PER_FLUSH) break;
            }
            block.append(line).append('\n');
        }
        if (block.length() > 0 && messages.size() < MAX_MESSAGES_PER_FLUSH) messages.add(block.toString());

        int skipped = dropped.getAndSet(0);
        if (skipped > 0) messages.add(null);
        for (String text : messages) {
            String content = text != null ? "```\n" + text + "```"
                    : discord.text("console-skipped", "lines", String.valueOf(skipped));
            if (webhook) {
                discord.webhooks().send("console", WebhookSender.Message.text(content));
            } else {
                channel.sendMessage(content).queue(null, err -> { });
            }
        }
    }

    // ── Commands from Discord ───────────────────────────────

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()) return;
        TextChannel channel = discord.getChannel("console");
        if (channel == null || !event.getChannel().getId().equals(channel.getId())) return;
        if (!discord.hasIntent(GatewayIntent.MESSAGE_CONTENT)) return;

        String command = event.getMessage().getContentRaw().trim();
        if (command.startsWith("/")) command = command.substring(1);
        if (command.isEmpty()) return;

        if (!discord.config().getBoolean("console.allow-commands", false)) {
            hint(event, discord.text("console-commands-off"));
            return;
        }
        Member member = event.getMember();
        List<String> roles = discord.config().getStringList("console.roles");
        if (member == null || member.getRoles().stream().noneMatch(r -> roles.contains(r.getId()))) {
            hint(event, discord.text("console-no-permission"));
            return;
        }
        String label = command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        String bare = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;
        for (String blocked : discord.config().getStringList("console.blocked-commands")) {
            if (blocked.equalsIgnoreCase(label) || blocked.equalsIgnoreCase(bare)) {
                hint(event, discord.text("console-blocked", "command", label));
                return;
            }
        }

        String finalCommand = command;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.DiscordLink link = plugin.getDatabaseManager().getLinkByDiscord(member.getId());
            String staff = link != null && link.playerName != null ? link.playerName : member.getUser().getName();
            plugin.getDatabaseManager().logAction(staff, "DISCORD:" + member.getId(), "CONSOLE_COMMAND", null, finalCommand);
            plugin.getLogger().info(staff + " (Discord) ran: /" + finalCommand);
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        });
    }

    private void hint(MessageReceivedEvent event, String text) {
        event.getMessage().reply(text).queue(r -> r.delete().queueAfter(15, TimeUnit.SECONDS, null, err -> { }), err -> { });
    }
}
