package mc.ollie;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final App plugin;
    private Connection connection;

    public DatabaseManager(App plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS bans (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "player_name TEXT NOT NULL," +
                            "player_uuid TEXT NOT NULL," +
                            "reason TEXT," +
                            "banned_by TEXT NOT NULL," +
                            "banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "expires_at TIMESTAMP," +
                            "active INTEGER DEFAULT 1" +
                            ")"
            );

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS ip_bans (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "ip TEXT NOT NULL," +
                            "reason TEXT," +
                            "banned_by TEXT NOT NULL," +
                            "banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "expires_at TIMESTAMP," +
                            "active INTEGER DEFAULT 1" +
                            ")"
            );

            // Add expires_at to existing ip_bans tables that predate this column
            try {
                statement.executeUpdate("ALTER TABLE ip_bans ADD COLUMN expires_at TIMESTAMP");
            } catch (SQLException ignored) { }

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS warns (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "player_name TEXT NOT NULL," +
                            "player_uuid TEXT NOT NULL," +
                            "reason TEXT," +
                            "warned_by TEXT NOT NULL," +
                            "warned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "active INTEGER DEFAULT 1" +
                            ")"
            );

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mutes (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "player_name TEXT NOT NULL," +
                            "player_uuid TEXT NOT NULL," +
                            "reason TEXT," +
                            "muted_by TEXT NOT NULL," +
                            "muted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "active INTEGER DEFAULT 1" +
                            ")"
            );

            plugin.getLogger().info("Database connected!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }

    public void banPlayer(String playerName, String playerUuid, String reason, String bannedBy, Timestamp expiresAt) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO bans (player_name, player_uuid, reason, banned_by, expires_at) VALUES (?, ?, ?, ?, ?)"
            );
            stmt.setString(1, playerName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, reason);
            stmt.setString(4, bannedBy);
            stmt.setTimestamp(5, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to ban player: " + e.getMessage());
        }
    }

    public void unbanPlayer(String playerName) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE bans SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1"
            );
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
        }
    }

    public boolean isBannedByName(String playerName) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM bans WHERE LOWER(player_name) = LOWER(?) AND active = 1 AND (expires_at IS NULL OR expires_at > ?)"
            );
            stmt.setString(1, playerName);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check ban: " + e.getMessage());
            return false;
        }
    }

    public boolean isBanned(String playerUuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM bans WHERE player_uuid = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)"
            );
            stmt.setString(1, playerUuid);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check ban: " + e.getMessage());
            return false;
        }
    }

    public void banIp(String ip, String reason, String bannedBy, Timestamp expiresAt) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO ip_bans (ip, reason, banned_by, expires_at) VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, ip);
            stmt.setString(2, reason);
            stmt.setString(3, bannedBy);
            stmt.setTimestamp(4, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to IP ban: " + e.getMessage());
        }
    }

    public void unbanIp(String ip) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE ip_bans SET active = 0 WHERE ip = ? AND active = 1"
            );
            stmt.setString(1, ip);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unban IP: " + e.getMessage());
        }
    }

    public boolean isIpBanned(String ip) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT 1 FROM ip_bans WHERE ip = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)"
            );
            stmt.setString(1, ip);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check IP ban: " + e.getMessage());
            return false;
        }
    }

    public ResultSet getIpBan(String ip) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM ip_bans WHERE ip = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)"
            );
            stmt.setString(1, ip);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            return stmt.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get IP ban: " + e.getMessage());
            return null;
        }
    }

    public void warnPlayer(String playerName, String playerUuid, String reason, String warnedBy) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO warns (player_name, player_uuid, reason, warned_by) VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, playerName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, reason);
            stmt.setString(4, warnedBy);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to warn player: " + e.getMessage());
        }
    }

    public int getWarnCount(String playerUuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM warns WHERE player_uuid = ? AND active = 1"
            );
            stmt.setString(1, playerUuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warn count: " + e.getMessage());
        }
        return 0;
    }

    public int getWarnCountByName(String playerName) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM warns WHERE LOWER(player_name) = LOWER(?) AND active = 1"
            );
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warn count by name: " + e.getMessage());
        }
        return 0;
    }

    public ResultSet getWarnsByName(String playerName) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT reason, warned_by, warned_at FROM warns WHERE LOWER(player_name) = LOWER(?) AND active = 1 ORDER BY warned_at ASC"
            );
            stmt.setString(1, playerName);
            return stmt.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warns: " + e.getMessage());
            return null;
        }
    }

    public void clearWarns(String playerName) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE warns SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1"
            );
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clear warns: " + e.getMessage());
        }
    }

    public void mutePlayer(String playerName, String playerUuid, String reason, String mutedBy) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO mutes (player_name, player_uuid, reason, muted_by) VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, playerName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, reason);
            stmt.setString(4, mutedBy);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to mute player: " + e.getMessage());
        }
    }

    public void unmutePlayer(String playerName) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE mutes SET active = 0 WHERE LOWER(player_name) = LOWER(?) AND active = 1"
            );
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unmute player: " + e.getMessage());
        }
    }

    public boolean isMuted(String playerUuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT 1 FROM mutes WHERE player_uuid = ? AND active = 1"
            );
            stmt.setString(1, playerUuid);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check mute: " + e.getMessage());
            return false;
        }
    }

    public boolean isMutedByName(String playerName) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT 1 FROM mutes WHERE LOWER(player_name) = LOWER(?) AND active = 1"
            );
            stmt.setString(1, playerName);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check mute by name: " + e.getMessage());
            return false;
        }
    }

    public List<String> getActiveBannedNames() {
        List<String> names = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT DISTINCT player_name FROM bans WHERE active = 1 AND (expires_at IS NULL OR expires_at > ?)");
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) names.add(rs.getString("player_name"));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get banned names: " + e.getMessage());
        }
        return names;
    }

    public List<String> getActiveMutedNames() {
        List<String> names = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT DISTINCT player_name FROM mutes WHERE active = 1");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) names.add(rs.getString("player_name"));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get muted names: " + e.getMessage());
        }
        return names;
    }

    public List<String> getActiveWarnedNames() {
        List<String> names = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT DISTINCT player_name FROM warns WHERE active = 1");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) names.add(rs.getString("player_name"));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get warned names: " + e.getMessage());
        }
        return names;
    }

    public ResultSet getBan(String playerUuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM bans WHERE player_uuid = ? AND active = 1 AND (expires_at IS NULL OR expires_at > ?)"
            );
            stmt.setString(1, playerUuid);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            return stmt.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get ban: " + e.getMessage());
            return null;
        }
    }
}