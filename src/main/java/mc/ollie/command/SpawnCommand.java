package mc.ollie.command;

import mc.ollie.App;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final Map<UUID, BukkitTask> countdowns = new HashMap<>();

    public SpawnCommand(App plugin) {
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

    private World resolveSpawnWorld() {
        String name = plugin.getFileManager().getSettings().getString("spawn-world", "");
        if (name != null && !name.isEmpty()) {
            World world = Bukkit.getWorld(name);
            if (world != null) return world;
        }
        return Bukkit.getWorlds().get(0);
    }

    private boolean isCountdownEnabled() {
        return plugin.getFileManager().getSettings().getBoolean("spawn-countdown", true);
    }

    public void cancelCountdown(UUID uuid) {
        BukkitTask task = countdowns.remove(uuid);
        if (task != null) task.cancel();
    }

    private void startCountdown(Player player, Location destination) {
        cancelCountdown(player.getUniqueId());

        Location startLocation = player.getLocation().clone();
        int seconds = plugin.getFileManager().getSettings().getInt("spawn-countdown-seconds", 3);

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
                    player.sendTitle(
                            ChatColor.translateAlternateColorCodes('&', getMessage("spawn-cancelled-title")),
                            ChatColor.translateAlternateColorCodes('&', getMessage("spawn-cancelled-subtitle")),
                            0, 40, 10
                    );
                    player.sendMessage(getMessage("spawn-cancelled"));
                    return;
                }

                if (remaining == 0) {
                    cancel();
                    countdowns.remove(player.getUniqueId());
                    player.teleport(destination);
                    player.sendTitle(
                            ChatColor.translateAlternateColorCodes('&', getMessage("spawn-teleported-title")),
                            ChatColor.translateAlternateColorCodes('&', getMessage("spawn-teleported-subtitle")),
                            0, 40, 10
                    );
                    player.sendMessage(getMessage("spawn-self"));
                    return;
                }

                player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&',
                                getMessage("spawn-countdown-title").replace("%seconds%", String.valueOf(remaining))),
                        ChatColor.translateAlternateColorCodes('&', getMessage("spawn-countdown-subtitle")),
                        0, 25, 0
                );
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdowns.put(player.getUniqueId(), task);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("Axentra.spawn.others")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.spawn")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            Location spawn = resolveSpawnWorld().getSpawnLocation();

            if (isCountdownEnabled() && !player.hasPermission("Axentra.spawn.bypass")) {
                startCountdown(player, spawn);
            } else {
                player.teleport(spawn);
                player.sendMessage(getMessage("spawn-self"));
            }
            return true;
        }

        if (!sender.hasPermission("Axentra.spawn.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        Location spawn = resolveSpawnWorld().getSpawnLocation();
        target.teleport(spawn);
        target.sendMessage(getMessage("spawn-received").replace("%player%", sender.getName()));
        sender.sendMessage(getMessage("spawn-other").replace("%player%", target.getName()));
        return true;
    }
}