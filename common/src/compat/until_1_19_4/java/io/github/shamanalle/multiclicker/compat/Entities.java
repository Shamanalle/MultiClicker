package io.github.shamanalle.multiclicker.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;

/** Entity state whose accessors were renamed between Minecraft versions. */
public final class Entities {
    private Entities() {
    }

    public static boolean onGround(Entity entity) {
        return entity.isOnGround();
    }

    /** The creature an entity is part of: the ender dragon for one of its body parts. */
    public static Entity whole(Entity entity) {
        return entity instanceof EnderDragonPart part ? part.parentMob : entity;
    }

    /** The id of the dimension the entity is in, such as {@code minecraft:the_nether}. */
    public static String dimension(Entity entity) {
        return entity.level.dimension().location().toString();
    }
}
