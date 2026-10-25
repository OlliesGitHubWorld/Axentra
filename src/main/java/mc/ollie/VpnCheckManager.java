package mc.ollie;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class VpnCheckManager {

    private final App plugin;
    private final HttpClient httpClient;

    public VpnCheckManager(App plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isEnabled() {
        return plugin.getFileManager().getProtection().getBoolean("vpn.enabled", false);
    }

    /**
     * Check if an IP is a VPN/proxy. Must be called async.
     * Returns null if the check fails or is disabled.
     */
    public Boolean checkIp(String ip) {
        FileConfiguration config = plugin.getFileManager().getProtection();
        if (!config.getBoolean("vpn.enabled", false)) return null;

        // Check cache first
        DatabaseManager.VpnCacheEntry cached = plugin.getDatabaseManager().getVpnCache(ip);
        if (cached != null) return cached.isVpn;

        String apiKey = config.getString("vpn.api-key", "");
        int timeoutMs = config.getInt("vpn.timeout-ms", 3000);
        int cacheHours = config.getInt("vpn.cache-hours", 24);

        try {
            String url = "https://proxycheck.io/v2/" + ip + "?key=" + apiKey + "&vpn=1";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Simple JSON parsing without external dependencies
            boolean isVpn = body.contains("\"proxy\":\"yes\"") || body.contains("\"vpn\":\"yes\"");

            long expiresAt = System.currentTimeMillis() + (cacheHours * 3600000L);
            plugin.getDatabaseManager().setVpnCache(ip, isVpn, expiresAt);

            return isVpn;
        } catch (Exception e) {
            plugin.getLogger().warning("VPN check failed for " + ip + ": " + e.getMessage());
            return null;
        }
    }
}
