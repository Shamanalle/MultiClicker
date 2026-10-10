# Changelog

All notable changes to **MultiClicker** will be documented in this file.

## [2.4.0] - 2026-10-10
### Added
- **Statistics.** A new screen in the menu (▤ *Statistics*) with what the mod did, kept between games:
  the current session, each server, all time and the last 20 sessions. It counts clicks, attacks, kills
  by mob, damage and DPS, catches by kind (fish, treasure, junk) and item, harvested crops, mined blocks,
  food eaten, totems used, deaths, experience, items dropped by the inventory cleaner, hotbar refills and
  anti-AFK actions, with rates per hour.
- **Graphs.** For the live session: real clicks per second against the target over the last minute, and
  a per-minute chart of clicks, kills, damage, catches, harvest, blocks or experience.
- **Exact damage.** Damage is measured from the health the mobs lose after your hits (on 1.19.4 and
  newer, from the server's damage events). Where a server hides the health of mobs, the screen says the
  damage is unknown instead of guessing.
- **Export and reset.** All statistics can be exported to a CSV file next to `stats.json`; reset asks for
  a second click.
- **Session summary.** When the mod is turned off after more than 30 seconds, a line in the chat sums up
  the session (*Notifications → Session summary*).
- **HUD.** The session lines also show damage, catches, harvest, blocks and experience, and optionally
  the rate per hour (*HUD → Per hour*).

## [2.3.0] - 2026-10-10
### Added
- **NeoForge.** A NeoForge jar for every Minecraft version from 1.20.1 to 26.3. It needs no other mods;
  the settings button is in the mod list. NeoForge does not exist for 1.20, and on 1.20.1 the jar runs on
  NeoForged Forge 47.1. All highlight styles work on NeoForge, including 1.21.9.
- **Minecraft 1.20 to 1.20.6** on Fabric.
- **Minecraft 1.19 to 1.19.4** on Fabric and Forge.
- **Forge.** A Forge jar for Minecraft 1.19 to 1.20.1 (for 1.20.1 it is the same file as the NeoForge
  jar, which runs on Forge 1.20.1 as is). On 1.19 to 1.19.3 the filled highlight is drawn additive (brighter where it
  overlaps), and search fields have no hint text before 1.19.3.
- **Notifications.** A message above the hotbar or in the chat, with a chime, when something needs your
  attention while the mod works: the inventory is full, the tool in hand is about to break, the last item in
  hand is used up or broke, auto eat finds no food, a player comes within a set radius, a fish is caught, or
  the mod stopped by itself and why. Each notification can be turned off; repeats are held back for a while.
- **Anti-AFK: switch slot.** Selects the next hotbar slot for a moment and goes back. Vanilla and most
  servers count it as activity even where they ignore swings and head turns. It waits while the clicker,
  a fishing line, an item in use or another module needs the item in hand. On by default.
- **AFK in place preset.** Only anti-AFK, without walking or jumping: sneak, swing and switch slot.

## [2.2.0] - 2026-10-09
### Added
- **Hotkeys.** Every module, each clicker channel (attack, use, jump) and each saved profile can get its
  own key or side mouse button that switches it while playing. Set them with the ⌨ button on a module card,
  the *Hotkey* row of a channel, or in *Profiles*. Profiles and presets never change hotkeys.
- **Click speed in clicks per second.** Each clicker channel can set its speed as an interval in ticks
  (as before) or as a rate from 1 to 20 CPS. Fractions of a tick carry over, so 12 CPS is kept on average.
- **Choice of random delay.** *Even* (as before), *bell curve* (a steady rhythm around the middle) and
  *natural* (mostly short, sometimes a longer pause). Natural is the new default; settings from earlier
  versions keep the even delay.
- **Sharing profiles.** A profile, or the current settings, can be copied as one line of text and pasted
  by someone else. Shared profiles never bring hotkeys and never overwrite an existing profile.
- **Profiles per server.** A profile can load by itself when you join a given server or single player.
- **Hotbar refill** module: a used-up hotbar stack or a broken tool is replaced by the same item from the
  inventory, a stack can be topped up before it runs out, and the tool in hand is swapped for a spare
  before it breaks (a tool with Mending is kept).
- **Auto farm** module: harvests ripe wheat, carrots, potatoes, beetroots, nether wart, cocoa and sweet
  berries around you (or only under the crosshair) and plants them again.
- **Farming** preset: auto farm, hotbar refill and auto eat.
- The HUD shows the loaded profile.
- Unit tests for the click timing, the random delay, profile sharing and the config files; CI runs them
  with every build.

### Changed
- Menu: click a number to type an exact value; changed settings are marked with a dot; a module can be
  reset at once with ↺; <kbd>Tab</kbd> switches the category; the cards slide in when the category changes;
  tips in the footer take turns.
- Profiles screen: wider, with a hotkey, the server binding and copying for each profile, *Paste* to add
  a shared profile, and *Save* turns into *Overwrite* when the name is taken. The loaded profile is marked.
- Loading a profile or a preset while the mod is on restarts the modules, so none keeps a key pressed.
- The config file has format version 3. Older files are converted when they are loaded; a file from a
  newer version is backed up to `multiclicker.json.v<N>.bak` before it is saved over.

## [2.1.1] - 2026-10-01
### Fixed
- The author in the mod metadata (shown in Mod Menu) is now Shamanalle.

### Changed
- The in-game tests pass on every version from 1.21.4 to 26.3 on the CI runners. On 1.21.9 to 1.21.11 the
  world load used to stall because the blurred menu background took the frame time of the software renderer.

## [2.1.0] - 2026-09-30
### Added
- Support for 17 Minecraft versions: 1.21, 1.21.1 to 1.21.11 and 26.1, 26.1.1, 26.1.2, 26.2, 26.3.
  Every version has its own jar (`MultiClicker-fabric-2.1.0+<minecraft version>.jar`).
- CI builds every version and plays the in-game tests on 1.21.4 and newer. 1.21 to 1.21.3 have no
  client test API in Fabric API, so they get a start-up check (the mod loads and every mixin applies).

### Changed
- One source tree for all versions: version specific code sits in small compat classes that are
  layered per Minecraft version.
- Fabric Loader 0.17 or newer is required from Minecraft 1.21.9 on (Fabric API needs it anyway).

### Known limits
- Minecraft 1.21.9: the outline and filled styles of the target highlight are not drawn (Fabric API
  for this version has no world rendering events). The glow style works. 1.21.10 and newer are fine.

## [2.0.1] - 2026-09-29
Everything below was found and checked by playing the mod in a real game (new in-game tests).

### Fixed
- Looting finishing blow never triggered: the client does not know the held weapon's attack
  damage bonus, so the kill was never predicted. The damage is now computed from its parts.
- Toggle sprint / toggle sneak: auto walk flipped sprint on every tick and anti-AFK left the
  player sneaking. Keys in toggle mode now get back exactly the state they had.
- Auto eat opened the chest, door or villager trade in the crosshair (and fed carrots to pigs):
  food is now eaten directly, without interacting with what you look at.
- Auto eat could send an attack with the food already in hand; modules now take turns with the
  hotbar (auto eat, Looting swap, auto tool) and give a slot back only if you did not pick
  another one meanwhile.
- Offhand took the hotbar food auto eat needed; it no longer swaps behind an open screen
  (except an emergency totem) and keeps the torch while auto tool holds a shovel.
- Auto fish only counts catches that were reeled in and keeps waiting when the fish got away.
- Inventory cleaner no longer drops items while you sort your inventory.

### Added
- 11 more languages: Ukrainian, German, French, Spanish, Portuguese (Brazil), Polish, Italian,
  Turkish, Chinese (Simplified), Japanese and Korean. Enchantments, items and mobs are called
  the way the game calls them in each language. The Mod Menu description is translated too.

### Changed
- Auto fish works with the rod in either hand, whatever the other hand holds.
- A setting name that does not fit is shown in full in its tooltip.
- Menu: the title no longer overlaps the categories; the sidebar fits Russian names and turns into
  icons on small screens; sliders shrink so setting names stay readable; module descriptions wrap
  to two lines; search also finds English names in any language.
- The HUD module list no longer shows the mining rules.
- A one-time chat hint shows the menu and toggle keys the first time you join a world.

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
