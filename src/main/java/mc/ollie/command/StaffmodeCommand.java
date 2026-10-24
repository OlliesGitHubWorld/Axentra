package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class StaffmodeCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public StaffmodeCommand(App plugin) {
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

    private ItemStack createStaffItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        if (!sender.hasPermission("Axentra.staffmode")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        String staffName = player.getName();
        String staffUuid = uuid.toString();

        boolean currentlyInStaffmode = plugin.getStaffmodeManager().isInStaffmode(uuid);

        if (currentlyInStaffmode) {
            // Disable staffmode
            plugin.getStaffmodeManager().restoreInventory(player);
            plugin.getServer().dispatchCommand(player, "vanish");

            // Auto off-duty when leaving staffmode
            if (plugin.getDutyManager().isOnDuty(uuid)) {
                plugin.getDutyManager().setOnDuty(uuid, false);
                plugin.getDatabaseManager().logAction(staffName, staffUuid, "DUTY_OFF", staffName, "Auto off-duty (staffmode exit)");
            }

            plugin.getDatabaseManager().logAction(staffName, staffUuid, "STAFFMODE_OFF", staffName, "Exited staffmode");
            player.sendMessage(getMessage("staffmode-disabled"));
        } else {
            // Enable staffmode
            plugin.getStaffmodeManager().saveInventory(player);
            player.getInventory().clear();

            player.getInventory().setItem(0, createStaffItem(Material.COMPASS, "&b&lRandom Teleport"));
            player.getInventory().setItem(1, createStaffItem(Material.ICE, "&c&lFreeze"));
            player.getInventory().setItem(2, createStaffItem(Material.BOOK, "&e&lInvSee"));
            player.getInventory().setItem(3, createStaffItem(Material.PAPER, "&6&lHistory"));
            player.getInventory().setItem(4, createStaffItem(Material.ANVIL, "&d&lPunish"));
            player.getInventory().setItem(5, createStaffItem(Material.ENDER_EYE, "&7&lVanish Toggle"));
            player.getInventory().setItem(8, createStaffItem(Material.REDSTONE, "&c&lExit Staffmode"));

            plugin.getServer().dispatchCommand(player, "vanish");

            // Auto on-duty when entering staffmode
            if (!plugin.getDutyManager().isOnDuty(uuid)) {
                plugin.getDutyManager().setOnDuty(uuid, true);
                plugin.getDatabaseManager().logAction(staffName, staffUuid, "DUTY_ON", staffName, "Auto on-duty (staffmode)");
            }

            plugin.getDatabaseManager().logAction(staffName, staffUuid, "STAFFMODE_ON", staffName, "Entered staffmode");
            player.sendMessage(getMessage("staffmode-enabled"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
