package io.github.shamanalle.multiclicker.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.compat.Slots;

import java.util.function.Predicate;

/**
 * Uses an item from the hotbar the way auto eat and auto potion do: selects it, uses it directly
 * (so the block or entity under the crosshair is never touched), holds the use key until the item
 * is finished and selects the previous slot again.
 */
public final class ItemUse {
    private static final int TIMEOUT = 100;
    private static final int START_RETRY = 4;

    public enum Result {
        RUNNING, USED, FAILED
    }

    private int previousSlot = -1;
    private int slot = -1;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private boolean active;
    private int timer;
    private boolean startedUsing;
    private boolean holdingUse;
    private boolean instant;
    private Predicate<ItemStack> stillValid = stack -> false;
    /** The stack as it was when the use started, to tell whether it was used up. */
    private ItemStack startStack = ItemStack.EMPTY;

    public boolean isActive() {
        return active;
    }

    /** True while the use key is held down for the item; the use clicker must not release it then. */
    public boolean holdsUseKey() {
        return holdingUse;
    }

    /**
     * Selects the slot; {@link #tick} does the rest.
     *
     * @param instant used in one go (a splash potion), not held like food
     * @param valid whether the selected item is still the one to use
     */
    public void begin(LocalPlayer player, int hotbarSlot, boolean instant, Predicate<ItemStack> valid) {
        int selected = Slots.selected(player.getInventory());
        previousSlot = hotbarSlot != selected ? selected : -1;
        slot = hotbarSlot;
        hand = InteractionHand.MAIN_HAND;
        active = true;
        this.instant = instant;
        stillValid = valid;
        timer = 0;
        startedUsing = false;
        holdingUse = false;
        startStack = player.getInventory().getItem(hotbarSlot).copy();
        Inventories.selectSlot(player, hotbarSlot);
    }

    /** Begins using the stack already held in the player's offhand without touching hotbar selection. */
    public void beginOffhand(LocalPlayer player, boolean instant, Predicate<ItemStack> valid) {
        previousSlot = -1;
        slot = -1;
        hand = InteractionHand.OFF_HAND;
        active = true;
        this.instant = instant;
        stillValid = valid;
        timer = 0;
        startedUsing = false;
        holdingUse = false;
        startStack = player.getOffhandItem().copy();
    }

    public Result tick(Minecraft mc, LocalPlayer player) {
        timer++;
        ItemStack activeStack = hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem();
        if (Screens.current(mc) != null || timer > TIMEOUT
                || (hand == InteractionHand.MAIN_HAND && Slots.selected(player.getInventory()) != slot)
                || !stillValid.test(activeStack) && !startedUsing) {
            finish(mc);
            return Result.FAILED;
        }
        if (!player.isUsingItem()) {
            if (startedUsing) {
                ItemStack current = hand == InteractionHand.OFF_HAND
                        ? player.getOffhandItem() : player.getInventory().getItem(slot);
                boolean used = wasUsed(current);
                finish(mc);
                return used ? Result.USED : Result.FAILED;
            }
            // Retry right away on ticks 1-2 and every few ticks after: the first attempt can fail while mining.
            boolean retryNow = timer <= 2 || timer % START_RETRY == 1;
            if (!retryNow || !Input.useItem(mc, hand)) {
                return Result.RUNNING;
            }
            if (instant) {
                finish(mc);
                return Result.USED;
            }
            if (!player.isUsingItem()) {
                return Result.RUNNING;
            }
        }
        startedUsing = true;
        // Vanilla stops using an item as soon as the use key is up.
        Input.hold(mc.options.keyUse);
        holdingUse = true;
        return Result.RUNNING;
    }

    public void finish(Minecraft mc) {
        if (holdingUse) {
            Input.release(mc.options.keyUse);
            holdingUse = false;
        }
        // Switch back only if the item is still selected: a slot the player picked meanwhile wins.
        if (mc.player != null && previousSlot != -1 && Slots.selected(mc.player.getInventory()) == slot) {
            Inventories.selectSlot(mc.player, previousSlot);
        }
        active = false;
        previousSlot = -1;
        slot = -1;
        hand = InteractionHand.MAIN_HAND;
    }

    /** Whether the use went through: the use clicker may have used the same item. */
    public boolean startedUsing() {
        return startedUsing;
    }

    /** One of the stack is gone (or it turned into its bowl or bottle). */
    private boolean wasUsed(ItemStack now) {
        return !now.is(startStack.getItem()) || now.getCount() < startStack.getCount();
    }
}
