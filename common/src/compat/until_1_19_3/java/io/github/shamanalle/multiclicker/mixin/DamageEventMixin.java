package io.github.shamanalle.multiclicker.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;

/** Before 1.19.4 there is no damage event; the damage is measured after our attacks instead. */
@Mixin(ClientPacketListener.class)
public abstract class DamageEventMixin {
}
