package mc.ollie.gui;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class TimelineGUI {

    private final App plugin;
    private final String playerName;
    private List<TimelineEntry> entries;
    private int currentPage;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<String, TimelineGUI> activeGUIs = new HashMap<>();

    public static class TimelineEntry {
        public final String type;
        public final long time;
        public final String description;
        public final ChatColor color;
        public final int linkedId; // case id or snapshot id, -1 if none

        public TimelineEntry(String type, long time, String description, ChatColor color) {
            this(type, time, description, color, -1);
        }

        public TimelineEntry(String type, long time, String description, ChatColor color, int linkedId) {
            this.type = type;
            this.time = time;
            this.description = description;
            this.color = color;
            this.linkedId = linkedId;
        }
    }

    public TimelineGUI(App plugin, String playerName) {
        this.plugin = plugin;
        this.playerName = playerName;
    }

    public static TimelineGUI getActiveGUI(String title) { return activeGUIs.get(title); }
    public static void removeActiveGUI(String title) { activeGUIs.remove(title); }

    public void open(Player staff, List<TimelineEntry> entries, int page) {
        this.entries = entries;
        int totalPages = (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        this.currentPage = page;

        String title = ChatColor.DARK_GRAY + "\u2726 " + ChatColor.DARK_PURPLE + ChatColor.BOLD + "Timeline"
                + ChatColor.DARK_GRAY + " \u2726 " + ChatColor.RESET + playerName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        ItemStack topBar = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, topBar);

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
            skullMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "\u23F0 " + playerName + "'s Timeline");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Total events: " + ChatColor.WHITE + entries.size());
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
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= CONTENT_SLOTS.length) break;

            TimelineEntry entry = entries.get(i);
            Material icon = getTypeIcon(entry.type);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Time: " + ChatColor.WHITE + sdf.format(new Date(entry.time)));
            lore.add(ChatColor.GRAY + truncate(entry.description, 50));
            if (entry.linkedId > 0) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "\u25B6 Click for details");
            }

            inv.setItem(CONTENT_SLOTS[slotIndex],
                    createItem(icon, entry.color + "" + ChatColor.BOLD + entry.type, lore));
        }

        // Bottom bar
        ItemStack botBar = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, botBar);

        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "\u25C0 Previous",
                    ChatColor.GRAY + "Go to page " + page));
        }
        inv.setItem(49, createItem(Material.BOOK, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Page " + (page + 1) + "/" + totalPages,
                ChatColor.GRAY + "Showing " + entries.size() + " events"));
        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Next \u25B6",
                    ChatColor.GRAY + "Go to page " + (page + 2)));
        }
        inv.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "\u2715 Close",
                ChatColor.GRAY + "Close this menu."));

        activeGUIs.put(title, this);
        staff.openInventory(inv);
    }

    public void handleClick(Player staff, int slot, String title) {
        if (slot == 53) { staff.closeInventory(); return; }
        if (slot == 48 && currentPage > 0) {
            removeActiveGUI(title);
            open(staff, entries, currentPage - 1);
            return;
        }
        int totalPages = (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE);
        if (slot == 50 && currentPage < totalPages - 1) {
            removeActiveGUI(title);
            open(staff, entries, currentPage + 1);
            return;
        }
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                int index = currentPage * ITEMS_PER_PAGE + i;
                if (index < entries.size()) {
                    TimelineEntry entry = entries.get(index);
                    if (entry.linkedId > 0) {
                        staff.closeInventory();
                        if (entry.type.equals("CASE")) {
                            staff.performCommand("case " + entry.linkedId);
                        } else if (entry.type.equals("DEATH")) {
                            staff.performCommand("restoreinv " + playerName + " snapshot " + entry.linkedId);
                        }
                    }
                }
                return;
            }
        }
    }

    private Material getTypeIcon(String type) {
        switch (type) {
            case "JOIN": return Material.LIME_DYE;
            case "QUIT": return Material.GRAY_DYE;
            case "CASE": return Material.RED_CONCRETE;
            case "DEATH": return Material.SKELETON_SKULL;
            case "NAME_CHANGE": return Material.NAME_TAG;
            case "IP_CHANGE": return Material.COMPASS;
            case "TRUST_CHANGE": return Material.DIAMOND;
            case "FREEZE": return Material.ICE;
            case "UNFREEZE": return Material.BLUE_ICE;
            case "NOTE": return Material.WRITABLE_BOOK;
            case "REPORT": return Material.BELL;
            default: return Material.PAPER;
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
