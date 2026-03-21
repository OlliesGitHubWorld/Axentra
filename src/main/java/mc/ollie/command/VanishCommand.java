package mc.ollie.command;

import mc.ollie.App;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VanishCommand implements CommandExecutor, TabCompleter {

    private final App plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> actionBarTasks = new HashMap<>();

    public VanishCommand(App plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    // Called when player runs /vanish to unvanish
    public void unvanish(Player target, CommandSender sender) {
        if (!vanishedPlayers.remove(target.getUniqueId())) return;
        stopActionBar(target);
        target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        target.removePotionEffect(PotionEffectType.NIGHT_VISION);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, target);
            if (!online.equals(target) && online.hasPermission("Axentra.vanish.see")) {
                sendRealGameMode(online, target);
            }
        }
        if (cfg("vanish-fake-join-quit")) broadcastFakeJoin(target);
        target.sendMessage(getMessage("vanish-disabled"));
        if (sender != null && !sender.equals(target)) {
            sender.sendMessage(getMessage("vanish-disabled-other").replace("%player%", target.getName()));
        }
    }

    // Called on PlayerQuitEvent — cleans up without broadcasting fake join
    public void cleanupOnQuit(Player player) {
        if (!vanishedPlayers.remove(player.getUniqueId())) return;
        stopActionBar(player);
        // Show to others (they're disconnecting but keeps internal state clean)
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    private boolean cfg(String key) {
        return plugin.getFileManager().getSettings().getBoolean(key, true);
    }

    private void vanish(Player target, CommandSender sender) {
        vanishedPlayers.add(target.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(target)) continue;
            if (online.hasPermission("Axentra.vanish.see")) {
                if (cfg("vanish-ghost-effect")) sendFakeSpectatorMode(online, target);
            } else {
                online.hidePlayer(plugin, target);
            }
        }
        if (cfg("vanish-night-vision")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
        }
        if (cfg("vanish-action-bar")) startActionBar(target);
        if (cfg("vanish-fake-join-quit")) broadcastFakeQuit(target);
        target.sendMessage(getMessage("vanish-enabled"));
        if (sender != null && !sender.equals(target)) {
            sender.sendMessage(getMessage("vanish-enabled-other").replace("%player%", target.getName()));
        }
    }

    private void startActionBar(Player player) {
        BukkitTask existing = actionBarTasks.remove(player.getUniqueId());
        if (existing != null) existing.cancel();
        long interval = plugin.getFileManager().getSettings().getLong("vanish-action-bar-interval", 40L);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline() && isVanished(player)) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(getMessage("vanish-actionbar")));
            }
        }, 0L, interval);
        actionBarTasks.put(player.getUniqueId(), task);
    }

    private void stopActionBar(Player player) {
        BukkitTask task = actionBarTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void broadcastFakeQuit(Player player) {
        String msg = formatMessage("quit", player, Math.max(0, Bukkit.getOnlinePlayers().size() - 1));
        if (msg == null) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player) && !online.hasPermission("Axentra.vanish.see")) {
                online.sendMessage(msg);
            }
        }
    }

    private void broadcastFakeJoin(Player player) {
        String msg = formatMessage("join", player, Bukkit.getOnlinePlayers().size());
        if (msg == null) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player) && !online.hasPermission("Axentra.vanish.see")) {
                online.sendMessage(msg);
            }
        }
    }

    private String formatMessage(String key, Player player, int count) {
        String message = plugin.getFileManager().getMessages().getString(key);
        if (message == null || message.equalsIgnoreCase("none") || message.isEmpty()) return null;
        message = message.replace("%player%", player.getName());
        message = message.replace("%playercount%", String.valueOf(count));
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Makes vanished player appear as spectator (ghost) to the observer via NMS reflection
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendFakeSpectatorMode(Player observer, Player vanished) {
        try {
            Object nmsVanished = vanished.getClass().getMethod("getHandle").invoke(vanished);
            Object nmsObserver = observer.getClass().getMethod("getHandle").invoke(observer);

            Object gameModeObj = null;
            for (Field f : nmsVanished.getClass().getFields()) {
                if (f.getType().getSimpleName().equals("ServerPlayerGameMode")) {
                    gameModeObj = f.get(nmsVanished);
                    break;
                }
            }
            if (gameModeObj == null) {
                for (Field f : nmsVanished.getClass().getDeclaredFields()) {
                    if (f.getType().getSimpleName().equals("ServerPlayerGameMode")) {
                        f.setAccessible(true);
                        gameModeObj = f.get(nmsVanished);
                        break;
                    }
                }
            }
            if (gameModeObj == null) return;

            Field gameModeField = null;
            Object realGameType = null;
            Class<?> gameTypeClass = null;
            for (Field f : gameModeObj.getClass().getDeclaredFields()) {
                if (f.getType().isEnum() && f.getType().getSimpleName().contains("GameType")) {
                    f.setAccessible(true);
                    Object val = f.get(gameModeObj);
                    if (val != null) {
                        gameModeField = f;
                        realGameType = val;
                        gameTypeClass = f.getType();
                        break;
                    }
                }
            }
            if (gameModeField == null || gameTypeClass == null) return;

            Object spectatorType = null;
            for (Object constant : gameTypeClass.getEnumConstants()) {
                if (constant.toString().equalsIgnoreCase("spectator")) {
                    spectatorType = constant;
                    break;
                }
            }
            if (spectatorType == null) return;

            gameModeField.set(gameModeObj, spectatorType);
            Object packet = buildUpdateGameModePacket(nmsVanished);
            gameModeField.set(gameModeObj, realGameType);

            if (packet == null) return;
            sendPacketToPlayer(nmsObserver, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("VanishCommand: failed to send fake spectator packet: " + e.getMessage());
        }
    }

    public void sendRealGameMode(Player observer, Player vanished) {
        try {
            Object nmsVanished = vanished.getClass().getMethod("getHandle").invoke(vanished);
            Object nmsObserver = observer.getClass().getMethod("getHandle").invoke(observer);
            Object packet = buildUpdateGameModePacket(nmsVanished);
            if (packet == null) return;
            sendPacketToPlayer(nmsObserver, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("VanishCommand: failed to send real game mode packet: " + e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object buildUpdateGameModePacket(Object nmsPlayer) throws Exception {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
        Object updateGameModeAction = null;
        for (Object constant : actionClass.getEnumConstants()) {
            if (constant.toString().equals("UPDATE_GAME_MODE")) {
                updateGameModeAction = constant;
                break;
            }
        }
        if (updateGameModeAction == null) return null;
        EnumSet actions = EnumSet.noneOf((Class<Enum>) actionClass);
        actions.add(updateGameModeAction);
        Constructor<?> ctor = packetClass.getDeclaredConstructor(EnumSet.class, Collection.class);
        return ctor.newInstance(actions, List.of(nmsPlayer));
    }

    private void sendPacketToPlayer(Object nmsPlayer, Object packet) throws Exception {
        Object connection = null;
        for (Field f : nmsPlayer.getClass().getFields()) {
            String typeName = f.getType().getSimpleName();
            if (typeName.equals("ServerGamePacketListenerImpl") || typeName.equals("PlayerConnection")) {
                connection = f.get(nmsPlayer);
                break;
            }
        }
        if (connection == null) return;
        for (Method m : connection.getClass().getMethods()) {
            if (m.getName().equals("send") && m.getParameterCount() == 1) {
                m.invoke(connection, packet);
                return;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.vanish")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("no-player"));
                return true;
            }
            Player player = (Player) sender;
            if (isVanished(player)) {
                unvanish(player, null);
            } else {
                vanish(player, null);
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found").replace("%player%", args[0]));
            return true;
        }

        if (!sender.hasPermission("Axentra.vanish.others")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (isVanished(target)) {
            unvanish(target, sender);
        } else {
            vanish(target, sender);
        }
        return true;
    }
}