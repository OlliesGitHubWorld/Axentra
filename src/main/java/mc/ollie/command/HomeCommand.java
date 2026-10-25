package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final Map<UUID, BukkitTask> countdowns = new HashMap<>();

    public HomeCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void cancelCountdown(UUID uuid) {
        BukkitTask task = countdowns.remove(uuid);
        if (task != null) task.cancel();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        if (args.length == 1) {
            String uuid = ((Player) sender).getUniqueId().toString();
            String input = args[0].toLowerCase();
            // Check if it's a staff lookup like "player:home"
            if (input.contains(":") && sender.hasPermission("Axentra.home.others")) {
                return Collections.emptyList();
            }
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
        if (!player.hasPermission("Axentra.home")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        String input = args.length > 0 ? args[0] : "home";
        String targetUuid;
        String homeName;

        // Staff syntax: player:homename
        if (input.contains(":") && player.hasPermission("Axentra.home.others")) {
            String[] parts = input.split(":", 2);
            String targetName = parts[0];
            homeName = parts.length > 1 ? parts[1].toLowerCase() : "home";
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                targetUuid = target.getUniqueId().toString();
            } else {
                targetUuid = plugin.getDatabaseManager().getUuidByName(targetName);
                if (targetUuid == null) {
                    targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();
                }
            }
        } else {
            targetUuid = player.getUniqueId().toString();
            homeName = input.toLowerCase();
        }

        final String finalUuid = targetUuid;
        final String finalName = homeName;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.HomeInfo home = plugin.getDatabaseManager().getHome(finalUuid, finalName);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (home == null) {
                    player.sendMessage(getMessage("home-not-found").replace("%home%", finalName));
                    return;
                }

                World world = Bukkit.getWorld(home.world);
                if (world == null) {
                    player.sendMessage(getMessage("home-world-not-found").replace("%home%", finalName));
                    return;
                }

                Location loc = new Location(world, home.x, home.y, home.z, home.yaw, home.pitch);
                int delay = plugin.getFileManager().getSettings().getInt("homes.teleport-delay", 3);

                if (delay > 0 && !player.hasPermission("Axentra.home.bypass")) {
                    startCountdown(player, loc, finalName, delay);
                } else {
                    player.teleport(loc);
                    player.sendMessage(getMessage("home-teleported").replace("%home%", finalName));
                }
            });
        });
        return true;
    }

    private void startCountdown(Player player, Location destination, String homeName, int seconds) {
        cancelCountdown(player.getUniqueId());

        Location startLocation = player.getLocation().clone();

        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    countdowns.remove(player.getUniqueId());
                    return;
                }

                Location current = player.getLocation();
                if (current.getBlockX() != startLocation.getBlockX()
                        || current.getBlockY() != startLocation.getBlockY()
                        || current.getBlockZ() != startLocation.getBlockZ()) {
                    cancel();
                    countdowns.remove(player.getUniqueId());
                    player.sendMessage(getMessage("home-cancelled"));
                    return;
                }

                if (remaining == 0) {
                    cancel();
                    countdowns.remove(player.getUniqueId());
                    player.teleport(destination);
                    player.sendMessage(getMessage("home-teleported").replace("%home%", homeName));
                    return;
                }

                player.sendTitle(
                        ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Teleporting",
                        ChatColor.GRAY + "in " + remaining + "s...",
                        0, 25, 0
                );
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdowns.put(player.getUniqueId(), task);
    }
}
