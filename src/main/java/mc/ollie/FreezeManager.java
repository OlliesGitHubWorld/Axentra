package mc.ollie;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central freeze state. Keeps an in-memory cache of frozen players (backed by the
 * frozen_players table) so hot event handlers never hit the database, and drives
 * temp-freeze expiry, reminders, particles and the [Frozen] tag.
 */
public class FreezeManager {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");
    private static final String TEAM_NAME = "axentra_frozen";

    private final App plugin;
    /** UUID -> expiry epoch millis (0 = permanent). Includes offline players. */
    private final Map<UUID, Long> frozen = new ConcurrentHashMap<>();
    /** Centre point frozen players may move around (online players only). */
    private final Map<UUID, Location> anchors = new ConcurrentHashMap<>();
    private final Map<UUID, String> previousTeams = new ConcurrentHashMap<>();
    private final Map<UUID, String> previousListNames = new ConcurrentHashMap<>();

    private BukkitTask secondTask;
    private BukkitTask particleTask;
    private long secondsElapsed;

    public FreezeManager(App plugin) {
        this.plugin = plugin;
    }

    public void start() {
        for (Map.Entry<String, Long> entry : plugin.getDatabaseManager().getFrozenPlayers().entrySet()) {
            try {
                frozen.put(UUID.fromString(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ignored) { }
        }

        // Players already online (e.g. after /reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isFrozen(player.getUniqueId())) applyEffects(player);
        }

        secondTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSecond, 20L, 20L);

        FileConfiguration protection = config();
        if (protection.getBoolean("freeze.particles.enabled", true)) {
            long interval = Math.max(1, protection.getLong("freeze.particles.interval-ticks", 10));
            particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnParticles, interval, interval);
        }
    }

    public void stop() {
        if (secondTask != null) secondTask.cancel();
        if (particleTask != null) particleTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isFrozen(player.getUniqueId())) clearEffects(player);
        }
    }

    // ── State ────────────────────────────────────────────────

    public boolean isFrozen(UUID uuid) {
        Long expiresAt = frozen.get(uuid);
        return expiresAt != null && (expiresAt == 0 || expiresAt > System.currentTimeMillis());
    }

    /** @return expiry epoch millis, 0 for permanent, -1 if not frozen */
    public long getExpiry(UUID uuid) {
        return isFrozen(uuid) ? frozen.get(uuid) : -1;
    }

    public Location getAnchor(UUID uuid) {
        return anchors.get(uuid);
    }

    /**
     * Freezes an online player.
     *
     * @param durationMillis 0 for a permanent freeze
     */
    public void freeze(Player target, CommandSender issuer, long durationMillis) {
        UUID uuid = target.getUniqueId();
        long expiresAt = durationMillis > 0 ? System.currentTimeMillis() + durationMillis : 0L;
        boolean alreadyFrozen = isFrozen(uuid);

        frozen.put(uuid, expiresAt);
        plugin.getDatabaseManager().freezePlayer(uuid.toString(), issuer.getName(), expiresAt);

        if (durationMillis > 0) {
            target.sendMessage(getMessage("freeze-tempfrozen").replace("%duration%", formatDuration(durationMillis)));
        } else {
            target.sendMessage(getMessage("freeze-frozen"));
        }
        if (!alreadyFrozen) applyEffects(target);

        String issuerUuid = issuer instanceof Player ? ((Player) issuer).getUniqueId().toString() : "CONSOLE";
        plugin.getDatabaseManager().logAction(issuer.getName(), issuerUuid, "FREEZE", target.getName(),
                durationMillis > 0 ? "Temp: " + formatDuration(durationMillis) : "");
    }

    /** Unfreezes a player. Works for offline players as long as a name is supplied for the audit log. */
    public void unfreeze(UUID uuid, String targetName, CommandSender issuer) {
        frozen.remove(uuid);
        plugin.getDatabaseManager().unfreezePlayer(uuid.toString());

        Player target = Bukkit.getPlayer(uuid);
        if (target != null) {
            clearEffects(target);
            target.sendMessage(getMessage("freeze-unfrozen"));
        }

        String issuerName = issuer != null ? issuer.getName() : "CONSOLE";
        String issuerUuid = issuer instanceof Player ? ((Player) issuer).getUniqueId().toString() : "CONSOLE";
        plugin.getDatabaseManager().logAction(issuerName, issuerUuid, "UNFREEZE", targetName,
                issuer == null ? "Temp freeze expired" : "");
    }

    /** Drops a player from the cache without touching the DB (used when logout-action bans them). */
    public void forget(UUID uuid) {
        frozen.remove(uuid);
        anchors.remove(uuid);
    }

    // ── Join / quit hooks ────────────────────────────────────

    public void handleJoin(Player player) {
        if (!isFrozen(player.getUniqueId())) return;
        player.sendMessage(getMessage("freeze-still-frozen"));
        applyEffects(player);
    }

    public void handleQuit(Player player) {
        if (isFrozen(player.getUniqueId())) clearEffects(player);
    }

    // ── Effects ──────────────────────────────────────────────

    private void applyEffects(Player player) {
        UUID uuid = player.getUniqueId();
        anchors.put(uuid, player.getLocation().clone());
        sendTitle(player, 100);

        FileConfiguration protection = config();
        String tag = color(protection.getString("freeze.tag", "&b&l[Frozen] &r"));

        if (protection.getBoolean("freeze.tag-display.tablist", true)) {
            previousListNames.put(uuid, player.getPlayerListName());
            player.setPlayerListName(tag + player.getPlayerListName());
        }

        if (protection.getBoolean("freeze.tag-display.nametag", true)) {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Team current = board.getEntryTeam(player.getName());
            if (current != null && !current.getName().equals(TEAM_NAME)) {
                previousTeams.put(uuid, current.getName());
            }
            Team team = board.getTeam(TEAM_NAME);
            if (team == null) team = board.registerNewTeam(TEAM_NAME);
            team.setPrefix(tag);
            team.addEntry(player.getName());
        }
    }

    private void clearEffects(Player player) {
        UUID uuid = player.getUniqueId();
        anchors.remove(uuid);
        player.resetTitle();

        String listName = previousListNames.remove(uuid);
        if (listName != null) player.setPlayerListName(listName);

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(TEAM_NAME);
        if (team != null && team.hasEntry(player.getName())) {
            team.removeEntry(player.getName());
            String previous = previousTeams.remove(uuid);
            Team previousTeam = previous != null ? board.getTeam(previous) : null;
            if (previousTeam != null) previousTeam.addEntry(player.getName());
        }
    }

    private void sendTitle(Player player, int stay) {
        player.sendTitle(getMessage("freeze-title"), getMessage("freeze-subtitle"), 0, stay, 10);
    }

    // ── Tasks ────────────────────────────────────────────────

    private void tickSecond() {
        secondsElapsed++;
        long now = System.currentTimeMillis();

        // Expire temp freezes (online and offline)
        for (Map.Entry<UUID, Long> entry : frozen.entrySet()) {
            long expiresAt = entry.getValue();
            if (expiresAt > 0 && expiresAt <= now) {
                UUID uuid = entry.getKey();
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                unfreeze(uuid, name != null ? name : uuid.toString(), null);
            }
        }

        FileConfiguration protection = config();
        int interval = protection.getInt("freeze.message-interval-seconds",
                protection.getInt("freeze.message-interval", 5));
        boolean remind = interval > 0 && secondsElapsed % interval == 0;
        boolean actionbar = protection.getBoolean("freeze.actionbar", true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!isFrozen(uuid)) continue;

            if (remind) {
                player.sendMessage(getMessage("freeze-reminder"));
                sendTitle(player, 40);
            }

            if (actionbar) {
                long expiresAt = frozen.getOrDefault(uuid, 0L);
                String bar = expiresAt > 0
                        ? getMessage("freeze-actionbar-temp").replace("%time%", formatDuration(expiresAt - now))
                        : getMessage("freeze-actionbar");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
            }
        }
    }

    private void spawnParticles() {
        FileConfiguration protection = config();
        Particle particle = resolveParticle(protection.getString("freeze.particles.type", "SNOWFLAKE"));
        int count = protection.getInt("freeze.particles.count", 8);
        double offset = protection.getDouble("freeze.particles.offset", 0.4);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isFrozen(player.getUniqueId())) continue;
            Location loc = player.getLocation().add(0, 1.0, 0);
            player.getWorld().spawnParticle(particle, loc, count, offset, 0.6, offset, 0.0);
        }
    }

    private Particle resolveParticle(String name) {
        try {
            Particle particle = Particle.valueOf(name.toUpperCase());
            // Particles that need extra data (dust colour, block type...) are not supported here
            if (particle.getDataType() == Void.class) return particle;
        } catch (IllegalArgumentException ignored) { }
        return Particle.SNOWFLAKE;
    }

    // ── Helpers ──────────────────────────────────────────────

    private FileConfiguration config() {
        return plugin.getFileManager().getProtection();
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        String message = plugin.getFileManager().getMessages().getString(path, "Message not found: " + path);
        return color(message.replace("%prefix%", prefix));
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Parses durations like "1d2h30m10s". Returns -1 if invalid. */
    public static long parseDuration(String input) {
        Matcher matcher = DURATION_PATTERN.matcher(input.toLowerCase());
        if (!matcher.matches()) return -1;
        long days    = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
        long hours   = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
        long minutes = matcher.group(3) != null ? Long.parseLong(matcher.group(3)) : 0;
        long seconds = matcher.group(4) != null ? Long.parseLong(matcher.group(4)) : 0;
        long millis = (days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000L;
        return millis > 0 ? millis : -1;
    }

    public static String formatDuration(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        long days    = seconds / 86400; seconds %= 86400;
        long hours   = seconds / 3600;  seconds %= 3600;
        long minutes = seconds / 60;    seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
