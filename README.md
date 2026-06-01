# PunishSystem

**A professional, network-wide punishment management plugin for Velocity proxy servers.**

PunishSystem gives your moderation team everything they need to keep the network clean: permanent bans, temporary bans, IP bans, network bans, kicks, warnings, predefined punishment presets, automatic multi-account detection, VPN/proxy blocking, and Discord webhook notifications — all without touching a single line of code.

---

## Features

### Core Punishment Commands
- **`/ban`** — Permanently ban a player with UUID tracking
- **`/tempban`** — Temporarily ban with flexible duration syntax (`30m`, `7d`, `1y`, `2w3d`, …)
- **`/kick`** — Disconnect an online player with a configurable message
- **`/unpunish`** / **`/unban`** — Lift any active ban

### Extended Moderation Tools
- **`/punish <player> <preset>`** — Apply a predefined punishment in one command — configure presets in `config.json` with reason, type, and duration, no recompile needed
- **IP Bans** — Block an IP address from connecting to the network
- **Network Bans** — Permanent server-wide ban with a dedicated appeal screen

### Warn System
- **`/warn <player> <reason>`** — Issue a formal warning; the player receives an in-game warning screen
- **`/warnings <player>`** — List all active warnings for a player
- **`/clearwarnings <player>`** — Remove all active warnings for a player

### Smart Detection
- **Multi-account detection** — When a player is banned, all accounts that have ever shared the same IP are automatically banned too
- **VPN / Proxy blocking** — Detects VPNs and proxies via ip-api.com with a configurable per-ISP whitelist and bypass permission

### Staff Tools
- **`/history <player>`** — Full punishment history (up to 50 entries)
- **`/check <player>`** — Active punishments at a glance
- **`/lookup <player>`** — Complete player profile: UUID, known IPs, linked accounts, ban history, online status
- **Staff notifications** — Every punishment is broadcast to online staff with the `punishsystem.notify` permission

### Integrations
- **Discord webhooks** — Automatic embeds for ban, tempban, unban, kick, IP ban, warn, and VPN events
- **LabyMod** — Native LabyMod Protocol support
- **bStats** — Anonymous usage statistics
- **Mojang API fallback** — Resolves UUIDs for offline players automatically
- **MySQL / MariaDB** — Reliable storage with HikariCP connection pooling
- **File storage fallback** — Works out of the box without a database; enable MySQL in `config.json` when ready

### Admin Friendly
- **`/punishreload`** — Hot-reload configuration without a server restart
- **Fully configurable messages** — Every player-facing text, the prefix, and all permission nodes are defined in `config.json`
- **Table prefix support** — Share a database with other plugins without conflicts

---

## Requirements

| | |
|-|-|
| **Velocity** | 3.x |
| **Java** | 21 or newer |
| **Database** | MySQL 5.7+ or MariaDB 10.4+ *(optional — file storage used if not configured)* |

---

## Quick Start

1. Drop `punishsystem-x.x.x.jar` into your `plugins/` folder.
2. Start Velocity once — a default `config.json` is generated in `plugins/punishsystem/`.
3. *(Optional)* Configure the `database` section with your MySQL credentials.
4. Restart or run `/punishreload`.
5. Assign permissions to your staff groups.

---

## Configuration

The plugin is configured entirely through `plugins/punishsystem/config.json`.

### database

```json
"database": {
  "enabled": false,
  "host": "localhost",
  "port": 3306,
  "database": "punishsystem",
  "username": "root",
  "password": "",
  "maxPoolSize": 10,
  "tablePrefix": ""
}
```

Set `"enabled": true` to activate MySQL/MariaDB. Without it the plugin falls back to local file storage.

### network

```json
"network": {
  "name": "YourNetwork",
  "nameFormatted": "§6§lYourNetwork",
  "prefix": "§8[§6PunishSystem§8] §7",
  "websiteUrl": "your-server.net",
  "unbanUrl": "your-server.net/appeal",
  "discordUrl": "discord.your-server.net",
  "unbanMethod": "discord"
}
```

### vpn

```json
"vpn": {
  "enabled": false,
  "kickOnDetect": true,
  "cacheExpireMinutes": 60,
  "whitelistedISPs": []
}
```

### webhooks

Set `"enabled": true` and provide Discord webhook URLs per action:

```json
"webhooks": {
  "enabled": true,
  "ban": "https://discord.com/api/webhooks/...",
  "tempban": "",
  "unban": "",
  "kick": "",
  "warn": "",
  "vpn": ""
}
```

### messages

Every player-facing message is configurable. Placeholders use `{placeholder}` syntax. Example:

```json
"banScreen": "§4§lBanned!\n\n§7Reason: §c{reason}\n§7Ban-ID: §e{id}\n\n§7Appeal at: §a{discord_url}"
```

### punishments (presets)

```json
"punishments": {
  "cheating": {
    "type": "TEMPBAN",
    "duration": "90d",
    "reason": "Cheating / Hacking"
  },
  "racism": {
    "type": "BAN",
    "reason": "Racist behaviour"
  }
}
```

Supported types: `BAN`, `TEMPBAN`, `KICK`, `WARN`, `NETWORK_BAN`

---

## Commands & Permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/ban <player> <reason>` | Permanently ban a player | `punishsystem.ban` |
| `/tempban <player> <duration> <reason>` | Temporarily ban a player | `punishsystem.tempban` |
| `/unban <player>` | Remove an active ban | `punishsystem.unban` |
| `/kick <player> <reason>` | Kick an online player | `punishsystem.kick` |
| `/punish <player> <preset>` | Apply a preset punishment | `punishsystem.punish` |
| `/unpunish <player>` | Remove active punishment | `punishsystem.unpunish` |
| `/warn <player> <reason>` | Warn a player | `punishsystem.warn` |
| `/warnings <player>` | Show active warnings | `punishsystem.check` |
| `/clearwarnings <player>` | Clear all warnings | `punishsystem.clearwarnings` |
| `/history <player>` | Show punishment history | `punishsystem.history` |
| `/check <player>` | Show active punishments | `punishsystem.check` |
| `/lookup <player>` | Full player profile | `punishsystem.lookup` |
| `/punishreload` | Reload config | `punishsystem.reload` |

**Aliases:** `/tban` → `/tempban` · `/unban` → `/unpunish` · `/strafen` → `/history` · `/preload` → `/punishreload`

**Special permissions:**

| Permission | Effect |
|------------|--------|
| `punishsystem.ipban` | Issue IP bans |
| `punishsystem.notify` | Receive staff notifications for every punishment |
| `punishsystem.bypass` | Bypass active bans (useful for admins) |
| `punishsystem.vpn.bypass` | Exempt from VPN / proxy detection |

All permissions can be overridden in `config.json` under the `permissions` section.

---

## Duration Syntax

`30s` · `15m` · `6h` · `7d` · `2w` · `1y` — combinations like `1d12h` are supported.

---

## License

PunishSystem is licensed under the **Mozilla Public License 2.0**.  
See [LICENSE](LICENSE) for full details.

You are free to use, modify, and distribute this plugin — including on private and public Minecraft servers — as long as you comply with the MPL 2.0 terms. Forks published publicly must use a different name. See [TRADEMARKS.md](TRADEMARKS.md) for details.

---

## Issues & Contributions

Found a bug or have a feature request? Open an issue on **[GitHub Issues](../../issues)**.  
Pull requests are welcome!
