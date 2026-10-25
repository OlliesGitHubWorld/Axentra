package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.gui.HistoryGUI;
import mc.ollie.gui.HomesGUI;
import mc.ollie.gui.PunishGUI;
import mc.ollie.gui.RestoreInvGUI;
import mc.ollie.gui.TimelineGUI;
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
            return;
        }

        // RestoreInv GUI (list view)
        RestoreInvGUI restoreInvListGUI = RestoreInvGUI.getActiveGUI(title);
        if (restoreInvListGUI != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            restoreInvListGUI.handleListClick(player, event.getRawSlot(), title);
            return;
        }

        // RestoreInv GUI (preview view)
        RestoreInvGUI restoreInvPreviewGUI = RestoreInvGUI.getPreviewGUI(title);
        if (restoreInvPreviewGUI != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            restoreInvPreviewGUI.handlePreviewClick(player, event.getRawSlot(), title);
            return;
        }

        // Timeline GUI
        TimelineGUI timelineGUI = TimelineGUI.getActiveGUI(title);
        if (timelineGUI != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            timelineGUI.handleClick(player, event.getRawSlot(), title);
            return;
        }

        // Homes GUI
        HomesGUI homesGUI = HomesGUI.getActiveGUI(title);
        if (homesGUI != null) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            homesGUI.handleClick(player, event.getRawSlot(), title, event.isShiftClick());
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        PunishGUI.removeActiveGUI(title);
        HistoryGUI.removeActiveGUI(title);
        RestoreInvGUI.removeActiveGUI(title);
        RestoreInvGUI.removePreviewGUI(title);
        TimelineGUI.removeActiveGUI(title);
        HomesGUI.removeActiveGUI(title);
    }
}
