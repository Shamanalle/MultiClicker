package io.github.shamanalle.multiclicker.module.automation;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.compat.Session;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.clicker.ClickerModule;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Input;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Performs small random actions at random intervals so the server does not kick you for idling. */
public class AntiAfkModule extends Module {
    private static final Random RANDOM = new Random();

    private enum Action {
        JUMP, SNEAK, SWING, ROTATE, STEP, SWITCH_SLOT
    }

    public final IntSetting minInterval = add(new IntSetting("min_interval", 30, 5, 600, Unit.SECONDS));
    public final IntSetting maxInterval = add(new IntSetting("max_interval", 60, 5, 600, Unit.SECONDS));
    public final BoolSetting jump = add(new BoolSetting("jump", true));
    public final BoolSetting sneak = add(new BoolSetting("sneak", true));
    public final BoolSetting swing = add(new BoolSetting("swing", true));
    public final BoolSetting rotate = add(new BoolSetting("rotate", false));
    public final BoolSetting step = add(new BoolSetting("step", false));
    public final BoolSetting switchSlot = add(new BoolSetting("switch_slot", true));

    private int countdown;
    private Action current;
    private int actionTick;
    private float originalYaw;
    private KeyMapping heldKey;
    private int originalSlot = -1;
    private int switchedSlot = -1;

    public AntiAfkModule() {
        super("anti_afk", Category.AUTOMATION, true, false);
    }

    @Override
    public void start(Minecraft mc) {
        scheduleNext();
        current = null;
    }

    @Override
    public void stop(Minecraft mc) {
        finishAction(mc);
    }

    @Override
    public void tick(Minecraft mc) {
        if (current != null) {
            tickAction(mc, mc.player);
            return;
        }
        if (--countdown > 0) {
            return;
        }
        scheduleNext();
        // Skip while a menu is open or the player is moving on their own.
        if (Screens.current(mc) != null || Input.isPlayerMoving(mc)) {
            return;
        }
        List<Action> pool = new ArrayList<>();
        if (jump.get()) pool.add(Action.JUMP);
        if (sneak.get()) pool.add(Action.SNEAK);
        if (swing.get()) pool.add(Action.SWING);
        if (rotate.get()) pool.add(Action.ROTATE);
        // Stepping back and forth would fight auto walk, which already keeps the player active.
        if (step.get() && !MultiClicker.get().autoWalk().isRunning()) pool.add(Action.STEP);
        if (switchSlot.get() && canSwitchSlot(mc.player)) pool.add(Action.SWITCH_SLOT);
        if (!pool.isEmpty()) {
            current = pool.get(RANDOM.nextInt(pool.size()));
            MultiClicker.get().stats().count(Stat.AFK_ACTIONS);
            actionTick = 0;
            tickAction(mc, mc.player);
        }
    }

    private void tickAction(Minecraft mc, LocalPlayer player) {
        int tick = actionTick++;
        switch (current) {
            case JUMP -> {
                if (tick == 0) press(mc.options.keyJump);
                else if (tick >= 2) finishAction(mc);
            }
            case SNEAK -> {
                if (tick == 0) press(mc.options.keyShift);
                else if (tick >= 5) finishAction(mc);
            }
            case SWING -> {
                Session.swing(player);
                finishAction(mc);
            }
            case ROTATE -> {
                if (tick == 0) {
                    originalYaw = player.getYRot();
                    float delta = (10.0F + RANDOM.nextFloat() * 20.0F) * (RANDOM.nextBoolean() ? 1 : -1);
                    player.setYRot(originalYaw + delta);
                } else if (tick >= 10) {
                    player.setYRot(originalYaw);
                    finishAction(mc);
                }
            }
            case STEP -> {
                if (tick == 0) press(mc.options.keyUp);
                else if (tick == 3) press(mc.options.keyDown);
                else if (tick >= 6) finishAction(mc);
            }
            case SWITCH_SLOT -> {
                if (tick == 0) {
                    originalSlot = Slots.selected(player.getInventory());
                    switchedSlot = (originalSlot + 1) % 9;
                    Slots.select(player.getInventory(), switchedSlot);
                } else {
                    finishAction(mc);
                }
            }
        }
    }

    private void press(KeyMapping key) {
        if (heldKey != null && heldKey != key) {
            Input.release(heldKey);
        }
        Input.click(key);
        heldKey = key;
    }

    private void finishAction(Minecraft mc) {
        if (heldKey != null) {
            Input.release(heldKey);
            heldKey = null;
        }
        // Back to the original slot, unless the player picked another one meanwhile.
        if (originalSlot != -1 && mc.player != null && Slots.selected(mc.player.getInventory()) == switchedSlot) {
            Slots.select(mc.player.getInventory(), originalSlot);
        }
        originalSlot = -1;
        switchedSlot = -1;
        current = null;
    }

    /**
     * The slot is switched for real for one tick, so it waits while anything uses the item in hand:
     * the clicker, a fishing line or an item in use (the server drops both), or a module that picked the slot.
     */
    private static boolean canSwitchSlot(LocalPlayer player) {
        MultiClicker mod = MultiClicker.get();
        ClickerModule clicker = mod.clicker();
        boolean clicking = clicker.isRunning() && (clicker.attack.enabled.get() || clicker.use.enabled.get());
        return !clicking && player.fishing == null && !player.isUsingItem() && !mod.autoEat().isBusy()
                && !mod.autoTool().isToolSelected() && !mod.autoFarm().isRunning() && !clicker.isSwappingWeapon();
    }

    private void scheduleNext() {
        int min = Math.min(minInterval.get(), maxInterval.get());
        int max = Math.max(minInterval.get(), maxInterval.get());
        countdown = (min + RANDOM.nextInt(max - min + 1)) * 20;
    }
}
