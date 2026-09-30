<div align="center">

<img src="common/src/main/resources/assets/multiclicker/icon.png" width="96" alt="MultiClicker icon">

# MultiClicker

**Auto clicker for Fabric with helpers for AFK farms, mining and fishing.**

[![Minecraft 1.21.8](https://img.shields.io/badge/Minecraft-1.21.8-62b47a)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/loader-Fabric-dbd0b4)](https://fabricmc.net/)
[![Release](https://img.shields.io/github/v/release/Shamanalle/MultiClicker?color=4c8bf5)](https://github.com/Shamanalle/MultiClicker/releases/latest)
[![In-game tests](https://github.com/Shamanalle/MultiClicker/actions/workflows/gametest.yml/badge.svg)](https://github.com/Shamanalle/MultiClicker/actions/workflows/gametest.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE)

**English** · [Русский](README.ru.md)

![A mob farm with MultiClicker: the status panel and the highlighted target](docs/images/en/hud.png)

</div>

Auto clicker mod for Minecraft 1.21.8 (Fabric). Clicks the attack, use and jump keys at a rhythm you set.
Includes modules for AFK farming, mining and fishing: auto eat, auto fish, auto tool, offhand, safety
stop, anti-AFK and others.

## Quick start

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.8**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and
   [`MultiClicker-fabric-<version>.jar`](https://github.com/Shamanalle/MultiClicker/releases/latest)
   into the `mods` folder. Optional: [Mod Menu](https://modrinth.com/mod/modmenu) adds a settings
   button to the mod list.
3. Join a world and press <kbd>O</kbd> to open the menu, <kbd>I</kbd> to turn the mod on or off.

Pick a ready-made setup under **Profiles → Presets**, then press <kbd>I</kbd>:

| Preset | What it sets up |
|:--|:--|
| **Mob farm** | Attacks on full charge with a small random delay. Passive mobs, pets and named mobs are left alone. Auto eat and anti-AFK are on. |
| **Mining** | The attack key is held, blocks are broken, auto tool picks the pickaxe, the clicker stops when the inventory is full. Auto eat is on. |
| **Fishing** | The clicker is off, auto fish reels in and casts again. Auto eat and anti-AFK are on. |

Your own settings can be saved as profiles.

> Many multiplayer servers don't allow auto clickers. Check the rules before using the mod online.

## Features

Every option has a tooltip in the menu. Right-click a setting to reset it.

### Auto clicker
- **Three channels: attack (LMB), use (RMB) and jump.** Each one can *click* or *hold* the key and has
  its own interval, hold time, pause and random delay.
- **Wait for full charge.** Attacks only when the weapon is charged, for full damage. Turn it off for
  1.8-style PvP.
- **Targets.** Only allowed creatures, creatures and blocks (mining), or anything, even into the air.
- **Pause while using items.** No attacks while you eat, drink, block or draw a bow.
- **Looting finishing blow.** Predicts whether the next hit kills. If so, it switches to the hotbar
  weapon with the best Looting, waits for it to charge, hits and switches back.
- **Start delay**, **attack limit** and **time limit**: the mod turns itself off when they run out.
- **Work in background.** The game keeps running when its window loses focus.
- **Lock camera.** Mouse movement doesn't turn the camera while the mod is on.
- Keys you hold yourself are not released. The clicker pauses while a screen is open.

### Target filter
Players, hostile, neutral and passive mobs and other entities (boats, minecarts, armor stands, end
crystals) are switched separately. On top of that: ignore babies, named mobs, pets and invisible
mobs.

### Survival
- **Safety.** Stops the mod, or leaves the server, when your health drops to a threshold, when you
  take any damage, or when another player comes within a set distance.
- **Offhand.** Keeps one item in the offhand, by priority: a totem at low health, food when
  you're hungry, a torch while you hold a pickaxe, otherwise a shield.
- **Auto eat.** Eats the best safe food from the hotbar, then returns to the previous slot. It can skip harmful
  food (rotten flesh, pufferfish, suspicious stew…) and golden food. It never opens the chest, door or
  villager you look at.

### Automation
- **Auto fish.** Reels in on a bite after a reaction time you set, casts again, recasts if nothing
  bites for too long, stops when the rod is about to break or after a number of catches. The rod can
  be in either hand.
- **Anti-AFK.** Jump, sneak, swing, look around or step, at random intervals between a minimum and a
  maximum. Each action can be turned off.
- **Auto walk.** Walks forward, optionally sprinting and jumping over obstacles.
- **Inventory cleaner.** Throws out the items from your junk list, one stack at a time. It never
  touches the hotbar unless you allow it.

### Mining
- **Mining rules.** A whitelist or blacklist of blocks the clicker may break, edited in a list editor
  with item icons. It can stop when the inventory is full and protect the held tool from breaking.
- **Auto tool.** Picks the fastest hotbar tool for the block, taking Efficiency into account, skips
  nearly broken tools and switches back afterwards.

### Interface
- **HUD.** A status panel in any corner with the scale you choose: active channels, clicks per second,
  attacks, kills, session time, ping, server TPS, FPS and the list of active modules. Every line can be
  hidden.
- **Target highlight.** The creature about to be hit is outlined, filled or glowing, in a color you
  pick.
- **Menu.** Categories, module cards, search (also by English names), tooltips and a list editor with
  item icons. Fits the window size; on small screens the sidebar turns into icons.
- **Feedback.** Accent color, a sound and a message above the hotbar when the mod turns on or off.
- **Profiles.** Save your settings under a name and load them later.

## Screenshots

| | |
|:--:|:--:|
| ![Clicker settings](docs/images/en/menu_clicker.png) | ![Survival modules](docs/images/en/menu_survival.png) |
| Settings: every module is a card, with a tooltip for every option | Survival: safety, offhand and auto eat |
| ![Presets and profiles](docs/images/en/profiles.png) | ![Block list editor](docs/images/en/list_editor.png) |
| Ready-made presets and your own profiles | List editor: item icons, add the held item or the block you look at |

## Controls

| Key | Action |
|:--:|:--|
| <kbd>I</kbd> | Turn the mod on / off |
| <kbd>O</kbd> | Open the settings |

Rebind them under *Options → Controls → Key Binds → MultiClicker*. In the menu, start typing to
search, and use the mouse wheel or the arrow keys to fine-tune sliders.

## Languages

English, Русский, Українська, Deutsch, Français, Español, Português (Brasil), Polski, Italiano,
Türkçe, 简体中文, 日本語, 한국어. Items, enchantments and mobs use the game's own names in each
language.

## Configuration

Settings are saved in `config/multiclicker.json` and profiles in `config/multiclicker/profiles/`.

## Building

You need JDK 21.

```bash
./gradlew build                       # → fabric/build/libs/MultiClicker-fabric-<version>.jar
./gradlew :fabric:runClientGameTest   # starts the game and plays every scenario
```

- `common/` holds the mod itself: modules, settings, the menu and the config.
- `fabric/` holds the Fabric entry point.
- `fabric/src/gametest/` holds the in-game tests, which are not part of the release jar.

## License

[MIT](LICENSE).
