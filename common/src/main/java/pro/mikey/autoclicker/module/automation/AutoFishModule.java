package pro.mikey.autoclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.mixin.FishingHookAccessor;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.Input;
import pro.mikey.autoclicker.util.Inventories;

import java.util.Random;

/** Reels in when a fish bites and casts the rod again. */
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
    private boolean pressed;
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
        if (pressed) {
            Input.release(mc.options.keyUse);
            pressed = false;
        }
        state = State.WAITING;
    }

    @Override
    public void tick(Minecraft mc) {
        if (pressed) {
            Input.release(mc.options.keyUse);
            pressed = false;
        }
        LocalPlayer player = mc.player;
        if (mc.screen != null || !isHoldingRod(player)) {
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
                    useRod(mc);
                    state = State.RECASTING;
                    timer = recastDelay.get();
                }
            }
            case REELING -> {
                if (hook == null) {
                    state = State.WAITING;
                } else if (--timer <= 0) {
                    useRod(mc);
                    catches++;
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
                    useRod(mc);
                    castGrace = CAST_GRACE;
                }
                state = State.WAITING;
                waitTicks = 0;
            }
        }
    }

    private void useRod(Minecraft mc) {
        Input.click(mc.options.keyUse);
        pressed = true;
    }

    private int randomDelay() {
        return jitter.get() > 0 ? RANDOM.nextInt(jitter.get() + 1) : 0;
    }

    /** The rod must be in the main hand, or in the offhand with an empty main hand. */
    private static boolean isHoldingRod(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof FishingRodItem
                || player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof FishingRodItem;
    }

    private static ItemStack rod(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof FishingRodItem
                ? player.getMainHandItem() : player.getOffhandItem();
    }

    @Override
    public String hudInfo() {
        return Integer.toString(catches);
    }
}
