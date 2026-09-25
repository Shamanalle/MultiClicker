package pro.mikey.autoclicker.modules.automation;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;
import pro.mikey.autoclicker.util.InventoryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Anti-AFK module with Setting-based configuration.
 */
public class AntiAfkModule implements Module {

    private static final Random RANDOM = new Random();

    // ── Settings ─────────────────────────────────────────────────────
    public final BooleanSetting enabled = new BooleanSetting("anti_afk.enabled", "Анти-АФК", false)
            .withDescription("Имитирует активность для обхода AFK-кика сервера");
    public final IntSetting minInterval = new IntSetting("anti_afk.min_interval", "Мин. интервал", 80, 20, 1200)
            .withDescription("Минимальная пауза между действиями в тиках. 20 = 1 секунда");
    public final IntSetting maxInterval = new IntSetting("anti_afk.max_interval", "Макс. интервал", 400, 20, 1200)
            .withDescription("Максимальная пауза. Реальный интервал — случайное число между мин и макс");
    public final BooleanSetting shiftAction = new BooleanSetting("anti_afk.shift", "Присед", true)
            .withDescription("Краткое нажатие Shift. Незаметно для других игроков");
    public final BooleanSetting stepAction = new BooleanSetting("anti_afk.step", "Шаг", true)
            .withDescription("Шаг вперёд/назад (3 тика). Имитирует микродвижение");
    public final BooleanSetting swingAction = new BooleanSetting("anti_afk.swing", "Взмах", false)
            .withDescription("Взмах рукой в воздухе. Виден другим игрокам, но не вызовет подозрений");
    public final BooleanSetting hotbarAction = new BooleanSetting("anti_afk.hotbar", "Хотбар", false)
            .withDescription("Краткая смена слота и возврат. Сервер фиксирует активность");
    public final BooleanSetting lookJitter = new BooleanSetting("anti_afk.look_jitter", "Обзор", false)
            .withDescription("Плавный поворот камеры с возвратом. Блокируется при CameraLock");
    public final FloatSetting lookAmplitude = new FloatSetting("anti_afk.look_amplitude", "Амплитуда", 1.0, 0.1, 3.0)
            .withDescription("Макс. угол поворота в градусах. Больше = заметнее, но убедительнее");
    public final BooleanSetting chatMessage = new BooleanSetting("anti_afk.chat_message", "Чат", false)
            .withDescription("Отправляет случайное сообщение в чат. Некоторые серверы проверяют chat activity");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, minInterval, maxInterval, shiftAction, stepAction,
                swingAction, hotbarAction, lookJitter, lookAmplitude, chatMessage);
    }

    // ── State ─────────────────────────────────────────────────────────
    public enum State {
        IDLE, SHIFTING, STEPPING_MOVE, STEPPING_RETURN,
        SWINGING, HOTBAR_CHANGE, LOOKING_DRIFT, LOOKING_RETURN, CHATTING
    }

    private State state = State.IDLE;
    private int stateTimer = 0;
    private int idleTimer = 0;
    private int nextInterval = 200;
    private int stepDir = 0;
    private int origHotbarSlot = -1;
    private boolean lookJitterActive = false;
    private float lookOrigYaw, lookOrigPitch;
    private float lookDeltaYaw, lookDeltaPitch;
    private int lookDuration = 3;
    private float lastYRot, lastXRot;

    // ── Module identity ───────────────────────────────────────────────
    @Override
    public String getId() {
        return "anti_afk";
    }

    @Override
    public String getDisplayName() {
        return "Анти-АФК";
    }

    @Override
    public Category getCategory() {
        return Category.AUTOMATION;
    }

    @Override
    public int tickPriority() {
        return 20;
    }

    public boolean isLookJitterActive() {
        return lookJitterActive;
    }

    @Override
    public void onDisable() {
        // Release any keys that might be held mid-action
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.options.keyShift.setDown(false);
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
        }
        // Restore look if we were mid-jitter
        if (lookJitterActive && mc != null && mc.player != null) {
            mc.player.setYRot(lookOrigYaw);
            mc.player.setXRot(lookOrigPitch);
        }
        state = State.IDLE;
        stateTimer = 0;
        idleTimer = 0;
        nextInterval = 200;
        stepDir = 0;
        lookJitterActive = false;
        // Restore hotbar if we were mid-swap
        if (origHotbarSlot != -1 && mc != null && mc.player != null) {
            InventoryUtils.setSelectedSlot(mc.player, origHotbarSlot);
            origHotbarSlot = -1;
        }
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        boolean isMoving = mc.player.input.hasForwardImpulse()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                || mc.options.keyDown.isDown() || mc.options.keyJump.isDown();
        float currentYRot = mc.player.getYRot();
        float currentXRot = mc.player.getXRot();
        boolean lookChanged = !lookJitterActive
                && (Math.abs(currentYRot - lastYRot) > 0.1f
                        || Math.abs(currentXRot - lastXRot) > 0.1f);
        boolean playerActive = isMoving || lookChanged
                || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown();

        lastYRot = currentYRot;
        lastXRot = currentXRot;

        if (playerActive) {
            idleTimer = 0;
            if (state == State.SHIFTING) {
                mc.options.keyShift.setDown(false);
                state = State.IDLE;
            }
        } else if (state == State.IDLE) {
            idleTimer++;
            if (idleTimer >= nextInterval) {
                List<State> pool = new ArrayList<>();
                if (shiftAction.get())
                    pool.add(State.SHIFTING);
                if (stepAction.get())
                    pool.add(State.STEPPING_MOVE);
                if (swingAction.get())
                    pool.add(State.SWINGING);
                if (hotbarAction.get())
                    pool.add(State.HOTBAR_CHANGE);
                if (lookJitter.get())
                    pool.add(State.LOOKING_DRIFT);
                if (chatMessage.get())
                    pool.add(State.CHATTING);

                if (!pool.isEmpty()) {
                    state = pool.get(RANDOM.nextInt(pool.size()));
                    stateTimer = 0;
                    int imin = minInterval.get();
                    int imax = Math.max(imin, maxInterval.get());
                    nextInterval = imin + (imax > imin ? RANDOM.nextInt(imax - imin + 1) : 0);
                    initAction(mc);
                } else {
                    idleTimer = 0;
                }
            }
        }

        if (state != State.IDLE) {
            executeAction(mc);
            return true;
        }
        return false;
    }

    private void initAction(Minecraft mc) {
        switch (state) {
            case STEPPING_MOVE -> stepDir = RANDOM.nextInt(4);
            case LOOKING_DRIFT -> {
                float amp = lookAmplitude.get().floatValue();
                lookOrigYaw = mc.player.getYRot();
                lookOrigPitch = mc.player.getXRot();
                lookDeltaYaw = (RANDOM.nextFloat() * 2f - 1f) * amp;
                lookDeltaPitch = (RANDOM.nextFloat() * 2f - 1f) * (amp * 0.5f);
                lookDuration = 2 + RANDOM.nextInt(3);
                lookJitterActive = true;
            }
            case HOTBAR_CHANGE -> {
                origHotbarSlot = InventoryUtils.getSelectedSlot(mc.player);
                InventoryUtils.setSelectedSlot(mc.player, (origHotbarSlot + 1) % 9);
            }
            default -> {
            }
        }
    }

    private void executeAction(Minecraft mc) {
        switch (state) {
            case SHIFTING -> {
                if (stateTimer == 0)
                    mc.options.keyShift.setDown(true);
                else if (stateTimer >= 3) {
                    mc.options.keyShift.setDown(false);
                    finishAction();
                }
                stateTimer++;
            }
            case STEPPING_MOVE -> {
                if (stateTimer == 0)
                    dirKey(mc, stepDir).setDown(true);
                else if (stateTimer >= 3) {
                    dirKey(mc, stepDir).setDown(false);
                    state = State.STEPPING_RETURN;
                    stateTimer = 0;
                    return;
                }
                stateTimer++;
            }
            case STEPPING_RETURN -> {
                if (stateTimer == 0)
                    dirKey(mc, stepDir ^ 1).setDown(true);
                else if (stateTimer >= 3) {
                    dirKey(mc, stepDir ^ 1).setDown(false);
                    finishAction();
                }
                stateTimer++;
            }
            case SWINGING -> {
                mc.player.swing(InteractionHand.MAIN_HAND);
                finishAction();
            }
            case HOTBAR_CHANGE -> {
                stateTimer++;
                if (stateTimer >= 2) {
                    InventoryUtils.setSelectedSlot(mc.player, origHotbarSlot);
                    origHotbarSlot = -1;
                    finishAction();
                }
            }
            case LOOKING_DRIFT -> {
                float p = (float) (stateTimer + 1) / lookDuration;
                mc.player.setYRot(lookOrigYaw + lookDeltaYaw * p);
                mc.player.setXRot(lookOrigPitch + lookDeltaPitch * p);
                stateTimer++;
                if (stateTimer >= lookDuration) {
                    stateTimer = 0;
                    state = State.LOOKING_RETURN;
                }
            }
            case LOOKING_RETURN -> {
                float p = (float) (stateTimer + 1) / lookDuration;
                mc.player.setYRot(lookOrigYaw + lookDeltaYaw * (1.0f - p));
                mc.player.setXRot(lookOrigPitch + lookDeltaPitch * (1.0f - p));
                stateTimer++;
                if (stateTimer >= lookDuration) {
                    mc.player.setYRot(lookOrigYaw);
                    mc.player.setXRot(lookOrigPitch);
                    lookJitterActive = false;
                    finishAction();
                }
            }
            case CHATTING -> {
                // #12 Send a random short chat command/message
                String[] messages = { ".", ",", " ", "..", ",," };
                String msg = messages[RANDOM.nextInt(messages.length)];
                if (mc.player.connection != null) {
                    mc.player.connection.sendChat(msg);
                }
                finishAction();
            }
            default -> {
            }
        }
    }

    private void finishAction() {
        state = State.IDLE;
        idleTimer = 0;
    }

    private KeyMapping dirKey(Minecraft mc, int dir) {
        return switch (dir) {
            case 1 -> mc.options.keyDown;
            case 2 -> mc.options.keyLeft;
            case 3 -> mc.options.keyRight;
            default -> mc.options.keyUp;
        };
    }
}
