package mc.ollie.discord;

import mc.ollie.App;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Collects the slash commands of all Discord features and registers them as guild commands in one
 * call when the bot connects (Discord replaces the whole list on every update).
 *
 * <p>Staff commands are visible to everyone in the command list, because bots can't hide commands
 * per role; they are checked against staff-roles when used. Server admins can hide them in
 * Server Settings > Integrations.
 */
public class SlashCommandManager extends ListenerAdapter {

    private record Entry(SlashCommandData data, boolean staffOnly,
                         Consumer<SlashCommandInteractionEvent> handler,
                         Function<CommandAutoCompleteInteractionEvent, List<Command.Choice>> completer) { }

    private final App plugin;
    private final DiscordManager discord;
    private final Map<String, Entry> commands = new LinkedHashMap<>();

    public SlashCommandManager(App plugin, DiscordManager discord) {
        this.plugin = plugin;
        this.discord = discord;
        discord.addListener(this);
        discord.onReady(this::registerAll);
    }

    public void register(SlashCommandData data, boolean staffOnly, Consumer<SlashCommandInteractionEvent> handler) {
        register(data, staffOnly, handler, null);
    }

    public void register(SlashCommandData data, boolean staffOnly, Consumer<SlashCommandInteractionEvent> handler,
                         Function<CommandAutoCompleteInteractionEvent, List<Command.Choice>> completer) {
        commands.put(data.getName(), new Entry(data, staffOnly, handler, completer));
    }

    /** Registers everything as guild commands; called on connect and after features add commands. */
    public void registerAll(JDA jda) {
        Guild guild = discord.getGuild();
        if (guild == null) return;
        List<SlashCommandData> data = new ArrayList<>();
        for (Entry entry : commands.values()) data.add(entry.data());
        guild.updateCommands().addCommands(data).queue(
                ok -> plugin.logInfo("Registered " + data.size() + " Discord slash commands."),
                err -> plugin.getLogger().warning("Could not register Discord slash commands: " + err.getMessage()
                        + ". Re-invite the bot with the applications.commands scope."));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || !event.getGuild().getId().equals(discord.getGuild() != null ? discord.getGuild().getId() : "")) return;
        Entry entry = commands.get(event.getName());
        if (entry == null) return;

        if (entry.staffOnly() && !discord.isStaff(event.getMember())) {
            event.reply(discord.text("command-no-permission")).setEphemeral(true).queue();
            return;
        }
        try {
            entry.handler().accept(event);
        } catch (Exception e) {
            plugin.getLogger().warning("Discord command /" + event.getName() + " failed: " + e.getMessage());
            if (!event.isAcknowledged()) {
                event.reply(discord.text("command-not-ready")).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        Entry entry = commands.get(event.getName());
        if (entry == null || entry.completer() == null) return;
        if (entry.staffOnly() && !discord.isStaff(event.getMember())) {
            event.replyChoices(Collections.emptyList()).queue();
            return;
        }
        try {
            List<Command.Choice> choices = entry.completer().apply(event);
            event.replyChoices(choices.size() > 25 ? choices.subList(0, 25) : choices).queue();
        } catch (Exception e) {
            event.replyChoices(Collections.emptyList()).queue();
        }
    }

    /** Autocomplete for player names, from the cached list of known players (no DB hit per keystroke). */
    public List<Command.Choice> playerNames(CommandAutoCompleteInteractionEvent event) {
        String typed = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
        List<Command.Choice> choices = new ArrayList<>();
        for (String name : plugin.getDatabaseManager().getKnownPlayerNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(typed)) {
                choices.add(new Command.Choice(name, name));
                if (choices.size() >= 25) break;
            }
        }
        return choices;
    }

    /** Whether replies to staff commands should be ephemeral (discord.yml ephemeral-replies). */
    public boolean ephemeral() {
        return discord.config().getBoolean("ephemeral-replies", true);
    }
}
