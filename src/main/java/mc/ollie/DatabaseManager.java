package mc.ollie;

import java.io.File;
import java.sql.*;

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