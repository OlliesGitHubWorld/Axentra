package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.gui.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvSeeCommand implements CommandExecutor, TabCompleter, Listener {

    // Layout (54 slots): rows 1-3 main inventory, row 4 hotbar, row 5 divider,
    // row 6 helmet/chestplate/leggings/boots + off hand.
    private static final int SIZE = 54;
    private static final int HELMET = 45, CHESTPLATE = 46, LEGGINGS = 47, BOOTS = 48, OFF_HAND = 50;

    private final App plugin;
    // Viewers with an editable invsee open → their session
    private final Map<UUID, EditHolder> editors = new HashMap<>();

    public InvSeeCommand(App plugin) {
        this.plugin = plugin;
        // Keep editable views in sync with changes the target makes themselves.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (EditHolder holder : editors.values()) {
                if (holder.dirty) continue;
                Player target = Bukkit.getPlayer(holder.target);
                if (target != null) fill(holder.inventory, target.getInventory());
            }
        }, 10L, 10L);
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

    /** Holder for an editable invsee view; not a MenuHolder so items can be moved. */
    private static class EditHolder implements InventoryHolder {
        private final UUID target;
        private Inventory inventory;
        private boolean dirty;

        private EditHolder(UUID target) {
            this.target = target;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static boolean isFillerSlot(int slot) {
        return (slot >= 36 && slot <= 44) || slot == 49 || slot > OFF_HAND && slot < SIZE;
    }

    private static ItemStack filler(String name) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Copies the target's inventory into the GUI layout. */
    private static void fill(Inventory gui, PlayerInventory inv) {
        ItemStack[] storage = inv.getStorageContents();
        for (int i = 9; i < storage.length && i < 36; i++) gui.setItem(i - 9, storage[i]);
        for (int i = 0; i < 9 && i < storage.length; i++) gui.setItem(27 + i, storage[i]);
        gui.setItem(HELMET, inv.getHelmet());
        gui.setItem(CHESTPLATE, inv.getChestplate());
        gui.setItem(LEGGINGS, inv.getLeggings());
        gui.setItem(BOOTS, inv.getBoots());
        gui.setItem(OFF_HAND, inv.getItemInOffHand());

        ItemStack pane = filler(" ");
        for (int slot = 36; slot <= 44; slot++) gui.setItem(slot, filler("&7Armor & Off Hand &8↓"));
        gui.setItem(49, filler("&8← &7Armor &8| &7Off Hand &8→"));
        for (int slot = OFF_HAND + 1; slot < SIZE; slot++) gui.setItem(slot, pane);
    }

    /** Writes the GUI contents back to the target's inventory. */
    private void push(EditHolder holder) {
        holder.dirty = false;
        Player target = Bukkit.getPlayer(holder.target);
        if (target == null) return;
        Inventory gui = holder.inventory;
        PlayerInventory inv = target.getInventory();
        for (int i = 0; i < 27; i++) inv.setItem(i + 9, gui.getItem(i));
        for (int i = 0; i < 9; i++) inv.setItem(i, gui.getItem(27 + i));
        inv.setHelmet(gui.getItem(HELMET));
        inv.setChestplate(gui.getItem(CHESTPLATE));
        inv.setLeggings(gui.getItem(LEGGINGS));
        inv.setBoots(gui.getItem(BOOTS));
        inv.setItemInOffHand(gui.getItem(OFF_HAND));
        target.updateInventory();
    }

    private void schedulePush(EditHolder holder) {
        if (holder.dirty) return;
        holder.dirty = true;
        // The click/drag result is only applied after the event, so sync on the next tick.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (holder.dirty) push(holder);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EditHolder holder)) return;
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || (event.getRawSlot() >= 0 && event.getRawSlot() < SIZE && isFillerSlot(event.getRawSlot()))) {
            event.setCancelled(true);
            return;
        }
        schedulePush(holder);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EditHolder holder)) return;
        for (int raw : event.getRawSlots()) {
            if (raw < SIZE && isFillerSlot(raw)) {
                event.setCancelled(true);
                return;
            }
        }
        schedulePush(holder);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditHolder holder)) return;
        if (holder.dirty) push(holder);
        editors.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID quitting = event.getPlayer().getUniqueId();
        // Close views of a target that leaves (changes are pushed before their data is saved).
        for (Map.Entry<UUID, EditHolder> entry : new ArrayList<>(editors.entrySet())) {
            if (entry.getValue().target.equals(quitting)) {
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer != null) viewer.closeInventory();
            }
        }
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
        String title = ChatColor.stripColor(target.getName() + "'s Inventory");

        // Editing your own inventory through a copy of it would duplicate items, so self is read-only.
        if (isReadOnly() || target.equals(player)) {
            // Snapshot in a MenuHolder: MenuProtectionListener blocks every click and drag.
            Inventory copy = MenuHolder.create(SIZE, title);
            fill(copy, target.getInventory());
            player.openInventory(copy);
        } else {
            EditHolder holder = new EditHolder(target.getUniqueId());
            holder.inventory = Bukkit.createInventory(holder, SIZE, title);
            fill(holder.inventory, target.getInventory());
            player.openInventory(holder.inventory);
            editors.put(player.getUniqueId(), holder);
        }

        player.sendMessage(getMessage("invsee-opened").replace("%player%", target.getName()));
        return true;
    }
}
