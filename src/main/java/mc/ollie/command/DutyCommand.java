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
import java.util.UUID;

public class DutyCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public DutyCommand(App plugin) {
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        if (!sender.hasPermission("Axentra.duty")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        String staffName = player.getName();
        String staffUuid = uuid.toString();

        boolean currentlyOnDuty = plugin.getDutyManager().isOnDuty(uuid);

        if (currentlyOnDuty) {
            plugin.getDutyManager().setOnDuty(uuid, false);
            plugin.getDatabaseManager().logAction(staffName, staffUuid, "DUTY_OFF", staffName, "Went off duty");
            player.sendMessage(getMessage("duty-off"));

            String notifyMsg = getMessage("duty-notify")
                    .replace("%player%", staffName)
                    .replace("%status%", "off duty");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) && online.hasPermission("Axentra.staffchat")) {
                    online.sendMessage(notifyMsg);
                }
            }
        } else {
            plugin.getDutyManager().setOnDuty(uuid, true);
            plugin.getDatabaseManager().logAction(staffName, staffUuid, "DUTY_ON", staffName, "Went on duty");
            player.sendMessage(getMessage("duty-on"));

            String notifyMsg = getMessage("duty-notify")
                    .replace("%player%", staffName)
                    .replace("%status%", "on duty");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) && online.hasPermission("Axentra.staffchat")) {
                    online.sendMessage(notifyMsg);
                }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
