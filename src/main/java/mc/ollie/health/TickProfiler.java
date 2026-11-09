package mc.ollie.health;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;

/**
 * Measures TPS (1m/5m/15m) and MSPT (avg/max over last 100 ticks).
 * Uses Paper's native API when available, falls back to own tick counter on Spigot.
 */
public class TickProfiler {

    private final App plugin;
    private boolean paperTps = false;
    private boolean paperMspt = false;

    // Spigot fallback TPS measurement
    private final long[] tickTimestamps = new long[1200]; // 1 minute of ticks
    private int tickIndex = 0;
    private int tickCount = 0;

    // MSPT tracking (last 100 ticks)
    private final double[] msptSamples = new double[100];
    private int msptIndex = 0;
    private int msptCount = 0;
    private long lastTickNano = 0;

    // Cached results
    private double tps1m = 20.0;
    private double tps5m = 20.0;
    private double tps15m = 20.0;
    private double msptAvg = 0.0;
    private double msptMax = 0.0;

    // Paper reflection
    private Method getTpsMethod;
    private Method getAverageTickTimeMethod;

    public TickProfiler(App plugin) {
        this.plugin = plugin;
        detectPaperApi();
    }

    private void detectPaperApi() {
        try {
            getTpsMethod = Bukkit.class.getMethod("getTPS");
            paperTps = true;
            plugin.logInfo("Paper TPS API detected.");
        } catch (NoSuchMethodException ignored) {}

        try {
            getAverageTickTimeMethod = Bukkit.class.getMethod("getAverageTickTime");
            paperMspt = true;
            plugin.logInfo("Paper MSPT API detected.");
        } catch (NoSuchMethodException ignored) {}

        if (!paperTps) {
            plugin.logInfo("Paper TPS API not found — using Spigot fallback tick counter.");
        }
    }

    public void start() {
        lastTickNano = System.nanoTime();

        // Runs every tick to track timing
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.nanoTime();
                long nowMs = System.currentTimeMillis();

                // MSPT: time since last tick in ms
                double mspt = (now - lastTickNano) / 1_000_000.0;
                lastTickNano = now;
                msptSamples[msptIndex] = mspt;
                msptIndex = (msptIndex + 1) % msptSamples.length;
                if (msptCount < msptSamples.length) msptCount++;

                // Spigot fallback: record tick timestamps for TPS calculation
                if (!paperTps) {
                    tickTimestamps[tickIndex] = nowMs;
                    tickIndex = (tickIndex + 1) % tickTimestamps.length;
                    if (tickCount < tickTimestamps.length) tickCount++;
                }

                // Update cached values every 20 ticks (1 second)
                if (msptIndex % 20 == 0) {
                    updateCachedValues();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void updateCachedValues() {
        // MSPT calculation
        if (msptCount > 0) {
            double sum = 0;
            double max = 0;
            for (int i = 0; i < msptCount; i++) {
                sum += msptSamples[i];
                if (msptSamples[i] > max) max = msptSamples[i];
            }
            msptAvg = sum / msptCount;
            msptMax = max;
        }

        if (paperTps) {
            try {
                double[] tps = (double[]) getTpsMethod.invoke(null);
                tps1m = tps[0];
                tps5m = tps[1];
                tps15m = tps[2];
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read Paper TPS: " + e.getMessage());
                paperTps = false;
            }
        }

        if (paperMspt) {
            try {
                double paperAvg = (double) getAverageTickTimeMethod.invoke(null);
                // Paper's value is more authoritative, blend with our own max tracking
                msptAvg = paperAvg;
            } catch (Exception e) {
                paperMspt = false;
            }
        }

        // Spigot fallback TPS
        if (!paperTps && tickCount > 1) {
            long now = System.currentTimeMillis();
            tps1m = calculateTps(now, 1200);  // 1 min = 1200 ticks
            tps5m = calculateTps(now, 1200);   // we only have 1 min buffer, reuse
            tps15m = calculateTps(now, 1200);
        }
    }

    private double calculateTps(long now, int maxTicks) {
        int samples = Math.min(tickCount, maxTicks);
        if (samples < 2) return 20.0;

        int oldest = (tickIndex - samples + tickTimestamps.length) % tickTimestamps.length;
        long elapsed = now - tickTimestamps[oldest];
        if (elapsed <= 0) return 20.0;

        double tps = (samples - 1) * 1000.0 / elapsed;
        return Math.min(tps, 20.0);
    }

    // ── Getters ──

    public double getTps1m() { return tps1m; }
    public double getTps5m() { return tps5m; }
    public double getTps15m() { return tps15m; }
    public double getMsptAvg() { return msptAvg; }
    public double getMsptMax() { return msptMax; }

    public long getMemoryUsed() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    public long getMemoryMax() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    public int getMemoryPercent() {
        long max = getMemoryMax();
        if (max <= 0) return 0;
        return (int) (getMemoryUsed() * 100 / max);
    }

    public boolean isPaperTps() { return paperTps; }
}
