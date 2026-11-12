package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.punish.PunishmentService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Punish from Discord: a menu with the punishments.yml templates under report messages and the
 * /lookup reply. Picking a template asks for confirmation, then the punishment is applied in-game
 * through the normal system, with the linked staff member's Minecraft name and source DISCORD.
 *
 * <p>Allowed for members with a staff role whose linked account had Axentra.punish or the
 * action's permission (e.g. Axentra.tempban) on their last join. This snapshot is used so staff
 * don't have to be online; it updates every time they join.
 */
public class DiscordModeration extends ListenerAdapter {

    private static final String MENU = "axentra:punish:";
    private static final String CONFIRM = "axentra:punishok:";
    private static final String CANCEL = "axentra:punishno";

    private final App plugin;
    private final DiscordManager discord;

    public DiscordModeration(App plugin, DiscordManager discord, ChannelRelay relay, DiscordCommands commands) {
        this.plugin = plugin;
        this.discord = discord;
        discord.addListener(this);
        relay.setReportExtraRows(report -> menu(report.targetUuid, report.id));
        commands.setLookupRows(profile -> menu(profile.uuid, 0));
    }

    /**
     * The template menu for a player, grouped by category like the /punish GUI. Each option shows
     * the step that would apply now. Reads the database: call off the main thread.
     */
    public List<ActionRow> menu(String targetUuid, int reportId) {
        ConfigurationSection templates = plugin.getPunishmentService().templates();
        if (templates == null || targetUuid == null) return List.of();
        Map<String, Integer> offenses = plugin.getDatabaseManager().getCaseCountsByReason(targetUuid);

        Map<String, List<String>> byCategory = new LinkedHashMap<>();
        for (String cat : List.of("WARNINGS", "MUTES & KICKS", "BANS")) byCategory.put(cat, new ArrayList<>());
        for (String key : templates.getKeys(false)) {
            ConfigurationSection tmpl = templates.getConfigurationSection(key);
            if (tmpl == null) continue;
            byCategory.get(PunishmentService.category(PunishmentService.getSteps(tmpl).get(0).action)).add(key);
        }

        StringSelectMenu.Builder builder = StringSelectMenu.create(MENU + targetUuid + ":" + reportId)
                .setPlaceholder(EmbedFactory.truncate(discord.text("punish-placeholder", "player",
                        Optional.ofNullable(plugin.getDatabaseManager().getLastKnownName(targetUuid)).orElse("?")), 150));
        for (Map.Entry<String, List<String>> category : byCategory.entrySet()) {
            for (String key : category.getValue()) {
                if (builder.getOptions().size() >= 25) break; // Discord limit
                ConfigurationSection tmpl = templates.getConfigurationSection(key);
                List<PunishmentService.Step> steps = PunishmentService.getSteps(tmpl);
                String reason = tmpl.getString("reason", "Punishment applied");
                PunishmentService.Step step = steps.get(PunishmentService.stepIndex(steps, offenses, reason));
                String label = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', tmpl.getString("display-name", key)));
                builder.addOption(EmbedFactory.truncate(category.getKey() + " · " + label, 100), key,
                        EmbedFactory.truncate(step.label() + " — " + reason, 100));
            }
        }
        if (builder.getOptions().isEmpty()) return List.of();
        return List.of(ActionRow.of(builder.build()));
    }

    /**
     * The Minecraft name of a member allowed to apply this action, or null. Reads the database:
     * call off the main thread.
     */
    private String allowedStaff(Member member, String action) {
        if (!discord.isStaff(member)) return null;
        DatabaseManager.DiscordLink link = plugin.getDatabaseManager().getLinkByDiscord(member.getId());
        if (link == null) return null;
        if (!link.hadPermission("Axentra.punish") && !link.hadPermission(PunishmentService.permissionFor(action))) return null;
        return link.playerName != null ? link.playerName : member.getUser().getName();
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith(MENU)) return;
        String[] ids = event.getComponentId().substring(MENU.length()).split(":");
        if (ids.length != 2) return;
        String targetUuid = ids[0];
        String reportId = ids[1];
        String key = event.getValues().isEmpty() ? null : event.getValues().get(0);
        if (key == null) return;
        Member member = event.getMember();

        event.deferReply(true).queue();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ConfigurationSection templates = plugin.getPunishmentService().templates();
            ConfigurationSection tmpl = templates != null ? templates.getConfigurationSection(key) : null;
            String name = plugin.getDatabaseManager().getLastKnownName(targetUuid);
            if (tmpl == null || name == null) {
                event.getHook().sendMessage(discord.text("punish-failed", "reason", "unknown template or player")).queue();
                return;
            }
            PunishmentService.Step step = plugin.getPunishmentService().currentStep(tmpl, targetUuid);
            if (allowedStaff(member, step.action) == null) {
                event.getHook().sendMessage(discord.text("punish-not-allowed")).queue();
                return;
            }
            String label = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', tmpl.getString("display-name", key)));
            event.getHook().sendMessage(discord.text("punish-confirm", "template", label + " (" + step.label() + ")",
                            "player", EmbedFactory.escape(name)))
                    .setComponents(ActionRow.of(
                            Button.danger(EmbedFactory.truncate(CONFIRM + targetUuid + ":" + reportId + ":" + key, 100), discord.text("punish-button-confirm")),
                            Button.secondary(CANCEL, discord.text("punish-button-cancel"))))
                    .queue();
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals(CANCEL)) {
            event.editMessage(discord.text("punish-cancelled")).setComponents().queue();
            return;
        }
        if (!id.startsWith(CONFIRM)) return;
        String[] parts = id.substring(CONFIRM.length()).split(":", 3);
        if (parts.length != 3) return;
        String targetUuid = parts[0];
        int reportId;
        try { reportId = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { return; }
        String key = parts[2];
        Member member = event.getMember();

        event.deferEdit().queue();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ConfigurationSection templates = plugin.getPunishmentService().templates();
            ConfigurationSection tmpl = templates != null ? templates.getConfigurationSection(key) : null;
            String name = plugin.getDatabaseManager().getLastKnownName(targetUuid);
            if (tmpl == null || name == null) {
                event.getHook().editOriginal(discord.text("punish-failed", "reason", "unknown template or player")).setComponents().queue();
                return;
            }
            // Recount right before applying, so two staff punishing at once both escalate correctly
            PunishmentService.Step step = plugin.getPunishmentService().currentStep(tmpl, targetUuid);
            String staffName = allowedStaff(member, step.action);
            if (staffName == null) {
                event.getHook().editOriginal(discord.text("punish-not-allowed")).setComponents().queue();
                return;
            }
            String reason = tmpl.getString("reason", "Punishment applied");
            String staffId = "DISCORD:" + member.getId();
            plugin.getDatabaseManager().logAction(staffName, staffId, "DISCORD_PUNISH", name, key + " → " + step.label());

            Bukkit.getScheduler().runTask(plugin, () -> plugin.getPunishmentService().apply(name, targetUuid,
                    step.action, reason, step.duration, staffName, staffId, "DISCORD",
                    result -> {
                        event.getHook().editOriginal(result.caseId() > 0
                                ? discord.text("punish-done", "type", result.caseType(), "player", EmbedFactory.escape(name),
                                        "id", String.valueOf(result.caseId()))
                                : discord.text("punish-failed", "reason", "database error")).setComponents().queue();
                        // Punished from a report: close it and link the case
                        if (reportId > 0 && result.caseId() > 0) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                                    plugin.getDatabaseManager().closeReport(reportId, staffName, result.caseId()));
                        }
                    }));
        });
    }
}
