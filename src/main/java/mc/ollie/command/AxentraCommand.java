package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import mc.ollie.gui.StaffGUI;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class AxentraCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private StaffGUI staffGUI;

    public AxentraCommand(App plugin) {
        this.plugin = plugin;
        this.staffGUI = new StaffGUI(plugin);
    }

    public StaffGUI getStaffGUI() {
        return staffGUI;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String formatUptime() {
        long seconds = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long days    = seconds / 86400;
        long hours   = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;
        if (days > 0)    return days + "d " + hours + "h " + minutes + "m " + secs + "s";
        if (hours > 0)   return hours + "h " + minutes + "m " + secs + "s";
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
    }

    public boolean isUpdateCheckerEnabled() {
        return plugin.getFileManager().getSettings().getBoolean("update-checker.enabled", true);
    }

    /** Fetches the latest release version from GitHub, or null on failure or when the checker is disabled. */
    public String getLatestVersion() {
        if (!isUpdateCheckerEnabled()) return null;
        try {
            // releases/latest skips drafts and pre-releases
            URL url = new URL("https://api.github.com/repos/OlliesGitHubWorld/Axentra/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"tag_name\"\\s*:\\s*\"([^\"]*)\"").matcher(response);
            if (!matcher.find()) return null;
            String version = normalizeVersion(matcher.group(1));
            return version.isEmpty() ? null : version;
        } catch (Exception e) {
            return null;
        }
    }

    /** Strips "v", whitespace/newlines and suffixes like "-SNAPSHOT": " v1.8.0-beta\n" → "1.8.0". */
    public static String normalizeVersion(String version) {
        if (version == null) return "";
        String v = version.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1).trim();
        int end = 0;
        while (end < v.length() && (Character.isDigit(v.charAt(end)) || v.charAt(end) == '.')) end++;
        v = v.substring(0, end);
        while (v.endsWith(".")) v = v.substring(0, v.length() - 1);
        return v;
    }

    /** True only when {@code remote} is numerically higher than {@code current} (1.10.0 > 1.9.0, 1.8 == 1.8.0). */
    public static boolean isNewerVersion(String remote, String current) {
        String[] r = normalizeVersion(remote).split("\\.");
        String[] c = normalizeVersion(current).split("\\.");
        for (int i = 0; i < Math.max(r.length, c.length); i++) {
            long rp = i < r.length ? parsePart(r[i]) : 0;
            long cp = i < c.length ? parsePart(c[i]) : 0;
            if (rp != cp) return rp > cp;
        }
        return false;
    }

    private static long parsePart(String part) {
        try {
            return part.isEmpty() ? 0 : Long.parseLong(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> tabs = new java.util.ArrayList<>();
            tabs.add("help");
            tabs.add("information");
            if (sender.hasPermission("Axentra.admin")) {
                tabs.add("reload");
                tabs.add("upgrade");
                tabs.add("settings");
            }
            return tabs;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getMessage("axentra-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload":
                if (!sender.hasPermission("Axentra.admin")) {
                    sender.sendMessage(getMessage("no-permission"));
                    return true;
                }
                plugin.getFileManager().reload();
                sender.sendMessage(getMessage("reload-success"));
                break;

            case "upgrade":
                if (!sender.hasPermission("Axentra.admin")) {
                    sender.sendMessage(getMessage("no-permission"));
                    return true;
                }
                if (!isUpdateCheckerEnabled()) {
                    sender.sendMessage(getMessage("upgrade-disabled"));
                    return true;
                }
                sender.sendMessage(getMessage("upgrade-checking"));
                String currentVersion = normalizeVersion(plugin.getDescription().getVersion());
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String latestVersion = getLatestVersion();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (latestVersion == null) {
                            sender.sendMessage(getMessage("upgrade-failed"));
                            return;
                        }
                        if (!isNewerVersion(latestVersion, currentVersion)) {
                            sender.sendMessage(getMessage("upgrade-up-to-date")
                                    .replace("%version%", currentVersion));
                        } else {
                            sender.sendMessage(getMessage("upgrade-available")
                                    .replace("%current%", currentVersion)
                                    .replace("%latest%", latestVersion));
                            sender.sendMessage(ChatColor.AQUA + "https://github.com/OlliesGitHubWorld/Axentra/releases/latest");
                        }
                    });
                });
                break;

            case "help":
                String[] helpArgs = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];
                new HelpCommand(plugin).onCommand(sender, command, label, helpArgs);
                break;

            case "settings":
                if (!sender.hasPermission("Axentra.admin")) {
                    sender.sendMessage(getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(getMessage("no-player"));
                    return true;
                }
                staffGUI.open((Player) sender);
                break;

            case "information":
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "---- Axentra Information ----");
                sender.sendMessage(ChatColor.AQUA + "Version: "  + ChatColor.GRAY + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.AQUA + "Author: "   + ChatColor.GRAY + String.join(", ", plugin.getDescription().getAuthors()));
                sender.sendMessage(ChatColor.AQUA + "Commands: " + ChatColor.GRAY + plugin.getDescription().getCommands().size());
                sender.sendMessage(ChatColor.AQUA + "Support: "  + ChatColor.GRAY + "https://discord.gg/kNjvQPNTw");
                break;

            default:
                sender.sendMessage(getMessage("axentra-usage"));
                break;
        }

        return true;
    }
    }