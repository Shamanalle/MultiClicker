package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BooleanSupplier;

@Mixin(ToggleKeyMapping.class)
public interface ToggleKeyMappingAccessor {
    /** True while the player uses "toggle" instead of "hold" for this key (sprint / sneak options). */
    @Accessor("needsToggle")
    BooleanSupplier multiclicker$getNeedsToggle();
}
