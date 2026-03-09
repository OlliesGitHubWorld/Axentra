<div align="center">

# ❓ Frequently Asked Questions

</div>

---

## Installation

**Q: I get two jar files after building, which one do I use?**
> Use `axentra-x.x.x.jar`. The `original-axentra-x.x.x.jar` is a backup created by the build process and does not include SQLite.

**Q: The plugin is not loading, what do I do?**
> Make sure you are running Spigot or Paper on Minecraft 1.21 or higher with Java 21 or higher. Check your console for any error messages.

---

## Configuration

**Q: How do I reload the config without restarting?**
> Run `/axentra reload` and both `config.yml` and `messages.yml` will be reloaded instantly.

**Q: How do I hide the join or leave message entirely?**
> Set the message to `""` (empty) in `messages.yml`.

**Q: How do I use the default Minecraft join or leave message?**
> Set the message to `none` in `messages.yml`.

**Q: How do I add color to messages?**
> Use `&` color codes. For example `&a` is green and `&c` is red. See the full list on the [Home](Home#-color-codes) page.

**Q: How do I set a ban appeal link?**
> Set `ban-appeal-link` in `config.yml` to your Discord or website link. Set it to `""` to hide it.

**Q: How do I set up automatic punishments for warnings?**
> Edit the `warn-punishments` section in `config.yml`. Each entry is a warn count mapped to an action (`kick`, `mute`, `tempban`, or `ban`). See the [Installation](Installation#warn-punishments) page for a full example.

---

## Commands

**Q: Can I ban offline players?**
> Yes! You can ban players even if they are not online. They will be blocked from joining when they try to connect.

**Q: I accidentally unbanned the wrong player, what do I do?**
> Just run `/ban <player>` again to re-ban them.

**Q: Can I warn offline players?**
> No, `/warn` requires the player to be online so their UUID can be recorded correctly.

**Q: How do I view a player's warnings?**
> Use `/warns <player>` to see all their active warnings with the reason, issuer and date.

**Q: A muted player left and rejoined, are they still muted?**
> Yes, mutes are stored in the database and persist across reconnects and server restarts.

**Q: The `/axentra upgrade` command says an update is available but doesn't update automatically.**
> The upgrade command only checks for updates, it does not download them automatically. Download the latest version from the [Releases](https://github.com/OlliesGitHubWorld/Axentra/releases/latest) page manually.

---

## Errors

**Q: I get `NullPointerException` on startup.**
> This usually means a message is missing from your `messages.yml`. Try deleting the file and restarting the server to regenerate the defaults.

**Q: The database is not connecting.**
> Make sure the `plugins/Axentra/` folder exists and the server has write permissions to it.

**Q: My question is not listed here!**
> Open a [Support Issue](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=question---support.md.md) and we will help you out!