package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempBanCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

    public TempBanCommand(App plugin) {
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
        } else if (args.length == 2) {
            completions.add("1h");
            completions.add("1d");
            completions.add("7d");
            completions.add("30d");
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.tempban")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("tempban-usage"));
            return true;
        }

        String playerName = args[0];
        String durationStr = args[1];

        long durationMillis = parseDuration(durationStr);
        if (durationMillis == -1) {
            sender.sendMessage(getMessage("tempban-invalid-duration"));
            return true;
        }

        // Build reason
        String reason;
        if (args.length > 2) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                reasonBuilder.append(args[i]);
                if (i < args.length - 1) reasonBuilder.append(" ");
            }
            reason = reasonBuilder.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("tempban-default-reason", "You have been temporarily banned!");
        }

        // Check if already banned
        if (plugin.getDatabaseManager().isBannedByName(playerName)) {
            sender.sendMessage(getMessage("ban-already-banned").replace("%player%", playerName));
            return true;
        }

        Player target = Bukkit.getPlayer(playerName);
        String uuid = target != null ? target.getUniqueId().toString()
                : Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
        String bannedBy = sender.getName();
        String formattedDuration = formatDuration(durationMillis);
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + durationMillis);

        // Get chat snapshot
        String chatLog = null;
        if (target != null) {
            ChatListener chatListener = plugin.getChatListener();
            if (chatListener != null) {
                chatLog = chatListener.getChatSnapshot(target.getUniqueId());
            }
        }

        plugin.getDatabaseManager().banPlayer(playerName, uuid, reason, bannedBy, expiresAt);

        // Create case
        String staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
        int caseId = plugin.getDatabaseManager().createCase("TEMPBAN", playerName, uuid, reason, bannedBy, expiresAt, chatLog);

        // Audit log
        plugin.getDatabaseManager().logAction(bannedBy, staffUuid, "TEMPBAN", playerName, reason + " (" + formattedDuration + ")");

        if (target != null) {
            String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
            String rawBanMessage = plugin.getFileManager().getMessages().getString("tempban-message");
            if (rawBanMessage == null) rawBanMessage = "&cYou have been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%";

            rawBanMessage = rawBanMessage.replace("%reason%", reason);
            rawBanMessage = rawBanMessage.replace("%duration%", formattedDuration);
            rawBanMessage = rawBanMessage.replace("%banned-by%", bannedBy);

            if (appealLink.isEmpty()) {
                rawBanMessage = rawBanMessage.replace("\n&7Appeal: &f%appeal%", "");
            } else {
                rawBanMessage = rawBanMessage.replace("%appeal%", appealLink);
            }

            target.kickPlayer(ChatColor.translateAlternateColorCodes('&', rawBanMessage));
        }

        sender.sendMessage(getMessage("tempban-success")
                .replace("%player%", playerName)
                .replace("%duration%", formattedDuration)
                .replace("%reason%", reason)
                + ChatColor.DARK_GRAY + " (Case #" + caseId + ")");

        if (plugin.getFileManager().getSettings().getBoolean("announce-ban")) {
            plugin.getServer().broadcastMessage(getMessage("tempban-announce")
                    .replace("%player%", playerName)
                    .replace("%duration%", formattedDuration)
                    .replace("%reason%", reason));
        }

        return true;
    }
}
