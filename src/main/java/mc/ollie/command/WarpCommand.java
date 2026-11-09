package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Handles /warp, /setwarp, /delwarp, /editwarp commands.
 */
public class WarpCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final Set<UUID> warmingUp = new HashSet<>();

    public WarpCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label.toLowerCase()) {
            case "setwarp":
                return handleSetWarp(sender, args);
            case "delwarp":
                return handleDelWarp(sender, args);
            case "warp":
                return handleWarp(sender, args);
            case "editwarp":
                return handleEditWarp(sender, args);
            default:
                return false;
        }
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.setwarp")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("prefix") + ChatColor.RED + "Usage: /setwarp <name>");
            return true;
        }

        String name = args[0];
        Location loc = player.getLocation();
        String defaultIcon = plugin.getFileManager().getSettings().getString("warps.default-icon", "ENDER_PEARL");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().setWarp(name, loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                    defaultIcon, player.getName(), null);
            plugin.getDatabaseManager().logAction(player.getName(), player.getUniqueId().toString(),
                    "SET_WARP", name, loc.getWorld().getName() + " " +
                            (int) loc.getX() + " " + (int) loc.getY() + " " + (int) loc.getZ());

            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(plugin.getMessageManager().getMessage("warp-set")
                            .replace("%name%", name)));
        });
        return true;
    }

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.delwarp")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().getMessage("prefix") + ChatColor.RED + "Usage: /delwarp <name>");
            return true;
        }

        String name = args[0];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.WarpInfo warp = plugin.getDatabaseManager().getWarp(name);
            if (warp == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessageManager().getMessage("warp-not-found")
                                .replace("%name%", name)));
                return;
            }
            plugin.getDatabaseManager().deleteWarp(name);
            plugin.getDatabaseManager().logAction(sender.getName(), "CONSOLE",
                    "DEL_WARP", name, null);

            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(plugin.getMessageManager().getMessage("warp-deleted")
                            .replace("%name%", name)));
        });
        return true;
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.warp")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
            return true;
        }
        if (args.length < 1) {
            // Show list
            showWarpList(sender);
            return true;
        }

        String name = args[0];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.WarpInfo warp = plugin.getDatabaseManager().getWarp(name);
            if (warp == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(plugin.getMessageManager().getMessage("warp-not-found")
                                .replace("%name%", name)));
                return;
            }

            // Permission check
            if (warp.permission != null && !warp.permission.isEmpty() &&
                    !player.hasPermission(warp.permission)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(plugin.getMessageManager().getMessage("warp-no-permission")));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                World world = Bukkit.getWorld(warp.world);
                if (world == null) {
                    player.sendMessage(plugin.getMessageManager().getMessage("warp-not-found")
                            .replace("%name%", name));
                    return;
                }

                Location target = new Location(world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
                int delay = plugin.getFileManager().getSettings().getInt("warps.teleport-delay", 3);

                if (delay <= 0) {
                    teleportToWarp(player, target, name);
                } else {
                    startWarpCountdown(player, target, name, delay);
                }
            });
        });
        return true;
    }

    private void startWarpCountdown(Player player, Location target, String name, int seconds) {
        if (warmingUp.contains(player.getUniqueId())) return;
        warmingUp.add(player.getUniqueId());

        Location startLoc = player.getLocation().clone();
        player.sendMessage(plugin.getMessageManager().getMessage("warp-countdown")
                .replace("%name%", name)
                .replace("%seconds%", String.valueOf(seconds)));

        new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    warmingUp.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // Check movement (block-level)
                if (player.getLocation().getBlockX() != startLoc.getBlockX() ||
                        player.getLocation().getBlockY() != startLoc.getBlockY() ||
                        player.getLocation().getBlockZ() != startLoc.getBlockZ()) {
                    player.sendMessage(plugin.getMessageManager().getMessage("warp-cancelled"));
                    warmingUp.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                remaining--;
                if (remaining <= 0) {
                    teleportToWarp(player, target, name);
                    warmingUp.remove(player.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void teleportToWarp(Player player, Location target, String name) {
        // Save for /back
        if (plugin.getFileManager().getSettings().getBoolean("enabled-modules.back", true)) {
            try {
                var backCmd = plugin.getCommand("back");
                if (backCmd != null && backCmd.getExecutor() instanceof BackCommand bc) {
                    bc.saveLocation(player);
                }
            } catch (Exception ignored) {}
        }

        player.teleport(target);
        player.sendMessage(plugin.getMessageManager().getMessage("warp-teleporting")
                .replace("%name%", name));
    }

    private void showWarpList(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> names = plugin.getDatabaseManager().getWarpNames();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (names.isEmpty()) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("warp-no-warps"));
                    return;
                }
                sender.sendMessage(plugin.getMessageManager().getMessage("warp-list-header"));
                sender.sendMessage(ChatColor.GRAY + String.join(", ", names));
            });
        });
    }

    private boolean handleEditWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.editwarp")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().getMessage("prefix") +
                    ChatColor.RED + "Usage: /editwarp <name> <icon|permission|order|rename> <value>");
            return true;
        }

        String name = args[0];
        String field = args[1].toLowerCase();
        String value = args[2];

        // Resolve the icon on the main thread ("hand" reads the player's held item)
        Material icon = null;
        if (field.equals("icon")) {
            if (value.equalsIgnoreCase("hand")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
                    return true;
                }
                icon = player.getInventory().getItemInMainHand().getType();
            } else {
                icon = Material.matchMaterial(value);
            }
            if (!isValidIcon(icon)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("warp-invalid-icon")
                        .replace("%icon%", value));
                return true;
            }
        }
        final Material newIcon = icon;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.WarpInfo warp = plugin.getDatabaseManager().getWarp(name);
            if (warp == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessageManager().getMessage("warp-not-found")
                                .replace("%name%", name)));
                return;
            }

            switch (field) {
                case "icon":
                    plugin.getDatabaseManager().updateWarpIcon(name, newIcon.name());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getMessageManager().getMessage("warp-icon-set")
                                    .replace("%name%", name).replace("%icon%", newIcon.name())));
                    break;
                case "order":
                    int position;
                    try {
                        position = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                sender.sendMessage(plugin.getMessageManager().getMessage("prefix") +
                                        ChatColor.RED + "Order must be a number (1 = first)."));
                        return;
                    }
                    int newIndex = plugin.getDatabaseManager().moveWarp(name, position - 1);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getMessageManager().getMessage("warp-order-set")
                                    .replace("%name%", name)
                                    .replace("%position%", String.valueOf(newIndex + 1))));
                    break;
                case "rename":
                    String result = renameWarp(plugin, sender, warp.name, value);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(result));
                    break;
                case "permission":
                    String perm = value.equalsIgnoreCase("none") ? null : value;
                    plugin.getDatabaseManager().updateWarpPermission(name, perm);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (perm == null) {
                            sender.sendMessage(plugin.getMessageManager().getMessage("warp-permission-removed")
                                    .replace("%name%", name));
                        } else {
                            sender.sendMessage(plugin.getMessageManager().getMessage("warp-permission-set")
                                    .replace("%name%", name).replace("%permission%", perm));
                        }
                    });
                    break;
                default:
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getMessageManager().getMessage("prefix") +
                                    ChatColor.RED + "Unknown field. Use 'icon', 'permission', 'order' or 'rename'."));
                    break;
            }
        });
        return true;
    }

    /**
     * Renames a warp after validating the new name. Runs database calls, so call it async.
     *
     * @return the message to send to the sender
     */
    public static String renameWarp(App plugin, CommandSender sender, String oldName, String newName) {
        if (!newName.matches("[A-Za-z0-9_\\-]{1,32}")) {
            return plugin.getMessageManager().getMessage("warp-invalid-name").replace("%name%", newName);
        }
        DatabaseManager.WarpInfo existing = plugin.getDatabaseManager().getWarp(newName);
        if (existing != null && !newName.equalsIgnoreCase(oldName)) {
            return plugin.getMessageManager().getMessage("warp-name-taken").replace("%name%", newName);
        }
        if (!plugin.getDatabaseManager().renameWarp(oldName, newName)) {
            return plugin.getMessageManager().getMessage("warp-not-found").replace("%name%", oldName);
        }
        String uuid = sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE";
        plugin.getDatabaseManager().logAction(sender.getName(), uuid, "RENAME_WARP", oldName, "to " + newName);
        return plugin.getMessageManager().getMessage("warp-renamed")
                .replace("%old%", oldName).replace("%name%", newName);
    }

    /** True if the material can be shown as an item in a GUI. */
    public static boolean isValidIcon(Material material) {
        return material != null && material.isItem() && !material.isAir();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = alias.toLowerCase();

        if (cmd.equals("warp") || cmd.equals("delwarp")) {
            if (args.length == 1) {
                List<String> names = plugin.getDatabaseManager().getWarpNames();
                names.removeIf(n -> !n.toLowerCase().startsWith(args[0].toLowerCase()));
                return names;
            }
        }

        if (cmd.equals("editwarp")) {
            if (args.length == 1) {
                List<String> names = plugin.getDatabaseManager().getWarpNames();
                names.removeIf(n -> !n.toLowerCase().startsWith(args[0].toLowerCase()));
                return names;
            }
            if (args.length == 2) {
                List<String> fields = new ArrayList<>(Arrays.asList("icon", "permission", "order", "rename"));
                fields.removeIf(f -> !f.startsWith(args[1].toLowerCase()));
                return fields;
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("icon")) {
                String typed = args[2].toLowerCase();
                List<String> icons = new ArrayList<>();
                if ("hand".startsWith(typed)) icons.add("hand");
                // Only list materials once something is typed, the full list is ~1500 entries
                if (!typed.isEmpty()) {
                    for (Material m : Material.values()) {
                        if (m.isLegacy() || !isValidIcon(m)) continue;
                        String key = m.name().toLowerCase();
                        if (key.startsWith(typed)) icons.add(key);
                    }
                }
                return icons;
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("order")) {
                List<String> positions = new ArrayList<>();
                int count = plugin.getDatabaseManager().getWarpNames().size();
                for (int i = 1; i <= count; i++) {
                    if (String.valueOf(i).startsWith(args[2])) positions.add(String.valueOf(i));
                }
                return positions;
            }
        }

        return Collections.emptyList();
    }
}
