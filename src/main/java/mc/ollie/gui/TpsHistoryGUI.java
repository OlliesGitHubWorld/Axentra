package mc.ollie.gui;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TPS history as a GUI: each content slot is a time block (oldest top-left, newest bottom-right),
 * coloured by its lowest TPS, with the stack size showing the average TPS.
 */
public class TpsHistoryGUI {

    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.AQUA + ChatColor.BOLD + "TPS History" + ChatColor.DARK_GRAY + " ✦";

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    // slot -> hours
    private static final int[] PERIOD_SLOTS = {46, 47, 48, 50, 51};
    private static final int[] PERIOD_HOURS = {1, 6, 12, 24, 72};

    private static final Map<UUID, TpsHistoryGUI> openGUIs = new HashMap<>();

    private final App plugin;

    public TpsHistoryGUI(App plugin) {
        this.plugin = plugin;
    }

    public static TpsHistoryGUI getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    public static void removeOpenGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }

    /** Loads the history async, then opens the GUI on the main thread. */
    public void open(Player player, int hours) {
        long now = System.currentTimeMillis();
        long since = now - hours * 3600000L;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.TpsHistoryEntry> entries =
                    plugin.getDatabaseManager().getTpsHistory(since, hours * 60 + 10);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                player.openInventory(build(entries, hours, since, now));
                openGUIs.put(player.getUniqueId(), this);
            });
        });
    }

    public void handleClick(Player player, int slot) {
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        for (int i = 0; i < PERIOD_SLOTS.length; i++) {
            if (PERIOD_SLOTS[i] == slot) {
                open(player, PERIOD_HOURS[i]);
                return;
            }
        }
    }

    private Inventory build(List<DatabaseManager.TpsHistoryEntry> entries, int hours, long since, long now) {
        Inventory inv = MenuHolder.create(54, GUI_TITLE);

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);
        ItemStack bar = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, bar);
        for (int i = 45; i < 54; i++) inv.setItem(i, bar);
        ItemStack side = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, side);
            inv.setItem(row * 9 + 8, side);
        }

        // Split the period into equal time blocks, oldest first
        int blocks = CONTENT_SLOTS.length;
        long blockMs = (now - since) / blocks;
        List<List<DatabaseManager.TpsHistoryEntry>> buckets = new ArrayList<>();
        for (int i = 0; i < blocks; i++) buckets.add(new ArrayList<>());
        for (DatabaseManager.TpsHistoryEntry e : entries) {
            int index = (int) ((e.time - since) / blockMs);
            if (index >= 0 && index < blocks) buckets.get(index).add(e);
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat(hours > 24 ? "dd-MM HH:mm" : "HH:mm");
        for (int i = 0; i < blocks; i++) {
            long from = since + i * blockMs;
            long to = from + blockMs;
            String range = timeFormat.format(new Date(from)) + " - " + timeFormat.format(new Date(to));
            inv.setItem(CONTENT_SLOTS[i], createBlockItem(buckets.get(i), range));
        }

        inv.setItem(4, createSummary(entries, hours));

        // Bottom bar: legend, period buttons, close
        inv.setItem(45, createItem(Material.BOOK, ChatColor.AQUA + "" + ChatColor.BOLD + "ℹ How to read",
                ChatColor.GRAY + "Each block is a piece of time,",
                ChatColor.GRAY + "oldest top-left, newest bottom-right.",
                "",
                ChatColor.GREEN + "■ " + ChatColor.GRAY + "Good  " + ChatColor.YELLOW + "■ " + ChatColor.GRAY + "Lag  "
                        + ChatColor.RED + "■ " + ChatColor.GRAY + "Heavy lag",
                ChatColor.DARK_GRAY + "■ " + ChatColor.GRAY + "No data (server offline)",
                "",
                ChatColor.GRAY + "Stack size = average TPS.",
                ChatColor.GRAY + "Colour = lowest TPS in that block.",
                ChatColor.GRAY + "Hover a block for details."));
        for (int i = 0; i < PERIOD_SLOTS.length; i++) {
            int h = PERIOD_HOURS[i];
            boolean selected = h == hours;
            String label = h < 24 ? h + (h == 1 ? " hour" : " hours") : (h / 24) + (h == 24 ? " day" : " days");
            ItemStack item = createItem(selected ? Material.CLOCK : Material.GRAY_DYE,
                    (selected ? ChatColor.AQUA : ChatColor.GRAY) + "" + ChatColor.BOLD + "Last " + label,
                    selected ? ChatColor.GREEN + "▸ Selected" : ChatColor.YELLOW + "▶ Click to view");
            if (selected) glow(item);
            inv.setItem(PERIOD_SLOTS[i], item);
        }
        inv.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "✕ Close",
                ChatColor.GRAY + "Close this menu."));
        return inv;
    }

    private ItemStack createBlockItem(List<DatabaseManager.TpsHistoryEntry> bucket, String range) {
        if (bucket.isEmpty()) {
            return createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + range,
                    ChatColor.GRAY + "No data recorded.");
        }

        double avgTps = bucket.stream().mapToDouble(e -> e.tps1m).average().orElse(20);
        double minTps = bucket.stream().mapToDouble(e -> e.tps1m).min().orElse(20);
        double avgMspt = bucket.stream().mapToDouble(e -> e.msptAvg).average().orElse(0);
        double maxMspt = bucket.stream().mapToDouble(e -> e.msptMax).max().orElse(0);
        double avgHealth = bucket.stream().mapToInt(e -> e.healthScore).average().orElse(0);
        int maxPlayers = bucket.stream().mapToInt(e -> e.onlinePlayers).max().orElse(0);
        DatabaseManager.TpsHistoryEntry last = bucket.get(0);
        for (DatabaseManager.TpsHistoryEntry e : bucket) if (e.time > last.time) last = e;

        Material material = minTps >= greenThreshold() ? Material.LIME_CONCRETE
                : minTps >= yellowThreshold() ? Material.YELLOW_CONCRETE : Material.RED_CONCRETE;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "TPS: " + tpsColor(avgTps) + String.format("%.1f", avgTps)
                + ChatColor.GRAY + " avg, lowest " + tpsColor(minTps) + String.format("%.1f", minTps));
        lore.add(ChatColor.GRAY + "MSPT: " + ChatColor.WHITE + String.format("%.1f", avgMspt)
                + ChatColor.GRAY + " avg, peak " + ChatColor.WHITE + String.format("%.1f", maxMspt));
        lore.add(ChatColor.GRAY + "Memory: " + ChatColor.WHITE + last.memoryUsed + "MB"
                + ChatColor.GRAY + " / " + last.memoryMax + "MB");
        lore.add(ChatColor.GRAY + "Health: " + ChatColor.WHITE + Math.round(avgHealth) + "/100");
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + maxPlayers + ChatColor.GRAY + " max");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "" + bucket.size() + " measurement" + (bucket.size() == 1 ? "" : "s"));

        ItemStack item = createItem(material, tpsColor(minTps) + "" + ChatColor.BOLD + range, lore);
        item.setAmount(Math.max(1, Math.min(20, (int) Math.round(avgTps))));
        return item;
    }

    private ItemStack createSummary(List<DatabaseManager.TpsHistoryEntry> entries, int hours) {
        String period = hours < 24 ? hours + "h" : (hours / 24) + "d";
        if (entries.isEmpty()) {
            return createItem(Material.NETHER_STAR, ChatColor.AQUA + "" + ChatColor.BOLD + "⏱ Last " + period,
                    ChatColor.GRAY + "No history data yet.",
                    ChatColor.GRAY + "Data is recorded every minute.");
        }

        double avgTps = entries.stream().mapToDouble(e -> e.tps1m).average().orElse(20);
        DatabaseManager.TpsHistoryEntry lowest = entries.stream()
                .min(Comparator.comparingDouble(e -> e.tps1m)).orElse(entries.get(0));
        double peakMspt = entries.stream().mapToDouble(e -> e.msptMax).max().orElse(0);
        double avgHealth = entries.stream().mapToInt(e -> e.healthScore).average().orElse(0);
        int maxPlayers = entries.stream().mapToInt(e -> e.onlinePlayers).max().orElse(0);
        long laggy = entries.stream().filter(e -> e.tps1m < yellowThreshold()).count();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Average TPS: " + tpsColor(avgTps) + String.format("%.1f", avgTps));
        lore.add(ChatColor.GRAY + "Lowest TPS: " + tpsColor(lowest.tps1m) + String.format("%.1f", lowest.tps1m)
                + ChatColor.DARK_GRAY + " (" + sdf.format(new Date(lowest.time)) + ")");
        lore.add(ChatColor.GRAY + "Peak MSPT: " + ChatColor.WHITE + String.format("%.1f", peakMspt));
        lore.add(ChatColor.GRAY + "Average health: " + ChatColor.WHITE + Math.round(avgHealth) + "/100");
        lore.add(ChatColor.GRAY + "Most players: " + ChatColor.WHITE + maxPlayers);
        lore.add("");
        lore.add(laggy == 0
                ? ChatColor.GREEN + "✔ No lag in this period"
                : ChatColor.RED + "⚠ " + laggy + " minute" + (laggy == 1 ? "" : "s") + " of heavy lag");

        ItemStack item = createItem(Material.NETHER_STAR, ChatColor.AQUA + "" + ChatColor.BOLD + "⏱ Last " + period, lore);
        return item;
    }

    private double greenThreshold() {
        return plugin.getFileManager().getHealth().getDouble("tps.green-threshold", 19.0);
    }

    private double yellowThreshold() {
        return plugin.getFileManager().getHealth().getDouble("tps.yellow-threshold", 15.0);
    }

    private ChatColor tpsColor(double tps) {
        if (tps >= greenThreshold()) return ChatColor.GREEN;
        if (tps >= yellowThreshold()) return ChatColor.YELLOW;
        return ChatColor.RED;
    }

    private void glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
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
