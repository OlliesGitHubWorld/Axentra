package mc.ollie.gui;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.listeners.ChatListener;
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

import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishGUI {

    private final App plugin;
    private final String targetName;

    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

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
        Inventory inv = Bukkit.createInventory(null, 54, title);

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
            String action = tmpl.getString("action", "warn").toLowerCase();
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
        placeTemplates(inv, templates, warnKeys, new int[]{11, 12, 13, 14, 15, 16});

        // Row 2: Mutes & Kicks
        inv.setItem(18, createCategory(Material.ORANGE_STAINED_GLASS_PANE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "🔇 MUTES & KICKS"));
        List<String> muteKickKeys = new ArrayList<>(muteKeys);
        muteKickKeys.addAll(kickKeys);
        placeTemplates(inv, templates, muteKickKeys, new int[]{20, 21, 22, 23, 24, 25});

        // Row 3: Bans
        inv.setItem(27, createCategory(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "" + ChatColor.BOLD + "🔨 BANS"));
        placeTemplates(inv, templates, banKeys, new int[]{29, 30, 31, 32, 33, 34});

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

    private void placeTemplates(Inventory inv, ConfigurationSection templates, List<String> keys, int[] slots) {
        for (int i = 0; i < Math.min(keys.size(), slots.length); i++) {
            String key = keys.get(i);
            ConfigurationSection tmpl = templates.getConfigurationSection(key);
            if (tmpl == null) continue;

            String displayName = ChatColor.translateAlternateColorCodes('&',
                    tmpl.getString("display-name", key));
            String action = tmpl.getString("action", "warn").toUpperCase();
            String reason = tmpl.getString("reason", "No reason specified");
            String duration = tmpl.getString("duration", "");
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

            String action = tmpl.getString("action", "warn").toLowerCase();
            String reason = tmpl.getString("reason", "Punishment applied");
            String durationStr = tmpl.getString("duration", "");

            staff.closeInventory();
            applyPunishment(staff, action, reason, durationStr);
        }
    }

    private void applyPunishment(Player staff, String action, String reason, String durationStr) {
        Player target = Bukkit.getPlayer(targetName);
        String uuid = target != null ? target.getUniqueId().toString()
                : Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();

        // Get chat snapshot
        String chatLog = null;
        if (target != null) {
            ChatListener chatListener = plugin.getChatListener();
            if (chatListener != null) {
                chatLog = chatListener.getChatSnapshot(target.getUniqueId());
            }
        }

        String staffName = staff.getName();
        String staffUuid = staff.getUniqueId().toString();
        Timestamp expiresAt = null;

        if (!durationStr.isEmpty()) {
            long millis = parseDuration(durationStr);
            if (millis > 0) {
                expiresAt = new Timestamp(System.currentTimeMillis() + millis);
            }
        }

        String caseType;

        String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");

        switch (action) {
            case "ban":
                caseType = "BAN";
                plugin.getDatabaseManager().banPlayer(targetName, uuid, reason, staffName, null);
                if (target != null) {
                    String banMsg = plugin.getFileManager().getMessages().getString("ban-message",
                            "&cYou have been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%\n&7Appeal: &f%appeal%");
                    banMsg = banMsg.replace("%reason%", reason).replace("%banned-by%", staffName);
                    if (appealLink.isEmpty()) {
                        banMsg = banMsg.replace("\n&7Appeal: &f%appeal%", "");
                    } else {
                        banMsg = banMsg.replace("%appeal%", appealLink);
                    }
                    target.kickPlayer(ChatColor.translateAlternateColorCodes('&', banMsg));
                }
                break;

            case "tempban":
                caseType = "TEMPBAN";
                plugin.getDatabaseManager().banPlayer(targetName, uuid, reason, staffName, expiresAt);
                if (target != null) {
                    String tempbanMsg = plugin.getFileManager().getMessages().getString("tempban-message",
                            "&cYou have been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%\n&7Appeal: &f%appeal%");
                    tempbanMsg = tempbanMsg.replace("%reason%", reason).replace("%duration%", durationStr).replace("%banned-by%", staffName);
                    if (appealLink.isEmpty()) {
                        tempbanMsg = tempbanMsg.replace("\n&7Appeal: &f%appeal%", "");
                    } else {
                        tempbanMsg = tempbanMsg.replace("%appeal%", appealLink);
                    }
                    target.kickPlayer(ChatColor.translateAlternateColorCodes('&', tempbanMsg));
                }
                break;

            case "mute":
                caseType = "MUTE";
                plugin.getDatabaseManager().mutePlayer(targetName, uuid, reason, staffName, null);
                if (target != null) {
                    String muteMsg = plugin.getFileManager().getMessages().getString("mute-message",
                            "%prefix%&cYou have been muted!\n&7Reason: &f%reason%");
                    String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
                    muteMsg = muteMsg.replace("%prefix%", prefix).replace("%reason%", reason);
                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', muteMsg));
                }
                break;

            case "tempmute":
                caseType = "TEMPMUTE";
                plugin.getDatabaseManager().mutePlayer(targetName, uuid, reason, staffName, expiresAt);
                if (target != null) {
                    String tempmuteMsg = plugin.getFileManager().getMessages().getString("tempmute-message",
                            "%prefix%&cYou have been temporarily muted!\n&7Reason: &f%reason%\n&7Duration: &f%duration%");
                    String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
                    tempmuteMsg = tempmuteMsg.replace("%prefix%", prefix).replace("%reason%", reason).replace("%duration%", durationStr);
                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', tempmuteMsg));
                }
                break;

            case "kick":
                caseType = "KICK";
                if (target != null) {
                    String kickMsg = plugin.getFileManager().getMessages().getString("kick-message",
                            "&cYou have been kicked!\n&7Reason: &f%reason%");
                    kickMsg = kickMsg.replace("%reason%", reason);
                    target.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMsg));
                }
                break;

            case "warn":
            default:
                caseType = "WARN";
                plugin.getDatabaseManager().warnPlayer(targetName, uuid, reason, staffName);
                if (target != null) {
                    int count = plugin.getDatabaseManager().getWarnCount(uuid);
                    String warnMsg = plugin.getFileManager().getMessages().getString("warn-message",
                            "%prefix%&eYou have been warned! &7Reason: &f%reason% &8(&e%count% total&8)");
                    String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
                    warnMsg = warnMsg.replace("%prefix%", prefix).replace("%reason%", reason).replace("%count%", String.valueOf(count));
                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', warnMsg));
                }
                break;
        }

        // Create case
        int caseId = plugin.getDatabaseManager().createCase(caseType, targetName, uuid, reason, staffName, expiresAt, chatLog);

        // Log audit
        plugin.getDatabaseManager().logAction(staffName, staffUuid, caseType, targetName,
                reason + (durationStr.isEmpty() ? "" : " (" + durationStr + ")"));

        staff.sendMessage(ChatColor.GREEN + "Applied " + ChatColor.WHITE + caseType.toLowerCase()
                + ChatColor.GREEN + " to " + ChatColor.WHITE + targetName
                + ChatColor.GREEN + " for: " + ChatColor.GRAY + reason
                + ChatColor.DARK_GRAY + " (Case #" + caseId + ")");
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

    private long parseDuration(String input) {
        Matcher matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.matches()) return -1;
        long days = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
        long hours = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
        long minutes = matcher.group(3) != null ? Long.parseLong(matcher.group(3)) : 0;
        long seconds = matcher.group(4) != null ? Long.parseLong(matcher.group(4)) : 0;
        long millis = (days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000L;
        return millis > 0 ? millis : -1;
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
