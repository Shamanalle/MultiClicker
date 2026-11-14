package io.github.shamanalle.multiclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Entities;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.util.Input;

/** Holds the forward key, optionally sprinting and jumping over obstacles. */
public class AutoWalkModule extends Module {
    public final BoolSetting sprint = add(new BoolSetting("sprint", false));
    public final BoolSetting jumpObstacles = add(new BoolSetting("jump_obstacles", true));

    private boolean walking;
    private boolean sprinting;
    private boolean jumping;
    private int inventoryPauseTicks;

    public AutoWalkModule() {
        super("auto_walk", Category.AUTOMATION, true, false);
    }

    @Override
    public void start(Minecraft mc) {
        inventoryPauseTicks = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (Screens.current(mc) != null) {
            stop(mc);
            return;
        }
        if (inventoryPauseTicks > 0) {
            releaseKeys(mc);
            inventoryPauseTicks--;
            return;
        }
        walking = true;
        Input.hold(mc.options.keyUp);
        if (sprint.get()) {
            Input.hold(mc.options.keySprint);
            sprinting = true;
        } else if (sprinting) {
            Input.release(mc.options.keySprint);
            sprinting = false;
        }
        boolean shouldJump = jumpObstacles.get() && player.horizontalCollision && Entities.onGround(player);
        if (shouldJump) {
            Input.hold(mc.options.keyJump);
            jumping = true;
        } else if (jumping) {
            releaseJump(mc);
        }
    }

    /**
     * Briefly pauses automated walking so inventory-safe swaps can execute while stationary.
     *
     * @return {@code true} if walking keys were released (or are already paused) and no other
     *     movement key remains active
     */
    public boolean pauseForInventory(Minecraft mc) {
        if (!isRunning() || Input.isPlayerMoving(mc)) {
            return false;
        }
        if (inventoryPauseTicks > 0) {
            return !Input.isMovementActive(mc);
        }
        if (!walking && !sprinting && !jumping) {
            return false;
        }
        releaseKeys(mc);
        if (Input.isMovementActive(mc)) {
            return false;
        }
        inventoryPauseTicks = 2;
        return true;
    }

    /** Whether automated walking is momentarily paused while another module uses the main inventory. */
    public boolean isPausedForInventory() {
        return inventoryPauseTicks > 0;
    }

    @Override
    public void stop(Minecraft mc) {
        releaseKeys(mc);
        inventoryPauseTicks = 0;
    }

    private void releaseKeys(Minecraft mc) {
        if (walking) {
            Input.release(mc.options.keyUp);
            walking = false;
        }
        if (sprinting) {
            Input.release(mc.options.keySprint);
            sprinting = false;
        }
        if (jumping) {
            releaseJump(mc);
        }
    }

    private void releaseJump(Minecraft mc) {
        MultiClicker mod = MultiClicker.get();
        if (mod == null || !mod.clicker().isHoldingJump()) {
            Input.release(mc.options.keyJump);
        }
        jumping = false;
    }
}
