package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.module.automation.AutoFarmModule;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    /** The block being broken, noted before it turns into air. */
    @Unique
    @Nullable
    private BlockState multiclicker$breaking;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void multiclicker$noteBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        multiclicker$breaking = mc.level == null ? null : mc.level.getBlockState(pos);
    }

    /** Counts the blocks broken while the mod is on; ripe crops count as harvested instead. */
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void multiclicker$countBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = multiclicker$breaking;
        multiclicker$breaking = null;
        MultiClicker mod = MultiClicker.get();
        if (mod != null && state != null && cir.getReturnValueZ() && !AutoFarmModule.isRipe(state)) {
            mod.stats().onBlockBroken(state.getBlock());
        }
    }
}
