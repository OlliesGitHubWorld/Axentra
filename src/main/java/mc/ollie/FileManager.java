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

    public FileManager(App plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        plugin.saveDefaultConfig();

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
    }

    public void reload() {
        plugin.reloadConfig();
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
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public FileConfiguration getSettings() {
        return plugin.getConfig();
    }
}