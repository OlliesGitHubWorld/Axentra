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

public class StaffchatCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public StaffchatCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.staffchat")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            // Toggle staffchat mode
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            boolean toggled = plugin.toggleStaffChat(player.getUniqueId());
            if (toggled) {
                player.sendMessage(getMessage("staffchat-enabled"));
            } else {
                player.sendMessage(getMessage("staffchat-disabled"));
            }
            return true;
        }

        // Send a single message to staff chat
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) messageBuilder.append(" ");
        }

        String senderName = sender instanceof Player ? sender.getName() : "Console";
        sendStaffMessage(senderName, messageBuilder.toString());
        return true;
    }

    public void sendStaffMessage(String senderName, String message) {
        String format = plugin.getFileManager().getMessages().getString("staffchat-format",
                "&8[&c&lStaff&8] &e%player% &8» &f%message%");
        String formatted = ChatColor.translateAlternateColorCodes('&',
                format.replace("%player%", senderName).replace("%message%", message));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("Axentra.staffchat")) {
                online.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
