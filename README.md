# MultiClicker

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8%20--%2026.3-blue.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Loader%20%3E%3D%200.16.0-green.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%20%7C%2025-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A modern, highly modular and feature-rich automation and auto-clicker mod supporting **Minecraft 1.21.8 up to 26.3 (Wilderness Bound)** for Fabric.

Designed with a clean component-based architecture, advanced humanization algorithms to bypass strict anti-cheat heuristics, survival fail-safes, and a sleek in-game GUI with real-time stats and 3D ESP target tracking.

---

## 📦 Supported Versions & Downloads

All official release builds are available on the [**GitHub Releases**](https://github.com/Shamanalle/MultiClicker/releases) page. Each Minecraft version has its own dedicated branch and optimized build:

| Minecraft Version | Update Name | Branch | Java | Download Artifact |
|:---:|:---|:---:|:---:|:---|
| **1.21.8** | *Baseline Release* | [`main`](https://github.com/Shamanalle/MultiClicker/tree/main) | Java 21 | [`MultiClicker-1.0.0+1.21.8.jar`](https://github.com/Shamanalle/MultiClicker/releases/download/v1.0.0/MultiClicker-1.0.0+1.21.8.jar) |
| **1.21.9** | *The Copper Age* | [`release/1.21.9`](https://github.com/Shamanalle/MultiClicker/tree/release/1.21.9) | Java 21 | [`MultiClicker-1.0.0+1.21.9.jar`](https://github.com/Shamanalle/MultiClicker/releases/download/v1.0.0/MultiClicker-1.0.0+1.21.9.jar) |
| **1.21.11** | *Mounts of Mayhem* | [`release/1.21.11`](https://github.com/Shamanalle/MultiClicker/tree/release/1.21.11) | Java 21 | [`MultiClicker-1.0.0+1.21.11.jar`](https://github.com/Shamanalle/MultiClicker/releases/download/v1.0.0/MultiClicker-1.0.0+1.21.11.jar) |
| **26.1** | *Tiny Takeover* | [`release/26.1`](https://github.com/Shamanalle/MultiClicker/tree/release/26.1) | Java 25 | [`MultiClicker-1.0.0+26.1.jar`](https://github.com/Shamanalle/MultiClicker/releases/download/v1.0.0/MultiClicker-1.0.0+26.1.jar) |
| **26.2** | *Chaos Cubed* | [`release/26.2`](https://github.com/Shamanalle/MultiClicker/tree/release/26.2) | Java 25 | [`MultiClicker-1.0.0+26.2.jar`](https://github.com/Shamanalle/MultiClicker/releases/download/v1.0.0/MultiClicker-1.0.0+26.2.jar) |
| **26.3** | *Wilderness Bound* | [`release/26.3`](https://github.com/Shamanalle/MultiClicker/tree/release/26.3) | Java 25 | [`MultiClicker-1.0.0+26.3.jar`](https://github.com/Shamanalle/MultiClicker/releases/download/v1.0.0/MultiClicker-1.0.0+26.3.jar) |

---

## 🌟 Key Features

### 🖱️ 1. Auto Clicker Engine (`CombatClickerModule`)
- **Independent Channels**: Fully separated settings for **Left Click (LMB)**, **Right Click (RMB)**, and **Jump**.
- **Dual Operating Modes**:
  - `Click`: High-frequency spamming with customizable per-tick delays (up to 20 CPS at 0 delay).
  - `Hold`: Continuous button holding for $N$ ticks (ideal for shields, bows, eating, or charged attacks).
- **Per-Channel Randomization**: Configurable delay ranges to avoid uniform, detectable click rhythms.
- **Armor Stand Protection**: Prevents accidental destruction of armor stands, item frames, and decorative entities during latency spikes.
- **Click Limit & Activation Delay**: Set a hard cap on clicks or a grace period after activation.

### ⚔️ 2. Smart Combat & Statistics
- **1.9+ Attack Cooldown Sync**: Automatically synchronizes strikes with attack charge to maximize DPS.
- **Auto Criticals**: Optimizes attack timing during the player's downward fall phase for 100% critical hit rate.
- **Weapon Check**: Automatically restricts auto-clicker activation to melee weapons (swords, axes, maces, tridents).
- **Entity Filters**: Fine-grained target filtering for players, hostile mobs, neutral/passive animals, teammates, and invisible entities.
- **Live Combat Stats**: Real-time **DPS** (15s rolling window), **KPM** (kills per minute), and total combat duration.

### 🛡️ 3. Anti-Cheat & Network Guard
- **Gaussian Click Distribution**: Simulates human click timings using normal distribution rather than predictable linear randomization.
- **Human Jitter & Miss Penalties**: Dynamically reduces CPS when looking away from targets and emulates micro-movements.
- **Network Lag Compensation**: Real-time ping tracking and server TPS monitoring.
- **Lag Spike Pause & Safe Disconnect**: Automatically pauses clicks during network freezes and safely disconnects without stuck keys when entering critical danger.

### ⛏️ 4. World & Mining
- **Adaptive Mining Mode**: Automatically distinguishes between attacking entities and breaking blocks, switching to continuous break logic.
- **Block Whitelist / Blacklist**: Whitelist high-value ores or blacklist unintended blocks.
- **Auto-Tool**: Instantly switches to the fastest hotbar tool for the target block with full server packet sync (`ServerboundSetCarriedItemPacket`).

### ❤️ 5. Survival & Protection
- **Panic Mode**: Emergency triggers when health drops below a configurable threshold (auto-disconnect or safe retreat).
- **Auto-Totem**: Instantly slots a Totem of Undying from your inventory into the offhand when health is critically low with network debounce.
- **Smart Offhand**: Intelligently swaps shields, golden apples, totems, and fireworks based on combat situation.
- **Auto-Eat**: Automatically consumes food from hotbar/inventory when hunger or health drops with packet synchronization.

### 🤖 6. Full Automation
- **Auto-Fish**: Autonomous fishing with splash-particle and bobber movement detection.
- **Anti-AFK**: Evades server AFK kicks using pseudo-random micro-movements, pitch/yaw rotation, and jumps.
- **Auto-Walk**: Continuous hands-free walking with obstacle jump assist and auto-pause when screens are open.
- **Trash Drop**: Configurable list of unwanted items to automatically discard from inventory.

### 📊 7. Visuals, HUD & 3D ESP
- **In-Game HUD Overlay**: Clean multi-corner display (Top-Left, Top-Right, Bottom-Left, Bottom-Right) showing active state, CPS, DPS, KPM, Ping, TPS, and active modules.
- **3D ESP Target Highlighter**:
  - 8 Render Modes: `Box`, `Filled Box`, `Circle`, `Arrow`, `Beacon`, `Cylinder`, `Cone`, and vanilla `Glow`.
  - 10 Palette Colors: Green, Red, Blue, Cyan, Yellow, Orange, Pink, White, Purple, Aqua.
  - Native vertex-buffer rendering (`RenderType.lines()`, `RenderType.debugQuads()`) compatible with modern Minecraft graphics pipelines.
- **Modern Options UI**: Version 7 sidebar navigation with real-time setting search, preset manager, profile export/import, and toast notifications.

---

## ⌨️ Controls & Keybindings

| Key | Action |
|:---:|:---|
| <kbd>I</kbd> | **Toggle MultiClicker** on/off |
| <kbd>O</kbd> | **Open GUI** settings & module configuration |
| <kbd>ESC</kbd> | Close configuration interface and apply changes |

*(All keybindings can be customized in the vanilla Minecraft Controls menu).*

---

## 📦 Requirements & Installation

1. Install the appropriate **Minecraft** version (from 1.21.8 up to 26.3).
2. Install **Fabric Loader** (version `0.16.0` or newer).
3. Download the matching **Fabric API** for your Minecraft version and place it into your `.minecraft/mods` folder.
4. Download the corresponding `MultiClicker-1.0.0+<version>.jar` from [**Releases**](https://github.com/Shamanalle/MultiClicker/releases) and place it into `.minecraft/mods`.
5. Launch the game and press <kbd>O</kbd> to configure your settings!

---

## 🛠️ Building from Source

### Prerequisites:
- **JDK 21** for branches `main` (1.21.8), `release/1.21.9`, and `release/1.21.11`.
- **JDK 25** for branches `release/26.1`, `release/26.2`, and `release/26.3`.

### Build Steps:
```bash
# 1. Clone the repository
git clone https://github.com/Shamanalle/MultiClicker.git
cd MultiClicker

# 2. Switch to the desired Minecraft version branch
git checkout release/26.3   # or: main, release/1.21.9, release/1.21.11, release/26.1, release/26.2

# 3. Build with Gradle wrapper
./gradlew build
```

Compiled `.jar` binaries will be located in:
- `fabric/build/libs/MultiClicker-fabric-1.0.0.jar`

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
