package pro.mikey.autoclicker.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    /** Synced from the server when a fish bites the hook. */
    @Accessor("biting")
    boolean multiclicker$isBiting();
}
