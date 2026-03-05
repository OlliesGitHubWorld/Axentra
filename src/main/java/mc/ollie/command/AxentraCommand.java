package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AxentraCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public AxentraCommand(App plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getLatestVersion() {
        try {
            URL url = new URL("https://api.github.com/repos/OlliesGitHubWorld/Axentra/releases");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            String tag = json.split("\"tag_name\":\"")[1].split("\"")[0];
            return tag.startsWith("v") ? tag.substring(1) : tag;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "upgrade", "help", "information");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("axentra-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload":
                plugin.getFileManager().reload();
                sender.sendMessage(getMessage("reload-success"));
                break;

            case "upgrade":
                sender.sendMessage(getMessage("upgrade-checking"));
                String currentVersion = plugin.getDescription().getVersion();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String latestVersion = getLatestVersion();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (latestVersion == null) {
                            sender.sendMessage(getMessage("upgrade-failed"));
                            return;
                        }

                        if (latestVersion.equalsIgnoreCase(currentVersion)) {
                            sender.sendMessage(getMessage("upgrade-up-to-date")
                                    .replace("%version%", currentVersion));
                        } else {
                            sender.sendMessage(getMessage("upgrade-available")
                                    .replace("%current%", currentVersion)
                                    .replace("%latest%", latestVersion));
                        }
                    });
                });
                break;

            case "help":
                String prefix = ChatColor.translateAlternateColorCodes('&',
                        plugin.getFileManager().getMessages().getString("prefix", ""));
                sender.sendMessage( ChatColor.BOLD + "" + ChatColor.AQUA + "---- Axentra Help ----");
                sender.sendMessage( ChatColor.AQUA + "/axentra <reload | upgrade | help | information>" + ChatColor.GRAY + " - Main Axentra command");
                sender.sendMessage( ChatColor.AQUA + "/kick <player> [reason]" + ChatColor.GRAY + " - Kicks a player from the server");
                sender.sendMessage( ChatColor.AQUA + "/ban <player> [reason]" + ChatColor.GRAY + " - Bans a player from the server");
                sender.sendMessage( ChatColor.AQUA + "/unban <player>" + ChatColor.GRAY + " - Unbans a player from the server");
                sender.sendMessage( ChatColor.AQUA + "/fly [player]" + ChatColor.GRAY + " - Toggle flight mode for yourself or others");
                sender.sendMessage( ChatColor.AQUA + "/clear [player]" + ChatColor.GRAY + " - Clears the player's inventory");
                sender.sendMessage( ChatColor.AQUA + "/repair" + ChatColor.GRAY + " - Repairs the item in your hand");
                break;

            case "information":
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "---- Axentra Information ----");
                sender.sendMessage(ChatColor.AQUA + "Description: " + ChatColor.GRAY + plugin.getDescription().getDescription());
                sender.sendMessage(ChatColor.AQUA + "Version: " + ChatColor.GRAY + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.AQUA + "Author: " + ChatColor.GRAY + String.join(", ", plugin.getDescription().getAuthors()));
                sender.sendMessage(ChatColor.AQUA + "Commands: " + ChatColor.GRAY + plugin.getDescription().getCommands().size());
                break;

            default:
                sender.sendMessage(getMessage("axentra-usage"));
                break;
        }

        return true;
    }
}