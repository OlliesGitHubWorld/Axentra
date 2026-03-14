<div align="center">

# 🔒 Permissions

</div>

---

## Permission Nodes

| Permission | Default | Description |
|---|---|---|
| `Axentra.admin` | OP | Access to all `/axentra` subcommands |
| `Axentra.anvil` | OP | Allows the player to use `/anvil` |
| `Axentra.workbench` | OP | Allows the player to use `/workbench` |
| `Axentra.cartography` | OP | Allows the player to use `/cartography` |
| `Axentra.grindstone` | OP | Allows the player to use `/grindstone` |
| `Axentra.loom` | OP | Allows the player to use `/loom` |
| `Axentra.smithingtable` | OP | Allows the player to use `/smithingtable` |
| `Axentra.stonecutter` | OP | Allows the player to use `/stonecutter` |
| `Axentra.enderchest` | OP | Allows the player to use `/enderchest` |
| `Axentra.clear` | OP | Allows the player to clear their own inventory |
| `Axentra.clear.others` | OP | Allows the player to clear other players' inventories |
| `Axentra.repair` | OP | Allows the player to repair their held item |
| `Axentra.fly` | OP | Allows the player to toggle flight for themselves |
| `Axentra.fly.others` | OP | Allows the player to toggle flight for others |
| `Axentra.heal` | OP | Allows the player to heal themselves |
| `Axentra.heal.others` | OP | Allows the player to heal others |
| `Axentra.feed` | OP | Allows the player to feed themselves |
| `Axentra.feed.others` | OP | Allows the player to feed others |
| `Axentra.hat` | OP | Allows the player to use `/hat` |
| `Axentra.ping` | OP | Allows the player to check their own ping |
| `Axentra.ping.others` | OP | Allows the player to check other players' ping |
| `Axentra.suicide` | OP | Allows the player to use `/suicide` |
| `Axentra.time` | OP | Allows the player to change the world time |
| `Axentra.weather` | OP | Allows the player to change the weather |
| `Axentra.warn` | OP | Allows the player to warn others |
| `Axentra.warns` | OP | Allows the player to view warnings |
| `Axentra.clearwarns` | OP | Allows the player to clear warnings |
| `Axentra.mute` | OP | Allows the player to mute others |
| `Axentra.unmute` | OP | Allows the player to unmute others |
| `Axentra.kick` | OP | Allows the player to kick others |
| `Axentra.ban` | OP | Allows the player to ban others |
| `Axentra.tempban` | OP | Allows the player to temporarily ban others |
| `Axentra.unban` | OP | Allows the player to unban others |
| `Axentra.banip` | OP | Allows the player to ban IP addresses |
| `Axentra.tempbanip` | OP | Allows the player to temporarily ban IP addresses |
| `Axentra.unbanip` | OP | Allows the player to unban IP addresses |

> **Note:** All permissions default to **OP only**. Use a permissions plugin like LuckPerms to grant them to other players or groups.

---

## Setting Permissions with LuckPerms

**Grant a permission to a player:**
```
/lp user <player> permission set Axentra.clear true
```

**Grant a permission to a group:**
```
/lp group <group> permission set Axentra.clear true
```

---

## Recommended Setup

Here's a recommended permission setup for different staff ranks:

| Rank | Permissions |
|---|---|
| **Admin** | All permissions |
| **Moderator** | `Axentra.kick`, `Axentra.ban`, `Axentra.tempban`, `Axentra.unban`, `Axentra.banip`, `Axentra.tempbanip`, `Axentra.unbanip`, `Axentra.mute`, `Axentra.unmute`, `Axentra.warn`, `Axentra.warns`, `Axentra.clearwarns`, `Axentra.clear`, `Axentra.clear.others`, `Axentra.repair` |
| **Helper** | `Axentra.kick`, `Axentra.warn`, `Axentra.warns`, `Axentra.mute`, `Axentra.unmute`, `Axentra.clear` |