package io.github.shamanalle.multiclicker.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Potion contents: an item component since 1.20.5, NBT before; some effects were renamed in 1.21.5. */
public final class Potions {
    private Potions() {
    }

    /** A potion that is drunk (not thrown). */
    public static boolean isDrinkable(ItemStack stack) {
        return stack.is(Items.POTION);
    }

    public static boolean isSplash(ItemStack stack) {
        return stack.is(Items.SPLASH_POTION);
    }

    /** Gives health back: instant health or regeneration. */
    public static boolean heals(ItemStack stack) {
        for (MobEffectInstance effect : effects(stack)) {
            if (effect.is(MobEffects.INSTANT_HEALTH) || effect.is(MobEffects.REGENERATION)) {
                return true;
            }
        }
        return false;
    }

    public static boolean resistsFire(ItemStack stack) {
        for (MobEffectInstance effect : effects(stack)) {
            if (effect.is(MobEffects.FIRE_RESISTANCE)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFireResistant(LivingEntity entity) {
        return entity.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    public static boolean isRegenerating(LivingEntity entity) {
        return entity.hasEffect(MobEffects.REGENERATION);
    }

    private static Iterable<MobEffectInstance> effects(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents == null ? List.of() : contents.getAllEffects();
    }
}
