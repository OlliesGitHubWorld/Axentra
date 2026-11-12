<div align="center">

# Axentra

**All-in-one server management for Spigot & Paper — moderation, staff tools, player protection, server health and Discord in one lightweight plugin.**

![Version](https://img.shields.io/github/v/release/OlliesGitHubWorld/Axentra.svg?style=flat-square&label=version&color=blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21%2B-green.svg?style=flat-square) ![Java](https://img.shields.io/badge/java-21-orange.svg?style=flat-square) ![License](https://img.shields.io/badge/license-MIT-purple.svg?style=flat-square)

[Website](https://axentramc.com) · [Download](https://github.com/OlliesGitHubWorld/Axentra/releases/latest) · [Wiki](https://axentramc.com/wiki.html) · [Changelog](https://axentramc.com/changelog.html) · [Report a Bug](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=bug_report.md) · [Request a Feature](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=feature_request.md)

</div>

---

## Why Axentra?

- **One plugin instead of ten** — punishments, staff mode, vanish, homes, warps, RTP, chat tools, server monitoring and a Discord bot
- **115+ commands, 36 modules** — turn off anything you don't need and its commands disappear completely
- **No plugin dependencies** — drop in the jar and go; data is stored in a local SQLite database, and the Discord library is downloaded automatically on first start
- **Built for performance** — async database access, cached mute and freeze checks, and a WAL-mode database
- **Fully configurable** — every message, GUI template and threshold lives in plain YAML, and configs update themselves

---

## See It in Action

### Staff Mode
Vanish, teleport to players, freeze them, check their inventory and history, and punish — all from one hotbar.

<img src="docs/assets/demo-staffmode.webp" alt="Staff mode: random teleport, freeze, invsee, history GUI and punish GUI" width="720">

### Warps
Reorder warps with shift-click, change icons with a hotbar key, and teleport — without leaving the menu.

<img src="docs/assets/demo-warps.gif" alt="Warps GUI: moving a warp to position 1, changing its icon and teleporting" width="720">

### Lag Tools
Find the heaviest chunks with `/lag chunks`, then check how the server performed over time in `/lag history`.

<img src="docs/assets/demo-lag.gif" alt="/lag chunks with hover details, followed by the /lag history GUI" width="720">

---

## Screenshots

| ![Lag overview](docs/assets/lag-overview.png) | ![Punishment GUI](docs/assets/punishment-gui.png) |
|:---:|:---:|
| ![Warps GUI](docs/assets/warps-gui.png) | ![Smart join alert](docs/assets/join-alert.png) |
| ![Maintenance mode](docs/assets/maintenance.png) | ![Restart countdown](docs/assets/restart-countdown.png) |

<details>
<summary><b>More screenshots</b></summary>

| ![Staff tools](docs/assets/staff-tools.png) | ![Inventory snapshots](docs/assets/inventory-snapshots.png) |
|:---:|:---:|
| ![Settings GUI](docs/assets/settings-gui.png) | ![Ban screen](docs/assets/ban-screen.png) |

</details>

---

## What's New in v1.9.0 — Discord Integration Update

- **Discord bot** — cases, reports (with a thread each), staff chat and public chat (both ways), alerts, join/leave and an optional console channel, plus `/case`, `/history`, `/lookup`, `/online` and `/health` slash commands
- **Account linking & role sync** — `/link` with a 6-digit code, optional required linking, nickname sync and Discord roles from permissions
- **Ban appeals** — players appeal with `/appeal <case>` in Discord; staff accept, deny or reduce from a full dossier, and the player gets a DM
- **Moderation from Discord** — punish with your punishment templates right under a report or `/lookup`
- **Weekly report** — players, playtime, cases, top staff, appeals and performance, compared with last week
- **Player info** — `/afk` (AFK time no longer counts as playtime), `/seen`, `/whois`, `/near` and a grouped `/list`

The bot is off by default — Axentra works exactly as before until you set it up. Read the **[full changelog](https://axentramc.com/changelog.html)** for every change.

---

## Features

| Category | Highlights |
|---|---|
| **Moderation** | Punishment GUI with templates and escalation, case system, history GUI, staff notes, audit log, chat log snapshots |
| **Punishments** | Kick, ban, tempban, IP ban, mute, tempmute, warn, freeze — with automatic punishments at warn thresholds |
| **Player Protection** | Inventory snapshots, trust levels, alt and VPN detection, freeze and tempfreeze, player timeline |
| **Staff Tools** | Staff mode with tool hotbar, vanish, duty tracking, reports GUI, staff statistics, smart join alerts |
| **Server Health** | `/lag` dashboard, lag chunks, health score, TPS history, lag alerts, auto cleanup, entity limits |
| **Server Control** | Scheduled restarts, maintenance mode with custom MOTD and whitelist |
| **Teleportation** | Homes, warps, random teleport, spawn, `/back`, TPA requests, staff teleports |
| **Chat** | Private messages, social spy, staff chat, broadcast, mute / slow / clear chat, nicknames |
| **Discord** | Account linking, role sync, case/report/alert channels, two-way staff chat, slash commands, ban appeals, punishing from Discord, weekly report |
| **Player Info** | AFK with anti-AFK-machine checks, `/seen`, `/whois`, `/near`, grouped `/list` |
| **Utility** | Fly, heal, feed, repair, god, speed, virtual workstations, weather and time control |

See every command and permission on the **[Wiki](https://axentramc.com/wiki.html#commands)**.

---

## Getting Started

### Requirements

- **Spigot** or **Paper** 1.21+ (Paper recommended for the most accurate TPS/MSPT readings)
- **Java 21** or newer

### Installation

1. Download the latest `axentra-x.x.x.jar` from [Releases](https://github.com/OlliesGitHubWorld/Axentra/releases/latest)
2. Put it in your server's `plugins/` folder
3. Restart the server
4. Adjust the config files in `plugins/Axentra/` to your liking
5. Run `/axentra reload` to apply changes

> Turning modules on or off requires a full server restart, not just a reload.

### Quick Start

A few commands to try right after installing:

| Command | What it does |
|---|---|
| `/punish <player>` | Open the punishment GUI for a player |
| `/lag` | See TPS, MSPT, memory and the health score |
| `/lag chunks` | Find the chunks causing the most lag |
| `/setwarp spawn` | Create a warp, then open `/warps` to manage it |
| `/restart schedule 10m` | Restart in 10 minutes with a bossbar countdown |
| `/maintenance on 30m Updating plugins` | Lock the server for 30 minutes |
| `/staffmode` | Toggle staff mode with the tool hotbar |
| `/axentra settings` | Change plugin settings in-game |

### Configuration Files

| File | Contents |
|---|---|
| `config.yml` | General settings, module toggles, homes, warps, RTP |
| `messages.yml` | Every message the plugin sends |
| `punishments.yml` | Punishment GUI templates and escalation ladders |
| `protection.yml` | Snapshots, trust, alt and VPN detection, freeze |
| `health.yml` | TPS monitoring, lag chunks, alerts, cleanup, entity limits, restarts, maintenance |
| `discord.yml` | Bot token, channels, linking, role sync, staff chat, alerts, appeals, weekly report, embed texts |

When you update Axentra, new settings are added to your existing files automatically. Your own values are kept.

### Permissions

All permissions start with `Axentra.` Staff features are granted to ops by default, and everyday player features such as homes, warps and `/rtp` are available to everyone. The full list is on the [Wiki](https://axentramc.com/wiki.html).

<details>
<summary><b>New in v1.9.0: commands and permissions</b></summary>

| Command | What it does | Permission |
|---|---|---|
| `/link`, `/unlink` | Link or unlink your Discord account | `Axentra.link` |
| `/appeals [open\|all]` | Handle appeals in a GUI | `Axentra.appeals` |
| `/afk [reason]` | Toggle AFK | `Axentra.afk` |
| `/seen <player>` | When a player was last online | `Axentra.seen` |
| `/whois <player>` | Everything about a player | `Axentra.whois` |
| `/near [radius]` | Players near you | `Axentra.near` |
| `/list` | Online players by group (aliases `/online`, `/who`) | `Axentra.list` |
| `/axentra discord <status\|report>` | Bot status, or send the weekly report now | `Axentra.admin` |

| Permission | Description | Default |
|---|---|---|
| `Axentra.link` | Link and unlink Discord | everyone |
| `Axentra.link.bypass` | Join without a link when linking is required | op |
| `Axentra.appeals` | View and handle appeals | op |
| `Axentra.discord.role.<name>` | Gets the Discord role mapped to `<name>` in `discord.yml` | — |
| `Axentra.afk` | Use `/afk` | everyone |
| `Axentra.afk.kickbypass` | Never kicked for being AFK | op |
| `Axentra.afk.exempt` | Never set AFK automatically (`/afk` still works) | op |
| `Axentra.seen` | Use `/seen` | everyone |
| `Axentra.seen.staff` | Last location and IP country in `/seen` | op |
| `Axentra.whois` | Use `/whois` | op |
| `Axentra.whois.sensitive` | IP, alts and VPN status in `/whois` | op |
| `Axentra.near` | Use `/near` | op |
| `Axentra.near.radius.<n>` | Use `/near` up to `<n>` blocks | — |
| `Axentra.list` | Use `/list` | everyone |
| `Axentra.list.group.<name>` | Puts the player in that `/list` group | — |

Discord slash commands: `/link`, `/unlink`, `/online` and `/appeal` for everyone; `/case`, `/history`, `/lookup` and `/health` for members with a role from `staff-roles`.

</details>

---

## Discord Setup

The bot is optional and off by default. To turn it on:

1. **Create a bot.** Go to the [Discord Developer Portal](https://discord.com/developers/applications), create an application, open **Bot** and click **Reset Token**. Copy the token into `token` in `plugins/Axentra/discord.yml`. Never share it.
2. **Turn on the intents.** On the same **Bot** page, enable **Server Members Intent** (instant role updates from Discord) and **Message Content Intent** (staff chat from Discord to Minecraft). Without them Axentra still runs; those two features are just limited.
3. **Invite the bot.** Open **OAuth2 → URL Generator**, tick the scopes `bot` and `applications.commands`, and give it: View Channels, Send Messages, Embed Links, Attach Files, Read Message History, Manage Messages, Create Public Threads, Manage Webhooks, Manage Roles and Manage Nicknames. Open the generated URL and add the bot to your server.
4. **Copy the IDs.** In Discord, enable **Developer Mode** (User Settings → Advanced). Right-click your server → **Copy Server ID** for `guild-id`, and right-click each channel → **Copy Channel ID** for the `channels` section.
5. **Pick staff roles.** Add the role IDs that may use staff commands and buttons to `staff-roles`.
6. **Start it.** Set `enabled: true`, run `/axentra reload` and check `/axentra discord status`. It shows the connection, which channels work and which intents are missing.

> Staff act from Discord with their **linked** Minecraft account. Buttons and menus check the in-game permission they had on their last join, so they work while staff are offline.

### For staff: staff chat between Discord and Minecraft

Everything typed in-game with `/staffchat` shows up in the Discord staff chat channel, and staff can answer from Discord. A Discord message is shown in-game to everyone with `Axentra.staffchat` when:

1. the bot has the **Message Content Intent** (Developer Portal → Bot),
2. your Discord account is **linked** (see below), and
3. you have a role from `staff-roles` in `discord.yml`.

If you aren't linked yet, the bot tells you so in the channel. Set `staffchat.confirm-reaction: true` to also get a ✅ under messages that reached Minecraft.

### Optional channels: public chat and console

Both are off by default. Add a channel ID under `channels` in `discord.yml` and set `enabled: true`.

- **`chat`** — Minecraft chat and a Discord channel talk to each other, with each player's name and head. Muted players and staff chat are never sent, mentions can't ping, and masked links are blocked. Muted or banned linked members can't talk from Discord. Use `chat.require-link: true` to only allow linked members.
- **`console`** — the server console, sent in batches with IP addresses hidden. **Only give admins access to this channel.** Running commands from it is off by default; turn on `console.allow-commands` and add the allowed role IDs to `console.roles`. Commands like `stop`, `op` and `lp` are always blocked, and every command is written to the audit log.

### For players: linking your Discord account

Linking connects your Minecraft account to your Discord account. You need it to appeal a ban in Discord, and your server may give you roles for it.

1. **In Minecraft**, type `/link` in the chat. You get a 6-digit code, for example `482913`. It works for 5 minutes.
2. **In the server's Discord**, type `/link` in any channel and pick the command from the server's bot in the list that pops up.
3. **Enter your code** in the `code` field and press Enter. The bot answers *"Linked to YourName"*, and you also get a message in Minecraft.

Type `/discord` in Minecraft to check whether you're linked. To unlink, use `/unlink` in Minecraft or in Discord.

**Something not working?**
- *"That code is invalid or expired"* — the code is older than 5 minutes or was already used. Type `/link` in Minecraft again for a new one.
- *`/link` doesn't show up in Discord* — the bot was invited without the `applications.commands` scope. Ask an admin to re-invite it (see step 3 above).
- *"The Discord bot is offline"* — the server's bot isn't running. Tell an admin; they can check `/axentra discord status`.

### For players: appealing a ban or mute

Your ban screen shows a case number, for example *"Appeal in our Discord with /appeal 42"*.

1. Link your account first (see above) — you can't join while banned, so link before you need it, or ask staff if your server lets you appeal without linking.
2. In the server's Discord, type `/appeal`. If your account is linked you don't need the case number: you pick from your active punishments. Otherwise, add the number, e.g. `/appeal 42`.
3. Fill in the form and send it. Staff review it, and the bot sends you a DM with the decision.

---

## FAQ

<details>
<summary><b><code>/restart</code> restarts the server immediately and ignores my arguments</b></summary>

Spigot and Paper ship their own `/restart`, which restarts right away. Before v1.8.0 it took priority over Axentra's command — update to the latest version. Spigot's built-in restart stays available as `/spigot:restart`, also when the **restarts** module is turned off.

</details>

<details>
<summary><b>The Discord bot doesn't start</b></summary>

Run `/axentra discord status`. It shows the last error, for example an invalid token, a missing `guild-id`, or a bot that isn't in your server. On the very first start the server downloads the Discord library from Maven Central, so it needs internet access once.

</details>

<details>
<summary><b>I turned a module on or off but nothing changed</b></summary>

Module toggles in `config.yml` are only applied at startup. Fully restart the server; `/axentra reload` is not enough.

</details>

<details>
<summary><b>The <code>[Frozen]</code> tag doesn't show above a player's head</b></summary>

The nametag uses the main scoreboard. Plugins that manage nametags themselves, such as TAB, can override it. The tag in the tab list and in chat still works, and each spot can be toggled in `protection.yml`.

</details>

<details>
<summary><b>What are <code>data.db-wal</code> and <code>data.db-shm</code>?</b></summary>

They belong to the database, which runs in WAL mode to avoid "database is locked" errors. This is normal — don't delete them while the server is running.

</details>

<details>
<summary><b>Staff can see players' IP addresses</b></summary>

IP addresses are only shown to players with `Axentra.alts.ip`. If a group has `Axentra.*`, set `Axentra.alts.ip` to `false` explicitly for staff that shouldn't see them.

</details>

<details>
<summary><b>My config is missing settings after an update</b></summary>

New settings are added to your files automatically on startup, with their comments. If something still looks off, rename the file and restart to generate a fresh copy, then copy your values back.

</details>

---

## Building from Source

```bash
git clone https://github.com/OlliesGitHubWorld/Axentra.git
cd Axentra
./mvnw clean package
```

The jar is written to `target/`. On Windows, use `mvnw.cmd` instead of `./mvnw`.

---

## Support & Contributing

- **Found a bug?** [Open a bug report](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=bug_report.md)
- **Have an idea?** [Request a feature](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=feature_request.md)
- **Want to help?** Pull requests are welcome — [open one here](https://github.com/OlliesGitHubWorld/Axentra/pulls)

---

## License

Axentra is released under the [MIT License](LICENSE).

<div align="center">

Made with ❤️ by [OlliesWorld](https://github.com/OlliesGitHubWorld)

</div>
