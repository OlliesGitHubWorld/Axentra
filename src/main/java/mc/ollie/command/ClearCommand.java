package mc.ollie.command;

import mc.ollie.App;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClearCommand implements CommandExecutor, TabCompleter {

    private static final int CONFIRM_SECONDS = 10;

    private final App plugin;
    // Sender name → pending clear awaiting /clear confirm
    private final Map<String, PendingClear> pending = new HashMap<>();

    private record PendingClear(UUID target, long expiresAt) {}

    public ClearCommand(App plugin) {
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
            if (pending.containsKey(sender.getName())) completions.add("confirm");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.clear")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        // /clear confirm - run the pending clear if it hasn't expired
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")
                && (pending.containsKey(sender.getName()) || Bukkit.getPlayerExact("confirm") == null)) {
            PendingClear request = pending.remove(sender.getName());
            if (request == null || System.currentTimeMillis() > request.expiresAt()) {
                sender.sendMessage(getMessage("clear-confirm-expired"));
                return true;
            }
            Player target = Bukkit.getPlayer(request.target());
            if (target == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", "?"));
                return true;
            }
            clear(sender, target);
            return true;
        }

        Player target;
        if (args.length == 0) {
            // /clear with no args - clear own inventory
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            target = (Player) sender;
        } else {
            // /clear <player> - clear another player's inventory
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
                return true;
            }

            if (!sender.hasPermission("Axentra.clear.others")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }
        }

        if (!plugin.getFileManager().getSettings().getBoolean("clear.require-confirm", true)) {
            clear(sender, target);
            return true;
        }

        pending.put(sender.getName(), new PendingClear(target.getUniqueId(),
                System.currentTimeMillis() + CONFIRM_SECONDS * 1000L));

        boolean self = target.equals(sender);
        sender.sendMessage(getMessage(self ? "clear-confirm" : "clear-confirm-other")
                .replace("%player%", target.getName()));
        String hint = getMessage("clear-confirm-hint").replace("%seconds%", String.valueOf(CONFIRM_SECONDS));
        if (sender instanceof Player player) {
            TextComponent button = new TextComponent(TextComponent.fromLegacyText(getMessage("clear-confirm-button")));
            button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clear confirm"));
            button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(getMessage("clear-confirm-hover"))));
            TextComponent line = new TextComponent(button);
            for (var part : TextComponent.fromLegacyText(" " + hint)) line.addExtra(part);
            player.spigot().sendMessage(line);
        } else {
            sender.sendMessage(hint);
        }
        return true;
    }

    private void clear(CommandSender sender, Player target) {
        if (plugin.getSnapshotManager() != null) {
            plugin.getSnapshotManager().createSnapshot(target, "CLEAR", null);
        }
        target.getInventory().clear();
        target.sendMessage(getMessage("clear-inventory"));
        if (!target.equals(sender)) {
            sender.sendMessage(getMessage("clear-other").replace("%player%", target.getName()));
        }
    }
}
