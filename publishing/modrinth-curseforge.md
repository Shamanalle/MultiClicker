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
# MultiClicker

Auto clicker (autoclicker) and AFK helper for Minecraft (Fabric, NeoForge and Forge). It clicks the attack, use and jump keys at a rhythm you set, and
comes with modules for AFK farms, mining, crop farming and fishing: auto eat, auto fish, auto farm, auto
tool, hotbar refill, offhand, safety stop, anti-AFK and others. Every module and profile can have a hotkey.

![A mob farm with MultiClicker: the status panel and the highlighted target](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/hud.png)

## Quick start

1. Fabric: install Fabric Loader and put **Fabric API** and the MultiClicker jar into the `mods` folder.
   [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds a settings button to the mod list.
   NeoForge or Forge: put the MultiClicker jar into the `mods` folder; nothing else is needed.
2. Join a world. Press **O** to open the menu and **I** to turn the mod on or off.
3. Open **Profiles → Presets**, pick a setup and press **I**.

| Preset | What it sets up |
|:--|:--|
| **Mob farm** | Attacks on full charge with a small random delay. Passive mobs, pets and named mobs are left alone. Auto eat and anti-AFK are on. |
| **Mining** | The attack key is held, blocks are broken, auto tool picks the pickaxe, the clicker stops when the inventory is full. Auto eat is on. |
| **Fishing** | The clicker is off, auto fish reels in and casts again. Auto eat and anti-AFK are on. |
| **Farming** | The clicker is off, auto farm harvests ripe crops around you and plants them again, hotbar refill brings more seeds. Auto eat is on. |

Your own settings can be saved as profiles: load one with a hotkey, have it load by itself on a server, or
share it as a line of text.

> Many multiplayer servers don't allow auto clickers. Check the server rules before you use the mod online.

## Features

Every option has a tooltip in the menu. Right-click a setting to reset it.

**Auto clicker**
- Three channels: attack (left mouse button), use (right mouse button) and jump. Each one can click or
  hold the key and has its own speed, hold time, pause, random delay and hotkey.
- Speed as an interval in ticks or in clicks per second (fractions of a tick carry over, so 12 CPS is
  kept on average).
- Random delay of your choice: even, bell curve (steady rhythm) or natural (mostly short, now and then a
  longer pause).
- Waits for full weapon charge for full damage. Turn it off for 1.8-style PvP.
- Targets: only allowed creatures, creatures and blocks (mining), or anything.
- Pauses while you eat, drink, block or draw a bow.
- Looting finishing blow: predicts whether the next hit kills; if so, it switches to the hotbar weapon
  with the best Looting, waits for it to charge, hits and switches back.
- Start delay, attack limit and time limit: the mod turns itself off when they run out.
- Works in the background when the game window loses focus, and can lock the camera.
- Keys you hold yourself are not released. The clicker pauses while a screen is open.

**Target filter**
Players, hostile, neutral and passive mobs and other entities (boats, minecarts, armor stands, end
crystals) are switched separately. You can also ignore babies, named mobs, pets and invisible mobs.

**Survival**
- Safety: stops the mod or leaves the server when your health drops to a threshold, when you take any
  damage, or when another player comes within a set distance.
- Offhand: keeps one item in the offhand by priority: a totem at low health, food when you're hungry,
  a torch while you hold a pickaxe, otherwise a shield.
- Auto eat: eats the best safe food from the hotbar and returns to the previous slot. It can skip harmful
  food (rotten flesh, pufferfish, suspicious stew) and golden food, and never opens the chest, door or
  villager you look at.

**Automation**
- Auto fish: reels in on a bite after a reaction time you set, casts again, recasts if nothing bites for
  too long, stops when the rod is about to break or after a number of catches. The rod can be in either hand.
- Anti-AFK: jump, sneak, swing, look around or step at random intervals. Each action can be turned off.
- Auto walk: walks forward, optionally sprinting and jumping over obstacles.
- Auto farm: harvests ripe wheat, carrots, potatoes, beetroots, nether wart, cocoa and sweet berries
  around you (or only under the crosshair) and plants them again.
- Hotbar refill: a used-up stack or a broken tool is replaced from the inventory, and the tool in your hand
  is swapped for a spare before it breaks, so a Mending tool is kept.
- Inventory cleaner: throws out the items from your junk list, one stack at a time. It never touches
  the hotbar unless you allow it.

**Mining**
- Mining rules: a whitelist or blacklist of blocks the clicker may break, edited in a list editor with
  item icons. It can stop when the inventory is full and protect the held tool from breaking.
- Auto tool: picks the fastest hotbar tool for the block, takes Efficiency into account, skips nearly
  broken tools and switches back afterwards.

**Interface**
- HUD: a status panel in any corner with the scale you choose: the loaded profile, active channels,
  clicks per second, attacks, kills, session time, ping, server TPS, FPS and the list of active modules.
  Every line can be hidden.
- Target highlight: the creature about to be hit is outlined, filled or glowing, in a color you pick.
- Menu: categories, module cards, search (also by English names), tooltips and a list editor with item
  icons. Click a number to type an exact value; changed settings are marked and a module resets with one
  click. It fits the window size; on small screens the sidebar turns into icons.
- Feedback: accent color, a sound and a message above the hotbar when the mod turns on or off.
- Notifications: a message above the hotbar or in the chat, with a chime, when the inventory is full,
  the tool in hand is about to break, the last item in hand is used up, auto eat finds no food, a player
  comes near, a fish is caught, or the mod stopped by itself. Each one can be turned off.
- Hotkeys: any module, each clicker channel and each profile can get a key or a side mouse button.
- Profiles: save your settings under a name and load them later, with a hotkey or by themselves on a
  given server. Copy a profile as one line of text to share it, paste one you got from someone else.

## Screenshots

![Clicker settings](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/menu_clicker.png)
![Automation modules](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/menu_automation.png)
![Survival modules](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/menu_survival.png)
![Visual modules](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/menu_visual.png)
![Presets and profiles](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/profiles.png)
![Block list editor](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/en/list_editor.png)

## Servers

Client-side only: nothing is installed on the server, and it works in singleplayer and on servers. Many
servers forbid auto clickers and AFK tools, so check the rules before using it there.

## Supported versions

One jar per loader and Minecraft version: Fabric for 1.19 to 1.19.4, 1.20 to 1.20.6, 1.21 to 1.21.11 and
26.1 to 26.3; NeoForge for 1.20.1 to 26.3 (NeoForge starts at 1.20.1); Forge for 1.19 to 1.20 (on Forge
1.20.1 use the NeoForge jar). Download the file for your loader and version. Minecraft 1.21.9 and newer needs Fabric Loader 0.17 or newer.

- On Fabric 1.21.9 the outline and filled highlight styles are not drawn (Fabric API for that version
  has no world rendering events); the glow style works.

## Controls

| Key | Action |
|:--:|:--|
| I | Turn the mod on / off |
| O | Open the settings |

Rebind them under Options → Controls → Key Binds → MultiClicker. Hotkeys for modules and clicker
channels are set in the menu (the ⌨ button of a module), hotkeys for profiles under Profiles. In the
menu, start typing to search, press Tab for the next category, and use the mouse wheel, the arrow keys
or a click on the number to set sliders.

## Languages

English, Русский, Українська, Deutsch, Français, Español, Português (Brasil), Polski, Italiano,
Türkçe, 简体中文, 日本語, 한국어. Items, enchantments and mobs use the game's own names in each language.

## Links

[Source code](https://github.com/Shamanalle/MultiClicker) · [Report a problem](https://github.com/Shamanalle/MultiClicker/issues) · MIT license
````

---

## 3. Russian description

Modrinth and CurseForge have one description per project. Two options: add this block at the end of
the English text inside `<details><summary>Русский</summary> ... </details>`, or use it as the
description of a Russian translation if the site offers one.

````markdown
# MultiClicker

Автокликер и помощник для AFK в Minecraft (Fabric, NeoForge и Forge). Нажимает клавиши атаки, использования и прыжка в заданном ритме.
Есть модули для AFK-ферм, копания, огорода и рыбалки: авто-еда, авто-рыбалка, автоферма,
авто-инструмент, пополнение хотбара, вторая рука, аварийная остановка, анти-АФК и другие. У каждого
модуля и профиля может быть горячая клавиша.

## Быстрый старт

1. Fabric: установите Fabric Loader, положите в папку `mods` **Fabric API** и jar мода для вашей версии.
   [Mod Menu](https://modrinth.com/mod/modmenu) не обязателен: он добавляет кнопку настроек в список модов.
   NeoForge или Forge: положите jar мода в папку `mods`, больше ничего не нужно.
2. Зайдите в мир. **O** открывает меню, **I** включает и выключает мод.
3. Откройте **Профили → Пресеты**, выберите набор и нажмите **I**.

| Пресет | Что настраивает |
|:--|:--|
| **Ферма мобов** | Бьёт при полной зарядке со случайной задержкой. Мирных мобов, питомцев и мобов с именем не трогает. Включены авто-еда и анти-АФК. |
| **Копание** | Клавиша атаки удерживается, блоки ломаются, авто-инструмент берёт кирку, кликер останавливается при полном инвентаре. Включена авто-еда. |
| **Рыбалка** | Кликер выключен, авто-рыбалка подсекает и забрасывает удочку снова. Включены авто-еда и анти-АФК. |
| **Ферма** | Кликер выключен, автоферма собирает созревший урожай вокруг и сажает заново, пополнение хотбара подкладывает семена. Включена авто-еда. |

Свои настройки можно сохранять как профили: загружать горячей клавишей, автоматически на нужном сервере
или делиться ими одной строкой текста.

> Многие серверы запрещают автокликеры. Перед игрой онлайн прочитайте правила сервера.

## Возможности

У каждой настройки в меню есть подсказка. Правый клик по настройке сбрасывает её.

**Автокликер**
- Три канала: атака (левая кнопка мыши), использование (правая) и прыжок. Каждый может нажимать или
  удерживать клавишу и имеет свою скорость, время удержания, паузу, случайную задержку и горячую клавишу.
- Скорость задаётся интервалом в тиках или в кликах в секунду (доли тика переносятся, так что 12 кл/с
  держатся в среднем).
- Случайная задержка на выбор: равномерная, колокол (ровный ритм) или естественная (чаще коротко,
  изредка пауза подольше).
- Ждёт полной зарядки оружия ради полного урона. Для PvP в стиле 1.8 это можно отключить.
- Цели: только разрешённые существа, существа и блоки (копание) или что угодно.
- Пауза, пока вы едите, пьёте, закрываетесь щитом или натягиваете лук.
- Добивающий удар с Добычей: предсказывает, убьёт ли следующий удар; если да, берёт из хотбара оружие
  с лучшей Добычей, ждёт зарядки, бьёт и возвращает прежний слот.
- Задержка старта, лимит атак и лимит времени: по их исчерпании мод выключается сам.
- Работает в фоне, когда окно игры не в фокусе; может блокировать камеру.
- Клавиши, которые вы держите сами, не отпускаются. Пока открыт экран, кликер на паузе.

**Фильтр целей**
Игроки, враждебные, нейтральные и мирные мобы и прочие сущности (лодки, вагонетки, стойки для брони,
кристаллы Края) включаются отдельно. Можно игнорировать детёнышей, мобов с именем, питомцев и невидимых мобов.

**Выживание**
- Безопасность: останавливает мод или выходит с сервера, когда здоровье падает до порога, при любом
  уроне или когда рядом появляется другой игрок.
- Вторая рука: держит один предмет по приоритету: тотем при низком здоровье, еду при голоде, факел с
  киркой в руке, иначе щит.
- Авто-еда: ест лучшую безопасную еду из хотбара и возвращает прежний слот. Может пропускать вредную
  еду (гнилая плоть, иглобрюх, подозрительное рагу) и золотую, и никогда не открывает сундук, дверь
  или торговца, на которого вы смотрите.

**Автоматизация**
- Авто-рыбалка: подсекает при поклёвке через заданное время реакции, забрасывает снова, перезабрасывает,
  если долго нет поклёвки, останавливается перед поломкой удочки или после заданного числа уловов.
  Удочка может быть в любой руке.
- Анти-АФК: прыжок, приседание, взмах, взгляд по сторонам или шаг через случайные промежутки. Каждое
  действие можно отключить.
- Авто-ходьба: идёт вперёд, при желании бежит и прыгает через препятствия.
- Автоферма: собирает созревшие пшеницу, морковь, картофель, свёклу, незерский нарост, какао и сладкие
  ягоды вокруг (или только под прицелом) и сажает заново.
- Пополнение хотбара: закончившийся стак или сломанный инструмент заменяется из инвентаря, а инструмент в
  руке меняется на запасной до поломки, так что инструмент с «Починкой» сохраняется.
- Очистка инвентаря: выбрасывает предметы из списка мусора по одному стаку. Хотбар не трогает без разрешения.

**Копание**
- Правила копания: белый или чёрный список блоков, которые кликер может ломать; редактируется в
  редакторе списков с иконками предметов. Может останавливаться при полном инвентаре и беречь
  инструмент в руке от поломки.
- Авто-инструмент: берёт из хотбара самый быстрый инструмент для блока с учётом Эффективности,
  пропускает почти сломанные и возвращает прежний слот.

**Интерфейс**
- HUD: панель состояния в любом углу и нужного размера: загруженный профиль, активные каналы, кликов
  в секунду, атаки, убийства, время сессии, пинг, TPS сервера, FPS и список активных модулей. Любую
  строку можно скрыть.
- Подсветка цели: существо, которое сейчас ударят, обводится, заливается или светится выбранным цветом.
- Меню: категории, карточки модулей, поиск (в том числе по английским названиям), подсказки и
  редактор списков с иконками. Число можно ввести с клавиатуры, изменённые настройки отмечены, модуль
  сбрасывается одним нажатием. Подстраивается под размер окна; на маленьких экранах боковая панель
  превращается в значки.
- Обратная связь: цвет акцента, звук и сообщение над хотбаром при включении и выключении мода.
- Уведомления: сообщение над хотбаром или в чате со звуком, когда инвентарь заполнен, инструмент в руке
  вот-вот сломается, последний предмет в руке закончился, авто-еда не нашла еды, рядом появился игрок,
  поймана рыба или мод сам остановился. Каждое можно выключить.
- Горячие клавиши: любому модулю, каналу кликера и профилю можно назначить клавишу или боковую кнопку мыши.
- Профили: сохраняйте настройки под именем и загружайте позже — горячей клавишей или сами на нужном
  сервере. Профиль копируется одной строкой текста, чтобы им поделиться, и так же вставляется чужой.

## Скриншоты

![Настройки кликера](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/ru/menu_clicker.png)
![Модули автоматизации](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/ru/menu_automation.png)
![Модули выживания](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/ru/menu_survival.png)
![Визуальные модули](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/ru/menu_visual.png)
![Пресеты и профили](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/ru/profiles.png)
![Редактор списка блоков](https://raw.githubusercontent.com/Shamanalle/MultiClicker/main/docs/images/ru/list_editor.png)

## Серверы

Мод только клиентский: на сервер ничего ставить не нужно, работает в одиночной игре и на серверах. Многие
серверы запрещают автокликеры и анти-АФК, поэтому сначала проверьте правила.

## Поддерживаемые версии

Один jar на каждый загрузчик и версию Minecraft: Fabric для 1.19–1.19.4, 1.20–1.20.6, 1.21–1.21.11 и
26.1–26.3; NeoForge для 1.20.1–26.3 (NeoForge начинается с 1.20.1); Forge для 1.19–1.20 (на Forge 1.20.1
используйте jar для NeoForge). Скачайте файл для своего загрузчика и версии. Для Minecraft 1.21.9 и новее нужен Fabric Loader 0.17 или новее.

- На Fabric 1.21.9 стили подсветки «обводка» и «заливка» не рисуются (в Fabric API для этой версии нет событий
  отрисовки мира); стиль «свечение» работает.

## Управление

| Клавиша | Действие |
|:--:|:--|
| I | Включить / выключить мод |
| O | Открыть настройки |

Клавиши меняются в Настройки → Управление → Клавиши → MultiClicker. Горячие клавиши модулей и каналов
назначаются в меню (кнопка ⌨ у модуля), клавиши профилей — в разделе «Профили». В меню начните
печатать, чтобы искать; Tab переключает категорию, а ползунки настраиваются колесом, стрелками или
вводом числа.

## Языки

English, Русский, Українська, Deutsch, Français, Español, Português (Brasil), Polski, Italiano,
Türkçe, 简体中文, 日本語, 한국어. Предметы, зачарования и мобы называются так, как в самой игре.

## Ссылки

[Исходный код](https://github.com/Shamanalle/MultiClicker) · [Сообщить о проблеме](https://github.com/Shamanalle/MultiClicker/issues) · лицензия MIT
````

---

## 4. Version fields

Upload every jar as its own version/file and tick the matching Minecraft version and the loader of the
jar: Fabric for `MultiClicker-fabric-*`, NeoForge for `MultiClicker-neoforge-*` (the 1.20.1 one also gets
Forge), Forge for `MultiClicker-forge-*` (Fabric 1.21.9 and newer: Fabric Loader 0.17+, mention it in the
changelog).

**Version name:** `MultiClicker 2.3.0 for Minecraft <version> (<loader>)`, for example `MultiClicker 2.3.0 for Minecraft 1.21.4 (Fabric)`.

**Version number:** `2.3.0+<Minecraft version>-<loader>`, for example `2.3.0+1.21.4-fabric`.

**Release channel:** Release.

**Dependencies:** Fabric jars: Fabric API (required), Mod Menu (optional). NeoForge and Forge jars: none.

**Changelog** (the same text for every file):

```
2.3.0

- NeoForge support: a NeoForge jar for every version from 1.20.1 to 26.3.
- Forge support for Minecraft 1.19 to 1.20 (on Forge 1.20.1 the NeoForge jar works).
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
2. Upload the 58 jars from the GitHub release (29 Fabric, 23 NeoForge, 6 Forge) as separate versions (section 4). Modrinth can attach
   several Minecraft versions to one file only when the file really works on all of them, so keep one
   jar per version.
3. CurseForge: the same text, the same 58 files (one file per loader and game version).
4. Both sites review new projects; the review can take from a few hours to a few days.
5. Later versions can be uploaded by the release workflow itself. In the repository settings
   (*Settings → Secrets and variables → Actions*) add the variables `MODRINTH_ID` and `CURSEFORGE_ID`
   (the project ids shown on each site) and the secrets `MODRINTH_TOKEN` (Modrinth → Settings → PATs,
   with the *Create versions* scope) and `CURSEFORGE_TOKEN` (CurseForge → API tokens). From then on every
   release also uploads its 58 jars to both sites, with the changelog of that version. A site whose
   token is missing is skipped.
