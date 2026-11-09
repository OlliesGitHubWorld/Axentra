package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.health.RestartManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class RestartCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public RestartCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.restart")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        RestartManager rm = plugin.getRestartManager();
        if (rm == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("lag-disabled"));
            return true;
        }

        if (args.length == 0) {
            showStatus(sender, rm);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "now":
                rm.scheduleRestart(10); // 10 second countdown
                sender.sendMessage(plugin.getMessageManager().getMessage("restart-now"));
                break;
            case "schedule":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("prefix") +
                            ChatColor.RED + "Usage: /restart schedule <seconds>");
                    return true;
                }
                try {
                    int seconds = parseTime(args[1]);
                    rm.scheduleRestart(seconds);
                    sender.sendMessage(plugin.getMessageManager().getMessage("restart-scheduled")
                            .replace("%time%", RestartManager.formatTime(seconds)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("prefix") +
                            ChatColor.RED + "Invalid time format. Use seconds or e.g. 5m, 1h.");
                }
                break;
            case "cancel":
                if (!rm.isRestarting()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("restart-no-scheduled"));
                    return true;
                }
                rm.cancelRestart();
                sender.sendMessage(plugin.getMessageManager().getMessage("restart-cancelled"));
                break;
            case "status":
                showStatus(sender, rm);
                break;
            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("prefix") +
                        ChatColor.RED + "Usage: /restart [now|schedule|cancel|status]");
                break;
        }
        return true;
    }

    private void showStatus(CommandSender sender, RestartManager rm) {
        if (rm.isRestarting()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("restart-status")
                    .replace("%time%", RestartManager.formatTime(rm.getSecondsRemaining())));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("restart-no-scheduled"));
        }
    }

    private int parseTime(String input) {
        input = input.toLowerCase();
        if (input.endsWith("m")) return Integer.parseInt(input.replace("m", "")) * 60;
        if (input.endsWith("h")) return Integer.parseInt(input.replace("h", "")) * 3600;
        if (input.endsWith("s")) return Integer.parseInt(input.replace("s", ""));
        return Integer.parseInt(input);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("Axentra.restart")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("now", "schedule", "cancel", "status"));
            subs.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("schedule")) {
            List<String> times = new ArrayList<>(Arrays.asList("30s", "60", "5m", "10m", "15m", "30m", "1h"));
            times.removeIf(s -> !s.startsWith(args[1].toLowerCase()));
            return times;
        }
        return Collections.emptyList();
    }
}
