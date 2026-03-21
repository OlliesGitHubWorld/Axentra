<div align="center">

![Version](https://img.shields.io/badge/version-1.3.0-blue?style=flat-square)
![Minecraft](https://img.shields.io/badge/minecraft-1.21-green?style=flat-square)
![Java](https://img.shields.io/badge/java-21-orange?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-purple?style=flat-square)
![Status](https://img.shields.io/badge/status-stable-brightgreen?style=flat-square)

# Axentra

**A powerful, lightweight Minecraft server management plugin.**

**[⬇️ Download](https://github.com/OlliesGitHubWorld/Axentra/releases/latest)** • **[📖 Wiki](https://github.com/OlliesGitHubWorld/Axentra/wiki)** • **[🐛 Report Bug](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=bug_report.md)** • **[✨ Request Feature](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=feature_request.md)**

</div>

---

## 🆕 What's New in v1.3.0

- **Teleportation** — `/spawn` with configurable countdown, `/setspawn`, `/tpa` request system, and `/back` on death or teleport
- **God Mode** — Toggle invincibility for yourself or others with `/god`
- **Vanish** — Full vanish system with see-vanished permission support
- **Gamemode & Speed** — Quick switching via `/gms`, `/gmc`, `/gma`, `/gmsp` and walk/fly speed control
- **Inventory Inspection** — `/invsee` and `/enderchestsee` with optional read-only mode

---

## 📖 About

Axentra is a Minecraft Bukkit/Spigot plugin that adds custom join/leave messages, inventory management, teleportation, god mode, vanish, and powerful server administration commands — all fully configurable through simple YAML files. No external databases or dependencies required!

---

## ✨ Features

- 🎉 **Custom Join & Leave Messages** — Fully customizable with color codes and placeholders
- 🌟 **First Join Detection** — Special message for first time players
- 🔨 **Staff Commands** — Kick, ban, tempban, mute, warn and more with full reason support
- ⚠️ **Warning System** — Warn players with configurable auto-punishments at thresholds
- 🔇 **Mute System** — Mute and unmute players to block chat messages
- 🚫 **IP Banning** — Ban and temp-ban IP addresses to block alt accounts
- 🛠️ **Utility Commands** — Clear inventory, repair items, fly, heal, feed, hat and more
- 🧰 **GUI Commands** — Open anvil, workbench, cartography, grindstone, loom, smithing table, stonecutter and ender chest anywhere
- 🌍 **World Control** — Set weather and time with dedicated shortcut commands
- 🚀 **Teleportation** — `/spawn` with countdown, `/tpa` request system, and `/back` on death or teleport
- 👻 **Vanish** — Toggle vanish for yourself or others, with see-vanished permission support
- ☠️ **God Mode** — Toggle invincibility for yourself or others
- 🎮 **Gamemode & Speed** — Quick gamemode switching (gms/gmc/gma/gmsp) and walk/fly speed control
- 👁️ **Inventory Inspection** — View other players' inventories and ender chests (read-only mode configurable)
- 📢 **Announcements** — Announce kicks, bans and warns to the whole server
- 🔗 **Ban Appeal Link** — Show a Discord or website link on the ban screen
- 🗃️ **SQLite Database** — Persistent ban, mute and warn storage, no external database needed
- 📝 **Fully Configurable** — Every message and setting is editable in YAML
- 🔄 **Live Reload** — Reload config and messages without restarting the server
- 🔔 **Update Notifications** — Automatically notifies admins of new updates on join

---

## 🚀 Installation

1. Download the latest `axentra-x.x.x.jar` from the [Releases](https://github.com/OlliesGitHubWorld/Axentra/releases/latest) page
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Edit `plugins/Axentra/config.yml` and `plugins/Axentra/messages.yml`
5. Run `/axentra reload` to apply your changes!

For a more detailed guide, check out the [Installation Wiki](https://github.com/OlliesGitHubWorld/Axentra/wiki/Installation).

---

## 🎮 Commands

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
| `/mute <player> [reason]` | — | Mutes a player | `Axentra.mute` |
| `/unmute <player>` | — | Unmutes a player | `Axentra.unmute` |

### Utility

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/clear [player]` | `/c` | Clears a player's inventory | `Axentra.clear` |
| `/repair` | `/r` | Repairs the item in your hand | `Axentra.repair` |
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
| `/spawn [player]` | — | Teleports to world spawn | `Axentra.spawn` |
| `/setspawn` | — | Sets the world spawn | `Axentra.setspawn` |
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
| `/axentra help` | `/ax help` | Shows all commands | — |
| `/axentra information` | `/ax information` | Shows plugin information | — |

For a full list of commands and usage, check out the [Commands Wiki](https://github.com/OlliesGitHubWorld/Axentra/wiki/Commands).

---

## 🔒 Permissions

Permissions with **Everyone** as default are granted to all players automatically. All others default to **OP**.

| Permission | Default | Description |
|---|---|---|
| `Axentra.admin` | OP | Access to all `/axentra` subcommands |
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
| `Axentra.unmute` | OP | Use `/unmute` |
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

For the full permissions reference, check out the [Permissions Wiki](https://github.com/OlliesGitHubWorld/Axentra/wiki/Permissions).

---

## ⚙️ Configuration

Axentra is fully configurable through two files:

- **`config.yml`** — General plugin settings such as announcements, ban appeal link, warn punishments, spawn countdown, TPA countdown, and invsee read-only mode
- **`messages.yml`** — Every message the plugin sends, supports color codes and placeholders

After making changes run `/axentra reload` to apply them without restarting the server.

---

## 🛠️ Requirements

- Minecraft server running **Spigot** or **Paper**
- Minecraft **1.21** or higher
- **Java 21** or higher

---

## 📦 Building from Source

```bash
git clone https://github.com/OlliesGitHubWorld/Axentra.git
cd Axentra
mvn clean package
```

The compiled jar will be in the `target/` folder.

---

## 🤝 Contributing

Contributions are welcome! Feel free to open a [Pull Request](https://github.com/OlliesGitHubWorld/Axentra/pulls) or an [Issue](https://github.com/OlliesGitHubWorld/Axentra/issues).

---

## 📜 License

Axentra is licensed under the [MIT License](LICENSE).

---

<div align="center">

Made with ❤️ by [OlliesWorld](https://github.com/OlliesGitHubWorld)

</div>
