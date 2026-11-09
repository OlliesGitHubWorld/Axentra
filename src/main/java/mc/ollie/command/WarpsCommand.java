package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.gui.MenuHolder;
import mc.ollie.DatabaseManager;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /warps — opens a styled GUI showing all available warps.
 * Players with Axentra.editwarp can also reorder warps and change their icons from the GUI.
 */
public class WarpsCommand implements CommandExecutor, TabCompleter, Listener {

    private final App plugin;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<UUID, WarpsGUIData> activeGUIs = new HashMap<>();
    private static final long RENAME_TIMEOUT_MS = 60_000;
    private final Map<UUID, PendingRename> pendingRenames = new ConcurrentHashMap<>();

    public WarpsCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.warps")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                List<String> names = plugin.getDatabaseManager().getWarpNames();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (names.isEmpty()) {
                        sender.sendMessage(plugin.getMessageManager().getMessage("warp-no-warps"));
                    } else {
                        sender.sendMessage(plugin.getMessageManager().getMessage("warp-list-header"));
                        sender.sendMessage(ChatColor.GRAY + String.join(", ", names));
                    }
                });
            });
            return true;
        }

        reopen(player, 0);
        return true;
    }

    /** Reloads warps from the database and opens the GUI on the given page. */
    private void reopen(Player player, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.WarpInfo> warps = plugin.getDatabaseManager().getAllWarps();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) openGUI(player, warps, page, null);
            });
        });
    }

    /**
     * @param selected name of the warp picked up for moving, or null when nothing is selected
     */
    private void openGUI(Player player, List<DatabaseManager.WarpInfo> warps, int page, String selected) {
        boolean canEdit = player.hasPermission("Axentra.editwarp");

        // Filter to warps the player can use; editors see every warp so positions match the real order
        List<DatabaseManager.WarpInfo> accessible = new ArrayList<>();
        for (DatabaseManager.WarpInfo warp : warps) {
            if (canEdit || warp.permission == null || warp.permission.isEmpty()
                    || player.hasPermission(warp.permission)) {
                accessible.add(warp);
            }
        }

        if (accessible.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("warp-no-warps"));
            return;
        }

        int totalPages = (int) Math.ceil((double) accessible.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = ChatColor.DARK_GRAY + "✦ " + ChatColor.AQUA + ChatColor.BOLD + "Warps"
                + ChatColor.DARK_GRAY + " ✦";
        Inventory inv = MenuHolder.create(54, title);

        // Background
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        // Top bar
        ItemStack topBar = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, topBar);

        // Info item (top center)
        List<String> infoLore = new ArrayList<>(Arrays.asList(
                "",
                ChatColor.GRAY + "Total warps: " + ChatColor.WHITE + accessible.size(),
                "",
                ChatColor.YELLOW + "Click" + ChatColor.GRAY + " a warp to teleport"
        ));
        if (canEdit) {
            infoLore.add("");
            infoLore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Editing");
            infoLore.add(ChatColor.YELLOW + "Shift+Click" + ChatColor.GRAY + " → select a warp to move");
            infoLore.add(ChatColor.GRAY + "  then " + ChatColor.YELLOW + "click" + ChatColor.GRAY + " another warp to place it there");
            infoLore.add(ChatColor.YELLOW + "Hotbar key 1-9" + ChatColor.GRAY + " → use that item as icon");
            infoLore.add(ChatColor.YELLOW + "Q (drop key)" + ChatColor.GRAY + " → rename");
        }
        inv.setItem(4, createItem(Material.ENDER_PEARL,
                ChatColor.AQUA + "" + ChatColor.BOLD + "✈ Server Warps", infoLore));

        // Side borders
        ItemStack sideBorder = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, sideBorder);
            inv.setItem(row * 9 + 8, sideBorder);
        }

        // Content — warp items
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, accessible.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= CONTENT_SLOTS.length) break;

            DatabaseManager.WarpInfo warp = accessible.get(i);
            Material icon = warp.icon != null ? Material.matchMaterial(warp.icon) : null;
            if (!WarpCommand.isValidIcon(icon)) icon = Material.ENDER_PEARL;

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + warp.world);
            lore.add(ChatColor.GRAY + "Location: " + ChatColor.WHITE +
                    String.format("%.0f, %.0f, %.0f", warp.x, warp.y, warp.z));
            if (canEdit) {
                lore.add(ChatColor.GRAY + "Position: " + ChatColor.WHITE + "#" + (i + 1));
                if (warp.permission != null && !warp.permission.isEmpty()) {
                    lore.add(ChatColor.GRAY + "Permission: " + ChatColor.WHITE + warp.permission);
                }
            }
            lore.add("");
            boolean isSelected = warp.name.equals(selected);
            if (isSelected) {
                lore.add(ChatColor.GOLD + "✔ Selected");
                lore.add(ChatColor.GRAY + "Click another warp to place it there");
                lore.add(ChatColor.GRAY + "Click this one again to cancel");
            } else if (selected != null) {
                lore.add(ChatColor.GOLD + "▶ Click to place " + ChatColor.WHITE + selected + ChatColor.GOLD + " here");
            } else {
                lore.add(ChatColor.YELLOW + "▶ Click to teleport");
                if (canEdit) {
                    lore.add(ChatColor.DARK_GRAY + "Shift+Click: move · 1-9: icon · Q: rename");
                }
            }

            String displayName = (isSelected ? ChatColor.GOLD : ChatColor.AQUA) + "" + ChatColor.BOLD + warp.name;
            ItemStack item = createItem(icon, displayName, lore);
            if (isSelected) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setEnchantmentGlintOverride(true);
                    item.setItemMeta(meta);
                }
            }
            inv.setItem(CONTENT_SLOTS[slotIndex], item);
        }

        // Bottom bar
        ItemStack botBar = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, botBar);

        // Pagination
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "◀ Previous",
                    Collections.singletonList(ChatColor.GRAY + "Go to page " + page)));
        }
        inv.setItem(49, createItem(Material.BOOK,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Page " + (page + 1) + "/" + totalPages,
                Collections.singletonList(ChatColor.GRAY + "Showing " + accessible.size() + " warps")));
        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "Next ▶",
                    Collections.singletonList(ChatColor.GRAY + "Go to page " + (page + 2))));
        }
        inv.setItem(53, createItem(Material.BARRIER,
                ChatColor.RED + "" + ChatColor.BOLD + "✕ Close",
                Collections.singletonList(ChatColor.GRAY + "Close this menu.")));

        // Register after opening: opening closes the previous menu, which clears its entry
        player.openInventory(inv);
        activeGUIs.put(player.getUniqueId(), new WarpsGUIData(inv, accessible, page, selected));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        WarpsGUIData data = activeGUIs.get(player.getUniqueId());
        if (data == null || event.getInventory() != data.inventory) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Close button
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        // Previous page
        // Page navigation keeps the selection, so warps can be moved across pages
        if (slot == 48 && data.page > 0) {
            openGUI(player, data.warps, data.page - 1, data.selected);
            return;
        }

        // Next page
        int totalPages = (int) Math.ceil((double) data.warps.size() / ITEMS_PER_PAGE);
        if (slot == 50 && data.page < totalPages - 1) {
            openGUI(player, data.warps, data.page + 1, data.selected);
            return;
        }

        // Content click — find warp
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] != slot) continue;

            int index = data.page * ITEMS_PER_PAGE + i;
            if (index >= data.warps.size()) return;
            DatabaseManager.WarpInfo warp = data.warps.get(index);

            boolean canEdit = player.hasPermission("Axentra.editwarp");
            ClickType click = event.getClick();

            if (canEdit && data.selected != null) {
                // A warp is picked up: clicking places it, clicking the same one cancels
                if (warp.name.equals(data.selected)) {
                    openGUI(player, data.warps, data.page, null);
                } else if (click == ClickType.LEFT || click == ClickType.RIGHT
                        || click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                    moveWarp(player, data.selected, index);
                }
            } else if (canEdit && (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT)) {
                openGUI(player, data.warps, data.page, warp.name);
            } else if (canEdit && click == ClickType.NUMBER_KEY) {
                setIconFromHotbar(player, warp.name, event.getHotbarButton(), data.page);
            } else if (canEdit && (click == ClickType.DROP || click == ClickType.CONTROL_DROP)) {
                pendingRenames.put(player.getUniqueId(), new PendingRename(warp.name, data.page));
                player.closeInventory();
                player.sendMessage(plugin.getMessageManager().getMessage("warp-rename-prompt")
                        .replace("%name%", warp.name));
            } else if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                player.closeInventory();
                player.performCommand("warp " + warp.name);
            }
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        WarpsGUIData data = activeGUIs.get(event.getPlayer().getUniqueId());
        if (data != null && event.getInventory() == data.inventory) {
            activeGUIs.remove(event.getPlayer().getUniqueId());
        }
    }

    /** Captures the new name typed in chat after pressing Q on a warp. Runs before other chat handling. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingRename pending = pendingRenames.remove(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (System.currentTimeMillis() > pending.expiresAt || input.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getMessageManager().getMessage("warp-rename-cancelled"));
            Bukkit.getScheduler().runTask(plugin, () -> reopen(player, pending.page));
            return;
        }

        // Already async, so the database work can run here directly
        String result = WarpCommand.renameWarp(plugin, player, pending.warpName, input);
        player.sendMessage(result);
        Bukkit.getScheduler().runTask(plugin, () -> reopen(player, pending.page));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingRenames.remove(event.getPlayer().getUniqueId());
    }

    private void moveWarp(Player player, String name, int newIndex) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int moved = plugin.getDatabaseManager().moveWarp(name, newIndex);
            if (moved < 0) return;
            plugin.getDatabaseManager().logAction(player.getName(), player.getUniqueId().toString(),
                    "MOVE_WARP", name, "position " + (moved + 1));
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getMessageManager().getMessage("warp-moved")
                        .replace("%name%", name).replace("%position%", String.valueOf(moved + 1)));
                // Follow the warp to its new page
                reopen(player, moved / ITEMS_PER_PAGE);
            });
        });
    }

    private void setIconFromHotbar(Player player, String name, int hotbarSlot, int page) {
        if (hotbarSlot < 0) return;
        ItemStack held = player.getInventory().getItem(hotbarSlot);
        Material icon = held != null ? held.getType() : null;
        if (!WarpCommand.isValidIcon(icon)) {
            player.sendMessage(plugin.getMessageManager().getMessage("warp-invalid-icon")
                    .replace("%icon%", "slot " + (hotbarSlot + 1)));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updateWarpIcon(name, icon.name());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getMessageManager().getMessage("warp-icon-set")
                        .replace("%name%", name).replace("%icon%", icon.name()));
                reopen(player, page);
            });
        });
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    private static class PendingRename {
        final String warpName;
        final int page;
        final long expiresAt = System.currentTimeMillis() + RENAME_TIMEOUT_MS;

        PendingRename(String warpName, int page) {
            this.warpName = warpName;
            this.page = page;
        }
    }

    private static class WarpsGUIData {
        final Inventory inventory;
        final List<DatabaseManager.WarpInfo> warps;
        final int page;
        final String selected;

        WarpsGUIData(Inventory inventory, List<DatabaseManager.WarpInfo> warps, int page, String selected) {
            this.inventory = inventory;
            this.page = page;
            this.warps = warps;
            this.selected = selected;
        }
    }
}
