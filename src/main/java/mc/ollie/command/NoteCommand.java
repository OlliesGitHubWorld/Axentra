package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NoteCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public NoteCommand(App plugin) {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                List<String> completions = new ArrayList<>();
                String input = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
                for (String name : plugin.getDatabaseManager().getKnownPlayerNames()) {
                    if (name.toLowerCase().startsWith(input) && !completions.contains(name)) {
                        completions.add(name);
                    }
                }
                return completions;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.note")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("note-usage"));
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "add": {
                if (args.length < 3) {
                    sender.sendMessage(getMessage("note-usage"));
                    return true;
                }

                String playerName = args[1];
                String uuid = getUuid(playerName);

                StringBuilder noteBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    noteBuilder.append(args[i]);
                    if (i < args.length - 1) noteBuilder.append(" ");
                }
                String note = noteBuilder.toString();

                plugin.getDatabaseManager().addNote(playerName, uuid, note, sender.getName());

                // Log audit
                logAudit(sender, "NOTE_ADD", playerName, note);

                sender.sendMessage(getMessage("note-added")
                        .replace("%player%", playerName)
                        .replace("%note%", note));
                break;
            }

            case "remove": {
                int noteId;
                try {
                    noteId = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("note-invalid-id"));
                    return true;
                }

                plugin.getDatabaseManager().removeNote(noteId);

                // Log audit
                logAudit(sender, "NOTE_REMOVE", "#" + noteId, null);

                sender.sendMessage(getMessage("note-removed").replace("%id%", String.valueOf(noteId)));
                break;
            }

            default:
                sender.sendMessage(getMessage("note-usage"));
        }

        return true;
    }

    private String getUuid(String playerName) {
        Player online = Bukkit.getPlayer(playerName);
        if (online != null) return online.getUniqueId().toString();
        return Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
    }

    private void logAudit(CommandSender sender, String action, String target, String details) {
        String staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
        plugin.getDatabaseManager().logAction(sender.getName(), staffUuid, action, target, details);
    }
}
