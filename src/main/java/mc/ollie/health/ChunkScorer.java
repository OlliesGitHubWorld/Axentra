package mc.ollie.health;

import mc.ollie.App;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calculates a chunk's lag score from its entities and tile entities.
 * Only tile entities that actually tick (hoppers, furnaces, spawners, ...) count by default;
 * passive ones like chests, signs and naturally generated sculk sensors are ignored.
 */
public class ChunkScorer {

    private final int entityWeight;
    private final int otherTileWeight;
    private final Map<String, Integer> tileWeights = new HashMap<>();

    public ChunkScorer(App plugin) {
        FileConfiguration health = plugin.getFileManager().getHealth();
        this.entityWeight = health.getInt("lag-chunks.weights.entity", 1);
        this.otherTileWeight = health.getInt("lag-chunks.weights.other-tile-entity", 0);
        ConfigurationSection section = health.getConfigurationSection("lag-chunks.weights.tile-entities");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                tileWeights.put(key.toUpperCase(), section.getInt(key));
            }
        }
    }

    public Result score(Chunk chunk) {
        Result result = new Result();
        for (Entity e : chunk.getEntities()) {
            if (e instanceof Player) {
                if (result.player == null) result.player = e.getName();
                continue;
            }
            result.entityCount++;
            result.entityTypes.merge(e.getType().name(), 1, Integer::sum);
        }
        result.score += result.entityCount * entityWeight;

        for (BlockState bs : getTileEntities(chunk)) {
            String type = bs.getType().name();
            int weight = tileWeights.getOrDefault(type, otherTileWeight);
            if (weight <= 0) continue;
            result.tileCount++;
            result.tileTypes.merge(type, 1, Integer::sum);
            result.score += weight;
        }
        return result;
    }

    // Paper's Chunk#getTileEntities(boolean useSnapshot) skips copying every block state,
    // which matters when scanning tens of thousands of chunks. Spigot falls back to snapshots.
    private static final Method NO_SNAPSHOT_TILES = findNoSnapshotMethod();

    private static Method findNoSnapshotMethod() {
        try {
            return Chunk.class.getMethod("getTileEntities", boolean.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static BlockState[] getTileEntities(Chunk chunk) {
        if (NO_SNAPSHOT_TILES != null) {
            try {
                return (BlockState[]) NO_SNAPSHOT_TILES.invoke(chunk, false);
            } catch (ReflectiveOperationException | ClassCastException ignored) {
            }
        }
        return chunk.getTileEntities();
    }

    public static class Result {
        public double score;
        public int entityCount;
        public int tileCount;
        public String player;
        public final Map<String, Integer> entityTypes = new LinkedHashMap<>();
        public final Map<String, Integer> tileTypes = new LinkedHashMap<>();
    }
}
