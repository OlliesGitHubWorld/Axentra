package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.PlayerProfile;
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
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * /whois &lt;player&gt; (v1.9.0) — everything about a player, online or offline: identity, playtime
 * without AFK, current state, trust, risk, cases, active punishment and linked Discord. IP, alts
 * and VPN status only with Axentra.whois.sensitive.
 */
public class WhoisCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public WhoisCommand(App plugin) {
        this.plugin = plugin;
    }

    private String msg(String key) {
        return plugin.getMessageManager().getMessage(key);
    }

    /** A label from messages.yml whois-labels.<key>, falling back to the given text. */
    private String label(String key, String fallback) {
        return plugin.getFileManager().getMessages().getString("whois-labels." + key, fallback);
    }

    private String line(String label, String value) {
        return msg("whois-line").replace("%label%", label).replace("%value%", value);
    }

    private String yesNo(boolean value) {
        return c(value ? label("yes", "&ayes") : label("no", "&7no"));
    }

    private static String c(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.whois")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(msg("whois-usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && plugin.isVanished(target) && !sender.hasPermission("Axentra.vanish.see")) target = null;
        boolean sensitive = sender.hasPermission("Axentra.whois.sensitive");

        // Live state is read here on the main thread
        List<String> live = new ArrayList<>();
        if (target != null) {
            live.add(line(label("gamemode", "Gamemode"), ChatColor.WHITE + target.getGameMode().name().toLowerCase()));
            live.add(line(label("fly", "Fly"), yesNo(target.getAllowFlight())));
            live.add(line(label("god", "God"), yesNo(plugin.isGod(target))));
            live.add(line(label("vanish", "Vanish"), yesNo(plugin.isVanished(target))));
            live.add(line(label("afk", "AFK"), yesNo(plugin.getAfkManager() != null && plugin.getAfkManager().isAfk(target))));
            live.add(line(label("frozen", "Frozen"), yesNo(plugin.getFreezeManager() != null && plugin.getFreezeManager().isFrozen(target.getUniqueId()))));
            if (sensitive && target.getAddress() != null) {
                live.add(line(label("ip", "IP"), ChatColor.WHITE + target.getAddress().getAddress().getHostAddress()));
            }
        }
        String query = target != null ? target.getUniqueId().toString() : args[0];
        boolean online = target != null;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerProfile p = PlayerProfile.load(plugin, query);
            if (p == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(msg("player-not-found").replace("%player%", args[0])));
                return;
            }
            DatabaseManager db = plugin.getDatabaseManager();
            String nickname = db.getNickname(p.uuid);
            String lastIp = null;
            if (sensitive && !online) {
                List<DatabaseManager.IpHistoryEntry> ips = db.getIpHistory(p.uuid);
                if (!ips.isEmpty()) lastIp = ips.get(0).ip;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            List<String> lines = new ArrayList<>();
            lines.add(msg("whois-header").replace("%player%", p.name));
            lines.add(line(label("uuid", "UUID"), ChatColor.WHITE + p.uuid));
            if (nickname != null) lines.add(line(label("nickname", "Nickname"), ChatColor.translateAlternateColorCodes('&', nickname)));
            lines.add(line(label("first-join", "First join"), ChatColor.WHITE + (p.firstJoin > 0 ? sdf.format(new Date(p.firstJoin)) : "-")));
            lines.add(line(label("last-seen", "Last seen"), online ? c(label("online-now", "&aonline now"))
                    : ChatColor.WHITE + (p.lastSeen > 0 ? sdf.format(new Date(p.lastSeen)) : "-")));
            lines.add(line(label("playtime", "Playtime"), ChatColor.WHITE + PlayerProfile.formatMinutes(p.playtimeMinutes) + c(label("without-afk", " &8(without AFK)"))));
            lines.addAll(live);
            if (p.trust != null) lines.add(line(label("trust", "Trust"), ChatColor.WHITE + p.trust));
            lines.add(line(label("risk", "Risk"), ChatColor.WHITE + String.valueOf(p.riskScore) + "/100"));
            lines.add(line(label("cases", "Cases"), ChatColor.WHITE + String.valueOf(p.caseCount)));
            String punishment = p.activeBan != null
                    ? c(p.activeBan.expiresAt != null ? label("tempbanned", "&ctempbanned") : label("banned", "&cbanned"))
                            + ChatColor.GRAY + " (" + p.activeBan.reason + ")"
                    : p.muted ? c(label("muted", "&6muted")) : c(label("none", "&7none"));
            lines.add(line(label("punishment", "Punishment"), punishment));
            lines.add(line(label("discord", "Discord"), p.discordName != null ? ChatColor.BLUE + p.discordName : c(label("not-linked", "&7not linked"))));
            if (sensitive) {
                if (lastIp != null) lines.add(line(label("last-ip", "Last IP"), ChatColor.WHITE + lastIp));
                lines.add(line(label("alts", "Alts"), ChatColor.WHITE + String.valueOf(p.alts) + (p.bannedAlts > 0 ? c(label("banned-alts", " &c(%count% banned)")).replace("%count%", String.valueOf(p.bannedAlts)) : "")));
                lines.add(line(label("vpn", "VPN"), p.vpn ? c(label("vpn-detected", "&cdetected")) : yesNo(false)));
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                lines.forEach(sender::sendMessage);
                if (sender instanceof Player viewer) viewer.spigot().sendMessage(buttons(p.name));
            });
        });
        return true;
    }

    private TextComponent buttons(String name) {
        TextComponent row = new TextComponent("  ");
        String[][] buttons = {
                {"History", "history", ChatColor.RED.toString()},
                {"Notes", "notes", ChatColor.BLUE.toString()},
                {"Timeline", "timeline", ChatColor.LIGHT_PURPLE.toString()},
                {"Alts", "alts", ChatColor.AQUA.toString()},
                {"Punish", "punish", ChatColor.GOLD.toString()}
        };
        for (String[] b : buttons) {
            TextComponent button = new TextComponent(ChatColor.DARK_GRAY + "[" + b[2] + b[0] + ChatColor.DARK_GRAY + "] ");
            button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + b[1] + " " + name));
            button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "/" + b[1] + " " + name)));
            row.addExtra(button);
        }
        return row;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (String n : plugin.getDatabaseManager().getKnownPlayerNames()) {
            if (n.toLowerCase().startsWith(args[0].toLowerCase())) names.add(n);
            if (names.size() >= 50) break;
        }
        return names;
    }
}
