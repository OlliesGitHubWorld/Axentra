package mc.ollie.gui;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class HomesGUI {

    private final App plugin;
    private final String playerName;
    private final boolean isOwn;
    private List<DatabaseManager.HomeInfo> homes;
    private int currentPage;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<String, HomesGUI> activeGUIs = new HashMap<>();

    public HomesGUI(App plugin, String playerName, boolean isOwn) {
        this.plugin = plugin;
        this.playerName = playerName;
        this.isOwn = isOwn;
    }

    public static HomesGUI getActiveGUI(String title) { return activeGUIs.get(title); }
    public static void removeActiveGUI(String title) { activeGUIs.remove(title); }

    public void open(Player viewer, List<DatabaseManager.HomeInfo> homes, int page) {
        this.homes = homes;
        int totalPages = (int) Math.ceil((double) homes.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        this.currentPage = page;

        String title = ChatColor.DARK_GRAY + "\u2726 " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Homes"
                + ChatColor.DARK_GRAY + " \u2726 " + ChatColor.RESET + playerName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        ItemStack topBar = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, topBar);

        // Info item
        inv.setItem(4, createItem(Material.RED_BED,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "\u2302 " + playerName + "'s Homes",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Total homes: " + ChatColor.WHITE + homes.size(),
                        "",
                        ChatColor.YELLOW + "Click" + ChatColor.GRAY + " to teleport",
                        isOwn ? ChatColor.YELLOW + "Shift-Click" + ChatColor.GRAY + " to delete" : ""
                )));

        // Side borders
        ItemStack sideBorder = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, sideBorder);
            inv.setItem(row * 9 + 8, sideBorder);
        }

        // Content
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, homes.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= CONTENT_SLOTS.length) break;

            DatabaseManager.HomeInfo home = homes.get(i);
            Material icon = getWorldIcon(home.world);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + home.world);
            lore.add(ChatColor.GRAY + "Location: " + ChatColor.WHITE
                    + String.format("%.1f, %.1f, %.1f", home.x, home.y, home.z));
            lore.add("");
            lore.add(ChatColor.YELLOW + "\u25B6 Click to teleport");
            if (isOwn) {
                lore.add(ChatColor.RED + "\u25B6 Shift-Click to delete");
            }

            inv.setItem(CONTENT_SLOTS[slotIndex],
                    createItem(icon, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + home.name, lore));
        }

        // Bottom bar
        ItemStack botBar = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, botBar);

        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "\u25C0 Previous",
                    Collections.singletonList(ChatColor.GRAY + "Go to page " + page)));
        }
        inv.setItem(49, createItem(Material.BOOK, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Page " + (page + 1) + "/" + totalPages,
                Collections.singletonList(ChatColor.GRAY + "Showing " + homes.size() + " homes")));
        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Next \u25B6",
                    Collections.singletonList(ChatColor.GRAY + "Go to page " + (page + 2))));
        }
        inv.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "\u2715 Close",
                Collections.singletonList(ChatColor.GRAY + "Close this menu.")));

        activeGUIs.put(title, this);
        viewer.openInventory(inv);
    }

    public void handleClick(Player viewer, int slot, String title, boolean shiftClick) {
        if (slot == 53) { viewer.closeInventory(); return; }
        if (slot == 48 && currentPage > 0) {
            removeActiveGUI(title);
            open(viewer, homes, currentPage - 1);
            return;
        }
        int totalPages = (int) Math.ceil((double) homes.size() / ITEMS_PER_PAGE);
        if (slot == 50 && currentPage < totalPages - 1) {
            removeActiveGUI(title);
            open(viewer, homes, currentPage + 1);
            return;
        }
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                int index = currentPage * ITEMS_PER_PAGE + i;
                if (index < homes.size()) {
                    DatabaseManager.HomeInfo home = homes.get(index);
                    if (shiftClick && isOwn) {
                        // Delete home
                        viewer.closeInventory();
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            plugin.getDatabaseManager().deleteHome(viewer.getUniqueId().toString(), home.name);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                viewer.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        plugin.getFileManager().getMessages().getString("prefix", "") + " "
                                        + plugin.getFileManager().getMessages().getString("delhome-success", "Home deleted.")
                                                .replace("%home%", home.name)));
                            });
                        });
                    } else {
                        // Teleport
                        viewer.closeInventory();
                        World world = Bukkit.getWorld(home.world);
                        if (world != null) {
                            Location loc = new Location(world, home.x, home.y, home.z, home.yaw, home.pitch);
                            viewer.teleport(loc);
                            viewer.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    plugin.getFileManager().getMessages().getString("prefix", "") + " "
                                    + plugin.getFileManager().getMessages().getString("home-teleported", "Teleported to home.")
                                            .replace("%home%", home.name)));
                        }
                    }
                }
                return;
            }
        }
    }

    private Material getWorldIcon(String worldName) {
        if (worldName.contains("nether")) return Material.NETHERRACK;
        if (worldName.contains("end")) return Material.END_STONE;
        return Material.RED_BED;
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

    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }
}
