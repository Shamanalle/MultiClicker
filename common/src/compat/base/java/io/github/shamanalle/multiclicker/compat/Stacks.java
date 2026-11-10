package io.github.shamanalle.multiclicker.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Item stack data: item components since 1.20.5, item methods and NBT tags before. */
public final class Stacks {
    private Stacks() {
    }

    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    /** Nutrition plus saturation, 0 for anything that is not food. */
    public static float foodValue(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? 0 : food.nutrition() + food.saturation();
    }

    /** Same item with the same data (enchantments, damage, name...); the count may differ. */
    public static boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /** Attack damage the item adds in the main hand. */
    public static double attackDamage(ItemStack stack) {
        return attackDamage(stack, 0.0);
    }

    /**
     * Total attack damage when holding {@code stack} in the main hand on top of {@code baseDamage},
     * applying additive, base-multiplied and total-multiplied attribute modifiers in vanilla order.
     */
    public static double attackDamage(ItemStack stack, double baseDamage) {
        double[] add = {baseDamage};
        double[] multBase = {0.0};
        double[] multTotal = {1.0};
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                    if (attribute.value() != Attributes.ATTACK_DAMAGE.value()) {
                        return;
                    }
                    switch (modifier.operation()) {
                        case ADD_VALUE -> add[0] += modifier.amount();
                        case ADD_MULTIPLIED_BASE -> multBase[0] += modifier.amount();
                        case ADD_MULTIPLIED_TOTAL -> multTotal[0] *= 1.0 + modifier.amount();
                    }
                });
        return (add[0] + add[0] * multBase[0]) * multTotal[0];
    }
}
