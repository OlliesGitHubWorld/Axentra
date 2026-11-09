package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.health.MaintenanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.text.SimpleDateFormat;
import java.util.*;

public class MaintenanceCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public MaintenanceCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.maintenance")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        MaintenanceManager mm = plugin.getMaintenanceManager();
        if (mm == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("lag-disabled"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                if (mm.isActive() || mm.isPending()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-already-on"));
                    return true;
                }
                // /maintenance on [duration] [-c <countdown>] [reason...]
                // duration is only taken from the first argument; -c may appear anywhere
                int duration = 0;
                int countdown = -1;
                List<String> reasonParts = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    String arg = args[i];
                    if ((arg.equalsIgnoreCase("-c") || arg.equalsIgnoreCase("--countdown")) && i + 1 < args.length) {
                        try {
                            String value = args[++i];
                            // plain number = seconds for the countdown
                            countdown = value.matches("\\d+") ? Integer.parseInt(value) : parseSeconds(value);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-invalid-time")
                                    .replace("%input%", args[i]));
                            return true;
                        }
                        continue;
                    }
                    if (i == 1) {
                        try {
                            duration = parseSeconds(arg) / 60;
                            continue;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    reasonParts.add(arg);
                }
                String reason = reasonParts.isEmpty() ? null : String.join(" ", reasonParts);
                mm.start(sender.getName(), duration, reason, countdown);
                if (!mm.isPending()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-on"));
                }
                break;

            case "off":
                if (mm.isPending()) {
                    mm.cancelCountdown();
                    return true;
                }
                if (!mm.isActive()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-already-off"));
                    return true;
                }
                mm.disable();
                sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-off"));
                break;

            case "status":
                if (mm.isActive()) {
                    String since = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(mm.getActivatedAt()));
                    sender.sendMessage(mm.format(plugin.getMessageManager().getMessage("maintenance-status-on")
                            .replace("%since%", since)));
                } else {
                    sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-status-off"));
                }
                break;

            case "add":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-usage"));
                    return true;
                }
                mm.addToWhitelist(args[1]);
                sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-added")
                        .replace("%player%", args[1]));
                break;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-usage"));
                    return true;
                }
                mm.removeFromWhitelist(args[1]);
                sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-removed")
                        .replace("%player%", args[1]));
                break;

            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("maintenance-usage"));
                break;
        }
        return true;
    }

    /**
     * Parses a time like "30s", "15m", "2h", "1d" or combinations like "1h30m" into seconds.
     * A plain number is treated as minutes.
     */
    private int parseSeconds(String input) {
        input = input.toLowerCase();
        if (input.matches("\\d+")) return Integer.parseInt(input) * 60;
        if (!input.matches("(\\d+[smhd])+")) throw new NumberFormatException(input);
        int total = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)([smhd])").matcher(input);
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            switch (m.group(2)) {
                case "s": total += value; break;
                case "m": total += value * 60; break;
                case "h": total += value * 3600; break;
                case "d": total += value * 86400; break;
            }
        }
        return total;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("Axentra.maintenance")) return Collections.emptyList();
        if (args.length == 1) {
            return filter(Arrays.asList("on", "off", "status", "add", "remove"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return null; // player names
        }
        if (args[0].equalsIgnoreCase("on")) {
            String prev = args[args.length - 2];
            if (prev.equalsIgnoreCase("-c") || prev.equalsIgnoreCase("--countdown")) {
                return filter(Arrays.asList("0s", "10s", "30s", "1m", "2m", "5m"), args[args.length - 1]);
            }
            List<String> options = new ArrayList<>();
            if (args.length == 2) options.addAll(Arrays.asList("15m", "30m", "1h", "2h", "1h30m"));
            if (!Arrays.asList(args).subList(1, args.length - 1).contains("-c")) options.add("-c");
            options.add("<reason>");
            return filter(options, args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>(options);
        result.removeIf(s -> !s.toLowerCase().startsWith(input.toLowerCase()));
        return result;
    }
}
