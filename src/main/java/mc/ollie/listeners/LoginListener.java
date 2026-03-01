package mc.ollie.listeners;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.ResultSet;

public class LoginListener implements Listener {

    private final App plugin;

    public LoginListener(App plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();

        if (plugin.getDatabaseManager().isBanned(uuid)) {
            try {
                ResultSet ban = plugin.getDatabaseManager().getBan(uuid);
                String reason = ban.getString("reason");
                String bannedBy = ban.getString("banned_by");

                String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
                String rawMessage = plugin.getFileManager().getMessages().getString("ban-message");

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