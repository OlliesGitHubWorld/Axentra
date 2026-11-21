package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.FreezeManager;
import mc.ollie.PlayerLookup;
import mc.ollie.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WarnCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

    public WarnCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private long parseDuration(String input) {
        Matcher matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.matches()) return -1;
        long days    = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
        long hours   = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
        long minutes = matcher.group(3) != null ? Long.parseLong(matcher.group(3)) : 0;
        long seconds = matcher.group(4) != null ? Long.parseLong(matcher.group(4)) : 0;
        long millis = (days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000L;
        return millis > 0 ? millis : -1;
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days    = seconds / 86400; seconds %= 86400;
        long hours   = seconds / 3600;  seconds %= 3600;
        long minutes = seconds / 60;    seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.warn")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("warn-usage"));
            return true;
        }

        // Also finds players who already logged out
        PlayerLookup.Target target = PlayerLookup.resolve(plugin, args[0]);

        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        String reason;
        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]);
                if (i < args.length - 1) reasonBuilder.append(" ");
            }
            reason = reasonBuilder.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("warn-default-reason", "Breaking the rules!");
        }

        // Get chat snapshot
        String chatLog = null;
        ChatListener chatListener = plugin.getChatListener();
        if (chatListener != null) {
            chatLog = chatListener.getChatSnapshot(UUID.fromString(target.uuid()));
        }

        plugin.getDatabaseManager().warnPlayer(target.name(), target.uuid(), reason, sender.getName());

        int warnCount = plugin.getDatabaseManager().getWarnCount(target.uuid());

        // Create case
        String staffName = sender.getName();
        String staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
        int caseId = plugin.getDatabaseManager().createCase("WARN", target.name(),
                target.uuid(), reason, staffName, null, chatLog);

        // Audit log
        plugin.getDatabaseManager().logAction(staffName, staffUuid, "WARN", target.name(),
                reason + " (warn #" + warnCount + ")");

        if (target.online() != null) {
            target.online().sendMessage(getMessage("warn-message")
                    .replace("%reason%", reason)
                    .replace("%count%", String.valueOf(warnCount)));
        }

        sender.sendMessage(getMessage("warn-success")
                .replace("%player%", target.name())
                .replace("%reason%", reason)
                .replace("%count%", String.valueOf(warnCount))
                + ChatColor.DARK_GRAY + " (Case #" + caseId + ")");

        if (plugin.getFileManager().getSettings().getBoolean("announce-warn")) {
            String announceMsg = getMessage("warn-announce")
                    .replace("%player%", target.name())
                    .replace("%reason%", reason)
                    .replace("%count%", String.valueOf(warnCount));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("Axentra.notify.warn")) {
                    online.sendMessage(announceMsg);
                }
            }
        }

        // Check for punishment at this warn count
        ConfigurationSection entry = findPunishment(warnCount);
        if (entry != null) {
            applyPunishment(target, entry, warnCount, sender);
        }

        return true;
    }

    /**
     * Finds the warn-punishments entry for a warn count. An exact key ("5") wins;
     * otherwise the first matching range ("1-10") or open range ("10+") is used.
     */
    private ConfigurationSection findPunishment(int warnCount) {
        ConfigurationSection punishments = plugin.getFileManager().getSettings().getConfigurationSection("warn-punishments");
        if (punishments == null) return null;

        ConfigurationSection exact = punishments.getConfigurationSection(String.valueOf(warnCount));
        if (exact != null) return exact;

        for (String key : punishments.getKeys(false)) {
            String k = key.trim();
            try {
                if (k.endsWith("+")) {
                    if (warnCount >= Integer.parseInt(k.substring(0, k.length() - 1).trim())) {
                        return punishments.getConfigurationSection(key);
                    }
                } else if (k.contains("-")) {
                    String[] parts = k.split("-", 2);
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    if (warnCount >= min && warnCount <= max) {
                        return punishments.getConfigurationSection(key);
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid warn-punishments key '" + key + "' (use 5, 1-10 or 10+)");
            }
        }
        return null;
    }

    private void broadcastToStaff(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("Axentra.notify.warn")) {
                online.sendMessage(message);
            }
        }
    }

    private void applyPunishment(PlayerLookup.Target target, ConfigurationSection entry, int warnCount, CommandSender issuer) {
        String action = entry.getString("action", "").toLowerCase();
        String punishReason = entry.getString("reason", "Reached " + warnCount + " warnings");
        String staffName = issuer.getName();
        String staffUuid = issuer instanceof Player ? ((Player) issuer).getUniqueId().toString() : "CONSOLE";
        Player online = target.online();

        switch (action) {
            case "kick": {
                // Nothing to kick when the player already logged out
                if (online == null) break;
                String kickMsg = getMessage("warn-punishment-kick-message").replace("%reason%", punishReason);
                online.kickPlayer(kickMsg);

                plugin.getDatabaseManager().createCase("KICK", target.name(),
                        target.uuid(), punishReason, staffName, null, null);
                plugin.getDatabaseManager().logAction(staffName, staffUuid, "KICK", target.name(),
                        punishReason + " (auto: " + warnCount + " warns)");

                broadcastToStaff(getMessage("warn-punishment-announce")
                        .replace("%player%", target.name())
                        .replace("%action%", "kicked")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            case "mute": {
                if (!plugin.getDatabaseManager().isMuted(target.uuid())) {
                    plugin.getDatabaseManager().mutePlayer(target.name(), target.uuid(), punishReason, issuer.getName(), null);
                    if (online != null) online.sendMessage(getMessage("mute-message").replace("%reason%", punishReason));

                    plugin.getDatabaseManager().createCase("MUTE", target.name(),
                            target.uuid(), punishReason, staffName, null, null);
                    plugin.getDatabaseManager().logAction(staffName, staffUuid, "MUTE", target.name(),
                            punishReason + " (auto: " + warnCount + " warns)");
                }
                broadcastToStaff(getMessage("warn-punishment-announce")
                        .replace("%player%", target.name())
                        .replace("%action%", "muted")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            case "tempban": {
                String durationStr = entry.getString("duration", "1d");
                long durationMillis = parseDuration(durationStr);
                if (durationMillis == -1) durationMillis = 86400000L; // fallback 1d
                String formattedDuration = formatDuration(durationMillis);
                Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + durationMillis);

                plugin.getDatabaseManager().banPlayer(target.name(), target.uuid(), punishReason, issuer.getName(), expiresAt);

                plugin.getDatabaseManager().createCase("TEMPBAN", target.name(),
                        target.uuid(), punishReason, staffName, expiresAt, null);
                plugin.getDatabaseManager().logAction(staffName, staffUuid, "TEMPBAN", target.name(),
                        punishReason + " (" + formattedDuration + ", auto: " + warnCount + " warns)");

                String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
                String rawBanMsg = plugin.getFileManager().getMessages().getString("tempban-message",
                        "&cYou have been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%\n&7Appeal: &f%appeal%");
                rawBanMsg = rawBanMsg.replace("%reason%", punishReason)
                        .replace("%duration%", formattedDuration)
                        .replace("%banned-by%", issuer.getName());
                if (appealLink.isEmpty()) {
                    rawBanMsg = rawBanMsg.replace("\n&7Appeal: &f%appeal%", "");
                } else {
                    rawBanMsg = rawBanMsg.replace("%appeal%", appealLink);
                }
                if (online != null) online.kickPlayer(ChatColor.translateAlternateColorCodes('&', rawBanMsg));

                broadcastToStaff(getMessage("warn-punishment-announce")
                        .replace("%player%", target.name())
                        .replace("%action%", "temp-banned (" + formattedDuration + ")")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            case "ban": {
                plugin.getDatabaseManager().banPlayer(target.name(), target.uuid(), punishReason, issuer.getName(), null);

                plugin.getDatabaseManager().createCase("BAN", target.name(),
                        target.uuid(), punishReason, staffName, null, null);
                plugin.getDatabaseManager().logAction(staffName, staffUuid, "BAN", target.name(),
                        punishReason + " (auto: " + warnCount + " warns)");

                String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
                String rawBanMsg = plugin.getFileManager().getMessages().getString("ban-message",
                        "&cYou have been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%\n&7Appeal: &f%appeal%");
                rawBanMsg = rawBanMsg.replace("%reason%", punishReason)
                        .replace("%banned-by%", issuer.getName());
                if (appealLink.isEmpty()) {
                    rawBanMsg = rawBanMsg.replace("\n&7Appeal: &f%appeal%", "");
                } else {
                    rawBanMsg = rawBanMsg.replace("%appeal%", appealLink);
                }
                if (online != null) online.kickPlayer(ChatColor.translateAlternateColorCodes('&', rawBanMsg));

                broadcastToStaff(getMessage("warn-punishment-announce")
                        .replace("%player%", target.name())
                        .replace("%action%", "banned")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            case "freeze":
            case "tempfreeze": {
                FreezeManager freezeManager = plugin.getFreezeManager();
                if (freezeManager == null) {
                    plugin.getLogger().warning("Warn punishment '" + action + "' needs the freeze module enabled");
                    break;
                }
                // Freezing only works on a player who is online
                if (online == null || online.hasPermission("Axentra.freeze.exempt")) break;

                long durationMillis = 0L;
                String durationStr = entry.getString("duration", "");
                if (!durationStr.isEmpty()) {
                    durationMillis = FreezeManager.parseDuration(durationStr);
                    if (durationMillis == -1) durationMillis = 0L;
                }
                freezeManager.freeze(online, issuer, durationMillis);
                online.sendMessage(getMessage("warn-punishment-freeze-message").replace("%reason%", punishReason));

                broadcastToStaff(getMessage("warn-punishment-announce")
                        .replace("%player%", target.name())
                        .replace("%action%", durationMillis > 0
                                ? "frozen (" + FreezeManager.formatDuration(durationMillis) + ")" : "frozen")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            default:
                plugin.getLogger().warning("Unknown warn punishment action '" + action + "' for warn count " + warnCount);
        }
    }
}
