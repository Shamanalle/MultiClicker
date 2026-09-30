package io.github.shamanalle.multiclicker.module.mining;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Lookups;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Inventories;


/** Switches to the fastest hotbar tool for the block being mined. */
public class AutoToolModule extends Module {
    private static final int SWITCH_BACK_DELAY = 10;

    public final BoolSetting switchBack = add(new BoolSetting("switch_back", true));
    public final IntSetting protectTool = add(new IntSetting("protect_tool", 10, 0, 100, Unit.NONE)
            .zeroMeans("options.off"));

    private int previousSlot = -1;
    private int toolSlot = -1;
    private int idleTicks;

    public AutoToolModule() {
        super("auto_tool", Category.MINING, true, false);
    }

    /** True while a tool picked by this module is selected (the player is mining). */
    public boolean isToolSelected() {
        return isRunning() && toolSlot != -1;
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        MultiClicker mod = MultiClicker.get();
        if (mod.autoEat().isBusy() || mod.clicker().isSwappingWeapon()) {
            // Another module has the hotbar right now; it switches back to our tool when done.
            idleTicks = 0;
            return;
        }
        int current = Slots.selected(player.getInventory());
        if (toolSlot != -1 && current != toolSlot) {
            // The player picked another slot: that choice wins, do not switch back later.
            previousSlot = -1;
            toolSlot = -1;
        }
        boolean mining = mc.screen == null && mc.options.keyAttack.isDown()
                && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK;
        if (!mining) {
            if (toolSlot != -1 && ++idleTicks >= SWITCH_BACK_DELAY) {
                restore(player);
            }
            return;
        }
        idleTicks = 0;
        BlockState state = mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos());
        if (state.isAir()) {
            return;
        }
        int best = findBestTool(mc, state);
        if (best != -1 && best != current) {
            if (toolSlot == -1) {
                previousSlot = switchBack.get() ? current : -1;
            }
            toolSlot = best;
            Inventories.selectSlot(player, best);
        }
    }

    @Override
    public void stop(Minecraft mc) {
        if (mc.player != null) {
            restore(mc.player);
        }
        previousSlot = -1;
        toolSlot = -1;
    }

    private void restore(LocalPlayer player) {
        if (previousSlot != -1 && Slots.selected(player.getInventory()) == toolSlot) {
            Inventories.selectSlot(player, previousSlot);
        }
        previousSlot = -1;
        toolSlot = -1;
        idleTicks = 0;
    }

    private int findBestTool(Minecraft mc, BlockState state) {
        Holder<Enchantment> efficiency = Lookups.enchantment(Lookups.enchantments(mc), Enchantments.EFFICIENCY);
        int current = Slots.selected(mc.player.getInventory());
        int bestSlot = -1;
        float bestScore = score(mc.player.getInventory().getItem(current), state, efficiency);
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty() || Inventories.isNearlyBroken(stack, protectTool.get())) {
                continue;
            }
            float score = score(stack, state, efficiency);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    /** Mining speed, with a bonus for tools that actually make the block drop. */
    private static float score(ItemStack stack, BlockState state, Holder<Enchantment> efficiency) {
        float speed = stack.getDestroySpeed(state);
        if (speed > 1.0F && efficiency != null) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(efficiency, stack);
            if (level > 0) {
                speed += level * level + 1;
            }
        }
        if (state.requiresCorrectToolForDrops() && stack.isCorrectToolForDrops(state)) {
            speed += 1000.0F;
        }
        return speed;
    }
}
