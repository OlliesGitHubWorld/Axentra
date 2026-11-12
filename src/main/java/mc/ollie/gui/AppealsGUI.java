package mc.ollie.gui;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.discord.AppealService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /appeals [open|all] (v1.9.0) — appeals in a GUI in the style of /history, with the same
 * actions as the Discord buttons: left-click accept, right-click deny, shift-click reduce.
 */
public class AppealsGUI implements Listener {

    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private record Entry(DatabaseManager.AppealInfo appeal, DatabaseManager.CaseInfo info) { }
    private record View(Inventory inventory, List<Entry> entries, boolean onlyOpen, int page) { }

    private final App plugin;
    private final AppealService appeals;
    private final Map<UUID, View> views = new HashMap<>();
    private final Map<UUID, Integer> pendingReduce = new ConcurrentHashMap<>();

    public AppealsGUI(App plugin, AppealService appeals) {
        this.plugin = plugin;
        this.appeals = appeals;
    }

    private String msg(String key) {
        return plugin.getMessageManager().getMessage(key);
    }

    /** Loads appeals off the main thread and opens the GUI. */
    public void open(Player player, boolean onlyOpen, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Entry> entries = new ArrayList<>();
            for (DatabaseManager.AppealInfo a : plugin.getDatabaseManager().getAppeals(onlyOpen, 200)) {
                entries.add(new Entry(a, plugin.getDatabaseManager().getCase(a.caseId)));
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (entries.isEmpty()) {
                    player.sendMessage(msg(onlyOpen ? "appeals-empty-open" : "appeals-empty"));
                    return;
                }
                show(player, entries, onlyOpen, page);
            });
        });
    }

    private void show(Player player, List<Entry> entries, boolean onlyOpen, int page) {
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
        page = Math.max(0, Math.min(page, pages - 1));

        String title = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + ChatColor.BOLD + "Appeals" + ChatColor.DARK_GRAY + " ✦";
        Inventory inv = MenuHolder.create(54, title);
        ItemStack bg = item(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);
        ItemStack bar = item(Material.ORANGE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) { inv.setItem(i, bar); inv.setItem(45 + i, bar); }
        ItemStack side = item(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int row = 1; row <= 4; row++) { inv.setItem(row * 9, side); inv.setItem(row * 9 + 8, side); }

        inv.setItem(4, item(Material.GOLDEN_APPLE, ChatColor.GOLD + "" + ChatColor.BOLD + "Appeals", List.of(
                "", ChatColor.GRAY + "Showing: " + ChatColor.WHITE + (onlyOpen ? "open" : "all") + " (" + entries.size() + ")",
                "", ChatColor.GREEN + "Left-click" + ChatColor.GRAY + " → accept",
                ChatColor.RED + "Right-click" + ChatColor.GRAY + " → deny",
                ChatColor.YELLOW + "Shift-click" + ChatColor.GRAY + " → reduce")));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(start + PER_PAGE, entries.size()); i++) {
            Entry e = entries.get(i);
            DatabaseManager.AppealInfo a = e.appeal();
            Material icon = switch (a.status) {
                case "ACCEPTED" -> Material.LIME_DYE;
                case "DENIED" -> Material.RED_DYE;
                case "REDUCED" -> Material.YELLOW_DYE;
                default -> Material.WRITABLE_BOOK;
            };
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (e.info() != null) {
                lore.add(ChatColor.GRAY + "Case: " + ChatColor.WHITE + "#" + e.info().id + " " + e.info().type);
                lore.add(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + cut(e.info().reason, 40));
                lore.add(ChatColor.GRAY + "By: " + ChatColor.WHITE + e.info().issuedBy);
            }
            lore.add(ChatColor.GRAY + "Submitted: " + ChatColor.WHITE + sdf.format(new Date(a.createdAt)));
            for (String[] qa : a.answerPairs()) {
                lore.add("");
                lore.add(ChatColor.GOLD + cut(qa[0], 40));
                for (String line : wrap(qa[1], 40, 3)) lore.add(ChatColor.WHITE + line);
            }
            lore.add("");
            if ("OPEN".equals(a.status)) {
                lore.add(ChatColor.GREEN + "▶ Left-click to accept");
                lore.add(ChatColor.RED + "▶ Right-click to deny");
                lore.add(ChatColor.YELLOW + "▶ Shift-click to reduce");
            } else {
                lore.add(ChatColor.GRAY + "Status: " + ChatColor.WHITE + a.status.toLowerCase()
                        + (a.decidedBy != null ? " by " + a.decidedBy : ""));
                if (a.decision != null && !a.decision.isBlank()) lore.add(ChatColor.GRAY + cut(a.decision, 40));
            }
            inv.setItem(SLOTS[i - start], item(icon, ChatColor.YELLOW + "" + ChatColor.BOLD
                    + (a.playerName != null ? a.playerName : "?") + ChatColor.DARK_GRAY + " #" + a.caseId, lore));
        }

        if (page > 0) inv.setItem(48, item(Material.ARROW, ChatColor.GOLD + "◀ Previous", null));
        inv.setItem(49, item(Material.HOPPER, ChatColor.GOLD + "Show " + (onlyOpen ? "all" : "open only"),
                List.of(ChatColor.GRAY + "Page " + (page + 1) + "/" + pages)));
        if (page < pages - 1) inv.setItem(50, item(Material.ARROW, ChatColor.GOLD + "Next ▶", null));
        inv.setItem(53, item(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "✕ Close", null));

        player.openInventory(inv);
        views.put(player.getUniqueId(), new View(inv, entries, onlyOpen, page));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        View view = views.get(player.getUniqueId());
        if (view == null || event.getInventory() != view.inventory()) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 53) { player.closeInventory(); return; }
        if (slot == 48 && view.page() > 0) { show(player, view.entries(), view.onlyOpen(), view.page() - 1); return; }
        if (slot == 50) { show(player, view.entries(), view.onlyOpen(), view.page() + 1); return; }
        if (slot == 49) { open(player, !view.onlyOpen(), 0); return; }

        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] != slot) continue;
            int index = view.page() * PER_PAGE + i;
            if (index >= view.entries().size()) return;
            Entry entry = view.entries().get(index);
            if (!"OPEN".equals(entry.appeal().status)) return;
            if (!canDecide(player, entry.info())) {
                player.sendMessage(msg("no-permission"));
                return;
            }
            ClickType click = event.getClick();
            if (click.isShiftClick()) {
                pendingReduce.put(player.getUniqueId(), entry.appeal().id);
                player.closeInventory();
                player.sendMessage(msg("appeals-reduce-prompt").replace("%case%", String.valueOf(entry.appeal().caseId)));
            } else if (click.isLeftClick()) {
                decide(player, entry.appeal().id, AppealService.Decision.ACCEPT, "", view);
            } else if (click.isRightClick()) {
                decide(player, entry.appeal().id, AppealService.Decision.DENY, "", view);
            }
            return;
        }
    }

    /** Axentra.appeals, or Axentra.unban / Axentra.unmute for the case type. */
    private boolean canDecide(Player player, DatabaseManager.CaseInfo info) {
        if (player.hasPermission("Axentra.appeals")) return true;
        if (info == null) return false;
        return player.hasPermission(info.type.contains("MUTE") ? "Axentra.unmute" : "Axentra.unban");
    }

    private void decide(Player player, int appealId, AppealService.Decision decision, String extra, View view) {
        String name = player.getName();
        String uuid = player.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String result = appeals.decide(appealId, decision, extra, name, uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                player.sendMessage(msg("appeals-result").replace("%result%", ChatColor.stripColor(result)));
                if (view != null) open(player, view.onlyOpen(), view.page());
            });
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Integer appealId = pendingReduce.remove(event.getPlayer().getUniqueId());
        if (appealId == null) return;
        event.setCancelled(true);
        String input = event.getMessage().trim();
        Player player = event.getPlayer();
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(msg("appeals-reduce-cancelled"));
            return;
        }
        // Already async: decide reads and writes the database directly
        String result = appeals.decide(appealId, AppealService.Decision.REDUCE, input, player.getName(), player.getUniqueId().toString());
        player.sendMessage(msg("appeals-result").replace("%result%", ChatColor.stripColor(result)));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        View view = views.get(event.getPlayer().getUniqueId());
        if (view != null && view.inventory() == event.getInventory()) views.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingReduce.remove(event.getPlayer().getUniqueId());
        views.remove(event.getPlayer().getUniqueId());
    }

    private static String cut(String text, int max) {
        if (text == null) return "-";
        return text.length() > max ? text.substring(0, max - 1) + "…" : text;
    }

    private static List<String> wrap(String text, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : (text != null ? text : "").split("\\s+")) {
            if (line.length() + word.length() + 1 > width && line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
                if (lines.size() >= maxLines) { lines.set(maxLines - 1, lines.get(maxLines - 1) + "…"); return lines; }
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
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
