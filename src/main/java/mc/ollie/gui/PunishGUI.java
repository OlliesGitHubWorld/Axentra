package mc.ollie.gui;

import mc.ollie.App;
import mc.ollie.punish.PunishmentService;
import mc.ollie.punish.PunishmentService.Step;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PunishGUI {

    private final App plugin;
    private final String targetName;


    private static final Map<String, PunishGUI> activeGUIs = new HashMap<>();

    // Slots where punishment items are placed (skipping borders/labels)
    private final List<String> templateKeys = new ArrayList<>();

    public PunishGUI(App plugin, String targetName) {
        this.plugin = plugin;
        this.targetName = targetName;
    }

    public String getTargetName() {
        return targetName;
    }

    public static PunishGUI getActiveGUI(String title) {
        return activeGUIs.get(title);
    }

    public static void removeActiveGUI(String title) {
        activeGUIs.remove(title);
    }

    public void open(Player staff) {
        FileConfiguration punishConfig = plugin.getFileManager().getPunishments();
        if (punishConfig == null) {
            staff.sendMessage(ChatColor.RED + "punishments.yml not found!");
            return;
        }

        ConfigurationSection templates = punishConfig.getConfigurationSection("templates");
        if (templates == null || templates.getKeys(false).isEmpty()) {
            staff.sendMessage(ChatColor.RED + "No punishment templates configured!");
            return;
        }

        String title = ChatColor.DARK_GRAY + "✦ " + ChatColor.RED + ChatColor.BOLD + "Punish" + ChatColor.DARK_GRAY + " ✦ " + ChatColor.RESET + targetName;
        Inventory inv = MenuHolder.create(54, title);

        // Fill everything with dark background
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        // ═══════════════════════════════════════════
        // Row 0: Top bar
        // ═══════════════════════════════════════════
        ItemStack topBar = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, topBar);

        // Earlier cases per reason, used to pick the escalation step of each template
        Map<String, Integer> offenses = plugin.getDatabaseManager().getCaseCountsByReason(resolveUuid());

        // Player head with info
        int caseCount = plugin.getDatabaseManager().getCaseCountByPlayer(targetName);
        int warnCount = plugin.getDatabaseManager().getWarnCountByName(targetName);
        boolean isBanned = plugin.getDatabaseManager().isBannedByName(targetName);
        boolean isMuted = plugin.getDatabaseManager().isMutedByName(targetName);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetName));
            skullMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "⚠ " + targetName);
            List<String> headLore = new ArrayList<>();
            headLore.add("");
            headLore.add(ChatColor.GRAY + "Cases: " + ChatColor.WHITE + caseCount);
            headLore.add(ChatColor.GRAY + "Warnings: " + ChatColor.WHITE + warnCount);
            headLore.add(ChatColor.GRAY + "Banned: " + (isBanned ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
            headLore.add(ChatColor.GRAY + "Muted: " + (isMuted ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
            headLore.add("");
            headLore.add(ChatColor.DARK_GRAY + "Select a punishment below.");
            skullMeta.setLore(headLore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(4, head);

        // ═══════════════════════════════════════════
        // Group templates by category based on action
        // ═══════════════════════════════════════════
        // We'll organize: Row 1 = Chat (warns/mutes), Row 2 = Gameplay (bans), Row 3 = General

        List<String> warnKeys = new ArrayList<>();
        List<String> muteKeys = new ArrayList<>();
        List<String> kickKeys = new ArrayList<>();
        List<String> banKeys = new ArrayList<>();

        for (String key : templates.getKeys(false)) {
            ConfigurationSection tmpl = templates.getConfigurationSection(key);
            if (tmpl == null) continue;
            String action = getSteps(tmpl).get(0).action;
            switch (action) {
                case "warn":     warnKeys.add(key); break;
                case "mute":
                case "tempmute": muteKeys.add(key); break;
                case "kick":     kickKeys.add(key); break;
                case "ban":
                case "tempban":  banKeys.add(key); break;
                default:         warnKeys.add(key); break;
            }
        }

        templateKeys.clear();

        // Row 1: Warnings
        inv.setItem(9, createCategory(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "⚠ WARNINGS"));
        placeTemplates(inv, templates, warnKeys, new int[]{11, 12, 13, 14, 15, 16}, offenses);

        // Row 2: Mutes & Kicks
        inv.setItem(18, createCategory(Material.ORANGE_STAINED_GLASS_PANE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "🔇 MUTES & KICKS"));
        List<String> muteKickKeys = new ArrayList<>(muteKeys);
        muteKickKeys.addAll(kickKeys);
        placeTemplates(inv, templates, muteKickKeys, new int[]{20, 21, 22, 23, 24, 25}, offenses);

        // Row 3: Bans
        inv.setItem(27, createCategory(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "" + ChatColor.BOLD + "🔨 BANS"));
        placeTemplates(inv, templates, banKeys, new int[]{29, 30, 31, 32, 33, 34}, offenses);

        // ═══════════════════════════════════════════
        // Row 4: Player history shortcut
        // ═══════════════════════════════════════════
        inv.setItem(36, createCategory(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "📋 QUICK ACTIONS"));

        inv.setItem(38, createAction(Material.BOOK, ChatColor.AQUA + "" + ChatColor.BOLD + "View History",
                ChatColor.GRAY + "View " + targetName + "'s full history.",
                "",
                ChatColor.YELLOW + "▶ Click to open"));

        inv.setItem(39, createAction(Material.WRITABLE_BOOK, ChatColor.AQUA + "" + ChatColor.BOLD + "View Notes",
                ChatColor.GRAY + "View staff notes for " + targetName + ".",
                "",
                ChatColor.YELLOW + "▶ Click to open"));

        // ═══════════════════════════════════════════
        // Row 5: Bottom bar + Close
        // ═══════════════════════════════════════════
        ItemStack botBar = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, botBar);
        inv.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "✕ Close",
                ChatColor.GRAY + "Close this menu."));

        activeGUIs.put(title, this);
        staff.openInventory(inv);
    }

    // Steps and escalation live in PunishmentService (v1.9.0), shared with Discord moderation
    private static List<Step> getSteps(ConfigurationSection tmpl) {
        return PunishmentService.getSteps(tmpl);
    }

    private static int stepIndex(List<Step> steps, Map<String, Integer> offenses, String reason) {
        return PunishmentService.stepIndex(steps, offenses, reason);
    }

    private String resolveUuid() {
        Player target = Bukkit.getPlayer(targetName);
        return target != null ? target.getUniqueId().toString()
                : Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();
    }

    private void placeTemplates(Inventory inv, ConfigurationSection templates, List<String> keys, int[] slots,
                                Map<String, Integer> offenses) {
        for (int i = 0; i < Math.min(keys.size(), slots.length); i++) {
            String key = keys.get(i);
            ConfigurationSection tmpl = templates.getConfigurationSection(key);
            if (tmpl == null) continue;

            String displayName = ChatColor.translateAlternateColorCodes('&',
                    tmpl.getString("display-name", key));
            String reason = tmpl.getString("reason", "No reason specified");
            List<Step> steps = getSteps(tmpl);
            int current = stepIndex(steps, offenses, reason);
            String action = steps.get(current).action.toUpperCase();
            String duration = steps.get(current).duration;
            String materialName = tmpl.getString("icon", "PAPER");

            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.PAPER;
            }

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Action: " + getActionColor(action) + action);
            lore.add(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + reason);
            if (!duration.isEmpty()) {
                lore.add(ChatColor.GRAY + "Duration: " + ChatColor.WHITE + duration);
            }
            if (steps.size() > 1) {
                int previous = offenses.getOrDefault(reason, 0);
                lore.add("");
                lore.add(ChatColor.GRAY + "Offense " + ChatColor.WHITE + "#" + (previous + 1)
                        + ChatColor.DARK_GRAY + " (" + previous + " earlier)");
                for (int s = 0; s < steps.size(); s++) {
                    boolean isCurrent = s == current;
                    String prefix = isCurrent ? ChatColor.GREEN + " ▶ " : ChatColor.DARK_GRAY + "   ";
                    String suffix = s == steps.size() - 1 ? "+" : "";
                    lore.add(prefix + (s + 1) + suffix + ". "
                            + (isCurrent ? getActionColor(steps.get(s).action) : ChatColor.DARK_GRAY.toString())
                            + steps.get(s).label());
                }
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Click to apply");

            inv.setItem(slots[i], createItem(material, displayName, lore));

            // Track which template key is at which slot
            while (templateKeys.size() <= slots[i]) templateKeys.add(null);
            templateKeys.set(slots[i], key);
        }
    }

    public void handleClick(Player staff, int slot) {
        // Close button
        if (slot == 49) {
            staff.closeInventory();
            return;
        }

        // History shortcut
        if (slot == 38) {
            staff.closeInventory();
            staff.performCommand("history " + targetName);
            return;
        }

        // Notes shortcut
        if (slot == 39) {
            staff.closeInventory();
            staff.performCommand("notes " + targetName);
            return;
        }

        // Check if it's a template slot
        if (slot >= 0 && slot < templateKeys.size() && templateKeys.get(slot) != null) {
            String key = templateKeys.get(slot);
            FileConfiguration punishConfig = plugin.getFileManager().getPunishments();
            ConfigurationSection templates = punishConfig.getConfigurationSection("templates");
            if (templates == null) return;

            ConfigurationSection tmpl = templates.getConfigurationSection(key);
            if (tmpl == null) return;

            String reason = tmpl.getString("reason", "Punishment applied");
            // Recount on click so two staff punishing at once both escalate correctly
            List<Step> steps = getSteps(tmpl);
            Step step = steps.get(stepIndex(steps,
                    plugin.getDatabaseManager().getCaseCountsByReason(resolveUuid()), reason));

            staff.closeInventory();
            applyPunishment(staff, step.action, reason, step.duration);
        }
    }

    private void applyPunishment(Player staff, String action, String reason, String durationStr) {
        String staffName = staff.getName();
        String staffUuid = staff.getUniqueId().toString();
        plugin.getPunishmentService().apply(targetName, null, action, reason, durationStr, staffName, staffUuid,
                "INGAME", result -> {
                    if (!staff.isOnline()) return;
                    staff.sendMessage(ChatColor.GREEN + "Applied " + ChatColor.WHITE + result.caseType().toLowerCase()
                            + ChatColor.GREEN + " to " + ChatColor.WHITE + targetName
                            + ChatColor.GREEN + " for: " + ChatColor.GRAY + reason
                            + ChatColor.DARK_GRAY + " (Case #" + result.caseId() + ")");
                });
    }

    private String getActionColor(String action) {
        switch (action.toUpperCase()) {
            case "WARN":     return ChatColor.YELLOW.toString();
            case "MUTE":
            case "TEMPMUTE": return ChatColor.GOLD.toString();
            case "KICK":     return ChatColor.LIGHT_PURPLE.toString();
            case "BAN":
            case "TEMPBAN":  return ChatColor.RED.toString();
            default:         return ChatColor.GRAY.toString();
        }
    }


    private ItemStack createCategory(Material glass, String name) {
        ItemStack item = new ItemStack(glass);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "▸ Category"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAction(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines.length > 0) {
                meta.setLore(Arrays.asList(loreLines));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines.length > 0) {
                meta.setLore(Arrays.asList(loreLines));
            }
            item.setItemMeta(meta);
        }
        return item;
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
