package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.command.AxentraCommand;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateListener implements Listener {

    private final App plugin;
    private final AxentraCommand axentraCommand;

    public UpdateListener(App plugin, AxentraCommand axentraCommand) {
        this.plugin = plugin;
        this.axentraCommand = axentraCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPermission("Axentra.admin")) return;
        if (!axentraCommand.isUpdateCheckerEnabled()) return;
        if (!plugin.getFileManager().getSettings().getBoolean("update-checker.notify-ops-on-join", true)) return;

        String currentVersion = AxentraCommand.normalizeVersion(plugin.getDescription().getVersion());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String latestVersion = axentraCommand.getLatestVersion();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (latestVersion == null || !event.getPlayer().isOnline()) return;
                if (AxentraCommand.isNewerVersion(latestVersion, currentVersion)) {
                    String prefix = ChatColor.translateAlternateColorCodes('&',
                            plugin.getFileManager().getMessages().getString("prefix", ""));
                    event.getPlayer().sendMessage(prefix + ChatColor.YELLOW + "New update available! " +
                            ChatColor.GRAY + currentVersion + ChatColor.DARK_GRAY + " → " +
                            ChatColor.GREEN + latestVersion);
                    event.getPlayer().sendMessage(ChatColor.AQUA + "https://github.com/OlliesGitHubWorld/Axentra/releases/latest");
                }
            });
        });
    }
}
