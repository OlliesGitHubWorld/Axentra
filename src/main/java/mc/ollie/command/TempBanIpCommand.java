package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempBanIpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

    public TempBanIpCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.tempbanip")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("tempbanip-usage"));
            return true;
        }

        String input = args[0];
        String durationStr = args[1];

        long durationMillis = parseDuration(durationStr);
        if (durationMillis == -1) {
            sender.sendMessage(getMessage("tempban-invalid-duration"));
            return true;
        }

        // Resolve player name to IP if needed
        String ip;
        String displayName;
        Player target = Bukkit.getPlayer(input);
        if (target != null) {
            InetSocketAddress address = target.getAddress();
            if (address == null) {
                sender.sendMessage(getMessage("banip-no-address").replace("%player%", input));
                return true;
            }
            ip = address.getAddress().getHostAddress();
            displayName = target.getName();
        } else {
            ip = input;
            displayName = "Unknown";
        }

        // Build reason
        String reason;
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]);
                if (i < args.length - 1) sb.append(" ");
            }
            reason = sb.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("tempbanip-default-reason", "Your IP has been temporarily banned!");
        }

        if (plugin.getDatabaseManager().isIpBanned(ip)) {
            sender.sendMessage(getMessage("banip-already-banned").replace("%ip%", ip));
            return true;
        }

        String formattedDuration = formatDuration(durationMillis);
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + durationMillis);

        plugin.getDatabaseManager().banIp(ip, reason, sender.getName(), expiresAt);

        // Kick all online players with this IP
        String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
        String rawKickMessage = plugin.getFileManager().getMessages().getString("tempbanip-message");
        if (rawKickMessage == null) rawKickMessage = "&cYour IP has been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%";
        rawKickMessage = rawKickMessage
                .replace("%reason%", reason)
                .replace("%duration%", formattedDuration)
                .replace("%banned-by%", sender.getName());
        if (appealLink.isEmpty()) {
            rawKickMessage = rawKickMessage.replace("\n&7Appeal: &f%appeal%", "");
        } else {
            rawKickMessage = rawKickMessage.replace("%appeal%", appealLink);
        }
        final String kickMessage = ChatColor.translateAlternateColorCodes('&', rawKickMessage);
        final String bannedIp = ip;

        for (Player online : Bukkit.getOnlinePlayers()) {
            InetSocketAddress address = online.getAddress();
            if (address != null && address.getAddress().getHostAddress().equals(bannedIp)) {
                online.kickPlayer(kickMessage);
            }
        }

        sender.sendMessage(getMessage("tempbanip-success")
                .replace("%ip%", ip)
                .replace("%duration%", formattedDuration)
                .replace("%reason%", reason));

        if (plugin.getFileManager().getSettings().getBoolean("announce-banip")) {
            plugin.getServer().broadcastMessage(getMessage("tempbanip-announce")
                    .replace("%player%", displayName)
                    .replace("%duration%", formattedDuration)
                    .replace("%reason%", reason));
        }

        return true;
    }
}
