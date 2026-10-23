package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NotesCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public NotesCommand(App plugin) {
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
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
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
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.note")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("notes-usage"));
            return true;
        }

        String playerName = args[0];
        List<DatabaseManager.NoteInfo> notes = plugin.getDatabaseManager().getNotesByPlayer(playerName);

        if (notes.isEmpty()) {
            sender.sendMessage(getMessage("notes-empty").replace("%player%", playerName));
            return true;
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "  Notes for " + playerName
                + ChatColor.GRAY + " (" + notes.size() + ")");
        sender.sendMessage("");

        for (DatabaseManager.NoteInfo note : notes) {
            if (sender instanceof Player) {
                TextComponent line = new TextComponent("  ");

                TextComponent noteText = new TextComponent("#" + note.id + " ");
                noteText.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

                TextComponent content = new TextComponent(note.note);
                content.setColor(net.md_5.bungee.api.ChatColor.WHITE);

                TextComponent meta = new TextComponent(" - " + note.addedBy + " " + formatDate(note.addedAt));
                meta.setColor(net.md_5.bungee.api.ChatColor.GRAY);

                TextComponent removeBtn = new TextComponent(" [X]");
                removeBtn.setColor(net.md_5.bungee.api.ChatColor.RED);
                removeBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/note remove " + note.id));
                removeBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(ChatColor.RED + "Click to remove this note")));

                line.addExtra(noteText);
                line.addExtra(content);
                line.addExtra(meta);
                line.addExtra(removeBtn);
                ((Player) sender).spigot().sendMessage(line);
            } else {
                sender.sendMessage(ChatColor.YELLOW + "  #" + note.id + " " + ChatColor.WHITE + note.note
                        + ChatColor.GRAY + " - " + note.addedBy + " " + formatDate(note.addedAt));
            }
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                                ");
        return true;
    }

    private String formatDate(String timestamp) {
        if (timestamp == null) return "";
        if (timestamp.length() >= 10) return timestamp.substring(0, 10);
        return timestamp;
    }
}
