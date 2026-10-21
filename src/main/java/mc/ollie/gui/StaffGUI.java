package mc.ollie.gui;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StaffGUI {

    private final App plugin;

    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.AQUA + ChatColor.BOLD + "Axentra Settings" + ChatColor.DARK_GRAY + " ✦";

    public StaffGUI(App plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill everything with dark background
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bg);
        }

        // ═══════════════════════════════════════════
        // Row 0: Title bar
        // ═══════════════════════════════════════════
        ItemStack topBar = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) gui.setItem(i, topBar);
        gui.setItem(4, createItem(Material.NETHER_STAR, ChatColor.AQUA + "" + ChatColor.BOLD + "⚙ Staff Settings",
                ChatColor.GRAY + "Manage server settings in real-time.",
                ChatColor.GRAY + "Changes apply immediately."));

        // ═══════════════════════════════════════════
        // Row 1: Chat Control
        // ═══════════════════════════════════════════
        gui.setItem(9, createCategory(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "💬 CHAT"));

        boolean chatMuted = plugin.isChatMuted();
        gui.setItem(11, createToggle(Material.IRON_BARS, ChatColor.RED + "" + ChatColor.BOLD + "Mute Chat",
                chatMuted, "Prevent all non-staff from chatting."));

        int slowSeconds = plugin.getSlowChatSeconds();
        boolean slowActive = slowSeconds > 0;
        gui.setItem(12, createToggle(Material.CLOCK, ChatColor.GOLD + "" + ChatColor.BOLD + "Slow Chat",
                slowActive,
                slowActive ? "Cooldown: §e" + slowSeconds + "s" : "No cooldown active.",
                "",
                "§7Left-click: §ftoggle on/off",
                "§7Right-click: §fcycle duration"));

        gui.setItem(13, createAction(Material.PAPER, ChatColor.WHITE + "" + ChatColor.BOLD + "Clear Chat",
                "Clears chat for all non-staff.",
                "",
                ChatColor.YELLOW + "▶ Click to clear chat"));

        // ═══════════════════════════════════════════
        // Row 2: Announcements
        // ═══════════════════════════════════════════
        gui.setItem(18, createCategory(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "📢 ANNOUNCE"));

        boolean annKick = plugin.getFileManager().getSettings().getBoolean("announce-kick", true);
        gui.setItem(20, createToggle(Material.LEATHER_BOOTS, ChatColor.RED + "Kick",
                annKick, "Announce when a player is kicked."));

        boolean annBan = plugin.getFileManager().getSettings().getBoolean("announce-ban", true);
        gui.setItem(21, createToggle(Material.IRON_DOOR, ChatColor.DARK_RED + "Ban",
                annBan, "Announce when a player is banned."));

        boolean annUnban = plugin.getFileManager().getSettings().getBoolean("announce-unban", true);
        gui.setItem(22, createToggle(Material.OAK_DOOR, ChatColor.GREEN + "Unban",
                annUnban, "Announce when a player is unbanned."));

        boolean annMute = plugin.getFileManager().getSettings().getBoolean("announce-mute", true);
        gui.setItem(23, createToggle(Material.BARRIER, ChatColor.GOLD + "Mute",
                annMute, "Announce when a player is muted."));

        boolean annUnmute = plugin.getFileManager().getSettings().getBoolean("announce-unmute", true);
        gui.setItem(24, createToggle(Material.NOTE_BLOCK, ChatColor.GREEN + "Unmute",
                annUnmute, "Announce when a player is unmuted."));

        boolean annWarn = plugin.getFileManager().getSettings().getBoolean("announce-warn", true);
        gui.setItem(25, createToggle(Material.YELLOW_DYE, ChatColor.YELLOW + "Warn",
                annWarn, "Announce when a player is warned."));

        // ═══════════════════════════════════════════
        // Row 3: Gameplay Settings
        // ═══════════════════════════════════════════
        gui.setItem(27, createCategory(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "🎮 GAMEPLAY"));

        boolean spawnCountdown = plugin.getFileManager().getSettings().getBoolean("spawn-countdown", true);
        gui.setItem(29, createToggle(Material.COMPASS, ChatColor.AQUA + "Spawn Countdown",
                spawnCountdown, "Show countdown when using /spawn."));

        boolean tpaTitle = plugin.getFileManager().getSettings().getBoolean("tpa-title-countdown", true);
        gui.setItem(30, createToggle(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "TPA Countdown",
                tpaTitle, "Show title countdown for TPA."));

        boolean invseeReadOnly = plugin.getFileManager().getSettings().getBoolean("invsee-read-only", true);
        gui.setItem(31, createToggle(Material.CHEST, ChatColor.BLUE + "InvSee Read-Only",
                invseeReadOnly, "Prevent item changes in /invsee."));

        // ═══════════════════════════════════════════
        // Row 4: Vanish Settings
        // ═══════════════════════════════════════════
        gui.setItem(36, createCategory(Material.PURPLE_STAINED_GLASS_PANE,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "👻 VANISH"));

        boolean vanishFakeJQ = plugin.getFileManager().getSettings().getBoolean("vanish-fake-join-quit", true);
        gui.setItem(38, createToggle(Material.OAK_SIGN, ChatColor.GRAY + "Fake Join/Quit",
                vanishFakeJQ, "Fake leave/join messages for vanish."));

        boolean vanishGhost = plugin.getFileManager().getSettings().getBoolean("vanish-ghost-effect", true);
        gui.setItem(39, createToggle(Material.GLASS, ChatColor.WHITE + "Ghost Effect",
                vanishGhost, "Show ghost effect to staff."));

        boolean vanishNV = plugin.getFileManager().getSettings().getBoolean("vanish-night-vision", true);
        gui.setItem(40, createToggle(Material.GOLDEN_CARROT, ChatColor.GOLD + "Night Vision",
                vanishNV, "Night vision while vanished."));

        boolean vanishGod = plugin.getFileManager().getSettings().getBoolean("vanish-god-mode", true);
        gui.setItem(41, createToggle(Material.GOLDEN_APPLE, ChatColor.RED + "God Mode",
                vanishGod, "Invincibility while vanished."));

        boolean vanishPickup = plugin.getFileManager().getSettings().getBoolean("vanish-no-pickup", true);
        gui.setItem(42, createToggle(Material.HOPPER, ChatColor.DARK_AQUA + "No Pickup",
                vanishPickup, "Block item pickup while vanished."));

        // ═══════════════════════════════════════════
        // Row 5: Bottom bar + Close
        // ═══════════════════════════════════════════
        ItemStack botBar = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) gui.setItem(i, botBar);
        gui.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "✕ Close",
                ChatColor.GRAY + "Close this menu."));

        player.openInventory(gui);
    }

    private ItemStack createCategory(Material glass, String name) {
        ItemStack item = new ItemStack(glass);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "▸ Category"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggle(Material icon, String name, boolean enabled, String... descLines) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        for (String line : descLines) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");
        if (enabled) {
            lore.add(ChatColor.GREEN + "▸ ENABLED");
            lore.add(ChatColor.DARK_GRAY + "Click to disable");
        } else {
            lore.add(ChatColor.RED + "▸ DISABLED");
            lore.add(ChatColor.DARK_GRAY + "Click to enable");
        }
        meta.setLore(lore);

        // Enchant glow for enabled items
        if (enabled) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAction(Material icon, String name, String... descLines) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (descLines.length > 0) {
            List<String> lore = new ArrayList<>();
            for (String line : descLines) {
                lore.add(ChatColor.GRAY + line);
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) {
            meta.setLore(Arrays.asList(loreLines));
        }
        item.setItemMeta(meta);
        return item;
    }
}
