package mc.ollie;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AFK (v1.9.0): /afk, automatic AFK after afk.auto-afk-minutes without real activity, a tag in
 * the tab list, an optional kick, and AFK time saved per session so it doesn't count as playtime
 * or for trust progression.
 *
 * <p>Only looking around, drifting in water or riding a vehicle doesn't count as activity, so
 * simple anti-AFK machines don't work (both configurable).
 */
public class AfkManager implements Listener {

    private record AfkState(long since, String reason) { }

    private final App plugin;
    private final Map<UUID, AfkState> afk = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, String> originalListName = new ConcurrentHashMap<>();
    private BukkitTask task;

    public AfkManager(App plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration config() { return plugin.getFileManager().getSettings(); }

    public void start() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) lastActivity.put(player.getUniqueId(), now);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::check, 100L, 100L);
    }

    /** Saves the AFK time of everyone still AFK (on shutdown). */
    public void stop() {
        if (task != null) task.cancel();
        for (UUID uuid : afk.keySet()) {
            AfkState state = afk.remove(uuid);
            if (state != null) plugin.getDatabaseManager().addAfkSeconds(uuid.toString(), (System.currentTimeMillis() - state.since()) / 1000);
            // Put the tab name back, otherwise the [AFK] tag sticks after a reload and gets doubled later
            Player player = Bukkit.getPlayer(uuid);
            String original = originalListName.remove(uuid);
            if (player != null && original != null) player.setPlayerListName(original);
        }
    }

    /** The tab name without any AFK tags, also ones left over from an earlier reload. */
    private String withoutTag(String listName, String tag) {
        if (tag.isEmpty() || listName == null) return listName;
        while (listName.startsWith(tag)) listName = listName.substring(tag.length());
        String plainTag = ChatColor.stripColor(tag);
        while (!plainTag.isEmpty() && ChatColor.stripColor(listName).startsWith(plainTag)
                && listName.contains(plainTag)) {
            listName = listName.substring(listName.indexOf(plainTag) + plainTag.length());
        }
        return listName;
    }

    public boolean isAfk(Player player) { return afk.containsKey(player.getUniqueId()); }

    public String getReason(Player player) {
        AfkState state = afk.get(player.getUniqueId());
        return state != null ? state.reason() : null;
    }

    /** How long the player has been AFK, in ms (0 when not AFK). */
    public long getAfkMillis(Player player) {
        AfkState state = afk.get(player.getUniqueId());
        return state != null ? System.currentTimeMillis() - state.since() : 0;
    }

    /** /afk: toggles AFK. */
    public void toggle(Player player, String reason) {
        if (isAfk(player)) setBack(player);
        else setAfk(player, reason, false);
    }

    private void setAfk(Player player, String reason, boolean auto) {
        if (afk.containsKey(player.getUniqueId())) return;
        afk.put(player.getUniqueId(), new AfkState(System.currentTimeMillis(), reason));

        String tag = ChatColor.translateAlternateColorCodes('&', config().getString("afk.tag-format", "&7[AFK] "));
        if (!tag.isEmpty()) {
            String clean = withoutTag(player.getPlayerListName(), tag);
            originalListName.put(player.getUniqueId(), clean);
            player.setPlayerListName(tag + clean);
        }

        String key = reason != null && !reason.isBlank() ? "afk-now-reason" : "afk-now";
        String message = plugin.getMessageManager().getMessage(key)
                .replace("%player%", player.getName()).replace("%reason%", reason != null ? reason : "");
        if (config().getBoolean("afk.broadcast", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(message);
        } else {
            player.sendMessage(message);
        }
    }

    private void setBack(Player player) {
        AfkState state = afk.remove(player.getUniqueId());
        if (state == null) return;
        String original = originalListName.remove(player.getUniqueId());
        if (original != null) player.setPlayerListName(original);

        long seconds = (System.currentTimeMillis() - state.since()) / 1000;
        String uuid = player.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().addAfkSeconds(uuid, seconds));

        String message = plugin.getMessageManager().getMessage("afk-back").replace("%player%", player.getName());
        if (config().getBoolean("afk.broadcast", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(message);
        } else {
            player.sendMessage(message);
        }
    }

    private void activity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        if (afk.containsKey(player.getUniqueId())) {
            // Most activity events are on the main thread; chat is not
            if (Bukkit.isPrimaryThread()) setBack(player);
            else Bukkit.getScheduler().runTask(plugin, () -> setBack(player));
        }
    }

    /** Every 5 seconds: auto-AFK and AFK kick. */
    private void check() {
        long now = System.currentTimeMillis();
        long autoMs = config().getLong("afk.auto-afk-minutes", 3) * 60_000L;
        long kickMs = config().getLong("afk.kick-minutes", 5) * 60_000L;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            AfkState state = afk.get(uuid);
            if (state == null) {
                long idle = now - lastActivity.getOrDefault(uuid, now);
                if (autoMs > 0 && idle >= autoMs && !plugin.isVanished(player)
                        && !player.hasPermission("Axentra.afk.exempt")) setAfk(player, null, true);
            } else if (kickMs > 0 && now - state.since() >= kickMs && !player.hasPermission("Axentra.afk.kickbypass")) {
                player.kickPlayer(plugin.getMessageManager().getMessage("afk-kick").replace("%prefix%", ""));
            }
        }
    }

    // ── Activity ────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        // Only looking around isn't activity
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        Player player = event.getPlayer();
        if (config().getBoolean("afk.ignore-water-movement", true) && player.isInWater()) return;
        if (config().getBoolean("afk.ignore-vehicle-movement", true) && player.isInsideVehicle()) return;
        activity(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) { activity(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().toLowerCase();
        if (cmd.equals("/afk") || cmd.startsWith("/afk ")) return;
        activity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) { activity(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) { activity(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { activity(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) activity(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lastActivity.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastActivity.remove(uuid);
        originalListName.remove(uuid);
        AfkState state = afk.remove(uuid);
        if (state != null) {
            long seconds = (System.currentTimeMillis() - state.since()) / 1000;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().addAfkSeconds(uuid.toString(), seconds));
        }
    }
}
