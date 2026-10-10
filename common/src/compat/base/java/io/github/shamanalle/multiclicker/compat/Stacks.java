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
        double[] total = {0};
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                    if (attribute.value() == Attributes.ATTACK_DAMAGE.value()
                            && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                        total[0] += modifier.amount();
                    }
                });
        return total[0];
    }
}
