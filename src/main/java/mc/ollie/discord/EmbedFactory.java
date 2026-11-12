package mc.ollie.discord;

import mc.ollie.App;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.time.Instant;
import java.util.Locale;

/**
 * Shared look for every Axentra embed: colors, footer, player links and heads, all from discord.yml.
 */
public class EmbedFactory {

    /** Discord limits */
    public static final int FIELD_MAX = 1024;
    public static final int DESCRIPTION_MAX = 4096;

    private final App plugin;
    private final DiscordManager discord;

    public EmbedFactory(App plugin, DiscordManager discord) {
        this.plugin = plugin;
        this.discord = discord;
    }

    public Color color(String key) {
        String hex = discord.config().getString("embeds.colors." + key.toLowerCase(Locale.ROOT), "#00BCD4");
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException e) {
            return new Color(0x00BCD4);
        }
    }

    /** A new embed with the configured color, footer and timestamp. */
    public EmbedBuilder base(String colorKey) {
        String footer = discord.config().getString("embeds.footer", "Axentra • {server}").replace("{server}", serverName());
        return new EmbedBuilder()
                .setColor(color(colorKey))
                .setFooter(truncate(footer, 2048))
                .setTimestamp(Instant.now());
    }

    /** server-name from discord.yml, used in footers and DMs. */
    public String serverName() {
        return discord.config().getString("server-name", "Minecraft");
    }

    /** Player name as a link to their profile (embeds.profile-url), or plain bold text. */
    public String playerLink(String name, String uuid) {
        String url = discord.config().getString("embeds.profile-url", "");
        String safe = escape(name);
        if (url == null || url.isBlank()) return "**" + safe + "**";
        return "[**" + safe + "**](" + url.replace("{uuid}", uuid != null ? uuid : "").replace("{name}", name) + ")";
    }

    /** Sets the player head as thumbnail when embeds.thumbnails is on. */
    public EmbedBuilder thumbnail(EmbedBuilder embed, String name, String uuid) {
        if (discord.config().getBoolean("embeds.thumbnails", true)) embed.setThumbnail(avatarUrl(name, uuid));
        return embed;
    }

    public String avatarUrl(String name, String uuid) {
        return discord.config().getString("embeds.avatar-url", "https://mc-heads.net/avatar/{uuid}/64")
                .replace("{uuid}", uuid != null ? uuid.replace("-", "") : name)
                .replace("{name}", name != null ? name : "");
    }

    /** Discord timestamp that every viewer sees in their own timezone. Style: f = date+time, R = relative. */
    public static String timestamp(long millis, char style) {
        return "<t:" + (millis / 1000) + ":" + style + ">";
    }

    public String sourceLabel(String source) {
        if (source == null) source = "INGAME";
        return discord.text("source-" + source.toLowerCase(Locale.ROOT));
    }

    /** Escapes Discord markdown in names and reasons so they render as typed. */
    public static String escape(String text) {
        if (text == null) return "";
        return text.replaceAll("([*_~`|>\\\\])", "\\\\$1");
    }

    /** Breaks @everyone/@here so they can never ping, even if mentions are allowed somewhere. */
    public static String defuse(String text) {
        if (text == null) return "";
        return text.replace("@everyone", "@​everyone").replace("@here", "@​here")
                .replace("<@", "<​@");
    }

    public static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }

    /** Formats milliseconds as "2d 3h", "45m", "30s". */
    public static String duration(long millis) {
        long s = Math.max(0, millis / 1000);
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0 && d == 0) sb.append(m).append("m ");
        if (sb.length() == 0) sb.append(sec).append("s");
        return sb.toString().trim();
    }
}
