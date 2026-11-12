package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.FreezeManager;
import mc.ollie.PlayerProfile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

/**
 * Ban and mute appeals: /appeal &lt;case-id&gt; opens a form in Discord, staff get a full dossier
 * in #appeals with Accept / Deny / Reduce / Discuss buttons, and the player gets a DM with the
 * outcome. {@link #decide} is shared with the in-game /appeals GUI.
 */
public class AppealService extends ListenerAdapter {

    private static final String FORM = "axentra:appealform:";
    private static final String BUTTON = "axentra:appeal:";
    private static final String DENY_FORM = "axentra:appealdeny:";
    private static final String REDUCE_FORM = "axentra:appealreduce:";
    private static final String PICK = "axentra:appealpick";
    private static final Set<String> APPEALABLE = Set.of("BAN", "TEMPBAN", "MUTE", "TEMPMUTE");

    public enum Decision { ACCEPT, DENY, REDUCE }

    private final App plugin;
    private final DiscordManager discord;
    private final EmbedFactory embeds;

    public AppealService(App plugin, DiscordManager discord, EmbedFactory embeds, SlashCommandManager commands) {
        this.plugin = plugin;
        this.discord = discord;
        this.embeds = embeds;
        discord.addListener(this);
        commands.register(Commands.slash("appeal", "Appeal a ban or mute")
                .addOption(OptionType.INTEGER, "case", "The case ID from your ban screen (optional when your account is linked)", false),
                false, this::onAppealCommand);
    }

    private DatabaseManager db() { return plugin.getDatabaseManager(); }

    private void async(Runnable task) { Bukkit.getScheduler().runTaskAsynchronously(plugin, task); }

    private boolean enabled() { return discord.config().getBoolean("appeals.enabled", true); }

    /** Whether a case is an active ban or mute. */
    public static boolean isAppealable(DatabaseManager.CaseInfo info) {
        if (info == null || !APPEALABLE.contains(info.type) || !info.active) return false;
        long expires = ChannelRelay.parseTime(info.expiresAt);
        return expires == 0 || expires > System.currentTimeMillis();
    }

    // ══════════════════════════════════════════════════════
    //  Player side
    // ══════════════════════════════════════════════════════

    /**
     * Checks whether this Discord user may appeal this case. Returns an error message, or null when OK.
     * Fills needsName[0] when the user isn't linked and must type their Minecraft name.
     */
    private String check(int caseId, String discordId, boolean[] needsName) {
        if (!enabled()) return discord.text("appeal-disabled");
        DatabaseManager.CaseInfo info = db().getCase(caseId);
        String id = String.valueOf(caseId);
        if (info == null) return discord.text("appeal-not-found", "id", id);
        if (!isAppealable(info)) return discord.text("appeal-not-appealable");

        DatabaseManager.DiscordLink link = db().getLinkByDiscord(discordId);
        if (link != null) {
            if (!link.playerUuid.equals(info.playerUuid)) return discord.text("appeal-not-yours");
        } else if (discord.config().getBoolean("linking.require-link", false)) {
            return discord.text("appeal-link-required");
        } else {
            needsName[0] = true;
        }

        DatabaseManager.AppealInfo latest = db().getLatestAppealForCase(caseId);
        if (latest != null) {
            if ("OPEN".equals(latest.status)) return discord.text("appeal-already-open", "id", id);
            if ("DENIED".equals(latest.status)) {
                long cooldown = Math.max(0, discord.config().getInt("appeals.cooldown-days", 7)) * 86_400_000L;
                long left = latest.decidedAt + cooldown - System.currentTimeMillis();
                if (left > 0) {
                    return discord.text("appeal-cooldown", "id", id, "days", String.valueOf((left + 86_399_999L) / 86_400_000L));
                }
            }
        }
        return null;
    }

    private void onAppealCommand(SlashCommandInteractionEvent event) {
        var option = event.getOption("case");
        if (option != null) {
            replyForm(event, option.getAsInt());
            return;
        }
        // No case number: look up the active bans and mutes of the linked account.
        // A modal has to be the first response, so this quick read runs right here on the JDA thread.
        if (!enabled()) {
            event.reply(discord.text("appeal-disabled")).setEphemeral(true).queue();
            return;
        }
        DatabaseManager.DiscordLink link = db().getLinkByDiscord(event.getUser().getId());
        if (link == null) {
            event.reply(discord.text("appeal-need-case")).setEphemeral(true).queue();
            return;
        }
        List<DatabaseManager.CaseInfo> active = new ArrayList<>();
        for (DatabaseManager.CaseInfo c : db().getCasesByPlayerUuid(link.playerUuid)) {
            if (isAppealable(c)) active.add(c);
        }
        if (active.isEmpty()) {
            event.reply(discord.text("appeal-no-active")).setEphemeral(true).queue();
        } else if (active.size() == 1) {
            replyForm(event, active.get(0).id);
        } else {
            net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.Builder menu =
                    net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu.create(PICK)
                            .setPlaceholder(EmbedFactory.truncate(discord.text("appeal-pick-placeholder"), 150));
            for (DatabaseManager.CaseInfo c : active.subList(0, Math.min(25, active.size()))) {
                menu.addOption(EmbedFactory.truncate("#" + c.id + " " + c.type, 100), String.valueOf(c.id),
                        EmbedFactory.truncate(c.reason != null ? c.reason : "-", 100));
            }
            event.reply(discord.text("appeal-pick")).setEphemeral(true).setComponents(ActionRow.of(menu.build())).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals(PICK) || event.getValues().isEmpty()) return;
        replyForm(event, parse(event.getValues().get(0)));
    }

    /** Checks the case and opens the appeal form. Used by /appeal and the pick menu. */
    private <T extends net.dv8tion.jda.api.interactions.callbacks.IModalCallback
            & net.dv8tion.jda.api.interactions.callbacks.IReplyCallback> void replyForm(T event, int caseId) {
        // A modal has to be the first response, so these quick checks run right here on the JDA thread
        boolean[] needsName = new boolean[1];
        String error = check(caseId, event.getUser().getId(), needsName);
        if (error != null) {
            event.reply(error).setEphemeral(true).queue();
            return;
        }

        List<ActionRow> rows = new ArrayList<>();
        if (needsName[0]) {
            rows.add(ActionRow.of(TextInput.create("mcname", discord.text("appeal-modal-name"), TextInputStyle.SHORT)
                    .setRequired(true).setMaxLength(16).build()));
        }
        List<String> questions = discord.config().getStringList("appeals.questions");
        if (questions.isEmpty()) questions = List.of("Why should your punishment be lifted?");
        for (int i = 0; i < Math.min(4, questions.size()); i++) {
            rows.add(ActionRow.of(TextInput.create("q" + i, EmbedFactory.truncate(questions.get(i), 45), TextInputStyle.PARAGRAPH)
                    .setRequired(true).setMaxLength(1000).build()));
        }
        Modal modal = Modal.create(FORM + caseId, EmbedFactory.truncate(discord.text("appeal-modal-title", "id", String.valueOf(caseId)), 45))
                .addComponents(rows).build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.startsWith(FORM)) {
            onAppealSubmitted(event, parse(id.substring(FORM.length())));
        } else if (id.startsWith(DENY_FORM)) {
            ModalMapping reason = event.getValue("reason");
            handleDecision(event, parse(id.substring(DENY_FORM.length())), Decision.DENY,
                    reason != null ? reason.getAsString().trim() : "");
        } else if (id.startsWith(REDUCE_FORM)) {
            ModalMapping duration = event.getValue("duration");
            handleDecision(event, parse(id.substring(REDUCE_FORM.length())), Decision.REDUCE,
                    duration != null ? duration.getAsString().trim() : "");
        }
    }

    private void onAppealSubmitted(ModalInteractionEvent event, int caseId) {
        User user = event.getUser();
        List<String> questions = discord.config().getStringList("appeals.questions");
        StringBuilder answers = new StringBuilder();
        for (int i = 0; i < Math.min(4, Math.max(1, questions.size())); i++) {
            ModalMapping value = event.getValue("q" + i);
            if (value == null) continue;
            String question = i < questions.size() ? questions.get(i) : "Why should your punishment be lifted?";
            if (answers.length() > 0) answers.append('\u001E');
            answers.append(question.replace('\u001E', ' ').replace('\u001F', ' ')).append('\u001F')
                    .append(value.getAsString().replace('\u001E', ' ').replace('\u001F', ' '));
        }
        ModalMapping nameValue = event.getValue("mcname");
        String typedName = nameValue != null ? nameValue.getAsString().trim() : null;

        event.deferReply(true).queue();
        async(() -> {
            boolean[] needsName = new boolean[1];
            String error = check(caseId, user.getId(), needsName);
            DatabaseManager.CaseInfo info = db().getCase(caseId);
            if (error == null && needsName[0] && (typedName == null || info == null || !typedName.equalsIgnoreCase(info.playerName))) {
                error = discord.text("appeal-name-mismatch", "id", String.valueOf(caseId));
            }
            if (error != null || info == null) {
                event.getHook().sendMessage(error != null ? error : discord.text("appeal-not-found", "id", String.valueOf(caseId))).queue();
                return;
            }

            int appealId = db().createAppeal(caseId, info.playerUuid, info.playerName, user.getId(), answers.toString());
            db().logAction(info.playerName, info.playerUuid, "APPEAL_SUBMITTED", info.playerName, "case #" + caseId);
            event.getHook().sendMessage(discord.text("appeal-submitted", "id", String.valueOf(caseId))).queue();
            postToStaff(appealId);
            notifyStaffInGame(info.playerName, caseId);
        });
    }

    private void notifyStaffInGame(String player, int caseId) {
        String msg = discord.ingame("appeal-new").replace("%player%", player).replace("%case%", String.valueOf(caseId));
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("Axentra.appeals")).forEach(p -> p.sendMessage(msg)));
    }

    // ══════════════════════════════════════════════════════
    //  Staff dossier
    // ══════════════════════════════════════════════════════

    /** Builds the dossier embed. Call off the main thread. */
    private MessageEmbed buildDossier(DatabaseManager.AppealInfo appeal) {
        DatabaseManager.CaseInfo info = db().getCase(appeal.caseId);
        PlayerProfile profile = appeal.playerUuid != null ? PlayerProfile.load(plugin, appeal.playerUuid) : null;

        EmbedBuilder embed = embeds.base("appeal").setTitle(discord.text("appeal-title",
                "id", String.valueOf(appeal.caseId), "player", appeal.playerName != null ? appeal.playerName : "?"));
        embeds.thumbnail(embed, appeal.playerName, appeal.playerUuid);

        if (info != null) {
            long expires = ChannelRelay.parseTime(info.expiresAt);
            String caseText = "**#" + info.id + " " + info.type + "** by " + EmbedFactory.escape(info.issuedBy) + "\n"
                    + EmbedFactory.escape(EmbedFactory.truncate(info.reason != null ? info.reason : "-", 300)) + "\n"
                    + (expires > 0 ? discord.text("case-field-expires") + ": " + EmbedFactory.timestamp(expires, 'R')
                    : discord.text("case-permanent"));
            embed.addField(discord.text("appeal-field-case"), caseText, false);
            if (info.chatLog != null && !info.chatLog.isBlank()) {
                embed.addField(discord.text("appeal-field-chatlog"),
                        "```\n" + EmbedFactory.truncate(info.chatLog.replace("```", "'''"), 600) + "\n```", false);
            }
        }

        if (appeal.playerUuid != null) {
            StringBuilder previous = new StringBuilder();
            Map<String, Integer> totals = new TreeMap<>();
            for (DatabaseManager.CaseInfo c : db().getCasesByPlayerUuid(appeal.playerUuid)) {
                if (c.id != appeal.caseId) totals.merge(c.type, 1, Integer::sum);
            }
            totals.forEach((type, count) -> previous.append("`").append(type).append("` ").append(count).append("  "));
            embed.addField(discord.text("appeal-field-previous"),
                    previous.length() > 0 ? previous.toString().trim() : discord.text("history-none"), false);

            StringBuilder notes = new StringBuilder();
            List<DatabaseManager.NoteInfo> noteList = db().getNotesByUuid(appeal.playerUuid);
            if (noteList != null) {
                for (DatabaseManager.NoteInfo note : noteList) {
                    notes.append("• ").append(EmbedFactory.escape(note.note)).append(" — ").append(EmbedFactory.escape(note.addedBy)).append('\n');
                }
            }
            if (notes.length() > 0) {
                embed.addField(discord.text("appeal-field-notes"), EmbedFactory.truncate(notes.toString(), 400), false);
            }
        }

        if (profile != null) {
            embed.addField(discord.text("appeal-field-profile"), discord.text("appeal-profile-value",
                    "trust", profile.trust != null ? profile.trust : "-",
                    "risk", profile.riskScore + "/100",
                    "alts", profile.alts + " (" + profile.bannedAlts + " banned)",
                    "playtime", PlayerProfile.formatMinutes(profile.playtimeMinutes),
                    "first", profile.firstJoin > 0 ? EmbedFactory.timestamp(profile.firstJoin, 'D') : "-"), false);
        }

        for (String[] qa : appeal.answerPairs()) {
            embed.addField(EmbedFactory.truncate(qa[0], 256), EmbedFactory.truncate(EmbedFactory.escape(qa[1]), 800), false);
        }
        embed.addField(discord.text("appeal-field-status"), statusText(appeal), false);
        return embed.build();
    }

    private String statusText(DatabaseManager.AppealInfo appeal) {
        String staff = EmbedFactory.escape(appeal.decidedBy != null ? appeal.decidedBy : "?");
        return switch (appeal.status) {
            case "ACCEPTED" -> discord.text("appeal-status-accepted", "staff", staff);
            case "REDUCED" -> discord.text("appeal-status-reduced", "staff", staff, "duration", appeal.decision);
            case "DENIED" -> appeal.decision != null && !appeal.decision.isBlank()
                    ? discord.text("appeal-status-denied-reason", "staff", staff, "reason", EmbedFactory.escape(appeal.decision))
                    : discord.text("appeal-status-denied", "staff", staff);
            default -> discord.text("appeal-status-open");
        };
    }

    private List<ActionRow> buttons(DatabaseManager.AppealInfo appeal) {
        Button thread = Button.secondary(BUTTON + "thread:" + appeal.id, discord.text("appeal-button-thread"));
        if (!"OPEN".equals(appeal.status)) return List.of(ActionRow.of(thread));
        return List.of(ActionRow.of(
                Button.success(BUTTON + "accept:" + appeal.id, discord.text("appeal-button-accept")),
                Button.danger(BUTTON + "deny:" + appeal.id, discord.text("appeal-button-deny")),
                Button.primary(BUTTON + "reduce:" + appeal.id, discord.text("appeal-button-reduce")),
                thread));
    }

    private void postToStaff(int appealId) {
        TextChannel channel = discord.getChannel("appeals");
        DatabaseManager.AppealInfo appeal = db().getAppeal(appealId);
        if (channel == null || appeal == null) return;
        // Optional ping so appeals don't sit unnoticed; only this role can be mentioned
        String pingRole = discord.config().getString("appeals.ping-role", "").trim();
        var action = pingRole.isEmpty()
                ? channel.sendMessageEmbeds(buildDossier(appeal))
                : channel.sendMessage("<@&" + pingRole + ">").setEmbeds(buildDossier(appeal)).mentionRoles(pingRole);
        action.setComponents(buttons(appeal)).queue(
                sent -> async(() -> db().saveDiscordMessage("APPEAL", appealId, channel.getId(), sent.getId())),
                err -> plugin.logInfo("Could not post appeal #" + appealId + " to Discord: " + err.getMessage()));
    }

    /** Rebuilds the #appeals message after a decision. Call off the main thread. */
    private void updateStaffMessage(int appealId) {
        String[] ref = db().getDiscordMessage("APPEAL", appealId);
        DatabaseManager.AppealInfo appeal = db().getAppeal(appealId);
        if (ref == null || appeal == null || discord.getGuild() == null) return;
        TextChannel channel = discord.getGuild().getTextChannelById(ref[0]);
        if (channel == null) return;
        channel.editMessageEmbedsById(ref[1], buildDossier(appeal)).setComponents(buttons(appeal)).queue(null, err -> { });
    }

    // ══════════════════════════════════════════════════════
    //  Staff buttons
    // ══════════════════════════════════════════════════════

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(BUTTON)) return;
        String[] parts = id.substring(BUTTON.length()).split(":");
        if (parts.length != 2) return;
        String action = parts[0];
        int appealId = parse(parts[1]);

        if (action.equals("thread")) {
            if (!discord.isStaff(event.getMember())) {
                event.reply(discord.text("command-no-permission")).setEphemeral(true).queue();
                return;
            }
            if (event.getMessage().getStartedThread() != null) {
                event.reply(event.getMessage().getStartedThread().getAsMention()).setEphemeral(true).queue();
                return;
            }
            DatabaseManager.AppealInfo appeal = db().getAppeal(appealId);
            String name = discord.text("appeal-thread-name", "id", String.valueOf(appeal != null ? appeal.caseId : appealId),
                    "player", appeal != null && appeal.playerName != null ? appeal.playerName : "?");
            event.getMessage().createThreadChannel(EmbedFactory.truncate(name, 100)).queue(
                    thread -> event.reply(thread.getAsMention()).setEphemeral(true).queue(),
                    err -> event.reply(discord.text("command-not-ready")).setEphemeral(true).queue());
            return;
        }

        // Modals must be the first response, so the permission check runs here (fast SQLite reads)
        DatabaseManager.AppealInfo appeal = db().getAppeal(appealId);
        if (appeal == null || !"OPEN".equals(appeal.status)) {
            event.reply(discord.text("appeal-already-decided")).setEphemeral(true).queue();
            return;
        }
        String staffName = allowedStaff(event.getMember(), appeal);
        if (staffName == null) {
            event.reply(discord.text("appeal-not-allowed")).setEphemeral(true).queue();
            return;
        }

        switch (action) {
            case "accept" -> handleDecision(event, appealId, Decision.ACCEPT, "");
            case "deny" -> event.replyModal(Modal.create(DENY_FORM + appealId, EmbedFactory.truncate(discord.text("appeal-deny-modal-title"), 45))
                    .addComponents(ActionRow.of(TextInput.create("reason", EmbedFactory.truncate(discord.text("appeal-deny-modal-reason"), 45),
                            TextInputStyle.PARAGRAPH).setRequired(false).setMaxLength(500).build())).build()).queue();
            case "reduce" -> event.replyModal(Modal.create(REDUCE_FORM + appealId, EmbedFactory.truncate(discord.text("appeal-reduce-modal-title"), 45))
                    .addComponents(ActionRow.of(TextInput.create("duration", EmbedFactory.truncate(discord.text("appeal-reduce-modal-duration"), 45),
                            TextInputStyle.SHORT).setRequired(true).setMaxLength(20).setPlaceholder("3d").build())).build()).queue();
            default -> { }
        }
    }

    /**
     * The Minecraft name of a member who may decide on this appeal, or null. Needs a staff role,
     * a linked account and the in-game permission (Axentra.appeals, or Axentra.unban / Axentra.unmute
     * for the case type) in the snapshot saved on their last join.
     */
    private String allowedStaff(Member member, DatabaseManager.AppealInfo appeal) {
        if (!discord.isStaff(member)) return null;
        DatabaseManager.DiscordLink link = db().getLinkByDiscord(member.getId());
        if (link == null) return null;
        DatabaseManager.CaseInfo info = db().getCase(appeal.caseId);
        String specific = info != null && info.type.contains("MUTE") ? "Axentra.unmute" : "Axentra.unban";
        if (!link.hadPermission("Axentra.appeals") && !link.hadPermission(specific)) return null;
        return link.playerName != null ? link.playerName : member.getUser().getName();
    }

    private void handleDecision(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, int appealId,
                                Decision decision, String extra) {
        Member member = event.getMember();
        event.deferReply(true).queue();
        async(() -> {
            DatabaseManager.AppealInfo appeal = db().getAppeal(appealId);
            String staffName = appeal != null ? allowedStaff(member, appeal) : null;
            if (staffName == null) {
                event.getHook().sendMessage(discord.text("appeal-not-allowed")).queue();
                return;
            }
            String result = decide(appealId, decision, extra, staffName, "DISCORD:" + member.getId());
            event.getHook().sendMessage(result).queue();
        });
    }

    // ══════════════════════════════════════════════════════
    //  Decision (shared with /appeals in-game)
    // ══════════════════════════════════════════════════════

    /**
     * Applies a decision. Call off the main thread. Returns a message for the staff member.
     *
     * @param extra the deny reason or the new duration (e.g. 3d) for REDUCE
     */
    public String decide(int appealId, Decision decision, String extra, String staffName, String staffId) {
        DatabaseManager.AppealInfo appeal = db().getAppeal(appealId);
        if (appeal == null || !"OPEN".equals(appeal.status)) return discord.text("appeal-already-decided");
        DatabaseManager.CaseInfo info = db().getCase(appeal.caseId);
        if (info == null) return discord.text("appeal-not-found", "id", String.valueOf(appeal.caseId));
        boolean mute = info.type.contains("MUTE");
        String caseRef = "case #" + info.id;

        switch (decision) {
            case ACCEPT -> {
                if (!db().decideAppeal(appealId, "ACCEPTED", staffName, null)) return discord.text("appeal-already-decided");
                if (mute) db().unmutePlayer(info.playerName, "APPEALED", staffName);
                else db().unbanPlayer(info.playerName, "APPEALED", staffName);
                db().logAction(staffName, staffId, "APPEAL_ACCEPTED", info.playerName, caseRef);
                dm(appeal, discord.text("appeal-dm-accepted", "id", String.valueOf(info.id), "server", embeds.serverName()));
            }
            case DENY -> {
                if (!db().decideAppeal(appealId, "DENIED", staffName, extra)) return discord.text("appeal-already-decided");
                db().logAction(staffName, staffId, "APPEAL_DENIED", info.playerName,
                        caseRef + (extra != null && !extra.isBlank() ? ": " + extra : ""));
                dm(appeal, extra != null && !extra.isBlank()
                        ? discord.text("appeal-dm-denied-reason", "id", String.valueOf(info.id), "server", embeds.serverName(), "reason", extra)
                        : discord.text("appeal-dm-denied", "id", String.valueOf(info.id), "server", embeds.serverName()));
            }
            case REDUCE -> {
                long millis = FreezeManager.parseDuration(extra != null ? extra : "");
                if (millis <= 0) return discord.text("appeal-reduce-invalid");
                if (!db().decideAppeal(appealId, "REDUCED", staffName, extra)) return discord.text("appeal-already-decided");
                db().setCaseExpiry(info.id, new Timestamp(System.currentTimeMillis() + millis), staffName);
                db().logAction(staffName, staffId, "APPEAL_REDUCED", info.playerName, caseRef + " → " + extra);
                dm(appeal, discord.text("appeal-dm-reduced", "id", String.valueOf(info.id), "server", embeds.serverName(),
                        "duration", extra));
            }
        }
        updateStaffMessage(appealId);
        return discord.text("appeal-decided");
    }

    /** DMs the player who appealed. Ignored when their DMs are closed. */
    private void dm(DatabaseManager.AppealInfo appeal, String text) {
        JDA jda = discord.getJda();
        if (jda == null || !discord.config().getBoolean("appeals.dm-result", true)) return;
        jda.retrieveUserById(appeal.discordId)
                .flatMap(User::openPrivateChannel)
                .flatMap(channel -> channel.sendMessage(text))
                .queue(null, err -> { });
    }

    private static int parse(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return -1; }
    }
}
