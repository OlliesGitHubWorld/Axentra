package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.PlayerProfile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Posts a weekly summary to the weekly-report channel: players, playtime, cases, top punishments,
 * top staff, appeals and performance, each compared with the week before (▲▼). Uses only data
 * Axentra already stores. /axentra discord report sends it right away.
 */
public class WeeklyReportService {

    private static final long WEEK = 7L * 86_400_000L;

    private final App plugin;
    private final DiscordManager discord;
    private final EmbedFactory embeds;
    private BukkitTask task;
    private LocalDate lastSent;

    public WeeklyReportService(App plugin, DiscordManager discord, EmbedFactory embeds) {
        this.plugin = plugin;
        this.discord = discord;
        this.embeds = embeds;
        discord.setWeeklyReportSender(this::sendNow);
        discord.onReady(jda -> Bukkit.getScheduler().runTask(plugin, this::schedule));
        discord.onReload(this::schedule);
        // A weekly-report webhook works without the bot
        Bukkit.getScheduler().runTask(plugin, this::schedule);
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(discord.config().getString("weekly-report.timezone", "UTC"));
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }

    /** Checks once a minute whether it's the configured day and time. */
    public void schedule() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tick, 20L * 30, 20L * 60);
    }

    private void tick() {
        if (!discord.canPost("weekly-report") || !discord.config().getBoolean("weekly-report.enabled", true)) return;
        ZonedDateTime now = ZonedDateTime.now(zone());
        DayOfWeek day;
        LocalTime time;
        try {
            day = DayOfWeek.valueOf(discord.config().getString("weekly-report.day", "MONDAY").toUpperCase(Locale.ROOT));
            time = LocalTime.parse(discord.config().getString("weekly-report.time", "09:00"));
        } catch (Exception e) {
            return;
        }
        if (now.getDayOfWeek() != day || now.toLocalTime().getHour() != time.getHour()
                || now.toLocalTime().getMinute() != time.getMinute()) return;
        if (now.toLocalDate().equals(lastSent)) return;
        lastSent = now.toLocalDate();
        send(null);
    }

    /** /axentra discord report */
    public void sendNow(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> send(sender));
    }

    private void send(CommandSender sender) {
        boolean webhook = discord.usesWebhook("weekly-report");
        TextChannel channel = webhook ? null : discord.getChannel("weekly-report");
        if (!webhook && channel == null) {
            reply(sender, discord.ingame("weekly-report-no-channel"));
            return;
        }
        long to = System.currentTimeMillis();
        long from = to - WEEK;
        DatabaseManager.WeeklyStats now = plugin.getDatabaseManager().getWeeklyStats(from, to);
        DatabaseManager.WeeklyStats before = plugin.getDatabaseManager().getWeeklyStats(from - WEEK, from);

        if (webhook) {
            discord.webhooks().send("weekly-report", WebhookSender.Message.embed(build(now, before, from, to).build()))
                    .thenAccept(ok -> reply(sender, discord.ingame(ok ? "weekly-report-sent" : "weekly-report-no-channel")));
            return;
        }
        channel.sendMessageEmbeds(build(now, before, from, to).build()).queue(
                ok -> reply(sender, discord.ingame("weekly-report-sent")),
                err -> reply(sender, discord.ingame("weekly-report-no-channel")));
    }

    private void reply(CommandSender sender, String message) {
        if (sender != null) Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    private EmbedBuilder build(DatabaseManager.WeeklyStats s, DatabaseManager.WeeklyStats p, long from, long to) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMM").withZone(zone());
        DateTimeFormatter peakFmt = DateTimeFormatter.ofPattern("EEEE HH:00").withZone(zone());
        EmbedBuilder embed = embeds.base("info").setTitle(discord.text("weekly-title"))
                .setDescription(discord.text("weekly-description", "from", dateFmt.format(Instant.ofEpochMilli(from)),
                        "to", dateFmt.format(Instant.ofEpochMilli(to))));

        embed.addField(discord.text("weekly-field-players"), discord.text("weekly-players-value",
                "new", s.newPlayers + trend(s.newPlayers, p.newPlayers),
                "unique", s.uniquePlayers + trend(s.uniquePlayers, p.uniquePlayers),
                "peak", s.peakPlayers + trend(s.peakPlayers, p.peakPlayers),
                "peak_time", s.peakTime > 0 ? peakFmt.format(Instant.ofEpochMilli(s.peakTime)) : "-"), true);

        long avgSession = s.sessions > 0 ? s.playtimeMillis / s.sessions : 0;
        long prevAvg = p.sessions > 0 ? p.playtimeMillis / p.sessions : 0;
        embed.addField(discord.text("weekly-field-playtime"), discord.text("weekly-playtime-value",
                "total", PlayerProfile.formatMinutes(s.playtimeMillis / 60000) + trend(s.playtimeMillis, p.playtimeMillis),
                "avg", PlayerProfile.formatMinutes(avgSession / 60000) + trend(avgSession, prevAvg)), true);

        StringBuilder cases = new StringBuilder();
        s.casesByType.forEach((type, count) -> cases.append("`").append(type).append("` ").append(count)
                .append(trend(count, p.casesByType.getOrDefault(type, 0))).append('\n'));
        embed.addField(discord.text("weekly-field-cases"), cases.length() > 0 ? cases.toString() : discord.text("weekly-none"), true);

        embed.addField(discord.text("weekly-field-templates"), topTemplates(s), true);
        embed.addField(discord.text("weekly-field-staff"), topStaff(s), false);
        embed.addField(discord.text("weekly-field-appeals"), discord.text("weekly-appeals-value",
                "submitted", s.appeals[0] + trend(s.appeals[0], p.appeals[0]),
                "accepted", String.valueOf(s.appeals[1]), "denied", String.valueOf(s.appeals[2])), false);

        embed.addField(discord.text("weekly-field-performance"), discord.text("weekly-performance-value",
                "tps", s.avgTps > 0 ? String.format(Locale.ROOT, "%.1f", s.avgTps) + trendDouble(s.avgTps, p.avgTps) : "-",
                "alerts", s.lagAlerts + trend(s.lagAlerts, p.lagAlerts),
                "health", s.avgHealth > 0 ? Math.round(s.avgHealth) + trendDouble(s.avgHealth, p.avgHealth) : "-"), false);
        return embed;
    }

    /** Top 3 templates, matched on their reason (cases store the reason, not the template). */
    private String topTemplates(DatabaseManager.WeeklyStats s) {
        ConfigurationSection templates = plugin.getPunishmentService().templates();
        if (templates == null) return discord.text("weekly-none");
        Map<String, Integer> counts = new HashMap<>();
        for (String key : templates.getKeys(false)) {
            ConfigurationSection tmpl = templates.getConfigurationSection(key);
            if (tmpl == null) continue;
            int n = s.casesByReason.getOrDefault(tmpl.getString("reason", ""), 0);
            if (n > 0) {
                String name = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', tmpl.getString("display-name", key)));
                counts.merge(name, n, Integer::sum);
            }
        }
        StringBuilder sb = new StringBuilder();
        counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(3)
                .forEach(e -> sb.append("**").append(EmbedFactory.escape(e.getKey())).append("** ").append(e.getValue()).append('\n'));
        return sb.length() > 0 ? sb.toString() : discord.text("weekly-none");
    }

    private String topStaff(DatabaseManager.WeeklyStats s) {
        List<Map.Entry<String, long[]>> list = new ArrayList<>(s.staff.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue()[0] * 10 + b.getValue()[1] * 5 + b.getValue()[2] / 60,
                a.getValue()[0] * 10 + a.getValue()[1] * 5 + a.getValue()[2] / 60));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            long[] v = list.get(i).getValue();
            sb.append(discord.text("weekly-staff-line", "rank", String.valueOf(i + 1),
                    "name", EmbedFactory.escape(list.get(i).getKey()), "cases", String.valueOf(v[0]),
                    "reports", String.valueOf(v[1]), "duty", PlayerProfile.formatMinutes(v[2]))).append('\n');
        }
        return sb.length() > 0 ? sb.toString() : discord.text("weekly-none");
    }

    private static String trend(long now, long before) {
        if (now == before) return "";
        return now > before ? " ▲" : " ▼";
    }

    private static String trendDouble(double now, double before) {
        if (before <= 0 || Math.abs(now - before) < 0.05) return "";
        return now > before ? " ▲" : " ▼";
    }
}
