package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.TrustManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AltsCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public AltsCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.alts")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getMessage("alts-usage"));
            return true;
        }

        String targetName = args[0];
        String targetUuid = resolveUuid(targetName);
        if (targetUuid == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", targetName));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> altUuids = plugin.getDatabaseManager().getAltUuids(targetUuid);
            int riskScore = calculateRiskScore(targetUuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  Alts: " + ChatColor.WHITE + targetName
                        + ChatColor.GRAY + " | Risk: " + getRiskColor(riskScore) + riskScore);
                sender.sendMessage("");

                if (altUuids.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  No known alt accounts.");
                } else {
                    for (String altUuid : altUuids) {
                        String altName = plugin.getDatabaseManager().getLastKnownName(altUuid);
                        if (altName == null) altName = altUuid.substring(0, 8);
                        boolean online = Bukkit.getPlayer(UUID.fromString(altUuid)) != null;
                        boolean banned = plugin.getDatabaseManager().isBanned(altUuid);
                        boolean muted = plugin.getDatabaseManager().isMuted(altUuid);
                        int cases = plugin.getDatabaseManager().getCaseCountByPlayer(altName);
                        long lastSeen = plugin.getDatabaseManager().getLastSessionTime(altUuid);

                        ChatColor nameColor = banned ? ChatColor.RED : muted ? ChatColor.GOLD : ChatColor.WHITE;
                        String status = online ? ChatColor.GREEN + "Online" : ChatColor.GRAY + "Offline";
                        String lastSeenStr = lastSeen > 0 ? new SimpleDateFormat("yyyy-MM-dd").format(new Date(lastSeen)) : "Never";

                        String line = "  " + nameColor + altName + " " + status
                                + ChatColor.GRAY + " | Last: " + lastSeenStr
                                + " | Cases: " + cases;
                        if (banned) line += ChatColor.RED + " [BANNED]";
                        if (muted) line += ChatColor.GOLD + " [MUTED]";

                        if (sender instanceof Player) {
                            TextComponent tc = new TextComponent(line);
                            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history " + altName));
                            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new Text(ChatColor.YELLOW + "Click to view history")));
                            ((Player) sender).spigot().sendMessage(tc);
                        } else {
                            sender.sendMessage(line);
                        }
                    }
                }

                // Show IPs if permission
                if (sender.hasPermission("Axentra.alts.ip")) {
                    List<DatabaseManager.IpHistoryEntry> ips = plugin.getDatabaseManager().getIpHistory(targetUuid);
                    if (!ips.isEmpty()) {
                        sender.sendMessage("");
                        sender.sendMessage(ChatColor.GRAY + "  IPs:");
                        for (DatabaseManager.IpHistoryEntry ip : ips) {
                            sender.sendMessage(ChatColor.DARK_GRAY + "    " + ip.ip + ChatColor.GRAY
                                    + " (last: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(ip.lastSeen)) + ")");
                        }
                    }
                }

                sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
            });
        });
        return true;
    }

    public int calculateRiskScore(String playerUuid) {
        FileConfiguration config = plugin.getFileManager().getProtection();
        int score = 0;

        // Banned alts
        List<String> altUuids = plugin.getDatabaseManager().getAltUuids(playerUuid);
        int bannedAlts = 0;
        for (String altUuid : altUuids) {
            if (plugin.getDatabaseManager().isBanned(altUuid)) bannedAlts++;
        }
        score += bannedAlts * config.getInt("alts.risk-weights.banned-alt", 30);

        // VPN
        DatabaseManager.VpnCacheEntry vpn = null;
        List<DatabaseManager.IpHistoryEntry> ips = plugin.getDatabaseManager().getIpHistory(playerUuid);
        for (DatabaseManager.IpHistoryEntry ip : ips) {
            DatabaseManager.VpnCacheEntry entry = plugin.getDatabaseManager().getVpnCache(ip.ip);
            if (entry != null && entry.isVpn) { vpn = entry; break; }
        }
        if (vpn != null) score += config.getInt("alts.risk-weights.vpn-detected", 20);

        // Trust NEW
        String trust = plugin.getTrustManager().getTrustLevel(playerUuid);
        if (trust.equals(TrustManager.NEW)) score += config.getInt("alts.risk-weights.trust-new", 15);

        // Case count
        String name = plugin.getDatabaseManager().getLastKnownName(playerUuid);
        if (name != null) {
            int cases = plugin.getDatabaseManager().getCaseCountByPlayer(name);
            score += cases * config.getInt("alts.risk-weights.case-count", 5);
        }

        return Math.min(100, score);
    }

    private ChatColor getRiskColor(int score) {
        if (score >= 60) return ChatColor.RED;
        if (score >= 30) return ChatColor.GOLD;
        return ChatColor.GREEN;
    }

    private String resolveUuid(String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) return player.getUniqueId().toString();
        String uuid = plugin.getDatabaseManager().getUuidByName(name);
        if (uuid != null) return uuid;
        return Bukkit.getOfflinePlayer(name).getUniqueId().toString();
    }
}
