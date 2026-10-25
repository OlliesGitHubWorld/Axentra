package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.gui.HomesGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HomesCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public HomesCommand(App plugin) {
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
        if (args.length == 1 && sender.hasPermission("Axentra.homes.others")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("Axentra.homes")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        String targetUuid;
        String targetName;

        if (args.length > 0 && player.hasPermission("Axentra.homes.others")) {
            String name = args[0];
            Player target = Bukkit.getPlayer(name);
            if (target != null) {
                targetUuid = target.getUniqueId().toString();
                targetName = target.getName();
            } else {
                targetUuid = plugin.getDatabaseManager().getUuidByName(name);
                if (targetUuid == null) {
                    targetUuid = Bukkit.getOfflinePlayer(name).getUniqueId().toString();
                }
                targetName = name;
            }
        } else {
            targetUuid = player.getUniqueId().toString();
            targetName = player.getName();
        }

        final String finalUuid = targetUuid;
        final String finalName = targetName;
        final boolean isOwn = finalUuid.equals(player.getUniqueId().toString());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.HomeInfo> homes = plugin.getDatabaseManager().getHomes(finalUuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (homes.isEmpty()) {
                    player.sendMessage(getMessage("homes-empty").replace("%player%", finalName));
                    return;
                }
                new HomesGUI(plugin, finalName, isOwn).open(player, homes, 0);
            });
        });
        return true;
    }
}
