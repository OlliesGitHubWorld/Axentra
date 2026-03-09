package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class LoginListener implements Listener {

    private final App plugin;

    public LoginListener(App plugin) {
        this.plugin = plugin;
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days    = seconds / 86400; seconds %= 86400;
        long hours   = seconds / 3600;  seconds %= 3600;
        long minutes = seconds / 60;    seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        // Check IP ban first
        String ip = event.getAddress().getHostAddress();
        if (plugin.getDatabaseManager().isIpBanned(ip)) {
            try {
                ResultSet ipBan = plugin.getDatabaseManager().getIpBan(ip);
                String reason = ipBan.getString("reason");
                String bannedBy = ipBan.getString("banned_by");
                String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");

                Timestamp ipExpiresAt = ipBan.getTimestamp("expires_at");
                String rawMessage;
                if (ipExpiresAt != null) {
                    rawMessage = plugin.getFileManager().getMessages().getString("tempbanip-message");
                    if (rawMessage == null) rawMessage = "&cYour IP has been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%";
                    long remaining = ipExpiresAt.getTime() - System.currentTimeMillis();
                    rawMessage = rawMessage.replace("%duration%", formatDuration(remaining));
                } else {
                    rawMessage = plugin.getFileManager().getMessages().getString("banip-message");
                    if (rawMessage == null) rawMessage = "&cYour IP has been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%";
                }
                rawMessage = rawMessage.replace("%reason%", reason).replace("%banned-by%", bannedBy);
                if (appealLink.isEmpty()) {
                    rawMessage = rawMessage.replace("\n&7Appeal: &f%appeal%", "");
                } else {
                    rawMessage = rawMessage.replace("%appeal%", appealLink);
                }

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        ChatColor.translateAlternateColorCodes('&', rawMessage));
                return;
            } catch (Exception e) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "Your IP is banned from this server!");
                return;
            }
        }

        String uuid = event.getUniqueId().toString();

        if (plugin.getDatabaseManager().isBanned(uuid)) {
            try {
                ResultSet ban = plugin.getDatabaseManager().getBan(uuid);
                String reason = ban.getString("reason");
                String bannedBy = ban.getString("banned_by");
                Timestamp expiresAt = ban.getTimestamp("expires_at");

                String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");

                String rawMessage;
                if (expiresAt != null) {
                    // Tempban — show remaining duration
                    rawMessage = plugin.getFileManager().getMessages().getString("tempban-message");
                    if (rawMessage == null) rawMessage = "&cYou have been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%";
                    long remaining = expiresAt.getTime() - System.currentTimeMillis();
                    rawMessage = rawMessage.replace("%duration%", formatDuration(remaining));
                } else {
                    rawMessage = plugin.getFileManager().getMessages().getString("ban-message");
                }

                rawMessage = rawMessage.replace("%reason%", reason);
                rawMessage = rawMessage.replace("%banned-by%", bannedBy);

                if (appealLink.isEmpty()) {
                    rawMessage = rawMessage.replace("\n&7Appeal: &f%appeal%", "");
                } else {
                    rawMessage = rawMessage.replace("%appeal%", appealLink);
                }

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        ChatColor.translateAlternateColorCodes('&', rawMessage));

            } catch (Exception e) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "You are banned from this server!");
            }
        }
    }
}