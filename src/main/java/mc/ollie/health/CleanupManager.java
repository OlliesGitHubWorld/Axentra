package mc.ollie.health;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles automatic ground-item cleanup on a configurable interval.
 * Sends warnings before clearing and respects whitelist/named item settings.
 */
public class CleanupManager {

    private final App plugin;
    private int taskId = -1;
    private boolean warningActive = false;

    public CleanupManager(App plugin) {
        this.plugin = plugin;
    }

    public void start() {
        FileConfiguration health = plugin.getFileManager().getHealth();
        if (!health.getBoolean("cleanup.enabled", true)) return;

        int intervalMinutes = health.getInt("cleanup.interval-minutes", 10);
        boolean onlyOnAlert = health.getBoolean("cleanup.only-on-alert", false);

        if (onlyOnAlert || intervalMinutes <= 0) return;

        long intervalTicks = intervalMinutes * 60L * 20L;
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                scheduleCleanup();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks).getTaskId();
    }

    /**
     * Triggers a cleanup with warning countdown.
     */
    public void scheduleCleanup() {
        if (warningActive) return;
        warningActive = true;

        FileConfiguration health = plugin.getFileManager().getHealth();
        List<Integer> warnings = health.getIntegerList("cleanup.warnings");
        String mode = health.getString("cleanup.warning-mode", "both");

        if (warnings.isEmpty()) {
            performCleanup();
            warningActive = false;
            return;
        }

        int maxWarning = warnings.stream().mapToInt(i -> i).max().orElse(60);

        // Schedule warnings
        for (int seconds : warnings) {
            long delay = (maxWarning - seconds) * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String msg = plugin.getMessageManager().getMessage("cleanup-warning")
                        .replace("%seconds%", String.valueOf(seconds));
                broadcastWarning(msg, mode);
            }, delay);
        }

        // Schedule actual cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int removed = performCleanup();
            String msg = plugin.getMessageManager().getMessage("cleanup-done")
                    .replace("%count%", String.valueOf(removed));
            Bukkit.broadcastMessage(msg);
            warningActive = false;
        }, maxWarning * 20L);
    }

    /**
     * Immediately clears ground items, returning the count removed.
     */
    public int performCleanup() {
        FileConfiguration health = plugin.getFileManager().getHealth();
        boolean skipNamed = health.getBoolean("cleanup.skip-named-items", true);
        List<String> whitelist = health.getStringList("cleanup.whitelist");
        Set<String> whitelistSet = new HashSet<>(whitelist);

        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    if (skipNamed && item.getItemStack().getItemMeta() != null &&
                            item.getItemStack().getItemMeta().hasDisplayName()) continue;
                    if (whitelistSet.contains(item.getItemStack().getType().name())) continue;
                    item.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    private void broadcastWarning(String message, String mode) {
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if ("actionbar".equals(mode) || "both".equals(mode)) {
                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacy(
                                ChatColor.translateAlternateColorCodes('&', message)));
            }
            if ("chat".equals(mode) || "both".equals(mode)) {
                p.sendMessage(message);
            }
        }
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
