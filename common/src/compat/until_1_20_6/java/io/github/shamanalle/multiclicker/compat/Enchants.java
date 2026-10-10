package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Enchantment levels of item stacks. Enchantments are registry entries since 1.21 and plain
 * objects before, so code outside this package never names them.
 */
public final class Enchants {
    private static final Enchants INSTANCE = new Enchants();

    private Enchants() {
    }

    /** The enchantments of the world the player is in. */
    public static Enchants of(Minecraft mc) {
        return INSTANCE;
    }

    public boolean hasLooting() {
        return true;
    }

    public int efficiency(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY, stack);
    }

    public int looting(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOOTING, stack);
    }

    /** Extra melee damage of the weapon's enchantments against this target. */
    public float damageBonus(ItemStack weapon, LivingEntity target) {
        return EnchantmentHelper.getDamageBonus(weapon, target.getType());
    }
}
