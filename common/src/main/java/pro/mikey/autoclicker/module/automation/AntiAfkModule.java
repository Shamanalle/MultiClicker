package pro.mikey.autoclicker.module.automation;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.Input;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Performs small random actions at random intervals so the server does not kick you for idling. */
public class AntiAfkModule extends Module {
    private static final Random RANDOM = new Random();

    private enum Action {
        JUMP, SNEAK, SWING, ROTATE, STEP
    }

    public final IntSetting minInterval = add(new IntSetting("min_interval", 30, 5, 600, Unit.SECONDS));
    public final IntSetting maxInterval = add(new IntSetting("max_interval", 60, 5, 600, Unit.SECONDS));
    public final BoolSetting jump = add(new BoolSetting("jump", true));
    public final BoolSetting sneak = add(new BoolSetting("sneak", true));
    public final BoolSetting swing = add(new BoolSetting("swing", true));
    public final BoolSetting rotate = add(new BoolSetting("rotate", false));
    public final BoolSetting step = add(new BoolSetting("step", false));

    private int countdown;
    private Action current;
    private int actionTick;
    private float originalYaw;
    private KeyMapping heldKey;

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
        if (mc.screen != null || Input.isPlayerMoving(mc)) {
            return;
        }
        List<Action> pool = new ArrayList<>();
        if (jump.get()) pool.add(Action.JUMP);
        if (sneak.get()) pool.add(Action.SNEAK);
        if (swing.get()) pool.add(Action.SWING);
        if (rotate.get()) pool.add(Action.ROTATE);
        if (step.get()) pool.add(Action.STEP);
        if (!pool.isEmpty()) {
            current = pool.get(RANDOM.nextInt(pool.size()));
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
                player.swing(InteractionHand.MAIN_HAND);
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
        current = null;
    }

    private void scheduleNext() {
        int min = Math.min(minInterval.get(), maxInterval.get());
        int max = Math.max(minInterval.get(), maxInterval.get());
        countdown = (min + RANDOM.nextInt(max - min + 1)) * 20;
    }
}
