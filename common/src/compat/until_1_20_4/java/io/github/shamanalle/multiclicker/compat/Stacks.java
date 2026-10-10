package io.github.shamanalle.multiclicker.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** Item stack data: item components since 1.20.5, item methods and NBT tags before. */
public final class Stacks {
    private Stacks() {
    }

    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.isEdible();
    }

    /** Nutrition plus saturation, 0 for anything that is not food. */
    public static float foodValue(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        return food == null ? 0 : food.getNutrition() * (1 + food.getSaturationModifier() * 2);
    }

    /** Same item with the same data (enchantments, damage, name...); the count may differ. */
    public static boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    /** Attack damage the item adds in the main hand. */
    public static double attackDamage(ItemStack stack) {
        double total = 0;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += modifier.getAmount();
            }
        }
        return total;
    }
}
