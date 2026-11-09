package mc.ollie.health;

import mc.ollie.App;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Monitors TPS and alerts staff when it drops below the configured threshold.
 */
public class TpsAlertManager {

    private final App plugin;
    private long lowTpsSince = 0;
    private long lastAlertTime = 0;
    private int activeAlertId = -1;
    private boolean alertActive = false;
    private String heaviestTpCommand = null;

    public TpsAlertManager(App plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Check every 5 seconds (100 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                check();
            }
        }.runTaskTimer(plugin, 200L, 100L);
    }

    public boolean isAlertActive() {
        return alertActive;
    }

    public int getActiveAlertId() {
        return activeAlertId;
    }

    private void check() {
        TickProfiler profiler = plugin.getTickProfiler();
        if (profiler == null) return;

        FileConfiguration health = plugin.getFileManager().getHealth();
        double threshold = health.getDouble("alerts.threshold", 15.0);
        int requiredDuration = health.getInt("alerts.duration-seconds", 30);
        int cooldown = health.getInt("alerts.cooldown-seconds", 300);

        double tps = profiler.getTps1m();
        long now = System.currentTimeMillis();

        if (tps < threshold) {
            if (lowTpsSince == 0) {
                lowTpsSince = now;
                return;
            }

            int elapsed = (int) ((now - lowTpsSince) / 1000);
            if (elapsed >= requiredDuration && !alertActive) {
                // Check cooldown
                if (now - lastAlertTime < cooldown * 1000L) return;

                triggerAlert(tps, elapsed);
                lastAlertTime = now;
                alertActive = true;
            }
        } else {
            if (alertActive) {
                // TPS recovered
                resolveAlert(tps);
                alertActive = false;
            }
            lowTpsSince = 0;
        }
    }

    private void triggerAlert(double tps, int durationSeconds) {
        HealthScoreService hs = plugin.getHealthScoreService();
        int healthScore = hs != null ? hs.calculate() : -1;

        // Save alert to DB async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            activeAlertId = plugin.getDatabaseManager().insertLagAlert(
                    System.currentTimeMillis(), tps, durationSeconds, healthScore);
        });

        // Find heaviest chunk for context
        String heaviestInfo = findHeaviestChunk();

        // Notify staff
        String prefix = plugin.getMessageManager().getMessage("prefix");
        // Message is already colour-translated, so the inserted colour code must be too
        String healthColor = color(hs != null ? hs.getLabelColor(healthScore) : "&c");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("Axentra.lag.alerts")) continue;

            String msg = plugin.getMessageManager().getMessage("lag-alert")
                    .replace("%tps%", String.format("%.1f", tps))
                    .replace("%duration%", String.valueOf(durationSeconds))
                    .replace("%health_color%", healthColor)
                    .replace("%health%", String.valueOf(healthScore));
            player.sendMessage(msg);

            if (heaviestInfo != null) {
                player.sendMessage(color(heaviestInfo));
            }

            // Clickable actions
            TextComponent actions = new TextComponent(color("  "));
            TextComponent tp = heaviestTpCommand != null
                    ? makeButton("&a[Teleport]", heaviestTpCommand, "Teleport to the heaviest chunk")
                    : makeButton("&a[Chunks]", "/lag chunks", "View heaviest chunks");
            TextComponent lag = makeButton("&e[Lag]", "/lag", "Server performance");
            actions.addExtra(tp);
            actions.addExtra(new TextComponent(" "));
            actions.addExtra(lag);
            player.spigot().sendMessage(actions);
        }
    }

    private void resolveAlert(double tps) {
        FileConfiguration health = plugin.getFileManager().getHealth();
        if (!health.getBoolean("alerts.recovery-message", true)) return;

        if (activeAlertId > 0) {
            int alertId = activeAlertId;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getDatabaseManager().resolveLagAlert(alertId));
        }

        String msg = plugin.getMessageManager().getMessage("lag-alert-recovery")
                .replace("%tps%", String.format("%.1f", tps));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("Axentra.lag.alerts")) {
                player.sendMessage(msg);
            }
        }

        activeAlertId = -1;
    }

    private String findHeaviestChunk() {
        double maxScore = 0;
        String result = null;
        heaviestTpCommand = null;
        ChunkScorer scorer = new ChunkScorer(plugin);

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                ChunkScorer.Result r = scorer.score(chunk);
                double score = r.score;

                if (score > maxScore) {
                    maxScore = score;
                    int bx = chunk.getX() * 16 + 8;
                    int bz = chunk.getZ() * 16 + 8;
                    String player = r.player;
                    heaviestTpCommand = "/lag tp " + world.getName() + " " + bx + " " + bz;

                    result = plugin.getMessageManager().getMessage("lag-alert-chunk")
                            .replace("%world%", world.getName())
                            .replace("%x%", String.valueOf(bx))
                            .replace("%z%", String.valueOf(bz))
                            .replace("%score%", String.format("%.0f", score));

                    if (player != null) {
                        result += "\n" + plugin.getMessageManager().getMessage("lag-alert-contributor")
                                .replace("%player%", player);
                    }
                }
            }
        }
        return result;
    }

    private TextComponent makeButton(String text, String command, String hover) {
        TextComponent btn = new TextComponent(color(text));
        btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(color(hover)).create()));
        return btn;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
