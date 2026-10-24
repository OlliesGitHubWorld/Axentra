package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final App plugin;

    public SetSpawnCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.setspawn")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        // Center on block if standing on a block corner
        double x = Math.floor(loc.getX()) + 0.5;
        double z = Math.floor(loc.getZ()) + 0.5;

        // Save exact location to config
        plugin.getConfig().set("spawn.world", loc.getWorld().getName());
        plugin.getConfig().set("spawn.x", x);
        plugin.getConfig().set("spawn.y", loc.getY());
        plugin.getConfig().set("spawn.z", z);
        plugin.getConfig().set("spawn.yaw", (double) loc.getYaw());
        plugin.getConfig().set("spawn.pitch", (double) loc.getPitch());
        plugin.saveConfig();

        // Also set the world spawn location
        player.getWorld().setSpawnLocation(loc);

        player.sendMessage(getMessage("setspawn-success"));
        return true;
    }
}
