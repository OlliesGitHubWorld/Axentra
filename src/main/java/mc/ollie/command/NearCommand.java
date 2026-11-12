package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.*;

/**
 * /near [radius] (v1.9.0) — players within the radius, nearest first, with distance and direction.
 * The radius is capped by near.max-radius, or by the highest Axentra.near.radius.&lt;n&gt; permission.
 */
public class NearCommand implements CommandExecutor, TabCompleter {

    private static final String[] DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final String RADIUS_PREFIX = "axentra.near.radius.";

    private final App plugin;

    public NearCommand(App plugin) {
        this.plugin = plugin;
    }

    private String msg(String key) {
        return plugin.getMessageManager().getMessage(key);
    }

    /** The highest Axentra.near.radius.<n> the player has, or near.max-radius. */
    private int maxRadius(Player player) {
        int max = -1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission().toLowerCase(Locale.ROOT);
            if (info.getValue() && perm.startsWith(RADIUS_PREFIX)) {
                try { max = Math.max(max, Integer.parseInt(perm.substring(RADIUS_PREFIX.length()))); } catch (NumberFormatException ignored) { }
            }
        }
        return max > 0 ? max : plugin.getFileManager().getSettings().getInt("near.max-radius", 500);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.near")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("rtp-player-only"));
            return true;
        }
        int max = maxRadius(player);
        int radius = plugin.getFileManager().getSettings().getInt("near.default-radius", 100);
        if (args.length > 0) {
            try {
                radius = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(msg("near-usage"));
                return true;
            }
        }
        radius = Math.max(1, Math.min(radius, max));

        boolean seeVanished = player.hasPermission("Axentra.vanish.see");
        Location here = player.getLocation();
        List<Map.Entry<Player, Double>> nearby = new ArrayList<>();
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || (!seeVanished && plugin.isVanished(other))) continue;
            double distance = other.getLocation().distance(here);
            if (distance <= radius) nearby.add(Map.entry(other, distance));
        }
        nearby.sort(Map.Entry.comparingByValue());

        if (nearby.isEmpty()) {
            player.sendMessage(msg("near-none").replace("%radius%", String.valueOf(radius)));
            return true;
        }
        player.sendMessage(msg("near-header").replace("%radius%", String.valueOf(radius))
                .replace("%count%", String.valueOf(nearby.size())));
        for (Map.Entry<Player, Double> entry : nearby) {
            Location there = entry.getKey().getLocation();
            player.sendMessage(msg("near-line").replace("%player%", entry.getKey().getName())
                    .replace("%distance%", String.valueOf(Math.round(entry.getValue())))
                    .replace("%direction%", direction(here, there)));
        }
        return true;
    }

    /** Compass direction from one location to another (N is -Z in Minecraft). */
    private static String direction(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) angle += 360;
        return DIRECTIONS[(int) Math.round(angle / 45) % 8];
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("50", "100", "200");
        return Collections.emptyList();
    }
}
