package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Nullable;

/**
 * Enchantment levels of item stacks. Enchantments are registry entries since 1.21 and plain
 * objects before, so code outside this package never names them.
 */
public final class Enchants {
    @Nullable
    private final Registry<Enchantment> registry;

    private Enchants(@Nullable Registry<Enchantment> registry) {
        this.registry = registry;
    }

    /** The enchantments of the world the player is in. */
    public static Enchants of(Minecraft mc) {
        return new Enchants(mc.level == null ? null : Lookups.enchantments(mc));
    }

    public boolean hasLooting() {
        return holder(Enchantments.LOOTING) != null;
    }

    public int efficiency(ItemStack stack) {
        return level(Enchantments.EFFICIENCY, stack);
    }

    public int looting(ItemStack stack) {
        return level(Enchantments.LOOTING, stack);
    }

    /** Extra melee damage of the weapon's enchantments against this target. */
    public float damageBonus(ItemStack weapon, LivingEntity target) {
        float bonus = 0;
        int sharpness = level(Enchantments.SHARPNESS, weapon);
        if (sharpness > 0) {
            bonus += 0.5F * sharpness + 0.5F;
        }
        if (Lookups.isMobOfTag(target, EntityTypeTags.SENSITIVE_TO_SMITE)) {
            bonus += 2.5F * level(Enchantments.SMITE, weapon);
        }
        if (Lookups.isMobOfTag(target, EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            bonus += 2.5F * level(Enchantments.BANE_OF_ARTHROPODS, weapon);
        }
        return bonus;
    }

    private int level(ResourceKey<Enchantment> key, ItemStack stack) {
        Holder<Enchantment> holder = holder(key);
        return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    @Nullable
    private Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
        return registry == null ? null : Lookups.enchantment(registry, key);
    }
}
