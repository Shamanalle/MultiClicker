package pro.mikey.autoclicker.modules.combat;

import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Anti-Cheat module — all settings for bypassing server-side anti-cheat.
 * Advanced randomization, GCD patch, movement protection, entity protection.
 */
public class AntiCheatModule implements Module {

        // ── Enums ──
        public enum PollingRate {
                HZ_125("125 Hz", 125), HZ_250("250 Hz", 250), HZ_500("500 Hz", 500), HZ_1000("1000 Hz", 1000);

                public final String label;
                public final int hz;

                PollingRate(String label, int hz) {
                        this.label = label;
                        this.hz = hz;
                }

                @Override
                public String toString() {
                        return label;
                }
        }

        // ── Продвинутая рандомизация ──
        public final BooleanSetting advancedRandomEnabled = new BooleanSetting("anticheat.advanced_random_enabled",
                        "Продвинутая рандомизация", false)
                        .withGroup("🎲 Продвинутая рандомизация")
                        .withDescription("Включает продвинутые алгоритмы рандомизации для обхода серверных античитов");
        public final BooleanSetting gaussianRandom = new BooleanSetting("anticheat.gaussian", "Гауссов рандом", false)
                        .withGroup("🎲 Продвинутая рандомизация")
                        .withDescription("Нормальное распределение вместо равномерного. Ближе к поведению человека");
        public final FloatSetting randomSigma = new FloatSetting("anticheat.random_sigma", "Sigma", 1.0, 0.1, 5.0)
                        .withGroup("🎲 Продвинутая рандомизация")
                        .withDescription("Ширина Гауссова распределения. Больше = больше разброс задержки");
        public final BooleanSetting gcdPatch = new BooleanSetting("anticheat.gcd_patch", "GCD Патч", false)
                        .withGroup("🎲 Продвинутая рандомизация")
                        .withDescription("Квантует интервалы к polling rate мыши. Обход GCD-анализа Grim/Vulcan");
        public final EnumSetting<PollingRate> pollingRate = new EnumSetting<>("anticheat.polling_rate", "Polling Rate",
                        PollingRate.HZ_1000, PollingRate.class)
                        .withGroup("🎲 Продвинутая рандомизация")
                        .withDescription("Имитируемая частота опроса мыши для обхода GCD-анализа");
        public final IntSetting skipChance = new IntSetting("anticheat.skip_chance", "Шанс пропуска %", 0, 0, 30)
                        .withGroup("🎲 Продвинутая рандомизация")
                        .withDescription("Вероятность пропустить удар. Имитирует 'промах' для обхода статистики");

        // ── Защита движения и камеры ──
        public final BooleanSetting movementProtection = new BooleanSetting("anticheat.movement_protection",
                        "Защита движения", false)
                        .withGroup("🏃 Защита движения")
                        .withDescription(
                                        "Не бьёт на бегу. Атакует только когда стоишь на месте");
        public final IntSetting movementDelay = new IntSetting("anticheat.movement_delay", "Задержка (тики)", 0, 0,
                        100)
                        .withGroup("🏃 Защита движения")
                        .withDescription(
                                        "Пауза после остановки перед первым ударом. 0 = бьёт сразу как остановился");
        public final BooleanSetting rotationProtection = new BooleanSetting("anticheat.rotation_protection",
                        "Защита камеры", false)
                        .withGroup("🏃 Защита движения")
                        .withDescription(
                                        "Не бьёт при резком повороте мыши, если ты не смотришь на моба. Если моб в прицеле — бьёт как обычно");
        public final BooleanSetting cameraLock = new BooleanSetting("anticheat.camera_lock", "Camera Lock", false)
                        .withGroup("🏃 Защита движения")
                        .withDescription(
                                        "Замораживает камеру — мышь не двигает обзор, пока кликер работает. Прицел остаётся на месте");

        // ── Защита сущностей ──
        public final BooleanSetting entityProtection = new BooleanSetting("anticheat.entity_protection",
                        "Entity Protection", true)
                        .withGroup("👁 Entity Protection")
                        .withDescription("Сервер отслеживает атаки по EntityID. Добавляет паузу при смене цели");
        public final IntSetting targetDelay = new IntSetting("anticheat.target_delay", "Target Delay", 5, 0, 40)
                        .withGroup("👁 Entity Protection")
                        .withDescription("Имитация реакции: пауза в тиках перед первой атакой новой цели");
        public final IntSetting entityProtTimeout = new IntSetting("anticheat.entity_prot_timeout", "EP Таймаут (с)",
                        10, 0, 60)
                        .withGroup("👁 Entity Protection")
                        .withDescription("Через сколько секунд считать сущность 'новой'. 0 = без таймаута");

        @Override
        public List<Setting<?>> getSettings() {
                return List.of(advancedRandomEnabled, gaussianRandom, randomSigma, gcdPatch, pollingRate, skipChance,
                                movementProtection, movementDelay, rotationProtection, cameraLock,
                                entityProtection, targetDelay, entityProtTimeout);
        }

        @Override
        public String getId() {
                return "anti_cheat";
        }

        @Override
        public String getDisplayName() {
                return "Античит";
        }

        @Override
        public Category getCategory() {
                return Category.PROTECTION;
        }

        @Override
        public int tickPriority() {
                return 200;
        }
}
