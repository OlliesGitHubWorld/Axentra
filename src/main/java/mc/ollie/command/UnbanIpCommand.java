package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnbanIpCommand implements CommandExecutor {

    private final App plugin;

    public UnbanIpCommand(App plugin) {
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.unbanip")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("unbanip-usage"));
            return true;
        }

        String ip = args[0];

        if (!plugin.getDatabaseManager().isIpBanned(ip)) {
            sender.sendMessage(getMessage("unbanip-not-banned").replace("%ip%", ip));
            return true;
        }

        plugin.getDatabaseManager().unbanIp(ip);
        sender.sendMessage(getMessage("unbanip-success").replace("%ip%", ip));
        return true;
    }
}