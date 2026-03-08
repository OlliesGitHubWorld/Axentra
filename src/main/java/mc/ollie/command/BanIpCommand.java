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
import java.util.ArrayList;
import java.util.List;

public class BanIpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public BanIpCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.banip")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("banip-usage"));
            return true;
        }

        String input = args[0];
        String ip;

        // Try to resolve as a player name first
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
            // Treat input as a raw IP
            ip = input;
            displayName = "Unknown";
        }

        // Build reason
        String reason;
        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]);
                if (i < args.length - 1) sb.append(" ");
            }
            reason = sb.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("banip-default-reason", "Your IP has been banned!");
        }

        if (plugin.getDatabaseManager().isIpBanned(ip)) {
            sender.sendMessage(getMessage("banip-already-banned").replace("%ip%", ip));
            return true;
        }

        plugin.getDatabaseManager().banIp(ip, reason, sender.getName(), null);

        // Kick all online players with this IP
        final String bannedIp = ip;
        String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
        String rawKickMessage = plugin.getFileManager().getMessages().getString("banip-message");
        if (rawKickMessage == null) rawKickMessage = "&cYour IP has been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%";
        rawKickMessage = rawKickMessage.replace("%reason%", reason).replace("%banned-by%", sender.getName());
        if (appealLink.isEmpty()) {
            rawKickMessage = rawKickMessage.replace("\n&7Appeal: &f%appeal%", "");
        } else {
            rawKickMessage = rawKickMessage.replace("%appeal%", appealLink);
        }
        final String kickMessage = ChatColor.translateAlternateColorCodes('&', rawKickMessage);

        for (Player online : Bukkit.getOnlinePlayers()) {
            InetSocketAddress address = online.getAddress();
            if (address != null && address.getAddress().getHostAddress().equals(bannedIp)) {
                online.kickPlayer(kickMessage);
            }
        }

        sender.sendMessage(getMessage("banip-success").replace("%ip%", ip).replace("%reason%", reason));

        if (plugin.getFileManager().getSettings().getBoolean("announce-banip")) {
            plugin.getServer().broadcastMessage(getMessage("banip-announce")
                    .replace("%player%", displayName)
                    .replace("%reason%", reason));
        }

        return true;
    }
}
