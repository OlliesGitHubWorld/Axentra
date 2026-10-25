package mc.ollie.gui;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.SnapshotManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class RestoreInvGUI {

    private final App plugin;
    private final String playerName;
    private List<DatabaseManager.SnapshotInfo> snapshots;
    private int currentPage;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<String, RestoreInvGUI> activeGUIs = new HashMap<>();
    private static final Map<String, RestoreInvGUI> previewGUIs = new HashMap<>();

    private DatabaseManager.SnapshotInfo previewSnapshot;

    public RestoreInvGUI(App plugin, String playerName) {
        this.plugin = plugin;
        this.playerName = playerName;
    }

    public String getPlayerName() { return playerName; }
    public List<DatabaseManager.SnapshotInfo> getSnapshots() { return snapshots; }
    public DatabaseManager.SnapshotInfo getPreviewSnapshot() { return previewSnapshot; }

    public static RestoreInvGUI getActiveGUI(String title) { return activeGUIs.get(title); }
    public static RestoreInvGUI getPreviewGUI(String title) { return previewGUIs.get(title); }
    public static void removeActiveGUI(String title) { activeGUIs.remove(title); }
    public static void removePreviewGUI(String title) { previewGUIs.remove(title); }

    public void open(Player staff, List<DatabaseManager.SnapshotInfo> snapshots, int page) {
        this.snapshots = snapshots;
        int totalPages = (int) Math.ceil((double) snapshots.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        this.currentPage = page;

        String title = ChatColor.DARK_GRAY + "\u2726 " + ChatColor.DARK_GREEN + ChatColor.BOLD + "Snapshots"
                + ChatColor.DARK_GRAY + " \u2726 " + ChatColor.RESET + playerName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        ItemStack topBar = createItem(Material.LIME_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, topBar);

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
            skullMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "\uD83D\uDDC3 " + playerName + "'s Snapshots");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Total snapshots: " + ChatColor.WHITE + snapshots.size());
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(4, head);

        // Side borders
        ItemStack sideBorder = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, sideBorder);
            inv.setItem(row * 9 + 8, sideBorder);
        }

        // Content
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, snapshots.size());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= CONTENT_SLOTS.length) break;

            DatabaseManager.SnapshotInfo snap = snapshots.get(i);
            Material icon = getReasonIcon(snap.reason);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Time: " + ChatColor.WHITE + sdf.format(new Date(snap.time)));
            lore.add(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + snap.reason);
            if (snap.deathCause != null) {
                lore.add(ChatColor.GRAY + "Death: " + ChatColor.WHITE + truncate(snap.deathCause, 40));
            }
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + snap.world);
            ItemStack[] items = SnapshotManager.deserializeItems(snap.inventoryData);
            lore.add(ChatColor.GRAY + "Items: " + ChatColor.WHITE + SnapshotManager.countItems(items));
            lore.add(ChatColor.GRAY + "XP: " + ChatColor.WHITE + snap.xpLevel);
            lore.add("");
            lore.add(ChatColor.YELLOW + "\u25B6 Click to preview");

            ChatColor color = getReasonColor(snap.reason);
            inv.setItem(CONTENT_SLOTS[slotIndex],
                    createItem(icon, color + "" + ChatColor.BOLD + snap.reason + " #" + snap.id, lore));
        }

        // Bottom bar
        ItemStack botBar = createItem(Material.LIME_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, botBar);

        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, ChatColor.GREEN + "" + ChatColor.BOLD + "\u25C0 Previous",
                    ChatColor.GRAY + "Go to page " + page));
        }
        inv.setItem(49, createItem(Material.BOOK, ChatColor.GREEN + "" + ChatColor.BOLD + "Page " + (page + 1) + "/" + totalPages,
                ChatColor.GRAY + "Showing " + snapshots.size() + " snapshots"));
        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, ChatColor.GREEN + "" + ChatColor.BOLD + "Next \u25B6",
                    ChatColor.GRAY + "Go to page " + (page + 2)));
        }
        inv.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "\u2715 Close",
                ChatColor.GRAY + "Close this menu."));

        activeGUIs.put(title, this);
        staff.openInventory(inv);
    }

    public void openPreview(Player staff, DatabaseManager.SnapshotInfo snapshot) {
        this.previewSnapshot = snapshot;
        String title = ChatColor.DARK_GRAY + "\u2726 " + ChatColor.GOLD + ChatColor.BOLD + "Preview"
                + ChatColor.DARK_GRAY + " \u2726 " + ChatColor.RESET + "#" + snapshot.id;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill with background
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        // Place inventory items (slots 0-35 in original inv → slots 0-35 here)
        ItemStack[] items = SnapshotManager.deserializeItems(snapshot.inventoryData);
        for (int i = 0; i < Math.min(items.length, 36); i++) {
            if (items[i] != null) inv.setItem(i, items[i]);
        }

        // Armor in slots 36-39
        ItemStack[] armor = SnapshotManager.deserializeItems(snapshot.armorData);
        for (int i = 0; i < Math.min(armor.length, 4); i++) {
            if (armor[i] != null) inv.setItem(36 + i, armor[i]);
        }

        // Offhand in slot 40
        ItemStack offhand = SnapshotManager.deserializeItem(snapshot.offhandData);
        if (offhand != null) inv.setItem(40, offhand);

        // Action buttons in bottom row
        ItemStack botBar = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, botBar);

        inv.setItem(46, createItem(Material.CHEST, ChatColor.GREEN + "" + ChatColor.BOLD + "Restore Inventory",
                ChatColor.GRAY + "Replace the player's inventory.", "", ChatColor.YELLOW + "\u25B6 Click to restore"));
        inv.setItem(47, createItem(Material.ENDER_CHEST, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Restore Enderchest",
                ChatColor.GRAY + "Replace the player's enderchest.", "", ChatColor.YELLOW + "\u25B6 Click to restore"));
        inv.setItem(48, createItem(Material.HOPPER, ChatColor.AQUA + "" + ChatColor.BOLD + "Copy to Self",
                ChatColor.GRAY + "Give these items to yourself.", "", ChatColor.YELLOW + "\u25B6 Click to copy"));
        inv.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "" + ChatColor.BOLD + "\u25C0 Back",
                ChatColor.GRAY + "Go back to snapshot list."));

        previewGUIs.put(title, this);
        staff.openInventory(inv);
    }

    public void handleListClick(Player staff, int slot, String title) {
        if (slot == 53) { staff.closeInventory(); return; }
        if (slot == 48 && currentPage > 0) {
            removeActiveGUI(title);
            open(staff, snapshots, currentPage - 1);
            return;
        }
        int totalPages = (int) Math.ceil((double) snapshots.size() / ITEMS_PER_PAGE);
        if (slot == 50 && currentPage < totalPages - 1) {
            removeActiveGUI(title);
            open(staff, snapshots, currentPage + 1);
            return;
        }
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                int index = currentPage * ITEMS_PER_PAGE + i;
                if (index < snapshots.size()) {
                    removeActiveGUI(title);
                    openPreview(staff, snapshots.get(index));
                }
                return;
            }
        }
    }

    public void handlePreviewClick(Player staff, int slot, String title) {
        if (previewSnapshot == null) return;

        // Restore inventory
        if (slot == 46) {
            staff.closeInventory();
            Player target = Bukkit.getPlayer(playerName);
            if (target != null && target.isOnline()) {
                // Create a MANUAL snapshot first
                plugin.getSnapshotManager().createSnapshot(target, "MANUAL", null);
                plugin.getSnapshotManager().restoreInventory(target, previewSnapshot);
                staff.sendMessage(ChatColor.GREEN + "Restored inventory of " + playerName + " from snapshot #" + previewSnapshot.id);
                plugin.getDatabaseManager().logAction(staff.getName(), staff.getUniqueId().toString(),
                        "INVENTORY_RESTORE", playerName, "Snapshot #" + previewSnapshot.id);
            } else {
                plugin.getDatabaseManager().addPendingRestore(
                        previewSnapshot.playerUuid, previewSnapshot.id, "INVENTORY", staff.getName());
                staff.sendMessage(ChatColor.YELLOW + playerName + " is offline. Restore queued for next login.");
                plugin.getDatabaseManager().logAction(staff.getName(), staff.getUniqueId().toString(),
                        "INVENTORY_RESTORE", playerName, "Snapshot #" + previewSnapshot.id + " (queued)");
            }
            return;
        }

        // Restore enderchest
        if (slot == 47) {
            staff.closeInventory();
            Player target = Bukkit.getPlayer(playerName);
            if (target != null && target.isOnline()) {
                plugin.getSnapshotManager().restoreEnderchest(target, previewSnapshot);
                staff.sendMessage(ChatColor.GREEN + "Restored enderchest of " + playerName + " from snapshot #" + previewSnapshot.id);
                plugin.getDatabaseManager().logAction(staff.getName(), staff.getUniqueId().toString(),
                        "INVENTORY_RESTORE", playerName, "Enderchest from Snapshot #" + previewSnapshot.id);
            } else {
                plugin.getDatabaseManager().addPendingRestore(
                        previewSnapshot.playerUuid, previewSnapshot.id, "ENDERCHEST", staff.getName());
                staff.sendMessage(ChatColor.YELLOW + playerName + " is offline. Enderchest restore queued for next login.");
            }
            return;
        }

        // Copy to self
        if (slot == 48) {
            staff.closeInventory();
            ItemStack[] items = SnapshotManager.deserializeItems(previewSnapshot.inventoryData);
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    HashMap<Integer, ItemStack> overflow = staff.getInventory().addItem(item);
                    for (ItemStack drop : overflow.values()) {
                        staff.getWorld().dropItemNaturally(staff.getLocation(), drop);
                    }
                }
            }
            staff.sendMessage(ChatColor.GREEN + "Copied items from snapshot #" + previewSnapshot.id + " to your inventory.");
            plugin.getDatabaseManager().logAction(staff.getName(), staff.getUniqueId().toString(),
                    "INVENTORY_COPY", playerName, "Snapshot #" + previewSnapshot.id);
            return;
        }

        // Back
        if (slot == 53) {
            removePreviewGUI(title);
            if (snapshots != null) {
                open(staff, snapshots, currentPage);
            } else {
                staff.closeInventory();
            }
        }
    }

    private Material getReasonIcon(String reason) {
        if (reason == null) return Material.CHEST;
        switch (reason) {
            case "DEATH": return Material.SKELETON_SKULL;
            case "JOIN":
            case "QUIT": return Material.OAK_DOOR;
            case "WORLD_CHANGE": return Material.ENDER_PEARL;
            case "CLEAR": return Material.BARRIER;
            case "MANUAL": return Material.WRITABLE_BOOK;
            default: return Material.CHEST;
        }
    }

    private ChatColor getReasonColor(String reason) {
        if (reason == null) return ChatColor.GRAY;
        switch (reason) {
            case "DEATH": return ChatColor.RED;
            case "JOIN": return ChatColor.GREEN;
            case "QUIT": return ChatColor.YELLOW;
            case "WORLD_CHANGE": return ChatColor.LIGHT_PURPLE;
            case "CLEAR": return ChatColor.DARK_RED;
            case "MANUAL": return ChatColor.AQUA;
            default: return ChatColor.GRAY;
        }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        return createItem(material, name, Arrays.asList(loreLines));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
