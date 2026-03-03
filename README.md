<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0--alpha-blue?style=flat-square)
![Minecraft](https://img.shields.io/badge/minecraft-1.13--1.21.11-green?style=flat-square)
![Java](https://img.shields.io/badge/java-21-orange?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-purple?style=flat-square)

# Axentra

**A powerful, lightweight Minecraft server management plugin.**

**[⬇️ Download](https://github.com/OlliesGitHubWorld/Axentra/releases/latest)** • **[📖 Wiki](https://github.com/OlliesGitHubWorld/Axentra/wiki)** • **[🐛 Report Bug](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=bug-report.md)** • **[✨ Request Feature](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=feature_request.md)**

</div>

---

## 📖 About

Axentra is a Minecraft Bukkit/Spigot plugin that adds custom join/leave messages, inventory management, and powerful server administration commands — all fully configurable through simple YAML files. No external databases or dependencies required!

---

## ✨ Features

- 🎉 **Custom Join & Leave Messages** — Fully customizable with color codes and placeholders
- 🌟 **First Join Detection** — Special message for first time players
- 🔨 **Staff Commands** — Kick, ban, unban and more with full reason support
- 🛠️ **Utility Commands** — Clear inventory, repair items on the fly
- 📢 **Announcements** — Announce kicks and bans to the whole server
- 🔗 **Ban Appeal Link** — Show a Discord or website link on the ban screen
- 🗃️ **SQLite Database** — Persistent ban storage, no external database needed
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
| `/clear [player]` | Clears a player's inventory | `Axentra.clear` |
| `/repair` | Repairs the item in your hand | `Axentra.repair` |
| `/kick <player> [reason]` | Kicks a player from the server | `Axentra.kick` |
| `/ban <player> [reason]` | Bans a player from the server | `Axentra.ban` |
| `/unban <player>` | Unbans a player from the server | `Axentra.unban` |
| `/axentra reload` | Reloads config and messages | `Axentra.admin` |
| `/axentra upgrade` | Checks for updates | `Axentra.admin` |
| `/axentra help` | Shows all commands | `Axentra.admin` |
| `/axentra information` | Shows plugin information | `Axentra.admin` |

For a full list of commands and usage, check out the [Commands Wiki](https://github.com/OlliesGitHubWorld/Axentra/wiki/Commands).

---

## 🔒 Permissions

| Permission | Default | Description |
|---|---|---|
| `Axentra.admin` | OP | Access to all `/axentra` subcommands |
| `Axentra.clear` | OP | Use `/clear` |
| `Axentra.repair` | OP | Use `/repair` |
| `Axentra.kick` | OP | Use `/kick` |
| `Axentra.ban` | OP | Use `/ban` |
| `Axentra.unban` | OP | Use `/unban` |

---

## ⚙️ Configuration

Axentra is fully configurable through two files:

- **`config.yml`** — General plugin settings such as kick/ban announcements and ban appeal link
- **`messages.yml`** — Every message the plugin sends, supports color codes and placeholders

After making changes run `/axentra reload` to apply them without restarting the server.

---

## 🛠️ Requirements

- Minecraft server running **Spigot** or **Paper**
- Minecraft **1.13** or higher
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
