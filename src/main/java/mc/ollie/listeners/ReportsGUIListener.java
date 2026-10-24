package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ReportsGUIListener implements Listener {

    private final App plugin;

    public ReportsGUIListener(App plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.translateAlternateColorCodes('&', "&8&lOpen Reports"))) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        // Get target name from display name
        String targetName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        // Get lore to find report info
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) return;

        // Find report ID from last lore line
        String lastLine = ChatColor.stripColor(lore.get(lore.size() - 1));
        if (lastLine.startsWith("ID: ")) {
            try {
                int reportId = Integer.parseInt(lastLine.substring(4));
                DatabaseManager.ReportInfo report = plugin.getDatabaseManager().getReport(reportId);
                if (report == null) return;

                // Claim the report
                plugin.getDatabaseManager().claimReport(reportId, player.getName());
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&b&lAxentra &8» &aClaimed report #" + reportId + " for &e" + targetName));

                // Teleport to target if online
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    player.teleport(target.getLocation());
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&b&lAxentra &8» &aTeleported to &e" + targetName));
                }

                // Log
                String staffUuid = player.getUniqueId().toString();
                plugin.getDatabaseManager().logAction(player.getName(), staffUuid, "REPORT_CLAIM",
                        targetName, "Claimed report #" + reportId);
            } catch (NumberFormatException ignored) {}
        }
    }
}
