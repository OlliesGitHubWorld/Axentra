package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.gui.RestoreInvGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestoreInvCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public RestoreInvCommand(App plugin) {
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
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            completions.add("snapshot");
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.restoreinv")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("restoreinv-usage"));
            return true;
        }

        String targetName = args[0];

        // /restoreinv <player> snapshot <id>
        if (args.length >= 3 && args[1].equalsIgnoreCase("snapshot")) {
            try {
                int snapshotId = Integer.parseInt(args[2]);
                DatabaseManager.SnapshotInfo snapshot = plugin.getDatabaseManager().getSnapshot(snapshotId);
                if (snapshot == null) {
                    sender.sendMessage(getMessage("restoreinv-not-found"));
                    return true;
                }
                if (sender instanceof Player) {
                    new RestoreInvGUI(plugin, targetName).openPreview((Player) sender, snapshot);
                } else {
                    sendSnapshotInfo(sender, snapshot);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(getMessage("restoreinv-invalid-id"));
            }
            return true;
        }

        // /restoreinv <player> — open GUI or console fallback
        String targetUuid = null;
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            targetUuid = target.getUniqueId().toString();
            targetName = target.getName();
        } else {
            targetUuid = plugin.getDatabaseManager().getUuidByName(targetName);
            if (targetUuid == null) {
                targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId().toString();
            }
        }

        final String finalUuid = targetUuid;
        final String finalName = targetName;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.SnapshotInfo> snapshots = plugin.getDatabaseManager().getSnapshots(finalUuid, 100);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (snapshots.isEmpty()) {
                    sender.sendMessage(getMessage("restoreinv-empty").replace("%player%", finalName));
                    return;
                }
                if (sender instanceof Player) {
                    new RestoreInvGUI(plugin, finalName).open((Player) sender, snapshots, 0);
                } else {
                    sender.sendMessage(ChatColor.AQUA + "Snapshots for " + finalName + ":");
                    for (DatabaseManager.SnapshotInfo snap : snapshots) {
                        sender.sendMessage(ChatColor.GRAY + " #" + snap.id + " " + snap.reason
                                + " - " + formatTime(snap.time));
                    }
                }
            });
        });
        return true;
    }

    private void sendSnapshotInfo(CommandSender sender, DatabaseManager.SnapshotInfo snap) {
        sender.sendMessage(ChatColor.AQUA + "Snapshot #" + snap.id);
        sender.sendMessage(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + snap.reason);
        sender.sendMessage(ChatColor.GRAY + "Time: " + ChatColor.WHITE + formatTime(snap.time));
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + snap.world);
        sender.sendMessage(ChatColor.GRAY + "XP Level: " + ChatColor.WHITE + snap.xpLevel);
        if (snap.deathCause != null) {
            sender.sendMessage(ChatColor.GRAY + "Death Cause: " + ChatColor.WHITE + snap.deathCause);
        }
    }

    private String formatTime(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new java.util.Date(millis));
    }
}
