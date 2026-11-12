package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class SocialCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public SocialCommand(App plugin) {
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = label.toLowerCase();

        String link = plugin.getFileManager().getSettings().getString("social-links." + commandName, "");
        if (link.isEmpty()) {
            sender.sendMessage(getMessage("social-not-configured").replace("%command%", commandName));
            return true;
        }

        String messageKey = "social-" + commandName;
        String message = plugin.getFileManager().getMessages().getString(messageKey);

        if (message == null) {
            // Fallback format
            message = "%prefix%&b" + commandName.substring(0, 1).toUpperCase() + commandName.substring(1) + ": &f%link%";
        }

        String prefix = plugin.getFileManager().getMessages().getString("prefix", "");
        message = message.replace("%prefix%", prefix).replace("%link%", link);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        if (command.getName().equalsIgnoreCase("discord") && sender instanceof org.bukkit.entity.Player player
                && plugin.getLinkService() != null) {
            showLinkStatus(player);
        }
        return true;
    }

    /** /discord also shows whether you're linked, with a clickable [Link] button (v1.9.0). */
    private void showLinkStatus(org.bukkit.entity.Player player) {
        String uuid = player.getUniqueId().toString();
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            mc.ollie.DatabaseManager.DiscordLink link = plugin.getDatabaseManager().getLinkByUuid(uuid);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (link != null) {
                    player.sendMessage(getMessage("discord-linked-as")
                            .replace("%discord%", link.discordName != null ? link.discordName : link.discordId));
                    return;
                }
                net.md_5.bungee.api.chat.TextComponent text = new net.md_5.bungee.api.chat.TextComponent(getMessage("discord-not-linked"));
                net.md_5.bungee.api.chat.TextComponent button = new net.md_5.bungee.api.chat.TextComponent(getMessage("discord-link-button"));
                button.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/link"));
                button.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.hover.content.Text(getMessage("discord-link-hover"))));
                text.addExtra(button);
                player.spigot().sendMessage(text);
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
