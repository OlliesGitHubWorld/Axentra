package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetHomeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public SetHomeCommand(App plugin) {
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
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("Axentra.sethome")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        String uuid = player.getUniqueId().toString();

        // Check world blacklist
        List<String> blacklist = plugin.getFileManager().getSettings().getStringList("homes.blacklisted-worlds");
        if (blacklist.contains(player.getWorld().getName())) {
            player.sendMessage(getMessage("sethome-blacklisted"));
            return true;
        }

        // Check limit
        int currentCount = plugin.getDatabaseManager().getHomeCount(uuid);
        boolean isNewHome = plugin.getDatabaseManager().getHome(uuid, name) == null;
        if (isNewHome) {
            int limit = getHomeLimit(player);
            if (limit >= 0 && currentCount >= limit) {
                player.sendMessage(getMessage("sethome-limit").replace("%limit%", String.valueOf(limit)));
                return true;
            }
        }

        Location loc = player.getLocation();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().setHome(uuid, name, loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(getMessage("sethome-success").replace("%home%", name));
            });
        });
        return true;
    }

    private int getHomeLimit(Player player) {
        if (player.hasPermission("Axentra.homes.unlimited")) return -1;

        // Check for Axentra.homes.<n> permissions, find highest
        int highest = plugin.getFileManager().getSettings().getInt("homes.default-limit", 3);
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("Axentra.homes." + i)) {
                highest = i;
                break;
            }
        }
        return highest;
    }
}
