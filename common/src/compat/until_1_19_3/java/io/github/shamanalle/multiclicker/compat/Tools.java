package io.github.shamanalle.multiclicker.compat;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import java.util.List;

/** Tool kinds: item tags since Minecraft 1.19.4, item classes before. */
public final class Tools {
    public static final int NONE = -1;
    public static final int PICKAXE = 2;

    private static final List<Class<? extends Item>> KINDS = List.of(SwordItem.class, AxeItem.class, PickaxeItem.class,
            ShovelItem.class, HoeItem.class);

    private Tools() {
    }

    /** The kind of tool (sword, axe, pickaxe, shovel or hoe) as an index, {@link #NONE} for other items. */
    public static int kind(ItemStack stack) {
        for (int i = 0; i < KINDS.size(); i++) {
            if (KINDS.get(i).isInstance(stack.getItem())) {
                return i;
            }
        }
        return NONE;
    }
}
