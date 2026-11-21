package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.PlayerLookup;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Resolves the argument of /banip, /tempbanip and /unbanip to an IP and, when a player
 * name was given, to that player's account so both can be (un)banned together.
 */
final class IpBanTarget {

    final String ip;
    /** Player name and UUID, or null when a raw IP was given. */
    final String playerName;
    final String playerUuid;
    final Player online;

    private IpBanTarget(String ip, String playerName, String playerUuid, Player online) {
        this.ip = ip;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.online = online;
    }

    boolean isPlayer() {
        return playerName != null;
    }

    static boolean looksLikeIp(String input) {
        return input.matches("\\d{1,3}(\\.\\d{1,3}){3}") || (input.contains(":") && input.matches("[0-9a-fA-F:.]+"));
    }

    /** Returns null when the input is a player whose IP is not known. */
    static IpBanTarget resolve(App plugin, String input) {
        if (looksLikeIp(input)) {
            return new IpBanTarget(input, null, null, null);
        }

        // Same lookup as /ban: online, left recently, or known to the database. Bukkit.getOfflinePlayer(name)
        // gives the wrong UUID on offline-mode and proxy servers, so the IP history came back empty.
        PlayerLookup.Target target = PlayerLookup.resolve(plugin, input);
        if (target == null) return null;

        if (target.online() != null) {
            InetSocketAddress address = target.online().getAddress();
            if (address == null) return null;
            return new IpBanTarget(address.getAddress().getHostAddress(), target.name(), target.uuid(), target.online());
        }

        // Offline player: use the most recent IP they joined with
        String ip = PlayerLookup.recentIp(target.uuid());
        if (ip == null) {
            List<DatabaseManager.IpHistoryEntry> history = plugin.getDatabaseManager().getIpHistory(target.uuid());
            for (DatabaseManager.IpHistoryEntry entry : history) {
                if (!"unknown".equals(entry.ip)) {
                    ip = entry.ip;
                    break;
                }
            }
        }
        if (ip == null) return null;
        return new IpBanTarget(ip, target.name(), target.uuid(), null);
    }
}
