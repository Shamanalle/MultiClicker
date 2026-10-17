package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    /** Synced from the server when a fish bites the hook. */
    @Accessor("biting")
    boolean multiclicker$isBiting();

    /** The server's check for open water (treasure loot) around the hook, run on the client's copy of the world. */
    @Invoker("calculateOpenWater")
    boolean multiclicker$calculateOpenWater(BlockPos pos);
}
