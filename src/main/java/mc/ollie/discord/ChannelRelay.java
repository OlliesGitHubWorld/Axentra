package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.event.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Posts Axentra activity to Discord channels: #cases, #reports, #staffchat (both ways), #alerts
 * and #join-leave. Bukkit events are handled off the main thread for anything that needs the
 * database, and every Discord call is queued, never awaited.
 */
public class ChannelRelay extends ListenerAdapter implements Listener {

    public static final String BUTTON_PREFIX = "axentra:report:";

    private final App plugin;
    private final DiscordManager discord;
    private final EmbedFactory embeds;

    private final List<String> joinLeaveBuffer = new ArrayList<>();
    private boolean joinLeaveFlushScheduled;
    private final Map<String, Webhook> webhooks = new java.util.concurrent.ConcurrentHashMap<>();

    /** Adds extra components below report messages (the punish menu, see DiscordModeration). */
    private java.util.function.Function<DatabaseManager.ReportInfo, List<ActionRow>> reportExtraRows = r -> List.of();

    public ChannelRelay(App plugin, DiscordManager discord, EmbedFactory embeds) {
        this.plugin = plugin;
        this.discord = discord;
        this.embeds = embeds;
        discord.addListener(this);
    }

    public void setReportExtraRows(java.util.function.Function<DatabaseManager.ReportInfo, List<ActionRow>> rows) {
        this.reportExtraRows = rows;
    }

    private DatabaseManager db() { return plugin.getDatabaseManager(); }

    private void async(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    // ══════════════════════════════════════════════════════
    //  Cases
    // ══════════════════════════════════════════════════════

    /** The embed for a case (used by #cases, /case and appeals). Call off the main thread: reads trust and history. */
    public CaseMessage buildCase(DatabaseManager.CaseInfo info) {
        String trust = plugin.getTrustManager() != null ? plugin.getTrustManager().getTrustLevel(info.playerUuid) : null;
        int previous = Math.max(0, db().getCasesByPlayerUuid(info.playerUuid).size() - 1);

        EmbedBuilder embed = embeds.base(info.type)
                .setTitle(discord.text("case-title", "id", String.valueOf(info.id), "type", info.type));
        embeds.thumbnail(embed, info.playerName, info.playerUuid);
        embed.addField(discord.text("case-field-player"), embeds.playerLink(info.playerName, info.playerUuid), true);
        embed.addField(discord.text("case-field-staff"), EmbedFactory.escape(info.issuedBy), true);
        embed.addField(discord.text("case-field-source"), embeds.sourceLabel(info.source), true);
        embed.addField(discord.text("case-field-reason"),
                EmbedFactory.truncate(EmbedFactory.escape(info.reason != null ? info.reason : "-"), EmbedFactory.FIELD_MAX), false);

        if (info.type.equals("BAN") || info.type.equals("MUTE") || info.type.startsWith("TEMP")) {
            long expires = parseTime(info.expiresAt);
            String value = expires > 0
                    ? EmbedFactory.timestamp(expires, 'f') + " (" + EmbedFactory.timestamp(expires, 'R') + ")"
                    : discord.text("case-permanent");
            embed.addField(discord.text("case-field-expires"), value, true);
        }
        if (trust != null) embed.addField(discord.text("case-field-trust"), trust, true);
        embed.addField(discord.text("case-field-previous"), String.valueOf(previous), true);
        embed.addField(discord.text("case-field-status"), statusLabel(info), true);

        FileUpload file = null;
        if (info.chatLog != null && !info.chatLog.isBlank()) {
            String block = "```\n" + info.chatLog.replace("```", "'''") + "\n```";
            if (block.length() <= EmbedFactory.FIELD_MAX) {
                embed.addField(discord.text("case-field-chatlog"), block, false);
            } else {
                embed.addField(discord.text("case-field-chatlog"), discord.text("case-chatlog-attached"), false);
                file = FileUpload.fromData(info.chatLog.getBytes(StandardCharsets.UTF_8), "chatlog-case-" + info.id + ".txt");
            }
        }
        return new CaseMessage(embed.build(), file);
    }

    public record CaseMessage(MessageEmbed embed, FileUpload chatLog) { }

    private String statusLabel(DatabaseManager.CaseInfo info) {
        if (info.status != null) return discord.text("case-status-" + info.status.toLowerCase(Locale.ROOT));
        if (info.active) {
            long expires = parseTime(info.expiresAt);
            if (expires > 0 && expires < System.currentTimeMillis()) return discord.text("case-status-expired");
            return discord.text("case-status-active");
        }
        return discord.text("case-status-resolved");
    }

    /** Database times are epoch millis or "yyyy-MM-dd HH:mm:ss" in UTC (CURRENT_TIMESTAMP). */
    public static long parseTime(String value) {
        if (value == null || value.isBlank()) return 0;
        try { return Long.parseLong(value.trim()); } catch (NumberFormatException ignored) { }
        try {
            return java.time.LocalDateTime.parse(value.trim().replace(' ', 'T'))
                    .toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        } catch (Exception e) {
            try { return java.sql.Timestamp.valueOf(value.trim()).getTime(); } catch (Exception ignored) { return 0; }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCaseCreated(CaseCreatedEvent event) {
        if (!discord.isConnected() || discord.getChannel("cases") == null) return;
        int caseId = event.getCaseId();
        async(() -> {
            TextChannel channel = discord.getChannel("cases");
            DatabaseManager.CaseInfo info = db().getCase(caseId);
            if (channel == null || info == null) return;
            CaseMessage message = buildCase(info);
            var action = channel.sendMessageEmbeds(message.embed());
            if (message.chatLog() != null) action = action.addFiles(message.chatLog());
            action.queue(sent -> async(() -> db().saveDiscordMessage("CASE", caseId, channel.getId(), sent.getId())),
                    err -> plugin.logInfo("Could not post case #" + caseId + " to Discord: " + err.getMessage()));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCaseUpdated(CaseUpdatedEvent event) {
        if (!discord.isConnected()) return;
        int caseId = event.getCaseId();
        String change = event.getChange();
        String actor = event.getActor();
        async(() -> {
            String[] ref = db().getDiscordMessage("CASE", caseId);
            if (ref == null) return;
            TextChannel channel = discord.getGuild() != null ? discord.getGuild().getTextChannelById(ref[0]) : null;
            if (channel == null) return;
            DatabaseManager.CaseInfo info = db().getCase(caseId);

            if (info == null) {
                // Deleted: keep the message but mark it
                channel.retrieveMessageById(ref[1]).queue(msg -> {
                    if (msg.getEmbeds().isEmpty()) return;
                    EmbedBuilder edited = new EmbedBuilder(msg.getEmbeds().get(0)).setColor(java.awt.Color.DARK_GRAY);
                    replaceField(edited, discord.text("case-field-status"), discord.text("case-status-deleted"));
                    msg.editMessageEmbeds(edited.build()).queue(null, err -> { });
                }, err -> { });
                return;
            }

            MessageEmbed embed = buildCase(info).embed();
            if (actor != null && info.status != null) {
                EmbedBuilder withActor = new EmbedBuilder(embed);
                replaceField(withActor, discord.text("case-field-status"),
                        discord.text("case-status-by", "status", statusLabel(info), "staff", EmbedFactory.escape(actor)));
                embed = withActor.build();
            }
            channel.editMessageEmbedsById(ref[1], embed).queue(null,
                    err -> plugin.logInfo("Could not update case #" + caseId + " on Discord (" + change + "): " + err.getMessage()));
        });
    }

    private static void replaceField(EmbedBuilder embed, String name, String value) {
        List<MessageEmbed.Field> fields = new ArrayList<>(embed.getFields());
        embed.clearFields();
        boolean replaced = false;
        for (MessageEmbed.Field f : fields) {
            if (f.getName() != null && f.getName().equals(name)) {
                embed.addField(name, value, f.isInline());
                replaced = true;
            } else {
                embed.addField(f);
            }
        }
        if (!replaced) embed.addField(name, value, true);
    }

    // ══════════════════════════════════════════════════════
    //  Reports
    // ══════════════════════════════════════════════════════

    private MessageEmbed buildReport(DatabaseManager.ReportInfo report, String location) {
        EmbedBuilder embed = embeds.base("report")
                .setTitle(discord.text("report-title", "id", String.valueOf(report.id), "target", report.targetName));
        embeds.thumbnail(embed, report.targetName, report.targetUuid);
        embed.addField(discord.text("report-field-target"), embeds.playerLink(report.targetName, report.targetUuid), true);
        embed.addField(discord.text("report-field-reporter"), embeds.playerLink(report.reporterName, report.reporterUuid), true);
        if (location != null) embed.addField(discord.text("report-field-location"), "`" + location + "`", true);
        embed.addField(discord.text("report-field-reason"),
                EmbedFactory.truncate(EmbedFactory.escape(report.reason != null ? report.reason : "-"), EmbedFactory.FIELD_MAX), false);
        embed.addField(discord.text("report-field-status"), reportStatus(report), false);
        if (report.chatLog != null && !report.chatLog.isBlank()) {
            embed.addField(discord.text("report-field-chatlog"),
                    "```\n" + EmbedFactory.truncate(report.chatLog.replace("```", "'''"), EmbedFactory.FIELD_MAX - 8) + "\n```", false);
        }
        return embed.build();
    }

    private String reportStatus(DatabaseManager.ReportInfo report) {
        if ("CLOSED".equals(report.status)) {
            String staff = EmbedFactory.escape(report.closedBy != null ? report.closedBy : "?");
            return report.linkedCaseId != null
                    ? discord.text("report-status-closed-case", "staff", staff, "case", String.valueOf(report.linkedCaseId))
                    : discord.text("report-status-closed", "staff", staff);
        }
        if ("CLAIMED".equals(report.status)) {
            return discord.text("report-status-claimed", "staff", EmbedFactory.escape(report.claimedBy));
        }
        return discord.text("report-status-open");
    }

    private List<ActionRow> reportComponents(DatabaseManager.ReportInfo report) {
        if ("CLOSED".equals(report.status)) return List.of();
        List<ItemComponent> buttons = new ArrayList<>();
        if (!"CLAIMED".equals(report.status)) {
            buttons.add(Button.primary(BUTTON_PREFIX + "claim:" + report.id, discord.text("report-button-claim")));
        }
        buttons.add(Button.secondary(BUTTON_PREFIX + "close:" + report.id, discord.text("report-button-close")));
        List<ActionRow> rows = new ArrayList<>();
        rows.add(ActionRow.of(buttons));
        rows.addAll(reportExtraRows.apply(report));
        return rows;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReportCreated(ReportCreatedEvent event) {
        if (!discord.isConnected() || discord.getChannel("reports") == null) return;
        int reportId = event.getReportId();
        String location = event.getLocation();
        async(() -> {
            TextChannel channel = discord.getChannel("reports");
            DatabaseManager.ReportInfo report = db().getReport(reportId);
            if (channel == null || report == null) return;
            channel.sendMessageEmbeds(buildReport(report, location))
                    .setComponents(reportComponents(report))
                    .queue(sent -> {
                                async(() -> db().saveDiscordMessage("REPORT", reportId, channel.getId(), sent.getId()));
                                // A thread per report so staff can discuss it without cluttering the channel
                                if (discord.config().getBoolean("reports.create-thread", true)) {
                                    String name = discord.text("report-thread-name", "id", String.valueOf(reportId),
                                            "target", report.targetName);
                                    sent.createThreadChannel(EmbedFactory.truncate(name, 100)).queue(null,
                                            err -> plugin.logInfo("Could not create a thread for report #" + reportId
                                                    + " (needs Create Public Threads): " + err.getMessage()));
                                }
                            },
                            err -> plugin.logInfo("Could not post report #" + reportId + " to Discord: " + err.getMessage()));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReportUpdated(ReportUpdatedEvent event) {
        if (!discord.isConnected()) return;
        int reportId = event.getReportId();
        async(() -> updateReportMessage(reportId));
    }

    private void updateReportMessage(int reportId) {
        String[] ref = db().getDiscordMessage("REPORT", reportId);
        DatabaseManager.ReportInfo report = db().getReport(reportId);
        if (ref == null || report == null || discord.getGuild() == null) return;
        TextChannel channel = discord.getGuild().getTextChannelById(ref[0]);
        if (channel == null) return;
        channel.retrieveMessageById(ref[1]).queue(msg -> {
            String location = null;
            if (!msg.getEmbeds().isEmpty()) {
                for (MessageEmbed.Field f : msg.getEmbeds().get(0).getFields()) {
                    if (discord.text("report-field-location").equals(f.getName()) && f.getValue() != null) {
                        location = f.getValue().replace("`", "");
                    }
                }
            }
            msg.editMessageEmbeds(buildReport(report, location)).setComponents(reportComponents(report))
                    .queue(null, err -> { });
        }, err -> { });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(BUTTON_PREFIX)) return;
        if (!discord.isStaff(event.getMember())) {
            event.reply(discord.text("command-no-permission")).setEphemeral(true).queue();
            return;
        }
        String[] parts = id.substring(BUTTON_PREFIX.length()).split(":");
        if (parts.length != 2) return;
        int reportId;
        try { reportId = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { return; }
        String action = parts[0];

        event.deferReply(true).queue();
        Member member = event.getMember();
        async(() -> {
            DatabaseManager.ReportInfo report = db().getReport(reportId);
            if (report == null || "CLOSED".equals(report.status)) {
                event.getHook().sendMessage(discord.text("report-already-closed", "id", String.valueOf(reportId))).queue();
                return;
            }
            String staffName = staffName(member);
            if (action.equals("claim")) {
                db().claimReport(reportId, staffName);
                db().logAction(staffName, "DISCORD:" + member.getId(), "CLAIM_REPORT", report.targetName, "#" + reportId);
                event.getHook().sendMessage(discord.text("report-claimed", "id", String.valueOf(reportId))).queue();
            } else if (action.equals("close")) {
                db().closeReport(reportId, staffName, null);
                db().logAction(staffName, "DISCORD:" + member.getId(), "CLOSE_REPORT", report.targetName, "#" + reportId);
                event.getHook().sendMessage(discord.text("report-closed", "id", String.valueOf(reportId))).queue();
            }
        });
    }

    /** Minecraft name of a linked member, else their Discord name. Call off the main thread. */
    public String staffName(Member member) {
        DatabaseManager.DiscordLink link = db().getLinkByDiscord(member.getId());
        return link != null && link.playerName != null ? link.playerName : member.getUser().getName();
    }

    // ══════════════════════════════════════════════════════
    //  Staff chat (both ways)
    // ══════════════════════════════════════════════════════

    /** In-game staff chat to Discord. Called by StaffchatCommand on the main thread. */
    public void relayStaffChat(String senderName, String message) {
        if (!discord.canPost("staffchat")) return;
        String clean = EmbedFactory.defuse(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
        Player sender = Bukkit.getPlayerExact(senderName);
        String uuid = sender != null ? sender.getUniqueId().toString() : null;

        if (discord.usesWebhook("staffchat")) {
            discord.webhooks().send("staffchat",
                    WebhookSender.Message.as(senderName, embeds.avatarUrl(senderName, uuid), clean));
            return;
        }
        TextChannel channel = discord.getChannel("staffchat");
        if (discord.config().getBoolean("staffchat.use-webhook", true)) {
            sendViaWebhook(channel, "Axentra Staffchat", senderName, uuid, clean);
        } else {
            String text = discord.config().getString("staffchat.to-discord-format", "**{player}**: {message}")
                    .replace("{player}", EmbedFactory.escape(senderName)).replace("{message}", clean);
            channel.sendMessage(EmbedFactory.truncate(text, 2000)).queue(null, err -> { });
        }
    }

    /**
     * Posts as a player (name + head) through a webhook named {@code hookName} in the channel,
     * creating it when needed. Falls back to a normal bot message without Manage Webhooks.
     */
    public void sendViaWebhook(TextChannel channel, String hookName, String name, String uuid, String text) {
        Webhook cached = webhooks.get(channel.getId());
        if (cached != null) {
            postWebhook(cached, name, uuid, text);
            return;
        }
        channel.retrieveWebhooks().queue(hooks -> {
            Webhook found = null;
            for (Webhook hook : hooks) {
                if (hookName.equals(hook.getName()) && hook.getToken() != null) {
                    found = hook;
                    break;
                }
            }
            if (found != null) {
                webhooks.put(channel.getId(), found);
                postWebhook(found, name, uuid, text);
            } else {
                channel.createWebhook(hookName).queue(created -> {
                    webhooks.put(channel.getId(), created);
                    postWebhook(created, name, uuid, text);
                }, err -> fallbackMessage(channel, name, text));
            }
        }, err -> fallbackMessage(channel, name, text)); // No Manage Webhooks permission
    }

    private void postWebhook(Webhook hook, String name, String uuid, String text) {
        hook.sendMessage(EmbedFactory.truncate(text, 2000))
                .setUsername(EmbedFactory.truncate(name, 80))
                .setAvatarUrl(embeds.avatarUrl(name, uuid))
                .queue(null, err -> webhooks.remove(hook.getChannel().getId()));
    }

    public String avatarUrl(String name, String uuid) {
        return embeds.avatarUrl(name, uuid);
    }

    public void fallbackMessage(TextChannel channel, String name, String text) {
        channel.sendMessage(EmbedFactory.truncate("**" + EmbedFactory.escape(name) + "**: " + text, 2000)).queue(null, err -> { });
    }
    /** Discord to in-game: only linked members with a staff role. Needs the MESSAGE CONTENT intent. */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()) return;
        TextChannel channel = discord.getChannel("staffchat");
        if (channel == null || !event.getChannel().getId().equals(channel.getId())) return;
        if (!discord.hasIntent(GatewayIntent.MESSAGE_CONTENT)) return;
        Member member = event.getMember();
        if (!discord.isStaff(member)) return;

        Message message = event.getMessage();
        String content = message.getContentDisplay();
        // Minecraft can't show images, so attachments become clickable links that open in the browser
        List<Message.Attachment> attachments = message.getAttachments();
        if (content.isBlank() && attachments.isEmpty()) return;
        String finalContent = EmbedFactory.truncate(content.replace('\n', ' '), 256);

        async(() -> {
            DatabaseManager.DiscordLink link = db().getLinkByDiscord(member.getId());
            if (link == null) {
                // Tell staff why their message didn't reach Minecraft, then clean up the hint
                message.reply(discord.text("staffchat-not-linked")).queue(
                        hint -> hint.delete().queueAfter(15, java.util.concurrent.TimeUnit.SECONDS, null, err -> { }),
                        err -> { });
                return;
            }
            if (discord.config().getBoolean("staffchat.confirm-reaction", false)) {
                message.addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")).queue(null, err -> { });
            }
            IngameLine rendered = render(
                    discord.config().getString("staffchat.to-minecraft-format", "&8[&9Discord&8] &e{name} &8» &f{message}"),
                    link.playerName != null ? link.playerName : member.getUser().getName(),
                    member.getUser().getName(), finalContent, attachments);
            net.md_5.bungee.api.chat.BaseComponent[] line = rendered.components();
            String console = rendered.console();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("Axentra.staffchat")) online.spigot().sendMessage(line);
                }
                Bukkit.getConsoleSender().sendMessage(console);
            });
        });
    }

    /** A Discord message rendered for Minecraft: chat components for players, plain text for the console. */
    public record IngameLine(net.md_5.bungee.api.chat.BaseComponent[] components, String console) { }

    /**
     * Renders a Discord message with a format like "&8[&9Discord&8] &e{name} &8» &f{message}".
     * The Discord text is never color-translated, so nobody can fake colors or formatting.
     * Attachments become clickable [Image]/[File] links, because Minecraft can't show images.
     */
    public IngameLine render(String format, String name, String discordName, String content,
                             List<Message.Attachment> attachments) {
        format = format.replace("{name}", name).replace("{discord}", discordName);
        int at = format.indexOf("{message}");
        String before = ChatColor.translateAlternateColorCodes('&', at >= 0 ? format.substring(0, at) : format + " ");
        String after = ChatColor.translateAlternateColorCodes('&', at >= 0 ? format.substring(at + "{message}".length()) : "");
        String text = ChatColor.stripColor(content);

        List<net.md_5.bungee.api.chat.BaseComponent> parts = new ArrayList<>(
                Arrays.asList(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(before + text)));
        StringBuilder console = new StringBuilder(before + text);
        for (Message.Attachment attachment : attachments) {
            String label = ChatColor.translateAlternateColorCodes('&', discord.config().getString(
                    attachment.isImage() ? "staffchat.image-label" : "staffchat.file-label",
                    attachment.isImage() ? "&b[Image]" : "&b[File]"));
            String hover = ChatColor.translateAlternateColorCodes('&', discord.config()
                    .getString("staffchat.attachment-hover", "&7Click to open &f{name}")
                    .replace("{name}", attachment.getFileName()));
            net.md_5.bungee.api.chat.TextComponent fileLink = new net.md_5.bungee.api.chat.TextComponent(
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText((text.isEmpty() ? "" : " ") + label));
            fileLink.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, attachment.getUrl()));
            fileLink.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                    new net.md_5.bungee.api.chat.hover.content.Text(hover)));
            parts.add(fileLink);
            console.append(' ').append(attachment.getUrl());
        }
        if (!after.isEmpty()) {
            parts.addAll(Arrays.asList(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(after)));
            console.append(after);
        }
        return new IngameLine(parts.toArray(new net.md_5.bungee.api.chat.BaseComponent[0]), console.toString());
    }

    // ══════════════════════════════════════════════════════
    //  Alerts
    // ══════════════════════════════════════════════════════

    private void alert(String type, String title, String description) {
        if (!discord.canPost("alerts") || !discord.config().getBoolean("alerts." + type, true)) return;
        MessageEmbed embed = embeds.base(type.startsWith("lag-recovered") || type.equals("maintenance-off") ? "success" : "alert")
                .setTitle(title).setDescription(EmbedFactory.truncate(description, EmbedFactory.DESCRIPTION_MAX)).build();
        postEmbed("alerts", embed);
    }

    /** Posts an embed through the channel's webhook when one is set, else as the bot. */
    private void postEmbed(String key, MessageEmbed embed) {
        if (discord.usesWebhook(key)) {
            discord.webhooks().send(key, WebhookSender.Message.embed(embed));
            return;
        }
        TextChannel channel = discord.getChannel(key);
        if (channel != null && discord.isConnected()) channel.sendMessageEmbeds(embed).queue(null, err -> { });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAlt(AltDetectedEvent event) {
        alert("alts", discord.text("alert-alt-title"), discord.text("alert-alt-description",
                "player", EmbedFactory.escape(event.getPlayerName()),
                "alts", String.valueOf(event.getAltCount()), "banned", String.valueOf(event.getBannedAlts())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVpn(VpnDetectedEvent event) {
        alert("vpn", discord.text("alert-vpn-title"), discord.text("alert-vpn-description",
                "player", EmbedFactory.escape(event.getPlayerName())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLag(LagAlertEvent event) {
        alert("lag", discord.text("alert-lag-title"), discord.text("alert-lag-description",
                "tps", String.format(Locale.ROOT, "%.1f", event.getTps()),
                "seconds", String.valueOf(event.getDurationSeconds()),
                "chunk", event.getHeaviestChunk() != null ? "`" + event.getHeaviestChunk() + "`" : "-"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLagRecovered(LagRecoveredEvent event) {
        alert("lag-recovered", discord.text("alert-lag-recovered-title"), discord.text("alert-lag-recovered-description",
                "tps", String.format(Locale.ROOT, "%.1f", event.getTps())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMaintenance(MaintenanceToggleEvent event) {
        if (event.isEnabled()) {
            String duration = event.getDurationMinutes() > 0
                    ? EmbedFactory.duration(event.getDurationMinutes() * 60_000L) : "-";
            alert("maintenance", discord.text("alert-maintenance-on-title"), discord.text("alert-maintenance-on-description",
                    "by", EmbedFactory.escape(event.getActivatedBy()),
                    "reason", EmbedFactory.escape(event.getReason() != null ? event.getReason() : "-"),
                    "duration", duration));
        } else {
            alert("maintenance", discord.text("alert-maintenance-off-title"), discord.text("alert-maintenance-off-description"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRestart(RestartScheduledEvent event) {
        alert("restarts", discord.text("alert-restart-title"), discord.text("alert-restart-description",
                "time", EmbedFactory.duration(event.getSeconds() * 1000L)));
    }

    // ══════════════════════════════════════════════════════
    //  Join / leave (batched)
    // ══════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Vanish is applied right after join, so check a tick later
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || plugin.isVanished(player)) return;
            boolean first = !player.hasPlayedBefore() && discord.config().getBoolean("join-leave.first-join", true);
            queueJoinLeave(discord.text(first ? "first-join-line" : "join-line", "player", EmbedFactory.escape(player.getName())));
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.isVanished(event.getPlayer())) return;
        queueJoinLeave(discord.text("leave-line", "player", EmbedFactory.escape(event.getPlayer().getName())));
    }

    /** Main thread. Lines within join-leave.batch-seconds are sent as one message to respect rate limits. */
    private void queueJoinLeave(String line) {
        if (!discord.canPost("join-leave")) return;
        joinLeaveBuffer.add(line);
        if (joinLeaveFlushScheduled) return;
        joinLeaveFlushScheduled = true;
        long delay = Math.max(1, discord.config().getInt("join-leave.batch-seconds", 10)) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, this::flushJoinLeave, delay);
    }

    private void flushJoinLeave() {
        joinLeaveFlushScheduled = false;
        if (joinLeaveBuffer.isEmpty()) return;
        List<String> lines = new ArrayList<>(joinLeaveBuffer);
        joinLeaveBuffer.clear();

        long visible = Bukkit.getOnlinePlayers().stream().filter(p -> !plugin.isVanished(p)).count();
        String footer = discord.text("join-footer", "online", String.valueOf(visible));
        StringBuilder chunk = new StringBuilder();
        for (String line : lines) {
            if (chunk.length() + line.length() + 1 > EmbedFactory.DESCRIPTION_MAX) {
                sendJoinLeave(chunk.toString(), footer);
                chunk.setLength(0);
            }
            chunk.append(line).append('\n');
        }
        if (chunk.length() > 0) sendJoinLeave(chunk.toString(), footer);
    }

    private void sendJoinLeave(String text, String footer) {
        MessageEmbed embed = embeds.base("info").setTitle(discord.text("join-title"))
                .setDescription(text).setFooter(footer).build();
        postEmbed("join-leave", embed);
    }
}
