package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Syncs Discord roles with permission nodes {@code Axentra.discord.role.<name>}, mapped in
 * discord.yml under role-sync.roles. Works with any permission plugin, because it only reads
 * permissions. Roles that aren't mapped are never touched.
 *
 * <ul>
 *   <li>MINECRAFT_TO_DISCORD: a player with the permission gets the role, and loses it without.</li>
 *   <li>DISCORD_TO_MINECRAFT: a member with the role gets the permission through a
 *       PermissionAttachment while online. This can't change groups in LuckPerms and others.</li>
 *   <li>BOTH: both of the above. Roles are only added from Minecraft, never removed, so a role
 *       given by hand in Discord isn't taken away again.</li>
 * </ul>
 */
public class RoleSyncService extends ListenerAdapter implements Listener {

    public static final String PERMISSION_PREFIX = "Axentra.discord.role.";

    private final App plugin;
    private final DiscordManager discord;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private BukkitTask task;

    public RoleSyncService(App plugin, DiscordManager discord, LinkService links) {
        this.plugin = plugin;
        this.discord = discord;
        discord.addListener(this);
        discord.onReady(jda -> Bukkit.getScheduler().runTask(plugin, this::restartTimer));
        discord.onReload(this::restartTimer);

        links.onLinked(link -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(UUID.fromString(link.playerUuid));
            if (player != null) sync(player, link);
        }));
        links.onUnlinked(this::afterUnlink);
    }

    private boolean enabled() {
        return discord.config().getBoolean("role-sync.enabled", false) && discord.isConnected();
    }

    private String direction() {
        return discord.config().getString("role-sync.direction", "MINECRAFT_TO_DISCORD").toUpperCase(Locale.ROOT);
    }

    private boolean toDiscord() { return !direction().equals("DISCORD_TO_MINECRAFT"); }
    private boolean toMinecraft() { return !direction().equals("MINECRAFT_TO_DISCORD"); }

    /** name -> role ID from role-sync.roles */
    private Map<String, String> mapping() {
        Map<String, String> map = new LinkedHashMap<>();
        ConfigurationSection section = discord.config().getConfigurationSection("role-sync.roles");
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            String id = section.getString(key, "").trim();
            if (!id.isEmpty()) map.put(key.toLowerCase(Locale.ROOT), id);
        }
        return map;
    }

    /** (Re)starts the periodic sync; called on connect and can be called after a reload. */
    public void restartTimer() {
        if (task != null) task.cancel();
        long minutes = Math.max(1, discord.config().getInt("role-sync.interval-minutes", 10));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled()) return;
            for (Player player : Bukkit.getOnlinePlayers()) syncAsync(player);
        }, 20L * 30, 20L * 60 * minutes);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Give permission plugins a moment to load the player's groups
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && enabled()) syncAsync(player);
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PermissionAttachment attachment = attachments.remove(event.getPlayer().getUniqueId());
        if (attachment != null) {
            try { event.getPlayer().removeAttachment(attachment); } catch (IllegalArgumentException ignored) { }
        }
    }

    /** Looks up the link off the main thread, then syncs. Must be called on the main thread. */
    public void syncAsync(Player player) {
        String uuid = player.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.DiscordLink link = plugin.getDatabaseManager().getLinkByUuid(uuid);
            if (link == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) sync(player, link);
            });
        });
    }

    /** Main thread: reads permissions, then changes roles through JDA (queued, never blocking). */
    private void sync(Player player, DatabaseManager.DiscordLink link) {
        if (!enabled()) return;
        Guild guild = discord.getGuild();
        if (guild == null) return;
        Map<String, String> mapping = mapping();
        if (mapping.isEmpty()) return;

        // Read permissions without our own Discord-granted attachment, otherwise a role removed in
        // Discord would be added straight back. Removed and restored in the same tick.
        PermissionAttachment ours = attachments.remove(player.getUniqueId());
        if (ours != null) {
            try { player.removeAttachment(ours); } catch (IllegalArgumentException ignored) { }
        }
        Map<String, Boolean> hasPermission = new HashMap<>();
        for (String name : mapping.keySet()) {
            hasPermission.put(name, player.hasPermission(PERMISSION_PREFIX + name));
        }
        if (ours != null) {
            PermissionAttachment restored = player.addAttachment(plugin);
            ours.getPermissions().forEach(restored::setPermission);
            attachments.put(player.getUniqueId(), restored);
        }
        boolean addOnly = direction().equals("BOTH");
        UUID uuid = player.getUniqueId();

        guild.retrieveMemberById(link.discordId).queue(member -> {
            Set<String> memberRoles = new HashSet<>();
            for (Role role : member.getRoles()) memberRoles.add(role.getId());

            if (toDiscord()) {
                for (Map.Entry<String, String> entry : mapping.entrySet()) {
                    Role role = guild.getRoleById(entry.getValue());
                    if (role == null || !canManage(guild, role)) continue;
                    boolean want = hasPermission.get(entry.getKey());
                    boolean has = memberRoles.contains(role.getId());
                    if (want && !has) {
                        guild.addRoleToMember(member, role).queue(null, err -> { });
                        memberRoles.add(role.getId());
                    } else if (!want && has && !addOnly) {
                        guild.removeRoleFromMember(member, role).queue(null, err -> { });
                        memberRoles.remove(role.getId());
                    }
                }
            }
            if (toMinecraft()) {
                Set<String> grant = new HashSet<>();
                for (Map.Entry<String, String> entry : mapping.entrySet()) {
                    if (memberRoles.contains(entry.getValue())) grant.add(entry.getKey());
                }
                Bukkit.getScheduler().runTask(plugin, () -> applyAttachment(uuid, grant));
            }
        }, err -> { }); // Not in the guild (anymore)
    }

    /**
     * Whether the bot may change this role: Manage Roles and a higher own role. Without this check
     * JDA throws right away instead of failing in the queue callback.
     */
    private static boolean canManage(Guild guild, Role role) {
        return guild.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_ROLES)
                && guild.getSelfMember().canInteract(role);
    }

    /** Main thread: replaces the permissions Axentra grants from Discord roles. */
    private void applyAttachment(UUID uuid, Set<String> grant) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        PermissionAttachment old = attachments.remove(uuid);
        if (old != null) {
            try { player.removeAttachment(old); } catch (IllegalArgumentException ignored) { }
        }
        if (grant.isEmpty()) return;
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String name : grant) attachment.setPermission(PERMISSION_PREFIX + name, true);
        attachments.put(uuid, attachment);
    }

    private void afterUnlink(DatabaseManager.DiscordLink link) {
        Guild guild = discord.getGuild();
        if (guild != null && discord.config().getBoolean("role-sync.remove-on-unlink", true)) {
            for (String roleId : mapping().values()) {
                Role role = guild.getRoleById(roleId);
                if (role != null && canManage(guild, role)) {
                    guild.removeRoleFromMember(UserSnowflake.fromId(link.discordId), role).queue(null, err -> { });
                }
            }
        }
        Bukkit.getScheduler().runTask(plugin, () -> applyAttachment(UUID.fromString(link.playerUuid), Collections.emptySet()));
    }

    // ── Instant updates from Discord (needs the SERVER MEMBERS intent) ──

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        onRolesChanged(event.getMember());
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        onRolesChanged(event.getMember());
    }

    private void onRolesChanged(Member member) {
        if (!enabled() || !toMinecraft()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.DiscordLink link = plugin.getDatabaseManager().getLinkByDiscord(member.getId());
            if (link == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(UUID.fromString(link.playerUuid));
                if (player != null) sync(player, link);
            });
        });
    }
}
