# Changelog

All notable changes to PunishSystem will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] — 2026-06-01

### Added
- Permanent ban (`/ban`) with UUID tracking and multi-account propagation
- Temporary ban (`/tempban`) with flexible duration syntax (e.g. `30m`, `7d`, `1y`)
- IP ban support (`IPBAN` type) with automatic detection on login
- Network ban (`/punish ... NETWORK_BAN`) — permanent server-wide ban with appeal screen
- Kick command (`/kick`) with configurable screen message
- Unpunish command (`/unpunish`, `/unban`) to lift active bans
- **Warn system** — `/warn <player> <reason>` issues a formal in-game warning; `/warnings <player>` lists active warnings; `/clearwarnings <player>` removes them all
- History command (`/history`) showing up to 50 past punishments per player
- Check command (`/check`) showing active punishments for a player
- Lookup command (`/lookup`) with full player profile: UUID, IPs, linked accounts, ban history
- Predefined punishment system (`/punish <player> <key>`) configured via `config.json`; supported types: `BAN`, `TEMPBAN`, `KICK`, `WARN`, `NETWORK_BAN`
- Multi-account detection: bans automatically apply to linked accounts sharing the same IP
- VPN / proxy detection via ip-api.com with configurable kick and ISP whitelist
- Discord webhook support for ban, tempban, unban, kick, IP ban, warn, and VPN events
- Staff notification broadcast for all punishments to players with `punishsystem.notify`
- Fully configurable messages, prefix, network info, and permissions via `config.json`
- MySQL/MariaDB backend with HikariCP connection pooling
- File storage fallback — works without a database; enable MySQL via `"database": { "enabled": true }` in `config.json`
- Mojang API fallback for resolving offline player UUIDs
- `/punishreload` (`/preload`) to hot-reload the configuration without restart
- LabyMod Protocol integration
- bStats metrics (plugin ID 31736)
- Velocity 3.x support, Java 21
- Mozilla Public License 2.0
