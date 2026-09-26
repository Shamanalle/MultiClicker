package pro.mikey.autoclicker.module.clicker;

import net.minecraft.client.KeyMapping;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.EnumSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.Input;

import java.util.Random;

/**
 * One simulated input (attack, use or jump) with its own timing settings.
 *
 * <ul>
 *   <li>{@link Mode#CLICK}: presses the key once every {@code interval} ticks.</li>
 *   <li>{@link Mode#HOLD}: holds the key for {@code holdTime} ticks (0 = forever), then waits
 *   {@code pause} ticks before holding it again.</li>
 * </ul>
 * Both modes add a random delay of up to {@code jitter} ticks to make the rhythm less uniform.
 */
public final class ClickChannel {
    public enum Mode {
        CLICK, HOLD
    }

    private static final Random RANDOM = new Random();

    private final String name;
    public final BoolSetting enabled;
    public final EnumSetting<Mode> mode;
    public final IntSetting interval;
    public final IntSetting holdTime;
    public final IntSetting pause;
    public final IntSetting jitter;

    private int cooldown;
    private int holdLeft;
    private boolean down;

    ClickChannel(ClickerModule module, String name, boolean enabledByDefault, Mode defaultMode, int defaultInterval) {
        this.name = name;
        this.enabled = module.register(new BoolSetting(name, enabledByDefault));
        this.mode = module.register(new EnumSetting<>(name + "_mode", defaultMode).visibleWhen(enabled::get));
        this.interval = module.register(new IntSetting(name + "_interval", defaultInterval, 1, 100, Unit.CLICK_INTERVAL)
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.CLICK));
        this.holdTime = module.register(new IntSetting(name + "_hold", 0, 0, 200, Unit.TICKS)
                .zeroMeans("multiclicker.value.forever")
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.HOLD));
        this.pause = module.register(new IntSetting(name + "_pause", 10, 1, 200, Unit.TICKS)
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.HOLD && holdTime.get() > 0));
        this.jitter = module.register(new IntSetting(name + "_jitter", 0, 0, 20, Unit.TICKS)
                .zeroMeans("options.off")
                .visibleWhen(() -> enabled.get() && (mode.get() == Mode.CLICK || holdTime.get() > 0)));
    }

    /**
     * Advances the channel by one tick.
     *
     * @param key       the key to simulate
     * @param allowed   whether an action may happen right now (valid target, not suppressed...)
     * @param countDown whether the delay timer advances this tick (the attack channel pauses it
     *                  until the weapon is charged, so the delay is added on top of the cooldown)
     * @return {@code true} if a new press happened this tick
     */
    boolean tick(KeyMapping key, boolean allowed, boolean countDown) {
        return mode.get() == Mode.CLICK ? tickClick(key, allowed, countDown) : tickHold(key, allowed);
    }

    private boolean tickClick(KeyMapping key, boolean allowed, boolean countDown) {
        if (down) {
            Input.release(key);
            down = false;
        }
        if (countDown && cooldown > 0) {
            cooldown--;
        }
        if (!allowed || cooldown > 0) {
            return false;
        }
        Input.click(key);
        down = true;
        cooldown = interval.get() + randomDelay();
        return true;
    }

    private boolean tickHold(KeyMapping key, boolean allowed) {
        if (!allowed) {
            if (down) {
                Input.release(key);
                down = false;
            }
            return false;
        }
        if (!down) {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            Input.click(key);
            down = true;
            holdLeft = holdTime.get();
            return true;
        }
        Input.hold(key);
        if (holdLeft > 0 && --holdLeft == 0) {
            Input.release(key);
            down = false;
            cooldown = pause.get() + randomDelay();
        }
        return false;
    }

    private int randomDelay() {
        int max = jitter.get();
        return max > 0 ? RANDOM.nextInt(max + 1) : 0;
    }

    /** Short label key for the HUD, e.g. {@code multiclicker.hud.attack}. */
    public String hudKey() {
        return "multiclicker.hud." + name;
    }

    /** Whether the channel is currently holding its key down in hold mode. */
    boolean isHolding() {
        return down && mode.get() == Mode.HOLD;
    }

    void reset(KeyMapping key) {
        if (down) {
            Input.release(key);
        }
        down = false;
        cooldown = 0;
        holdLeft = 0;
    }
}
