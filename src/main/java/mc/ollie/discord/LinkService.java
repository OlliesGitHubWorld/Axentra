package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Links Minecraft accounts to Discord accounts with a 6-digit code: /link in-game gives a code,
 * /link &lt;code&gt; in Discord uses it. Also handles linking.require-link, the linked role,
 * nickname sync and the permission snapshot used to check offline staff.
 */
public class LinkService implements Listener {

    /** Permissions stored in the snapshot, so Discord actions can be checked while staff are offline. */
    public static final List<String> SNAPSHOT_PERMISSIONS = List.of(
            "Axentra.punish", "Axentra.ban", "Axentra.tempban", "Axentra.mute", "Axentra.tempmute",
            "Axentra.warn", "Axentra.kick", "Axentra.unban", "Axentra.unmute", "Axentra.appeals",
            "Axentra.reports", "Axentra.staffchat");

    private final App plugin;
    private final DiscordManager discord;
    /** Pre-login result for require-link: uuid -> link code, or "" when linked. Filled async before login. */
    private final Map<UUID, String> pendingCodes = new ConcurrentHashMap<>();
    private final List<Consumer<DatabaseManager.DiscordLink>> linkListeners = new ArrayList<>();
    private final List<Consumer<DatabaseManager.DiscordLink>> unlinkListeners = new ArrayList<>();

    public LinkService(App plugin, DiscordManager discord, SlashCommandManager commands) {
        this.plugin = plugin;
        this.discord = discord;

        commands.register(Commands.slash("link", "Link your Discord to your Minecraft account")
                        .addOption(OptionType.STRING, "code", "The 6-digit code from /link in-game", true),
                false, this::onLinkCommand);
        commands.register(Commands.slash("unlink", "Unlink your Discord from your Minecraft account"),
                false, this::onUnlinkCommand);
    }

    /** Called (async) after a successful link, e.g. for role sync. */
    public void onLinked(Consumer<DatabaseManager.DiscordLink> listener) { linkListeners.add(listener); }

    /** Called (async) after an unlink with the removed link. */
    public void onUnlinked(Consumer<DatabaseManager.DiscordLink> listener) { unlinkListeners.add(listener); }

    private DatabaseManager db() { return plugin.getDatabaseManager(); }

    private boolean oneToOne() { return discord.config().getBoolean("linking.one-to-one", true); }

    private int codeMinutes() { return Math.max(1, discord.config().getInt("linking.code-minutes", 5)); }

    // ── In-game /link and /unlink ───────────────────────────

    public void handleLinkCommand(Player player) {
        if (!discord.isConnected()) {
            player.sendMessage(discord.ingame("link-bot-offline"));
            return;
        }
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.DiscordLink existing = db().getLinkByUuid(uuid);
            if (existing != null && oneToOne()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(discord.ingame("link-already")
                        .replace("%discord%", existing.discordName != null ? existing.discordName : existing.discordId)));
                return;
            }
            String code = db().createLinkCode(uuid, name, codeMinutes());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (code == null) {
                    player.sendMessage(discord.ingame("link-bot-offline"));
                    return;
                }
                String invite = plugin.getFileManager().getSettings().getString("social-links.discord", "");
                player.sendMessage(discord.ingame("link-code").replace("%code%", code)
                        .replace("%minutes%", String.valueOf(codeMinutes()))
                        .replace("%discord%", invite.isEmpty() ? "-" : invite));
                TextComponent copy = new TextComponent(discord.ingame("link-copy-button"));
                copy.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "/link " + code));
                copy.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(discord.ingame("link-copy-hover"))));
                player.spigot().sendMessage(copy);
            });
        });
    }

    public void handleUnlinkCommand(Player player) {
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.DiscordLink> removed = db().removeLinksByUuid(uuid);
            if (removed.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(discord.ingame("unlink-not-linked")));
                return;
            }
            for (DatabaseManager.DiscordLink link : removed) afterUnlink(link, name);
            String discordName = removed.get(0).discordName != null ? removed.get(0).discordName : removed.get(0).discordId;
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(discord.ingame("unlink-success")
                    .replace("%discord%", discordName)));
        });
    }

    // ── Discord /link and /unlink ───────────────────────────

    private void onLinkCommand(SlashCommandInteractionEvent event) {
        String code = Objects.requireNonNull(event.getOption("code")).getAsString().trim();
        User user = event.getUser();
        event.deferReply(true).queue();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.DiscordLink own = db().getLinkByDiscord(user.getId());
            if (own != null && oneToOne()) {
                event.getHook().sendMessage(discord.text("link-already-linked", "player", own.playerName)).queue();
                return;
            }
            String[] target = db().takeLinkCode(code);
            if (target == null) {
                event.getHook().sendMessage(discord.text("link-invalid-code")).queue();
                return;
            }
            String uuid = target[0];
            String name = target[1];
            if (oneToOne() && db().getLinkByUuid(uuid) != null) {
                event.getHook().sendMessage(discord.text("link-player-taken", "player", name)).queue();
                return;
            }

            db().addLink(uuid, name, user.getId(), user.getName());
            db().logAction(name, uuid, "DISCORD_LINK", name, user.getName());
            event.getHook().sendMessage(discord.text("link-success", "player", name)).queue();

            DatabaseManager.DiscordLink link = db().getLinkByUuid(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(UUID.fromString(uuid));
                if (online != null) {
                    online.sendMessage(discord.ingame("link-linked").replace("%discord%", user.getName()));
                    saveSnapshot(online);
                }
            });
            if (link != null) afterLink(link);
        });
    }

    private void onUnlinkCommand(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        event.deferReply(true).queue();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.DiscordLink> removed = db().removeLinksByDiscord(user.getId());
            if (removed.isEmpty()) {
                event.getHook().sendMessage(discord.text("unlink-not-linked")).queue();
                return;
            }
            // Answer first, so a problem with roles can never leave Discord stuck on "thinking…"
            event.getHook().sendMessage(discord.text("unlink-success", "player", removed.get(0).playerName)).queue();
            for (DatabaseManager.DiscordLink link : removed) afterUnlink(link, link.playerName);
        });
    }

    /**
     * Gives or removes a role, but only when the bot is allowed to. JDA throws right away (not in
     * the queue callback) when the role is above the bot's own role or Manage Roles is missing.
     */
    private void changeRole(Guild guild, String discordId, Role role, boolean add) {
        net.dv8tion.jda.api.entities.Member self = guild.getSelfMember();
        if (!self.hasPermission(net.dv8tion.jda.api.Permission.MANAGE_ROLES) || !self.canInteract(role)) {
            plugin.getLogger().warning("Can't " + (add ? "give" : "remove") + " the role '" + role.getName()
                    + "': the bot needs Manage Roles and its own role must be above that role (Server Settings > Roles).");
            return;
        }
        try {
            (add ? guild.addRoleToMember(UserSnowflake.fromId(discordId), role)
                 : guild.removeRoleFromMember(UserSnowflake.fromId(discordId), role))
                    .queue(null, err -> plugin.logInfo("Could not change the role '" + role.getName() + "': " + err.getMessage()));
        } catch (RuntimeException e) {
            plugin.logInfo("Could not change the role '" + role.getName() + "': " + e.getMessage());
        }
    }

    // ── After link / unlink ─────────────────────────────────

    private void afterLink(DatabaseManager.DiscordLink link) {
        Guild guild = discord.getGuild();
        if (guild != null) {
            String roleId = discord.config().getString("linking.linked-role", "").trim();
            Role role = roleId.isEmpty() ? null : guild.getRoleById(roleId);
            if (role != null) changeRole(guild, link.discordId, role, true);
            try {
                syncNickname(guild, link);
            } catch (RuntimeException e) {
                plugin.logInfo("Could not change the nickname: " + e.getMessage());
            }
        }
        for (Consumer<DatabaseManager.DiscordLink> listener : linkListeners) {
            try { listener.accept(link); } catch (RuntimeException e) { plugin.logInfo("Link hook failed: " + e.getMessage()); }
        }
    }

    private void afterUnlink(DatabaseManager.DiscordLink link, String playerName) {
        db().logAction(playerName != null ? playerName : link.playerUuid, link.playerUuid, "DISCORD_UNLINK",
                playerName, link.discordName);
        Guild guild = discord.getGuild();
        if (guild != null) {
            String roleId = discord.config().getString("linking.linked-role", "").trim();
            Role role = roleId.isEmpty() ? null : guild.getRoleById(roleId);
            if (role != null) changeRole(guild, link.discordId, role, false);
        }
        for (Consumer<DatabaseManager.DiscordLink> listener : unlinkListeners) {
            try { listener.accept(link); } catch (RuntimeException e) { plugin.logInfo("Unlink hook failed: " + e.getMessage()); }
        }
    }

    /** Sets the Discord nickname to the Minecraft name (linking.sync-nickname). Silently skips members above the bot. */
    public void syncNickname(Guild guild, DatabaseManager.DiscordLink link) {
        if (!discord.config().getBoolean("linking.sync-nickname", false) || link.playerName == null) return;
        guild.retrieveMemberById(link.discordId).queue(member -> {
            if (link.playerName.equals(member.getNickname())) return;
            if (!guild.getSelfMember().canInteract(member)
                    || !guild.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.NICKNAME_MANAGE)) return;
            try {
                guild.modifyNickname(member, link.playerName).queue(null, err -> { });
            } catch (RuntimeException ignored) { }
        }, err -> { });
    }

    // ── Join: require-link and permission snapshot ──────────

    private boolean requireLink() {
        return discord.config().getBoolean("linking.require-link", false) && discord.isConnected();
    }

    /** Looks up the link before login (async), so the login check below doesn't touch the database. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED || !requireLink()) return;
        String uuid = event.getUniqueId().toString();
        if (db().getLinkByUuid(uuid) != null) {
            pendingCodes.put(event.getUniqueId(), "");
        } else {
            String code = db().createLinkCode(uuid, event.getName(), codeMinutes());
            pendingCodes.put(event.getUniqueId(), code != null ? code : "");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        String code = pendingCodes.remove(event.getPlayer().getUniqueId());
        if (code == null || code.isEmpty() || !requireLink()) return;
        if (event.getPlayer().hasPermission("Axentra.link.bypass")) return;

        String invite = plugin.getFileManager().getSettings().getString("social-links.discord", "");
        String message = plugin.getFileManager().getMessages().getString("link-required-kick", "")
                .replace("%code%", code)
                .replace("%minutes%", String.valueOf(codeMinutes()))
                .replace("%discord%", invite);
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', message));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        saveSnapshot(player);
        if (discord.isConnected() && discord.config().getBoolean("linking.join-reminder", true)
                && player.hasPermission("Axentra.link")) {
            remindIfUnlinked(player);
        }
    }

    /** Tells unlinked players how to link, a few seconds after joining so it isn't lost in join spam. */
    private void remindIfUnlinked(Player player) {
        String uuid = player.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (db().getLinkByUuid(uuid) != null) return;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                TextComponent text = new TextComponent(discord.ingame("link-join-reminder"));
                TextComponent button = new TextComponent(discord.ingame("link-join-reminder-button"));
                button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/link"));
                button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(discord.ingame("link-join-reminder-hover"))));
                text.addExtra(button);
                player.spigot().sendMessage(text);
            }, 100L);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingCodes.remove(event.getPlayer().getUniqueId());
    }

    /** Stores which staff permissions the player has, for checks while they're offline. */
    public void saveSnapshot(Player player) {
        StringBuilder perms = new StringBuilder();
        for (String perm : SNAPSHOT_PERMISSIONS) {
            if (player.hasPermission(perm)) {
                if (perms.length() > 0) perms.append(',');
                perms.append(perm);
            }
        }
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        String snapshot = perms.toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> db().updateLinkProfile(uuid, name, snapshot));
    }
}
