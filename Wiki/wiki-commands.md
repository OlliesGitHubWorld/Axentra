<div align="center">

# 🎮 Commands

</div>

---

## Utility Commands

### /clear
Clears a player's inventory.

| | |
|---|---|
| **Usage** | `/clear [player]` |
| **Aliases** | `/c` |
| **Permission** | `Axentra.clear` |
| **Permission (others)** | `Axentra.clear.others` |

**Examples:**
```
/clear
/clear OlliesOtherWorld
```

---

### /repair
Repairs the durability of the item in your main hand.

| | |
|---|---|
| **Usage** | `/repair` |
| **Aliases** | `/r` |
| **Permission** | `Axentra.repair` |

> **Note:** The item must be damaged and you must be holding an item.

---

### /fly
Toggles flight mode for yourself or another player.

| | |
|---|---|
| **Usage** | `/fly [player]` |
| **Permission** | `Axentra.fly` |
| **Permission (others)** | `Axentra.fly.others` |

**Examples:**
```
/fly
/fly OlliesOtherWorld
```

---

### /heal
Fully restores a player's health.

| | |
|---|---|
| **Usage** | `/heal [player]` |
| **Permission** | `Axentra.heal` |
| **Permission (others)** | `Axentra.heal.others` |

**Examples:**
```
/heal
/heal OlliesOtherWorld
```

---

### /feed
Fully restores a player's hunger.

| | |
|---|---|
| **Usage** | `/feed [player]` |
| **Permission** | `Axentra.feed` |
| **Permission (others)** | `Axentra.feed.others` |

**Examples:**
```
/feed
/feed OlliesOtherWorld
```

---

## Staff Commands

### /warn
Issues a warning to a player. Warnings are stored persistently in the database. Automatic punishments can be configured in `config.yml` to trigger at certain warning counts.

| | |
|---|---|
| **Usage** | `/warn <player> [reason]` |
| **Permission** | `Axentra.warn` |

**Examples:**
```
/warn OlliesWorldYT
/warn OlliesWorldYT Spamming in chat
```

> **Note:** See [Warn Punishments](Installation#warn-punishments) in the config to set up automatic actions at warning thresholds.

---

### /warns
Displays all active warnings for a player.

| | |
|---|---|
| **Usage** | `/warns [player]` |
| **Permission** | `Axentra.warns` |

**Examples:**
```
/warns
/warns OlliesWorldYT
```

---

### /clearwarns
Clears all active warnings for a player.

| | |
|---|---|
| **Usage** | `/clearwarns <player>` |
| **Permission** | `Axentra.clearwarns` |

**Examples:**
```
/clearwarns OlliesWorldYT
```

---

### /mute
Prevents a player from sending chat messages.

| | |
|---|---|
| **Usage** | `/mute <player> [reason]` |
| **Permission** | `Axentra.mute` |

**Examples:**
```
/mute OlliesWorldYT
/mute OlliesWorldYT Spamming
```

---

### /unmute
Removes a mute from a player, allowing them to chat again.

| | |
|---|---|
| **Usage** | `/unmute <player>` |
| **Permission** | `Axentra.unmute` |

**Examples:**
```
/unmute OlliesWorldYT
```

---

### /kick
Kicks a player from the server with an optional reason.

| | |
|---|---|
| **Usage** | `/kick <player> [reason]` |
| **Permission** | `Axentra.kick` |

**Examples:**
```
/kick OlliesWorldYT
/kick OlliesWorldYT Breaking the rules
```

---

### /ban
Permanently bans a player from the server.

| | |
|---|---|
| **Usage** | `/ban <player> [reason]` |
| **Permission** | `Axentra.ban` |

**Examples:**
```
/ban OlliesWorldYT
/ban OlliesWorldYT Hacking
```

> **Note:** You can ban offline players too. Banned players will see the ban reason and appeal link when they try to join.

---

### /tempban
Temporarily bans a player for a specified duration.

| | |
|---|---|
| **Usage** | `/tempban <player> <duration> [reason]` |
| **Permission** | `Axentra.tempban` |

**Duration format:** Combine any of `d` (days), `h` (hours), `m` (minutes), `s` (seconds).

**Examples:**
```
/tempban OlliesWorldYT 1d
/tempban OlliesWorldYT 12h Griefing
/tempban OlliesWorldYT 1d12h30m Ban evading
```

---

### /unban
Unbans a previously banned player.

| | |
|---|---|
| **Usage** | `/unban <player>` |
| **Permission** | `Axentra.unban` |

**Examples:**
```
/unban OlliesWorldYT
```

---

### /banip
Permanently bans an IP address from the server. Can target a player name or a raw IP.

| | |
|---|---|
| **Usage** | `/banip <player\|ip> [reason]` |
| **Permission** | `Axentra.banip` |

**Examples:**
```
/banip OlliesWorldYT
/banip OlliesWorldYT Alt account
/banip 192.168.1.1
```

---

### /tempbanip
Temporarily bans an IP address for a specified duration.

| | |
|---|---|
| **Usage** | `/tempbanip <player\|ip> <duration> [reason]` |
| **Permission** | `Axentra.tempbanip` |

**Examples:**
```
/tempbanip OlliesWorldYT 7d
/tempbanip 192.168.1.1 1d Suspicious activity
```

---

### /unbanip
Unbans a previously banned IP address.

| | |
|---|---|
| **Usage** | `/unbanip <ip>` |
| **Permission** | `Axentra.unbanip` |

**Examples:**
```
/unbanip 192.168.1.1
```

---

## Admin Commands

### /axentra
The main Axentra administration command.

| | |
|---|---|
| **Usage** | `/axentra <reload\|upgrade\|help\|information>` |
| **Aliases** | `/ax` |
| **Permission** | `Axentra.admin` |

| Subcommand | Description |
|---|---|
| `/axentra reload` | Reloads `config.yml` and `messages.yml` without restarting |
| `/axentra upgrade` | Checks GitHub for a newer version |
| `/axentra help` | Shows all commands and descriptions |
| `/axentra information` | Shows plugin, server and player information |