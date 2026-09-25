package pro.mikey.autoclicker.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.CombatStats;
import pro.mikey.autoclicker.Language;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import pro.mikey.autoclicker.modules.combat.SmartCombatModule;
import pro.mikey.autoclicker.modules.combat.AntiCheatModule;
import pro.mikey.autoclicker.modules.combat.NetworkGuardModule;
import pro.mikey.autoclicker.ClientPing;
import pro.mikey.autoclicker.ClientTPS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import pro.mikey.autoclicker.ui.McWidget;

/**
 * HudModule — owns HUD overlay rendering and ESP settings.
 * Features: configurable position, arraylist, watermark.
 */
public class HudModule implements Module {

    // ── Enums ──
    public enum EspMode {
        OFF("Выкл"), BOX("Рамка"), FILLED("Заливка"), CIRCLE("Круг"),
        ARROW("Стрела"), BEACON("Маяк"), GLOW("Свечение"), CYLINDER("Цилиндр"), CONE("Конус");

        public final String label;

        EspMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum EspColor {
        GREEN("Зелёный", 0x55FF55),
        RED("Красный", 0xFF5555),
        BLUE("Синий", 0x5555FF),
        CYAN("Голубой", 0x55FFFF),
        YELLOW("Жёлтый", 0xFFFF55),
        ORANGE("Оранж.", 0xFFAA33),
        PINK("Розовый", 0xFF55FF),
        WHITE("Белый", 0xFFFFFF),
        PURPLE("Фиолет.", 0xAA55FF),
        AQUA("Бирюза", 0x33FFCC);

        public final String label;
        public final int rgb;

        EspColor(String label, int rgb) {
            this.label = label;
            this.rgb = rgb;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum HudPosition {
        TOP_LEFT("Верх-лево"), TOP_RIGHT("Верх-право"),
        BOTTOM_LEFT("Низ-лево"), BOTTOM_RIGHT("Низ-право");

        public final String label;

        HudPosition(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ── Settings ──
    public final BooleanSetting enabled = new BooleanSetting("hud.enabled", "HUD Включён", true)
            .withGroup("📊 Оверлей").withDescription("Показывает инфо-оверлей в игре. Отключите для чистого экрана");
    public final EnumSetting<HudPosition> hudPosition = new EnumSetting<>("hud.position", "Позиция HUD",
            HudPosition.TOP_LEFT, HudPosition.class)
            .withGroup("📊 Оверлей").withDescription("Угол экрана для отображения HUD оверлея");
    public final BooleanSetting statsEnabled = new BooleanSetting("hud.stats", "Статистика", true)
            .withGroup("📊 Оверлей").withDescription("CPS, убийства, пинг, TPS в реальном времени на экране");
    public final BooleanSetting profileInfo = new BooleanSetting("hud.profile_info", "Профиль Info", false)
            .withGroup("📊 Оверлей").withDescription("Показывает имя текущего профиля настроек на HUD");
    public final BooleanSetting arraylistEnabled = new BooleanSetting("hud.arraylist", "Arraylist", false)
            .withGroup("📊 Оверлей").withDescription("Список активных настроек на HUD. Сортируется по ширине текста");
    public final BooleanSetting watermarkEnabled = new BooleanSetting("hud.watermark", "Watermark", true)
            .withGroup("📊 Оверлей").withDescription("Тонкий бренд 'MultiClicker' на экране");
    public final EnumSetting<EspMode> espMode = new EnumSetting<>("hud.esp_mode", "ESP Режим", EspMode.OFF,
            EspMode.class).withGroup("👁 ESP")
            .withDescription("Визуальная подсветка цели: 9 режимов отображения");
    public final FloatSetting hudScale = new FloatSetting("hud.scale", "Масштаб HUD", 1.0, 0.5, 2.0)
            .withGroup("📊 Оверлей").withDescription("Масштаб HUD оверлея. 1.0 = стандарт, 0.5 = мелкий, 2.0 = крупный");
    public final EnumSetting<EspColor> espColor = new EnumSetting<>("hud.esp_color", "Цвет ESP", EspColor.GREEN,
            EspColor.class).withGroup("👁 ESP")
            .withDescription("Цвет ESP подсветки. Переключайте для выбора");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, hudPosition, statsEnabled, profileInfo, arraylistEnabled, watermarkEnabled,
                espMode, hudScale, espColor);
    }

    @Override
    public String getId() {
        return "hud";
    }

    @Override
    public String getDisplayName() {
        return "HUD";
    }

    @Override
    public Category getCategory() {
        return Category.RENDER;
    }

    @Override
    public int tickPriority() {
        return -1;
    } // No tick — rendering only

    @Override
    public boolean onTick(Minecraft mc) {
        return false;
    }

    // ══════════════════════════════════════════════════════════════
    // Rendering
    // ══════════════════════════════════════════════════════════════

    public void render(GuiGraphics context, net.minecraft.client.DeltaTracker delta) {
        if (!enabled.get())
            return;
        AutoClicker instance = AutoClicker.getInstance();
        if (instance == null)
            return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null)
            return;

        HudPosition pos = hudPosition.get();
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        boolean isRight = (pos == HudPosition.TOP_RIGHT || pos == HudPosition.BOTTOM_RIGHT);
        boolean isBottom = (pos == HudPosition.BOTTOM_LEFT || pos == HudPosition.BOTTOM_RIGHT);

        // ── Watermark ──
        int lineIndex = 0;
        if (watermarkEnabled.get()) {
            int accent = instance.getAccentColor() & 0xFFFFFF;
            Component wmComp = Component.empty()
                    .append(Component.literal("⚡ ").withStyle(Style.EMPTY.withColor(accent)))
                    .append(Component.literal("MultiClicker ").withStyle(Style.EMPTY.withColor(0xDDDDEE)))
                    .append(Component.literal("v1.0").withStyle(Style.EMPTY.withColor(0x777788)));
            drawHudLine(context, client, wmComp, lineIndex++, screenW, screenH, isRight, isBottom);
        }

        boolean isActive = instance.isActiveState();
        CombatClickerModule clicker = instance.getModuleManager()
                .<CombatClickerModule>get("combat_clicker").orElse(null);
        SmartCombatModule smartCombat = instance.getModuleManager()
                .<SmartCombatModule>get("smart_combat").orElse(null);
        AntiCheatModule antiCheat = instance.getModuleManager()
                .<AntiCheatModule>get("anti_cheat").orElse(null);
        NetworkGuardModule networkGuard = instance.getModuleManager()
                .<NetworkGuardModule>get("network_guard").orElse(null);
        MiningModule miningMod = instance.getModuleManager()
                .<MiningModule>get("mining").orElse(null);

        if (isActive && clicker != null) {
            if (miningMod != null && miningMod.miningMode.get()) {
                drawHudLine(context, client, Component.literal("§6● §f⛏ Mining"), lineIndex++,
                        screenW, screenH, isRight, isBottom);
            }
            if (clicker.leftActive.get()) {
                drawHudLine(context, client, Language.HUD_HOLDING.getText(I18n.get(client.options.keyAttack.getName())),
                        lineIndex++, screenW, screenH, isRight, isBottom);
            }
            if (clicker.rightActive.get()) {
                drawHudLine(context, client, Language.HUD_HOLDING.getText(I18n.get(client.options.keyUse.getName())),
                        lineIndex++, screenW, screenH, isRight, isBottom);
            }
            if (clicker.jumpActive.get()) {
                drawHudLine(context, client, Language.HUD_HOLDING.getText(I18n.get(client.options.keyJump.getName())),
                        lineIndex++, screenW, screenH, isRight, isBottom);
            }
        }

        // Stats line
        if (statsEnabled.get()) {
            CombatStats stats = instance.getCombatStats();
            String dpsStr = formatStat2(stats.getDPS());
            String kpmStr = formatStat2(stats.getKPM());
            String tpsStr = formatStat1(ClientTPS.getTPS());
            int rawPing = ClientPing.getPing();
            String pingStr = rawPing >= 0 ? String.valueOf(rawPing) : "?";
            long duration = isActive ? System.currentTimeMillis() - instance.getStartTime() : 0;
            String timeStr = formatTime(duration);

            Component text = Component.literal(
                    "§7☠ §f" + stats.getKillCount() +
                            "  §7⚔ §f" + dpsStr + "/s" +
                            "  §7⏱ §f" + kpmStr + "/m" +
                            "  §7⏳ §f" + timeStr +
                            "  §7◈ §f" + tpsStr +
                            "  §7📡 §f" + pingStr + "ms");
            drawHudLine(context, client, text, lineIndex++, screenW, screenH, isRight, isBottom);
        }

        // Profile info
        if (profileInfo.get() && clicker != null) {
            StringBuilder sb = new StringBuilder();
            if (clicker.respectCooldown.get()) {
                sb.append("§aCooldown");
            } else {
                sb.append("§eSpeed §f").append(clicker.leftSpeed.get()).append("t");
            }
            if (clicker.leftClickMode.get() == CombatClickerModule.ClickMode.HOLD)
                sb.append(" §7| §bHold");
            if (clicker.leftRandomize.get())
                sb.append(" §7| §dRand §f0..").append(clicker.leftRandomizeRange.get());
            if (clicker.mobMode.get())
                sb.append(" §7| §cMobs");
            if (clicker.respectShield.get())
                sb.append(" §7| §9Shield");
            if (antiCheat != null && antiCheat.entityProtection.get())
                sb.append(" §7| §6SrvC");
            if (networkGuard != null && networkGuard.enabled.get() && networkGuard.tpsSync.get())
                sb.append(" §7| §bTSync");
            drawHudLine(context, client, Component.literal("§7⚙ §f" + sb), lineIndex++,
                    screenW, screenH, isRight, isBottom);
        }

        // ── Arraylist (active settings display) ──
        if (arraylistEnabled.get() && isActive && clicker != null) {
            lineIndex++; // spacer
            List<String> entries = new ArrayList<>();
            if (clicker.respectCooldown.get())
                entries.add("§aCooldown");
            if (antiCheat != null && antiCheat.gcdPatch.get())
                entries.add("§bGCD " + antiCheat.pollingRate.get());
            if (networkGuard != null && networkGuard.enabled.get() && networkGuard.tpsSync.get())
                entries.add("§bTPS Sync");
            if (clicker.leftRandomize.get())
                entries.add("§dRandom " + clicker.leftRandomizeRange.get());
            if (clicker.mobMode.get())
                entries.add("§cMob Mode");
            if (antiCheat != null && antiCheat.entityProtection.get())
                entries.add("§6Entity Protect");
            if (clicker.respectShield.get())
                entries.add("§9Shield");
            if (antiCheat != null && antiCheat.cameraLock.get())
                entries.add("§eCam Lock");
            if (smartCombat != null && smartCombat.lootingSwapper.get())
                entries.add("§aLooting");

            // Sort by text width descending for aesthetic stacking
            entries.sort(Comparator.comparingInt(s -> -client.font.width(s)));

            for (String entry : entries) {
                drawHudLine(context, client, Component.literal(entry), lineIndex++,
                        screenW, screenH, isRight, isBottom);
            }
        }
    }

    // ── Helper methods ──

    private void drawHudLine(GuiGraphics context, Minecraft client, Component text,
            int lineIndex, int screenW, int screenH, boolean isRight, boolean isBottom) {
        int lineHeight = 14;
        int margin = 6;
        int textWidth = client.font.width(text);
        int padding = 4;

        int lx, ly;
        if (isRight) {
            lx = screenW - textWidth - margin - padding;
        } else {
            lx = margin;
        }
        if (isBottom) {
            ly = screenH - margin - lineHeight - (lineHeight * lineIndex);
        } else {
            ly = margin + 28 + (lineHeight * lineIndex);
        }

        // Background pill
        McWidget.fill(context, lx - padding, ly - 2, lx + textWidth + padding, ly + 10, 0x60000000);

        int accentAlpha = 0xAA000000;
        if (AutoClicker.getInstance() != null) {
            accentAlpha = 0xAA000000 | (AutoClicker.getInstance().getAccentColor() & 0x00FFFFFF);
        }
        // Accent bar on the side
        if (isRight) {
            McWidget.fill(context, lx + textWidth + padding - 1, ly - 2, lx + textWidth + padding, ly + 10,
                    accentAlpha);
        } else {
            McWidget.fill(context, lx - padding, ly - 2, lx - padding + 1, ly + 10, accentAlpha);
        }
        context.drawString(client.font, text, lx, ly, 0xFFFFFFFF, true);
    }

    private String formatStat2(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.format("%.2f", value);
    }

    private String formatStat1(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.format("%.1f", value);
    }

    private String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
