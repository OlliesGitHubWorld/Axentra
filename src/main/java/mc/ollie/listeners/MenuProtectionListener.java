package mc.ollie.listeners;

import mc.ollie.gui.MenuHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Blocks taking, placing, shift-clicking and dragging items while any Axentra menu is open.
 * Button handling still happens in the GUI-specific listeners.
 */
public class MenuProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (MenuHolder.isMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (MenuHolder.isMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }
}
