package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.gui.TimelineGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TimelineCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public TimelineCommand(App plugin) {
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
        if (args.length == 2) {
            return Arrays.asList("JOIN", "QUIT", "CASE", "DEATH", "NAME_CHANGE", "TRUST_CHANGE", "FREEZE", "NOTE", "REPORT")
                    .stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.timeline")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getMessage("timeline-usage"));
            return true;
        }

        String targetName = args[0];
        String filter = args.length > 1 ? args[1].toUpperCase() : null;

        String targetUuid = resolveUuid(targetName);
        if (targetUuid == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", targetName));
            return true;
        }

        final String finalUuid = targetUuid;
        final String finalName = targetName;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<TimelineGUI.TimelineEntry> entries = buildTimeline(finalUuid, finalName, filter, sender.hasPermission("Axentra.alts.ip"));
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entries.isEmpty()) {
                    sender.sendMessage(getMessage("timeline-empty").replace("%player%", finalName));
                    return;
                }
                if (sender instanceof Player) {
                    new TimelineGUI(plugin, finalName).open((Player) sender, entries, 0);
                } else {
                    // Console fallback
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    sender.sendMessage(ChatColor.AQUA + "Timeline for " + finalName + ":");
                    for (TimelineGUI.TimelineEntry e : entries) {
                        sender.sendMessage(ChatColor.GRAY + " [" + sdf.format(new Date(e.time)) + "] "
                                + e.color + e.type + ChatColor.GRAY + " - " + e.description);
                    }
                }
            });
        });
        return true;
    }

    public List<TimelineGUI.TimelineEntry> buildTimeline(String playerUuid, String playerName, String filter, boolean showIp) {
        List<TimelineGUI.TimelineEntry> entries = new ArrayList<>();

        // Sessions (JOIN/QUIT)
        if (filter == null || filter.equals("JOIN") || filter.equals("QUIT")) {
            for (DatabaseManager.SessionInfo session : plugin.getDatabaseManager().getPlayerSessions(playerUuid, 200)) {
                long duration = (session.quitTime > 0 ? session.quitTime : System.currentTimeMillis()) - session.joinTime;
                String durStr = formatDuration(duration);
                entries.add(new TimelineGUI.TimelineEntry("JOIN", session.joinTime,
                        "Session started" + (durStr.isEmpty() ? "" : " (duration: " + durStr + ")"), ChatColor.GREEN));
                if (session.quitTime > 0) {
                    entries.add(new TimelineGUI.TimelineEntry("QUIT", session.quitTime,
                            "Session ended (" + durStr + ")", ChatColor.YELLOW));
                }
            }
        }

        // Cases
        if (filter == null || filter.equals("CASE")) {
            for (DatabaseManager.CaseInfo c : plugin.getDatabaseManager().getCasesByPlayerUuid(playerUuid)) {
                long time;
                try { time = Long.parseLong(c.issuedAt); } catch (Exception e) { time = System.currentTimeMillis(); }
                entries.add(new TimelineGUI.TimelineEntry("CASE", time,
                        c.type + " #" + c.id + ": " + (c.reason != null ? c.reason : "No reason"),
                        ChatColor.RED, c.id));
            }
        }

        // Deaths
        if (filter == null || filter.equals("DEATH")) {
            for (DatabaseManager.DeathInfo death : plugin.getDatabaseManager().getDeaths(playerUuid, 200)) {
                entries.add(new TimelineGUI.TimelineEntry("DEATH", death.time,
                        death.cause != null ? death.cause : "Unknown cause",
                        ChatColor.DARK_RED, death.snapshotId));
            }
        }

        // Name changes
        if (filter == null || filter.equals("NAME_CHANGE")) {
            List<DatabaseManager.NameHistoryEntry> names = plugin.getDatabaseManager().getNameHistory(playerUuid);
            for (int i = 0; i < names.size(); i++) {
                DatabaseManager.NameHistoryEntry name = names.get(i);
                String prev = i + 1 < names.size() ? names.get(i + 1).name : "?";
                if (i < names.size() - 1) {
                    entries.add(new TimelineGUI.TimelineEntry("NAME_CHANGE", name.changedAt,
                            prev + " \u2192 " + name.name, ChatColor.LIGHT_PURPLE));
                }
            }
        }

        // IP changes
        if (showIp && (filter == null || filter.equals("IP_CHANGE"))) {
            List<DatabaseManager.IpHistoryEntry> ips = plugin.getDatabaseManager().getIpHistory(playerUuid);
            for (DatabaseManager.IpHistoryEntry ip : ips) {
                entries.add(new TimelineGUI.TimelineEntry("IP_CHANGE", ip.firstSeen,
                        "IP: " + ip.ip, ChatColor.DARK_GRAY));
            }
        }

        // Trust changes
        if (filter == null || filter.equals("TRUST_CHANGE")) {
            for (DatabaseManager.TrustHistoryEntry th : plugin.getDatabaseManager().getTrustHistory(playerUuid, 200)) {
                entries.add(new TimelineGUI.TimelineEntry("TRUST_CHANGE", th.time,
                        (th.oldLevel != null ? th.oldLevel : "NONE") + " \u2192 " + th.newLevel
                        + " (" + (th.reason != null ? th.reason : "") + ")", ChatColor.AQUA));
            }
        }

        // Notes
        if (filter == null || filter.equals("NOTE")) {
            for (DatabaseManager.NoteInfo note : plugin.getDatabaseManager().getNotesByUuid(playerUuid)) {
                long time;
                try { time = Long.parseLong(note.addedAt); } catch (Exception e) { time = System.currentTimeMillis(); }
                entries.add(new TimelineGUI.TimelineEntry("NOTE", time,
                        "By " + note.addedBy + ": " + note.note, ChatColor.BLUE));
            }
        }

        // Freeze/Unfreeze from audit log
        if (filter == null || filter.equals("FREEZE")) {
            // Check from audit_log
            try (java.sql.PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(
                    "SELECT action, target, details, timestamp FROM audit_log WHERE target = ? AND (action = 'FREEZE' OR action = 'UNFREEZE') ORDER BY timestamp DESC LIMIT 100")) {
                stmt.setString(1, playerName);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String action = rs.getString("action");
                        String timestamp = rs.getString("timestamp");
                        long time;
                        try { time = Long.parseLong(timestamp); } catch (Exception e) { time = System.currentTimeMillis(); }
                        entries.add(new TimelineGUI.TimelineEntry(action, time,
                                action.equals("FREEZE") ? "Player was frozen" : "Player was unfrozen",
                                action.equals("FREEZE") ? ChatColor.DARK_AQUA : ChatColor.GREEN));
                    }
                }
            } catch (Exception ignored) {}
        }

        // Sort by time descending
        entries.sort((a, b) -> Long.compare(b.time, a.time));
        return entries;
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600; seconds %= 3600;
        long m = seconds / 60; seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (h == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private String resolveUuid(String name) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) return player.getUniqueId().toString();
        String uuid = plugin.getDatabaseManager().getUuidByName(name);
        if (uuid != null) return uuid;
        return Bukkit.getOfflinePlayer(name).getUniqueId().toString();
    }
}
