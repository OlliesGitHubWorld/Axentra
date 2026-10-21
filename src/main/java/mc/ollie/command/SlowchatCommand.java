package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SlowchatCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public SlowchatCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.slowchat")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            // Toggle off if active, or use default
            if (plugin.getSlowChatSeconds() > 0) {
                plugin.setSlowChatSeconds(0);
                Bukkit.broadcastMessage(getMessage("slowchat-disabled"));
            } else {
                int defaultSeconds = plugin.getFileManager().getSettings().getInt("slowchat-default-seconds", 5);
                plugin.setSlowChatSeconds(defaultSeconds);
                Bukkit.broadcastMessage(getMessage("slowchat-enabled").replace("%seconds%", String.valueOf(defaultSeconds)));
            }
            return true;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("off") || arg.equals("0")) {
            plugin.setSlowChatSeconds(0);
            Bukkit.broadcastMessage(getMessage("slowchat-disabled"));
            return true;
        }

        try {
            int seconds = Integer.parseInt(arg);
            if (seconds < 1 || seconds > 300) {
                sender.sendMessage(getMessage("slowchat-invalid"));
                return true;
            }
            plugin.setSlowChatSeconds(seconds);
            Bukkit.broadcastMessage(getMessage("slowchat-enabled").replace("%seconds%", String.valueOf(seconds)));
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("slowchat-usage"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("3", "5", "10", "30", "60", "off");
        }
        return Collections.emptyList();
    }
}
