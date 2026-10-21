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

public class RealnameCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public RealnameCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.realname")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("realname-usage"));
            return true;
        }

        String searchNick = args[0].toLowerCase();
        String searchStripped = ChatColor.stripColor(
                ChatColor.translateAlternateColorCodes('&', searchNick));

        for (Player player : Bukkit.getOnlinePlayers()) {
            String displayStripped = ChatColor.stripColor(player.getDisplayName()).toLowerCase();
            if (displayStripped.contains(searchStripped)) {
                sender.sendMessage(getMessage("realname-found")
                        .replace("%nick%", player.getDisplayName())
                        .replace("%player%", player.getName()));
                return true;
            }
        }

        sender.sendMessage(getMessage("realname-not-found").replace("%nick%", args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
