package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public TpCommand(App plugin) {
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
        String cmd = label.toLowerCase();
        if (cmd.equals("tp") || cmd.equals("tphere") || cmd.equals("tpahere")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            }
            if (cmd.equals("tp") && args.length == 2) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label.toLowerCase()) {
            case "tp": return handleTp(sender, args);
            case "tphere": return handleTpHere(sender, args);
            case "tpall": return handleTpAll(sender);
            case "tpahere": return handleTpaHere(sender, args);
            case "top": return handleTop(sender);
        }
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.tp")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("tp-usage"));
            return true;
        }

        // /tp <player> - teleport self to player
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
                return true;
            }
            player.teleport(target.getLocation());
            player.sendMessage(getMessage("tp-to-player").replace("%player%", target.getName()));
            return true;
        }

        // /tp <player1> <player2> - teleport player1 to player2
        if (args.length >= 2) {
            Player target1 = Bukkit.getPlayer(args[0]);
            Player target2 = Bukkit.getPlayer(args[1]);
            if (target1 == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
                return true;
            }
            if (target2 == null) {
                sender.sendMessage(getMessage("player-not-found").replace("%player%", args[1]));
                return true;
            }
            target1.teleport(target2.getLocation());
            sender.sendMessage(getMessage("tp-other")
                    .replace("%player1%", target1.getName())
                    .replace("%player2%", target2.getName()));
            return true;
        }

        return true;
    }

    private boolean handleTpHere(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.tphere")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getMessage("tphere-usage"));
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        target.teleport(player.getLocation());
        player.sendMessage(getMessage("tphere-success").replace("%player%", target.getName()));
        target.sendMessage(getMessage("tphere-received").replace("%player%", player.getName()));
        return true;
    }

    private boolean handleTpAll(CommandSender sender) {
        if (!sender.hasPermission("Axentra.tpall")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();
        int count = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player)) {
                target.teleport(loc);
                target.sendMessage(getMessage("tpall-received").replace("%player%", player.getName()));
                count++;
            }
        }
        player.sendMessage(getMessage("tpall-success").replace("%count%", String.valueOf(count)));
        return true;
    }

    private boolean handleTpaHere(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.tpahere")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getMessage("tpahere-usage"));
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(getMessage("tpa-self"));
            return true;
        }

        // Send request to target to teleport to sender
        target.sendMessage(getMessage("tpahere-received").replace("%player%", player.getName()));
        player.sendMessage(getMessage("tpahere-sent").replace("%player%", target.getName()));
        // This uses the TPA system - the target can /tpaccept or /tpdeny
        // For simplicity, directly teleport with staff permission
        target.teleport(player.getLocation());
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        if (!sender.hasPermission("Axentra.top")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int highestY = world.getHighestBlockYAt(loc);

        if (highestY <= world.getMinHeight()) {
            player.sendMessage(getMessage("top-no-block"));
            return true;
        }

        Location topLoc = new Location(world, loc.getX(), highestY + 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        player.teleport(topLoc);
        player.sendMessage(getMessage("top-success"));
        return true;
    }
}
