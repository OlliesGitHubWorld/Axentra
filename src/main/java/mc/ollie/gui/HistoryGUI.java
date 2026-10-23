package mc.ollie.gui;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class HistoryGUI {

    private final App plugin;
    private final String playerName;
    private final List<DatabaseManager.CaseInfo> cases;
    private int currentPage;

    private static final int CASES_PER_PAGE = 28; // 4 rows of 7

    // Slots in the 4 content rows (rows 1-4), excluding left/right borders
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<String, HistoryGUI> activeGUIs = new HashMap<>();

    public HistoryGUI(App plugin, String playerName, List<DatabaseManager.CaseInfo> cases) {
        this.plugin = plugin;
        this.playerName = playerName;
        this.cases = cases;
    }

    public String getPlayerName() {
        return playerName;
    }

    public List<DatabaseManager.CaseInfo> getCases() {
        return cases;
    }

    public static HistoryGUI getActiveGUI(String title) {
        return activeGUIs.get(title);
    }

    public static void removeActiveGUI(String title) {
        activeGUIs.remove(title);
    }

    public void open(Player staff, int page) {
        int totalPages = (int) Math.ceil((double) cases.size() / CASES_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        this.currentPage = page;

        String title = ChatColor.DARK_GRAY + "✦ " + ChatColor.DARK_AQUA + ChatColor.BOLD + "History"
                + ChatColor.DARK_GRAY + " ✦ " + ChatColor.RESET + playerName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill everything with dark background
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        // ═══════════════════════════════════════════
        // Row 0: Top bar
        // ═══════════════════════════════════════════
        ItemStack topBar = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, topBar);

        // Player head with summary
        int warnCount = 0, muteCount = 0, banCount = 0, kickCount = 0;
        for (DatabaseManager.CaseInfo c : cases) {
            switch (c.type.toUpperCase()) {
                case "WARN":     warnCount++; break;
                case "MUTE":
                case "TEMPMUTE": muteCount++; break;
                case "BAN":
                case "TEMPBAN":  banCount++; break;
                case "KICK":     kickCount++; break;
            }
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
            skullMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "📋 " + playerName + "'s History");
            List<String> headLore = new ArrayList<>();
            headLore.add("");
            headLore.add(ChatColor.GRAY + "Total cases: " + ChatColor.WHITE + cases.size());
            headLore.add("");
            headLore.add(ChatColor.YELLOW + "⚠ " + warnCount + " Warning" + (warnCount != 1 ? "s" : ""));
            headLore.add(ChatColor.GOLD + "🔇 " + muteCount + " Mute" + (muteCount != 1 ? "s" : ""));
            headLore.add(ChatColor.LIGHT_PURPLE + "👢 " + kickCount + " Kick" + (kickCount != 1 ? "s" : ""));
            headLore.add(ChatColor.RED + "🔨 " + banCount + " Ban" + (banCount != 1 ? "s" : ""));
            skullMeta.setLore(headLore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(4, head);

        // ═══════════════════════════════════════════
        // Left/right borders (rows 1-4)
        // ═══════════════════════════════════════════
        ItemStack sideBorder = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, sideBorder);
            inv.setItem(row * 9 + 8, sideBorder);
        }

        // ═══════════════════════════════════════════
        // Content: Cases
        // ═══════════════════════════════════════════
        int start = page * CASES_PER_PAGE;
        int end = Math.min(start + CASES_PER_PAGE, cases.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= CONTENT_SLOTS.length) break;

            DatabaseManager.CaseInfo c = cases.get(i);
            Material icon = getTypeIcon(c.type);
            ChatColor typeColor = getTypeColor(c.type);

            String displayName = typeColor + "" + ChatColor.BOLD + c.type + " #" + c.id;

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + (c.reason != null ? c.reason : "No reason"));
            lore.add(ChatColor.GRAY + "By: " + ChatColor.WHITE + c.issuedBy);
            lore.add(ChatColor.GRAY + "Date: " + ChatColor.WHITE + formatDate(c.issuedAt));
            if (c.expiresAt != null) {
                lore.add(ChatColor.GRAY + "Expires: " + ChatColor.WHITE + formatDate(c.expiresAt));
            }
            String statusColor = c.active ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
            lore.add(ChatColor.GRAY + "Status: " + statusColor + (c.active ? "Active" : "Resolved"));
            if (c.chatLog != null && !c.chatLog.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.AQUA + "📝 Has chat log");
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Click for details");

            inv.setItem(CONTENT_SLOTS[slotIndex], createItem(icon, displayName, lore));
        }

        // ═══════════════════════════════════════════
        // Row 5: Bottom bar + Navigation
        // ═══════════════════════════════════════════
        ItemStack botBar = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, botBar);

        // Previous page
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, ChatColor.AQUA + "" + ChatColor.BOLD + "◀ Previous",
                    ChatColor.GRAY + "Go to page " + page));
        }

        // Page info
        inv.setItem(49, createItem(Material.BOOK, ChatColor.AQUA + "" + ChatColor.BOLD + "Page " + (page + 1) + "/" + totalPages,
                ChatColor.GRAY + "Showing " + cases.size() + " cases"));

        // Next page
        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, ChatColor.AQUA + "" + ChatColor.BOLD + "Next ▶",
                    ChatColor.GRAY + "Go to page " + (page + 2)));
        }

        // Close
        inv.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "✕ Close",
                ChatColor.GRAY + "Close this menu."));

        activeGUIs.put(title, this);
        staff.openInventory(inv);
    }

    public void handleClick(Player staff, int slot, String title) {
        // Close
        if (slot == 53) {
            staff.closeInventory();
            return;
        }

        // Previous page
        if (slot == 48 && currentPage > 0) {
            removeActiveGUI(title);
            open(staff, currentPage - 1);
            return;
        }

        // Next page
        int totalPages = (int) Math.ceil((double) cases.size() / CASES_PER_PAGE);
        if (slot == 50 && currentPage < totalPages - 1) {
            removeActiveGUI(title);
            open(staff, currentPage + 1);
            return;
        }

        // Case click
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                int index = currentPage * CASES_PER_PAGE + i;
                if (index < cases.size()) {
                    staff.closeInventory();
                    staff.performCommand("case " + cases.get(index).id);
                }
                return;
            }
        }
    }

    private Material getTypeIcon(String type) {
        switch (type.toUpperCase()) {
            case "BAN":
            case "TEMPBAN":  return Material.RED_CONCRETE;
            case "MUTE":
            case "TEMPMUTE": return Material.ORANGE_CONCRETE;
            case "WARN":     return Material.YELLOW_CONCRETE;
            case "KICK":     return Material.PURPLE_CONCRETE;
            default:         return Material.WHITE_CONCRETE;
        }
    }

    private ChatColor getTypeColor(String type) {
        switch (type.toUpperCase()) {
            case "BAN":
            case "TEMPBAN":  return ChatColor.RED;
            case "MUTE":
            case "TEMPMUTE": return ChatColor.GOLD;
            case "WARN":     return ChatColor.YELLOW;
            case "KICK":     return ChatColor.LIGHT_PURPLE;
            default:         return ChatColor.GRAY;
        }
    }

    private String formatDate(String timestamp) {
        if (timestamp == null) return "N/A";
        try {
            long millis = Long.parseLong(timestamp);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.format(new java.util.Date(millis));
        } catch (NumberFormatException ignored) {}
        if (timestamp.length() >= 16) return timestamp.substring(0, 16);
        return timestamp;
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        return createItem(material, name, Arrays.asList(loreLines));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
