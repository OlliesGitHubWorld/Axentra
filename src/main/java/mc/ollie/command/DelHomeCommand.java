package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public DelHomeCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        if (args.length == 1) {
            String uuid = ((Player) sender).getUniqueId().toString();
            String input = args[0].toLowerCase();
            List<String> names = plugin.getDatabaseManager().getHomeNames(uuid);
            List<String> result = new ArrayList<>();
            for (String name : names) {
                if (name.toLowerCase().startsWith(input)) result.add(name);
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("Axentra.delhome")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(getMessage("delhome-usage"));
            return true;
        }

        String name = args[0].toLowerCase();
        String uuid = player.getUniqueId().toString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean existed = plugin.getDatabaseManager().getHome(uuid, name) != null;
            if (existed) {
                plugin.getDatabaseManager().deleteHome(uuid, name);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (existed) {
                    player.sendMessage(getMessage("delhome-success").replace("%home%", name));
                } else {
                    player.sendMessage(getMessage("home-not-found").replace("%home%", name));
                }
            });
        });
        return true;
    }
}
