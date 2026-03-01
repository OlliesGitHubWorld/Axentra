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
        // config.yml is handled automatically by Bukkit
        plugin.saveDefaultConfig();

        // Setup messages.yml manually
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false); // Copies from jar
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Sync any missing default values from the jar's messages.yml
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public void reload() {
        plugin.reloadConfig();
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public FileConfiguration getSettings() {
        return plugin.getConfig();
    }
}