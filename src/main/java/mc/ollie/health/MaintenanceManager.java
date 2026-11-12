package mc.ollie.health;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages maintenance mode: blocks non-whitelisted player logins,
 * custom MOTD, persists state across restarts via DB, auto-off after duration.
 */
public class MaintenanceManager implements Listener {

    private final App plugin;
    private boolean active = false;
    private final Set<String> whitelist = new HashSet<>();
    private long activatedAt = 0;
    private int durationMinutes = 0;
    private String activatedBy = null;
    private String reason = null;
    private BukkitTask countdownTask = null;
    private BossBar bossBar = null;

    public MaintenanceManager(App plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // Load persisted state from DB
        DatabaseManager.MaintenanceInfo info = plugin.getDatabaseManager().getMaintenance();
        if (info != null && info.active) {
            this.active = true;
            this.activatedAt = info.activatedAt;
            this.durationMinutes = info.durationMinutes;
            this.activatedBy = info.activatedBy;
            this.reason = info.reason;

            // Check if auto-off duration expired
            if (durationMinutes > 0) {
                long elapsed = (System.currentTimeMillis() - activatedAt) / 60000;
                if (elapsed >= durationMinutes) {
                    disable();
                    return;
                }
            }

            plugin.logInfo("Maintenance mode is active (restored from DB).");
        }

        // Load config whitelist
        FileConfiguration health = plugin.getFileManager().getHealth();
        for (String entry : health.getStringList("maintenance.whitelist")) {
            whitelist.add(entry.toLowerCase());
        }

        // Auto-off timer check every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                if (active && durationMinutes > 0) {
                    long elapsed = (System.currentTimeMillis() - activatedAt) / 60000;
                    if (elapsed >= durationMinutes) {
                        Bukkit.getScheduler().runTask(plugin, () -> disable());
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, 1200L);
    }

    /**
     * Starts maintenance after a countdown, broadcasting warnings, titles, sounds and a bossbar
     * to online players. countdownSeconds < 0 uses maintenance.countdown-seconds; 0 enables immediately.
     */
    public void start(String activatedBy, int durationMinutes, String reason, int countdownSeconds) {
        FileConfiguration health = plugin.getFileManager().getHealth();
        int seconds = countdownSeconds >= 0 ? countdownSeconds : health.getInt("maintenance.countdown-seconds", 0);
        if (seconds <= 0) {
            enable(activatedBy, durationMinutes, reason);
            return;
        }

        this.activatedBy = activatedBy;
        this.durationMinutes = durationMinutes;
        this.reason = reason;
        this.activatedAt = System.currentTimeMillis() + seconds * 1000L;

        int titleLast = health.getInt("maintenance.title-last-seconds", 10);
        int soundLast = health.getInt("maintenance.sound-last-seconds", 10);
        Sound tickSound = parseSound(health.getString("maintenance.sound", "BLOCK_NOTE_BLOCK_PLING"), Sound.BLOCK_NOTE_BLOCK_PLING);
        Sound endSound = parseSound(health.getString("maintenance.end-sound", "ENTITY_ENDER_DRAGON_GROWL"), Sound.ENTITY_ENDER_DRAGON_GROWL);

        if (health.getBoolean("maintenance.bossbar", true)) {
            BarColor barColor;
            try { barColor = BarColor.valueOf(health.getString("maintenance.bossbar-color", "RED")); }
            catch (Exception e) { barColor = BarColor.RED; }
            bossBar = Bukkit.createBossBar("", barColor, BarStyle.SOLID);
        }

        countdownTask = new BukkitRunnable() {
            int left = seconds;

            @Override
            public void run() {
                if (left <= 0) {
                    cancel();
                    removeBossBar();
                    // Final sound + title, then give players a moment to see it before the kick
                    String title = format(plugin.getMessageManager().getMessage("maintenance-start-title"));
                    String subtitle = format(plugin.getMessageManager().getMessage("maintenance-start-subtitle"));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(title, subtitle, 0, 50, 10);
                        if (endSound != null) p.playSound(p.getLocation(), endSound, 1.0f, 1.0f);
                    }
                    countdownTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        countdownTask = null;
                        enable(activatedBy, durationMinutes, reason);
                    }, 40L);
                    return;
                }

                if (bossBar != null) {
                    bossBar.setTitle(format(plugin.getMessageManager().getMessage("maintenance-bossbar")
                            .replace("%seconds%", String.valueOf(left))));
                    bossBar.setProgress(Math.max(0, Math.min(1, left / (double) seconds)));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!bossBar.getPlayers().contains(p)) bossBar.addPlayer(p);
                    }
                }

                if (left == seconds || left % 30 == 0 || left == 10 || left <= 5) {
                    Bukkit.broadcastMessage(format(plugin.getMessageManager().getMessage("maintenance-countdown")
                            .replace("%seconds%", String.valueOf(left))));
                }

                if (left <= titleLast) {
                    String title = format(plugin.getMessageManager().getMessage("maintenance-countdown-title"));
                    String subtitle = format(plugin.getMessageManager().getMessage("maintenance-countdown-subtitle")
                            .replace("%seconds%", String.valueOf(left)));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(title, subtitle, 0, 25, 5);
                    }
                }

                if (left <= soundLast && tickSound != null) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), tickSound, 1.0f, left <= 3 ? 2.0f : 1.0f);
                    }
                }
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private Sound parseSound(String name, Sound fallback) {
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("none")) return null;
        try { return Sound.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }

    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public boolean isPending() {
        return countdownTask != null;
    }

    public void cancelCountdown() {
        if (countdownTask == null) return;
        countdownTask.cancel();
        countdownTask = null;
        removeBossBar();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.resetTitle();
        }
        Bukkit.broadcastMessage(plugin.getMessageManager().getMessage("maintenance-cancelled"));
    }

    public void enable(String activatedBy, int durationMinutes, String reason) {
        this.active = true;
        this.activatedAt = System.currentTimeMillis();
        this.durationMinutes = durationMinutes;
        this.activatedBy = activatedBy;
        this.reason = reason;

        plugin.getDatabaseManager().setMaintenance(true, activatedBy, durationMinutes, reason);
        plugin.getDatabaseManager().logAction(activatedBy, activatedBy, "MAINTENANCE_ON", null,
                (durationMinutes > 0 ? "Duration: " + durationMinutes + "m" : "Indefinite")
                        + (reason != null ? " | Reason: " + reason : ""));

        // Kick non-whitelisted/non-bypass players
        FileConfiguration health = plugin.getFileManager().getHealth();
        String kickMsg = format(health.getString("maintenance.kick-message", "&cMaintenance mode."));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!canBypass(p)) {
                p.kickPlayer(kickMsg);
            }
        }
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.MaintenanceToggleEvent(true, activatedBy, reason, durationMinutes));
    }

    public void disable() {
        this.active = false;
        plugin.getDatabaseManager().clearMaintenance();
        plugin.getDatabaseManager().logAction("SYSTEM", "SYSTEM", "MAINTENANCE_OFF", null, null);
        mc.ollie.event.AxentraEvent.fire(new mc.ollie.event.MaintenanceToggleEvent(false, activatedBy, reason, 0));
    }

    public boolean isActive() {
        return active;
    }

    public long getActivatedAt() {
        return activatedAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getActivatedBy() {
        return activatedBy;
    }

    public String getReason() {
        return reason;
    }

    /**
     * Replaces maintenance placeholders and translates color codes.
     * Placeholders: %reason%, %by%, %since%, %duration%, %remaining%, %ends%, %discord%, %website%
     */
    public String format(String text) {
        if (text == null) return "";
        FileConfiguration health = plugin.getFileManager().getHealth();
        FileConfiguration settings = plugin.getFileManager().getSettings();
        String unknown = health.getString("maintenance.unknown-duration-text", "Unknown");

        String shownReason = (reason == null || reason.isEmpty())
                ? health.getString("maintenance.default-reason", "Scheduled maintenance")
                : reason;
        String duration = durationMinutes > 0 ? formatMinutes(durationMinutes) : unknown;
        String remaining = unknown;
        String ends = unknown;
        if (durationMinutes > 0) {
            long endsAt = activatedAt + durationMinutes * 60000L;
            long left = Math.max(0, (endsAt - System.currentTimeMillis()) / 60000);
            remaining = left > 0 ? formatMinutes(left) : "<1m";
            ends = new SimpleDateFormat("HH:mm").format(new Date(endsAt));
        }

        return ChatColor.translateAlternateColorCodes('&', text
                .replace("%reason%", shownReason)
                .replace("%by%", activatedBy != null ? activatedBy : "Staff")
                .replace("%since%", new SimpleDateFormat("HH:mm").format(new Date(activatedAt)))
                .replace("%duration%", duration)
                .replace("%remaining%", remaining)
                .replace("%ends%", ends)
                .replace("%discord%", settings.getString("social-links.discord", ""))
                .replace("%website%", settings.getString("social-links.website", "")));
    }

    private String formatMinutes(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        if (h > 0 && m > 0) return h + "h " + m + "m";
        if (h > 0) return h + "h";
        return m + "m";
    }

    public boolean canBypass(Player player) {
        if (player.hasPermission("Axentra.maintenance.bypass")) return true;
        if (whitelist.contains(player.getName().toLowerCase())) return true;
        if (whitelist.contains(player.getUniqueId().toString())) return true;
        return false;
    }

    public void addToWhitelist(String nameOrUuid) {
        whitelist.add(nameOrUuid.toLowerCase());
    }

    public void removeFromWhitelist(String nameOrUuid) {
        whitelist.remove(nameOrUuid.toLowerCase());
    }

    /**
     * Permissions are only known once the player object exists, so the check happens here
     * instead of in AsyncPlayerPreLoginEvent. canBypass covers the whitelist and the bypass permission.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!active || event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
        if (canBypass(event.getPlayer())) return;

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, joinDeniedMessage());
    }

    private String joinDeniedMessage() {
        FileConfiguration health = plugin.getFileManager().getHealth();
        return format(health.getString("maintenance.join-denied",
                health.getString("maintenance.kick-message", "&cServer is under maintenance.")));
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (!active) return;

        FileConfiguration health = plugin.getFileManager().getHealth();
        String motd = format(health.getString("maintenance.motd", "&c&lMaintenance Mode"));
        event.setMotd(motd);

        if (health.getBoolean("maintenance.hide-players", true)) {
            event.setMaxPlayers(0);
        }
    }
}
