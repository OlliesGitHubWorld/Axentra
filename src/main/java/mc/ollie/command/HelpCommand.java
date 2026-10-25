package mc.ollie.command;

import mc.ollie.App;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class HelpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private static final int ENTRIES_PER_PAGE = 8;

    private static final String LINE_COLOR = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH;
    private static final String LINE = "                                                ";

    public HelpCommand(App plugin) {
        this.plugin = plugin;
    }

    static class HelpEntry {
        final String section;
        final String usage;
        final String description;

        HelpEntry(String section, String usage, String description) {
            this.section = section;
            this.usage = usage;
            this.description = description;
        }
    }

    private void add(List<HelpEntry> list, CommandSender sender, String permission,
                     String section, String usage, String description) {
        if (sender.hasPermission(permission)) {
            list.add(new HelpEntry(section, usage, description));
        }
    }

    private List<HelpEntry> buildEntries(CommandSender sender) {
        List<HelpEntry> entries = new ArrayList<>();

        // Admin
        if (sender.hasPermission("Axentra.admin")) {
            entries.add(new HelpEntry("Admin", "/axentra reload", "Reload config and messages"));
            entries.add(new HelpEntry("Admin", "/axentra upgrade", "Check for updates"));
            entries.add(new HelpEntry("Admin", "/axentra information", "Show plugin information"));
            entries.add(new HelpEntry("Admin", "/axentra settings", "Open settings GUI"));
        }

        // Moderation
        add(entries, sender, "Axentra.punish",        "Moderation", "/punish <player>",                           "Open punishment GUI");
        add(entries, sender, "Axentra.warn",          "Moderation", "/warn <player> [reason]",                    "Warn a player");
        add(entries, sender, "Axentra.warns",         "Moderation", "/warns [player]",                            "View a player's warnings");
        add(entries, sender, "Axentra.clearwarns",    "Moderation", "/clearwarns <player>",                       "Clear a player's warnings");
        add(entries, sender, "Axentra.mute",          "Moderation", "/mute <player> [reason]",                    "Mute a player");
        add(entries, sender, "Axentra.unmute",        "Moderation", "/unmute <player>",                           "Unmute a player");
        add(entries, sender, "Axentra.kick",          "Moderation", "/kick <player> [reason]",                    "Kick a player");
        add(entries, sender, "Axentra.ban",           "Moderation", "/ban <player> [reason]",                     "Permanently ban a player");
        add(entries, sender, "Axentra.tempban",       "Moderation", "/tempban <player> <duration> [reason]",      "Temporarily ban a player");
        add(entries, sender, "Axentra.banip",         "Moderation", "/banip <player|ip> [reason]",                "Ban an IP address");
        add(entries, sender, "Axentra.tempbanip",     "Moderation", "/tempbanip <player|ip> <duration> [reason]", "Temporarily ban an IP");
        add(entries, sender, "Axentra.unban",         "Moderation", "/unban <player>",                            "Unban a player");
        add(entries, sender, "Axentra.unbanip",       "Moderation", "/unbanip <ip>",                              "Unban an IP address");
        add(entries, sender, "Axentra.invsee",        "Moderation", "/invsee <player>",                           "View a player's inventory");
        add(entries, sender, "Axentra.enderchestsee", "Moderation", "/enderchestsee <player>",                    "View a player's ender chest");
        add(entries, sender, "Axentra.case",          "Moderation", "/case <id>",                                 "View case details");
        add(entries, sender, "Axentra.history",       "Moderation", "/history <player>",                          "View moderation history");
        add(entries, sender, "Axentra.note",          "Moderation", "/note add <player> <note>",                  "Add a staff note");
        add(entries, sender, "Axentra.note",          "Moderation", "/notes <player>",                            "View staff notes");
        add(entries, sender, "Axentra.auditlog",      "Moderation", "/auditlog <staff>",                          "View staff audit log");

        // Player
        add(entries, sender, "Axentra.fly",      "Player", "/fly [player]",       "Toggle flight");
        add(entries, sender, "Axentra.heal",     "Player", "/heal [player]",      "Heal a player");
        add(entries, sender, "Axentra.feed",     "Player", "/feed [player]",      "Feed a player");
        add(entries, sender, "Axentra.god",      "Player", "/god [player]",       "Toggle god mode");
        add(entries, sender, "Axentra.vanish",   "Player", "/vanish [player]",    "Toggle vanish");
        add(entries, sender, "Axentra.speed",    "Player", "/speed <1-10|reset>", "Set walk/fly speed");
        add(entries, sender, "Axentra.gamemode", "Player", "/gamemode <mode>",    "Change gamemode");
        add(entries, sender, "Axentra.clear",    "Player", "/clear [player]",     "Clear inventory");
        add(entries, sender, "Axentra.repair",   "Player", "/repair",             "Repair held item");
        add(entries, sender, "Axentra.hat",      "Player", "/hat",                "Wear item as a hat");
        add(entries, sender, "Axentra.ping",     "Player", "/ping [player]",      "Check ping");
        add(entries, sender, "Axentra.suicide",  "Player", "/suicide",            "Kill yourself");

        // Teleport
        add(entries, sender, "Axentra.spawn", "Teleport", "/spawn [player]", "Teleport to spawn");
        add(entries, sender, "Axentra.back",  "Teleport", "/back",           "Return to last location");
        add(entries, sender, "Axentra.tpa",   "Teleport", "/tpa <player>",   "Request teleport to a player");

        // Utility
        add(entries, sender, "Axentra.anvil",         "Utility", "/anvil",         "Open an anvil");
        add(entries, sender, "Axentra.workbench",     "Utility", "/workbench",     "Open a crafting table");
        add(entries, sender, "Axentra.cartography",   "Utility", "/cartography",   "Open a cartography table");
        add(entries, sender, "Axentra.grindstone",    "Utility", "/grindstone",    "Open a grindstone");
        add(entries, sender, "Axentra.loom",          "Utility", "/loom",          "Open a loom");
        add(entries, sender, "Axentra.smithingtable", "Utility", "/smithingtable", "Open a smithing table");
        add(entries, sender, "Axentra.stonecutter",   "Utility", "/stonecutter",   "Open a stonecutter");
        add(entries, sender, "Axentra.enderchest",    "Utility", "/enderchest",    "Open your ender chest");

        // Protection
        add(entries, sender, "Axentra.restoreinv",  "Protection", "/restoreinv <player>",             "View/restore inventory snapshots");
        add(entries, sender, "Axentra.trust",        "Protection", "/trust <player>",                  "View player trust level");
        add(entries, sender, "Axentra.trust.set",    "Protection", "/trust set <player> <level>",      "Set player trust level");
        add(entries, sender, "Axentra.alts",         "Protection", "/alts <player>",                   "View alt accounts");
        add(entries, sender, "Axentra.freeze",       "Protection", "/freeze <player>",                 "Freeze/unfreeze a player");
        add(entries, sender, "Axentra.timeline",     "Protection", "/timeline <player> [filter]",      "View player timeline");

        // Homes & Teleport
        add(entries, sender, "Axentra.sethome",  "Homes & Teleport", "/sethome [name]",     "Set a home");
        add(entries, sender, "Axentra.home",     "Homes & Teleport", "/home [name]",        "Teleport to a home");
        add(entries, sender, "Axentra.delhome",  "Homes & Teleport", "/delhome <name>",     "Delete a home");
        add(entries, sender, "Axentra.homes",    "Homes & Teleport", "/homes [player]",     "View homes GUI");
        add(entries, sender, "Axentra.tp",       "Homes & Teleport", "/tp <player>",        "Teleport to a player");
        add(entries, sender, "Axentra.tphere",   "Homes & Teleport", "/tphere <player>",    "Teleport player to you");
        add(entries, sender, "Axentra.tpall",    "Homes & Teleport", "/tpall",              "Teleport all players to you");
        add(entries, sender, "Axentra.top",      "Homes & Teleport", "/top",                "Teleport to highest block");

        // World
        add(entries, sender, "Axentra.time",     "World", "/time <set|add> <value>",    "Set or add world time");
        add(entries, sender, "Axentra.weather",  "World", "/weather <clear|rain|storm>", "Set the weather");
        add(entries, sender, "Axentra.setspawn", "World", "/setspawn",                   "Set the world spawn");

        return entries;
    }

    private Map<String, List<HelpEntry>> groupBySection(List<HelpEntry> entries) {
        LinkedHashMap<String, List<HelpEntry>> grouped = new LinkedHashMap<>();
        for (HelpEntry entry : entries) {
            grouped.computeIfAbsent(entry.section, k -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<HelpEntry> entries = buildEntries(sender);
        Map<String, List<HelpEntry>> grouped = groupBySection(entries);

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>(grouped.keySet());
            return suggestions.stream()
                    .map(String::toLowerCase)
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<HelpEntry> entries = buildEntries(sender);
        Map<String, List<HelpEntry>> grouped = groupBySection(entries);

        // /help - show category overview
        if (args.length == 0) {
            showCategoryOverview(sender, grouped);
            return true;
        }

        // /help <page_number> - also show category overview (legacy compat)
        try {
            int page = Integer.parseInt(args[0]);
            showCategoryOverview(sender, grouped);
            return true;
        } catch (NumberFormatException ignored) {}

        // /help <category> [page]
        String categoryInput = args[0].toLowerCase();
        String matchedCategory = null;
        for (String cat : grouped.keySet()) {
            if (cat.toLowerCase().equals(categoryInput)) {
                matchedCategory = cat;
                break;
            }
        }

        if (matchedCategory == null) {
            sender.sendMessage(ChatColor.RED + "Unknown category '" + args[0] + "'. Use /help to see all categories.");
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        showCategoryPage(sender, matchedCategory, grouped.get(matchedCategory), page);
        return true;
    }

    private void showCategoryOverview(CommandSender sender, Map<String, List<HelpEntry>> grouped) {
        sender.sendMessage(LINE_COLOR + LINE);
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  AXENTRA HELP");
        sender.sendMessage(ChatColor.GRAY + "  Select a category to view commands.");
        sender.sendMessage("");

        for (Map.Entry<String, List<HelpEntry>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            int count = entry.getValue().size();
            String icon = getCategoryIcon(category);

            if (sender instanceof Player) {
                TextComponent line = new TextComponent("  ");

                TextComponent categoryText = new TextComponent(icon + " " + category);
                categoryText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                categoryText.setBold(true);
                categoryText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category.toLowerCase()));
                categoryText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(ChatColor.YELLOW + "Click to view " + category + " commands")));

                TextComponent countText = new TextComponent(" (" + count + ")");
                countText.setColor(net.md_5.bungee.api.ChatColor.GRAY);

                line.addExtra(categoryText);
                line.addExtra(countText);
                ((Player) sender).spigot().sendMessage(line);
            } else {
                sender.sendMessage(ChatColor.AQUA + "  " + icon + " " + ChatColor.BOLD + category
                        + ChatColor.GRAY + " (" + count + ")");
            }
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Tip: " + ChatColor.WHITE + "/help <category>" + ChatColor.GRAY + " to view commands");
        sender.sendMessage(LINE_COLOR + LINE);
    }

    private void showCategoryPage(CommandSender sender, String category, List<HelpEntry> entries, int page) {
        int totalPages = (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (page < 1 || page > totalPages) {
            sender.sendMessage(ChatColor.RED + "Page " + page + " does not exist. (1-" + totalPages + ")");
            return;
        }

        int start = (page - 1) * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        String icon = getCategoryIcon(category);

        sender.sendMessage(LINE_COLOR + LINE);
        if (totalPages > 1) {
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  " + icon + " " + category.toUpperCase()
                    + ChatColor.GRAY + " (Page " + page + "/" + totalPages + ")");
        } else {
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  " + icon + " " + category.toUpperCase());
        }
        sender.sendMessage("");

        for (int i = start; i < end; i++) {
            HelpEntry entry = entries.get(i);

            if (sender instanceof Player) {
                TextComponent line = new TextComponent("  ");

                TextComponent usageText = new TextComponent(entry.usage);
                usageText.setColor(net.md_5.bungee.api.ChatColor.AQUA);

                String baseCommand = entry.usage.split(" ")[0];
                usageText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, baseCommand + " "));
                usageText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(ChatColor.YELLOW + entry.description + "\n" + ChatColor.GRAY + "Click to use")));

                TextComponent descText = new TextComponent(" - " + entry.description);
                descText.setColor(net.md_5.bungee.api.ChatColor.GRAY);

                line.addExtra(usageText);
                line.addExtra(descText);
                ((Player) sender).spigot().sendMessage(line);
            } else {
                sender.sendMessage(ChatColor.AQUA + "  " + entry.usage + ChatColor.GRAY + " - " + entry.description);
            }
        }

        sender.sendMessage("");

        // Navigation bar
        if (sender instanceof Player && totalPages > 1) {
            TextComponent nav = new TextComponent("  ");

            if (page > 1) {
                TextComponent prev = new TextComponent("[< Previous]");
                prev.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category.toLowerCase() + " " + (page - 1)));
                prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Go to page " + (page - 1))));
                nav.addExtra(prev);
                nav.addExtra(new TextComponent(" "));
            }

            TextComponent back = new TextComponent("[Categories]");
            back.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
            back.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help"));
            back.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Back to categories")));
            nav.addExtra(back);

            if (page < totalPages) {
                nav.addExtra(new TextComponent(" "));
                TextComponent next = new TextComponent("[Next >]");
                next.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category.toLowerCase() + " " + (page + 1)));
                next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Go to page " + (page + 1))));
                nav.addExtra(next);
            }

            ((Player) sender).spigot().sendMessage(nav);
        } else if (sender instanceof Player) {
            TextComponent nav = new TextComponent("  ");
            TextComponent back = new TextComponent("[< Back to categories]");
            back.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
            back.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help"));
            back.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Back to categories")));
            nav.addExtra(back);
            ((Player) sender).spigot().sendMessage(nav);
        }

        sender.sendMessage(LINE_COLOR + LINE);
    }

    private String getCategoryIcon(String category) {
        switch (category) {
            case "Admin":      return "\u2699";  // gear
            case "Moderation": return "\u2694";  // swords
            case "Player":     return "\u2764";  // heart
            case "Teleport":   return "\u2716";  // compass-like
            case "Utility":    return "\u2692";  // hammer/pick
            case "Protection": return "\u2726";  // sparkle
            case "Homes & Teleport": return "\u2302"; // house
            case "World":      return "\u2600";  // sun
            default:           return "\u25CF";  // bullet
        }
    }
}
