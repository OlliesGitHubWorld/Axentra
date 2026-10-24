package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportsCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public ReportsCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        if (!sender.hasPermission("Axentra.reports")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;

        List<DatabaseManager.ReportInfo> openReports = plugin.getDatabaseManager().getOpenReports();

        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&', "&8&lOpen Reports"));

        int slot = 0;
        for (DatabaseManager.ReportInfo report : openReports) {
            if (slot >= 45) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        "&e" + report.targetName));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Reported by: &f" + report.reporterName));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Reason: &f" + report.reason));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Time: &f" + report.reportedAt));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&aClick to view details"));
                // Store report ID in lore for the GUI listener to read
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8ID: " + report.id));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
