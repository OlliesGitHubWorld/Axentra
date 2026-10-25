<div align="center">

![Version](https://img.shields.io/badge/version-1.7.0-blue?style=flat-square)
![Minecraft](https://img.shields.io/badge/minecraft-1.21-green?style=flat-square)
![Java](https://img.shields.io/badge/java-21-orange?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-purple?style=flat-square)
![Status](https://img.shields.io/badge/status-stable-brightgreen?style=flat-square)

# Axentra

**A powerful, lightweight Minecraft server management plugin.**

**[Website](https://axentramc.com)** | **[Download](https://github.com/OlliesGitHubWorld/Axentra/releases/latest)** | **[Wiki](https://axentramc.com/wiki.html)** | **[Report Bug](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=bug_report.md)** | **[Request Feature](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=feature_request.md)**

</div>

---

## What's New in v1.7.0 — Player Protection Update

- **Inventory Snapshots** — Automatic snapshots on death, join, quit, world change & clear. Browse and restore via `/restoreinv` GUI
- **Trust System** — Three-tier player trust (NEW → MEMBER → TRUSTED) with automatic progression and manual override
- **Alt Detection** — Detect alt accounts via shared IP addresses with risk scoring and clickable alerts
- **VPN Detection** — Automatic VPN/proxy detection via proxycheck.io with configurable actions
- **Freeze System** — Database-persisted `/freeze` that survives restarts, blocks all actions, configurable logout punishments
- **Player Timeline** — `/timeline` shows a player's full activity history (joins, quits, punishments, snapshots)
- **Home System** — `/sethome`, `/home`, `/delhome`, `/homes` with paginated GUI, permission-based limits, teleport delay
- **Staff Teleport** — `/tp`, `/tphere`, `/tpall`, `/tpahere`, `/top` commands
- **Smart Join Alerts** — Staff see alt accounts, VPN status, trust level, and active cases on player join
- **Case Management** — `/case resolve <id>` and `/case delete <id>` with clickable actions
- **Auto-resolve** — Kicks and warns are now automatically resolved instead of staying active
- **Config Auto-updater** — Template-based YML merge adds new keys with comments in the correct position

---

## About

Axentra is a Minecraft Bukkit/Spigot plugin that provides server management, moderation, player protection, staff tools, chat control, private messaging, teleportation, homes, vanish, and utility commands — all fully configurable through YAML files with a built-in SQLite database.

---

## Features

- **Player Protection** — Inventory snapshots, trust system, alt detection, VPN detection, freeze system, player timeline
- **Staff Management** — Staffmode with tool hotbar, duty tracking, player reports with GUI, staff statistics and leaderboard
- **Smart Moderation** — Punishment GUI, case system with resolve/delete, history GUI, staff notes, audit log, chat log snapshots
- **Moderation Suite** — Kick, ban, tempban, IP ban, mute, tempmute, warn with auto-punishments at thresholds
- **Chat Control** — Broadcast, clear chat, mute chat, slow chat, staff-only chat
- **Private Messaging** — `/msg`, `/reply`, `/msgtoggle` with social spy for staff
- **Nicknames** — Display names with color codes, persistent in database
- **Social Links** — Configurable `/discord`, `/website`, `/store` commands
- **Staff Settings GUI** — In-game clickable menu to toggle server settings in real-time
- **Utility Commands** — Clear inventory, repair items, fly, heal, feed, hat, ping, god mode, speed control
- **Virtual Workstations** — Anvil, workbench, cartography, grindstone, loom, smithing table, stonecutter, ender chest
- **World Control** — Weather and time commands with shortcut aliases
- **Home System** — Set, teleport, delete homes with paginated GUI, permission-based limits, world blacklists
- **Teleportation** — Spawn with countdown, TPA request system, back on death/teleport, staff tp commands
- **Vanish** — Ghost effect, night vision, god mode, no pickup, fake join/quit messages
- **Gamemode & Speed** — Quick gamemode switching (gms/gmc/gma/gmsp) and walk/fly speed control
- **Inventory Inspection** — View other players' inventories and ender chests
- **Announcements** — Permission-based announce toggles for kicks, bans, mutes, warns
- **Modular System** — 21 toggleable modules in config.yml
- **SQLite Database** — Persistent storage for all data
- **Fully Configurable** — Every message and setting is editable in YAML
- **Config Auto-update** — New config keys added automatically with comments on plugin update
- **Update Notifications** — Automatically notifies admins of new updates on join

---

## Installation

1. Download the latest `axentra-x.x.x.jar` from the [Releases](https://github.com/OlliesGitHubWorld/Axentra/releases/latest) page
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Edit `plugins/Axentra/config.yml` and `plugins/Axentra/messages.yml`
5. Run `/axentra reload` to apply your changes!

For a more detailed guide, check out the [Wiki](https://axentramc.com/wiki.html).

---

## Commands

### Protection & Tracking

| Command | Description | Permission |
|---|---|---|
| `/restoreinv <player>` | View and restore inventory snapshots | `Axentra.restoreinv` |
| `/trust <player>` | View a player's trust level | `Axentra.trust` |
| `/trust set <player> <level>` | Set a player's trust level | `Axentra.trust.set` |
| `/alts <player>` | View a player's alt accounts | `Axentra.alts` |
| `/freeze <player>` | Freeze a player | `Axentra.freeze` |
| `/unfreeze <player>` | Unfreeze a player | `Axentra.freeze` |
| `/timeline <player> [filter]` | View a player's activity timeline | `Axentra.timeline` |

### Homes & Teleport

| Command | Description | Permission |
|---|---|---|
| `/sethome [name]` | Set a home location | `Axentra.sethome` |
| `/home [name]` | Teleport to a home | `Axentra.home` |
| `/delhome <name>` | Delete a home | `Axentra.delhome` |
| `/homes [player]` | View all homes in a GUI | `Axentra.homes` |
| `/tp <player> [player]` | Teleport to a player | `Axentra.tp` |
| `/tphere <player>` | Teleport a player to you | `Axentra.tphere` |
| `/tpall` | Teleport all players to you | `Axentra.tpall` |
| `/tpahere <player>` | Request a player to teleport to you | `Axentra.tpahere` |
| `/top` | Teleport to the highest block | `Axentra.top` |

### Staff Management

| Command | Description | Permission |
|---|---|---|
| `/duty` | Toggle on/off duty | `Axentra.duty` |
| `/staffmode` | Toggle staffmode with tool hotbar | `Axentra.staffmode` |
| `/report <player> <reason>` | Report a player | `Axentra.report` |
| `/reports` | Open the reports GUI | `Axentra.reports` |
| `/staffstats [player]` | View staff statistics | `Axentra.staffstats` |
| `/stafflist` | View online staff with duty status | `Axentra.stafflist` |

### Smart Moderation

| Command | Description | Permission |
|---|---|---|
| `/punish <player>` | Opens the punishment GUI with templates | `Axentra.punish` |
| `/case <id>` | View details of a moderation case | `Axentra.case` |
| `/case resolve <id>` | Mark a case as resolved | `Axentra.case` |
| `/case delete <id>` | Permanently delete a case | `Axentra.case.delete` |
| `/history <player>` | View a player's full moderation history | `Axentra.history` |
| `/note add <player> <text>` | Add a staff note on a player | `Axentra.note` |
| `/note remove <id>` | Remove a staff note | `Axentra.note` |
| `/notes <player>` | View all staff notes on a player | `Axentra.note` |
| `/auditlog <staff> [page]` | View a staff member's action log | `Axentra.auditlog` |

### Chat Control

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/broadcast <message>` | `/bc` | Broadcast a message to all players | `Axentra.broadcast` |
| `/clearchat` | `/cc` | Clear the chat for all players | `Axentra.clearchat` |
| `/mutechat` | `/mc` | Toggle global chat mute | `Axentra.mutechat` |
| `/slowchat [seconds\|off]` | `/slowmode` | Toggle slow chat mode | `Axentra.slowchat` |
| `/staffchat [message]` | `/sc` | Send or toggle staff chat | `Axentra.staffchat` |

### Messaging

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/msg <player> <message>` | `/tell`, `/whisper`, `/w`, `/pm` | Send a private message | `Axentra.msg` |
| `/reply <message>` | `/r` | Reply to the last private message | `Axentra.msg` |
| `/msgtoggle` | — | Toggle receiving private messages | `Axentra.msg` |

### Formatting

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/nick <name\|off> [player]` | `/nickname` | Set your or another player's nickname | `Axentra.nick` |
| `/realname <nickname>` | — | Find the real name of a nicknamed player | `Axentra.realname` |
| `/me <action>` | — | Send an action message | `Axentra.me` |
| `/discord` | — | Show the server's Discord link | — |
| `/website` | — | Show the server's website | — |
| `/store` | — | Show the server's store link | — |

### Moderation

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/kick <player> [reason]` | — | Kicks a player | `Axentra.kick` |
| `/ban <player> [reason]` | — | Permanently bans a player | `Axentra.ban` |
| `/tempban <player> <duration> [reason]` | — | Temporarily bans a player | `Axentra.tempban` |
| `/unban <player>` | — | Unbans a player | `Axentra.unban` |
| `/banip <player\|ip> [reason]` | — | Bans an IP address | `Axentra.banip` |
| `/tempbanip <player\|ip> <duration> [reason]` | — | Temporarily bans an IP | `Axentra.tempbanip` |
| `/unbanip <ip>` | — | Unbans an IP address | `Axentra.unbanip` |
| `/warn <player> [reason]` | — | Warns a player | `Axentra.warn` |
| `/warns [player]` | — | View a player's warnings | `Axentra.warns` |
| `/clearwarns <player>` | — | Clears a player's warnings | `Axentra.clearwarns` |
| `/mute <player> [reason]` | — | Permanently mutes a player | `Axentra.mute` |
| `/tempmute <player> <duration> [reason]` | — | Temporarily mutes a player | `Axentra.tempmute` |
| `/unmute <player>` | — | Unmutes a player | `Axentra.unmute` |

### Utility

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/clear [player]` | `/c` | Clears a player's inventory | `Axentra.clear` |
| `/repair` | — | Repairs the item in your hand | `Axentra.repair` |
| `/fly [player]` | — | Toggles flight mode | `Axentra.fly` |
| `/heal [player]` | — | Heals a player | `Axentra.heal` |
| `/feed [player]` | — | Feeds a player | `Axentra.feed` |
| `/ping [player]` | — | Checks ping | `Axentra.ping` |
| `/suicide` | — | Kill yourself | `Axentra.suicide` |
| `/hat` | — | Place held item on your head | `Axentra.hat` |
| `/anvil` | — | Opens an anvil | `Axentra.anvil` |
| `/workbench` | `/wb`, `/craft` | Opens a crafting table | `Axentra.workbench` |
| `/cartography` | `/carto` | Opens a cartography table | `Axentra.cartography` |
| `/grindstone` | `/grind` | Opens a grindstone | `Axentra.grindstone` |
| `/loom` | — | Opens a loom | `Axentra.loom` |
| `/smithingtable` | `/smithing`, `/smith` | Opens a smithing table | `Axentra.smithingtable` |
| `/stonecutter` | `/stone` | Opens a stonecutter | `Axentra.stonecutter` |
| `/enderchest` | `/ec`, `/echest` | Opens your ender chest | `Axentra.enderchest` |

### World

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/weather <clear\|rain\|storm>` | — | Sets the weather | `Axentra.weather` |
| `/sun` | `/sky` | Sets weather to clear | `Axentra.weather` |
| `/rain` | — | Sets weather to rain | `Axentra.weather` |
| `/storm` | — | Sets weather to thunderstorm | `Axentra.weather` |
| `/time <set\|add> <value>` | — | Sets or adds world time | `Axentra.time` |
| `/day` | — | Sets time to day | `Axentra.time` |
| `/noon` | — | Sets time to noon | `Axentra.time` |
| `/night` | — | Sets time to night | `Axentra.time` |
| `/midnight` | — | Sets time to midnight | `Axentra.time` |

### Teleportation

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/spawn [player]` | — | Teleports to server spawn | `Axentra.spawn` |
| `/setspawn` | — | Sets the server spawn (exact, centered) | `Axentra.setspawn` |
| `/back` | — | Teleports to your previous location | `Axentra.back` |
| `/tpa <player>` | — | Sends a teleport request | `Axentra.tpa` |
| `/tpaccept` | — | Accepts a teleport request | `Axentra.tpa` |
| `/tpdeny` | — | Denies a teleport request | `Axentra.tpa` |
| `/tpacancel` | — | Cancels your outgoing request | `Axentra.tpa` |
| `/tptoggle` | — | Toggles receiving teleport requests | `Axentra.tpa` |

### Player Management

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/gamemode <mode> [player]` | `/gm` | Changes gamemode | `Axentra.gamemode` |
| `/gms [player]` | — | Switches to survival | `Axentra.gamemode` |
| `/gmc [player]` | — | Switches to creative | `Axentra.gamemode` |
| `/gma [player]` | — | Switches to adventure | `Axentra.gamemode` |
| `/gmsp [player]` | — | Switches to spectator | `Axentra.gamemode` |
| `/speed <1-10\|reset> [walk\|fly] [player]` | — | Sets walk or fly speed | `Axentra.speed` |
| `/god [player]` | — | Toggles god mode | `Axentra.god` |
| `/vanish [player]` | `/v` | Toggles vanish mode | `Axentra.vanish` |
| `/invsee <player>` | — | Views a player's inventory | `Axentra.invsee` |
| `/enderchestsee <player>` | `/ecsee`, `/echestsee` | Views a player's ender chest | `Axentra.enderchestsee` |

### Plugin

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/axentra reload` | `/ax reload` | Reloads config and messages | `Axentra.admin` |
| `/axentra upgrade` | `/ax upgrade` | Checks for updates | `Axentra.admin` |
| `/axentra settings` | `/ax settings` | Opens the staff settings GUI | `Axentra.admin` |
| `/axentra help` | `/ax help` | Shows all commands | — |
| `/axentra information` | `/ax information` | Shows plugin information | — |

For the full documentation, check out the [Wiki](https://axentramc.com/wiki.html).

---

## Permissions

Permissions with **Everyone** as default are granted to all players automatically. All others default to **OP**.

| Permission | Default | Description |
|---|---|---|
| `Axentra.admin` | OP | Access to all `/axentra` subcommands |
| `Axentra.duty` | OP | Toggle duty status |
| `Axentra.staffmode` | OP | Toggle staffmode |
| `Axentra.report` | Everyone | Report a player |
| `Axentra.reports` | OP | View and manage reports |
| `Axentra.staffstats` | OP | View staff statistics |
| `Axentra.stafflist` | OP | View online staff list |
| `Axentra.punish` | OP | Use the punishment GUI |
| `Axentra.case` | OP | View moderation cases |
| `Axentra.case.delete` | OP | Permanently delete cases |
| `Axentra.history` | OP | View player moderation history |
| `Axentra.note` | OP | Manage staff notes + join notifications |
| `Axentra.auditlog` | OP | View the audit log |
| `Axentra.restoreinv` | OP | View and restore inventory snapshots |
| `Axentra.trust` | OP | View player trust levels |
| `Axentra.trust.set` | OP | Set player trust levels manually |
| `Axentra.alts` | OP | View player alt accounts |
| `Axentra.alts.ip` | OP | View player IP addresses in alt lookup |
| `Axentra.freeze` | OP | Freeze and unfreeze players |
| `Axentra.freeze.exempt` | OP | Cannot be frozen |
| `Axentra.timeline` | OP | View player activity timelines |
| `Axentra.sethome` | Everyone | Set home locations |
| `Axentra.home` | Everyone | Teleport to homes |
| `Axentra.home.others` | OP | Teleport to other players' homes |
| `Axentra.home.bypass` | OP | Skip home teleport delay |
| `Axentra.delhome` | Everyone | Delete homes |
| `Axentra.homes` | Everyone | View homes GUI |
| `Axentra.homes.others` | OP | View other players' homes |
| `Axentra.homes.unlimited` | OP | Unlimited homes |
| `Axentra.tp` | OP | Teleport to players |
| `Axentra.tphere` | OP | Teleport players to you |
| `Axentra.tpall` | OP | Teleport all players to you |
| `Axentra.tpahere` | OP | Request players to teleport to you |
| `Axentra.top` | Everyone | Teleport to highest block |
| `Axentra.broadcast` | OP | Use `/broadcast` |
| `Axentra.clearchat` | OP | Use `/clearchat` |
| `Axentra.clearchat.bypass` | OP | Chat not cleared by `/clearchat` |
| `Axentra.mutechat` | OP | Use `/mutechat` |
| `Axentra.mutechat.bypass` | OP | Chat when chat is globally muted |
| `Axentra.slowchat` | OP | Use `/slowchat` |
| `Axentra.slowchat.bypass` | OP | Bypass slow chat cooldown |
| `Axentra.staffchat` | OP | Use `/staffchat` |
| `Axentra.msg` | Everyone | Use `/msg`, `/reply`, `/msgtoggle` |
| `Axentra.msg.bypass` | OP | Message players with msgtoggle off |
| `Axentra.socialspy` | OP | See private messages between others |
| `Axentra.nick` | OP | Set your own nickname |
| `Axentra.nick.others` | OP | Set other players' nicknames |
| `Axentra.nick.color` | OP | Use color codes in nicknames |
| `Axentra.realname` | Everyone | Use `/realname` |
| `Axentra.me` | Everyone | Use `/me` |
| `Axentra.spawn` | Everyone | Use `/spawn` on yourself |
| `Axentra.spawn.others` | OP | Teleport others to spawn |
| `Axentra.spawn.bypass` | OP | Skip the `/spawn` countdown |
| `Axentra.setspawn` | OP | Use `/setspawn` |
| `Axentra.back` | Everyone | Use `/back` |
| `Axentra.tpa` | Everyone | Use all TPA commands |
| `Axentra.vanish` | OP | Toggle vanish for yourself |
| `Axentra.vanish.others` | OP | Toggle vanish for others |
| `Axentra.vanish.see` | OP | See vanished players |
| `Axentra.god` | OP | Toggle god mode for yourself |
| `Axentra.god.others` | OP | Toggle god mode for others |
| `Axentra.invsee` | OP | Use `/invsee` |
| `Axentra.enderchestsee` | OP | Use `/enderchestsee` |
| `Axentra.gamemode` | OP | Change your own gamemode |
| `Axentra.gamemode.others` | OP | Change other players' gamemode |
| `Axentra.speed` | OP | Set your own speed |
| `Axentra.speed.others` | OP | Set other players' speed |
| `Axentra.fly` | OP | Use `/fly` on yourself |
| `Axentra.fly.others` | OP | Use `/fly` on others |
| `Axentra.heal` | OP | Use `/heal` on yourself |
| `Axentra.heal.others` | OP | Use `/heal` on others |
| `Axentra.feed` | OP | Use `/feed` on yourself |
| `Axentra.feed.others` | OP | Use `/feed` on others |
| `Axentra.clear` | OP | Use `/clear` on yourself |
| `Axentra.clear.others` | OP | Use `/clear` on others |
| `Axentra.repair` | OP | Use `/repair` |
| `Axentra.kick` | OP | Use `/kick` |
| `Axentra.ban` | OP | Use `/ban` |
| `Axentra.tempban` | OP | Use `/tempban` |
| `Axentra.unban` | OP | Use `/unban` |
| `Axentra.banip` | OP | Use `/banip` |
| `Axentra.tempbanip` | OP | Use `/tempbanip` |
| `Axentra.unbanip` | OP | Use `/unbanip` |
| `Axentra.warn` | OP | Use `/warn` |
| `Axentra.warns` | OP | Use `/warns` |
| `Axentra.clearwarns` | OP | Use `/clearwarns` |
| `Axentra.mute` | OP | Use `/mute` |
| `Axentra.tempmute` | OP | Use `/tempmute` |
| `Axentra.unmute` | OP | Use `/unmute` |
| `Axentra.notify.kick` | OP | See kick announcements |
| `Axentra.notify.ban` | OP | See ban announcements |
| `Axentra.notify.warn` | OP | See warn announcements |
| `Axentra.notify.mute` | OP | See mute/unmute announcements |
| `Axentra.weather` | OP | Use weather commands |
| `Axentra.time` | OP | Use time commands |
| `Axentra.ping` | OP | Check your own ping |
| `Axentra.ping.others` | OP | Check other players' ping |
| `Axentra.hat` | OP | Use `/hat` |
| `Axentra.suicide` | OP | Use `/suicide` |
| `Axentra.anvil` | OP | Use `/anvil` |
| `Axentra.workbench` | OP | Use `/workbench` |
| `Axentra.cartography` | OP | Use `/cartography` |
| `Axentra.grindstone` | OP | Use `/grindstone` |
| `Axentra.loom` | OP | Use `/loom` |
| `Axentra.smithingtable` | OP | Use `/smithingtable` |
| `Axentra.stonecutter` | OP | Use `/stonecutter` |
| `Axentra.enderchest` | OP | Use `/enderchest` |

For the full permissions reference, check out the [Wiki](https://axentramc.com/wiki.html).

---

## Configuration

Axentra is fully configurable through four files:

- **`config.yml`** — General plugin settings: 21 module toggles, announcements, ban appeal link, warn punishments, spawn/TPA countdowns, homes settings, invsee read-only mode, slow chat defaults, social media links, vanish options, report cooldown
- **`messages.yml`** — Every message the plugin sends, supports color codes and placeholders
- **`punishments.yml`** — Punishment GUI templates with display name, action, reason, duration and icon
- **`protection.yml`** — Player protection settings: VPN detection API key, freeze options, snapshot triggers, trust thresholds, alt detection

After making changes run `/axentra reload` to apply them without restarting the server. New config keys are added automatically with their comments when the plugin updates.

---

## Requirements

- Minecraft server running **Spigot** or **Paper**
- Minecraft **1.21** or higher
- **Java 21** or higher

---

## Building from Source

```bash
git clone https://github.com/OlliesGitHubWorld/Axentra.git
cd Axentra
./mvnw clean package
```

The compiled jar will be in the `target/` folder.

---

## Contributing

Contributions are welcome! Feel free to open a [Pull Request](https://github.com/OlliesGitHubWorld/Axentra/pulls) or an [Issue](https://github.com/OlliesGitHubWorld/Axentra/issues).

---

## License

Axentra is licensed under the [MIT License](LICENSE).

---

<div align="center">

Made with love by [OlliesWorld](https://github.com/OlliesGitHubWorld)

</div>
