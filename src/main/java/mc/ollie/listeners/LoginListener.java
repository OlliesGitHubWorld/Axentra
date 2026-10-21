package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

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
            DatabaseManager.IpBanInfo ipBan = plugin.getDatabaseManager().getIpBan(ip);
            if (ipBan == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "Your IP is banned from this server!");
                return;
            }

            String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");

            String rawMessage;
            if (ipBan.expiresAt != null) {
                rawMessage = plugin.getFileManager().getMessages().getString("tempbanip-message");
                if (rawMessage == null) rawMessage = "&cYour IP has been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%";
                long remaining = ipBan.expiresAt.getTime() - System.currentTimeMillis();
                rawMessage = rawMessage.replace("%duration%", formatDuration(remaining));
            } else {
                rawMessage = plugin.getFileManager().getMessages().getString("banip-message");
                if (rawMessage == null) rawMessage = "&cYour IP has been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%";
            }
            rawMessage = rawMessage.replace("%reason%", ipBan.reason).replace("%banned-by%", ipBan.bannedBy);
            if (appealLink.isEmpty()) {
                rawMessage = rawMessage.replace("\n&7Appeal: &f%appeal%", "");
            } else {
                rawMessage = rawMessage.replace("%appeal%", appealLink);
            }

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    ChatColor.translateAlternateColorCodes('&', rawMessage));
            return;
        }

        String uuid = event.getUniqueId().toString();

        if (plugin.getDatabaseManager().isBanned(uuid)) {
            DatabaseManager.BanInfo ban = plugin.getDatabaseManager().getBan(uuid);
            if (ban == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "You are banned from this server!");
                return;
            }

            String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");

            String rawMessage;
            if (ban.expiresAt != null) {
                rawMessage = plugin.getFileManager().getMessages().getString("tempban-message");
                if (rawMessage == null) rawMessage = "&cYou have been temporarily banned!\n&7Reason: &f%reason%\n&7Duration: &f%duration%\n&7Banned by: &f%banned-by%";
                long remaining = ban.expiresAt.getTime() - System.currentTimeMillis();
                rawMessage = rawMessage.replace("%duration%", formatDuration(remaining));
            } else {
                rawMessage = plugin.getFileManager().getMessages().getString("ban-message");
                if (rawMessage == null) rawMessage = "&cYou have been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%";
            }

            rawMessage = rawMessage.replace("%reason%", ban.reason);
            rawMessage = rawMessage.replace("%banned-by%", ban.bannedBy);

            if (appealLink.isEmpty()) {
                rawMessage = rawMessage.replace("\n&7Appeal: &f%appeal%", "");
            } else {
                rawMessage = rawMessage.replace("%appeal%", appealLink);
            }

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    ChatColor.translateAlternateColorCodes('&', rawMessage));
        }
    }
}
