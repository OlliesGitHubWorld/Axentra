package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TrustCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public TrustCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("set");
            completions.add("reset");
            for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            return completions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.asList("NEW", "MEMBER", "TRUSTED").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getMessage("trust-usage"));
            return true;
        }

        // /trust set <player> <level>
        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("Axentra.trust.admin")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(getMessage("trust-set-usage"));
                return true;
            }
            String targetName = args[1];
            String level = args[2].toUpperCase();
            if (!level.equals("NEW") && !level.equals("MEMBER") && !level.equals("TRUSTED")) {
                sender.sendMessage(getMessage("trust-invalid-level"));
                return true;
            }
            String uuid = resolveUuid(targetName);
            if (uuid == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", targetName));
                return true;
            }
            plugin.getTrustManager().setTrust(uuid, level, true, "Set by " + sender.getName(), sender.getName());
            sender.sendMessage(getMessage("trust-set-success")
                    .replace("%player%", targetName).replace("%level%", level));
            plugin.getDatabaseManager().logAction(sender.getName(),
                    sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE",
                    "TRUST_SET", targetName, "Set to " + level);
            return true;
        }

        // /trust reset <player>
        if (args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("Axentra.trust.admin")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(getMessage("trust-reset-usage"));
                return true;
            }
            String targetName = args[1];
            String uuid = resolveUuid(targetName);
            if (uuid == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", targetName));
                return true;
            }
            plugin.getTrustManager().resetTrust(uuid, sender.getName());
            sender.sendMessage(getMessage("trust-reset-success").replace("%player%", targetName));
            plugin.getDatabaseManager().logAction(sender.getName(),
                    sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE",
                    "TRUST_RESET", targetName, "Reset to NEW, auto-upgrade re-enabled");
            return true;
        }

        // /trust <player> — view trust info
        if (!sender.hasPermission("Axentra.trust")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        String targetName = args[0];
        String uuid = resolveUuid(targetName);
        if (uuid == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", targetName));
            return true;
        }

        String level = plugin.getTrustManager().getTrustLevel(uuid);
        boolean manual = plugin.getTrustManager().isManual(uuid);
        long playtime = plugin.getDatabaseManager().getPlaytimeMinutes(uuid);
        long minutesToNext = plugin.getTrustManager().getMinutesToNextLevel(uuid);

        DatabaseManager.TrustInfo info = plugin.getDatabaseManager().getTrust(uuid);
        String lastChange = info != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(info.updatedAt)) : "N/A";

        ChatColor levelColor = level.equals("TRUSTED") ? ChatColor.GREEN : level.equals("MEMBER") ? ChatColor.YELLOW : ChatColor.GRAY;

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  Trust: " + ChatColor.WHITE + targetName);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Level: " + levelColor + level);
        sender.sendMessage(ChatColor.GRAY + "  Playtime: " + ChatColor.WHITE + playtime + " minutes");
        if (!level.equals("TRUSTED")) {
            sender.sendMessage(ChatColor.GRAY + "  Next level in: " + ChatColor.WHITE + minutesToNext + " minutes");
        }
        sender.sendMessage(ChatColor.GRAY + "  Manual: " + (manual ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
        sender.sendMessage(ChatColor.GRAY + "  Last change: " + ChatColor.WHITE + lastChange);
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        return true;
    }

    private String resolveUuid(String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) return player.getUniqueId().toString();
        String uuid = plugin.getDatabaseManager().getUuidByName(name);
        if (uuid != null) return uuid;
        return Bukkit.getOfflinePlayer(name).getUniqueId().toString();
    }
}
