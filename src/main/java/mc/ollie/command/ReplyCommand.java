package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public ReplyCommand(App plugin) {
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        if (!sender.hasPermission("Axentra.msg")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getDatabaseManager().isMuted(player.getUniqueId().toString())) {
            player.sendMessage(getMessage("mute-command-blocked"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(getMessage("reply-usage"));
            return true;
        }

        UUID lastUuid = plugin.getLastMessaged(player.getUniqueId());
        if (lastUuid == null) {
            player.sendMessage(getMessage("reply-no-target"));
            return true;
        }

        Player target = Bukkit.getPlayer(lastUuid);
        if (target == null || !target.isOnline()) {
            player.sendMessage(getMessage("reply-offline"));
            return true;
        }

        if (plugin.isMsgToggleOff(target.getUniqueId()) && !player.hasPermission("Axentra.msg.bypass")) {
            player.sendMessage(getMessage("msg-toggled-off").replace("%player%", target.getName()));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) messageBuilder.append(" ");
        }
        String message = messageBuilder.toString();

        player.sendMessage(getMessage("msg-sent")
                .replace("%player%", target.getName())
                .replace("%message%", message));

        target.sendMessage(getMessage("msg-received")
                .replace("%player%", player.getName())
                .replace("%message%", message));

        plugin.setLastMessaged(player.getUniqueId(), target.getUniqueId());
        plugin.setLastMessaged(target.getUniqueId(), player.getUniqueId());

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("Axentra.socialspy") && !online.equals(player) && !online.equals(target)) {
                online.sendMessage(getMessage("msg-spy")
                        .replace("%sender%", player.getName())
                        .replace("%receiver%", target.getName())
                        .replace("%message%", message));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
