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

- **Inventory Snapshots** — Automatic snapshots on death/join/quit/clear with restore GUI
- **Trust System** — Three-tier trust (NEW → MEMBER → TRUSTED) with auto-progression
- **Alt & VPN Detection** — Shared IP alt detection with risk scoring, VPN detection via proxycheck.io
- **Freeze System** — Database-persisted freeze that survives restarts, configurable logout actions
- **Player Timeline** — Full activity history (joins, quits, punishments, snapshots)
- **Home System** — `/sethome`, `/home`, `/delhome`, `/homes` GUI with permission-based limits
- **Staff Teleport** — `/tp`, `/tphere`, `/tpall`, `/tpahere`, `/top`
- **Case Management** — `/case resolve` and `/case delete` with clickable actions
- **Smart Join Alerts** — Staff see alts, VPN, trust level and active cases on join
- **Config Auto-updater** — Adds new keys with comments in the correct position on update

---

## Features

| Category | Highlights |
|---|---|
| **Player Protection** | Inventory snapshots, trust system, alt detection, VPN detection, freeze, player timeline |
| **Staff Management** | Staffmode with tool hotbar, duty tracking, reports GUI, staff statistics, smart join alerts |
| **Moderation** | Punishment GUI, case system, history GUI, staff notes, audit log, chat log snapshots |
| **Punishments** | Kick, ban, tempban, IP ban, mute, tempmute, warn with auto-punishments at thresholds |
| **Homes & Teleport** | Homes with GUI, tp/tphere/tpall/top, TPA requests, spawn with countdown, back |
| **Chat** | Private messaging, social spy, broadcast, mute/slow/clear chat, staff chat, nicknames |
| **Vanish** | Ghost effect, fake join/quit, night vision, god mode, no pickup, action bar |
| **Utility** | Fly, heal, feed, repair, clear, god, speed, virtual workstations, weather/time control |
| **Configuration** | 21 toggleable modules, 4 config files, in-game settings GUI, auto-updating configs |

100+ commands — see the full list on the **[Wiki](https://axentramc.com/wiki.html)**.

---

## Installation

1. Download the latest `axentra-x.x.x.jar` from [Releases](https://github.com/OlliesGitHubWorld/Axentra/releases/latest)
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Edit the config files in `plugins/Axentra/`
5. Run `/axentra reload` to apply changes

---

## Requirements

- **Spigot** or **Paper** 1.21+
- **Java 21** or higher
- No dependencies

---

## Building from Source

```bash
git clone https://github.com/OlliesGitHubWorld/Axentra.git
cd Axentra
./mvnw clean package
```

---

## Contributing

Contributions are welcome! Open a [Pull Request](https://github.com/OlliesGitHubWorld/Axentra/pulls) or an [Issue](https://github.com/OlliesGitHubWorld/Axentra/issues).

---

## License

[MIT License](LICENSE)

---

<div align="center">

Made with love by [OlliesWorld](https://github.com/OlliesGitHubWorld)

</div>
