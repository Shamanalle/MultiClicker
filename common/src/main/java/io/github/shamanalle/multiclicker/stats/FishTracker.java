package io.github.shamanalle.multiclicker.stats;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import io.github.shamanalle.multiclicker.compat.Ids;

import java.util.HashMap;
import java.util.Map;

/**
 * Finds out what a catch was. The server throws the loot from the hook to the player as an item
 * entity, which is looked for near the hook right after reeling in. Some server plugins put the
 * loot straight into the inventory instead; when no item shows up, what was added to the
 * inventory meanwhile counts as the catch.
 */
final class FishTracker {
    /** How long to look for the loot after reeling in. */
    private static final int WINDOW = 40;
    /** After the first item, how long to wait for more (plugins may throw several). */
    private static final int SETTLE = 5;
    private static final double RADIUS = 3.0;

    private final Statistics stats;
    private final IntSet seen = new IntOpenHashSet();
    private final Map<Item, Integer> inventoryBefore = new HashMap<>();
    private Vec3 hook = Vec3.ZERO;
    private int ticksLeft;
    private boolean found;

    FishTracker(Statistics stats) {
        this.stats = stats;
    }

    void clear() {
        ticksLeft = 0;
    }

    void onCatch(Minecraft mc, FishingHook fishingHook) {
        hook = fishingHook.position();
        seen.clear();
        // Items already lying around are not the catch.
        for (ItemEntity item : itemsNear(mc, hook, RADIUS * 2)) {
            seen.add(item.getId());
        }
        inventoryBefore.clear();
        count(mc.player.getInventory(), inventoryBefore);
        ticksLeft = WINDOW;
        found = false;
    }

    void tick(Minecraft mc) {
        if (ticksLeft <= 0) {
            return;
        }
        for (ItemEntity item : itemsNear(mc, hook, RADIUS)) {
            ItemStack stack = item.getItem();
            // The stack arrives in a separate packet; an empty one is looked at again next tick.
            if (stack.isEmpty() || !seen.add(item.getId()) || item.getAge() > WINDOW) {
                continue;
            }
            stats.loot(Ids.itemId(stack.getItem()), stack.getCount(), stack.isEnchanted());
            if (!found) {
                found = true;
                ticksLeft = Math.min(ticksLeft, SETTLE);
            }
        }
        if (--ticksLeft == 0 && !found) {
            Map<Item, Integer> now = new HashMap<>();
            count(mc.player.getInventory(), now);
            now.forEach((item, amount) -> {
                int added = amount - inventoryBefore.getOrDefault(item, 0);
                if (added > 0) {
                    stats.loot(Ids.itemId(item), added, false);
                }
            });
        }
    }

    private static Iterable<ItemEntity> itemsNear(Minecraft mc, Vec3 pos, double radius) {
        return mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius));
    }

    private static void count(Inventory inventory, Map<Item, Integer> counts) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }
}
