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

public class MeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public MeCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.me")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("me-usage"));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (plugin.getDatabaseManager().isMuted(player.getUniqueId().toString())) {
                player.sendMessage(getMessage("mute-command-blocked"));
                return true;
            }
        }

        StringBuilder actionBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            actionBuilder.append(args[i]);
            if (i < args.length - 1) actionBuilder.append(" ");
        }

        String senderName = sender instanceof Player ? ((Player) sender).getDisplayName() : "Console";
        String format = plugin.getFileManager().getMessages().getString("me-format", "&d* %player% %action%");
        String formatted = ChatColor.translateAlternateColorCodes('&',
                format.replace("%player%", senderName).replace("%action%", actionBuilder.toString()));

        Bukkit.broadcastMessage(formatted);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
