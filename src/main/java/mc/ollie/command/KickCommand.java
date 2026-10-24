package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class KickCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public KickCommand(App plugin) {
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
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.kick")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("kick-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        String reason;
        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]);
                if (i < args.length - 1) reasonBuilder.append(" ");
            }
            reason = reasonBuilder.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("kick-default-reason", "You have been kicked!");
        }

        // Get chat snapshot before kicking
        String chatLog = null;
        ChatListener chatListener = plugin.getChatListener();
        if (chatListener != null) {
            chatLog = chatListener.getChatSnapshot(target.getUniqueId());
        }

        // Create case
        String staffName = sender.getName();
        String staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
        int caseId = plugin.getDatabaseManager().createCase("KICK", target.getName(),
                target.getUniqueId().toString(), reason, staffName, null, chatLog);

        // Audit log
        plugin.getDatabaseManager().logAction(staffName, staffUuid, "KICK", target.getName(), reason);

        String kickMessage = getMessage("kick-message").replace("%reason%", reason);
        target.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));

        sender.sendMessage(getMessage("kick-success")
                .replace("%player%", target.getName())
                .replace("%reason%", reason)
                + ChatColor.DARK_GRAY + " (Case #" + caseId + ")");

        if (plugin.getFileManager().getSettings().getBoolean("announce-kick")) {
            String announceMsg = getMessage("kick-announce")
                    .replace("%player%", target.getName())
                    .replace("%reason%", reason);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("Axentra.notify.kick")) {
                    online.sendMessage(announceMsg);
                }
            }
        }

        return true;
    }
}
