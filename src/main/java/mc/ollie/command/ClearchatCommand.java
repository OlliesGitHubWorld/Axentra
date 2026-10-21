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

public class ClearchatCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public ClearchatCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.clearchat")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("Axentra.clearchat.bypass")) {
                for (int i = 0; i < 100; i++) {
                    player.sendMessage("");
                }
            }
        }

        String senderName = sender instanceof Player ? sender.getName() : "Console";
        Bukkit.broadcastMessage(getMessage("clearchat-cleared").replace("%player%", senderName));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
