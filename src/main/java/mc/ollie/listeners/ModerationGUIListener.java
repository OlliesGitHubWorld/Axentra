package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.gui.HistoryGUI;
import mc.ollie.gui.PunishGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class ModerationGUIListener implements Listener {

    private final App plugin;

    public ModerationGUIListener(App plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Punish GUI
        PunishGUI punishGUI = PunishGUI.getActiveGUI(title);
        if (punishGUI != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            punishGUI.handleClick(player, event.getRawSlot());
            return;
        }

        // History GUI
        HistoryGUI historyGUI = HistoryGUI.getActiveGUI(title);
        if (historyGUI != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            historyGUI.handleClick(player, event.getRawSlot(), title);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        PunishGUI.removeActiveGUI(title);
        HistoryGUI.removeActiveGUI(title);
    }
}
