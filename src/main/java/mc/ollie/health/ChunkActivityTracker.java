package mc.ollie.health;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buffers lag-relevant player activity (block placements, entity spawns)
 * in memory and flushes to the database every minute.
 */
public class ChunkActivityTracker {

    private final App plugin;

    // Key: "uuid:world:chunkX:chunkZ:type:detail"
    private final Map<String, BufferedActivity> buffer = new ConcurrentHashMap<>();

    public ChunkActivityTracker(App plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Flush every 60 seconds (1200 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                flush();
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, 1200L);

        // Track time-in-chunk for online players every 20 seconds (400 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    trackTimeInChunk(p);
                }
            }
        }.runTaskTimer(plugin, 400L, 400L);
    }

    /**
     * Records a lag-relevant action by a player in their current chunk.
     */
    public void record(Player player, String actionType, String detail) {
        int chunkX = player.getLocation().getBlockX() >> 4;
        int chunkZ = player.getLocation().getBlockZ() >> 4;
        String world = player.getWorld().getName();
        String uuid = player.getUniqueId().toString();

        String key = uuid + ":" + world + ":" + chunkX + ":" + chunkZ + ":" + actionType + ":" + detail;
        buffer.compute(key, (k, existing) -> {
            if (existing == null) {
                return new BufferedActivity(uuid, player.getName(), world, chunkX, chunkZ, actionType, detail, 1);
            }
            existing.count++;
            return existing;
        });
    }

    /**
     * Records a lag-relevant action at a specific chunk location.
     */
    public void record(Player player, String world, int chunkX, int chunkZ, String actionType, String detail) {
        String uuid = player.getUniqueId().toString();
        String key = uuid + ":" + world + ":" + chunkX + ":" + chunkZ + ":" + actionType + ":" + detail;
        buffer.compute(key, (k, existing) -> {
            if (existing == null) {
                return new BufferedActivity(uuid, player.getName(), world, chunkX, chunkZ, actionType, detail, 1);
            }
            existing.count++;
            return existing;
        });
    }

    private void trackTimeInChunk(Player player) {
        record(player, "TIME", null);
    }

    private void flush() {
        if (buffer.isEmpty()) return;

        List<DatabaseManager.ChunkActivityEntry> entries = new ArrayList<>();
        long now = System.currentTimeMillis();

        buffer.forEach((key, activity) -> {
            entries.add(new DatabaseManager.ChunkActivityEntry(
                    0, activity.uuid, activity.playerName,
                    activity.world, activity.chunkX, activity.chunkZ,
                    activity.actionType, activity.detail, activity.count, now));
        });
        buffer.clear();

        plugin.getDatabaseManager().batchInsertChunkActivity(entries);
    }

    private static class BufferedActivity {
        final String uuid;
        final String playerName;
        final String world;
        final int chunkX;
        final int chunkZ;
        final String actionType;
        final String detail;
        int count;

        BufferedActivity(String uuid, String playerName, String world, int chunkX, int chunkZ,
                        String actionType, String detail, int count) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.actionType = actionType;
            this.detail = detail;
            this.count = count;
        }
    }
}
