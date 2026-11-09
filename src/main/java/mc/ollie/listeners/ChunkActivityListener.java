package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.health.ChunkActivityTracker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPlaceEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens for lag-relevant block placements and entity spawns,
 * forwarding them to ChunkActivityTracker for buffered DB persistence.
 */
public class ChunkActivityListener implements Listener {

    private final App plugin;
    private final Set<String> trackedBlocks = new HashSet<>();
    private final Set<String> trackedEntities = new HashSet<>();

    public ChunkActivityListener(App plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration health = plugin.getFileManager().getHealth();
        trackedBlocks.clear();
        for (String s : health.getStringList("lag-chunks.tracked-blocks")) {
            trackedBlocks.add(s.toUpperCase());
        }
        trackedEntities.clear();
        for (String s : health.getStringList("lag-chunks.tracked-entities")) {
            trackedEntities.add(s.toUpperCase());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ChunkActivityTracker tracker = plugin.getChunkActivityTracker();
        if (tracker == null) return;

        String blockType = event.getBlock().getType().name();
        if (trackedBlocks.contains(blockType)) {
            tracker.record(event.getPlayer(), "PLACE", blockType);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        ChunkActivityTracker tracker = plugin.getChunkActivityTracker();
        if (tracker == null) return;

        Player player = event.getPlayer();
        if (player == null) return;

        Entity entity = event.getEntity();
        String entityType = entity.getType().name();
        if (trackedEntities.contains(entityType)) {
            tracker.record(player, "SPAWN", entityType);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        ChunkActivityTracker tracker = plugin.getChunkActivityTracker();
        if (tracker == null) return;

        // Only track player-caused spawns (breeding, spawn eggs)
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING &&
                event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }

        // Find nearest player to attribute
        Entity entity = event.getEntity();
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : entity.getWorld().getPlayers()) {
            double dist = p.getLocation().distanceSquared(entity.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }

        if (nearest != null && nearestDist < 256) { // within 16 blocks
            int chunkX = entity.getLocation().getBlockX() >> 4;
            int chunkZ = entity.getLocation().getBlockZ() >> 4;
            tracker.record(nearest, entity.getWorld().getName(), chunkX, chunkZ,
                    "SPAWN", entity.getType().name());
        }
    }
}
