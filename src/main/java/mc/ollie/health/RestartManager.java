package mc.ollie.health;

import mc.ollie.App;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages scheduled server restarts with warnings, bossbar countdown, and sounds.
 */
public class RestartManager {

    private final App plugin;
    private BukkitTask countdownTask;
    private BukkitTask dailyCheckTask;
    private BossBar bossBar;
    private int secondsRemaining = -1;
    private boolean restarting = false;

    public RestartManager(App plugin) {
        this.plugin = plugin;
    }

    public void start() {
        FileConfiguration health = plugin.getFileManager().getHealth();
        List<String> dailyTimes = health.getStringList("restarts.daily");
        if (dailyTimes.isEmpty()) return;

        // Check every 30 seconds if it's time for a daily restart
        dailyCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkDailySchedule();
            }
        }.runTaskTimer(plugin, 600L, 600L); // every 30s
    }

    private void checkDailySchedule() {
        if (restarting) return;

        FileConfiguration health = plugin.getFileManager().getHealth();
        List<String> dailyTimes = health.getStringList("restarts.daily");
        String tzName = health.getString("restarts.timezone", "UTC");
        ZoneId zone = ZoneId.of(tzName);
        LocalTime now = LocalTime.now(zone);

        for (String timeStr : dailyTimes) {
            try {
                LocalTime target = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
                long diff = Duration.between(now, target).getSeconds();

                // Schedule when within 30 minutes and not yet counting down
                if (diff > 0 && diff <= 1800 && secondsRemaining < 0) {
                    scheduleRestart((int) diff);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    public void scheduleRestart(int seconds) {
        if (restarting) return;
        this.secondsRemaining = seconds;

        FileConfiguration health = plugin.getFileManager().getHealth();
        List<Integer> warnings = health.getIntegerList("restarts.warnings");
        int soundLast = health.getInt("restarts.sound-last-seconds", 10);
        String soundName = health.getString("restarts.sound", "BLOCK_NOTE_BLOCK_PLING");
        BarColor barColor;
        try { barColor = BarColor.valueOf(health.getString("restarts.bossbar-color", "RED")); }
        catch (Exception e) { barColor = BarColor.RED; }
        BarStyle barStyle;
        try { barStyle = BarStyle.valueOf(health.getString("restarts.bossbar-style", "SOLID")); }
        catch (Exception e) { barStyle = BarStyle.SOLID; }

        bossBar = Bukkit.createBossBar(
                ChatColor.RED + "Server restarting in " + formatTime(seconds),
                barColor, barStyle);

        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }

        Sound sound;
        try { sound = Sound.valueOf(soundName); }
        catch (Exception e) { sound = Sound.BLOCK_NOTE_BLOCK_PLING; }

        Sound finalSound = sound;
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (secondsRemaining <= 0) {
                    performRestart();
                    cancel();
                    return;
                }

                // Update bossbar
                if (bossBar != null) {
                    bossBar.setTitle(ChatColor.RED + "Server restarting in " + formatTime(secondsRemaining));
                    bossBar.setProgress(Math.max(0, Math.min(1, secondsRemaining / (double) seconds)));
                    // Add new players who may have joined
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!bossBar.getPlayers().contains(p)) bossBar.addPlayer(p);
                    }
                }

                // Warning messages
                if (warnings.contains(secondsRemaining)) {
                    String msg = plugin.getMessageManager().getMessage("restart-warning")
                            .replace("%time%", formatTime(secondsRemaining));
                    Bukkit.broadcastMessage(msg);

                    // Title for last 10s
                    if (secondsRemaining <= 10) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendTitle(
                                    ChatColor.RED + "" + ChatColor.BOLD + "RESTARTING",
                                    ChatColor.YELLOW + "in " + secondsRemaining + "s",
                                    0, 25, 5);
                        }
                    }
                }

                // Sound countdown
                if (secondsRemaining <= soundLast) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), finalSound, 1.0f,
                                secondsRemaining <= 3 ? 2.0f : 1.0f);
                    }
                }

                secondsRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        restarting = true;
    }

    public void cancelRestart() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        secondsRemaining = -1;
        restarting = false;
    }

    public boolean isRestarting() {
        return restarting;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    private void performRestart() {
        FileConfiguration health = plugin.getFileManager().getHealth();
        List<String> preCommands = health.getStringList("restarts.pre-commands");
        String kickMessage = ChatColor.translateAlternateColorCodes('&',
                health.getString("restarts.kick-message", "&cServer is restarting!"));
        boolean useSpigotRestart = health.getBoolean("restarts.use-spigot-restart", false);

        // Run pre-commands
        for (String cmd : preCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        // Log to audit
        plugin.getDatabaseManager().logAction("CONSOLE", "CONSOLE", "RESTART", null, "Scheduled restart");

        // Kick all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(kickMessage);
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        // Restart or shutdown
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (useSpigotRestart) {
                Bukkit.spigot().restart();
            } else {
                Bukkit.shutdown();
            }
        }, 20L);
    }

    public void stop() {
        cancelRestart();
        if (dailyCheckTask != null) {
            dailyCheckTask.cancel();
            dailyCheckTask = null;
        }
    }

    public static String formatTime(int seconds) {
        if (seconds >= 3600) {
            int h = seconds / 3600;
            int m = (seconds % 3600) / 60;
            return h + "h " + m + "m";
        }
        if (seconds >= 60) {
            int m = seconds / 60;
            int s = seconds % 60;
            return s > 0 ? m + "m " + s + "s" : m + "m";
        }
        return seconds + "s";
    }
}
