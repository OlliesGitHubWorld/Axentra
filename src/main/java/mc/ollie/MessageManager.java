package mc.ollie;

import org.bukkit.ChatColor;

public class MessageManager {

    private final App plugin;

    public MessageManager(App plugin) {
        this.plugin = plugin;
    }

    public String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path, String player) {
        return getMessage(path).replace("%player%", player);
    }
}