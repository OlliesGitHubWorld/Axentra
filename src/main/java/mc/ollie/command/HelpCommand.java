package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpCommand implements CommandExecutor {

    private final App plugin;

    public HelpCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "---- Axentra Help ----");

        // Admin commands
        if (sender.hasPermission("Axentra.admin")) {
            sender.sendMessage(ChatColor.AQUA + "/axentra reload" + ChatColor.GRAY + " - Reloads config and messages");
            sender.sendMessage(ChatColor.AQUA + "/axentra upgrade" + ChatColor.GRAY + " - Checks for updates");
        }

        // Moderation
        if (sender.hasPermission("Axentra.kick"))
            sender.sendMessage(ChatColor.AQUA + "/kick <player> [reason]" + ChatColor.GRAY + " - Kicks a player");
        if (sender.hasPermission("Axentra.ban"))
            sender.sendMessage(ChatColor.AQUA + "/ban <player> [reason]" + ChatColor.GRAY + " - Bans a player");
        if (sender.hasPermission("Axentra.tempban"))
            sender.sendMessage(ChatColor.AQUA + "/tempban <player> <duration> [reason]" + ChatColor.GRAY + " - Temporarily bans a player");
        if (sender.hasPermission("Axentra.banip"))
            sender.sendMessage(ChatColor.AQUA + "/banip <player|ip> [reason]" + ChatColor.GRAY + " - Bans an IP address");
        if (sender.hasPermission("Axentra.tempbanip"))
            sender.sendMessage(ChatColor.AQUA + "/tempbanip <player|ip> <duration> [reason]" + ChatColor.GRAY + " - Temporarily bans an IP address");
        if (sender.hasPermission("Axentra.unban"))
            sender.sendMessage(ChatColor.AQUA + "/unban <player>" + ChatColor.GRAY + " - Unbans a player");
        if (sender.hasPermission("Axentra.unbanip"))
            sender.sendMessage(ChatColor.AQUA + "/unbanip <ip>" + ChatColor.GRAY + " - Unbans an IP address");

        // Player
        if (sender.hasPermission("Axentra.fly"))
            sender.sendMessage(ChatColor.AQUA + "/fly [player]" + ChatColor.GRAY + " - Toggles flight");
        if (sender.hasPermission("Axentra.heal"))
            sender.sendMessage(ChatColor.AQUA + "/heal [player]" + ChatColor.GRAY + " - Heals a player");
        if (sender.hasPermission("Axentra.feed"))
            sender.sendMessage(ChatColor.AQUA + "/feed [player]" + ChatColor.GRAY + " - Feeds a player");
        if (sender.hasPermission("Axentra.clear"))
            sender.sendMessage(ChatColor.AQUA + "/clear [player]" + ChatColor.GRAY + " - Clears inventory");
        if (sender.hasPermission("Axentra.repair"))
            sender.sendMessage(ChatColor.AQUA + "/repair" + ChatColor.GRAY + " - Repairs held item");

        // Always visible
        sender.sendMessage(ChatColor.AQUA + "/axentra information" + ChatColor.GRAY + " - Shows plugin information");
        sender.sendMessage(ChatColor.AQUA + "/axentra help" + ChatColor.GRAY + " - Shows this menu");

        return true;
    }
}