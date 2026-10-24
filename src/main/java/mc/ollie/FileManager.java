package mc.ollie;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileManager {

    private final App plugin;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    private File punishmentsFile;
    private FileConfiguration punishmentsConfig;

    public FileManager(App plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        plugin.saveDefaultConfig();

        mergeConfigDefaults();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
            messagesConfig.options().copyDefaults(true);
            try {
                messagesConfig.save(messagesFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save messages.yml: " + e.getMessage());
            }
        }

        // Load punishments.yml
        punishmentsFile = new File(plugin.getDataFolder(), "punishments.yml");
        if (!punishmentsFile.exists()) {
            plugin.saveResource("punishments.yml", false);
        }
        punishmentsConfig = YamlConfiguration.loadConfiguration(punishmentsFile);

        InputStream defaultPunishments = plugin.getResource("punishments.yml");
        if (defaultPunishments != null) {
            YamlConfiguration defaultPunishConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultPunishments));
            punishmentsConfig.setDefaults(defaultPunishConfig);
            punishmentsConfig.options().copyDefaults(true);
            try {
                punishmentsConfig.save(punishmentsFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save punishments.yml: " + e.getMessage());
            }
        }
    }

    private void mergeConfigDefaults() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
            boolean changed = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!defaultConfig.isConfigurationSection(key) && !fileConfig.contains(key)) {
                    fileConfig.set(key, defaultConfig.get(key));
                    plugin.getLogger().info("Added missing config key: " + key);
                    changed = true;
                }
            }
            if (changed) {
                try {
                    fileConfig.save(configFile);
                    plugin.reloadConfig();
                    plugin.getLogger().info("Config updated with new default values.");
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not save config.yml: " + e.getMessage());
                }
            }
        }
    }

    public void reload() {
        plugin.reloadConfig();
        mergeConfigDefaults();
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
            messagesConfig.options().copyDefaults(true);
            try {
                messagesConfig.save(messagesFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save messages.yml: " + e.getMessage());
            }
        }

        // Reload punishments.yml
        punishmentsConfig = YamlConfiguration.loadConfiguration(punishmentsFile);
        InputStream defaultPunishments = plugin.getResource("punishments.yml");
        if (defaultPunishments != null) {
            YamlConfiguration defaultPunishConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultPunishments));
            punishmentsConfig.setDefaults(defaultPunishConfig);
            punishmentsConfig.options().copyDefaults(true);
            try {
                punishmentsConfig.save(punishmentsFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save punishments.yml: " + e.getMessage());
            }
        }
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public FileConfiguration getSettings() {
        return plugin.getConfig();
    }

    public FileConfiguration getPunishments() {
        return punishmentsConfig;
    }
}
