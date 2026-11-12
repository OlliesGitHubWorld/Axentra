package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /seen &lt;player&gt; (v1.9.0) — online with session length, or when they were last online,
 * plus total playtime (without AFK). Staff with Axentra.seen.staff also see the last location
 * and the country of the last IP (never the IP itself).
 */
public class SeenCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final Map<String, String> countryCache = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public SeenCommand(App plugin) {
        this.plugin = plugin;
    }

    private String msg(String key) {
        return plugin.getMessageManager().getMessage(key);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.seen")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(msg("seen-usage"));
            return true;
        }
        String name = args[0];
        boolean staff = sender.hasPermission("Axentra.seen.staff");
        Player online = Bukkit.getPlayerExact(name);
        boolean visible = online != null && (!plugin.isVanished(online) || sender.hasPermission("Axentra.vanish.see"));
        Location onlineLocation = visible ? online.getLocation() : null;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager db = plugin.getDatabaseManager();
            String uuid = visible ? online.getUniqueId().toString() : db.findUuidByName(name);
            if (uuid == null) {
                reply(sender, msg("player-not-found").replace("%player%", name));
                return;
            }
            String realName = visible ? online.getName() : Optional.ofNullable(db.getLastKnownName(uuid)).orElse(name);
            long playtime = Math.max(0, db.getPlaytimeMinutes(uuid));
            List<String> lines = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            if (visible) {
                long start = db.getCurrentSessionStart(uuid);
                lines.add(msg("seen-online").replace("%player%", realName)
                        .replace("%session%", start > 0 ? PlayerProfile.formatMinutes((System.currentTimeMillis() - start) / 60000) : "-"));
            } else {
                long last = db.getLastSessionTime(uuid);
                if (last <= 0) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                    last = off.getLastPlayed();
                }
                lines.add(last > 0
                        ? msg("seen-offline").replace("%player%", realName)
                            .replace("%ago%", PlayerProfile.formatMinutes((System.currentTimeMillis() - last) / 60000))
                            .replace("%date%", sdf.format(new Date(last)))
                        : msg("seen-never").replace("%player%", realName));
            }
            lines.add(msg("seen-playtime").replace("%playtime%", PlayerProfile.formatMinutes(playtime)));

            if (staff) {
                Location loc = onlineLocation;
                if (loc == null) {
                    try { loc = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getLocation(); } catch (Throwable ignored) { }
                }
                if (loc != null && loc.getWorld() != null) {
                    lines.add(msg("seen-location").replace("%world%", loc.getWorld().getName())
                            .replace("%x%", String.valueOf(loc.getBlockX())).replace("%y%", String.valueOf(loc.getBlockY()))
                            .replace("%z%", String.valueOf(loc.getBlockZ())));
                }
                List<DatabaseManager.IpHistoryEntry> ips = db.getIpHistory(uuid);
                String country = ips.isEmpty() ? null : country(ips.get(0).ip);
                lines.add(msg("seen-country").replace("%country%", country != null ? country : msg("seen-country-unknown")));
            }
            Bukkit.getScheduler().runTask(plugin, () -> lines.forEach(sender::sendMessage));
        });
        return true;
    }

    /**
     * Country of an IP through proxycheck.io, only when VPN detection is set up (protection.yml),
     * so Axentra never sends IPs anywhere you didn't already allow. Runs async; cached.
     */
    private String country(String ip) {
        if (ip == null) return null;
        String cached = countryCache.get(ip);
        if (cached != null) return cached.isEmpty() ? null : cached;
        var protection = plugin.getFileManager().getProtection();
        if (!protection.getBoolean("vpn.enabled", false)) return null;
        try {
            String url = "https://proxycheck.io/v2/" + ip + "?key=" + protection.getString("vpn.api-key", "") + "&asn=1";
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            int i = body.indexOf("\"country\":");
            String country = null;
            if (i >= 0) {
                int start = body.indexOf('"', i + 10) + 1;
                int end = body.indexOf('"', start);
                if (start > 0 && end > start) country = body.substring(start, end);
            }
            countryCache.put(ip, country != null ? country : "");
            return country;
        } catch (Exception e) {
            return null;
        }
    }

    private void reply(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (String n : plugin.getDatabaseManager().getKnownPlayerNames()) {
            if (n.toLowerCase().startsWith(args[0].toLowerCase())) names.add(n);
            if (names.size() >= 50) break;
        }
        return names;
    }
}
