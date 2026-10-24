package mc.ollie;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StaffmodeManager {

    private final Set<UUID> inStaffmode = new HashSet<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, Boolean> savedFlyState = new HashMap<>();
    private final Map<UUID, GameMode> savedGameMode = new HashMap<>();

    public boolean isInStaffmode(UUID uuid) {
        return inStaffmode.contains(uuid);
    }

    public void saveInventory(Player player) {
        UUID uuid = player.getUniqueId();
        savedInventories.put(uuid, player.getInventory().getContents().clone());
        savedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        savedFlyState.put(uuid, player.getAllowFlight());
        savedGameMode.put(uuid, player.getGameMode());
        inStaffmode.add(uuid);

        // Enable fly in staffmode
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();
        player.getInventory().clear();
        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.get(uuid));
            savedInventories.remove(uuid);
        }
        if (savedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(savedArmor.get(uuid));
            savedArmor.remove(uuid);
        }

        // Restore original fly state and gamemode
        if (savedGameMode.containsKey(uuid)) {
            player.setGameMode(savedGameMode.get(uuid));
            savedGameMode.remove(uuid);
        }
        if (savedFlyState.containsKey(uuid)) {
            player.setAllowFlight(savedFlyState.get(uuid));
            if (!savedFlyState.get(uuid)) player.setFlying(false);
            savedFlyState.remove(uuid);
        }

        inStaffmode.remove(uuid);
    }

    public Set<UUID> getStaffmodeUsers() {
        return new HashSet<>(inStaffmode);
    }
}
