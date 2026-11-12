package mc.ollie.punish;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.FreezeManager;
import mc.ollie.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;

/**
 * Applies punishments from punishments.yml templates, for the /punish GUI and for Discord
 * (v1.9.0). The database work runs off the main thread; kicks and messages run on it.
 */
public class PunishmentService {

    /** One step of a template: an action with an optional duration, e.g. "tempmute 1h". */
    public static class Step {
        public final String action;
        public final String duration;

        public Step(String action, String duration) {
            this.action = action;
            this.duration = duration;
        }

        public String label() {
            return action.toUpperCase(Locale.ROOT) + (duration.isEmpty() ? "" : " " + duration);
        }
    }

    /** The outcome of a punishment: the case type (BAN, MUTE, ...) and ID (-1 on failure). */
    public record Result(String caseType, int caseId) { }

    private final App plugin;

    public PunishmentService(App plugin) {
        this.plugin = plugin;
    }

    public ConfigurationSection templates() {
        return plugin.getFileManager().getPunishments() != null
                ? plugin.getFileManager().getPunishments().getConfigurationSection("templates") : null;
    }

    /**
     * The template's escalation ladder ("escalation" list), or its single action/duration
     * when it has none.
     */
    public static List<Step> getSteps(ConfigurationSection tmpl) {
        List<Step> steps = new ArrayList<>();
        for (String entry : tmpl.getStringList("escalation")) {
            String[] parts = entry.trim().split("\\s+", 2);
            if (parts[0].isEmpty()) continue;
            steps.add(new Step(parts[0].toLowerCase(Locale.ROOT), parts.length > 1 ? parts[1].trim() : ""));
        }
        if (steps.isEmpty()) {
            steps.add(new Step(tmpl.getString("action", "warn").toLowerCase(Locale.ROOT), tmpl.getString("duration", "")));
        }
        return steps;
    }

    /** Picks the step for the next offense: earlier cases with the same reason, capped at the last step. */
    public static int stepIndex(List<Step> steps, Map<String, Integer> offenses, String reason) {
        return Math.min(offenses.getOrDefault(reason, 0), steps.size() - 1);
    }

    /** The step a template applies to this player now. Reads the database: call off the main thread. */
    public Step currentStep(ConfigurationSection tmpl, String targetUuid) {
        List<Step> steps = getSteps(tmpl);
        String reason = tmpl.getString("reason", "Punishment applied");
        return steps.get(stepIndex(steps, plugin.getDatabaseManager().getCaseCountsByReason(targetUuid), reason));
    }

    /** Category used to group templates, matching the rows of the /punish GUI. */
    public static String category(String action) {
        return switch (action) {
            case "mute", "tempmute", "kick" -> "MUTES & KICKS";
            case "ban", "tempban" -> "BANS";
            default -> "WARNINGS";
        };
    }

    /** The in-game permission for an action, e.g. Axentra.tempban. */
    public static String permissionFor(String action) {
        return "Axentra." + action.toLowerCase(Locale.ROOT);
    }

    /**
     * Applies a punishment. Call on the main thread; the callback also runs on the main thread.
     *
     * @param targetUuid the UUID, or null to look it up by name
     * @param source     INGAME or DISCORD (stored on the case)
     */
    public void apply(String targetName, String targetUuid, String action, String reason, String durationStr,
                      String staffName, String staffUuid, String source, Consumer<Result> callback) {
        Player target = Bukkit.getPlayerExact(targetName);
        String uuid = targetUuid != null ? targetUuid
                : target != null ? target.getUniqueId().toString()
                : Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();

        String chatLog = null;
        ChatListener chatListener = plugin.getChatListener();
        if (target != null && chatListener != null) chatLog = chatListener.getChatSnapshot(target.getUniqueId());

        long millis = durationStr.isEmpty() ? 0 : FreezeManager.parseDuration(durationStr);
        Timestamp expiresAt = millis > 0 ? new Timestamp(System.currentTimeMillis() + millis) : null;
        String caseType = switch (action) {
            case "ban" -> "BAN";
            case "tempban" -> "TEMPBAN";
            case "mute" -> "MUTE";
            case "tempmute" -> "TEMPMUTE";
            case "kick" -> "KICK";
            default -> "WARN";
        };
        String finalChatLog = chatLog;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager db = plugin.getDatabaseManager();
            switch (caseType) {
                case "BAN", "TEMPBAN" -> db.banPlayer(targetName, uuid, reason, staffName, expiresAt);
                case "MUTE", "TEMPMUTE" -> db.mutePlayer(targetName, uuid, reason, staffName, expiresAt);
                case "WARN" -> db.warnPlayer(targetName, uuid, reason, staffName);
                default -> { }
            }
            int warnCount = caseType.equals("WARN") ? db.getWarnCount(uuid) : 0;
            int caseId = db.createCase(caseType, targetName, uuid, reason, staffName, expiresAt, finalChatLog, source);
            db.logAction(staffName, staffUuid, caseType, targetName,
                    reason + (durationStr.isEmpty() ? "" : " (" + durationStr + ")"));

            Bukkit.getScheduler().runTask(plugin, () -> {
                notifyTarget(targetName, caseType, reason, durationStr, staffName, caseId, warnCount);
                if (callback != null) callback.accept(new Result(caseType, caseId));
            });
        });
    }

    /** Kicks or messages the target if they're online. Main thread. */
    private void notifyTarget(String targetName, String caseType, String reason, String durationStr,
                              String staffName, int caseId, int warnCount) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return;
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");

        switch (caseType) {
            case "BAN", "TEMPBAN" -> {
                boolean temp = caseType.equals("TEMPBAN");
                String msg = plugin.getFileManager().getMessages().getString(temp ? "tempban-message" : "ban-message",
                        temp ? "&cYou have been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%\n&7Appeal: &f%appeal%"
                             : "&cYou have been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%\n&7Appeal: &f%appeal%");
                msg = msg.replace("%reason%", reason).replace("%duration%", durationStr).replace("%banned-by%", staffName);
                msg = appealLink.isEmpty() ? msg.replace("\n&7Appeal: &f%appeal%", "") : msg.replace("%appeal%", appealLink);
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&', msg) + plugin.appealHint(caseId));
            }
            case "MUTE", "TEMPMUTE" -> {
                boolean temp = caseType.equals("TEMPMUTE");
                String msg = plugin.getFileManager().getMessages().getString(temp ? "tempmute-message" : "mute-message",
                        temp ? "%prefix%&cYou have been temporarily muted!\n&7Reason: &f%reason%\n&7Duration: &f%duration%"
                             : "%prefix%&cYou have been muted!\n&7Reason: &f%reason%");
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("%prefix%", prefix)
                        .replace("%reason%", reason).replace("%duration%", durationStr)));
            }
            case "KICK" -> {
                String msg = plugin.getFileManager().getMessages().getString("kick-message",
                        "&cYou have been kicked!\n&7Reason: &f%reason%");
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&', msg.replace("%reason%", reason)));
            }
            default -> {
                String msg = plugin.getFileManager().getMessages().getString("warn-message",
                        "%prefix%&eYou have been warned! &7Reason: &f%reason% &8(&e%count% total&8)");
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("%prefix%", prefix)
                        .replace("%reason%", reason).replace("%count%", String.valueOf(warnCount))));
            }
        }
    }
}
