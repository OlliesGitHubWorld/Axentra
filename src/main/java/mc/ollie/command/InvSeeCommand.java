package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.gui.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InvSeeCommand implements CommandExecutor, TabCompleter, Listener {

    private final App plugin;
    private final Set<UUID> readOnlyViewers = new HashSet<>();

    public InvSeeCommand(App plugin) {
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

    private boolean isReadOnly() {
        return plugin.getFileManager().getSettings().getBoolean("invsee-read-only", true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (readOnlyViewers.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        readOnlyViewers.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && player == sender) continue;
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.invsee")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("invsee-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        Player player = (Player) sender;

        if (isReadOnly()) {
            // Layout: rows 1-3 main inventory, row 4 hotbar, row 5 armor + offhand
            PlayerInventory inv = target.getInventory();
            Inventory copy = MenuHolder.create(45,
                    ChatColor.stripColor(target.getName() + "'s Inventory"));
            ItemStack[] storage = inv.getStorageContents();
            for (int i = 9; i < storage.length; i++) copy.setItem(i - 9, storage[i]);
            for (int i = 0; i < 9 && i < storage.length; i++) copy.setItem(27 + i, storage[i]);
            copy.setItem(36, inv.getHelmet());
            copy.setItem(37, inv.getChestplate());
            copy.setItem(38, inv.getLeggings());
            copy.setItem(39, inv.getBoots());
            copy.setItem(41, inv.getItemInOffHand());
            player.openInventory(copy);
            readOnlyViewers.add(player.getUniqueId());
        } else {
            player.openInventory(target.getInventory());
        }

        player.sendMessage(getMessage("invsee-opened").replace("%player%", target.getName()));
        return true;
    }
}