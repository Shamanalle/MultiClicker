package pro.mikey.autoclicker.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.mikey.autoclicker.MultiClicker;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Colors the glow outline of the highlighted target with the configured color. */
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void multiclicker$targetGlowColor(CallbackInfoReturnable<Integer> cir) {
        MultiClicker mod = MultiClicker.get();
        if (mod != null && mod.highlight().shouldGlow((Entity) (Object) this)) {
            cir.setReturnValue(mod.highlight().color() & 0xFFFFFF);
        }
    }
}
