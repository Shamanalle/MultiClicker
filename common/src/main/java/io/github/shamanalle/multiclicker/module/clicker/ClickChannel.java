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
    public final BoolSetting onlyWhileHeld;
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
    private boolean holdingContinuous;

    ClickChannel(ClickerModule module, String name, boolean enabledByDefault, Mode defaultMode, int defaultInterval,
                 int defaultCps) {
        this.name = name;
        this.enabled = module.register(new BoolSetting(name, enabledByDefault));
        this.mode = module.register(new EnumSetting<>(name + "_mode", defaultMode).visibleWhen(enabled::get));
        this.onlyWhileHeld = module.register(new BoolSetting(name + "_only_while_held", false).visibleWhen(enabled::get));
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

    /** True when this channel may act given its "only while held" requirement. */
    boolean isTriggerSatisfied(KeyMapping key) {
        return !onlyWhileHeld.get() || Input.isPhysicallyDown(key);
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
        boolean physicallyHeld = onlyWhileHeld.get() && Input.isPhysicallyDown(key);
        boolean mayAct = allowed && isTriggerSatisfied(key);
        holdingContinuous = false;
        return mode.get() == Mode.CLICK ? tickClick(key, mayAct, countDown, physicallyHeld) : tickHold(key, mayAct, physicallyHeld);
    }

    /**
     * Holds the attack key continuously while breaking a block in {@link Mode#CLICK}, so blocks with
     * non-zero hardness do not reset their mining progress on every click interval.
     */
    boolean tickContinuousBlock(KeyMapping key, boolean allowed) {
        boolean mayAct = allowed && isTriggerSatisfied(key);
        if (mode.get() == Mode.HOLD) {
            holdingContinuous = false;
            return tickHold(key, mayAct, onlyWhileHeld.get() && Input.isPhysicallyDown(key));
        }
        if (!mayAct) {
            if (down) {
                releaseKey(key, onlyWhileHeld.get() && Input.isPhysicallyDown(key));
                down = false;
            }
            holdingContinuous = false;
            return false;
        }
        if (!down || !holdingContinuous) {
            Input.click(key);
            down = true;
            holdingContinuous = true;
            return true;
        }
        Input.hold(key);
        return false;
    }

    /**
     * Attacks an entity while the channel is in {@link Mode#HOLD}: vanilla only swings at entities on
     * discrete clicks, so holding the key without pulsing a click would never hit a mob that steps in
     * front of the player while mining.
     */
    boolean tickEntityInHoldMode(KeyMapping key, boolean allowed, boolean countDown) {
        boolean physicallyHeld = onlyWhileHeld.get() && Input.isPhysicallyDown(key);
        boolean mayAct = allowed && isTriggerSatisfied(key);
        holdingContinuous = false;
        if (down) {
            releaseKey(key, physicallyHeld);
            down = false;
        }
        if (countDown) {
            timer.tick();
        }
        if (!mayAct || !timer.ready()) {
            return false;
        }
        Input.click(key);
        down = true;
        double delay = holdTime.get() > 0 ? pause.get() + randomDelay() : 1.0;
        timer.restart(delay);
        return true;
    }

    private boolean tickClick(KeyMapping key, boolean allowed, boolean countDown, boolean suppressPhysicalHold) {
        if (down) {
            releaseKey(key, suppressPhysicalHold);
            down = false;
        } else if (suppressPhysicalHold) {
            // Keep the key logically up between clicks so holding LMB/RMB physically clicks at the set rate.
            Input.forceUp(key);
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

    private boolean tickHold(KeyMapping key, boolean allowed, boolean suppressPhysicalHold) {
        if (!allowed) {
            if (down) {
                releaseKey(key, suppressPhysicalHold);
                down = false;
            } else if (suppressPhysicalHold) {
                Input.forceUp(key);
            }
            return false;
        }
        if (!down) {
            if (!timer.ready()) {
                if (suppressPhysicalHold) {
                    Input.forceUp(key);
                }
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
            releaseKey(key, suppressPhysicalHold);
            down = false;
            timer.restart(pause.get() + randomDelay());
        }
        return false;
    }

    private static void releaseKey(KeyMapping key, boolean suppressPhysicalHold) {
        if (suppressPhysicalHold) {
            Input.forceUp(key);
        } else {
            Input.release(key);
        }
    }

    /**
     * Lets the delay run without clicking, while another part of the clicker makes the hit (the
     * Looting finishing blow, a hit that first lowers the shield).
     *
     * @return {@code true} once the next click is due
     */
    boolean waitTick(KeyMapping key, boolean countDown) {
        holdingContinuous = false;
        boolean physicallyHeld = onlyWhileHeld.get() && Input.isPhysicallyDown(key);
        if (down) {
            releaseKey(key, physicallyHeld);
            down = false;
        } else if (physicallyHeld) {
            Input.forceUp(key);
        }
        if (countDown) {
            timer.tick();
        }
        return timer.ready();
    }

    /** Another part of the clicker clicked: the next click waits the usual delay. */
    void clicked() {
        if (mode.get() == Mode.CLICK) {
            timer.restart(clickInterval() + randomDelay());
        }
    }

    /** Lets go of the key but keeps the delay running (the hit is handed to another part). */
    void release(KeyMapping key) {
        if (down) {
            Input.release(key);
        }
        down = false;
        holdingContinuous = false;
        holdLeft = 0;
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

    /** Whether the channel is currently holding its key down continuously (hold mode or block mining). */
    boolean isHolding() {
        return down && (mode.get() == Mode.HOLD || holdingContinuous);
    }

    /** Another module holds the key now (auto eat, to eat): forget the press without releasing it. */
    void handOver() {
        down = false;
        holdingContinuous = false;
        holdLeft = 0;
    }

    void reset(KeyMapping key) {
        if (down || onlyWhileHeld.get()) {
            Input.release(key);
        }
        down = false;
        holdingContinuous = false;
        timer.reset();
        holdLeft = 0;
    }
}
