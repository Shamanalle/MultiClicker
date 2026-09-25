# Changelog

All notable changes to **MultiClicker** will be documented in this file.

## [1.0.0] - 2026-09-25
### Architecture & Refactoring
- Completely modernized modular architecture on top of Architectury Loom (Fabric & NeoForge targets).
- Removed legacy development artifacts, debug dumps, and stub mixins.
- Migrated profile storage to standard `config/multiclicker-profiles/` with centralized SLF4J/Log4j2 logging.
- Upgraded 3D ESP rendering pipeline to native `RenderType.lines()` and `RenderType.debugQuads()` compatible with Minecraft 1.21.8+.

### Features
- **Combat Clicker**: Independent LMB, RMB, and Jump control with separate Click/Hold modes, speeds, and randomization ranges.
- **Smart Combat & Anti-Cheat**: 1.9+ cooldown-aware attacks, automatic critical hit timing, weapon checking, normal/Gaussian click distribution, miss penalties, jitter simulation, and network lag protection.
- **Survival & Automation**: Auto-Totem, Smart Offhand, Panic Mode, Auto-Eat, Auto-Tool, Mining Block Filters, Auto-Fish, Anti-AFK, Auto-Walk, and Trash Drop.
- **Visuals & HUD**: Real-time HUD (CPS, DPS, KPM, Ping, TPS, Active Modules) with 4-corner positioning and 3D ESP with 8 rendering modes (Box, Filled, Circle, Arrow, Beacon, Glow, Cylinder, Cone) in 10 customizable colors.
- **UI & Configuration**: Modern sidebar navigation interface (v7) with smooth transitions, live search, profiles export/import, and toast notifications.
