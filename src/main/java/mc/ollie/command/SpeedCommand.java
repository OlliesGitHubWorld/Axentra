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
import java.util.Arrays;
import java.util.List;

public class SpeedCommand implements CommandExecutor, TabCompleter {

    // Vanilla Bukkit defaults
    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED  = 0.1f;

    private final App plugin;

    public SpeedCommand(App plugin) {
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

    /**
     * Maps user input 1-10 to Bukkit speed values.
     * Walk: 1 = 0.2 (vanilla default), 10 = 1.0 (max)
     * Fly:  1 = 0.1 (vanilla default), 10 = 1.0 (max)
     */
    private float toWalkSpeed(int input) {
        // 1 → 0.2, 10 → 1.0  (linear: 0.2 + (input-1) * (0.8/9))
        return DEFAULT_WALK_SPEED + (input - 1) * ((1.0f - DEFAULT_WALK_SPEED) / 9.0f);
    }

    private float toFlySpeed(int input) {
        // 1 → 0.1, 10 → 1.0  (linear: 0.1 + (input-1) * (0.9/9))
        return DEFAULT_FLY_SPEED + (input - 1) * ((1.0f - DEFAULT_FLY_SPEED) / 9.0f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "reset"));
        } else if (args.length == 2) {
            // args[1] can be walk/fly or a player name
            completions.addAll(Arrays.asList("walk", "fly"));
            if (sender.hasPermission("Axentra.speed.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
        } else if (args.length == 3) {
            String typeArg = args[1].toLowerCase();
            if (typeArg.equals("walk") || typeArg.equals("fly")) {
                // args[1] was a type, so args[2] is a player
                if (sender.hasPermission("Axentra.speed.others")) {
                    for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
                }
            }
            // if args[1] was a player name, there's no further argument
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.speed")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("speed-usage"));
            return true;
        }

        // Handle reset: /speed reset [walk|fly] [player]
        if (args[0].equalsIgnoreCase("reset")) {
            Boolean flyMode = null;
            int playerArgIndex = 1;
            if (args.length >= 2) {
                String typeArg = args[1].toLowerCase();
                if (typeArg.equals("walk")) { flyMode = false; playerArgIndex = 2; }
                else if (typeArg.equals("fly")) { flyMode = true; playerArgIndex = 2; }
            }
            String targetName = args.length > playerArgIndex ? args[playerArgIndex] : null;
            return handleReset(sender, flyMode, targetName);
        }

        // Parse speed value 1-10
        int speedInput;
        try {
            speedInput = Integer.parseInt(args[0]);
            if (speedInput < 1 || speedInput > 10) {
                sender.sendMessage(getMessage("speed-invalid"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("speed-invalid"));
            return true;
        }

        // Parse optional [walk|fly] type
        Boolean flyMode = null;
        int playerArgIndex = 1;
        if (args.length >= 2) {
            String typeArg = args[1].toLowerCase();
            if (typeArg.equals("walk")) { flyMode = false; playerArgIndex = 2; }
            else if (typeArg.equals("fly")) { flyMode = true; playerArgIndex = 2; }
        }

        String targetName = args.length > playerArgIndex ? args[playerArgIndex] : null;

        if (targetName == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            boolean useFly = flyMode != null ? flyMode : player.isFlying();
            applySpeed(player, speedInput, useFly);
            player.sendMessage(getMessage("speed-self")
                    .replace("%speed%", String.valueOf(speedInput))
                    .replace("%type%", useFly ? "fly" : "walk"));
            return true;
        }

        if (!sender.hasPermission("Axentra.speed.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", targetName));
            return true;
        }

        boolean useFly = flyMode != null ? flyMode : target.isFlying();
        applySpeed(target, speedInput, useFly);
        target.sendMessage(getMessage("speed-received")
                .replace("%speed%", String.valueOf(speedInput))
                .replace("%type%", useFly ? "fly" : "walk")
                .replace("%player%", sender instanceof Player ? sender.getName() : "Console"));
        sender.sendMessage(getMessage("speed-other")
                .replace("%speed%", String.valueOf(speedInput))
                .replace("%type%", useFly ? "fly" : "walk")
                .replace("%player%", target.getName()));
        return true;
    }

    private boolean handleReset(CommandSender sender, Boolean flyMode, String targetName) {
        if (targetName == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            resetSpeed(player, flyMode);
            String type = flyMode == null ? "walk & fly" : (flyMode ? "fly" : "walk");
            player.sendMessage(getMessage("speed-reset").replace("%type%", type));
            return true;
        }

        if (!sender.hasPermission("Axentra.speed.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", targetName));
            return true;
        }

        resetSpeed(target, flyMode);
        String type = flyMode == null ? "walk & fly" : (flyMode ? "fly" : "walk");
        target.sendMessage(getMessage("speed-reset-received")
                .replace("%type%", type)
                .replace("%player%", sender instanceof Player ? sender.getName() : "Console"));
        sender.sendMessage(getMessage("speed-reset-other")
                .replace("%type%", type)
                .replace("%player%", target.getName()));
        return true;
    }

    private void applySpeed(Player player, int input, boolean fly) {
        if (fly) {
            player.setFlySpeed(toFlySpeed(input));
        } else {
            player.setWalkSpeed(toWalkSpeed(input));
        }
    }

    private void resetSpeed(Player player, Boolean flyMode) {
        if (flyMode == null || !flyMode) player.setWalkSpeed(DEFAULT_WALK_SPEED);
        if (flyMode == null || flyMode)  player.setFlySpeed(DEFAULT_FLY_SPEED);
    }
}