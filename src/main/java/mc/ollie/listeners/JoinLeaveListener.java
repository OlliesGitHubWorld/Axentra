package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.TrustManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class JoinLeaveListener implements Listener {

    private final App plugin;

    public JoinLeaveListener(App plugin) {
        this.plugin = plugin;
    }

    private static final String DEFAULT_JOIN = "%player% joined the game";

    private World resolveSpawnWorld() {
        String name = plugin.getFileManager().getSettings().getString("spawn-world", "");
        if (name != null && !name.isEmpty()) {
            World world = Bukkit.getWorld(name);
            if (world != null) return world;
            plugin.getLogger().warning("spawn-world '" + name + "' not found, falling back to default world.");
        }
        return Bukkit.getWorlds().get(0);
    }
    private static final String DEFAULT_QUIT = "%player% left the game";

    private String getMessage(String path, Player player, String defaultMessage, int playerCount) {
        String message = plugin.getFileManager().getMessages().getString(path);

        if (message == null || message.equalsIgnoreCase("none")) {
            return ChatColor.YELLOW + defaultMessage.replace("%player%", player.getName());
        }

        if (message.isEmpty()) {
            return null;
        }

        message = message.replace("%player%", player.getName());
        message = message.replace("%playercount%", String.valueOf(playerCount));
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int count = Bukkit.getOnlinePlayers().size(); // includes the joining player

        if (!player.hasPlayedBefore()) {
            event.setJoinMessage(getMessage("first-join", player, DEFAULT_JOIN, count));
        } else {
            event.setJoinMessage(getMessage("join", player, DEFAULT_JOIN, count));
        }

        // Load saved nickname
        String nickname = plugin.getDatabaseManager().getNickname(player.getUniqueId().toString());
        if (nickname != null) {
            String coloredNick = ChatColor.translateAlternateColorCodes('&', nickname);
            player.setDisplayName(coloredNick + ChatColor.RESET);
            player.setPlayerListName(coloredNick);
        }

        String joinLocation = plugin.getFileManager().getSettings().getString("join-location", "last-location");
        if (joinLocation.equalsIgnoreCase("spawn")) {
            player.teleport(resolveSpawnWorld().getSpawnLocation());
        }

        // Notify staff about notes on this player
        notifyStaffNotes(player);

        // Smart join alert for staff
        sendSmartJoinAlert(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        int count = Bukkit.getOnlinePlayers().size() - 1; // exclude the leaving player
        event.setQuitMessage(getMessage("quit", event.getPlayer(), DEFAULT_QUIT, count));
    }

    private void notifyStaffNotes(Player joiningPlayer) {
        if (!plugin.getDatabaseManager().hasNotes(joiningPlayer.getUniqueId().toString())) {
            return;
        }

        List<DatabaseManager.NoteInfo> notes = plugin.getDatabaseManager().getNotesByUuid(joiningPlayer.getUniqueId().toString());
        if (notes.isEmpty()) return;

        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String prefixFormatted = ChatColor.translateAlternateColorCodes('&', prefix);

        String alertMsg = prefixFormatted + ChatColor.YELLOW + joiningPlayer.getName()
                + ChatColor.GRAY + " has " + ChatColor.YELLOW + notes.size()
                + ChatColor.GRAY + " staff note" + (notes.size() != 1 ? "s" : "")
                + ChatColor.GRAY + ". Use " + ChatColor.WHITE + "/notes " + joiningPlayer.getName()
                + ChatColor.GRAY + " to view.";

        // Delay by 1 tick to ensure join message shows first
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("Axentra.note")) {
                    staff.sendMessage(alertMsg);
                }
            }
        }, 1L);
    }

    private void sendSmartJoinAlert(Player player) {
        if (!plugin.getFileManager().getSettings().getBoolean("enabled-modules.alts", true)) return;
        if (!plugin.getFileManager().getProtection().getBoolean("alts.notify", true)) return;

        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> altUuids = plugin.getDatabaseManager().getAltUuids(uuid);
            int bannedAlts = 0;
            for (String altUuid : altUuids) {
                if (plugin.getDatabaseManager().isBanned(altUuid)) bannedAlts++;
            }

            String trust = "UNKNOWN";
            if (plugin.getTrustManager() != null) {
                trust = plugin.getTrustManager().getTrustLevel(uuid);
            }

            int caseCount = plugin.getDatabaseManager().getCaseCountByPlayer(name);
            int noteCount = 0;
            List<DatabaseManager.NoteInfo> notes = plugin.getDatabaseManager().getNotesByUuid(uuid);
            if (notes != null) noteCount = notes.size();
            int reportCount = plugin.getDatabaseManager().getRecentReportCount(name, 30);

            boolean hasVpn = false;
            List<DatabaseManager.IpHistoryEntry> ips = plugin.getDatabaseManager().getIpHistory(uuid);
            for (DatabaseManager.IpHistoryEntry ip : ips) {
                DatabaseManager.VpnCacheEntry vpn = plugin.getDatabaseManager().getVpnCache(ip.ip);
                if (vpn != null && vpn.isVpn) { hasVpn = true; break; }
            }

            // Only show alert if there's something noteworthy
            boolean hasAlts = !altUuids.isEmpty();
            boolean hasIssues = bannedAlts > 0 || hasVpn || caseCount > 0 || reportCount > 0
                    || (trust.equals(TrustManager.NEW));

            if (!hasAlts && !hasIssues) return;

            final int finalBannedAlts = bannedAlts;
            final String finalTrust = trust;
            final int finalCaseCount = caseCount;
            final int finalNoteCount = noteCount;
            final int finalReportCount = reportCount;
            final boolean finalHasVpn = hasVpn;
            final int altCount = altUuids.size();

            Bukkit.getScheduler().runTask(plugin, () -> {
                StringBuilder sb = new StringBuilder();
                sb.append(ChatColor.DARK_GRAY).append("[").append(ChatColor.GOLD).append("!").append(ChatColor.DARK_GRAY).append("] ");
                sb.append(ChatColor.YELLOW).append(name).append(ChatColor.GRAY).append(" joined");

                if (altCount > 0) {
                    sb.append(ChatColor.GRAY).append(" | Alts: ").append(ChatColor.WHITE).append(altCount);
                    if (finalBannedAlts > 0) {
                        sb.append(ChatColor.RED).append(" (").append(finalBannedAlts).append(" banned)");
                    }
                }

                if (finalHasVpn) {
                    sb.append(ChatColor.RED).append(" | VPN");
                }

                ChatColor trustColor = finalTrust.equals(TrustManager.NEW) ? ChatColor.RED :
                        finalTrust.equals(TrustManager.MEMBER) ? ChatColor.YELLOW : ChatColor.GREEN;
                sb.append(ChatColor.GRAY).append(" | Trust: ").append(trustColor).append(finalTrust);

                if (finalCaseCount > 0) {
                    sb.append(ChatColor.GRAY).append(" | Cases: ").append(ChatColor.RED).append(finalCaseCount);
                }

                TextComponent mainMsg = new TextComponent(sb.toString());

                // Add clickable buttons
                TextComponent altsBtn = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.AQUA + "Alts" + ChatColor.DARK_GRAY + "]");
                altsBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/alts " + name));
                altsBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "View alt accounts")));

                TextComponent historyBtn = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.RED + "History" + ChatColor.DARK_GRAY + "]");
                historyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history " + name));
                historyBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "View history")));

                TextComponent timelineBtn = new TextComponent(ChatColor.DARK_GRAY + " [" + ChatColor.LIGHT_PURPLE + "Timeline" + ChatColor.DARK_GRAY + "]");
                timelineBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/timeline " + name));
                timelineBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "View timeline")));

                mainMsg.addExtra(altsBtn);
                mainMsg.addExtra(historyBtn);
                mainMsg.addExtra(timelineBtn);

                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("Axentra.alts")) {
                        staff.spigot().sendMessage(mainMsg);
                    }
                }
            });
        });
    }
}
