package mc.ollie.health;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Calculates a 0-100 composite health score based on TPS, MSPT, memory,
 * entities-per-chunk, and open reports + staff presence.
 */
public class HealthScoreService {

    private final App plugin;
    private int cachedScore = 100;

    public HealthScoreService(App plugin) {
        this.plugin = plugin;
    }

    public int calculate() {
        FileConfiguration health = plugin.getFileManager().getHealth();
        TickProfiler profiler = plugin.getTickProfiler();
        if (profiler == null) return 100;

        int wTps = health.getInt("health-score.weights.tps", 40);
        int wMspt = health.getInt("health-score.weights.mspt", 20);
        int wMemory = health.getInt("health-score.weights.memory", 15);
        int wEntities = health.getInt("health-score.weights.entities-per-chunk", 15);
        int wReports = health.getInt("health-score.weights.reports-and-staff", 10);

        double scoreTps = calculateTpsScore(profiler.getTps1m());
        double scoreMspt = calculateMsptScore(profiler.getMsptAvg(), health);
        double scoreMemory = calculateMemoryScore(profiler);
        double scoreEntities = calculateEntityScore(health);
        double scoreReports = calculateReportScore();

        double total = (scoreTps * wTps + scoreMspt * wMspt + scoreMemory * wMemory +
                scoreEntities * wEntities + scoreReports * wReports) / 100.0;

        cachedScore = Math.max(0, Math.min(100, (int) Math.round(total)));
        return cachedScore;
    }

    public int getCachedScore() {
        return cachedScore;
    }

    public String getLabel() {
        return getLabel(cachedScore);
    }

    public String getLabel(int score) {
        FileConfiguration health = plugin.getFileManager().getHealth();
        int excellent = health.getInt("health-score.labels.excellent", 90);
        int good = health.getInt("health-score.labels.good", 70);
        int fair = health.getInt("health-score.labels.fair", 50);

        if (score >= excellent) return "Excellent";
        if (score >= good) return "Good";
        if (score >= fair) return "Fair";
        return "Poor";
    }

    public String getLabelColor(int score) {
        FileConfiguration health = plugin.getFileManager().getHealth();
        int excellent = health.getInt("health-score.labels.excellent", 90);
        int good = health.getInt("health-score.labels.good", 70);
        int fair = health.getInt("health-score.labels.fair", 50);

        if (score >= excellent) return "&a";
        if (score >= good) return "&e";
        if (score >= fair) return "&6";
        return "&c";
    }

    private double calculateTpsScore(double tps) {
        if (tps >= 19.5) return 100;
        if (tps <= 5.0) return 0;
        return ((tps - 5.0) / 14.5) * 100;
    }

    private double calculateMsptScore(double mspt, FileConfiguration health) {
        double perfect = health.getDouble("health-score.thresholds.perfect-mspt", 30.0);
        double bad = health.getDouble("health-score.thresholds.bad-mspt", 100.0);
        if (mspt <= perfect) return 100;
        if (mspt >= bad) return 0;
        return ((bad - mspt) / (bad - perfect)) * 100;
    }

    private double calculateMemoryScore(TickProfiler profiler) {
        int percent = profiler.getMemoryPercent();
        if (percent <= 50) return 100;
        if (percent >= 95) return 0;
        return ((95 - percent) / 45.0) * 100;
    }

    private double calculateEntityScore(FileConfiguration health) {
        int perfect = health.getInt("health-score.thresholds.perfect-entities-per-chunk", 20);
        int bad = health.getInt("health-score.thresholds.bad-entities-per-chunk", 100);

        int totalEntities = 0;
        int loadedChunks = 0;
        for (World world : Bukkit.getWorlds()) {
            totalEntities += world.getEntities().size();
            loadedChunks += world.getLoadedChunks().length;
        }
        if (loadedChunks == 0) return 100;

        double avgPerChunk = (double) totalEntities / loadedChunks;
        if (avgPerChunk <= perfect) return 100;
        if (avgPerChunk >= bad) return 0;
        return ((bad - avgPerChunk) / (bad - perfect)) * 100;
    }

    private double calculateReportScore() {
        int openReports = plugin.getDatabaseManager().getOpenReports().size();
        int onlineStaff = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("Axentra.staff")) onlineStaff++;
        }

        // Ideal: 0 open reports, at least 1 staff online
        double reportPenalty = Math.min(openReports * 10.0, 50.0);
        double staffBonus = onlineStaff > 0 ? 50.0 : 0.0;
        return Math.max(0, 100 - reportPenalty + staffBonus - 50);
    }
}
