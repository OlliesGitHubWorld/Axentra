package mc.ollie;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class SnapshotManager {

    private final App plugin;

    public SnapshotManager(App plugin) {
        this.plugin = plugin;
    }

    public int createSnapshot(Player player, String reason, String deathCause) {
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        Location loc = player.getLocation();

        String invData = serializeItems(player.getInventory().getContents());
        String armorData = serializeItems(player.getInventory().getArmorContents());
        String offhandData = serializeItem(player.getInventory().getItemInOffHand());
        String ecData = serializeItems(player.getEnderChest().getContents());

        return plugin.getDatabaseManager().createSnapshot(
                uuid, name, invData, armorData, offhandData, ecData,
                player.getLevel(), player.getExp(),
                player.getHealth(), player.getFoodLevel(),
                player.getGameMode().name(),
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                loc.getX(), loc.getY(), loc.getZ(),
                reason, deathCause
        );
    }

    public static String serializeItems(ItemStack[] items) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos);
            oos.writeInt(items.length);
            for (ItemStack item : items) {
                oos.writeObject(item);
            }
            oos.close();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static ItemStack[] deserializeItems(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream ois = new BukkitObjectInputStream(bis);
            int size = ois.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) ois.readObject();
            }
            ois.close();
            return items;
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }

    public static String serializeItem(ItemStack item) {
        return serializeItems(new ItemStack[]{item});
    }

    public static ItemStack deserializeItem(String data) {
        ItemStack[] items = deserializeItems(data);
        return items.length > 0 ? items[0] : null;
    }

    public static int countItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) count++;
        }
        return count;
    }

    public void restoreInventory(Player player, DatabaseManager.SnapshotInfo snapshot) {
        ItemStack[] inv = deserializeItems(snapshot.inventoryData);
        ItemStack[] armor = deserializeItems(snapshot.armorData);
        ItemStack offhand = deserializeItem(snapshot.offhandData);

        player.getInventory().setContents(inv);
        player.getInventory().setArmorContents(armor);
        if (offhand != null) player.getInventory().setItemInOffHand(offhand);
        player.setLevel(snapshot.xpLevel);
        player.setExp(snapshot.xpProgress);
        player.setHealth(Math.min(snapshot.health, player.getMaxHealth()));
        player.setFoodLevel(snapshot.food);
        try {
            player.setGameMode(GameMode.valueOf(snapshot.gamemode));
        } catch (Exception ignored) {}
    }

    public void restoreEnderchest(Player player, DatabaseManager.SnapshotInfo snapshot) {
        ItemStack[] ec = deserializeItems(snapshot.enderchestData);
        player.getEnderChest().setContents(ec);
    }

    public void cleanupSnapshots(String playerUuid) {
        int maxPerPlayer = plugin.getFileManager().getProtection().getInt("snapshots.max-per-player", 30);
        int maxAgeDays = plugin.getFileManager().getProtection().getInt("snapshots.max-age-days", 14);
        plugin.getDatabaseManager().cleanupSnapshots(playerUuid, maxPerPlayer, maxAgeDays);
    }
}
