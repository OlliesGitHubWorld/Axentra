package mc.ollie.health;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Records TPS/MSPT/memory snapshots to the database every minute.
 * Purges old data based on configured retention period.
 */
public class TpsHistoryRecorder {

    private final App plugin;

    public TpsHistoryRecorder(App plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Record every 60 seconds (1200 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                record();
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, 1200L);

        // Purge old data once per hour (72000 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                purge();
            }
        }.runTaskTimerAsynchronously(plugin, 72000L, 72000L);
    }

    private void record() {
        TickProfiler profiler = plugin.getTickProfiler();
        HealthScoreService healthService = plugin.getHealthScoreService();
        if (profiler == null) return;

        // Calculate health score on main thread first (needs entity counts)
        Bukkit.getScheduler().runTask(plugin, () -> {
            int score = healthService != null ? healthService.calculate() : 100;
            int online = Bukkit.getOnlinePlayers().size();

            // Then insert async
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getDatabaseManager().insertTpsHistory(
                    System.currentTimeMillis(),
                    profiler.getTps1m(), profiler.getTps5m(), profiler.getTps15m(),
                    profiler.getMsptAvg(), profiler.getMsptMax(),
                    profiler.getMemoryUsed(), profiler.getMemoryMax(),
                    score, online
                )
            );
        });
    }

    private void purge() {
        int days = plugin.getFileManager().getHealth().getInt("tps.history-retention-days", 30);
        plugin.getDatabaseManager().purgeTpsHistory(days);
        plugin.getDatabaseManager().purgeChunkActivity(days);
    }
}
