package io.github.shamanalle.multiclicker.module.mining;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import io.github.shamanalle.multiclicker.module.Category;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.EnumSetting;
import io.github.shamanalle.multiclicker.setting.IntSetting;
import io.github.shamanalle.multiclicker.setting.ListSetting;
import io.github.shamanalle.multiclicker.setting.Unit;
import io.github.shamanalle.multiclicker.util.Inventories;

import java.util.List;

/** Rules that decide whether the attack clicker may break the block under the crosshair. */
public class MiningModule extends Module {
    public enum FilterMode {
        OFF, WHITELIST, BLACKLIST
    }

    public final EnumSetting<FilterMode> filterMode = add(new EnumSetting<>("filter_mode", FilterMode.OFF));
    public final ListSetting blocks = add(new ListSetting("blocks", ListSetting.Kind.BLOCK, List.of())
            .visibleWhen(() -> filterMode.get() != FilterMode.OFF));
    public final BoolSetting stopWhenFull = add(new BoolSetting("stop_when_full", false));
    public final IntSetting protectTool = add(new IntSetting("protect_tool", 10, 0, 100, Unit.NONE)
            .zeroMeans("options.off"));

    public MiningModule() {
        super("mining", Category.MINING, true, true);
    }

    public boolean allowsBreaking(Minecraft mc, BlockPos pos) {
        if (!isEnabled()) {
            return true;
        }
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        boolean listed = blocks.contains(state.getBlock());
        boolean filtered = switch (filterMode.get()) {
            case OFF -> false;
            case WHITELIST -> !listed;
            case BLACKLIST -> listed;
        };
        if (filtered) {
            return false;
        }
        if (stopWhenFull.get() && mc.player.getInventory().getFreeSlot() == -1) {
            return false;
        }
        return !Inventories.isNearlyBroken(mc.player.getMainHandItem(), protectTool.get());
    }
}
