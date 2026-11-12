package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class BanIpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public BanIpCommand(App plugin) {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.banip")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("banip-usage"));
            return true;
        }

        String input = args[0];

        // A player name (online or offline) bans their IP and their account; anything else is a raw IP
        IpBanTarget target = IpBanTarget.resolve(plugin, input);
        if (target == null) {
            sender.sendMessage(getMessage("banip-no-address").replace("%player%", input));
            return true;
        }
        String ip = target.ip;
        String displayName = target.isPlayer() ? target.playerName : "Unknown";

        // Build reason
        String reason;
        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]);
                if (i < args.length - 1) sb.append(" ");
            }
            reason = sb.toString();
        } else {
            reason = plugin.getFileManager().getMessages().getString("banip-default-reason", "Your IP has been banned!");
        }

        // Only show the resolved IP to staff allowed to see IPs; a typed-in IP is already known to the sender
        String shownIp = !target.isPlayer() || sender.hasPermission("Axentra.alts.ip")
                ? ip : getMessage("ip-hidden").replace("%player%", displayName);

        boolean ipBanned = plugin.getDatabaseManager().isIpBanned(ip);
        boolean accountBanned = target.isPlayer() && plugin.getDatabaseManager().isBannedByName(target.playerName);
        if (ipBanned && (!target.isPlayer() || accountBanned)) {
            sender.sendMessage(getMessage("banip-already-banned").replace("%ip%", shownIp));
            return true;
        }

        if (!ipBanned) {
            plugin.getDatabaseManager().banIp(ip, reason, sender.getName(), null);
        }

        // Ban the account too, with one case covering both
        int caseId = -1;
        if (target.isPlayer()) {
            if (!accountBanned) {
                plugin.getDatabaseManager().banPlayer(target.playerName, target.playerUuid, reason, sender.getName(), null);
            }
            String chatLog = null;
            if (target.online != null && plugin.getChatListener() != null) {
                chatLog = plugin.getChatListener().getChatSnapshot(target.online.getUniqueId());
            }
            caseId = plugin.getDatabaseManager().createCase("BAN", target.playerName, target.playerUuid,
                    "[IP] " + reason, sender.getName(), null, chatLog);
            String staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
            plugin.getDatabaseManager().logAction(sender.getName(), staffUuid, "BANIP", target.playerName, reason);
        }

        // Kick all online players with this IP
        final String bannedIp = ip;
        String appealLink = plugin.getFileManager().getSettings().getString("ban-appeal-link", "");
        String rawKickMessage = plugin.getFileManager().getMessages().getString("banip-message");
        if (rawKickMessage == null) rawKickMessage = "&cYour IP has been banned!\n&7Reason: &f%reason%\n&7Banned by: &f%banned-by%";
        rawKickMessage = rawKickMessage.replace("%reason%", reason).replace("%banned-by%", sender.getName());
        if (appealLink.isEmpty()) {
            rawKickMessage = rawKickMessage.replace("\n&7Appeal: &f%appeal%", "");
        } else {
            rawKickMessage = rawKickMessage.replace("%appeal%", appealLink);
        }
        final String kickMessage = ChatColor.translateAlternateColorCodes('&', rawKickMessage);

        for (Player online : Bukkit.getOnlinePlayers()) {
            InetSocketAddress address = online.getAddress();
            if (address != null && address.getAddress().getHostAddress().equals(bannedIp)) {
                online.kickPlayer(kickMessage);
            }
        }

        sender.sendMessage(getMessage("banip-success").replace("%ip%", shownIp).replace("%reason%", reason));
        if (target.isPlayer()) {
            sender.sendMessage(getMessage("banip-account").replace("%player%", target.playerName)
                    + ChatColor.DARK_GRAY + " (Case #" + caseId + ")");
        }

        if (plugin.getFileManager().getSettings().getBoolean("announce-banip")) {
            String announceMsg = getMessage("banip-announce")
                    .replace("%player%", displayName)
                    .replace("%reason%", reason);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("Axentra.notify.ban")) {
                    online.sendMessage(announceMsg);
                }
            }
        }

        return true;
    }
}
