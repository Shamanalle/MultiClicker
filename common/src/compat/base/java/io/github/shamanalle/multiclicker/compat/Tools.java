package io.github.shamanalle.multiclicker.compat;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Tool kinds: item tags since Minecraft 1.19.4, item classes before. */
public final class Tools {
    public static final int NONE = -1;
    public static final int PICKAXE = 2;

    private static final List<TagKey<Item>> KINDS = List.of(ItemTags.SWORDS, ItemTags.AXES, ItemTags.PICKAXES,
            ItemTags.SHOVELS, ItemTags.HOES);

    private Tools() {
    }

    /** The kind of tool (sword, axe, pickaxe, shovel or hoe) as an index, {@link #NONE} for other items. */
    public static int kind(ItemStack stack) {
        for (int i = 0; i < KINDS.size(); i++) {
            if (stack.is(KINDS.get(i))) {
                return i;
            }
        }
        return NONE;
    }
}
