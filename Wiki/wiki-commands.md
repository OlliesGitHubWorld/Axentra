<div align="center">

# 🎮 Commands

</div>

---

## Utility Commands

### /anvil
Opens an anvil without needing a physical block.

| | |
|---|---|
| **Usage** | `/anvil` |
| **Permission** | `Axentra.anvil` |

---

### /workbench
Opens a crafting table without needing a physical block.

| | |
|---|---|
| **Usage** | `/workbench` |
| **Aliases** | `/wb`, `/craft`, `/craftingtable` |
| **Permission** | `Axentra.workbench` |

---

### /cartography
Opens a cartography table without needing a physical block.

| | |
|---|---|
| **Usage** | `/cartography` |
| **Aliases** | `/carto`, `/cartographytable` |
| **Permission** | `Axentra.cartography` |

---

### /grindstone
Opens a grindstone without needing a physical block.

| | |
|---|---|
| **Usage** | `/grindstone` |
| **Aliases** | `/grind` |
| **Permission** | `Axentra.grindstone` |

---

### /loom
Opens a loom without needing a physical block.

| | |
|---|---|
| **Usage** | `/loom` |
| **Permission** | `Axentra.loom` |

---

### /smithingtable
Opens a smithing table without needing a physical block.

| | |
|---|---|
| **Usage** | `/smithingtable` |
| **Aliases** | `/smithing`, `/smith` |
| **Permission** | `Axentra.smithingtable` |

---

### /stonecutter
Opens a stonecutter without needing a physical block.

| | |
|---|---|
| **Usage** | `/stonecutter` |
| **Aliases** | `/stone` |
| **Permission** | `Axentra.stonecutter` |

---

### /enderchest
Opens your ender chest from anywhere.

| | |
|---|---|
| **Usage** | `/enderchest` |
| **Aliases** | `/ec`, `/echest` |
| **Permission** | `Axentra.enderchest` |

---

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

### /hat
Places the item in your hand on your head as a helmet.

| | |
|---|---|
| **Usage** | `/hat` |
| **Permission** | `Axentra.hat` |

> **Note:** If you are already wearing a helmet it will be swapped to your hand.

---

### /ping
Displays your current ping or another player's ping in milliseconds.

| | |
|---|---|
| **Usage** | `/ping [player]` |
| **Permission** | `Axentra.ping` |
| **Permission (others)** | `Axentra.ping.others` |

**Examples:**
```
/ping
/ping OlliesOtherWorld
```

---

### /suicide
Instantly kills yourself.

| | |
|---|---|
| **Usage** | `/suicide` |
| **Permission** | `Axentra.suicide` |

---

## World Commands

### /time
Sets or adds to the time in the world.

| | |
|---|---|
| **Usage** | `/time <set\|add> <day\|noon\|night\|midnight\|ticks>` |
| **Permission** | `Axentra.time` |

| Shortcut | Description |
|---|---|
| `/day` | Sets time to day |
| `/noon` | Sets time to noon |
| `/night` | Sets time to night |
| `/midnight` | Sets time to midnight |

**Examples:**
```
/time set day
/time set 6000
/time add 1000
/day
/night
```

---

### /weather
Sets the weather in the world.

| | |
|---|---|
| **Usage** | `/weather <clear\|rain\|storm>` |
| **Permission** | `Axentra.weather` |

| Shortcut | Description |
|---|---|
| `/sun` | Sets weather to clear |
| `/sky` | Sets weather to clear |
| `/rain` | Sets weather to rain |
| `/storm` | Sets weather to thunderstorm |

**Examples:**
```
/weather clear
/weather storm
/sun
/rain
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
| `/axentra information` | Shows plugin information |