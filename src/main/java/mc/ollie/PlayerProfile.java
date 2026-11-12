package mc.ollie;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.UUID;

/**
 * Everything staff want to know about a player in one object: trust, risk score, alts, playtime,
 * first and last seen, cases, notes and the linked Discord account. Used by /lookup and appeals
 * in Discord and by /whois and /seen in-game.
 *
 * <p>{@link #load} reads the database, so call it off the main thread. IP addresses are never stored here.
 */
public class PlayerProfile {

    public String uuid;
    public String name;
    public String trust;
    public int riskScore;
    public int alts;
    public int bannedAlts;
    public boolean vpn;
    public long playtimeMinutes;
    public long firstJoin;
    public long lastSeen;
    public int caseCount;
    public int noteCount;
    public String discordName;
    public String discordId;
    public DatabaseManager.BanInfo activeBan;
    public boolean muted;

    /** Loads the profile, or returns null when the player never joined. Call off the main thread. */
    public static PlayerProfile load(App plugin, String nameOrUuid) {
        DatabaseManager db = plugin.getDatabaseManager();
        String uuid = isUuid(nameOrUuid) ? nameOrUuid : db.findUuidByName(nameOrUuid);
        if (uuid == null) return null;

        PlayerProfile p = new PlayerProfile();
        p.uuid = uuid;
        String known = db.getLastKnownName(uuid);
        p.name = known != null ? known : nameOrUuid;
        p.trust = plugin.getTrustManager() != null ? plugin.getTrustManager().getTrustLevel(uuid) : null;

        List<String> altUuids = db.getAltUuids(uuid);
        p.alts = altUuids.size();
        for (String alt : altUuids) {
            if (db.isBanned(alt)) p.bannedAlts++;
        }
        for (DatabaseManager.IpHistoryEntry ip : db.getIpHistory(uuid)) {
            DatabaseManager.VpnCacheEntry entry = db.getVpnCache(ip.ip);
            if (entry != null && entry.isVpn) { p.vpn = true; break; }
        }

        p.playtimeMinutes = Math.max(0, db.getPlaytimeMinutes(uuid));
        p.firstJoin = db.getFirstSessionTime(uuid);
        if (p.firstJoin == 0) p.firstJoin = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getFirstPlayed();
        p.lastSeen = db.getLastSessionTime(uuid);
        p.caseCount = db.getCasesByPlayerUuid(uuid).size();
        List<DatabaseManager.NoteInfo> notes = db.getNotesByUuid(uuid);
        p.noteCount = notes != null ? notes.size() : 0;

        DatabaseManager.DiscordLink link = db.getLinkByUuid(uuid);
        if (link != null) {
            p.discordName = link.discordName;
            p.discordId = link.discordId;
        }
        p.activeBan = db.getBan(uuid);
        p.muted = db.isMuted(uuid);
        p.riskScore = riskScore(plugin, p);
        return p;
    }

    /** The alt risk score (0-100) from protection.yml alts.risk-weights. */
    public static int riskScore(App plugin, PlayerProfile p) {
        FileConfiguration config = plugin.getFileManager().getProtection();
        int score = p.bannedAlts * config.getInt("alts.risk-weights.banned-alt", 30);
        if (p.vpn) score += config.getInt("alts.risk-weights.vpn-detected", 20);
        if (TrustManager.NEW.equals(p.trust)) score += config.getInt("alts.risk-weights.trust-new", 15);
        score += p.caseCount * config.getInt("alts.risk-weights.case-count", 5);
        return Math.min(100, score);
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return value.length() == 36;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** "3d 4h", "45m" */
    public static String formatMinutes(long minutes) {
        long d = minutes / 1440, h = (minutes % 1440) / 60, m = minutes % 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }
}
