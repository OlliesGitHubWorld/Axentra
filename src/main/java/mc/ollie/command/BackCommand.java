package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackCommand implements CommandExecutor {

    private final App plugin;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    // flag to prevent /back itself from overwriting the saved location
    private final java.util.Set<UUID> backingPlayers = new java.util.HashSet<>();

    public BackCommand(App plugin) {
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

    public void saveLocation(Player player) {
        if (backingPlayers.contains(player.getUniqueId())) return;
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public boolean isBacking(UUID uuid) {
        return backingPlayers.contains(uuid);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("Axentra.back")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        Location last = lastLocations.get(player.getUniqueId());
        if (last == null) {
            player.sendMessage(getMessage("back-no-location"));
            return true;
        }

        backingPlayers.add(player.getUniqueId());
        player.teleport(last);
        backingPlayers.remove(player.getUniqueId());

        player.sendMessage(getMessage("back-success"));
        return true;
    }
}