package pro.mikey.autoclicker.modules.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;
import pro.mikey.autoclicker.util.InventoryUtils;

import java.util.List;

/**
 * Auto-Tool: selects the best tool for the block being mined.
 */
public class AutoToolModule implements Module {

    public final BooleanSetting enabled = new BooleanSetting("auto_tool.enabled", "Авто-Инструмент", false)
            .withDescription("Автоматически выбирает лучший инструмент для блока. Учитывает Efficiency");
    public final BooleanSetting saveTool = new BooleanSetting("auto_tool.save_tool", "Сохранять инструмент", false)
            .withDescription(
                    "Не выбирает инструмент с низкой прочностью. Защищает от поломки неритовых/алмазных инструментов");
    public final IntSetting saveThreshold = new IntSetting("auto_tool.save_threshold", "Порог прочности", 10, 1, 100)
            .withDescription("Минимальный остаток прочности. Ниже — инструмент не будет выбран");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, saveTool, saveThreshold);
    }

    private int originalSlot = -1;

    @Override
    public String getId() {
        return "auto_tool";
    }

    @Override
    public String getDisplayName() {
        return "Авто-Инструмент";
    }

    @Override
    public Category getCategory() {
        return Category.AUTOMATION;
    }

    @Override
    public int tickPriority() {
        return 60;
    }

    @Override
    public void onDisable() {
        if (originalSlot != -1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                InventoryUtils.setSelectedSlot(mc.player, originalSlot);
            }
            originalSlot = -1;
        }
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        // Select best tool when crosshair is on a block (no click needed)
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr
                && mc.level != null) {
            BlockState state = mc.level.getBlockState(bhr.getBlockPos());
            if (!state.isAir()) {
                int bestSlot = findBestTool(mc, state);
                if (bestSlot != -1) {
                    int currentSlot = InventoryUtils.getSelectedSlot(mc.player);
                    if (currentSlot != bestSlot) {
                        if (originalSlot == -1)
                            originalSlot = currentSlot;
                        InventoryUtils.setSelectedSlot(mc.player, bestSlot);
                    }
                }
                return false;
            }
        }
        // Looking at air/entity/nothing → revert to original slot
        if (originalSlot != -1) {
            InventoryUtils.setSelectedSlot(mc.player, originalSlot);
            originalSlot = -1;
        }
        return false;
    }

    private int findBestTool(Minecraft mc, BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty())
                continue;

            if (saveTool.get() && stack.isDamageableItem()) {
                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                if (remaining <= saveThreshold.get())
                    continue;
            }

            float speed = stack.getDestroySpeed(state);
            if (speed > 1.0f && mc.level != null) {
                try {
                    var enchantRegistry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var effHolder = enchantRegistry.getOrThrow(Enchantments.EFFICIENCY);
                    int eff = EnchantmentHelper.getItemEnchantmentLevel(effHolder, stack);
                    if (eff > 0)
                        speed += (eff * eff + 1);
                } catch (Exception ignored) {
                }
            }

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
}
