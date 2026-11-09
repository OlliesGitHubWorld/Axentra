package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPlaceEvent;

import java.util.*;

/**
 * Enforces per-chunk entity limits configured in health.yml.
 * Blocks spawns when the limit for a given type/group is exceeded.
 */
public class EntityLimitListener implements Listener {

    private final App plugin;

    // Group mappings
    private static final Set<EntityType> ANIMALS = new HashSet<>(Arrays.asList(
            EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA,
            EntityType.RABBIT, EntityType.GOAT, EntityType.CAMEL, EntityType.SNIFFER,
            EntityType.CAT, EntityType.WOLF, EntityType.PARROT, EntityType.FOX,
            EntityType.BEE, EntityType.FROG, EntityType.TURTLE, EntityType.AXOLOTL,
            EntityType.PANDA, EntityType.MOOSHROOM
    ));

    public EntityLimitListener(App plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isEnabled()) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        Entity entity = event.getEntity();
        if (isOverLimit(entity)) {
            // Check if a nearby player has bypass
            boolean bypassed = false;
            for (Player p : entity.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(entity.getLocation()) < 256 &&
                        p.hasPermission("Axentra.entitylimit.bypass")) {
                    bypassed = true;
                    break;
                }
            }

            if (!bypassed) {
                event.setCancelled(true);
                if (shouldRemoveExcess()) {
                    entity.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        if (player != null && player.hasPermission("Axentra.entitylimit.bypass")) return;

        Entity entity = event.getEntity();
        if (isOverLimit(entity)) {
            event.setCancelled(true);
            if (player != null) {
                String type = entity.getType().name();
                int[] info = getChunkCount(entity);
                int limit = getLimit(entity.getType().name());
                player.sendMessage(plugin.getMessageManager().getMessage("entity-limit-reached")
                        .replace("%type%", type)
                        .replace("%count%", String.valueOf(info[0]))
                        .replace("%limit%", String.valueOf(limit)));
            }
        }
    }

    private boolean isOverLimit(Entity entity) {
        String typeName = entity.getType().name();
        int specificLimit = getLimit(typeName);
        int groupLimit = ANIMALS.contains(entity.getType()) ? getLimit("ANIMALS") : -1;

        Chunk chunk = entity.getLocation().getChunk();

        if (specificLimit > 0) {
            int count = countInChunk(chunk, entity.getType());
            if (count >= specificLimit) return true;
        }

        if (groupLimit > 0 && ANIMALS.contains(entity.getType())) {
            int count = countAnimalsInChunk(chunk);
            if (count >= groupLimit) return true;
        }

        return false;
    }

    private int getLimit(String typeOrGroup) {
        FileConfiguration health = plugin.getFileManager().getHealth();
        return health.getInt("entity-limits.limits." + typeOrGroup, -1);
    }

    private int countInChunk(Chunk chunk, EntityType type) {
        int count = 0;
        for (Entity e : chunk.getEntities()) {
            if (e.getType() == type) count++;
        }
        return count;
    }

    private int countAnimalsInChunk(Chunk chunk) {
        int count = 0;
        for (Entity e : chunk.getEntities()) {
            if (ANIMALS.contains(e.getType())) count++;
        }
        return count;
    }

    private int[] getChunkCount(Entity entity) {
        Chunk chunk = entity.getLocation().getChunk();
        int count = countInChunk(chunk, entity.getType());
        return new int[]{count};
    }

    private boolean isEnabled() {
        return plugin.getFileManager().getHealth().getBoolean("entity-limits.enabled", true);
    }

    private boolean shouldRemoveExcess() {
        return plugin.getFileManager().getHealth().getBoolean("entity-limits.remove-excess", false);
    }
}
