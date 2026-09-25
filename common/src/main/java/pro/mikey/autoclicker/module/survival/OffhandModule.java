package pro.mikey.autoclicker.module.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.IntSetting;
import pro.mikey.autoclicker.setting.Unit;
import pro.mikey.autoclicker.util.Inventories;

import java.util.function.Predicate;

/**
 * Keeps the most useful item in the offhand, by priority:
 * totem (low health) &gt; food (hungry) &gt; torch (holding a pickaxe) &gt; shield.
 *
 * <p>Each rule has hysteresis so the offhand does not flip back and forth around a threshold,
 * and nothing but an emergency totem is swapped while an item is being used.</p>
 */
public class OffhandModule extends Module {
    /** Ticks to wait for the server to confirm a swap before trying again. */
    private static final int SWAP_COOLDOWN = 10;
    /** A totem stays in the offhand until health is this many percent above the threshold. */
    private static final int TOTEM_HYSTERESIS = 20;
    private static final int FULL_HUNGER = 20;

    public final IntSetting totemHealth = add(new IntSetting("totem_health", 100, 0, 100, Unit.PERCENT)
            .zeroMeans("options.off"));
    public final IntSetting foodHunger = add(new IntSetting("food_hunger", 0, 0, 19, Unit.HUNGER)
            .zeroMeans("options.off"));
    public final BoolSetting torchWithPickaxe = add(new BoolSetting("torch_with_pickaxe", false));
    public final BoolSetting shield = add(new BoolSetting("shield", false));

    private int cooldown;

    private enum Want {
        TOTEM(stack -> stack.is(Items.TOTEM_OF_UNDYING)),
        FOOD(stack -> AutoEatModule.isAcceptableFood(stack, true, true)),
        TORCH(stack -> stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)),
        SHIELD(stack -> stack.is(Items.SHIELD));

        final Predicate<ItemStack> matcher;

        Want(Predicate<ItemStack> matcher) {
            this.matcher = matcher;
        }
    }

    public OffhandModule() {
        super("offhand", Category.SURVIVAL, true, false);
    }

    @Override
    public void stop(Minecraft mc) {
        cooldown = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        LocalPlayer player = mc.player;
        if (player.isCreative() || player.isSpectator() || !Inventories.canClickInventory(mc)) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        float health = player.getHealth() / player.getMaxHealth() * 100.0F;
        int hunger = player.getFoodData().getFoodLevel();

        for (Want want : Want.values()) {
            if (!wants(want, player, offhand, health, hunger)) {
                continue;
            }
            if (want.matcher.test(offhand)) {
                return; // The best item we want is already there.
            }
            // Only an emergency totem may interrupt eating, blocking or drawing a bow.
            if (want != Want.TOTEM && player.isUsingItem()) {
                return;
            }
            int slot = find(player, want);
            if (slot != -1) {
                Inventories.swapWithOffhand(mc, slot);
                cooldown = SWAP_COOLDOWN;
                return;
            }
            // Not in the inventory: fall through to the next priority.
        }
    }

    private boolean wants(Want want, LocalPlayer player, ItemStack offhand, float health, int hunger) {
        return switch (want) {
            case TOTEM -> totemHealth.get() > 0 && (health <= totemHealth.get()
                    || offhand.is(Items.TOTEM_OF_UNDYING) && health <= totemHealth.get() + TOTEM_HYSTERESIS);
            case FOOD -> foodHunger.get() > 0 && !MultiClicker.get().autoEat().isBusy()
                    && (hunger <= foodHunger.get() || Want.FOOD.matcher.test(offhand) && hunger < FULL_HUNGER);
            case TORCH -> torchWithPickaxe.get() && player.getMainHandItem().is(ItemTags.PICKAXES);
            case SHIELD -> shield.get();
        };
    }

    /**
     * Finds a matching stack, preferring the main inventory over the hotbar and never taking the
     * item the player is holding. Food picks the most nourishing stack.
     */
    private static int find(LocalPlayer player, Want want) {
        int selected = player.getInventory().getSelectedSlot();
        int best = -1;
        float bestScore = -1;
        for (int i = 0; i < Inventories.MAIN_SIZE; i++) {
            int slot = (i + Inventories.HOTBAR_SIZE) % Inventories.MAIN_SIZE; // 9..35, then 0..8
            if (slot == selected) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (!want.matcher.test(stack)) {
                continue;
            }
            if (want != Want.FOOD) {
                return slot;
            }
            float score = AutoEatModule.foodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    @Override
    public String hudInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || totemHealth.get() == 0) {
            return null;
        }
        int count = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) ? mc.player.getOffhandItem().getCount() : 0;
        for (int slot = 0; slot < Inventories.MAIN_SIZE; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        return Integer.toString(count);
    }
}
