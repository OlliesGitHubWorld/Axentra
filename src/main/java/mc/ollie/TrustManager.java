package mc.ollie;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class TrustManager {

    private final App plugin;
    private BukkitTask periodicTask;

    public static final String NEW = "NEW";
    public static final String MEMBER = "MEMBER";
    public static final String TRUSTED = "TRUSTED";

    public TrustManager(App plugin) {
        this.plugin = plugin;
    }

    public void start() {
        periodicTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkAutoUpgrade(player.getUniqueId().toString());
            }
        }, 6000L, 6000L); // every 5 minutes
    }

    public void stop() {
        if (periodicTask != null) periodicTask.cancel();
    }

    public String getTrustLevel(String playerUuid) {
        DatabaseManager.TrustInfo info = plugin.getDatabaseManager().getTrust(playerUuid);
        if (info == null) return NEW;
        return info.level;
    }

    public boolean isManual(String playerUuid) {
        DatabaseManager.TrustInfo info = plugin.getDatabaseManager().getTrust(playerUuid);
        return info != null && info.manual;
    }

    public void setTrust(String playerUuid, String level, boolean manual, String reason, String changedBy) {
        String oldLevel = getTrustLevel(playerUuid);
        if (oldLevel.equals(level)) return;
        plugin.getDatabaseManager().setTrust(playerUuid, level, manual);
        plugin.getDatabaseManager().addTrustHistory(playerUuid, oldLevel, level, reason, changedBy);
    }

    public void resetTrust(String playerUuid, String changedBy) {
        String oldLevel = getTrustLevel(playerUuid);
        plugin.getDatabaseManager().setTrust(playerUuid, NEW, false);
        plugin.getDatabaseManager().addTrustHistory(playerUuid, oldLevel, NEW, "Manual reset", changedBy);
    }

    public void initPlayer(String playerUuid) {
        DatabaseManager.TrustInfo info = plugin.getDatabaseManager().getTrust(playerUuid);
        if (info == null) {
            plugin.getDatabaseManager().setTrust(playerUuid, NEW, false);
            plugin.getDatabaseManager().addTrustHistory(playerUuid, null, NEW, "First join", "SYSTEM");
        }
    }

    public void checkAutoUpgrade(String playerUuid) {
        if (isManual(playerUuid)) return;

        FileConfiguration config = plugin.getFileManager().getProtection();
        String currentLevel = getTrustLevel(playerUuid);
        long playtime = plugin.getDatabaseManager().getPlaytimeMinutes(playerUuid);
        boolean hasActivePunishment = plugin.getDatabaseManager().isBanned(playerUuid)
                || plugin.getDatabaseManager().isMuted(playerUuid);

        if (currentLevel.equals(NEW)) {
            long required = config.getLong("trust.member-playtime-minutes", 60);
            if (playtime >= required && !hasActivePunishment) {
                setTrust(playerUuid, MEMBER, false, "Playtime reached " + playtime + "m", "SYSTEM");
            }
        } else if (currentLevel.equals(MEMBER)) {
            long required = config.getLong("trust.trusted-playtime-minutes", 600);
            int cleanDays = config.getInt("trust.trusted-clean-days", 14);
            if (playtime >= required && !hasActivePunishment
                    && !plugin.getDatabaseManager().hasRecentCase(playerUuid, cleanDays)) {
                setTrust(playerUuid, TRUSTED, false, "Playtime reached " + playtime + "m, clean record", "SYSTEM");
            }
        }
    }

    public void onPunishment(String playerUuid, String punishmentType) {
        FileConfiguration config = plugin.getFileManager().getProtection();
        String currentLevel = getTrustLevel(playerUuid);

        if (punishmentType.equals("BAN") || punishmentType.equals("TEMPBAN")) {
            if (config.getBoolean("trust.downgrade.on-ban", true)) {
                if (!currentLevel.equals(NEW)) {
                    setTrust(playerUuid, NEW, false, punishmentType + " issued", "SYSTEM");
                }
            }
        } else if (punishmentType.equals("WARN") || punishmentType.equals("MUTE") || punishmentType.equals("TEMPMUTE")) {
            if (config.getBoolean("trust.downgrade.on-warn-or-mute", true)) {
                String downgraded = downgradeOne(currentLevel);
                if (!downgraded.equals(currentLevel)) {
                    setTrust(playerUuid, downgraded, false, punishmentType + " issued", "SYSTEM");
                }
            }
        }
    }

    private String downgradeOne(String level) {
        switch (level) {
            case TRUSTED: return MEMBER;
            case MEMBER: return NEW;
            default: return NEW;
        }
    }

    public static int levelToInt(String level) {
        switch (level) {
            case TRUSTED: return 2;
            case MEMBER: return 1;
            default: return 0;
        }
    }

    public long getMinutesToNextLevel(String playerUuid) {
        FileConfiguration config = plugin.getFileManager().getProtection();
        String level = getTrustLevel(playerUuid);
        long playtime = plugin.getDatabaseManager().getPlaytimeMinutes(playerUuid);

        if (level.equals(NEW)) {
            long required = config.getLong("trust.member-playtime-minutes", 60);
            return Math.max(0, required - playtime);
        } else if (level.equals(MEMBER)) {
            long required = config.getLong("trust.trusted-playtime-minutes", 600);
            return Math.max(0, required - playtime);
        }
        return 0;
    }
}
