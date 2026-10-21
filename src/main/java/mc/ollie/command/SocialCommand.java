package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class SocialCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public SocialCommand(App plugin) {
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
        String commandName = label.toLowerCase();

        String link = plugin.getFileManager().getSettings().getString("social-links." + commandName, "");
        if (link.isEmpty()) {
            sender.sendMessage(getMessage("social-not-configured").replace("%command%", commandName));
            return true;
        }

        String messageKey = "social-" + commandName;
        String message = plugin.getFileManager().getMessages().getString(messageKey);

        if (message == null) {
            // Fallback format
            message = "%prefix%&b" + commandName.substring(0, 1).toUpperCase() + commandName.substring(1) + ": &f%link%";
        }

        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        message = message.replace("%prefix%", prefix).replace("%link%", link);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
