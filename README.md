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

## 📖 About

Axentra is a Minecraft Bukkit/Spigot plugin that adds custom join/leave messages, inventory management, and powerful server administration commands — all fully configurable through simple YAML files. No external databases or dependencies required!

---

## ✨ Features

- 🎉 **Custom Join & Leave Messages** — Fully customizable with color codes and placeholders
- 🌟 **First Join Detection** — Special message for first time players
- 🔨 **Staff Commands** — Kick, ban, tempban, mute, warn and more with full reason support
- ⚠️ **Warning System** — Warn players with configurable auto-punishments at thresholds
- 🔇 **Mute System** — Mute and unmute players to block chat messages
- 🚫 **IP Banning** — Ban and temp-ban IP addresses to block alt accounts
- 🛠️ **Utility Commands** — Clear inventory, repair items, fly, heal, feed and more
- 🪣 **Virtual Block Commands** — Open anvils, workbenches, enderchests and more without a physical block
- 🌤️ **World Commands** — Change time and weather with shortcuts like /day, /night, /sun and /rain
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

| Command | Description | Permission |
|---|---|---|
| `/anvil` | Opens an anvil | `Axentra.anvil` |
| `/workbench` | Opens a crafting table | `Axentra.workbench` |
| `/cartography` | Opens a cartography table | `Axentra.cartography` |
| `/grindstone` | Opens a grindstone | `Axentra.grindstone` |
| `/loom` | Opens a loom | `Axentra.loom` |
| `/smithingtable` | Opens a smithing table | `Axentra.smithingtable` |
| `/stonecutter` | Opens a stonecutter | `Axentra.stonecutter` |
| `/enderchest` | Opens your ender chest | `Axentra.enderchest` |
| `/clear [player]` | Clears a player's inventory | `Axentra.clear` |
| `/repair` | Repairs the item in your hand | `Axentra.repair` |
| `/fly [player]` | Toggles flight mode | `Axentra.fly` |
| `/heal [player]` | Heals a player | `Axentra.heal` |
| `/feed [player]` | Feeds a player | `Axentra.feed` |
| `/hat` | Places your held item on your head | `Axentra.hat` |
| `/ping [player]` | Shows ping in milliseconds | `Axentra.ping` |
| `/suicide` | Kills yourself | `Axentra.suicide` |
| `/time <set\|add> <value>` | Sets or adds to the world time | `Axentra.time` |
| `/weather <clear\|rain\|storm>` | Sets the weather | `Axentra.weather` |
| `/warn <player> [reason]` | Warns a player | `Axentra.warn` |
| `/warns [player]` | View a player's warnings | `Axentra.warns` |
| `/clearwarns <player>` | Clears a player's warnings | `Axentra.clearwarns` |
| `/mute <player> [reason]` | Mutes a player | `Axentra.mute` |
| `/unmute <player>` | Unmutes a player | `Axentra.unmute` |
| `/kick <player> [reason]` | Kicks a player | `Axentra.kick` |
| `/ban <player> [reason]` | Permanently bans a player | `Axentra.ban` |
| `/tempban <player> <duration> [reason]` | Temporarily bans a player | `Axentra.tempban` |
| `/unban <player>` | Unbans a player | `Axentra.unban` |
| `/banip <player\|ip> [reason]` | Bans an IP address | `Axentra.banip` |
| `/tempbanip <player\|ip> <duration> [reason]` | Temporarily bans an IP | `Axentra.tempbanip` |
| `/unbanip <ip>` | Unbans an IP address | `Axentra.unbanip` |
| `/axentra reload` | Reloads config and messages | `Axentra.admin` |
| `/axentra upgrade` | Checks for updates | `Axentra.admin` |
| `/axentra help` | Shows all commands | — |
| `/axentra information` | Shows plugin, server and player information | — |

For a full list of commands and usage, check out the [Commands Wiki](https://github.com/OlliesGitHubWorld/Axentra/wiki/Commands).

---

## 🔒 Permissions

| Permission | Default | Description |
|---|---|---|
| `Axentra.admin` | OP | Access to all `/axentra` subcommands |
| `Axentra.anvil` | OP | Use `/anvil` |
| `Axentra.workbench` | OP | Use `/workbench` |
| `Axentra.cartography` | OP | Use `/cartography` |
| `Axentra.grindstone` | OP | Use `/grindstone` |
| `Axentra.loom` | OP | Use `/loom` |
| `Axentra.smithingtable` | OP | Use `/smithingtable` |
| `Axentra.stonecutter` | OP | Use `/stonecutter` |
| `Axentra.enderchest` | OP | Use `/enderchest` |
| `Axentra.clear` | OP | Use `/clear` on yourself |
| `Axentra.clear.others` | OP | Use `/clear` on others |
| `Axentra.repair` | OP | Use `/repair` |
| `Axentra.fly` | OP | Use `/fly` on yourself |
| `Axentra.fly.others` | OP | Use `/fly` on others |
| `Axentra.heal` | OP | Use `/heal` on yourself |
| `Axentra.heal.others` | OP | Use `/heal` on others |
| `Axentra.feed` | OP | Use `/feed` on yourself |
| `Axentra.feed.others` | OP | Use `/feed` on others |
| `Axentra.hat` | OP | Use `/hat` |
| `Axentra.ping` | OP | Use `/ping` on yourself |
| `Axentra.ping.others` | OP | Use `/ping` on others |
| `Axentra.suicide` | OP | Use `/suicide` |
| `Axentra.time` | OP | Use `/time` and shortcuts |
| `Axentra.weather` | OP | Use `/weather` and shortcuts |
| `Axentra.warn` | OP | Use `/warn` |
| `Axentra.warns` | OP | Use `/warns` |
| `Axentra.clearwarns` | OP | Use `/clearwarns` |
| `Axentra.mute` | OP | Use `/mute` |
| `Axentra.unmute` | OP | Use `/unmute` |
| `Axentra.kick` | OP | Use `/kick` |
| `Axentra.ban` | OP | Use `/ban` |
| `Axentra.tempban` | OP | Use `/tempban` |
| `Axentra.unban` | OP | Use `/unban` |
| `Axentra.banip` | OP | Use `/banip` |
| `Axentra.tempbanip` | OP | Use `/tempbanip` |
| `Axentra.unbanip` | OP | Use `/unbanip` |

---

## ⚙️ Configuration

Axentra is fully configurable through two files:

- **`config.yml`** — General plugin settings such as announcements, ban appeal link, and warn punishments
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
