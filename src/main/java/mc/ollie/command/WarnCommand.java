package mc.ollie.command;

import mc.ollie.App;
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

        Player target = Bukkit.getPlayer(args[0]);

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

        plugin.getDatabaseManager().warnPlayer(target.getName(), target.getUniqueId().toString(), reason, sender.getName());

        int warnCount = plugin.getDatabaseManager().getWarnCount(target.getUniqueId().toString());

        target.sendMessage(getMessage("warn-message")
                .replace("%reason%", reason)
                .replace("%count%", String.valueOf(warnCount)));

        sender.sendMessage(getMessage("warn-success")
                .replace("%player%", target.getName())
                .replace("%reason%", reason)
                .replace("%count%", String.valueOf(warnCount)));

        if (plugin.getFileManager().getSettings().getBoolean("announce-warn")) {
            plugin.getServer().broadcastMessage(getMessage("warn-announce")
                    .replace("%player%", target.getName())
                    .replace("%reason%", reason)
                    .replace("%count%", String.valueOf(warnCount)));
        }

        // Check for punishment at this warn count
        ConfigurationSection punishments = plugin.getFileManager().getSettings().getConfigurationSection("warn-punishments");
        if (punishments != null && punishments.contains(String.valueOf(warnCount))) {
            ConfigurationSection entry = punishments.getConfigurationSection(String.valueOf(warnCount));
            if (entry != null) {
                applyPunishment(target, entry, warnCount, sender);
            }
        }

        return true;
    }

    private void applyPunishment(Player target, ConfigurationSection entry, int warnCount, CommandSender issuer) {
        String action = entry.getString("action", "").toLowerCase();
        String punishReason = entry.getString("reason", "Reached " + warnCount + " warnings");

        switch (action) {
            case "kick": {
                String kickMsg = getMessage("warn-punishment-kick-message").replace("%reason%", punishReason);
                target.kickPlayer(kickMsg);
                plugin.getServer().broadcastMessage(getMessage("warn-punishment-announce")
                        .replace("%player%", target.getName())
                        .replace("%action%", "kicked")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            case "mute": {
                if (!plugin.getDatabaseManager().isMuted(target.getUniqueId().toString())) {
                    plugin.getDatabaseManager().mutePlayer(target.getName(), target.getUniqueId().toString(), punishReason, issuer.getName());
                    target.sendMessage(getMessage("mute-message").replace("%reason%", punishReason));
                }
                plugin.getServer().broadcastMessage(getMessage("warn-punishment-announce")
                        .replace("%player%", target.getName())
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

                plugin.getDatabaseManager().banPlayer(target.getName(), target.getUniqueId().toString(), punishReason, issuer.getName(), expiresAt);

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
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&', rawBanMsg));

                plugin.getServer().broadcastMessage(getMessage("warn-punishment-announce")
                        .replace("%player%", target.getName())
                        .replace("%action%", "temp-banned (" + formattedDuration + ")")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            case "ban": {
                plugin.getDatabaseManager().banPlayer(target.getName(), target.getUniqueId().toString(), punishReason, issuer.getName(), null);

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
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&', rawBanMsg));

                plugin.getServer().broadcastMessage(getMessage("warn-punishment-announce")
                        .replace("%player%", target.getName())
                        .replace("%action%", "banned")
                        .replace("%count%", String.valueOf(warnCount)));
                break;
            }
            default:
                plugin.getLogger().warning("Unknown warn punishment action '" + action + "' for warn count " + warnCount);
        }
    }
}