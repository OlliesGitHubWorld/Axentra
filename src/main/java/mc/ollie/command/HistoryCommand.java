package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.gui.HistoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HistoryCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public HistoryCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
            for (String name : plugin.getDatabaseManager().getKnownPlayerNames()) {
                if (name.toLowerCase().startsWith(input) && !completions.contains(name)) {
                    completions.add(name);
                }
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.history")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("history-usage"));
            return true;
        }

        String playerName = args[0];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.CaseInfo> cases = plugin.getDatabaseManager().getCasesByPlayer(playerName);
            String discordName = null;
            if (!cases.isEmpty()) {
                DatabaseManager.DiscordLink link = plugin.getDatabaseManager().getLinkByUuid(cases.get(0).playerUuid);
                if (link != null) discordName = link.discordName;
            }
            String finalDiscord = discordName;
            Bukkit.getScheduler().runTask(plugin, () -> showHistory(sender, playerName, cases, finalDiscord));
        });
        return true;
    }

    private void showHistory(CommandSender sender, String playerName, List<DatabaseManager.CaseInfo> cases, String discordName) {
        if (!(sender instanceof Player)) {
            // Console output
            showConsoleHistory(sender, playerName, cases);
            return;
        }

        if (cases.isEmpty()) {
            sender.sendMessage(getMessage("history-empty").replace("%player%", playerName));
            return;
        }

        if (!((Player) sender).isOnline()) return;
        new HistoryGUI(plugin, playerName, cases).setDiscordName(discordName).open((Player) sender, 0);
    }

    private void showConsoleHistory(CommandSender sender, String playerName, List<DatabaseManager.CaseInfo> cases) {
        if (cases.isEmpty()) {
            sender.sendMessage(getMessage("history-empty").replace("%player%", playerName));
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "History for " + playerName + " (" + cases.size() + " cases)");
        for (DatabaseManager.CaseInfo c : cases) {
            String status = c.active ? ChatColor.GREEN + "Active" : ChatColor.RED + "Resolved";
            sender.sendMessage(ChatColor.GRAY + " #" + c.id + " " + ChatColor.WHITE + c.type
                    + ChatColor.GRAY + " - " + (c.reason != null ? c.reason : "No reason")
                    + " " + ChatColor.GRAY + "by " + c.issuedBy + " [" + status + ChatColor.GRAY + "]");
        }
    }
}
