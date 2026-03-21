package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements CommandExecutor {

    private final App plugin;
    private static final int ENTRIES_PER_PAGE = 8;

    public HelpCommand(App plugin) {
        this.plugin = plugin;
    }

    private static class HelpEntry {
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
            entries.add(new HelpEntry("Admin", "/axentra reload",      "Reload config and messages"));
            entries.add(new HelpEntry("Admin", "/axentra upgrade",     "Check for updates"));
            entries.add(new HelpEntry("Admin", "/axentra information", "Show plugin information"));
        }

        // Moderation
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
        add(entries, sender, "Axentra.spawn",  "Teleport", "/spawn [player]", "Teleport to spawn");
        add(entries, sender, "Axentra.back",   "Teleport", "/back",           "Return to last location");
        add(entries, sender, "Axentra.tpa",    "Teleport", "/tpa <player>",   "Request teleport to a player");

        // Utility
        add(entries, sender, "Axentra.anvil",         "Utility", "/anvil",         "Open an anvil");
        add(entries, sender, "Axentra.workbench",     "Utility", "/workbench",     "Open a crafting table");
        add(entries, sender, "Axentra.cartography",   "Utility", "/cartography",   "Open a cartography table");
        add(entries, sender, "Axentra.grindstone",    "Utility", "/grindstone",    "Open a grindstone");
        add(entries, sender, "Axentra.loom",          "Utility", "/loom",          "Open a loom");
        add(entries, sender, "Axentra.smithingtable", "Utility", "/smithingtable", "Open a smithing table");
        add(entries, sender, "Axentra.stonecutter",   "Utility", "/stonecutter",   "Open a stonecutter");
        add(entries, sender, "Axentra.enderchest",    "Utility", "/enderchest",    "Open your ender chest");

        // World
        add(entries, sender, "Axentra.time",     "World", "/time <set|add> <value>",    "Set or add world time");
        add(entries, sender, "Axentra.weather",  "World", "/weather <clear|rain|storm>", "Set the weather");
        add(entries, sender, "Axentra.setspawn", "World", "/setspawn",                   "Set the world spawn");

        // General — always visible
        if (!sender.hasPermission("Axentra.admin"))
            entries.add(new HelpEntry("General", "/axentra information", "Show plugin information"));
        entries.add(new HelpEntry("General", "/help [page]", "Show this help menu"));

        return entries;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<HelpEntry> entries = buildEntries(sender);

        int totalPages = (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        if (page < 1 || page > totalPages) {
            sender.sendMessage(ChatColor.RED + "Page " + page + " does not exist. Use /help 1-" + totalPages + ".");
            return true;
        }

        int start = (page - 1) * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "   ---- Axentra Help (" + page + "/" + totalPages + ") ----");

        String prevSection = start > 0 ? entries.get(start - 1).section : null;
        for (int i = start; i < end; i++) {
            HelpEntry entry = entries.get(i);
            if (!entry.section.equals(prevSection)) {
                sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + " [" + entry.section + "]");
                prevSection = entry.section;
            }
            sender.sendMessage(ChatColor.AQUA + "  " + entry.usage + ChatColor.GRAY + " - " + entry.description);
        }

        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "  Next: " + ChatColor.AQUA + "/help " + (page + 1));
        }

        return true;
    }
}