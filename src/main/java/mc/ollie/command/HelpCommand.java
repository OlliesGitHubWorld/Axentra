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
    private static final int ENTRIES_PER_PAGE = 7;

    public HelpCommand(App plugin) {
        this.plugin = plugin;
    }

    /** A single line in the help list — either a section header or a command entry. */
    private static class HelpEntry {
        final boolean isHeader;
        final String usage;
        final String description;

        HelpEntry(String header) {
            this.isHeader = true;
            this.usage = header;
            this.description = null;
        }

        HelpEntry(String usage, String description) {
            this.isHeader = false;
            this.usage = usage;
            this.description = description;
        }
    }

    private void addHeader(List<HelpEntry> list, String title) {
        list.add(new HelpEntry(title));
    }

    private void addEntry(List<HelpEntry> list, CommandSender sender, String permission, String usage, String description) {
        if (sender.hasPermission(permission)) {
            list.add(new HelpEntry(usage, description));
        }
    }

    private List<HelpEntry> buildEntries(CommandSender sender) {
        List<HelpEntry> entries = new ArrayList<>();

        // Admin
        if (sender.hasPermission("Axentra.admin")) {
            addHeader(entries, "Admin");
            entries.add(new HelpEntry("/axentra reload",      "Reloads config and messages"));
            entries.add(new HelpEntry("/axentra upgrade",     "Checks for updates"));
            entries.add(new HelpEntry("/axentra information", "Shows plugin information"));
        }

        // Moderation
        List<HelpEntry> mod = new ArrayList<>();
        addEntry(mod, sender, "Axentra.warn",       "/warn <player> [reason]",                    "Warns a player");
        addEntry(mod, sender, "Axentra.warns",       "/warns [player]",                            "View a player's warnings");
        addEntry(mod, sender, "Axentra.clearwarns",  "/clearwarns <player>",                       "Clears a player's warnings");
        addEntry(mod, sender, "Axentra.mute",        "/mute <player> [reason]",                    "Mutes a player");
        addEntry(mod, sender, "Axentra.unmute",      "/unmute <player>",                           "Unmutes a player");
        addEntry(mod, sender, "Axentra.kick",        "/kick <player> [reason]",                    "Kicks a player");
        addEntry(mod, sender, "Axentra.ban",         "/ban <player> [reason]",                     "Permanently bans a player");
        addEntry(mod, sender, "Axentra.tempban",     "/tempban <player> <duration> [reason]",      "Temporarily bans a player");
        addEntry(mod, sender, "Axentra.banip",       "/banip <player|ip> [reason]",                "Bans an IP address");
        addEntry(mod, sender, "Axentra.tempbanip",   "/tempbanip <player|ip> <duration> [reason]", "Temporarily bans an IP");
        addEntry(mod, sender, "Axentra.unban",       "/unban <player>",                            "Unbans a player");
        addEntry(mod, sender, "Axentra.unbanip",     "/unbanip <ip>",                              "Unbans an IP address");
        if (!mod.isEmpty()) {
            addHeader(entries, "Moderation");
            entries.addAll(mod);
        }

        // Player
        List<HelpEntry> player = new ArrayList<>();
        addEntry(player, sender, "Axentra.fly",    "/fly [player]",   "Toggles flight");
        addEntry(player, sender, "Axentra.heal",   "/heal [player]",  "Heals a player");
        addEntry(player, sender, "Axentra.feed",   "/feed [player]",  "Feeds a player");
        addEntry(player, sender, "Axentra.clear",  "/clear [player]", "Clears inventory");
        addEntry(player, sender, "Axentra.repair", "/repair",         "Repairs held item");
        if (!player.isEmpty()) {
            addHeader(entries, "Player");
            entries.addAll(player);
        }

        // General (always visible)
        addHeader(entries, "General");
        if (!sender.hasPermission("Axentra.admin"))
            entries.add(new HelpEntry("/axentra information", "Shows plugin information"));
        entries.add(new HelpEntry("/help [page]", "Shows this menu"));

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

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "---- Axentra Help (" + page + "/" + totalPages + ") ----");

        for (int i = start; i < end; i++) {
            HelpEntry entry = entries.get(i);
            if (entry.isHeader) {
                sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "  [" + entry.usage + "]");
            } else {
                sender.sendMessage(ChatColor.AQUA + "  " + entry.usage + ChatColor.GRAY + " - " + entry.description);
            }
        }

        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "  Next page: " + ChatColor.AQUA + "/help " + (page + 1));
        }

        return true;
    }
}