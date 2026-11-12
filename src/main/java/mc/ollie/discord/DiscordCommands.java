package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.PlayerGroups;
import mc.ollie.PlayerProfile;
import mc.ollie.health.HealthScoreService;
import mc.ollie.health.TickProfiler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Function;

/**
 * Discord slash commands: /case, /history, /lookup and /health for staff, /online for everyone.
 * Replies are deferred and the work happens off the main thread; Bukkit data is read with a
 * short main-thread task.
 */
public class DiscordCommands {

    private final App plugin;
    private final DiscordManager discord;
    private final EmbedFactory embeds;
    private final ChannelRelay relay;
    private final SlashCommandManager commands;

    /** Extra components under /lookup (the punish menu, see DiscordModeration). */
    private Function<PlayerProfile, List<ActionRow>> lookupRows = p -> List.of();

    public DiscordCommands(App plugin, DiscordManager discord, EmbedFactory embeds, ChannelRelay relay,
                           SlashCommandManager commands) {
        this.plugin = plugin;
        this.discord = discord;
        this.embeds = embeds;
        this.relay = relay;
        this.commands = commands;

        commands.register(Commands.slash("case", "Show a moderation case")
                .addOption(OptionType.INTEGER, "id", "Case ID", true), true, this::onCase);
        commands.register(Commands.slash("history", "Summary of a player's cases")
                        .addOption(OptionType.STRING, "player", "Minecraft name", true, true),
                true, this::onHistory, commands::playerNames);
        commands.register(Commands.slash("lookup", "Everything about a player (no IPs)")
                        .addOption(OptionType.STRING, "player", "Minecraft name", true, true),
                true, this::onLookup, commands::playerNames);
        commands.register(Commands.slash("online", "Who is online right now"), false, this::onOnline);
        commands.register(Commands.slash("health", "Server health, TPS and MSPT"), true, this::onHealth);
    }

    public void setLookupRows(Function<PlayerProfile, List<ActionRow>> rows) {
        this.lookupRows = rows;
    }

    private DatabaseManager db() { return plugin.getDatabaseManager(); }

    private void async(Runnable task) { Bukkit.getScheduler().runTaskAsynchronously(plugin, task); }

    // ── /case ───────────────────────────────────────────────

    private void onCase(SlashCommandInteractionEvent event) {
        int id = Objects.requireNonNull(event.getOption("id")).getAsInt();
        event.deferReply(commands.ephemeral()).queue();
        async(() -> {
            DatabaseManager.CaseInfo info = db().getCase(id);
            if (info == null) {
                event.getHook().sendMessage(discord.text("case-not-found", "id", String.valueOf(id))).queue();
                return;
            }
            ChannelRelay.CaseMessage message = relay.buildCase(info);
            var action = event.getHook().sendMessageEmbeds(message.embed());
            if (message.chatLog() != null) action = action.addFiles(message.chatLog());
            action.queue();
        });
    }

    // ── /history ────────────────────────────────────────────

    private void onHistory(SlashCommandInteractionEvent event) {
        String name = Objects.requireNonNull(event.getOption("player")).getAsString();
        event.deferReply(commands.ephemeral()).queue();
        async(() -> {
            String uuid = db().findUuidByName(name);
            if (uuid == null) {
                event.getHook().sendMessage(discord.text("player-not-found", "player", EmbedFactory.escape(name))).queue();
                return;
            }
            String realName = Optional.ofNullable(db().getLastKnownName(uuid)).orElse(name);
            List<DatabaseManager.CaseInfo> cases = db().getCasesByPlayerUuid(uuid);

            Map<String, Integer> totals = new TreeMap<>();
            for (DatabaseManager.CaseInfo c : cases) totals.merge(c.type, 1, Integer::sum);
            StringBuilder totalText = new StringBuilder();
            totals.forEach((type, count) -> totalText.append("`").append(type).append("` ").append(count).append("  "));

            StringBuilder latest = new StringBuilder();
            for (int i = 0; i < Math.min(5, cases.size()); i++) {
                DatabaseManager.CaseInfo c = cases.get(i);
                long time = ChannelRelay.parseTime(c.issuedAt);
                latest.append("**#").append(c.id).append("** ").append(c.type).append(" – ")
                        .append(EmbedFactory.escape(EmbedFactory.truncate(c.reason != null ? c.reason : "-", 80)))
                        .append(time > 0 ? " " + EmbedFactory.timestamp(time, 'R') : "").append('\n');
            }

            EmbedBuilder embed = embeds.base("info").setTitle(discord.text("history-title", "player", realName));
            embeds.thumbnail(embed, realName, uuid);
            embed.addField(discord.text("history-field-totals"),
                    totalText.length() > 0 ? totalText.toString().trim() : discord.text("history-none"), false);
            embed.addField(discord.text("history-field-latest"),
                    latest.length() > 0 ? EmbedFactory.truncate(latest.toString(), EmbedFactory.FIELD_MAX) : discord.text("history-none"), false);
            if (plugin.getTrustManager() != null) {
                embed.addField(discord.text("lookup-field-trust"), plugin.getTrustManager().getTrustLevel(uuid), true);
            }
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    // ── /lookup ─────────────────────────────────────────────

    private void onLookup(SlashCommandInteractionEvent event) {
        String name = Objects.requireNonNull(event.getOption("player")).getAsString();
        event.deferReply(commands.ephemeral()).queue();
        async(() -> {
            PlayerProfile p = PlayerProfile.load(plugin, name);
            if (p == null) {
                event.getHook().sendMessage(discord.text("player-not-found", "player", EmbedFactory.escape(name))).queue();
                return;
            }
            boolean online = Bukkit.getPlayer(UUID.fromString(p.uuid)) != null;
            EmbedBuilder embed = embeds.base("info").setTitle(discord.text("lookup-title", "player", p.name));
            embeds.thumbnail(embed, p.name, p.uuid);
            if (p.trust != null) embed.addField(discord.text("lookup-field-trust"), p.trust, true);
            embed.addField(discord.text("lookup-field-risk"), p.riskScore + "/100", true);
            embed.addField(discord.text("lookup-field-alts"), discord.text("lookup-alts-value",
                    "alts", String.valueOf(p.alts), "banned", String.valueOf(p.bannedAlts)), true);
            embed.addField(discord.text("lookup-field-playtime"), PlayerProfile.formatMinutes(p.playtimeMinutes), true);
            embed.addField(discord.text("lookup-field-first-join"),
                    p.firstJoin > 0 ? EmbedFactory.timestamp(p.firstJoin, 'D') : discord.text("lookup-never"), true);
            embed.addField(discord.text("lookup-field-last-seen"), online ? discord.text("lookup-online-now")
                    : p.lastSeen > 0 ? EmbedFactory.timestamp(p.lastSeen, 'R') : discord.text("lookup-never"), true);
            embed.addField(discord.text("lookup-field-discord"),
                    p.discordId != null ? "<@" + p.discordId + ">" : discord.text("lookup-not-linked"), true);
            embed.addField(discord.text("history-field-totals"), String.valueOf(p.caseCount), true);
            embed.addField(discord.text("lookup-field-notes"), String.valueOf(p.noteCount), true);

            event.getHook().sendMessageEmbeds(embed.build()).setComponents(lookupRows.apply(p)).queue();
        });
    }

    // ── /online ─────────────────────────────────────────────

    private void onOnline(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Bukkit.getScheduler().runTask(plugin, () -> {
            LinkedHashMap<String, List<Player>> groups = PlayerGroups.group(plugin, false);
            int visible = groups.values().stream().mapToInt(List::size).sum();

            List<String> onDuty = new ArrayList<>();
            if (plugin.getDutyManager() != null) {
                for (UUID uuid : plugin.getDutyManager().getOnDutyStaff()) {
                    Player staff = Bukkit.getPlayer(uuid);
                    if (staff != null && !plugin.isVanished(staff)) onDuty.add(EmbedFactory.escape(staff.getName()));
                }
            }

            EmbedBuilder embed = embeds.base("info").setTitle(discord.text("online-title",
                    "online", String.valueOf(visible), "max", String.valueOf(Bukkit.getMaxPlayers())));
            if (visible == 0) embed.setDescription(discord.text("online-none"));
            for (Map.Entry<String, List<Player>> group : groups.entrySet()) {
                StringBuilder names = new StringBuilder();
                for (Player p : group.getValue()) {
                    if (names.length() > 0) names.append(", ");
                    names.append(EmbedFactory.escape(p.getName()));
                }
                embed.addField(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', group.getKey()))
                        + " (" + group.getValue().size() + ")", EmbedFactory.truncate(names.toString(), EmbedFactory.FIELD_MAX), false);
            }
            if (!onDuty.isEmpty()) {
                embed.addField(discord.text("online-field-staff"), String.join(", ", onDuty), false);
            }
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

    // ── /health ─────────────────────────────────────────────

    private void onHealth(SlashCommandInteractionEvent event) {
        TickProfiler profiler = plugin.getTickProfiler();
        if (profiler == null) {
            event.reply(discord.text("health-unavailable")).setEphemeral(true).queue();
            return;
        }
        event.deferReply(commands.ephemeral()).queue();
        // The health score reads loaded chunks and entities, so it has to run on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            HealthScoreService health = plugin.getHealthScoreService();
            int score = health != null ? health.calculate() : -1;
            String label = health != null ? health.getLabel(score) : "";

            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576;
            long maxMb = rt.maxMemory() / 1048576;

            EmbedBuilder embed = embeds.base(score >= 70 || score < 0 ? "success" : score >= 50 ? "warn" : "alert")
                    .setTitle(discord.text("health-title"));
            if (score >= 0) embed.addField(discord.text("health-field-score"), "**" + score + "/100** " + label, false);
            embed.addField(discord.text("health-field-tps"), String.format(Locale.ROOT, "%.1f / %.1f / %.1f",
                    profiler.getTps1m(), profiler.getTps5m(), profiler.getTps15m()), true);
            embed.addField(discord.text("health-field-mspt"), String.format(Locale.ROOT, "%.1f / %.1f",
                    profiler.getMsptAvg(), profiler.getMsptMax()), true);
            embed.addField(discord.text("health-field-players"),
                    Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers(), true);
            embed.addField(discord.text("health-field-memory"), usedMb + " / " + maxMb + " MB", true);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }
}
