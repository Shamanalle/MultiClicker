package io.github.shamanalle.multiclicker.compat;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

import java.util.List;

/** Tool kinds: item tags since Minecraft 1.19.4, item classes before. */
public final class Tools {
    public static final int NONE = -1;
    public static final int SWORD = 0;
    public static final int AXE = 1;
    public static final int PICKAXE = 2;
    public static final int SHOVEL = 3;
    public static final int HOE = 4;
    public static final int FISHING_ROD = 5;
    public static final int SPEAR = 6;

    private static final List<TagKey<Item>> KINDS = List.of(ItemTags.SWORDS, ItemTags.AXES, ItemTags.PICKAXES,
            ItemTags.SHOVELS, ItemTags.HOES);

    private Tools() {
    }

    /** The kind of tool (sword, axe, pickaxe, shovel, hoe or rod) as an index, {@link #NONE} for other items. */
    public static int kind(ItemStack stack) {
        for (int i = 0; i < KINDS.size(); i++) {
            if (stack.is(KINDS.get(i))) {
                return i;
            }
        }
        if (stack.getItem() instanceof FishingRodItem) {
            return FISHING_ROD;
        }
        return NONE;
    }

    /** True for melee weapons: swords, axes, tridents and maces. */
    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int k = kind(stack);
        return k == SWORD || k == AXE || k == SPEAR
                || stack.getItem() instanceof TridentItem
                || "minecraft:mace".equals(Ids.itemId(stack.getItem()));
    }
}
