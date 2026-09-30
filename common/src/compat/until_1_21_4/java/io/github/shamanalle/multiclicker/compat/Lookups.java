package io.github.shamanalle.multiclicker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.enchantment.Enchantment;

/** Registry, effect and ownership lookups whose names changed between Minecraft versions. */
public final class Lookups {
    private Lookups() {
    }

    public static boolean isMobOfTag(LivingEntity entity, TagKey<EntityType<?>> tag) {
        return entity.getType().is(tag);
    }

    public static boolean hasOwner(OwnableEntity entity) {
        return entity.getOwnerUUID() != null;
    }

    public static Holder<MobEffect> strength() {
        return MobEffects.DAMAGE_BOOST;
    }

    public static Holder<MobEffect> weakness() {
        return MobEffects.WEAKNESS;
    }

    public static Holder<MobEffect> resistance() {
        return MobEffects.DAMAGE_RESISTANCE;
    }

    public static Registry<Enchantment> enchantments(Minecraft mc) {
        return mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    }

    /** The enchantment holder, or {@code null} when the registry has no such entry. */
    public static Holder<Enchantment> enchantment(Registry<Enchantment> registry, ResourceKey<Enchantment> key) {
        return registry.get(key).orElse(null);
    }
}
