package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public UnbanCommand(App plugin) {
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
        if (args.length == 1) {
            return plugin.getDatabaseManager().getActiveBannedNames();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.unban")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("unban-usage"));
            return true;
        }

        String playerName = args[0];

        if (!plugin.getDatabaseManager().isBannedByName(playerName)) {
            sender.sendMessage(getMessage("unban-not-banned").replace("%player%", playerName));
            return true;
        }

        plugin.getDatabaseManager().unbanPlayer(playerName);
        sender.sendMessage(getMessage("unban-success").replace("%player%", playerName));

        if (plugin.getFileManager().getSettings().getBoolean("announce-unban")) {
            String announceMsg = getMessage("unban-announce")
                    .replace("%player%", playerName);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("Axentra.notify.ban")) {
                    online.sendMessage(announceMsg);
                }
            }
        }

        return true;
    }
}