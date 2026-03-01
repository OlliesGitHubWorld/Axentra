package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class RepairCommand implements CommandExecutor {

    private final App plugin;

    public RepairCommand(App plugin) {
        this.plugin = plugin;
    }

private String getMessage(String path) {
    return plugin.getMessageManager().getMessage(path);
}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("no-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("Axentra.repair")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            player.sendMessage(getMessage("repair-no-item"));
            return true;
        }

        ItemMeta meta = item.getItemMeta();

        if (!(meta instanceof Damageable) || ((Damageable) meta).getDamage() == 0) {
            player.sendMessage(getMessage("repair-not-damaged"));
            return true;
        }

        ((Damageable) meta).setDamage(0);
        item.setItemMeta(meta);
        player.sendMessage(getMessage("repair-success"));
        return true;
    }
}