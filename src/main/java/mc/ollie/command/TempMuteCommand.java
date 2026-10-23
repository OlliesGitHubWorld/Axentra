package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempMuteCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

    public TempMuteCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private long parseDuration(String input) {
        Matcher matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.matches()) return -1;
        long days    = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
        long hours   = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
        long minutes = matcher.group(3) != null ? Long.parseLong(matcher.group(3)) : 0;
        long seconds = matcher.group(4) != null ? Long.parseLong(matcher.group(4)) : 0;
        long millis = (days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000L;
        return millis > 0 ? millis : -1;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            completions.add("5m");
            completions.add("10m");
            completions.add("30m");
            completions.add("1h");
            completions.add("1d");
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.tempmute")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("tempmute-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        String durationStr = args[1];
        long durationMillis = parseDuration(durationStr);
        if (durationMillis == -1) {
            sender.sendMessage(getMessage("tempmute-invalid-duration"));
            return true;
        }

        if (plugin.getDatabaseManager().isMuted(target.getUniqueId().toString())) {
            sender.sendMessage(getMessage("mute-already-muted").replace("%player%", target.getName()));
            return true;
        }

        String reason;
        if (args.length > 2) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                reasonBuilder.append(args[i]);
                if (i < args.length - 1) reasonBuilder.append(" ");
            }
            reason = reasonBuilder.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("tempmute-default-reason", "You have been temporarily muted!");
        }

        String formattedDuration = formatDuration(durationMillis);
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + durationMillis);

        // Get chat snapshot
        String chatLog = null;
        ChatListener chatListener = plugin.getChatListener();
        if (chatListener != null) {
            chatLog = chatListener.getChatSnapshot(target.getUniqueId());
        }

        plugin.getDatabaseManager().mutePlayer(target.getName(), target.getUniqueId().toString(), reason, sender.getName(), expiresAt);

        // Create case
        String staffName = sender.getName();
        String staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
        int caseId = plugin.getDatabaseManager().createCase("TEMPMUTE", target.getName(),
                target.getUniqueId().toString(), reason, staffName, expiresAt, chatLog);

        // Audit log
        plugin.getDatabaseManager().logAction(staffName, staffUuid, "TEMPMUTE", target.getName(),
                reason + " (" + formattedDuration + ")");

        target.sendMessage(getMessage("tempmute-message")
                .replace("%reason%", reason)
                .replace("%duration%", formattedDuration));

        sender.sendMessage(getMessage("tempmute-success")
                .replace("%player%", target.getName())
                .replace("%duration%", formattedDuration)
                .replace("%reason%", reason)
                + ChatColor.DARK_GRAY + " (Case #" + caseId + ")");

        if (plugin.getFileManager().getSettings().getBoolean("announce-mute")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("Axentra.notify.mute")) {
                    player.sendMessage(getMessage("tempmute-announce")
                            .replace("%player%", target.getName())
                            .replace("%duration%", formattedDuration)
                            .replace("%reason%", reason));
                }
            }
        }

        return true;
    }
}
