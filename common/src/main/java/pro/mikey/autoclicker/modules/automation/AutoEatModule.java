package pro.mikey.autoclicker.modules.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;
import pro.mikey.autoclicker.util.InventoryUtils;

import java.util.List;
import java.util.Set;

/**
 * Auto-Eat module: swaps to the best food and eats when hunger is low.
 * Food is prioritized by nutrition × saturation, deprioritizing negative-effect
 * items.
 */
public class AutoEatModule implements Module {

    // Items known to have negative effects — heavily penalized in scoring
    private static final Set<net.minecraft.world.item.Item> NEGATIVE_EFFECT_FOODS = Set.of(
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.PUFFERFISH,
            Items.POISONOUS_POTATO,
            Items.CHORUS_FRUIT);

    public final BooleanSetting enabled = new BooleanSetting("auto_eat.enabled", "Авто-Еда", false)
            .withDescription("Автоматически ест, когда голод падает ниже порога. Ищет еду в хотбаре и оффхенде");
    public final IntSetting threshold = new IntSetting("auto_eat.threshold", "Порог голода", 14, 1, 20)
            .withDescription("Начинает есть при голоде ≤ этого значения. 20 = полная шкала, 6 = не может спринтовать");
    public final BooleanSetting protectRare = new BooleanSetting("auto_eat.protect_rare", "Защита редкой еды", true)
            .withDescription("Не ест Golden Apple, Enchanted Golden Apple. Защищает ценное от случайного поедания");
    public final BooleanSetting pauseClicker = new BooleanSetting("auto_eat.pause_clicker", "Пауза кликера", true)
            .withDescription("Приостанавливает автоклик ЛКМ пока идёт еда. Клики могут прерывать еду");

    // Flag accessible by CombatClickerModule to check if eating is in progress
    private boolean currentlyEating = false;

    public boolean isCurrentlyEating() {
        return currentlyEating && pauseClicker.get();
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, threshold, protectRare, pauseClicker);
    }

    // ── State ─────────────────────────────────────────────────────
    public enum State {
        IDLE, SWAPPING, EATING, EATING_OFFHAND, RETURN_SWAP
    }

    private State state = State.IDLE;
    private int timer = 0;
    private int preEatSlot = -1;

    @Override
    public String getId() {
        return "auto_eat";
    }

    @Override
    public String getDisplayName() {
        return "Авто-Еда";
    }

    @Override
    public Category getCategory() {
        return Category.AUTOMATION;
    }

    @Override
    public int tickPriority() {
        return 30;
    }

    @Override
    public void onDisable() {
        if (state != State.IDLE) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null)
                mc.options.keyUse.setDown(false);
            if (mc != null && mc.player != null && preEatSlot != -1) {
                InventoryUtils.setSelectedSlot(mc.player, preEatSlot);
            }
        }
        state = State.IDLE;
        timer = 0;
        preEatSlot = -1;
        currentlyEating = false;
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        int hunger = mc.player.getFoodData().getFoodLevel();

        switch (state) {
            case IDLE -> {
                if (hunger > threshold.get())
                    return false;
                ItemStack offhand = mc.player.getOffhandItem();
                if (isFoodItem(offhand) && !isRareProtected(offhand)) {
                    state = State.EATING_OFFHAND;
                    currentlyEating = true;
                    mc.options.keyUse.setDown(true);
                    timer = 0;
                    return true;
                }
                int foodSlot = findBestFoodSlot(mc);
                if (foodSlot == -1)
                    return false;
                preEatSlot = InventoryUtils.getSelectedSlot(mc.player);
                InventoryUtils.setSelectedSlot(mc.player, foodSlot);
                state = State.SWAPPING;
                timer = 0;
                return true;
            }
            case SWAPPING -> {
                timer++;
                if (timer >= 2) {
                    mc.options.keyUse.setDown(true);
                    state = State.EATING;
                    currentlyEating = true;
                    timer = 0;
                }
                return true;
            }
            case EATING, EATING_OFFHAND -> {
                timer++;
                if (timer > 80) {
                    // Safety timeout — eating took too long, abort
                    mc.options.keyUse.setDown(false);
                    currentlyEating = false;
                    state = State.IDLE;
                    if (preEatSlot != -1) {
                        InventoryUtils.setSelectedSlot(mc.player, preEatSlot);
                        preEatSlot = -1;
                    }
                } else if (!mc.player.isUsingItem() && timer > 5) {
                    // Normal completion — player finished eating
                    mc.options.keyUse.setDown(false);
                    currentlyEating = false;
                    if (state == State.EATING && preEatSlot != -1) {
                        state = State.RETURN_SWAP;
                        timer = 0;
                    } else {
                        state = State.IDLE;
                        preEatSlot = -1;
                    }
                }
                return true;
            }
            case RETURN_SWAP -> {
                InventoryUtils.setSelectedSlot(mc.player, preEatSlot);
                preEatSlot = -1;
                state = State.IDLE;
                currentlyEating = false;
                return false;
            }
        }
        return false;
    }

    /**
     * Find the best food slot in the hotbar (0-8), scored by nutrition ×
     * saturation.
     * Items with known negative effects are heavily penalized.
     */
    private int findBestFoodSlot(Minecraft mc) {
        int bestSlot = -1;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!isFoodItem(stack))
                continue;
            // #6 protectRare
            if (isRareProtected(stack))
                continue;

            float score = computeFoodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    /**
     * Scores a food item: nutrition × saturationModifier, minus penalty for
     * negative effects.
     */
    private static float computeFoodScore(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null)
            return -1000f;

        float nutrition = food.nutrition();
        float saturation = food.saturation();
        float score = nutrition * saturation;

        // Heavy penalty for known negative-effect foods
        if (NEGATIVE_EFFECT_FOODS.contains(stack.getItem())) {
            score -= 100f;
        }

        return score;
    }

    public static boolean isFoodItem(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null;
    }

    /**
     * Returns true if this item is "rare" and protectRare is enabled.
     */
    private boolean isRareProtected(ItemStack stack) {
        if (!protectRare.get())
            return false;
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }
}
