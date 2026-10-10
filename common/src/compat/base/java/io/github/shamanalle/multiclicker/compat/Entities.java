package io.github.shamanalle.multiclicker.compat;

import net.minecraft.world.entity.Entity;

/** Entity state whose accessors were renamed between Minecraft versions. */
public final class Entities {
    private Entities() {
    }

    public static boolean onGround(Entity entity) {
        return entity.onGround();
    }
}
