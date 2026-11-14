package io.github.shamanalle.multiclicker.module.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Potions;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Inventories;
import io.github.shamanalle.multiclicker.util.ItemUse;
import io.github.shamanalle.multiclicker.util.Rotations;

import java.util.function.Predicate;

/**
 * Drinks or throws a potion from the hotbar: healing or regeneration at low health, fire
 * resistance when burning. Splash potions are thrown at the feet: the view turns down smoothly
 * so the server sees human-paced pitch packets before the throw.
 */
public class AutoPotionModule extends Module {
    private static final int RETRY_COOLDOWN = 20;
    private static final int HEAL_COOLDOWN = 40;
    private static final float THROW_PITCH = 89.5F;
    private static final float THROW_READY_PITCH = 86.0F;
    private static final float PITCH_STEP = 45.0F;

    public final IntSetting health = add(new IntSetting("health", 40, 0, 95, Unit.PERCENT).zeroMeans("options.off"));
    public final BoolSetting fire = add(new BoolSetting("fire", true));
    public final BoolSetting splash = add(new BoolSetting("splash", true));

    private final ItemUse use = new ItemUse();
    private int cooldown;
    private int slot = -1;
    private boolean throwing;
    private boolean restoringPitch;
    private int aimedTicks;
    private boolean healing;
    private float savedPitch;

    public AutoPotionModule() {
        super("auto_potion", Category.SURVIVAL, true, false);
    }

    /** True while a potion is being drunk or thrown; the clicker and auto eat wait. */
    public boolean isBusy() {
        return slot != -1 && !restoringPitch && isRunning();
    }

    public boolean holdsUseKey() {
        return use.holdsUseKey() && isBusy();
    }

    @Override
    public void stop(Minecraft mc) {
        if (use.isActive()) {
            use.finish(mc);
        }
        if ((throwing || restoringPitch) && mc.player != null) {
            mc.player.setXRot(savedPitch);
        }
        slot = -1;
        throwing = false;
        restoringPitch = false;
        aimedTicks = 0;
        cooldown = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (restoringPitch) {
            if (cooldown > 0) {
                cooldown--;
            }
            if (Screens.current(mc) != null) {
                return;
            }
            float next = Rotations.clampPitch(Rotations.stepToward(mc, player.getXRot(), savedPitch, PITCH_STEP));
            player.setXRot(next);
            if (Math.abs(next - savedPitch) <= Math.max(0.05F, Rotations.gcdStep(mc))) {
                slot = -1;
                throwing = false;
                restoringPitch = false;
                aimedTicks = 0;
            }
            return;
        }
        if (slot != -1) {
            tickUse(mc, player);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        MultiClicker mod = MultiClicker.get();
        boolean usingItem = player.isUsingItem() && !mod.clicker().isHoldingUse();
        if (Screens.current(mc) != null || player.isCreative() || player.isSpectator() || player.isDeadOrDying()
                || usingItem || mod.autoEat().isBusy() || mod.clicker().isSwappingWeapon()) {
            return;
        }
        float percent = player.getHealth() / player.getMaxHealth() * 100.0F;
        boolean hurt = health.get() > 0 && percent <= health.get()
                && (!Potions.isRegenerating(player) || percent <= health.get() / 2.0F);
        boolean burning = fire.get() && (player.isOnFire() || player.isInLava()) && !Potions.isFireResistant(player);
        int found = hurt ? find(player, Potions::heals) : -1;
        healing = found != -1;
        if (found == -1 && burning) {
            found = find(player, Potions::resistsFire);
        }
        if (found == -1) {
            return;
        }
        slot = found;
        throwing = Potions.isSplash(player.getInventory().getItem(found));
        aimedTicks = 0;
        if (throwing) {
            savedPitch = player.getXRot();
            tickUse(mc, player);
        } else {
            use.begin(player, slot, false, Potions::isDrinkable);
            tickUse(mc, player);
        }
    }

    private void tickUse(Minecraft mc, LocalPlayer player) {
        if (throwing) {
            float next = Rotations.clampPitch(Rotations.stepToward(mc, player.getXRot(), THROW_PITCH, PITCH_STEP));
            player.setXRot(next);
            if (!use.isActive()) {
                // Step smoothly down and let vanilla send one downward rotation packet before throwing.
                if (next < THROW_READY_PITCH || aimedTicks++ == 0) {
                    return;
                }
                use.begin(player, slot, true, Potions::isSplash);
            }
        }
        ItemUse.Result result = use.tick(mc, player);
        if (result != ItemUse.Result.RUNNING) {
            done(mc, result == ItemUse.Result.USED);
        }
    }

    private void done(Minecraft mc, boolean used) {
        if (use.isActive()) {
            use.finish(mc);
        }
        cooldown = !used ? RETRY_COOLDOWN : healing ? HEAL_COOLDOWN : 2;
        if (throwing && mc.player != null) {
            restoringPitch = true;
        } else {
            slot = -1;
            throwing = false;
            aimedTicks = 0;
        }
    }

    /** The best hotbar slot with a potion of this kind: splash acts at once, or acts as fallback when drinkables run out. */
    private int find(LocalPlayer player, Predicate<ItemStack> effect) {
        int drinkable = -1;
        int splashSlot = -1;
        for (int i = 0; i < Inventories.HOTBAR_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!effect.test(stack)) {
                continue;
            }
            if (Potions.isSplash(stack)) {
                if (splash.get()) {
                    return i;
                }
                if (splashSlot == -1) {
                    splashSlot = i;
                }
            } else if (drinkable == -1 && Potions.isDrinkable(stack)) {
                drinkable = i;
            }
        }
        return drinkable != -1 ? drinkable : splashSlot;
    }
}
