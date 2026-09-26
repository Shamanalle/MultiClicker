# Changelog

All notable changes to **MultiClicker** will be documented in this file.

## [2.0.0] - 2026-09-25
Full rewrite of the mod.

### Changed
- New architecture: modules with typed, translatable settings and a single versioned config file
  (`config/multiclicker.json`, written atomically). Old `multiclicker-settings.json` is not migrated.
- Clicks go through vanilla key mappings: the attack cooldown, block breaking and server sync are
  exactly vanilla, and keys the player physically holds are never released by the mod.
- Settings are loaded when the game starts instead of on the first server join.
- Completely redesigned settings menu: category sidebar, module cards, instant search, tooltips,
  animated controls, right-click reset, list editor with item icons, profiles and presets screen.
- Redesigned HUD panel.
- Full English and Russian localization (the Spanish file was incomplete and has been removed).

### Added
- Mining with a held attack key keeps working when the game window is in the background.
- "Work in background" option: the game does not pause on focus loss while the mod is active.
- Target filter: ignore named mobs, pets and invisible entities; armor stands and other non-mob
  entities are skipped unless enabled.
- Auto fish: auto cast, recast timeout, rod protection, catch limit.
- Auto tool prefers tools that actually drop the block; tool durability protection.
- Safety module: stop or disconnect on low health, damage or a nearby player.
- Session time limit; profiles and built-in presets (mob farm, mining, fishing).
- Looting finishing blow: predicts whether the next hit kills the target (attack attribute,
  Strength/Weakness, Sharpness/Smite/Bane, target armor, toughness, Resistance, absorption),
  selects the best Looting weapon from the hotbar, waits until it is charged, hits and switches back.
- Camera lock: mouse movement does not turn the camera while the mod is active.
- Offhand module (replaces auto totem): totem > food > torch with a pickaxe > shield, with
  hysteresis, no swaps while an item is in use (except an emergency totem), never takes the held item.

### Removed
- Settings that had no real effect: "GCD patch", polling rate, skip chance.
- Direct packet attacks and attacking entities that already left the crosshair.
- Anti-AFK chat spam, the unused NeoForge module and generated analysis files.

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
