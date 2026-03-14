package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class CartographyCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private static final Method OPEN_CARTOGRAPHY;

    static {
        Method m = null;
        try {
            m = Player.class.getMethod("openCartographyTable", Location.class, boolean.class);
        } catch (NoSuchMethodException ignored) {}
        OPEN_CARTOGRAPHY = m;
    }

    public CartographyCommand(App plugin) {
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
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        if (!sender.hasPermission("Axentra.cartography")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        if (OPEN_CARTOGRAPHY != null) {
            try {
                OPEN_CARTOGRAPHY.invoke(player, null, true);
                return true;
            } catch (Exception ignored) {}
        }
        // Fallback for standard Spigot
        player.openInventory(Bukkit.createInventory(player, InventoryType.CARTOGRAPHY));
        return true;
    }
}