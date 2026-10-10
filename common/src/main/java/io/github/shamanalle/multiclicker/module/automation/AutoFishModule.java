package io.github.shamanalle.multiclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.mixin.FishingHookAccessor;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Input;
import io.github.shamanalle.multiclicker.util.Inventories;

import java.util.Random;

/**
 * Reels in when a fish bites and casts the rod again. The rod is used directly (not through the
 * use key), so it works from either hand, never clicks the block in front of the player and does
 * not fight with the use clicker.
 */
public class AutoFishModule extends Module {
    private static final Random RANDOM = new Random();
    /** How long to wait for the hook entity to appear after casting. */
    private static final int CAST_GRACE = 40;

    public final IntSetting reaction = add(new IntSetting("reaction", 6, 0, 40, Unit.TICKS));
    public final IntSetting jitter = add(new IntSetting("jitter", 4, 0, 20, Unit.TICKS).zeroMeans("options.off"));
    public final BoolSetting autoCast = add(new BoolSetting("auto_cast", true));
    public final IntSetting recastDelay = add(new IntSetting("recast_delay", 20, 5, 100, Unit.TICKS)
            .visibleWhen(autoCast::get));
    public final IntSetting timeout = add(new IntSetting("timeout", 60, 0, 180, Unit.SECONDS).zeroMeans("options.off")
            .visibleWhen(autoCast::get));
    public final IntSetting protectRod = add(new IntSetting("protect_rod", 5, 0, 64, Unit.NONE).zeroMeans("options.off"));
    public final IntSetting catchLimit = add(new IntSetting("catch_limit", 0, 0, 1000, Unit.NONE)
            .zeroMeans("multiclicker.value.unlimited"));

    private enum State {
        WAITING, REELING, RECASTING
    }

    private State state = State.WAITING;
    private int timer;
    private int waitTicks;
    private int castGrace;
    private int catches;

    public AutoFishModule() {
        super("auto_fish", Category.AUTOMATION, true, false);
    }

    /** While fishing, the use key belongs to this module and the use clicker stays idle. */
    public boolean controlsUseKey() {
        Minecraft mc = Minecraft.getInstance();
        return isRunning() && mc.player != null && isHoldingRod(mc.player);
    }

    @Override
    public void start(Minecraft mc) {
        state = State.WAITING;
        timer = 0;
        waitTicks = 0;
        castGrace = 0;
        catches = 0;
    }

    @Override
    public void stop(Minecraft mc) {
        state = State.WAITING;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        // Eating takes the main hand for a moment; the fishing state continues afterwards.
        if (Screens.current(mc) != null || !isHoldingRod(player) || MultiClicker.get().autoEat().isBusy()) {
            return;
        }
        if (Inventories.isNearlyBroken(rod(player), protectRod.get())) {
            MultiClicker.get().setActive(false, Component.translatable("multiclicker.message.rod_protected"));
            return;
        }
        FishingHook hook = player.fishing;
        switch (state) {
            case WAITING -> {
                if (hook == null) {
                    if (castGrace > 0) {
                        castGrace--;
                    } else if (autoCast.get()) {
                        state = State.RECASTING;
                        timer = 0;
                    }
                    return;
                }
                castGrace = 0;
                waitTicks++;
                if (((FishingHookAccessor) hook).multiclicker$isBiting()) {
                    state = State.REELING;
                    timer = reaction.get() + randomDelay();
                } else if (autoCast.get() && timeout.get() > 0 && waitTicks > timeout.get() * 20) {
                    // Nothing bites (hook stuck or in a bad spot): pull it in and try again.
                    useRod(mc, player);
                    state = State.RECASTING;
                    timer = recastDelay.get();
                }
            }
            case REELING -> {
                if (hook == null || !((FishingHookAccessor) hook).multiclicker$isBiting()) {
                    // The fish got away before the reaction delay ran out: keep waiting on the same cast.
                    state = State.WAITING;
                } else if (--timer <= 0) {
                    if (!useRod(mc, player)) {
                        return; // hands busy this tick: try again on the next one
                    }
                    catches++;
                    MultiClicker.get().notifications().fishCaught(mc, catches);
                    if (catchLimit.get() > 0 && catches >= catchLimit.get()) {
                        MultiClicker.get().setActive(false,
                                Component.translatable("multiclicker.message.catch_limit", catches));
                        return;
                    }
                    state = State.RECASTING;
                    timer = recastDelay.get() + randomDelay();
                }
            }
            case RECASTING -> {
                if (--timer > 0) {
                    return;
                }
                if (player.fishing == null && autoCast.get()) {
                    useRod(mc, player);
                    castGrace = CAST_GRACE;
                }
                state = State.WAITING;
                waitTicks = 0;
            }
        }
    }

    private static boolean useRod(Minecraft mc, LocalPlayer player) {
        return Input.useItem(mc, isRod(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    private int randomDelay() {
        return jitter.get() > 0 ? RANDOM.nextInt(jitter.get() + 1) : 0;
    }

    /** A rod in either hand works: it is used directly, whatever the other hand holds. */
    private static boolean isHoldingRod(LocalPlayer player) {
        return isRod(player.getMainHandItem()) || isRod(player.getOffhandItem());
    }

    private static boolean isRod(ItemStack stack) {
        return stack.getItem() instanceof FishingRodItem;
    }

    private static ItemStack rod(LocalPlayer player) {
        return isRod(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
    }

    @Override
    public String hudInfo() {
        return Integer.toString(catches);
    }
}
