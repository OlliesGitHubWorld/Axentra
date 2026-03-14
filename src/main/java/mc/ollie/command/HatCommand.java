package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.List;

public class HatCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public HatCommand(App plugin) {
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

        if (!sender.hasPermission("Axentra.hat")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        PlayerInventory inv = player.getInventory();
        ItemStack hand = inv.getItemInMainHand();

        if (hand.getType() == Material.AIR) {
            sender.sendMessage(getMessage("hat-no-item"));
            return true;
        }

        ItemStack currentHelmet = inv.getHelmet();
        inv.setHelmet(hand.clone());
        inv.setItemInMainHand(currentHelmet != null ? currentHelmet : new ItemStack(Material.AIR));

        sender.sendMessage(getMessage("hat-success"));
        return true;
    }
}