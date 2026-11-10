package io.github.shamanalle.multiclicker.module.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.compat.Stacks;
import io.github.shamanalle.multiclicker.compat.Tools;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.stats.Stat;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Inventories;
import io.github.shamanalle.multiclicker.util.ItemUse;

import java.util.Set;

/**
 * Eats the best food when hungry, from the hotbar or (optionally) by bringing a food stack from the
 * main inventory. The attack and use clickers pause while eating and the previously selected slot
 * is restored afterwards. The food is eaten directly, so eating never opens the chest, door or
 * villager trade under the crosshair.
 */
public class AutoEatModule extends Module {
    private static final Set<Item> HARMFUL_FOOD = Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE,
            Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.CHICKEN, Items.SUSPICIOUS_STEW, Items.CHORUS_FRUIT);
    private static final Set<Item> VALUABLE_FOOD = Set.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_CARROT);
    private static final int RETRY_COOLDOWN = 20;

    public final IntSetting hunger = add(new IntSetting("hunger", 14, 1, 19, Unit.HUNGER));
    public final BoolSetting fromInventory = add(new BoolSetting("from_inventory", true));
    public final BoolSetting avoidHarmful = add(new BoolSetting("avoid_harmful", true));
    public final BoolSetting keepValuable = add(new BoolSetting("keep_valuable", true));

    private final ItemUse meal = new ItemUse();
    private int cooldown;
    private int borrowedMainSlot = -1;
    private int borrowedHotbarSlot = -1;
    private boolean pendingBorrowedEat;

    public AutoEatModule() {
        super("auto_eat", Category.SURVIVAL, true, false);
    }

    /** True while eating; the clicker, auto tool and auto fish wait so they do not interrupt the meal. */
    public boolean isBusy() {
        return (meal.isActive() || pendingBorrowedEat) && isRunning();
    }

    /** True while the use key is held down for the meal; the use clicker must not release it then. */
    public boolean holdsUseKey() {
        return meal.holdsUseKey() && isBusy();
    }

    @Override
    public void stop(Minecraft mc) {
        if (meal.isActive()) {
            meal.finish(mc);
        }
        pendingBorrowedEat = false;
        returnBorrowedSlot(mc, true);
        cooldown = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (meal.isActive()) {
            tickEating(mc, player);
            return;
        }
        if (pendingBorrowedEat) {
            pendingBorrowedEat = false;
            if (borrowedHotbarSlot >= 0
                    && isAcceptableFood(player.getInventory().getItem(borrowedHotbarSlot), avoidHarmful.get(), keepValuable.get())) {
                meal.begin(player, borrowedHotbarSlot, false, AutoEatModule::isFood);
                tickEating(mc, player);
            } else {
                returnBorrowedSlot(mc, false);
            }
            return;
        }
        boolean usingItem = player.isUsingItem() && !MultiClicker.get().clicker().isHoldingUse();
        boolean canEatNow = Screens.current(mc) == null && !player.isCreative() && !player.isSpectator()
                && !player.isDeadOrDying() && !usingItem
                && !MultiClicker.get().clicker().isSwappingWeapon() && !MultiClicker.get().autoPotion().isBusy();
        if (borrowedMainSlot != -1) {
            ItemStack borrowed = player.getInventory().getItem(borrowedHotbarSlot);
            if (canEatNow && player.getFoodData().getFoodLevel() <= hunger.get()
                    && isAcceptableFood(borrowed, avoidHarmful.get(), keepValuable.get())) {
                if (cooldown > 0) {
                    cooldown--;
                    return;
                }
                meal.begin(player, borrowedHotbarSlot, false, AutoEatModule::isFood);
                tickEating(mc, player);
                return;
            }
            returnBorrowedSlot(mc, false);
            if (borrowedMainSlot != -1) {
                return;
            }
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        // An item the player is using is not interrupted, but one the use clicker holds up for good
        // (a shield) is: the clicker lets go once the meal starts.
        if (!canEatNow || player.getFoodData().getFoodLevel() > hunger.get()) {
            return;
        }
        int slot = findFood(player, 0, Inventories.HOTBAR_SIZE);
        if (slot == -1 && fromInventory.get()) {
            int mainSlot = findFood(player, Inventories.HOTBAR_SIZE, Inventories.MAIN_SIZE);
            if (mainSlot != -1) {
                if (!Inventories.canClickMainInventory(mc)) {
                    return;
                }
                int emptySlot = findEmptyHotbarSlot(player.getInventory());
                if (emptySlot != -1) {
                    Inventories.swapWithHotbar(mc, mainSlot, emptySlot);
                    cooldown = 1;
                } else {
                    int borrow = chooseBorrowSlot(player.getInventory());
                    Inventories.swapWithHotbar(mc, mainSlot, borrow);
                    borrowedMainSlot = mainSlot;
                    borrowedHotbarSlot = borrow;
                    pendingBorrowedEat = true;
                }
                return;
            }
        }
        if (slot == -1) {
            MultiClicker.get().notifications().noFood(mc);
            return;
        }
        meal.begin(player, slot, false, AutoEatModule::isFood);
        tickEating(mc, player);
    }

    private void returnBorrowedSlot(Minecraft mc, boolean immediate) {
        if (borrowedMainSlot == -1) {
            return;
        }
        if (immediate ? Inventories.canClickInventory(mc) : Inventories.canClickMainInventory(mc)) {
            Inventories.swapWithHotbar(mc, borrowedMainSlot, borrowedHotbarSlot);
            borrowedMainSlot = -1;
            borrowedHotbarSlot = -1;
        }
    }

    private static int findEmptyHotbarSlot(Inventory inventory) {
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static int chooseBorrowSlot(Inventory inventory) {
        int selected = Slots.selected(inventory);
        for (int slot = Inventories.HOTBAR_SIZE - 1; slot >= 0; slot--) {
            ItemStack stack = inventory.getItem(slot);
            if (slot != selected && !Tools.isWeapon(stack) && !stack.is(Items.TOTEM_OF_UNDYING)) {
                return slot;
            }
        }
        return (selected + Inventories.HOTBAR_SIZE - 1) % Inventories.HOTBAR_SIZE;
    }

    private void tickEating(Minecraft mc, LocalPlayer player) {
        ItemUse.Result result = meal.tick(mc, player);
        if (result == ItemUse.Result.USED) {
            MultiClicker.get().stats().count(Stat.FOOD);
        }
        if (result != ItemUse.Result.RUNNING) {
            // Finished the item; the next tick decides whether to eat another one.
            cooldown = meal.startedUsing() ? 2 : RETRY_COOLDOWN;
        }
    }

    private int findFood(LocalPlayer player, int fromSlot, int toSlot) {
        int bestSlot = -1;
        float bestScore = 0;
        for (int slot = fromSlot; slot < toSlot; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isAcceptableFood(stack, avoidHarmful.get(), keepValuable.get()) && foodScore(stack) > bestScore) {
                bestScore = foodScore(stack);
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    public static boolean isFood(ItemStack stack) {
        return Stacks.isFood(stack);
    }

    /** Food that is safe to eat automatically under the given rules. */
    public static boolean isAcceptableFood(ItemStack stack, boolean avoidHarmful, boolean keepValuable) {
        return isFood(stack)
                && !(avoidHarmful && HARMFUL_FOOD.contains(stack.getItem()))
                && !(keepValuable && VALUABLE_FOOD.contains(stack.getItem()));
    }

    /** Higher is better: nutrition plus saturation. */
    public static float foodScore(ItemStack stack) {
        return Stacks.foodValue(stack);
    }
}
