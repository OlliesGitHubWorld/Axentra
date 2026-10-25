package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffmodeListener implements Listener {

    private final App plugin;
    public StaffmodeListener(App plugin) {
        this.plugin = plugin;
    }

    public boolean isFrozen(UUID uuid) {
        return plugin.getDatabaseManager().isPlayerFrozen(uuid.toString());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!plugin.getStaffmodeManager().isInStaffmode(player.getUniqueId())) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        event.setCancelled(true);

        switch (name) {
            case "Random Teleport": {
                List<Player> candidates = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player) && !online.hasPermission("Axentra.staffchat")) {
                        candidates.add(online);
                    }
                }
                if (candidates.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&lAxentra &8» &cNo players to teleport to!"));
                    return;
                }
                Player target = candidates.get((int) (Math.random() * candidates.size()));
                player.teleport(target.getLocation());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&lAxentra &8» &aTeleported to &e" + target.getName()));
                break;
            }
            case "Vanish Toggle": {
                plugin.getServer().dispatchCommand(player, "vanish");
                break;
            }
            case "Exit Staffmode": {
                plugin.getServer().dispatchCommand(player, "staffmode");
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!plugin.getStaffmodeManager().isInStaffmode(player.getUniqueId())) return;
        if (!(event.getRightClicked() instanceof Player)) return;

        Player target = (Player) event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        event.setCancelled(true);

        switch (name) {
            case "Freeze": {
                player.performCommand("freeze " + target.getName());
                break;
            }
            case "InvSee": {
                plugin.getServer().dispatchCommand(player, "invsee " + target.getName());
                break;
            }
            case "History": {
                plugin.getServer().dispatchCommand(player, "history " + target.getName());
                break;
            }
            case "Punish": {
                plugin.getServer().dispatchCommand(player, "punish " + target.getName());
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (plugin.getStaffmodeManager().isInStaffmode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getStaffmodeManager().isInStaffmode(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Restore inventory if in staffmode
        if (plugin.getStaffmodeManager().isInStaffmode(uuid)) {
            plugin.getStaffmodeManager().restoreInventory(player);
        }

        // Remove from duty
        if (plugin.getDutyManager().isOnDuty(uuid)) {
            plugin.getDutyManager().setOnDuty(uuid, false);
        }

    }
}
