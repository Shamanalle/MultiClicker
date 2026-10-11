package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    /** Ticks vanilla ignores attack clicks for after a click into the air (or while a screen is open). */
    @Accessor("missTime")
    int multiclicker$getMissTime();
}
