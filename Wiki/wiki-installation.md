<div align="center">

# 🚀 Installation

</div>

---

## Requirements

Before installing Axentra make sure your server meets these requirements:

| Requirement | Minimum |
|---|---|
| Server Software | Spigot or Paper |
| Minecraft Version | 1.21 or higher |
| Java Version | Java 21 or higher |

---

## 📥 Downloading Axentra

1. Go to the [Releases](https://github.com/OlliesGitHubWorld/Axentra/releases/latest) page
2. Download the latest `axentra-x.x.x.jar` file
3. Do **not** download the `original-axentra-x.x.x.jar` file

---

## 📂 Installing Axentra

1. Place the downloaded jar into your server's `plugins/` folder
2. Restart your server
3. Axentra will generate the following files automatically:

```
plugins/
  Axentra/
    config.yml
    messages.yml
    data.db
```

---

## ⚙️ Configuration

After installing, you can configure Axentra by editing the generated files:

- **`config.yml`** — General plugin settings such as announcements, ban appeal link, and warn punishments
- **`messages.yml`** — Every message the plugin sends including join/leave messages, command responses and more

After making changes run `/axentra reload` to apply them without restarting the server.

### Warn Punishments

You can configure automatic punishments that trigger when a player reaches a certain number of warnings. Edit the `warn-punishments` section in `config.yml`:

```yaml
warn-punishments:
  3:
    action: kick
    reason: "You have accumulated 3 warnings."
  5:
    action: mute
    reason: "You have accumulated 5 warnings."
  7:
    action: tempban
    duration: "1d"
    reason: "You have accumulated 7 warnings."
  10:
    action: ban
    reason: "You have accumulated 10 warnings."
```

| Action | Description |
|---|---|
| `kick` | Kicks the player with the given reason |
| `mute` | Mutes the player, preventing them from chatting |
| `tempban` | Temporarily bans the player (requires a `duration` field) |
| `ban` | Permanently bans the player |

Add or remove thresholds as needed. Set the section to `{}` to disable all auto-punishments.

---

## ✅ Verifying Installation

Once the server has started you should see this in your console:

```
[Axentra] Database connected!
[Axentra]   ================================
[Axentra]        A  X  E  N  T  R  A
[Axentra]   ================================
[Axentra]   Version: 1.1.0
[Axentra]   Author:  OlliesWorld
[Axentra]   Status:  Successfully started!
[Axentra]   ================================
```

If you see any errors, check the [FAQ](FAQ) page or open a [Support Issue](https://github.com/OlliesGitHubWorld/Axentra/issues/new?template=question.md).