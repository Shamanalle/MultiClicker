package pro.mikey.autoclicker.module.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.Input;
import pro.mikey.autoclicker.util.Inventories;

import java.util.Set;

/**
 * Eats the best food from the hotbar when hungry. The attack and use clickers pause while eating
 * and the previously selected slot is restored afterwards.
 */
public class AutoEatModule extends Module {
    private static final Set<Item> HARMFUL_FOOD = Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE,
            Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.CHICKEN, Items.SUSPICIOUS_STEW, Items.CHORUS_FRUIT);
    private static final Set<Item> VALUABLE_FOOD = Set.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_CARROT);
    private static final int EAT_TIMEOUT = 100;
    private static final int RETRY_COOLDOWN = 20;

    public final IntSetting hunger = add(new IntSetting("hunger", 14, 1, 19, Unit.HUNGER));
    public final BoolSetting avoidHarmful = add(new BoolSetting("avoid_harmful", true));
    public final BoolSetting keepValuable = add(new BoolSetting("keep_valuable", true));

    private enum State {
        IDLE, EATING
    }

    private State state = State.IDLE;
    private int previousSlot = -1;
    private int timer;
    private int cooldown;
    private boolean startedUsing;

    public AutoEatModule() {
        super("auto_eat", Category.SURVIVAL, true, false);
    }

    /** True while eating; the clicker pauses so it does not interrupt the meal. */
    public boolean isBusy() {
        return state == State.EATING && isRunning();
    }

    @Override
    public void stop(Minecraft mc) {
        if (state == State.EATING) {
            finish(mc);
        }
        cooldown = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (state == State.EATING) {
            tickEating(mc, player);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (mc.screen != null || player.isCreative() || player.isSpectator()
                || player.getFoodData().getFoodLevel() > hunger.get() || player.isUsingItem()
                || MultiClicker.get().clicker().isSwappingWeapon()) {
            return;
        }
        int slot = findFood(player);
        if (slot == -1) {
            return;
        }
        int selected = player.getInventory().getSelectedSlot();
        previousSlot = slot != selected ? selected : -1;
        Inventories.selectSlot(player, slot);
        state = State.EATING;
        timer = 0;
        startedUsing = false;
    }

    private void tickEating(Minecraft mc, LocalPlayer player) {
        timer++;
        if (mc.screen != null || timer > EAT_TIMEOUT || !isFood(player.getMainHandItem())) {
            finish(mc);
            return;
        }
        if (player.isUsingItem()) {
            startedUsing = true;
            Input.hold(mc.options.keyUse);
        } else if (startedUsing) {
            // Finished the item; keep eating next tick if still hungry, otherwise stop.
            finish(mc);
        } else if (timer == 1) {
            Input.click(mc.options.keyUse);
        } else {
            Input.hold(mc.options.keyUse);
        }
    }

    private void finish(Minecraft mc) {
        Input.release(mc.options.keyUse);
        if (mc.player != null && previousSlot != -1) {
            Inventories.selectSlot(mc.player, previousSlot);
        }
        previousSlot = -1;
        state = State.IDLE;
        cooldown = startedUsing ? 2 : RETRY_COOLDOWN;
    }

    private int findFood(LocalPlayer player) {
        int bestSlot = -1;
        float bestScore = 0;
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isAcceptableFood(stack, avoidHarmful.get(), keepValuable.get()) && foodScore(stack) > bestScore) {
                bestScore = foodScore(stack);
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    /** Food that is safe to eat automatically under the given rules. */
    public static boolean isAcceptableFood(ItemStack stack, boolean avoidHarmful, boolean keepValuable) {
        return isFood(stack)
                && !(avoidHarmful && HARMFUL_FOOD.contains(stack.getItem()))
                && !(keepValuable && VALUABLE_FOOD.contains(stack.getItem()));
    }

    /** Higher is better: nutrition plus saturation. */
    public static float foodScore(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? 0 : food.nutrition() + food.saturation();
    }
}
