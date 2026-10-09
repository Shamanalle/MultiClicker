package io.github.shamanalle.multiclicker.module.clicker;

import net.minecraft.client.KeyMapping;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.KeySetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Input;

import java.util.Random;

/**
 * One simulated input (attack, use or jump) with its own timing settings.
 *
 * <ul>
 *   <li>{@link Mode#CLICK}: presses the key at the set speed, either an interval in ticks or a
 *   number of clicks per second.</li>
 *   <li>{@link Mode#HOLD}: holds the key for {@code holdTime} ticks (0 = forever), then waits
 *   {@code pause} ticks before holding it again.</li>
 * </ul>
 * Both modes add a random delay of up to {@code jitter} ticks, spread as {@code jitterType} says,
 * to make the rhythm less uniform.
 */
public final class ClickChannel {
    public enum Mode {
        CLICK, HOLD
    }

    /** How the click speed is set. */
    public enum Rate {
        INTERVAL, CPS
    }

    private static final Random RANDOM = new Random();

    private final String name;
    public final BoolSetting enabled;
    public final KeySetting key;
    public final EnumSetting<Mode> mode;
    public final EnumSetting<Rate> rate;
    public final IntSetting interval;
    public final IntSetting cps;
    public final IntSetting holdTime;
    public final IntSetting pause;
    public final IntSetting jitter;
    public final EnumSetting<Jitter> jitterType;

    private final ClickTimer timer = new ClickTimer();
    private int holdLeft;
    private boolean down;

    ClickChannel(ClickerModule module, String name, boolean enabledByDefault, Mode defaultMode, int defaultInterval,
                 int defaultCps) {
        this.name = name;
        this.enabled = module.register(new BoolSetting(name, enabledByDefault));
        this.mode = module.register(new EnumSetting<>(name + "_mode", defaultMode).visibleWhen(enabled::get));
        this.rate = module.register(new EnumSetting<>(name + "_rate", Rate.INTERVAL)
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.CLICK));
        this.interval = module.register(new IntSetting(name + "_interval", defaultInterval, 1, 100, Unit.CLICK_INTERVAL)
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.CLICK && rate.get() == Rate.INTERVAL));
        this.cps = module.register(new IntSetting(name + "_cps", defaultCps, 1, 20, Unit.CPS)
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.CLICK && rate.get() == Rate.CPS));
        this.holdTime = module.register(new IntSetting(name + "_hold", 0, 0, 200, Unit.TICKS)
                .zeroMeans("multiclicker.value.forever")
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.HOLD));
        this.pause = module.register(new IntSetting(name + "_pause", 10, 1, 200, Unit.TICKS)
                .visibleWhen(() -> enabled.get() && mode.get() == Mode.HOLD && holdTime.get() > 0));
        this.jitter = module.register(new IntSetting(name + "_jitter", 0, 0, 20, Unit.TICKS)
                .zeroMeans("options.off")
                .visibleWhen(this::jitterApplies));
        this.jitterType = module.register(new EnumSetting<>(name + "_jitter_type", Jitter.NATURAL)
                .visibleWhen(() -> jitterApplies() && jitter.get() > 0));
        // Last in the group: the channel works without it, and it shows even while the channel is off.
        this.key = module.register(new KeySetting(name + "_key"));
    }

    private boolean jitterApplies() {
        return enabled.get() && (mode.get() == Mode.CLICK || holdTime.get() > 0);
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
        if (countDown) {
            timer.tick();
        }
        if (!allowed || !timer.ready()) {
            return false;
        }
        Input.click(key);
        down = true;
        timer.restart(clickInterval() + randomDelay());
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
            if (!timer.ready()) {
                timer.tick();
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
            timer.restart(pause.get() + randomDelay());
        }
        return false;
    }

    /** Ticks between two clicks without the random delay; fractional in CPS mode. */
    public double clickInterval() {
        return rate.get() == Rate.CPS ? 20.0 / cps.get() : interval.get();
    }

    /** Average clicks per second including the random delay, for the HUD. */
    public double averageCps() {
        return 20.0 / (clickInterval() + jitterType.get().mean(jitter.get()));
    }

    private double randomDelay() {
        return jitterType.get().sample(jitter.get(), RANDOM);
    }

    /** Short label key for the HUD, e.g. {@code multiclicker.hud.attack}. */
    public String hudKey() {
        return "multiclicker.hud." + name;
    }

    /** Name key of the channel, e.g. {@code multiclicker.module.clicker.attack}. */
    public String nameKey() {
        return enabled.translationKey();
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
        timer.reset();
        holdLeft = 0;
    }
}
