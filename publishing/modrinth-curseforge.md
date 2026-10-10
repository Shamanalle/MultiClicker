# MultiClicker: texts for Modrinth and CurseForge

Everything below is ready to paste. Check the category names in the upload form: both sites rename them
from time to time.

Contents: 1. Project settings · 2. English description · 3. Russian description · 4. Short texts for
the version fields · 5. Upload checklist

---

## 1. Project settings

| Field | Value |
|:--|:--|
| Name | `MultiClicker` |
| Slug / URL | `multiclicker` (if it is taken: `multiclicker-fabric`) |
| Summary (Modrinth, 256 characters max; CurseForge, 250 max) | see below |
| Loaders | Fabric, NeoForge, Forge |
| Environment | Client: required. Server: unsupported (the mod is client-only) |
| License | MIT |
| Source code | https://github.com/Shamanalle/MultiClicker |
| Issues | https://github.com/Shamanalle/MultiClicker/issues |
| Dependencies | Fabric: Fabric API required, Mod Menu optional. NeoForge and Forge: none |
| Modrinth categories (up to 3) | Utility, Game mechanics |
| CurseForge category | Utility & QoL (if the list has no such entry: Miscellaneous) |

**Summary, English** (209 characters):

> Auto clicker (autoclicker) and AFK helper for Fabric, NeoForge and Forge: auto attack, auto eat, auto fish, auto farm, auto tool, hotbar refill, anti-AFK and hotkeys for mob farms, mining, farming and fishing.

**Summary, Russian** (for the Russian description or a translation of the project):

> Автокликер для Fabric, NeoForge и Forge с авто-едой, авто-рыбалкой, автофермой, авто-инструментом, пополнением хотбара, второй рукой, аварийной остановкой, анти-АФК и горячими клавишами.

Why this wording: both sites search the name and the summary (Modrinth has no free-form tags, CurseForge has only fixed categories). The words people type
(`auto clicker`, `autoclicker`, `auto eat`, `auto fish`, `auto farm`, `afk`) are in the summary and the first
paragraph, so the name can stay `MultiClicker`.

---

## 2. English description

Markdown, works on Modrinth. For CurseForge paste it into the Markdown editor (or switch the editor
to Markdown first). Upload the screenshots to the gallery on both sites; the links below point to the
repository images, so they work on Modrinth as they are.

````markdown
An auto clicker and AFK helper for Minecraft: it clicks attack, use and jump at the rhythm you set, and its modules take care of AFK farms, mining, crop farming and fishing. Client-side only, works in singleplayer and on servers.

![A mob farm: the status panel and the highlighted target](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/hud.png)

## Quick start

1. Put the jar for your loader and Minecraft version into `mods` (on Fabric also [Fabric API](https://modrinth.com/mod/fabric-api)).
2. In a world press **O** for the menu and **I** to turn the mod on or off.
3. Open **Profiles → Presets**, pick a setup and press **I**.

| Preset | What it does |
|---|---|
| **Mob farm** | Hits on full charge with a small random delay, leaves passive mobs, pets and named mobs alone. Auto eat and anti-AFK on. |
| **Mining** | Holds the attack key, auto tool picks the pickaxe, stops when the inventory is full. |
| **Fishing** | Auto fish reels in and casts again. Auto eat and anti-AFK on. |
| **Farming** | Auto farm harvests ripe crops around you and plants them again, hotbar refill brings more seeds. |

## Features

**🖱️ Auto clicker**
- Three channels: attack, use and jump. Each one clicks or holds its key, with its own speed, pause, random delay and hotkey.
- Speed in ticks or in clicks per second. Random delay: even, bell curve or natural.
- Waits for full weapon charge (turn it off for 1.8-style PvP) and pauses while you eat, block or draw a bow.
- Looting finishing blow: the killing hit is dealt with the best Looting weapon from the hotbar.
- Start delay, attack and time limits. Works in the background when the game window is not focused.

**🎯 Targets**
- Players, hostile, neutral and passive mobs and other entities are switched separately.
- Ignore babies, named mobs, pets and invisible mobs. For mining, blocks can be targets too.

**❤️ Survival**
- Safety: stop the mod or leave the server at low health, on any damage, or when a player comes near.
- Offhand: a totem at low health, food when you're hungry, a torch with a pickaxe, otherwise a shield.
- Auto eat: the best safe food from the hotbar. It never opens the chest, door or villager you look at.

**⚙️ Automation**
- Auto fish: reels in on a bite after the reaction time you set and casts again. Stops before the rod breaks.
- Auto farm: wheat, carrots, potatoes, beetroots, nether wart, cocoa and sweet berries, harvested and planted again.
- Hotbar refill: used-up stacks and broken tools are replaced from the inventory. A tool is swapped out before it breaks, so Mending tools are kept.
- Anti-AFK (jump, sneak, swing, look around, step), auto walk and an inventory cleaner for your junk list.

**⛏️ Mining**
- A whitelist or blacklist of blocks the clicker may break, edited with item icons.
- Auto tool picks the fastest tool for the block, counts Efficiency and skips nearly broken tools.
- Stops when the inventory is full and protects the tool in hand.

**👀 HUD and menu**
- Status panel: profile, CPS, attacks, kills, damage, catches, harvest, session time, ping, TPS, FPS and active modules. Any corner, any scale.
- Statistics for the session, each server and all time: kills by mob, exact damage and DPS, catches by kind, harvest, blocks, rates per hour, a live CPS graph, history of the last 20 sessions and CSV export.
- The target about to be hit is outlined, filled or glowing, in your color.
- Notifications with a chime: full inventory, tool about to break, no food, a player nearby, a fish caught.
- A menu with search, tooltips and exact values. It fits any window size.

**🔗 Also:** a hotkey for any module, channel or profile (side mouse buttons too), profiles that load by themselves on a given server, a profile shared as one line of text. 13 languages.

## Which file do I need?

| You play on | File |
|---|---|
| Fabric 1.19 – 1.19.4, 1.20 – 1.20.6, 1.21 – 1.21.11, 26.1 – 26.3 | `MultiClicker-fabric`, needs [Fabric API](https://modrinth.com/mod/fabric-api); [Mod Menu](https://modrinth.com/mod/modmenu) optional |
| NeoForge 1.20.1 – 26.3 | `MultiClicker-neoforge` |
| Forge 1.19 – 1.20.1 | `MultiClicker-forge` |

Fabric 1.21.9 and newer needs Fabric Loader 0.17 or newer. On Fabric 1.21.9 only the glow highlight is drawn.

> Many servers forbid auto clickers and AFK tools. Check the rules before using it there.

**[All settings and controls →](https://github.com/Shamanalle/MultiClicker#readme)** · [Changelog](https://github.com/Shamanalle/MultiClicker/blob/main/CHANGELOG.md) · [Report a bug](https://github.com/Shamanalle/MultiClicker/issues)
````

---

## 3. Russian description

Both sites have one description per project, so this block goes after the English text. On Modrinth put
it inside `<details><summary><b>🇷🇺 Русский</b></summary> ... </details>` (the "Create Modrinth project"
workflow does that). On CurseForge paste it after a `---` line; both languages fit.

````markdown
Автокликер и помощник для AFK в Minecraft: нажимает атаку, использование и прыжок в заданном ритме, а модули берут на себя AFK-фермы, копание, огород и рыбалку. Только клиент, работает в одиночной игре и на серверах.

## Быстрый старт

1. Положите jar для своего загрузчика и версии Minecraft в `mods` (на Fabric ещё [Fabric API](https://modrinth.com/mod/fabric-api)).
2. В мире **O** открывает меню, **I** включает и выключает мод.
3. Откройте **Профили → Пресеты**, выберите набор и нажмите **I**.

| Пресет | Что делает |
|---|---|
| **Ферма мобов** | Бьёт при полной зарядке со случайной задержкой, не трогает мирных мобов, питомцев и мобов с именем. Авто-еда и анти-АФК включены. |
| **Копание** | Держит клавишу атаки, авто-инструмент берёт кирку, останавливается при полном инвентаре. |
| **Рыбалка** | Авто-рыбалка подсекает и забрасывает снова. Авто-еда и анти-АФК включены. |
| **Ферма** | Автоферма собирает созревший урожай вокруг и сажает заново, пополнение хотбара подкладывает семена. |

## Возможности

**🖱️ Автокликер**
- Три канала: атака, использование и прыжок. Каждый нажимает или удерживает свою клавишу, со своей скоростью, паузой, случайной задержкой и горячей клавишей.
- Скорость в тиках или в кликах в секунду. Случайная задержка: равномерная, колокол или естественная.
- Ждёт полной зарядки оружия (для PvP в стиле 1.8 можно выключить) и ставится на паузу, пока вы едите, закрываетесь щитом или натягиваете лук.
- Добивание с Добычей: смертельный удар наносится оружием с лучшей Добычей из хотбара.
- Задержка старта, лимиты атак и времени. Работает в фоне, когда окно игры не в фокусе.

**🎯 Цели**
- Игроки, враждебные, нейтральные и мирные мобы и прочие сущности включаются отдельно.
- Можно не трогать детёнышей, мобов с именем, питомцев и невидимых. Для копания целью могут быть и блоки.

**❤️ Выживание**
- Безопасность: остановить мод или выйти с сервера при низком здоровье, при любом уроне или когда рядом игрок.
- Вторая рука: тотем при низком здоровье, еда при голоде, факел с киркой, иначе щит.
- Авто-еда: лучшая безопасная еда из хотбара. Никогда не открывает сундук, дверь или торговца, на которого вы смотрите.

**⚙️ Автоматизация**
- Авто-рыбалка: подсекает при поклёвке через заданное время реакции и забрасывает снова. Останавливается до поломки удочки.
- Автоферма: пшеница, морковь, картофель, свёкла, незерский нарост, какао и сладкие ягоды — собирает и сажает заново.
- Пополнение хотбара: закончившиеся стаки и сломанные инструменты заменяются из инвентаря. Инструмент меняется до поломки, так что инструменты с «Починкой» сохраняются.
- Анти-АФК (прыжок, приседание, взмах, взгляд по сторонам, шаг), авто-ходьба и очистка инвентаря по списку мусора.

**⛏️ Копание**
- Белый или чёрный список блоков, которые кликер может ломать, с иконками предметов.
- Авто-инструмент берёт самый быстрый инструмент для блока с учётом Эффективности и пропускает почти сломанные.
- Останавливается при полном инвентаре и бережёт инструмент в руке.

**👀 HUD и меню**
- Панель состояния: профиль, кл/с, удары, убийства, урон, улов, урожай, время сессии, пинг, TPS, FPS и активные модули. Любой угол, любой размер.
- Статистика за сессию, по серверам и за всё время: убийства по мобам, точный урон и DPS, улов по видам, урожай, блоки, скорость в час, живой график кл/с, история последних 20 сессий и экспорт в CSV.
- Цель, которую сейчас ударят, обводится, заливается или светится вашим цветом.
- Уведомления со звуком: полный инвентарь, инструмент вот-вот сломается, нет еды, рядом игрок, поймана рыба.
- Меню с поиском, подсказками и вводом точных значений. Подстраивается под любой размер окна.

**🔗 Ещё:** горячая клавиша для любого модуля, канала и профиля (и боковые кнопки мыши), профили, которые сами загружаются на нужном сервере, профиль одной строкой текста, чтобы поделиться. 13 языков.

## Какой файл нужен?

| Вы играете на | Файл |
|---|---|
| Fabric 1.19 – 1.19.4, 1.20 – 1.20.6, 1.21 – 1.21.11, 26.1 – 26.3 | `MultiClicker-fabric`, нужен [Fabric API](https://modrinth.com/mod/fabric-api); [Mod Menu](https://modrinth.com/mod/modmenu) по желанию |
| NeoForge 1.20.1 – 26.3 | `MultiClicker-neoforge` |
| Forge 1.19 – 1.20.1 | `MultiClicker-forge` |

Fabric 1.21.9 и новее требует Fabric Loader 0.17 или новее. На Fabric 1.21.9 рисуется только подсветка «свечение».

> Многие серверы запрещают автокликеры и анти-АФК. Сначала проверьте правила.

**[Все настройки и управление →](https://github.com/Shamanalle/MultiClicker/blob/main/README.ru.md)** · [Список изменений](https://github.com/Shamanalle/MultiClicker/blob/main/CHANGELOG.md) · [Сообщить об ошибке](https://github.com/Shamanalle/MultiClicker/issues)
````

---

## 4. Version fields

Upload every jar as its own version/file and tick the Minecraft versions in its name and the loader of the
jar: Fabric for `MultiClicker-fabric-*`, NeoForge for `MultiClicker-neoforge-*`, Forge for `MultiClicker-forge-*` (Fabric 1.21.9 and newer: Fabric Loader 0.17+, mention it in the
changelog). A jar named for a range, such as `MultiClicker-fabric-2.5.0+1.21.6-1.21.8.jar`, is for every
version of that range (1.21.6, 1.21.7 and 1.21.8); see `versions/shared-jars.txt`.

**Version name:** `MultiClicker <mod version> for Minecraft <versions> (<loader>)`, for example `MultiClicker 2.5.0 for Minecraft 1.21.5 (Fabric)`
or `MultiClicker 2.5.0 for Minecraft 1.21.6–1.21.8 (Fabric)`.

**Version number:** `<mod version>+<Minecraft versions>-<loader>`, for example `2.5.0+1.21.5-fabric` or `2.5.0+1.21.6-1.21.8-fabric`.

**Release channel:** Release.

**Dependencies:** Fabric jars: Fabric API (required), Mod Menu (optional). NeoForge and Forge jars: none.

**Changelog** (the same text for every file; the release workflow uploads the section of `CHANGELOG.md`
followed by the one of `CHANGELOG.ru.md`):

```
2.4.0

- Statistics: a new screen with the session, each server, all time and the last 20 sessions. Clicks,
  kills by mob, exact damage and DPS, catches by kind and item, harvest, mined blocks, food, totems,
  deaths, experience, rates per hour, a live clicks-per-second graph and a per-minute chart.
- Export statistics to CSV; a summary of the session in the chat when the mod is turned off.
- HUD: damage, catches, harvest, blocks and experience, optionally per hour.
- Works on Minecraft 1.19 to 1.19.4, 1.20 to 1.20.6, 1.21 to 1.21.11 and 26.1 to 26.3; download the file
  for your loader and game version. Minecraft 1.21.9 and newer needs Fabric Loader 0.17 or newer.

2.3.0

- NeoForge support: a NeoForge jar for every version from 1.20.1 to 26.3.
- Forge support for Minecraft 1.19 to 1.20.1.
- Fabric support for Minecraft 1.19 to 1.19.4 and 1.20 to 1.20.6.
- Notifications: a message above the hotbar or in the chat when the inventory is full, a tool is about
  to break, the last item in hand is used up, there is no food, a player comes near, a fish is caught or
  the mod stopped by itself.
- Works on Minecraft 1.19 to 1.19.4, 1.20 to 1.20.6, 1.21 to 1.21.11 and 26.1 to 26.3; download the file
  for your loader and game version. Minecraft 1.21.9 and newer needs Fabric Loader 0.17 or newer.

2.2.0

- Hotkeys for every module, clicker channel and profile.
- Click speed in clicks per second, and a choice of random delay: even, bell curve or natural.
- Profiles can be shared as one line of text and can load by themselves on a given server.
- New modules: Auto Farm (harvests and replants crops) and Hotbar Refill (refills used-up stacks and
  swaps a tool before it breaks). New Farming preset.
- Menu: type exact numbers, changed settings are marked, reset a module at once, Tab switches categories.
- Works on Minecraft 1.21 to 1.21.11 and 26.1 to 26.3; download the file for your game version.
  Minecraft 1.21.9 and newer needs Fabric Loader 0.17 or newer.
```

Full history: https://github.com/Shamanalle/MultiClicker/blob/main/CHANGELOG.md

---

## 5. Upload checklist

1. Modrinth: create the project, set the fields from section 1, paste section 2 (add section 3 in a
   `<details>` block at the end), add the icon `common/src/main/resources/assets/multiclicker/icon.png`
   and the screenshots to the gallery (set `hud.png` as the featured image).
2. Upload the jars from the GitHub release as separate versions (section 4). A jar named for a range of
   Minecraft versions gets all of them; CI checks before every release that building the mod for each of
   them gives the same code.
3. CurseForge: the same text, the same files (one file per loader and jar).
4. Both sites review new projects; the review can take from a few hours to a few days.
5. Later versions can be uploaded by the release workflow itself. In the repository settings
   (*Settings → Secrets and variables → Actions*) add the variables `MODRINTH_ID` and `CURSEFORGE_ID`
   (the project ids shown on each site) and the secrets `MODRINTH_TOKEN` (Modrinth → Settings → PATs,
   with the *Create versions* scope) and `CURSEFORGE_TOKEN` (CurseForge → API tokens). From then on every
   release also uploads its jars to both sites, with the changelog of that version in English and
   Russian. A site whose
   token is missing is skipped.
