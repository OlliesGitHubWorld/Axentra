package mc.ollie.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an inventory as an Axentra menu. MenuProtectionListener cancels every click
 * and drag in an inventory with this holder, so items can never be taken out of a
 * menu even if the per-GUI click routing fails to recognise it.
 */
public class MenuHolder implements InventoryHolder {

    private Inventory inventory;

    public static Inventory create(int size, String title) {
        MenuHolder holder = new MenuHolder();
        holder.inventory = Bukkit.createInventory(holder, size, title);
        return holder.inventory;
    }

    public static boolean isMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MenuHolder;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
