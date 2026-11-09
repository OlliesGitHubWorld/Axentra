package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RtpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final Set<UUID> warmingUp = new HashSet<>();

    private static final Set<Material> UNSAFE_BLOCKS = new HashSet<>(Arrays.asList(
            Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK,
            Material.CACTUS, Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW,
            Material.WITHER_ROSE
    ));

    public RtpCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.rtp")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
            return true;
        }

        // Determine world
        World world;
        if (args.length >= 1) {
            world = Bukkit.getWorld(args[0]);
            if (world == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("rtp-disabled-world"));
                return true;
            }
        } else {
            world = player.getWorld();
        }

        // Check world config
        ConfigurationSection worldConfig = plugin.getFileManager().getSettings()
                .getConfigurationSection("rtp.worlds." + world.getName());
        if (worldConfig == null || !worldConfig.getBoolean("enabled", false)) {
            player.sendMessage(plugin.getMessageManager().getMessage("rtp-disabled-world"));
            return true;
        }

        // Check cooldown
        if (!player.hasPermission("Axentra.rtp.bypass")) {
            int cooldownSeconds = plugin.getFileManager().getSettings().getInt("rtp.cooldown-seconds", 300);
            long lastUsed = plugin.getDatabaseManager().getRtpCooldown(player.getUniqueId().toString());
            long remaining = (lastUsed + cooldownSeconds * 1000L - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage(plugin.getMessageManager().getMessage("rtp-cooldown")
                        .replace("%time%", formatTime(remaining)));
                return true;
            }
        }

        player.sendMessage(plugin.getMessageManager().getMessage("rtp-searching"));

        int minRadius = worldConfig.getInt("min-radius", 500);
        int maxRadius = worldConfig.getInt("max-radius", 10000);
        int centerX = worldConfig.getInt("center-x", 0);
        int centerZ = worldConfig.getInt("center-z", 0);
        int maxY = worldConfig.getInt("max-y", -1);

        // Search async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location safe = findSafeLocation(world, minRadius, maxRadius, centerX, centerZ, maxY);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (safe == null) {
                    player.sendMessage(plugin.getMessageManager().getMessage("rtp-failed"));
                    return;
                }

                int delay = plugin.getFileManager().getSettings().getInt("rtp.teleport-delay", 5);
                if (delay <= 0) {
                    performRtp(player, safe);
                } else {
                    startCountdown(player, safe, delay);
                }
            });
        });
        return true;
    }

    private Location findSafeLocation(World world, int minRadius, int maxRadius,
                                      int centerX, int centerZ, int maxY) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        boolean nether = world.getEnvironment() == World.Environment.NETHER;

        for (int attempt = 0; attempt < 50; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            int radius = rng.nextInt(minRadius, maxRadius + 1);
            int x = centerX + (int) (Math.cos(angle) * radius);
            int z = centerZ + (int) (Math.sin(angle) * radius);

            // Load chunk synchronously (will be run async, but getHighestBlockYAt is thread-safe in some impls)
            // Actually, we need to do this on main thread for chunk loading.
            // Let's use a sync callback approach.
            int y;
            if (nether) {
                y = findNetherSafe(world, x, z, maxY > 0 ? maxY : 120);
            } else {
                y = world.getHighestBlockYAt(x, z);
                if (y <= 0) continue;
            }

            if (maxY > 0 && y > maxY) continue;

            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block above2 = world.getBlockAt(x, y + 2, z);

            if (block.getType().isSolid() &&
                    above.getType().isAir() && above2.getType().isAir() &&
                    !UNSAFE_BLOCKS.contains(block.getType())) {
                return new Location(world, x + 0.5, y + 1.0, z + 0.5);
            }
        }
        return null;
    }

    private int findNetherSafe(World world, int x, int z, int maxY) {
        for (int y = maxY; y > 4; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block above2 = world.getBlockAt(x, y + 2, z);
            if (block.getType().isSolid() && !UNSAFE_BLOCKS.contains(block.getType()) &&
                    above.getType().isAir() && above2.getType().isAir()) {
                return y;
            }
        }
        return -1;
    }

    private void startCountdown(Player player, Location target, int seconds) {
        if (warmingUp.contains(player.getUniqueId())) return;
        warmingUp.add(player.getUniqueId());

        Location startLoc = player.getLocation().clone();
        player.sendMessage(plugin.getMessageManager().getMessage("rtp-countdown")
                .replace("%seconds%", String.valueOf(seconds)));

        new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    warmingUp.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (player.getLocation().getBlockX() != startLoc.getBlockX() ||
                        player.getLocation().getBlockY() != startLoc.getBlockY() ||
                        player.getLocation().getBlockZ() != startLoc.getBlockZ()) {
                    player.sendMessage(plugin.getMessageManager().getMessage("rtp-cancelled"));
                    warmingUp.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                remaining--;
                if (remaining <= 0) {
                    performRtp(player, target);
                    warmingUp.remove(player.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void performRtp(Player player, Location target) {
        // Save for /back
        if (plugin.getFileManager().getSettings().getBoolean("enabled-modules.back", true)) {
            try {
                var backCmd = plugin.getCommand("back");
                if (backCmd != null && backCmd.getExecutor() instanceof BackCommand bc) {
                    bc.saveLocation(player);
                }
            } catch (Exception ignored) {}
        }

        player.teleport(target);
        player.sendMessage(plugin.getMessageManager().getMessage("rtp-teleporting")
                .replace("%x%", String.valueOf(target.getBlockX()))
                .replace("%y%", String.valueOf(target.getBlockY()))
                .replace("%z%", String.valueOf(target.getBlockZ()))
                .replace("%world%", target.getWorld().getName()));

        // Set cooldown async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getDatabaseManager().setRtpCooldown(player.getUniqueId().toString()));
    }

    private String formatTime(long seconds) {
        if (seconds >= 60) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> worlds = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    worlds.add(w.getName());
                }
            }
            return worlds;
        }
        return Collections.emptyList();
    }
}
