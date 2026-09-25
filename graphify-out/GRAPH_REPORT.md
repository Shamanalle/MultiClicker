# Graph Report - MultiClicker-1.0.0  (2026-09-25)

## Corpus Check
- Corpus is ~39,212 words - fits in a single context window. You may not need a graph.

## Summary
- 1012 nodes · 2300 edges · 59 communities (45 shown, 14 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 247 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- GUI Options Screen
- GUI Options Screen
- AutoClicker Core Engine
- 3D ESP Target Rendering
- Profile Management
- Core Event Bus
- Smart Combat Engine
- Anti-AFK System
- Configuration Storage
- Trash Item Dropper
- Ping & Latency Tracking
- Configuration Storage
- Configuration Storage
- Game Engine Mixins
- Combat Damage & DPS Stats
- String List Editor UI
- Auto-Totem Offhand Switcher
- Auto-Eat System
- Combat Damage & DPS Stats
- Help Documentation Screen
- Auto-Totem Offhand Switcher
- Auto-Walk Navigation
- HUD Overlay & Info Display
- Subsystem: Booleansetting Booleansetting
- Subsystem: Enumsetting Cycle
- Anti-Cheat Heuristics
- GUI Options Screen
- Auto-Fish Automation
- Modern UI Controls
- Smart Offhand Rotator
- HUD Overlay & Info Display
- Game Engine Mixins
- AutoClicker Core Engine
- Auto-Fish Automation
- HUD Overlay & Info Display
- Profile Management
- HUD Overlay & Info Display
- Subsystem: Module Category
- Auto-Walk Navigation
- HUD Overlay & Info Display
- Panic Mode & Fail-Safe
- Mob Target Filtering
- Profile Management
- Subsystem: Floatsetting Floatsetting
- Network Guard & Protection
- Mining & Block Filtering
- ModMenu API Support
- AutoClicker Core Engine
- Auto-Eat System
- Auto-Fish Automation
- Subsystem: Autotoolmodule Enchantmenthelper
- HUD Overlay & Info Display
- Mob Target Filtering
- Subsystem: Gradlew Entry

## God Nodes (most connected - your core abstractions)
1. `Setting` - 71 edges
2. `CombatClickerModule` - 65 edges
3. `McWidget` - 59 edges
4. `Module` - 58 edges
5. `Language` - 50 edges
6. `OptionsScreen` - 44 edges
7. `AutoClicker` - 40 edges
8. `Category` - 28 edges
9. `ModuleManager` - 28 edges
10. `BooleanSetting` - 26 edges

## Surprising Connections (you probably didn't know these)
- `Adaptive Mining System` --references--> `MiningModule`  [INFERRED]
  README.md → common/src/main/java/pro/mikey/autoclicker/modules/world/MiningModule.java
- `Panic Mode Emergency Trigger` --references--> `PanicModeModule`  [INFERRED]
  README.md → common/src/main/java/pro/mikey/autoclicker/modules/world/PanicModeModule.java
- `Live Combat Statistics` --references--> `CombatStats`  [INFERRED]
  README.md → common/src/main/java/pro/mikey/autoclicker/CombatStats.java
- `Sidebar Configuration UI (v7)` --references--> `OptionsScreen`  [INFERRED]
  README.md → common/src/main/java/pro/mikey/autoclicker/OptionsScreen.java
- `Centralized Profile Storage & Logging` --references--> `ProfileManager`  [INFERRED]
  CHANGELOG.md → common/src/main/java/pro/mikey/autoclicker/ProfileManager.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Anti-Cheat Humanization Architecture** — readme_gaussian_click_distribution, readme_jitter_miss_penalties, readme_lag_compensation, readme_anticheat_guard [INFERRED 0.85]
- **Survival Fail-Safe Protection Suite** — readme_panic_mode, readme_auto_totem, readme_smart_offhand, readme_auto_eat [INFERRED 0.85]
- **MultiClicker 1.0.0 Architectural Modernization** — changelog_v1_0_0, changelog_architectury_loom_migration, changelog_profile_storage_centralization, changelog_native_render_upgrade [EXTRACTED 1.00]

## Communities (59 total, 14 thin omitted)

### Community 0 - "GUI Options Screen"
Cohesion: 0.07
Nodes (14): Override, McSlider, Override, McToggle, Override, McWidget, NarrationPriority, net.minecraft.client.gui.components.events.GuiEventListener (+6 more)

### Community 1 - "GUI Options Screen"
Cohesion: 0.08
Nodes (11): Override, OptionsScreen, Override, McNavPanel, NavCategory, NavItem, Override, McScrollView (+3 more)

### Community 2 - "AutoClicker Core Engine"
Cohesion: 0.07
Nodes (14): DeltaTracker, GuiGraphics, PoseStack, CombatClickerModule, Item, Override, LootingSwapState, OFF (+6 more)

### Community 3 - "3D ESP Target Rendering"
Cohesion: 0.06
Nodes (32): aabb, clientplayconnectionevents, clienttickevent, com.mojang.blaze3d.vertex.VertexConsumer, Notification, NotificationRenderer, dist, entity (+24 more)

### Community 4 - "Profile Management"
Cohesion: 0.04
Nodes (49): Language, GUI_ACTIVE, GUI_AFK_HOLOGRAM, GUI_ANTI_AFK, GUI_ANTI_AFK_INTERVAL, GUI_ANTI_AFK_MODE, GUI_ATTACK, GUI_AUTO_EAT (+41 more)

### Community 5 - "Core Event Bus"
Cohesion: 0.07
Nodes (27): AttackEvent, CameraRotateEvent, EntityDamagedEvent, EntityDeathEvent, EventBus, EventHandler, FishBiteEvent, FramePickEvent (+19 more)

### Community 6 - "Smart Combat Engine"
Cohesion: 0.06
Nodes (33): Architectury Loom Modernization, MultiClicker Changelog, MultiClicker 1.0.0 Release, AttackMethod, DIRECT, LEGIT, Override, LowHpAction (+25 more)

### Community 7 - "Anti-AFK System"
Cohesion: 0.08
Nodes (16): AntiAfkModule, Override, State, CHATTING, HOTBAR_CHANGE, IDLE, LOOKING_DRIFT, LOOKING_RETURN (+8 more)

### Community 8 - "Configuration Storage"
Cohesion: 0.08
Nodes (8): ConfigManager, DeltaTracker, GuiGraphics, PoseStack, SuppressWarnings, ModuleManager, Preset, PresetManager

### Community 9 - "Trash Item Dropper"
Cohesion: 0.09
Nodes (17): builtinregistries, clicktype, DropMode, BLACKLIST, WHITELIST, Override, toString(), TrashDropModule (+9 more)

### Community 10 - "Ping & Latency Tracking"
Cohesion: 0.15
Nodes (10): ClientPing, MixinClientPacketListener, MixinKeepAlive, MixinPingPacketListener, net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl, net.minecraft.client.multiplayer.ClientPacketListener, net.minecraft.network.protocol.common.ClientboundKeepAlivePacket, net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket (+2 more)

### Community 11 - "Configuration Storage"
Cohesion: 0.13
Nodes (4): SuppressWarnings, Setting, Entry, JsonElement

### Community 12 - "Configuration Storage"
Cohesion: 0.16
Nodes (12): chatformatting, AutoClicker, files, Gson, io, logmanager, net.minecraft.client.KeyMapping, org.apache.logging.log4j.Logger (+4 more)

### Community 13 - "Game Engine Mixins"
Cohesion: 0.23
Nodes (12): at, MixinArmorStand, MixinGameRenderer, MixinLivingEntity, MixinMouseHandler, entityhitresult, livingentity, net.minecraft.client.MouseHandler (+4 more)

### Community 14 - "Combat Damage & DPS Stats"
Cohesion: 0.19
Nodes (3): CombatStats, LivingEntity, Live Combat Statistics

### Community 15 - "String List Editor UI"
Cohesion: 0.24
Nodes (3): StringListSetting, Override, McStringListEditor

### Community 16 - "Auto-Totem Offhand Switcher"
Cohesion: 0.21
Nodes (7): InventoryUtils, datacomponents, foodproperties, items, net.minecraft.world.entity.player.Player, net.minecraft.world.item.Item, set

### Community 17 - "Auto-Eat System"
Cohesion: 0.21
Nodes (5): AutoEatModule, Item, Override, net.minecraft.world.item.ItemStack, Auto-Eat Sustenance Module

### Community 18 - "Combat Damage & DPS Stats"
Cohesion: 0.35
Nodes (3): MixinMinecraft, org.spongepowered.asm.mixin.injection.callback.CallbackInfo, org.spongepowered.asm.mixin.injection.Inject

### Community 19 - "Help Documentation Screen"
Cohesion: 0.23
Nodes (4): Entry, HelpScreen, Override, net.minecraft.client.gui.screens.Screen

### Community 20 - "Auto-Totem Offhand Switcher"
Cohesion: 0.17
Nodes (8): AutoTotemModule, Item, Override, State, DONE, IDLE, PLACE_OFFHAND, Auto-Totem Offhand Replenisher

### Community 22 - "HUD Overlay & Info Display"
Cohesion: 0.19
Nodes (9): ClientTPS, getText(), toString(), comparator, i18n, mth, net.minecraft.network.chat.Component, net.minecraft.network.chat.MutableComponent (+1 more)

### Community 23 - "Subsystem: Booleansetting Booleansetting"
Cohesion: 0.18
Nodes (3): BooleanSetting, IntSetting, Override

### Community 24 - "Subsystem: Enumsetting Cycle"
Cohesion: 0.23
Nodes (3): EnumSetting, Override, McDropdown

### Community 25 - "Anti-Cheat Heuristics"
Cohesion: 0.21
Nodes (8): AntiCheatModule, Override, PollingRate, HZ_1000, HZ_125, HZ_250, HZ_500, toString()

### Community 26 - "GUI Options Screen"
Cohesion: 0.26
Nodes (7): arraylist, automation, combat, component, list, ui, world

### Community 27 - "Auto-Fish Automation"
Cohesion: 0.17
Nodes (6): consumer, copyonwritearraylist, linkedhashmap, map, random, util

### Community 29 - "Smart Offhand Rotator"
Cohesion: 0.27
Nodes (4): Item, Override, SmartOffhandModule, Smart Offhand Situational Swapper

### Community 30 - "HUD Overlay & Info Display"
Cohesion: 0.27
Nodes (5): Native Vertex-Buffer Render Upgrade, HudModule, Override, 3D ESP Target Highlighter, In-Game HUD Overlay

### Community 31 - "Game Engine Mixins"
Cohesion: 0.29
Nodes (5): InventoryAccessor, MixinMultiPlayerGameMode, net.minecraft.client.multiplayer.MultiPlayerGameMode, net.minecraft.world.entity.player.Inventory, org.spongepowered.asm.mixin.gen.Accessor

### Community 32 - "AutoClicker Core Engine"
Cohesion: 0.18
Nodes (10): ClickMode, CLICK, HOLD, toString(), glfw, hashmap, hitresult, inputconstants (+2 more)

### Community 33 - "Auto-Fish Automation"
Cohesion: 0.31
Nodes (3): AutoFishModule, Override, Auto-Fish Autonomous Fishing

### Community 34 - "HUD Overlay & Info Display"
Cohesion: 0.18
Nodes (11): EspColor, AQUA, BLUE, CYAN, GREEN, ORANGE, PINK, PURPLE (+3 more)

### Community 35 - "Profile Management"
Cohesion: 0.22
Nodes (8): collectors, MixinFishingHook, file, ioexception, minecraft, net.minecraft.world.entity.projectile.FishingHook, shadow, stream

### Community 37 - "Subsystem: Module Category"
Cohesion: 0.20
Nodes (7): Category, AUTOMATION, CLICKER, COMBAT, PROTECTION, RENDER, WORLD

### Community 38 - "Auto-Walk Navigation"
Cohesion: 0.33
Nodes (3): AutoWalkModule, Override, Auto-Walk Obstacle Navigation

### Community 39 - "HUD Overlay & Info Display"
Cohesion: 0.20
Nodes (10): EspMode, ARROW, BEACON, BOX, CIRCLE, CONE, CYLINDER, FILLED (+2 more)

### Community 41 - "Mob Target Filtering"
Cohesion: 0.33
Nodes (3): Override, MobFilterModule, Target Entity Filters

### Community 46 - "ModMenu API Support"
Cohesion: 0.47
Nodes (4): com.terraformersmc.modmenu.api.ConfigScreenFactory, com.terraformersmc.modmenu.api.ModMenuApi, Override, ModMenuAPIImpl

### Community 48 - "Auto-Eat System"
Cohesion: 0.33
Nodes (6): State, EATING, EATING_OFFHAND, IDLE, RETURN_SWAP, SWAPPING

### Community 49 - "Auto-Fish Automation"
Cohesion: 0.33
Nodes (6): State, BITE_DETECTED, IDLE, RECASTING, REELING, WAITING_BITE

### Community 50 - "Subsystem: Autotoolmodule Enchantmenthelper"
Cohesion: 0.40
Nodes (4): enchantmenthelper, enchantments, itemstack, registries

### Community 51 - "HUD Overlay & Info Display"
Cohesion: 0.40
Nodes (5): HudPosition, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT

### Community 52 - "Mob Target Filtering"
Cohesion: 0.50
Nodes (3): animal, monster, npc

### Community 53 - "Subsystem: Gradlew Entry"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **145 isolated node(s):** `HUD_HOLDING`, `MSG_HOLDING_KEYS`, `MSG_RELEASED_KEYS`, `GUI_SPEED`, `GUI_ACTIVE` (+140 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 286 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Module` connect `Auto-Walk Navigation` to `AutoClicker Core Engine`, `Core Event Bus`, `Smart Combat Engine`, `Anti-AFK System`, `Configuration Storage`, `Trash Item Dropper`, `Configuration Storage`, `Auto-Totem Offhand Switcher`, `Auto-Eat System`, `Auto-Totem Offhand Switcher`, `HUD Overlay & Info Display`, `Anti-Cheat Heuristics`, `GUI Options Screen`, `Auto-Fish Automation`, `Smart Offhand Rotator`, `HUD Overlay & Info Display`, `AutoClicker Core Engine`, `Auto-Fish Automation`, `Subsystem: Module Category`, `Auto-Walk Navigation`, `Panic Mode & Fail-Safe`, `Mob Target Filtering`, `Network Guard & Protection`, `Mining & Block Filtering`, `Subsystem: Autotoolmodule Enchantmenthelper`, `Mob Target Filtering`?**
  _High betweenness centrality (0.174) - this node is a cross-community bridge._
- **Why does `Language` connect `Profile Management` to `HUD Overlay & Info Display`?**
  _High betweenness centrality (0.141) - this node is a cross-community bridge._
- **Why does `CombatClickerModule` connect `AutoClicker Core Engine` to `AutoClicker Core Engine`, `3D ESP Target Rendering`, `Smart Combat Engine`, `Trash Item Dropper`, `Configuration Storage`, `Game Engine Mixins`, `Network Guard & Protection`, `AutoClicker Core Engine`, `Mining & Block Filtering`, `Auto-Eat System`, `Auto-Walk Navigation`, `HUD Overlay & Info Display`, `Subsystem: Booleansetting Booleansetting`, `Subsystem: Enumsetting Cycle`, `Anti-Cheat Heuristics`, `Auto-Fish Automation`, `Game Engine Mixins`?**
  _High betweenness centrality (0.131) - this node is a cross-community bridge._
- **What connects `HUD_HOLDING`, `MSG_HOLDING_KEYS`, `MSG_RELEASED_KEYS` to the rest of the system?**
  _145 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `GUI Options Screen` be split into smaller, more focused modules?**
  _Cohesion score 0.06698564593301436 - nodes in this community are weakly interconnected._
- **Should `GUI Options Screen` be split into smaller, more focused modules?**
  _Cohesion score 0.07894736842105263 - nodes in this community are weakly interconnected._
- **Should `AutoClicker Core Engine` be split into smaller, more focused modules?**
  _Cohesion score 0.06753246753246753 - nodes in this community are weakly interconnected._