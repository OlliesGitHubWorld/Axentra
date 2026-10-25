package mc.ollie;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class FileManager {

    private final App plugin;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    private File punishmentsFile;
    private FileConfiguration punishmentsConfig;

    private File protectionFile;
    private FileConfiguration protectionConfig;

    public FileManager(App plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        plugin.saveDefaultConfig();
        mergeDefaults("config.yml");

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        mergeDefaults("messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        punishmentsFile = new File(plugin.getDataFolder(), "punishments.yml");
        if (!punishmentsFile.exists()) plugin.saveResource("punishments.yml", false);
        mergeDefaults("punishments.yml");
        punishmentsConfig = YamlConfiguration.loadConfiguration(punishmentsFile);

        protectionFile = new File(plugin.getDataFolder(), "protection.yml");
        if (!protectionFile.exists()) plugin.saveResource("protection.yml", false);
        mergeDefaults("protection.yml");
        protectionConfig = YamlConfiguration.loadConfiguration(protectionFile);
    }

    /**
     * Merges missing keys from the bundled default yml into the user's file.
     * Uses the default file as a template: rebuilds the file from the template structure
     * while preserving user values. This guarantees correct ordering, no duplicate comments,
     * and proper placement of new keys with their comments.
     */
    private void mergeDefaults(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) return;

        // Load both configs via Bukkit YAML parser
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        InputStream cs = plugin.getResource(fileName);
        if (cs == null) return;
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(cs));

        // Check if any keys are missing
        Set<String> missingKeys = new LinkedHashSet<>();
        for (String key : defaultConfig.getKeys(true)) {
            if (!userConfig.contains(key)) {
                missingKeys.add(key);
            }
        }
        if (missingKeys.isEmpty()) return;

        // Read template (default) file as raw lines
        InputStream ts = plugin.getResource(fileName);
        if (ts == null) return;
        List<String> templateLines = readLines(ts);

        // Read user file as raw lines
        List<String> userLines;
        try {
            userLines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read " + fileName + ": " + e.getMessage());
            return;
        }

        // Map line numbers to key paths
        Map<Integer, String> templateLineToKey = mapLinesToKeys(templateLines);
        Map<Integer, String> userLineToKey = mapLinesToKeys(userLines);

        // Build output from template, replacing values with user's values where they exist
        List<String> output = new ArrayList<>();
        int i = 0;
        while (i < templateLines.size()) {
            String keyPath = templateLineToKey.get(i);

            if (keyPath == null) {
                // Comment, blank line, or list item from template — copy as-is
                output.add(templateLines.get(i));
                i++;
                continue;
            }

            int keyIndent = getIndent(templateLines.get(i));
            boolean isSection = defaultConfig.isConfigurationSection(keyPath);
            boolean isListInDefault = !isSection && defaultConfig.isList(keyPath);
            boolean isListInUser = !isSection && userConfig.isList(keyPath);
            boolean isList = isListInDefault || isListInUser;

            if (isSection) {
                // Section header — always copy from template (structure/comments stay correct)
                output.add(templateLines.get(i));
                i++;
                continue;
            }

            if (userConfig.contains(keyPath)) {
                // Key exists in user config — use user's value
                int userLine = findKeyLine(userLineToKey, keyPath);

                if (isList) {
                    if (userLine >= 0) {
                        // Copy user's key line
                        output.add(userLines.get(userLine));
                        // Copy user's list items
                        int ul = userLine + 1;
                        while (ul < userLines.size()) {
                            String uLine = userLines.get(ul);
                            if (uLine.trim().startsWith("-") && getIndent(uLine) > keyIndent) {
                                output.add(uLine);
                                ul++;
                            } else {
                                break;
                            }
                        }
                    } else {
                        // Fallback: copy from template
                        output.add(templateLines.get(i));
                    }
                    // Skip template's list items
                    i++;
                    while (i < templateLines.size()) {
                        String tl = templateLines.get(i);
                        if (tl.trim().startsWith("-") && getIndent(tl) > keyIndent) {
                            i++;
                        } else {
                            break;
                        }
                    }
                    continue;
                }

                // Simple value key
                if (userLine >= 0) {
                    output.add(userLines.get(userLine));
                } else {
                    output.add(templateLines.get(i));
                }
            } else {
                // Missing key — copy from template (new default value + comments are already in template)
                output.add(templateLines.get(i));
            }

            i++;
        }

        // Write the merged file
        try {
            Files.write(file.toPath(), output, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not update " + fileName + ": " + e.getMessage());
            return;
        }

        // Log added keys (only leaf keys, not sections)
        for (String key : missingKeys) {
            if (!defaultConfig.isConfigurationSection(key)) {
                plugin.getLogger().info("[" + fileName + "] Added missing key: " + key);
            }
        }

        if (fileName.equals("config.yml")) {
            plugin.reloadConfig();
        }
    }

    private List<String> readLines(InputStream stream) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ignored) {}
        return lines;
    }

    /**
     * Maps line numbers to full YAML key paths by tracking indentation.
     */
    private Map<Integer, String> mapLinesToKeys(List<String> lines) {
        Map<Integer, String> result = new LinkedHashMap<>();
        List<int[]> indentStack = new ArrayList<>();
        List<String> keyNames = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) continue;

            int colonPos = trimmed.indexOf(':');
            if (colonPos <= 0) continue;

            String key = trimmed.substring(0, colonPos).trim();
            if (key.contains(" ") || key.contains("\"") || key.contains("'")) continue;

            int indent = getIndent(line);

            // Pop deeper or equal indent levels
            while (!indentStack.isEmpty() && indentStack.get(indentStack.size() - 1)[0] >= indent) {
                indentStack.remove(indentStack.size() - 1);
                keyNames.remove(keyNames.size() - 1);
            }

            // Build full path
            StringBuilder fullPath = new StringBuilder();
            for (String parent : keyNames) {
                fullPath.append(parent).append(".");
            }
            fullPath.append(key);

            result.put(i, fullPath.toString());

            indentStack.add(new int[]{indent});
            keyNames.add(key);
        }

        return result;
    }

    private int getIndent(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') indent++;
            else break;
        }
        return indent;
    }

    private int findKeyLine(Map<Integer, String> lineToKey, String targetKey) {
        for (Map.Entry<Integer, String> entry : lineToKey.entrySet()) {
            if (entry.getValue().equals(targetKey)) return entry.getKey();
        }
        return -1;
    }

    public void reload() {
        plugin.reloadConfig();
        mergeDefaults("config.yml");

        mergeDefaults("messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        mergeDefaults("punishments.yml");
        punishmentsConfig = YamlConfiguration.loadConfiguration(punishmentsFile);

        mergeDefaults("protection.yml");
        protectionConfig = YamlConfiguration.loadConfiguration(protectionFile);
    }

    public FileConfiguration getMessages() { return messagesConfig; }
    public FileConfiguration getSettings() { return plugin.getConfig(); }
    public FileConfiguration getPunishments() { return punishmentsConfig; }
    public FileConfiguration getProtection() { return protectionConfig; }
}
