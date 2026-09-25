package pro.mikey.autoclicker;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pro.mikey.autoclicker.core.ModuleManager;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.ui.*;
import pro.mikey.autoclicker.modules.combat.*;
import pro.mikey.autoclicker.modules.world.*;
import pro.mikey.autoclicker.modules.automation.*;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * OptionsScreen v7 — Sidebar + Content Panel, 7 categories grouped by function.
 *
 * 🖱 КЛИКЕР: ЛКМ, ПКМ, Прыжок, Общие
 * ⚔ БОЙ:    Combat (merged: Боевые + SmartCombat + MobFilter + CombatFeedback)
 * 🛡 АНТИЧИТ: AntiCheat (merged: AntiCheat + NetworkGuard)
 * ⛏ КОПКА:  Режим копки, Фильтр блоков, Авто-Инструмент
 * ❤ ВЫЖИВАНИЕ: Panic, Тотем, Offhand, Авто-Еда
 * 🤖 АВТО:   Рыбалка, АФК, Ходьба, Дроп
 * 📊 HUD:    Оверлей
 */
public class OptionsScreen extends Screen {

    private static final int HEAD = 30, FOOT = 18;
    private static final int NAV_W = 136;

    private McNavPanel nav;
    private McScrollView content;
    private String selectedId = "lmb";

    private String query = "";
    private boolean searchOn;
    private int blink;
    private float openAnim;

    private final AutoClicker ac = AutoClicker.getInstance();
    private final ModuleManager mm = ac.getModuleManager();

    public OptionsScreen() {
        super(Component.literal("MultiClicker"));
    }

    // ══════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════

    @Override
    protected void init() {
        super.init();
        nav = new McNavPanel(0, HEAD, NAV_W, height - HEAD - FOOT);
        content = new McScrollView(NAV_W, HEAD, width - NAV_W, height - HEAD - FOOT);
        buildNav();
        nav.setSelectedId(selectedId);
        nav.setOnSelect(id -> { selectedId = id; rebuildContent(); });
        rebuildContent();
    }

    // ══════════════════════════════════════════
    // Sidebar — 7 categories, 13 items
    // ══════════════════════════════════════════

    private void buildNav() {
        nav.clearCategories();
        CombatClickerModule c = mm.getModule(CombatClickerModule.class);
        MiningModule mining = mm.getModule(MiningModule.class);
        MiningFilterModule mf = mm.getModule(MiningFilterModule.class);
        AntiCheatModule ach = mm.getModule(AntiCheatModule.class);
        AutoTotemModule totem = mm.getModule(AutoTotemModule.class);
        SmartOffhandModule offhand = mm.getModule(SmartOffhandModule.class);
        AutoEatModule eat = mm.getModule(AutoEatModule.class);
        AutoFishModule fish = mm.getModule(AutoFishModule.class);
        AntiAfkModule afk = mm.getModule(AntiAfkModule.class);
        AutoWalkModule walk = mm.getModule(AutoWalkModule.class);
        AutoToolModule tool = mm.getModule(AutoToolModule.class);
        TrashDropModule trash = mm.getModule(TrashDropModule.class);
        PanicModeModule panic = mm.getModule(PanicModeModule.class);
        HudModule hud = mm.getModule(HudModule.class);

        // 🖱 КЛИКЕР — pure auto-click speed/mode
        nav.addCategory(new McNavPanel.NavCategory("🖱", "КЛИКЕР", 0xFF5599FF, List.of(
            new McNavPanel.NavItem("lmb", "ЛКМ", c != null ? c.leftActive : null),
            new McNavPanel.NavItem("rmb", "ПКМ", c != null ? c.rightActive : null),
            new McNavPanel.NavItem("jump", "Прыжок", c != null ? c.jumpActive : null),
            new McNavPanel.NavItem("general", "Общие", null)
        )));

        // ⚔ БОЙ — all combat logic in one screen
        nav.addCategory(new McNavPanel.NavCategory("⚔", "БОЙ", 0xFFFF5577, List.of(
            new McNavPanel.NavItem("combat", "Бой", null)
        )));

        // 🛡 АНТИЧИТ — all stealth/bypass in one screen
        nav.addCategory(new McNavPanel.NavCategory("🛡", "АНТИЧИТ", 0xFFFFAA33, List.of(
            new McNavPanel.NavItem("anticheat", "Античит", ach != null ? ach.advancedRandomEnabled : null)
        )));

        // ⛏ КОПКА — mining, block filter, auto-tool
        nav.addCategory(new McNavPanel.NavCategory("⛏", "КОПКА", 0xFFCC8833, List.of(
            new McNavPanel.NavItem("mining", "Режим копки", mining != null ? mining.miningMode : null),
            new McNavPanel.NavItem("mining_filter", "Фильтр блоков", mf != null ? mf.enabled : null),
            new McNavPanel.NavItem("auto_tool", "Авто-Инструмент", tool != null ? tool.enabled : null)
        )));

        // ❤ ВЫЖИВАНИЕ — staying alive
        nav.addCategory(new McNavPanel.NavCategory("❤", "ВЫЖИВАНИЕ", 0xFFFF6688, List.of(
            new McNavPanel.NavItem("panic_mode", "Panic Mode", panic != null ? panic.enabled : null),
            new McNavPanel.NavItem("auto_totem", "Авто-Тотем", totem != null ? totem.enabled : null),
            new McNavPanel.NavItem("smart_offhand", "Smart Offhand", offhand != null ? offhand.enabled : null),
            new McNavPanel.NavItem("auto_eat", "Авто-Еда", eat != null ? eat.enabled : null)
        )));

        // 🤖 АВТО — independent automation
        nav.addCategory(new McNavPanel.NavCategory("🤖", "АВТО", 0xFF44CC66, List.of(
            new McNavPanel.NavItem("auto_fish", "Авто-Рыбалка", fish != null ? fish.enabled : null),
            new McNavPanel.NavItem("anti_afk", "Анти-АФК", afk != null ? afk.enabled : null),
            new McNavPanel.NavItem("auto_walk", "Авто-Ходьба", walk != null ? walk.enabled : null),
            new McNavPanel.NavItem("trash_drop", "Авто-Дроп", trash != null ? trash.enabled : null)
        )));

        // 📊 HUD
        nav.addCategory(new McNavPanel.NavCategory("📊", "HUD", 0xFFAA66FF, List.of(
            new McNavPanel.NavItem("hud", "HUD Оверлей", hud != null ? hud.enabled : null)
        )));
    }

    // ══════════════════════════════════════════
    // Content builder dispatch
    // ══════════════════════════════════════════

    private void rebuildContent() {
        if (content == null) return;
        content.clear();
        int cw = width - NAV_W - 30;
        BooleanSupplier mChk = () -> {
            MiningModule m = mm.getModule(MiningModule.class);
            return m != null && m.miningMode.get();
        };

        switch (selectedId) {
            case "lmb"           -> buildLmb(cw, mChk);
            case "rmb"           -> buildRmb(cw, mChk);
            case "jump"          -> buildJump(cw);
            case "general"       -> buildGeneral(cw);
            case "combat"        -> buildCombat(cw, mChk);
            case "anticheat"     -> buildAntiCheat(cw, mChk);
            case "mining"        -> buildMining(cw);
            case "mining_filter" -> buildMiningFilter(cw);
            case "auto_tool"     -> buildAutoTool(cw);
            case "panic_mode"    -> buildPanicMode(cw);
            case "auto_totem"    -> buildAutoTotem(cw);
            case "smart_offhand" -> buildSmartOffhand(cw);
            case "auto_eat"      -> buildAutoEat(cw);
            case "auto_fish"     -> buildAutoFish(cw);
            case "anti_afk"      -> buildAntiAfk(cw);
            case "auto_walk"     -> buildAutoWalk(cw);
            case "trash_drop"    -> buildTrashDrop(cw);
            case "hud"           -> buildHud(cw);
        }
    }

    private Runnable saver() {
        return () -> { ac.saveConfig(); rebuildContent(); };
    }

    // ══════════════════════════════════════════
    // 🖱 КЛИКЕР
    // ══════════════════════════════════════════

    private void buildLmb(int cw, BooleanSupplier mChk) {
        CombatClickerModule c = mm.getModule(CombatClickerModule.class);
        if (c == null) return;
        McSection s = new McSection(0, 0, cw, "ЛКМ Автоклик", 0xFF5599FF, c.leftActive)
            .add(c.leftClickMode)
            .add(c.leftSpeed)
            .addIf(c.leftHoldDuration, c.leftClickMode, CombatClickerModule.ClickMode.HOLD)
            .add(c.leftRandomize)
            .addIf(c.leftRandomizeRange, c.leftRandomize, true)
            .build();
        s.withDim(mChk, "Отключено в режиме копки");
        s.withReset(saver());
        content.add(s);
    }

    private void buildRmb(int cw, BooleanSupplier mChk) {
        CombatClickerModule c = mm.getModule(CombatClickerModule.class);
        if (c == null) return;
        McSection s = new McSection(0, 0, cw, "ПКМ Автоклик", 0xFF5599FF, c.rightActive)
            .add(c.rightClickMode)
            .add(c.rightSpeed)
            .addIf(c.rightHoldDuration, c.rightClickMode, CombatClickerModule.ClickMode.HOLD)
            .add(c.rightRandomize)
            .addIf(c.rightRandomizeRange, c.rightRandomize, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    private void buildJump(int cw) {
        CombatClickerModule c = mm.getModule(CombatClickerModule.class);
        if (c == null) return;
        McSection s = new McSection(0, 0, cw, "Авто-прыжок", 0xFF5599FF, c.jumpActive)
            .add(c.jumpClickMode)
            .add(c.jumpSpeed)
            .addIf(c.jumpHoldDuration, c.jumpClickMode, CombatClickerModule.ClickMode.HOLD)
            .add(c.jumpRandomize)
            .addIf(c.jumpRandomizeRange, c.jumpRandomize, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    private void buildGeneral(int cw) {
        CombatClickerModule c = mm.getModule(CombatClickerModule.class);
        if (c == null) return;
        McSection s = new McSection(0, 0, cw, "Общие настройки", 0xFF8899BB, null)
            .add(c.pauseInGui)
            .add(c.clickLimit)
            .add(c.activationDelay)
            .add(c.toggleSound)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    // ══════════════════════════════════════════
    // ⚔ БОЙ — merged: CombatOpts + SmartCombat + MobFilter + CombatFeedback
    // ══════════════════════════════════════════

    private void buildCombat(int cw, BooleanSupplier mChk) {
        CombatClickerModule c = mm.getModule(CombatClickerModule.class);
        SmartCombatModule sc = mm.getModule(SmartCombatModule.class);
        MobFilterModule mf = mm.getModule(MobFilterModule.class);
        CombatFeedbackModule cf = mm.getModule(CombatFeedbackModule.class);

        // Section 1: Основное (attack rules + method)
        if (c != null && sc != null) {
            McSection s1 = new McSection(0, 0, cw, "Основное", 0xFFFF5577, null)
                .add(sc.attackMethod)
                .add(c.respectCooldown)
                .add(c.respectShield)
                .add(c.mobMode)
                .build();
            s1.withDim(mChk, "Отключено в режиме копки");
            s1.withReset(saver());
            content.add(s1);
        }

        // Section 2: Smart Trigger + Looting
        if (sc != null) {
            McSection s2 = new McSection(0, 0, cw, "Умная атака", 0xFFFF5577, null)
                .add(sc.smartTrigger)
                .addIf(sc.smartTriggerCount, sc.smartTrigger, true)
                .add(sc.lootingSwapper)
                .build();
            s2.withDim(mChk, "Отключено в режиме копки");
            s2.withReset(saver());
            content.add(s2);
        }

        // Section 3: Моб-фильтр
        if (mf != null) {
            McSection s3 = new McSection(0, 0, cw, "Моб-фильтр", 0xFFFF5577, null)
                .add(mf.filterBabies)
                .add(mf.filterAdults)
                .add(mf.allowHostile)
                .add(mf.allowPassive)
                .add(mf.allowNeutral)
                .build();
            s3.withDim(mChk, "Отключено в режиме копки");
            s3.withReset(saver());
            content.add(s3);
        }

        // Section 4: Безопасность HP
        if (sc != null) {
            McSection s4 = new McSection(0, 0, cw, "Безопасность HP", 0xFFFF5577, sc.safetyEnabled)
                .addIf(sc.healthThreshold, sc.safetyEnabled, true)
                .addIf(sc.lowHpAction, sc.safetyEnabled, true)
                .build();
            s4.withDim(mChk, "Отключено в режиме копки");
            s4.withReset(saver());
            content.add(s4);
        }

        // Section 5: Звуки — hit
        if (cf != null) {
            McSection s5 = new McSection(0, 0, cw, "Звук удара", 0xFFFF5577, cf.hitSoundEnabled)
                .addIf(cf.hitSoundVolume, cf.hitSoundEnabled, true)
                .addIf(cf.hitSoundType, cf.hitSoundEnabled, true)
                .build();
            s5.withReset(saver());
            content.add(s5);

            // Section 6: Звуки — kill
            McSection s6 = new McSection(0, 0, cw, "Звук убийства", 0xFFFF5577, cf.killSoundEnabled)
                .addIf(cf.killSoundVolume, cf.killSoundEnabled, true)
                .addIf(cf.killSoundType, cf.killSoundEnabled, true)
                .build();
            s6.withReset(saver());
            content.add(s6);
        }
    }

    // ══════════════════════════════════════════
    // 🛡 АНТИЧИТ — merged: AntiCheat + NetworkGuard
    // ══════════════════════════════════════════

    private void buildAntiCheat(int cw, BooleanSupplier mChk) {
        AntiCheatModule a = mm.getModule(AntiCheatModule.class);
        NetworkGuardModule n = mm.getModule(NetworkGuardModule.class);

        // Section 1: Рандомизация
        if (a != null) {
            McSection s1 = new McSection(0, 0, cw, "Продвинутая рандомизация", 0xFFFFAA33, a.advancedRandomEnabled)
                .addIf(a.gaussianRandom, a.advancedRandomEnabled, true)
                .addIf(a.randomSigma, a.gaussianRandom, true)
                .addIf(a.gcdPatch, a.advancedRandomEnabled, true)
                .addIf(a.pollingRate, a.gcdPatch, true)
                .addIf(a.skipChance, a.advancedRandomEnabled, true)
                .build();
            s1.withDim(mChk, "Отключено в режиме копки");
            s1.withReset(saver());
            content.add(s1);

            // Section 2: Защита движения
            McSection s2 = new McSection(0, 0, cw, "Защита движения", 0xFFFFAA33, a.movementProtection)
                .addIf(a.movementDelay, a.movementProtection, true)
                .add(a.rotationProtection)
                .add(a.cameraLock)
                .build();
            s2.withDim(mChk, "Отключено в режиме копки");
            s2.withReset(saver());
            content.add(s2);

            // Section 3: Entity Protection
            McSection s3 = new McSection(0, 0, cw, "Entity Protection", 0xFFFFAA33, a.entityProtection)
                .addIf(a.targetDelay, a.entityProtection, true)
                .addIf(a.entityProtTimeout, a.entityProtection, true)
                .build();
            s3.withDim(mChk, "Отключено в режиме копки");
            s3.withReset(saver());
            content.add(s3);
        }

        // Section 4: Сетевая защита
        if (n != null) {
            McSection s4 = new McSection(0, 0, cw, "Сетевая защита", 0xFFFFAA33, n.enabled)
                .addIf(n.pingLimit, n.enabled, true)
                .addIf(n.tpsLimit, n.enabled, true)
                .addIf(n.tpsSync, n.enabled, true)
                .build();
            s4.withDim(mChk, "Отключено в режиме копки");
            s4.withReset(saver());
            content.add(s4);
        }
    }

    // ══════════════════════════════════════════
    // ⛏ КОПКА — mining mode, block filter, auto-tool
    // ══════════════════════════════════════════

    private void buildMining(int cw) {
        MiningModule m = mm.getModule(MiningModule.class);
        if (m == null) return;
        McSection s = new McSection(0, 0, cw, "Режим копки", 0xFFCC8833, m.miningMode)
            .addIf(m.saveToolEnabled, m.miningMode, true)
            .addIf(m.saveToolThreshold, m.saveToolEnabled, true)
            .addIf(m.stopWhenFull, m.miningMode, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    private void buildMiningFilter(int cw) {
        MiningFilterModule m = mm.getModule(MiningFilterModule.class);
        if (m == null) return;
        McSection s = new McSection(0, 0, cw, "Фильтр блоков", 0xFFCC8833, m.enabled)
            .addIf(m.mode, m.enabled, true)
            .addIf(m.blockList, m.enabled, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    private void buildAutoTool(int cw) {
        AutoToolModule t = mm.getModule(AutoToolModule.class);
        if (t == null) return;
        McSection s = new McSection(0, 0, cw, "Авто-Инструмент", 0xFFCC8833, t.enabled)
            .addIf(t.saveTool, t.enabled, true)
            .addIf(t.saveThreshold, t.saveTool, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    // ══════════════════════════════════════════
    // ❤ ВЫЖИВАНИЕ — panic, totem, offhand, eat
    // ══════════════════════════════════════════

    private void buildPanicMode(int cw) {
        PanicModeModule p = mm.getModule(PanicModeModule.class);
        if (p == null) return;
        McSection s = new McSection(0, 0, cw, "Panic Mode", 0xFFFF6688, p.enabled)
            .addIf(p.hpThreshold, p.enabled, true)
            .addIf(p.playerRadius, p.enabled, true)
            .addIf(p.onDamage, p.enabled, true)
            .addIf(p.disconnectDelay, p.enabled, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    private void buildAutoTotem(int cw) {
        AutoTotemModule t = mm.getModule(AutoTotemModule.class);
        if (t == null) return;
        McSection s = new McSection(0, 0, cw, "Авто-Тотем", 0xFFFF6688, t.enabled)
            .addIf(t.hpThreshold, t.enabled, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    private void buildSmartOffhand(int cw) {
        SmartOffhandModule o = mm.getModule(SmartOffhandModule.class);
        if (o == null) return;
        McSection s = new McSection(0, 0, cw, "Smart Offhand", 0xFFFF6688, o.enabled)
            .addIf(o.totemHp, o.enabled, true)
            .addIf(o.foodHunger, o.enabled, true)
            .addIf(o.shieldDefault, o.enabled, true)
            .addIf(o.torchOnPickaxe, o.enabled, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    private void buildAutoEat(int cw) {
        AutoEatModule e = mm.getModule(AutoEatModule.class);
        if (e == null) return;
        McSection s = new McSection(0, 0, cw, "Авто-Еда", 0xFFFF6688, e.enabled)
            .addIf(e.threshold, e.enabled, true)
            .addIf(e.protectRare, e.enabled, true)
            .addIf(e.pauseClicker, e.enabled, true)
            .build();
        s.withReset(saver());
        content.add(s);
    }

    // ══════════════════════════════════════════
    // 🤖 АВТО — independent automation
    // ══════════════════════════════════════════

    private void buildAutoFish(int cw) {
        AutoFishModule f = mm.getModule(AutoFishModule.class);
        if (f == null) return;
        Setting.BooleanSetting master = f.enabled;
        McSection s = new McSection(0, 0, cw, "Авто-Рыбалка", 0xFF44CC66, master);
        for (Setting<?> st : f.getSettings()) {
            if (st != master) s.addIf(st, master, true);
        }
        s.build().withReset(saver());
        content.add(s);
    }

    private void buildAntiAfk(int cw) {
        AntiAfkModule a = mm.getModule(AntiAfkModule.class);
        if (a == null) return;

        McSection s1 = new McSection(0, 0, cw, "Анти-АФК", 0xFF44CC66, a.enabled)
            .addIf(a.minInterval, a.enabled, true)
            .addIf(a.maxInterval, a.enabled, true)
            .build();
        s1.withReset(saver());
        content.add(s1);

        McSection s2 = new McSection(0, 0, cw, "Действия АФК", 0xFF44CC66, null)
            .add(a.shiftAction)
            .add(a.stepAction)
            .add(a.swingAction)
            .add(a.hotbarAction)
            .add(a.lookJitter)
            .addIf(a.lookAmplitude, a.lookJitter, true)
            .add(a.chatMessage)
            .build();
        s2.withDim(() -> !a.enabled.get(), "Включите Анти-АФК выше");
        s2.withReset(saver());
        content.add(s2);
    }

    private void buildAutoWalk(int cw) {
        AutoWalkModule w = mm.getModule(AutoWalkModule.class);
        if (w == null) return;
        Setting.BooleanSetting master = w.enabled;
        McSection s = new McSection(0, 0, cw, "Авто-Ходьба", 0xFF44CC66, master);
        for (Setting<?> st : w.getSettings()) {
            if (st != master) s.addIf(st, master, true);
        }
        s.build().withReset(saver());
        content.add(s);
    }

    private void buildTrashDrop(int cw) {
        TrashDropModule t = mm.getModule(TrashDropModule.class);
        if (t == null) return;
        Setting.BooleanSetting master = t.enabled;
        McSection s = new McSection(0, 0, cw, "Авто-Дроп", 0xFF44CC66, master);
        for (Setting<?> st : t.getSettings()) {
            if (st != master) s.addIf(st, master, true);
        }
        s.build().withReset(saver());
        content.add(s);
    }

    // ══════════════════════════════════════════
    // 📊 HUD
    // ══════════════════════════════════════════

    private void buildHud(int cw) {
        HudModule h = mm.getModule(HudModule.class);
        if (h == null) return;
        Setting.BooleanSetting master = h.enabled;
        McSection s = new McSection(0, 0, cw, "HUD Оверлей", 0xFFAA66FF, master);
        for (Setting<?> st : h.getSettings()) {
            if (st != master) s.addIf(st, master, true);
        }
        s.build().withReset(saver());
        content.add(s);
    }

    // ══════════════════════════════════════════
    // Rendering
    // ══════════════════════════════════════════

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        openAnim = Math.min(1f, openAnim + dt * 0.07f);
        float e = 1f - (1f - openAnim) * (1f - openAnim);
        McWidget.fill(g, 0, 0, width, height, ((int)(e * 0xE0) << 24) | 0x000A0A14);

        rHead(g, mx, my, dt);
        if (nav != null) nav.render(g, mx, my, dt);
        if (content != null) content.render(g, mx, my, dt);
        rFoot(g, mx, my);
        rSearch(g, mx, my);
    }

    private void rHead(GuiGraphics g, int mx, int my, float dt) {
        McWidget.gradV(g, 0, 0, width, HEAD, 0xFF111128, 0xFF0D0D20);
        McWidget.fill(g, 0, HEAD - 1, width, HEAD, 0xFF1E1E40);
        McWidget.fill(g, 0, 0, width, 1, 0x14FFFFFF);
        g.drawString(font, "MultiClicker", 10, (HEAD - 9) / 2, 0xFFE2E2EE, true);

        // ON/OFF pill
        boolean on = ac.isActiveState();
        String st = on ? "ON" : "OFF";
        int stX = 10 + font.width("MultiClicker") + 10;
        int pillW = font.width(st) + 14;
        int pillY = (HEAD - 14) / 2;
        McWidget.pill(g, stX, pillY, pillW, 14, on ? 0x2855FF99 : 0x14FFFFFF);
        McWidget.circle(g, stX + 5, HEAD / 2, 3, on ? 0xFF55FF99 : 0xFF666688);
        g.drawString(font, st, stX + 11, (HEAD - 9) / 2, on ? 0xFF55FF99 : 0xFF666688, false);

        // Accent dots
        int dX = stX + pillW + 12;
        int[] accs = {0xFF5599FF, 0xFF44CC66, 0xFFFF5577, 0xFFAA66FF, 0xFFFFAA33, 0xFF44DDDD};
        for (int i = 0; i < accs.length; i++) {
            int cxx = dX + i * 13;
            boolean cur = accs[i] == ac.getAccentColor();
            McWidget.circle(g, cxx, HEAD / 2, cur ? 5 : 3, accs[i]);
            if (cur) McWidget.circle(g, cxx, HEAD / 2, 6,
                (0x28 << 24) | (accs[i] & 0x00FFFFFF));
        }

        // Help
        int hx = width - 22;
        boolean hh = mx >= hx - 5 && mx <= hx + 12 && my >= 4 && my < HEAD - 4;
        McWidget.panel(g, hx - 4, (HEAD - 14) / 2, 14, 14,
            hh ? 0xFF1C1C38 : 0x00000000, hh ? McWidget.accent() : 0xFF2A2A50);
        g.drawString(font, "?", hx, (HEAD - 9) / 2, hh ? McWidget.accent() : 0xFF7777AA, false);
    }

    private void rFoot(GuiGraphics g, int mx, int my) {
        int fy = height - FOOT;
        McWidget.fill(g, 0, fy, width, height, 0xFF0A0A14);
        McWidget.fill(g, 0, fy, width, fy + 1,
            (0x18 << 24) | (McWidget.accent() & 0x00FFFFFF));
        String tps = "TPS " + String.format("%.1f", ClientTPS.getTPS());
        String ping = "Ping " + ClientPing.getPing() + "ms";
        g.drawString(font, tps, 10, fy + 5, 0xFF8888BB, false);
        int sx = 10 + font.width(tps) + 8;
        McWidget.fill(g, sx, fy + 4, sx + 1, fy + FOOT - 4, 0x25FFFFFF);
        g.drawString(font, ping, sx + 6, fy + 5, 0xFF8888BB, false);
        String ver = "v1.0";
        g.drawString(font, ver, width - font.width(ver) - 10, fy + 5, 0xFF555577, false);
    }

    private void rSearch(GuiGraphics g, int mx, int my) {
        blink++;
        int sw = 110, sh = 14, sx = width - sw - 36, sy = (HEAD - sh) / 2;
        McWidget.panel(g, sx, sy, sw, sh,
            searchOn ? 0xC0181830 : 0x40181830,
            searchOn ? McWidget.accent() : 0x22FFFFFF);
        if (query.isEmpty() && !searchOn)
            g.drawString(font, "Поиск...", sx + 5, sy + 3, 0xFF404065, false);
        else
            g.drawString(font, query, sx + 5, sy + 3, 0xFFE2E2EE, false);
        if (searchOn && (blink / 20) % 2 == 0)
            McWidget.fill(g, sx + 5 + font.width(query), sy + 2,
                sx + 6 + font.width(query), sy + sh - 2, 0xFFE2E2EE);
    }

    // ══════════════════════════════════════════
    // Input
    // ══════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Help
        int hx = width - 22;
        if (mx >= hx - 5 && mx <= hx + 12 && my >= 4 && my < HEAD - 4 && btn == 0) {
            minecraft.setScreen(new HelpScreen(this));
            return true;
        }

        // Accent dots
        if (my < HEAD && btn == 0) {
            boolean on = ac.isActiveState();
            String st = on ? "ON" : "OFF";
            int stX = 10 + font.width("MultiClicker") + 10;
            int pillW = font.width(st) + 14;
            int dX = stX + pillW + 12;
            int[] accs = {0xFF5599FF, 0xFF44CC66, 0xFFFF5577, 0xFFAA66FF, 0xFFFFAA33, 0xFF44DDDD};
            for (int i = 0; i < accs.length; i++) {
                int cxx = dX + i * 13;
                if (mx >= cxx - 6 && mx <= cxx + 6 && my >= HEAD / 2.0 - 6 && my <= HEAD / 2.0 + 6) {
                    ac.setAccentColor(accs[i]);
                    ac.saveConfig();
                    return true;
                }
            }
        }

        // Search
        int sw = 110, sh = 14, sxf = width - sw - 36, sy = (HEAD - sh) / 2;
        if (mx >= sxf && mx < sxf + sw && my >= sy && my < sy + sh) {
            searchOn = true;
            return true;
        } else if (my < HEAD) {
            searchOn = false;
        }

        // Nav
        if (nav != null && mx >= 0 && mx < NAV_W && my >= HEAD && my < height - FOOT) {
            if (nav.mouseClicked(mx, my, btn)) return true;
        }

        // Content
        if (content != null && mx >= NAV_W && my >= HEAD && my < height - FOOT) {
            if (content.mouseClicked(mx, my, btn)) return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (content != null && content.mouseReleased(mx, my, btn)) return true;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (content != null && content.mouseDragged(mx, my, btn, dx, dy)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dh, double dv) {
        if (nav != null && mx < NAV_W && nav.mouseScrolled(mx, my, dh, dv)) return true;
        if (content != null && content.mouseScrolled(mx, my, dh, dv)) return true;
        return super.mouseScrolled(mx, my, dh, dv);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (content != null && content.keyPressed(key, scan, mods)) return true;
        if (key == 256) {
            if (searchOn) { searchOn = false; query = ""; return true; }
            onClose();
            return true;
        }
        if (searchOn && key == 259 && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (content != null && content.charTyped(c, mods)) return true;
        if (searchOn && c >= 32) { query += c; return true; }
        return super.charTyped(c, mods);
    }

    @Override
    public void onClose() {
        ac.saveConfig();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
