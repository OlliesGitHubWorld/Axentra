package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    // requester UUID -> target UUID
    private final Map<UUID, UUID> outgoing = new HashMap<>();
    // target UUID -> requester UUID
    private final Map<UUID, UUID> incoming = new HashMap<>();
    // requester UUID -> timeout task
    private final Map<UUID, BukkitTask> timeouts = new HashMap<>();
    // requester UUID -> active countdown task
    private final Map<UUID, BukkitTask> countdowns = new HashMap<>();
    // players who have disabled TPA requests
    private final java.util.Set<UUID> tpToggled = new java.util.HashSet<>();

    private static final int TIMEOUT_SECONDS = 60;

    public TpaCommand(App plugin) {
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

    private void cleanupRequest(UUID requesterUUID) {
        UUID targetUUID = outgoing.remove(requesterUUID);
        if (targetUUID != null) incoming.remove(targetUUID);
        BukkitTask task = timeouts.remove(requesterUUID);
        if (task != null) task.cancel();
    }

    private void cancelCountdown(UUID requesterUUID) {
        BukkitTask task = countdowns.remove(requesterUUID);
        if (task != null) task.cancel();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (label.equalsIgnoreCase("tpa") && args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender)) completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;

        switch (label.toLowerCase()) {
            case "tpa":
                return handleTpa(player, args);
            case "tpaccept":
                return handleAccept(player);
            case "tpdeny":
                return handleDeny(player);
            case "tpacancel":
                return handleCancel(player);
            case "tptoggle":
                return handleToggle(player);
        }
        return true;
    }

    private boolean handleTpa(Player requester, String[] args) {
        if (!requester.hasPermission("Axentra.tpa")) {
            requester.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            requester.sendMessage(getMessage("tpa-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            requester.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(getMessage("tpa-self"));
            return true;
        }

        if (tpToggled.contains(target.getUniqueId())) {
            requester.sendMessage(getMessage("tpa-toggled-off").replace("%player%", target.getName()));
            return true;
        }

        // Cancel any existing outgoing request from this player
        if (outgoing.containsKey(requester.getUniqueId())) {
            UUID oldTargetUUID = outgoing.get(requester.getUniqueId());
            cleanupRequest(requester.getUniqueId());
            Player oldTarget = Bukkit.getPlayer(oldTargetUUID);
            if (oldTarget != null) {
                oldTarget.sendMessage(getMessage("tpa-cancelled-target").replace("%player%", requester.getName()));
            }
        }

        outgoing.put(requester.getUniqueId(), target.getUniqueId());
        incoming.put(target.getUniqueId(), requester.getUniqueId());

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (outgoing.containsKey(requester.getUniqueId())) {
                cleanupRequest(requester.getUniqueId());
                requester.sendMessage(getMessage("tpa-expired-requester").replace("%player%", target.getName()));
                if (target.isOnline()) {
                    target.sendMessage(getMessage("tpa-expired-target").replace("%player%", requester.getName()));
                }
            }
        }, TIMEOUT_SECONDS * 20L);

        timeouts.put(requester.getUniqueId(), task);

        requester.sendMessage(getMessage("tpa-sent").replace("%player%", target.getName()));
        target.sendMessage(getMessage("tpa-received")
                .replace("%player%", requester.getName())
                .replace("%seconds%", String.valueOf(TIMEOUT_SECONDS)));
        return true;
    }

    private boolean handleAccept(Player target) {
        if (!target.hasPermission("Axentra.tpa")) {
            target.sendMessage(getMessage("no-permission"));
            return true;
        }

        UUID requesterUUID = incoming.get(target.getUniqueId());
        if (requesterUUID == null) {
            target.sendMessage(getMessage("tpa-no-request"));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        cleanupRequest(requesterUUID);

        if (requester == null || !requester.isOnline()) {
            target.sendMessage(getMessage("tpa-requester-offline"));
            return true;
        }

        target.sendMessage(getMessage("tpa-accept-target").replace("%player%", requester.getName()));
        requester.sendMessage(getMessage("tpa-accept-requester").replace("%player%", target.getName()));

        startCountdown(requester, target);
        return true;
    }

    private boolean isTitleEnabled() {
        return plugin.getFileManager().getSettings().getBoolean("tpa-title-countdown", true);
    }

    private void sendTitle(Player player, String titleKey, String subtitleKey, String... replacements) {
        if (!isTitleEnabled()) return;
        String title = getMessage(titleKey);
        String subtitle = getMessage(subtitleKey);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            title = title.replace(replacements[i], replacements[i + 1]);
            subtitle = subtitle.replace(replacements[i], replacements[i + 1]);
        }
        player.sendTitle(title, subtitle, 0, 40, 10);
    }

    private void startCountdown(Player requester, Player target) {
        // Cancel any existing countdown for this requester
        cancelCountdown(requester.getUniqueId());

        Location startLocation = requester.getLocation().clone();
        int seconds = plugin.getFileManager().getSettings().getInt("tpa-countdown-seconds", 3);

        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!requester.isOnline()) {
                    cancel();
                    countdowns.remove(requester.getUniqueId());
                    return;
                }

                // Check if the player moved (block-level precision)
                Location current = requester.getLocation();
                if (current.getBlockX() != startLocation.getBlockX()
                        || current.getBlockY() != startLocation.getBlockY()
                        || current.getBlockZ() != startLocation.getBlockZ()) {
                    cancel();
                    countdowns.remove(requester.getUniqueId());
                    sendTitle(requester, "tpa-moved-title", "tpa-moved-subtitle");
                    requester.sendMessage(getMessage("tpa-moved"));
                    return;
                }

                if (remaining == 0) {
                    cancel();
                    countdowns.remove(requester.getUniqueId());
                    if (target.isOnline()) {
                        requester.teleport(target.getLocation());
                        sendTitle(requester, "tpa-teleported-title", "tpa-teleported-subtitle",
                                "%player%", target.getName());
                    } else {
                        requester.sendMessage(getMessage("tpa-target-offline"));
                    }
                    return;
                }

                if (isTitleEnabled()) {
                    requester.sendTitle(
                            getMessage("tpa-countdown-title").replace("%seconds%", String.valueOf(remaining)),
                            getMessage("tpa-countdown-subtitle"),
                            0, 25, 0
                    );
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdowns.put(requester.getUniqueId(), task);
    }

    private boolean handleDeny(Player target) {
        if (!target.hasPermission("Axentra.tpa")) {
            target.sendMessage(getMessage("no-permission"));
            return true;
        }

        UUID requesterUUID = incoming.get(target.getUniqueId());
        if (requesterUUID == null) {
            target.sendMessage(getMessage("tpa-no-request"));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        cleanupRequest(requesterUUID);

        target.sendMessage(getMessage("tpa-deny-target").replace("%player%", requester != null ? requester.getName() : "Unknown"));
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(getMessage("tpa-deny-requester").replace("%player%", target.getName()));
        }
        return true;
    }

    private boolean handleToggle(Player player) {
        if (!player.hasPermission("Axentra.tpa")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (tpToggled.contains(player.getUniqueId())) {
            tpToggled.remove(player.getUniqueId());
            player.sendMessage(getMessage("tptoggle-enabled"));
        } else {
            tpToggled.add(player.getUniqueId());
            // Cancel any incoming request now that they've disabled TPA
            UUID requesterUUID = incoming.get(player.getUniqueId());
            if (requesterUUID != null) {
                cleanupRequest(requesterUUID);
                Player requester = Bukkit.getPlayer(requesterUUID);
                if (requester != null) {
                    requester.sendMessage(getMessage("tpa-toggled-off").replace("%player%", player.getName()));
                }
            }
            player.sendMessage(getMessage("tptoggle-disabled"));
        }
        return true;
    }

    private boolean handleCancel(Player requester) {
        if (!requester.hasPermission("Axentra.tpa")) {
            requester.sendMessage(getMessage("no-permission"));
            return true;
        }

        UUID targetUUID = outgoing.get(requester.getUniqueId());
        if (targetUUID == null) {
            requester.sendMessage(getMessage("tpa-no-outgoing"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetUUID);
        cleanupRequest(requester.getUniqueId());

        requester.sendMessage(getMessage("tpa-cancel-requester").replace("%player%", target != null ? target.getName() : "Unknown"));
        if (target != null && target.isOnline()) {
            target.sendMessage(getMessage("tpa-cancelled-target").replace("%player%", requester.getName()));
        }
        return true;
    }
}