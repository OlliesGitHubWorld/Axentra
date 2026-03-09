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

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public MuteCommand(App plugin) {
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
        if (!sender.hasPermission("Axentra.mute")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("mute-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (plugin.getDatabaseManager().isMuted(target.getUniqueId().toString())) {
            sender.sendMessage(getMessage("mute-already-muted").replace("%player%", target.getName()));
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
            reason = plugin.getFileManager().getMessages().getString("mute-default-reason", "You have been muted!");
        }

        plugin.getDatabaseManager().mutePlayer(target.getName(), target.getUniqueId().toString(), reason, sender.getName());

        target.sendMessage(getMessage("mute-message").replace("%reason%", reason));
        sender.sendMessage(getMessage("mute-success").replace("%player%", target.getName()).replace("%reason%", reason));

        if (plugin.getFileManager().getSettings().getBoolean("announce-mute")) {
            plugin.getServer().broadcastMessage(getMessage("mute-announce")
                    .replace("%player%", target.getName())
                    .replace("%reason%", reason));
        }

        return true;
    }
}
