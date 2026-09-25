package pro.mikey.autoclicker.modules.combat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import pro.mikey.autoclicker.util.InventoryUtils;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import pro.mikey.autoclicker.core.EventBus;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;
import pro.mikey.autoclicker.mixin.InventoryAccessor;
import pro.mikey.autoclicker.ClientPing;
import pro.mikey.autoclicker.ClientTPS;
import pro.mikey.autoclicker.modules.automation.AutoEatModule;
import pro.mikey.autoclicker.modules.world.MiningModule;
import pro.mikey.autoclicker.modules.world.MiningFilterModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * CombatClickerModule — core auto-clicker engine.
 *
 * <p>
 * Owns click-timing state machine for LMB / RMB / Jump.
 * Each button has independent randomization settings.
 * Advanced anti-cheat, network, mining, and smart combat settings
 * live in dedicated modules; this class reads them at runtime.
 */
public class CombatClickerModule implements Module {

    private static final Random RANDOM = new Random();
    private static final int OFFHAND_INVENTORY_SLOT = 40;

    // ── Enums ──
    public enum ClickMode {
        CLICK("Click"), HOLD("Hold");

        public final String label;

        ClickMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Settings — organized by button, each with own randomization
    // ══════════════════════════════════════════════════════════════

    // ── ЛКМ (Left Mouse Button) ──
    public final BooleanSetting leftActive = new BooleanSetting("clicker.left.active", "Автоклик", false)
            .withGroup("🖱 ЛКМ (Атака)")
            .withDescription("Включает автоклик левой кнопкой. Атака и копка");
    public final EnumSetting<ClickMode> leftClickMode = new EnumSetting<>("clicker.left.spamming", "Режим",
            ClickMode.CLICK, ClickMode.class)
            .withGroup("🖱 ЛКМ (Атака)")
            .withDescription("Click = быстрые нажатия, Hold = удержание кнопки");
    public final IntSetting leftSpeed = new IntSetting("clicker.left.speed", "Скорость", 0, 0, 100)
            .withGroup("🖱 ЛКМ (Атака)")
            .withDescription("Задержка между кликами в тиках. 0 = каждый тик (20 CPS)");
    public final IntSetting leftHoldDuration = new IntSetting("clicker.left.hold_duration", "Hold тиков", 1, 1, 200)
            .withGroup("🖱 ЛКМ (Атака)")
            .withDescription("Сколько тиков удерживать кнопку в Hold-режиме");
    public final BooleanSetting leftRandomize = new BooleanSetting("clicker.left.randomize", "Рандомизация", false)
            .withGroup("🖱 ЛКМ (Атака)")
            .withDescription("Добавляет случайную задержку к каждому клику ЛКМ для обхода античита");
    public final IntSetting leftRandomizeRange = new IntSetting("clicker.left.randomize_range", "Диапазон рандома", 4,
            0, 40)
            .withGroup("🖱 ЛКМ (Атака)")
            .withDescription("Макс. добавляемая задержка в тиках для ЛКМ");

    // ── ПКМ (Right Mouse Button) ──
    public final BooleanSetting rightActive = new BooleanSetting("clicker.right.active", "Автоклик", false)
            .withGroup("🖱 ПКМ (Использование)")
            .withDescription("Включает автоклик правой кнопкой. Использование, еда, блоки");
    public final EnumSetting<ClickMode> rightClickMode = new EnumSetting<>("clicker.right.spamming", "Режим",
            ClickMode.CLICK, ClickMode.class)
            .withGroup("🖱 ПКМ (Использование)")
            .withDescription("Click = быстрые нажатия, Hold = удержание кнопки");
    public final IntSetting rightSpeed = new IntSetting("clicker.right.speed", "Скорость", 0, 0, 100)
            .withGroup("🖱 ПКМ (Использование)")
            .withDescription("Задержка между кликами в тиках. 0 = каждый тик");
    public final IntSetting rightHoldDuration = new IntSetting("clicker.right.hold_duration", "Hold тиков", 1, 1, 200)
            .withGroup("🖱 ПКМ (Использование)")
            .withDescription("Сколько тиков удерживать кнопку в Hold-режиме");
    public final BooleanSetting rightRandomize = new BooleanSetting("clicker.right.randomize", "Рандомизация", false)
            .withGroup("🖱 ПКМ (Использование)")
            .withDescription("Добавляет случайную задержку к каждому клику ПКМ");
    public final IntSetting rightRandomizeRange = new IntSetting("clicker.right.randomize_range", "Диапазон рандома", 4,
            0, 40)
            .withGroup("🖱 ПКМ (Использование)")
            .withDescription("Макс. добавляемая задержка в тиках для ПКМ");

    // ── Прыжок (Jump) ──
    public final BooleanSetting jumpActive = new BooleanSetting("clicker.jump.active", "Авто-прыжок", false)
            .withGroup("⬆ Прыжок")
            .withDescription("Включает авто-прыжок. Полезно для PvP и фарма");
    public final EnumSetting<ClickMode> jumpClickMode = new EnumSetting<>("clicker.jump.spamming", "Режим",
            ClickMode.CLICK, ClickMode.class)
            .withGroup("⬆ Прыжок")
            .withDescription("Click = повторные прыжки, Hold = удержание пробела");
    public final IntSetting jumpSpeed = new IntSetting("clicker.jump.speed", "Скорость", 0, 0, 100)
            .withGroup("⬆ Прыжок")
            .withDescription("Задержка между прыжками в тиках. 0 = прыгает каждый тик");
    public final IntSetting jumpHoldDuration = new IntSetting("clicker.jump.hold_duration", "Hold тиков", 1, 1, 200)
            .withGroup("⬆ Прыжок")
            .withDescription("Сколько тиков удерживать пробел в Hold-режиме");
    public final BooleanSetting jumpRandomize = new BooleanSetting("clicker.jump.randomize", "Рандомизация", false)
            .withGroup("⬆ Прыжок")
            .withDescription("Добавляет случайную задержку к прыжкам");
    public final IntSetting jumpRandomizeRange = new IntSetting("clicker.jump.randomize_range", "Диапазон рандома", 4,
            0, 40)
            .withGroup("⬆ Прыжок")
            .withDescription("Макс. добавляемая задержка в тиках для прыжков");

    // ── Базовые боевые настройки ──
    public final BooleanSetting respectCooldown = new BooleanSetting("clicker.respect_cooldown", "Кулдаун", true)
            .withGroup("⚔ Боевые настройки")
            .withDescription("Ждёт перезарядку атаки (1.9+). Отключите для серверов 1.8 PvP");
    public final BooleanSetting respectShield = new BooleanSetting("clicker.respect_shield", "Щит", false)
            .withGroup("⚔ Боевые настройки")
            .withDescription("Не атакует, пока вы блокируете щитом. Предотвращает случайное снятие блока");
    public final BooleanSetting mobMode = new BooleanSetting("clicker.mob_mode", "Mob Mode", false)
            .withGroup("⚔ Боевые настройки")
            .withDescription("Атакует только мобов (LivingEntity). Игнорирует предметы, лодки, вагонетки");

    // ── Общие настройки кликера ──
    public final BooleanSetting pauseInGui = new BooleanSetting("clicker.pause_in_gui", "Пауза в GUI", true)
            .withGroup("⚙ Общие")
            .withDescription("Приостанавливает кликер при открытом интерфейсе (сундук, инвентарь, верстак)");
    public final IntSetting clickLimit = new IntSetting("clicker.click_limit", "Лимит кликов", 0, 0, 10000)
            .withGroup("⚙ Общие")
            .withDescription("Кликнуть N раз и остановиться. 0 = бесконечно");
    public final IntSetting activationDelay = new IntSetting("clicker.activation_delay", "Задержка старта", 0, 0, 100)
            .withGroup("⚙ Общие")
            .withDescription("Тиков задержки после [I] перед началом кликов. Даёт время навестись на цель");
    public final BooleanSetting toggleSound = new BooleanSetting("clicker.toggle_sound", "Звук вкл/выкл", true)
            .withGroup("⚙ Общие")
            .withDescription("Проигрывает тихий звук при нажатии [I]. Аудио-фидбек вкл/выкл");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(
                leftActive, leftClickMode, leftSpeed, leftHoldDuration, leftRandomize, leftRandomizeRange,
                rightActive, rightClickMode, rightSpeed, rightHoldDuration, rightRandomize, rightRandomizeRange,
                jumpActive, jumpClickMode, jumpSpeed, jumpHoldDuration, jumpRandomize, jumpRandomizeRange,
                respectCooldown, respectShield, mobMode,
                pauseInGui, clickLimit, activationDelay, toggleSound);
    }

    // ══════════════════════════════════════════════════════════════
    // Cached references to companion modules (set in onInit)
    // ══════════════════════════════════════════════════════════════

    private SmartCombatModule smartCombat;
    private AntiCheatModule antiCheat;
    private NetworkGuardModule networkGuard;
    private MiningModule mining;
    private AutoEatModule autoEat;
    private MiningFilterModule miningFilter;

    // ══════════════════════════════════════════════════════════════
    // Runtime state
    // ══════════════════════════════════════════════════════════════

    private int leftTimeout = 0, rightTimeout = 0, jumpTimeout = 0;
    private int leftCooldownDelay = 0;
    private int cooldownBufferTicks = 0;
    private int leftHoldCounter = 0, rightHoldCounter = 0, jumpHoldCounter = 0;
    private boolean leftClickPending = false, rightClickPending = false, jumpClickPending = false;

    // Click limit / activation delay state
    private int totalClickCount = 0;
    private int activationDelayCounter = 0;
    private int movementDelayCounter = 0;

    // Entity protection state
    private int entityProtHitId = -1;
    private long entityProtSentTime = 0;
    public final Map<Integer, Integer> armorStandResilience = new HashMap<>();

    // Target tracking
    private Entity lastTarget;
    private int reactionTimer = 0;
    private float lastYRot, lastXRot;

    // Frame-latched entity (set by MixinGameRenderer)
    private volatile Entity frameLatchedEntity = null;
    private Entity prePickEntity = null;

    // Looting Swapper FSM
    public enum LootingSwapState {
        OFF, PHASE1_SWAP, PHASE2_WAIT, PHASE3_ATTACK
    }

    private LootingSwapState lootingSwapState = LootingSwapState.OFF;
    private int lootingSwapTimer = 0;
    private Entity savedLootingTarget;
    private int originalHotbarSlot = -1;
    private boolean lootingUseOffhand = false;

    // ESP target
    private Entity espTarget = null;

    // ── Module identity ──
    @Override
    public String getId() {
        return "combat_clicker";
    }

    @Override
    public String getDisplayName() {
        return "Кликер";
    }

    @Override
    public Category getCategory() {
        return Category.CLICKER;
    }

    @Override
    public int tickPriority() {
        return 100;
    }

    // ══════════════════════════════════════════════════════════════
    // Public accessors (for mixins)
    // ══════════════════════════════════════════════════════════════

    public void setFrameLatchedEntity(Entity e) {
        this.frameLatchedEntity = e;
    }

    public Entity consumeFrameLatchedEntity() {
        Entity e = frameLatchedEntity;
        frameLatchedEntity = null;
        return e;
    }

    public void setPrePickEntity(Entity e) {
        this.prePickEntity = e;
    }

    public Entity getPrePickEntity() {
        return prePickEntity;
    }

    public Entity getLastTarget() {
        return lastTarget;
    }

    public Entity getEspTarget() {
        return espTarget;
    }

    public void setEspTarget(Entity e) {
        this.espTarget = e;
    }

    public boolean isEntityProtBusy() {
        return entityProtHitId != -1;
    }

    public void recordEntityProtAttack(Entity target) {
        entityProtHitId = target.getId();
        entityProtSentTime = System.currentTimeMillis();
    }

    public boolean isCameraLockActive() {
        return leftActive.get() && antiCheat != null && antiCheat.cameraLock.get();
    }

    public boolean isEntityProtectionEnabled() {
        return antiCheat != null && antiCheat.entityProtection.get();
    }

    // ══════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════

    @Override
    public void onInit(Minecraft mc) {
        // Cache companion module references
        var mgr = pro.mikey.autoclicker.AutoClicker.getInstance().getModuleManager();
        smartCombat = mgr.<SmartCombatModule>get("smart_combat").orElse(null);
        antiCheat = mgr.<AntiCheatModule>get("anti_cheat").orElse(null);
        networkGuard = mgr.<NetworkGuardModule>get("network_guard").orElse(null);
        mining = mgr.<MiningModule>get("mining").orElse(null);
        autoEat = mgr.<AutoEatModule>get("auto_eat").orElse(null);
        miningFilter = mgr.<MiningFilterModule>get("mining_filter").orElse(null);
    }

    @Override
    public void onDisable() {
        resetAllState();
    }

    private void resetAllState() {
        leftTimeout = rightTimeout = jumpTimeout = 0;
        leftCooldownDelay = 0;
        cooldownBufferTicks = 0;
        leftHoldCounter = rightHoldCounter = jumpHoldCounter = 0;
        leftClickPending = rightClickPending = jumpClickPending = false;
        entityProtHitId = -1;
        entityProtSentTime = 0;
        armorStandResilience.clear();
        lootingSwapState = LootingSwapState.OFF;
        lootingSwapTimer = 0;
        savedLootingTarget = null;
        originalHotbarSlot = -1;
        lootingUseOffhand = false;
        reactionTimer = 0;
        lastTarget = null;
        espTarget = null;
        frameLatchedEntity = null;
        prePickEntity = null;
        totalClickCount = 0;
        activationDelayCounter = 0;
        movementDelayCounter = 0;
    }

    public void resetTimeouts() {
        leftTimeout = rightTimeout = jumpTimeout = 0;
        totalClickCount = 0;
        activationDelayCounter = activationDelay.get();
        cooldownBufferTicks = 0;
        reactionTimer = 0;
    }

    public void releaseAllKeys(Minecraft mc) {
        if (leftActive.get())
            mc.options.keyAttack.setDown(false);
        if (rightActive.get())
            mc.options.keyUse.setDown(false);
        if (jumpActive.get())
            mc.options.keyJump.setDown(false);
    }

    // ══════════════════════════════════════════════════════════════
    // onTick — the core loop
    // ══════════════════════════════════════════════════════════════

    @Override
    public boolean onTick(Minecraft mc) {
        if (mc.player == null)
            return false;

        // #1 Pause in GUI
        if (pauseInGui.get() && mc.screen != null) {
            return false;
        }

        // #3 Activation delay
        if (activationDelayCounter > 0) {
            activationDelayCounter--;
            return false;
        }

        // #2 Click limit
        if (clickLimit.get() > 0 && totalClickCount >= clickLimit.get()) {
            releaseAllKeys(mc);
            return false;
        }

        // #5 Pause clicker while eating
        if (autoEat != null && autoEat.isCurrentlyEating()) {
            return false;
        }

        // Looting Swapper FSM
        if (lootingSwapState != LootingSwapState.OFF) {
            handleLootingSwapper(mc);
            if (lootingSwapState != LootingSwapState.OFF)
                return false;
        }

        // Low HP check (from SmartCombatModule)
        if (smartCombat != null && smartCombat.safetyEnabled.get() && smartCombat.healthThreshold.get() > 0) {
            float hpPercent = (mc.player.getHealth() / mc.player.getMaxHealth()) * 100f;
            if (hpPercent < smartCombat.healthThreshold.get()) {
                if (smartCombat.lowHpAction.get() == SmartCombatModule.LowHpAction.DISCONNECT) {
                    pro.mikey.autoclicker.AutoClicker instance = pro.mikey.autoclicker.AutoClicker.getInstance();
                    if (instance != null) {
                        instance.safeDisconnect(mc, "Low HP (" + (int) hpPercent + "%)");
                    }
                    return true;
                } else {
                    mc.options.keyAttack.setDown(false);
                    return false;
                }
            }
        }

        // Mining mode (from MiningModule)
        if (mining != null && mining.miningMode.get()) {
            // #7 Stop when inventory full
            if (mining.stopWhenFull.get() && mc.player.getInventory().getFreeSlot() == -1) {
                mc.options.keyAttack.setDown(false);
                return false;
            }
            // #14 Mining filter check
            if (miningFilter != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(bhr.getBlockPos());
                if (!miningFilter.allows(state)) {
                    mc.options.keyAttack.setDown(false);
                    return false;
                }
            }
            mc.options.keyAttack.setDown(true);
            if (rightActive.get())
                handleKey(mc, mc.options.keyUse, rightClickMode.get() == ClickMode.CLICK, rightSpeed.get(),
                        rightHoldDuration.get(), rightRandomize.get(), rightRandomizeRange.get(), false);
            if (jumpActive.get())
                handleKey(mc, mc.options.keyJump, jumpClickMode.get() == ClickMode.CLICK, jumpSpeed.get(),
                        jumpHoldDuration.get(), jumpRandomize.get(), jumpRandomizeRange.get(), true);
            return false;
        }

        // Normal attack logic
        if (leftActive.get()) {
            handleAttackKey(mc);
        }
        if (rightActive.get()) {
            handleKey(mc, mc.options.keyUse, rightClickMode.get() == ClickMode.CLICK, rightSpeed.get(),
                    rightHoldDuration.get(), rightRandomize.get(), rightRandomizeRange.get(), false);
        }
        if (jumpActive.get()) {
            handleKey(mc, mc.options.keyJump, jumpClickMode.get() == ClickMode.CLICK, jumpSpeed.get(),
                    jumpHoldDuration.get(), jumpRandomize.get(), jumpRandomizeRange.get(), true);
        }

        // Update entity protection
        updateEntityProtection(mc);

        // Update rotation history
        lastYRot = mc.player.getYRot();
        lastXRot = mc.player.getXRot();

        return false;
    }

    // ══════════════════════════════════════════════════════════════
    // Attack key handling
    // ══════════════════════════════════════════════════════════════

    private void handleAttackKey(Minecraft mc) {
        KeyMapping key = mc.options.keyAttack;

        // Save Tool (from MiningModule) — only in mining mode
        if (mining != null && mining.miningMode.get() && mining.saveToolEnabled.get()) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (mainHand.isDamageableItem()) {
                int durability = mainHand.getMaxDamage() - mainHand.getDamageValue();
                if (durability <= mining.saveToolThreshold.get()) {
                    key.setDown(false);
                    return;
                }
            }
        }

        // Movement Protection (from AntiCheatModule) with #8 delay
        if (antiCheat != null && antiCheat.movementProtection.get()) {
            var delta = mc.player.getDeltaMovement();
            boolean isMoving = Math.abs(delta.x) > 0.01 || Math.abs(delta.z) > 0.01 || mc.player.isSprinting();
            if (isMoving) {
                int delay = antiCheat.movementDelay.get();
                if (delay > 0 && movementDelayCounter < delay) {
                    // Grace period — allow attack, but keep counting
                    movementDelayCounter++;
                } else {
                    // Grace expired (or no delay) — block attack while moving
                    key.setDown(false);
                    return;
                }
            } else {
                movementDelayCounter = 0;
            }
        }

        // Rotation Protection (from AntiCheatModule)
        if (antiCheat != null && antiCheat.rotationProtection.get()) {
            float deltaY = Math.abs(mc.player.getYRot() - lastYRot);
            float deltaX = Math.abs(mc.player.getXRot() - lastXRot);
            if (deltaY > 2.0f || deltaX > 2.0f) {
                if (!hasEntityTarget(mc)) {
                    key.setDown(false);
                    return;
                }
            }
        }

        // Target Acquisition Delay (from AntiCheatModule)
        int tDelay = antiCheat != null ? antiCheat.targetDelay.get() : 0;
        if (tDelay > 0) {
            Entity currentTarget = (mc.hitResult instanceof EntityHitResult ehr) ? ehr.getEntity() : null;
            if (currentTarget != lastTarget) {
                lastTarget = currentTarget;
                if (currentTarget instanceof LivingEntity) {
                    int randomPart = leftRandomize.get() ? RANDOM.nextInt(Math.max(1, leftRandomizeRange.get())) : 0;
                    reactionTimer = tDelay + randomPart;
                }
            }
            if (reactionTimer > 0) {
                reactionTimer--;
                key.setDown(false);
                return;
            }
        }

        // Click pending (release after 1 tick)
        if (leftClickPending) {
            key.setDown(false);
            leftClickPending = false;
            if (mc.gameMode != null)
                mc.gameMode.stopDestroyBlock();
            return;
        }

        // Hold state
        if (leftHoldCounter > 0) {
            key.setDown(true);
            leftHoldCounter--;
            if (leftHoldCounter <= 0) {
                key.setDown(false);
                if (mc.gameMode != null)
                    mc.gameMode.stopDestroyBlock();
            }
            return;
        }

        // Determine trigger
        boolean shouldTrigger = false;

        if (respectCooldown.get()) {
            if (mobMode.get() && !isPlayerLookingAtMob(mc)) {
                key.setDown(false);
                return;
            }
            boolean isBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
            if (mc.player.getAttackStrengthScale(0.5F) >= 1.0F) {
                if (cooldownBufferTicks > 0) {
                    cooldownBufferTicks--;
                    key.setDown(false);
                    return;
                }
                if (isBlock) {
                    if (leftTimeout > 0) {
                        leftTimeout--;
                        key.setDown(false);
                        return;
                    }
                    leftTimeout = leftSpeed.get();
                }
                if (leftRandomize.get() && leftCooldownDelay > 0) {
                    leftCooldownDelay--;
                    key.setDown(false);
                } else {
                    shouldTrigger = true;
                }
            } else {
                key.setDown(false);
                int ping = ClientPing.getPing();
                cooldownBufferTicks = (ping > 0) ? Math.min(5, ping / 100) : 0;
            }
        } else {
            if (mobMode.get() && !isPlayerLookingAtMob(mc)) {
                key.setDown(false);
                return;
            }
            if (leftSpeed.get() > 0) {
                if (leftTimeout <= 0) {
                    resetLeftTimeout();
                    shouldTrigger = true;
                }
                leftTimeout--;
            } else {
                shouldTrigger = true;
            }
        }

        if (shouldTrigger) {
            // Skip chance (from AntiCheatModule)
            int skipChance = antiCheat != null ? antiCheat.skipChance.get() : 0;
            if (skipChance > 0 && RANDOM.nextInt(100) < skipChance) {
                if (leftRandomize.get() && respectCooldown.get())
                    generateLeftCooldownDelay();
                key.setDown(false);
                return;
            }

            boolean attacked = attemptMobAttack(mc);
            if (!attacked) {
                key.setDown(false);
                return;
            }

            totalClickCount++;
            key.setDown(false);
            if (leftRandomize.get())
                generateLeftCooldownDelay();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Non-attack key handling (right-click, jump) — with per-button randomization
    // ══════════════════════════════════════════════════════════════

    private void handleKey(Minecraft mc, KeyMapping key, boolean spamming, int speed, int holdDur,
            boolean randomize, int randomRange, boolean isJump) {
        boolean isRight = !isJump;

        // Click pending
        boolean pending = isRight ? rightClickPending : jumpClickPending;
        if (pending) {
            key.setDown(false);
            if (isRight)
                rightClickPending = false;
            else
                jumpClickPending = false;
            return;
        }

        // Hold state
        int holdCtr = isRight ? rightHoldCounter : jumpHoldCounter;
        if (holdCtr > 0) {
            key.setDown(true);
            if (isRight)
                rightHoldCounter--;
            else
                jumpHoldCounter--;
            if ((isRight ? rightHoldCounter : jumpHoldCounter) <= 0)
                key.setDown(false);
            return;
        }

        boolean shouldTrigger = false;
        int timeout = isRight ? rightTimeout : jumpTimeout;

        int effectiveSpeed = speed;
        if (randomize && randomRange > 0) {
            effectiveSpeed = speed + RANDOM.nextInt(randomRange + 1);
        }

        if (effectiveSpeed > 0) {
            if (timeout <= 0) {
                if (isRight)
                    rightTimeout = effectiveSpeed;
                else
                    jumpTimeout = effectiveSpeed;
                shouldTrigger = true;
            }
            if (isRight)
                rightTimeout--;
            else
                jumpTimeout--;
        } else {
            shouldTrigger = true;
        }

        if (shouldTrigger) {
            key.setDown(true);
            if (spamming) {
                if (isRight)
                    rightClickPending = true;
                else
                    jumpClickPending = true;
            } else {
                if (isRight)
                    rightHoldCounter = holdDur;
                else
                    jumpHoldCounter = holdDur;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // attemptMobAttack — resolve target and attack
    // ══════════════════════════════════════════════════════════════

    private boolean attemptMobAttack(Minecraft mc) {
        SmartCombatModule.AttackMethod method = smartCombat != null
                ? smartCombat.attackMethod.get()
                : SmartCombatModule.AttackMethod.LEGIT;
        HitResult rayTrace = mc.hitResult;
        Entity resolvedTarget = null;
        boolean hasEntityHit = false;

        if (method == SmartCombatModule.AttackMethod.LEGIT) {
            if (rayTrace instanceof EntityHitResult ehr) {
                resolvedTarget = ehr.getEntity();
                hasEntityHit = true;
            }
        } else {
            Entity frameLatch = this.frameLatchedEntity;
            Entity prePick = this.prePickEntity;
            if (rayTrace instanceof EntityHitResult ehr) {
                resolvedTarget = ehr.getEntity();
                hasEntityHit = true;
            } else if (frameLatch instanceof LivingEntity le && le.isAlive() && le.isAttackable()) {
                resolvedTarget = frameLatch;
                hasEntityHit = true;
            } else if (prePick instanceof LivingEntity le2 && le2.isAlive() && le2.isAttackable()) {
                resolvedTarget = prePick;
                hasEntityHit = true;
            }
        }

        if (hasEntityHit && mc.gameMode != null) {
            Entity target = resolvedTarget;
            espTarget = target;

            // Mob filter
            if (mobMode.get() && target instanceof LivingEntity lt) {
                if (!passesMobFilter(lt))
                    return false;
            }

            // Entity protection (from AntiCheatModule)
            boolean entityProt = antiCheat != null && antiCheat.entityProtection.get();
            if (entityProt && entityProtHitId != -1)
                return false;

            // Ping/TPS checks (from NetworkGuardModule)
            if (networkGuard != null && networkGuard.enabled.get()) {
                if (networkGuard.pingLimit.get() > 0) {
                    int ping = ClientPing.getPing();
                    if (ping >= 0 && ping > networkGuard.pingLimit.get())
                        return false;
                }
                if (networkGuard.tpsLimit.get() > 0) {
                    if (ClientTPS.getTPS() < networkGuard.tpsLimit.get())
                        return false;
                }
            }

            // Looting Swapper (from SmartCombatModule)
            boolean lootSwap = smartCombat != null && smartCombat.lootingSwapper.get();
            if (lootSwap && target instanceof LivingEntity lt
                    && lootingSwapState == LootingSwapState.OFF
                    && !isSword(mc.player.getMainHandItem().getItem())) {
                int swordSlot = findBestLootingSword(mc);
                if (swordSlot != -1) {
                    ItemStack bestSword = (swordSlot == OFFHAND_INVENTORY_SLOT)
                            ? mc.player.getOffhandItem()
                            : mc.player.getInventory().getItem(swordSlot);
                    double swordDamage = computeItemAttackDamage(mc, bestSword);
                    if (lt.getHealth() <= (float) swordDamage) {
                        if (swordSlot == OFFHAND_INVENTORY_SLOT) {
                            originalHotbarSlot = -1;
                        } else {
                            originalHotbarSlot = ((InventoryAccessor) (Object) mc.player.getInventory()).getSelected();
                            ((InventoryAccessor) (Object) mc.player.getInventory()).setSelected(swordSlot);
                        }
                        lootingSwapState = LootingSwapState.PHASE1_SWAP;
                        lootingSwapTimer = 0;
                        savedLootingTarget = target;
                        lootingUseOffhand = (swordSlot == OFFHAND_INVENTORY_SLOT);
                        return false;
                    }
                }
            }

            // Smart Trigger (from SmartCombatModule)
            if (smartCombat != null && smartCombat.smartTrigger.get() && target instanceof LivingEntity) {
                var box = target.getBoundingBox().inflate(2.5D, 1.0D, 2.5D);
                int count = mc.level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && e.isAttackable() && !e.equals(mc.player)).size();
                if (count < smartCombat.smartTriggerCount.get())
                    return false;
            }

            // Shield check
            if (respectShield.get() && isShielding(mc.player))
                return false;

            // Execute attack
            if (method == SmartCombatModule.AttackMethod.LEGIT) {
                KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_LEFT));
                EventBus.get().emit(new EventBus.AttackEvent(mc.player, resolvedTarget));
                return true;
            } else {
                if (respectCooldown.get() && mc.player.getAttackStrengthScale(0.5F) < 1.0F)
                    return false;
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
                EventBus.get().emit(new EventBus.AttackEvent(mc.player, resolvedTarget));
                return true;
            }
        } else {
            if (!mobMode.get()) {
                KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_LEFT));
                return true;
            }
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════

    private boolean hasEntityTarget(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity le
                && le.isAlive() && le.isAttackable())
            return true;
        if (frameLatchedEntity instanceof LivingEntity le && le.isAlive() && le.isAttackable())
            return true;
        if (prePickEntity instanceof LivingEntity le2 && le2.isAlive() && le2.isAttackable())
            return true;
        return false;
    }

    private boolean isPlayerLookingAtMob(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity le
                && le.isAlive() && le.isAttackable() && passesMobFilter(le))
            return true;
        if (frameLatchedEntity instanceof LivingEntity le && le.isAlive() && le.isAttackable()
                && passesMobFilter(le))
            return true;
        if (prePickEntity instanceof LivingEntity le2 && le2.isAlive() && le2.isAttackable()
                && passesMobFilter(le2))
            return true;
        return false;
    }

    private boolean passesMobFilter(LivingEntity e) {
        var opt = pro.mikey.autoclicker.AutoClicker.getInstance().getModuleManager()
                .<MobFilterModule>get("mob_filter");
        return opt.map(mf -> mf.passes(e)).orElse(true);
    }

    private static boolean isShielding(net.minecraft.client.player.LocalPlayer player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem;
    }

    public static boolean isSword(net.minecraft.world.item.Item item) {
        return InventoryUtils.isSword(item);
    }

    private void handleLootingSwapper(Minecraft mc) {
        switch (lootingSwapState) {
            case PHASE1_SWAP -> {
                lootingSwapTimer = 0;
                lootingSwapState = LootingSwapState.PHASE2_WAIT;
            }
            case PHASE2_WAIT -> {
                lootingSwapTimer++;
                if (mc.player.getAttackStrengthScale(0) >= 1.0f || lootingSwapTimer >= 40) {
                    lootingSwapState = LootingSwapState.PHASE3_ATTACK;
                }
            }
            case PHASE3_ATTACK -> {
                if (savedLootingTarget != null && savedLootingTarget.isAlive() && mc.gameMode != null) {
                    mc.gameMode.attack(mc.player, savedLootingTarget);
                    mc.player.swing(lootingUseOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                }
                if (originalHotbarSlot != -1) {
                    ((InventoryAccessor) (Object) mc.player.getInventory()).setSelected(originalHotbarSlot);
                }
                lootingSwapState = LootingSwapState.OFF;
                lootingUseOffhand = false;
            }
            default -> {
            }
        }
    }

    private void updateEntityProtection(Minecraft mc) {
        if (entityProtHitId == -1 || mc.level == null)
            return;
        Entity entity = mc.level.getEntity(entityProtHitId);
        if (!(entity instanceof LivingEntity livingTarget) || entity.isRemoved()) {
            entityProtHitId = -1;
            return;
        }

        int timeoutSec = antiCheat != null ? antiCheat.entityProtTimeout.get() : 10;
        long timeoutMs = timeoutSec > 0 ? timeoutSec * 1000L : 10000L;
        if (System.currentTimeMillis() - entityProtSentTime > timeoutMs) {
            entityProtHitId = -1;
            return;
        }

        if (entity instanceof ArmorStand) {
            Integer forcedTime = armorStandResilience.get(entity.getId());
            if (forcedTime != null && forcedTime > 0)
                return;
            entityProtHitId = -1;
            return;
        }

        if (livingTarget.hurtTime > 0) {
            // Waiting for hurtTime to return to 0
        } else {
            entityProtHitId = -1;
        }
    }

    private int findBestLootingSword(Minecraft mc) {
        int bestSlot = -1;
        int bestLootingLevel = 0;
        double bestDamage = 0;
        try {
            var enchantRegistry = mc.level.registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var lootingHolder = enchantRegistry
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty() || !isSword(stack.getItem()))
                    continue;
                int level = net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(lootingHolder, stack);
                if (level <= 0)
                    continue;
                double dmg = computeItemAttackDamage(mc, stack);
                if (level > bestLootingLevel || (level == bestLootingLevel && dmg > bestDamage)) {
                    bestSlot = i;
                    bestLootingLevel = level;
                    bestDamage = dmg;
                }
            }
            ItemStack offhand = mc.player.getOffhandItem();
            if (!offhand.isEmpty() && isSword(offhand.getItem())) {
                int level = net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(lootingHolder, offhand);
                if (level > 0) {
                    double dmg = computeItemAttackDamage(mc, offhand);
                    if (level > bestLootingLevel || (level == bestLootingLevel && dmg > bestDamage)) {
                        bestSlot = OFFHAND_INVENTORY_SLOT;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return bestSlot;
    }

    private double computeItemAttackDamage(Minecraft mc, ItemStack stack) {
        double[] dmg = { 1.0 };
        try {
            var attrMods = stack.getComponents().get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            if (attrMods != null) {
                attrMods.forEach(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attr, mod) -> {
                    if (attr.value() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.value()
                            && mod.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                        dmg[0] += mod.amount();
                    }
                });
            }
            if (mc.level != null) {
                var enchantRegistry = mc.level.registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                var sharpness = enchantRegistry
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
                int level = net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(sharpness, stack);
                if (level > 0)
                    dmg[0] += level * 0.5 + 0.5;
            }
        } catch (Exception e) {
            dmg[0] = 5.0;
        }
        return dmg[0];
    }

    private void resetLeftTimeout() {
        leftTimeout = computeTimeout(leftSpeed.get());
    }

    /**
     * Computes a randomized timeout in ticks.
     * Uses per-button LMB randomization + global anti-cheat settings (Gaussian,
     * GCD).
     */
    private int computeTimeout(int baseTicks) {
        double result = baseTicks;

        // TPS Sync (from NetworkGuardModule)
        if (networkGuard != null && networkGuard.enabled.get() && networkGuard.tpsSync.get()) {
            float serverTps = Math.max(1.0f, ClientTPS.getTPS());
            result = result * (20.0 / serverTps);
        }

        // Per-button randomization (LMB)
        if (leftRandomize.get()) {
            boolean gaussian = antiCheat != null && antiCheat.gaussianRandom.get();
            if (gaussian) {
                double sigma = antiCheat != null ? antiCheat.randomSigma.get() : 1.0;
                result += RANDOM.nextGaussian() * sigma;
            } else {
                int range = leftRandomizeRange.get();
                if (range > 0)
                    result += RANDOM.nextInt(range + 1);
            }
        }

        // GCD Patch (from AntiCheatModule)
        boolean gcd = antiCheat != null && antiCheat.gcdPatch.get();
        if (gcd && leftRandomize.get()) {
            int hz = antiCheat != null ? antiCheat.pollingRate.get().hz : 1000;
            double gcdMs = 1000.0 / hz;
            double resultMs = result * 50.0;
            resultMs = Math.round(resultMs / gcdMs) * gcdMs;
            result = resultMs / 50.0;
        }

        return Math.max(0, (int) Math.round(result));
    }

    private void generateLeftCooldownDelay() {
        boolean gaussian = antiCheat != null && antiCheat.gaussianRandom.get();
        boolean gcd = antiCheat != null && antiCheat.gcdPatch.get();

        if (gaussian) {
            double sigma = antiCheat != null ? antiCheat.randomSigma.get() : 1.0;
            double raw = Math.abs(RANDOM.nextGaussian() * sigma);
            if (gcd) {
                int hz = antiCheat != null ? antiCheat.pollingRate.get().hz : 1000;
                double gcdMs = 1000.0 / hz;
                double rawMs = raw * 50.0;
                rawMs = Math.round(rawMs / gcdMs) * gcdMs;
                leftCooldownDelay = Math.max(0, (int) Math.round(rawMs / 50.0));
            } else {
                leftCooldownDelay = Math.max(0, (int) Math.round(raw));
            }
        } else {
            int range = leftRandomizeRange.get();
            leftCooldownDelay = range > 0 ? RANDOM.nextInt(range + 1) : 0;
        }
    }
}
