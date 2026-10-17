package io.github.shamanalle.multiclicker.module.session;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.stats.StatFormat;

/**
 * A target for the current session, such as 500 kills or 64 fish. Its progress is shown on the HUD
 * and in the menu; reaching it sends a notification and can turn the mod off.
 */
public class GoalsModule extends Module {
    /** The counters a goal can be set for. */
    public enum Goal {
        KILLS(Stat.KILLS),
        ATTACKS(Stat.ATTACKS),
        CLICKS(Stat.CLICKS),
        CATCHES(Stat.CATCHES),
        FISH(Stat.FISH),
        TREASURE(Stat.TREASURE),
        CROPS(Stat.CROPS),
        BLOCKS(Stat.BLOCKS),
        XP(Stat.XP),
        FOOD(Stat.FOOD);

        public final Stat stat;

        Goal(Stat stat) {
            this.stat = stat;
        }
    }

    public final EnumSetting<Goal> goal = add(new EnumSetting<>("goal", Goal.KILLS)
            .labels(value -> I18n.get(value.stat.translationKey())));
    public final IntSetting target = add(new IntSetting("target", 100, 1, 100_000, Unit.NONE).logScale());
    public final BoolSetting stop = add(new BoolSetting("stop", false));

    /** Whether this session already reached the goal, so it is announced once. */
    private boolean reached;
    /** The goal was changed: count what is already done as reached without announcing it. */
    private boolean changed;

    public GoalsModule() {
        super("goals", Category.SESSION, true, false);
        goal.onChange(() -> changed = true);
        target.onChange(() -> changed = true);
    }

    @Override
    public void start(Minecraft mc) {
        reached = false;
        changed = false;
    }

    @Override
    public void resume(Minecraft mc) {
        // A profile switch keeps the session, and so whether its goal was reached.
    }

    @Override
    public void tick(Minecraft mc) {
        long done = current();
        if (changed) {
            changed = false;
            reached = done >= target.get();
            return;
        }
        if (reached || done < target.get()) {
            return;
        }
        reached = true;
        MultiClicker mod = MultiClicker.get();
        Component text = describe();
        mod.notifications().goalReached(mc, text, stop.get());
        if (stop.get()) {
            mod.setActive(false, Component.translatable("multiclicker.notify.goal_reached", text));
        }
    }

    /** How much of the goal this session has done. */
    public long current() {
        MultiClicker mod = MultiClicker.get();
        return mod.stats().isRunning() ? mod.stats().session().get(goal.get().stat) : 0;
    }

    /** 0..1. */
    public float progress() {
        return Math.min(1, (float) current() / target.get());
    }

    /** E.g. "Kills: 100". */
    public Component describe() {
        return Component.translatable(goal.get().stat.translationKey()).append(": " + StatFormat.compact(target.get()));
    }

    /** E.g. "37 / 100". */
    public String progressText() {
        return StatFormat.compact(current()) + " / " + StatFormat.compact(target.get());
    }

    @Override
    public String hudInfo() {
        return progressText();
    }
}
