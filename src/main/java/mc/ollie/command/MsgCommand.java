package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MsgCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public MsgCommand(App plugin) {
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
                if (sender instanceof Player && player.equals(sender)) continue;
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.msg")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("msg-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (sender instanceof Player && target.equals(sender)) {
            sender.sendMessage(getMessage("msg-self"));
            return true;
        }

        if (sender instanceof Player) {
            Player senderPlayer = (Player) sender;
            if (plugin.getDatabaseManager().isMuted(senderPlayer.getUniqueId().toString())) {
                senderPlayer.sendMessage(getMessage("mute-command-blocked"));
                return true;
            }
        }

        if (plugin.isMsgToggleOff(target.getUniqueId()) && !sender.hasPermission("Axentra.msg.bypass")) {
            sender.sendMessage(getMessage("msg-toggled-off").replace("%player%", target.getName()));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) messageBuilder.append(" ");
        }
        String message = messageBuilder.toString();

        String senderName = sender instanceof Player ? sender.getName() : "Console";

        sender.sendMessage(getMessage("msg-sent")
                .replace("%player%", target.getName())
                .replace("%message%", message));

        target.sendMessage(getMessage("msg-received")
                .replace("%player%", senderName)
                .replace("%message%", message));

        // Store last messaged for /reply
        if (sender instanceof Player) {
            plugin.setLastMessaged(((Player) sender).getUniqueId(), target.getUniqueId());
        }
        plugin.setLastMessaged(target.getUniqueId(), sender instanceof Player ? ((Player) sender).getUniqueId() : null);

        // Spy for staff
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("Axentra.socialspy") && !online.equals(sender) && !online.equals(target)) {
                online.sendMessage(getMessage("msg-spy")
                        .replace("%sender%", senderName)
                        .replace("%receiver%", target.getName())
                        .replace("%message%", message));
            }
        }

        return true;
    }
}
