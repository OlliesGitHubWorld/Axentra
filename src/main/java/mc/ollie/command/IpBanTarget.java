package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
    @SuppressWarnings("deprecation")
    static IpBanTarget resolve(App plugin, String input) {
        Player online = Bukkit.getPlayer(input);
        if (online != null) {
            InetSocketAddress address = online.getAddress();
            if (address == null) return null;
            return new IpBanTarget(address.getAddress().getHostAddress(),
                    online.getName(), online.getUniqueId().toString(), online);
        }

        if (looksLikeIp(input)) {
            return new IpBanTarget(input, null, null, null);
        }

        // Offline player: use the most recent IP they joined with
        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        String uuid = offline.getUniqueId().toString();
        String name = offline.getName() != null ? offline.getName() : input;
        List<DatabaseManager.IpHistoryEntry> history = plugin.getDatabaseManager().getIpHistory(uuid);
        if (history.isEmpty()) return null;
        return new IpBanTarget(history.get(0).ip, name, uuid, null);
    }
}
