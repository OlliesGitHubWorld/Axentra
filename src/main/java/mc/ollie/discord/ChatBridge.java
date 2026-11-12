package mc.ollie.discord;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Public chat between Minecraft and a Discord channel (channels.chat).
 *
 * <p>Minecraft to Discord: only chat that actually went through, so muted players, staff chat
 * toggles and cancelled messages never leak. Mentions can never ping, and markdown and masked
 * links are escaped unless chat.allow-markdown is on.
 *
 * <p>Discord to Minecraft: needs the MESSAGE CONTENT intent. A linked member who is muted or
 * banned in Minecraft isn't relayed. With chat.require-link only linked members are relayed.
 */
public class ChatBridge extends ListenerAdapter implements Listener {

    private final App plugin;
    private final DiscordManager discord;
    private final ChannelRelay relay;

    public ChatBridge(App plugin, DiscordManager discord, ChannelRelay relay) {
        this.plugin = plugin;
        this.discord = discord;
        this.relay = relay;
        discord.addListener(this);
    }

    // ── Minecraft → Discord ─────────────────────────────────

    /** MONITOR + ignoreCancelled: only messages that really reached the chat (not muted, not staff chat). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!discord.canPost("chat")) return;
        Player player = event.getPlayer();
        if (discord.config().getBoolean("chat.skip-vanished", true) && plugin.isVanished(player)) return;

        String text = ChatColor.stripColor(event.getMessage());
        if (!discord.config().getBoolean("chat.allow-markdown", false)) {
            // Also escape [ ] so nobody can post masked links like [free nitro](http://...)
            text = EmbedFactory.escape(text).replace("[", "\\[").replace("]", "\\]");
        }
        text = EmbedFactory.defuse(text);
        if (text.isBlank()) return;

        String name = player.getName();
        String uuid = player.getUniqueId().toString();
        if (discord.usesWebhook("chat")) {
            discord.webhooks().send("chat", WebhookSender.Message.as(name, relay.avatarUrl(name, uuid), text));
            return;
        }
        TextChannel channel = discord.getChannel("chat");
        if (channel == null) return;
        if (discord.config().getBoolean("chat.use-webhook", true)) {
            relay.sendViaWebhook(channel, "Axentra Chat", name, uuid, text);
        } else {
            String line = discord.config().getString("chat.to-discord-format", "**{player}**: {message}")
                    .replace("{player}", EmbedFactory.escape(name)).replace("{message}", text);
            channel.sendMessage(EmbedFactory.truncate(line, 2000)).queue(null, err -> { });
        }
    }

    // ── Discord → Minecraft ─────────────────────────────────

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()) return;
        TextChannel channel = discord.getChannel("chat");
        if (channel == null || !event.getChannel().getId().equals(channel.getId())) return;
        if (!discord.hasIntent(GatewayIntent.MESSAGE_CONTENT)) return;

        Member member = event.getMember();
        Message message = event.getMessage();
        List<Message.Attachment> attachments = message.getAttachments();
        String content = message.getContentDisplay();
        if (content.isBlank() && attachments.isEmpty()) return;
        String text = EmbedFactory.truncate(content.replace('\n', ' '), 256);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager.DiscordLink link = plugin.getDatabaseManager().getLinkByDiscord(event.getAuthor().getId());
            if (link == null && discord.config().getBoolean("chat.require-link", false)) {
                hint(message, discord.text("chat-not-linked"));
                return;
            }
            if (link != null && (plugin.getDatabaseManager().isMuted(link.playerUuid)
                    || plugin.getDatabaseManager().isBanned(link.playerUuid))) {
                hint(message, discord.text("chat-muted"));
                return;
            }
            String discordName = member != null ? member.getEffectiveName() : event.getAuthor().getName();
            String name = link != null && link.playerName != null ? link.playerName : discordName;
            ChannelRelay.IngameLine line = relay.render(
                    discord.config().getString("chat.to-minecraft-format", "&9[Discord] &f{name}&7: &f{message}"),
                    ChatColor.stripColor(name), ChatColor.stripColor(discordName), text, attachments);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) online.spigot().sendMessage(line.components());
                Bukkit.getConsoleSender().sendMessage(line.console());
            });
        });
    }

    /** A short reply that removes itself, so the channel stays clean. */
    private void hint(Message message, String text) {
        message.reply(text).queue(reply -> reply.delete().queueAfter(15, TimeUnit.SECONDS, null, err -> { }), err -> { });
    }
}
