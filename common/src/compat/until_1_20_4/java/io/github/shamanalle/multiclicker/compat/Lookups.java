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
import net.minecraft.world.effect.MobEffectInstance;
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

    /** Level (amplifier + 1) of the effect on the entity, 0 without it. */
    public static int strength(LivingEntity entity) {
        return level(entity, MobEffects.DAMAGE_BOOST);
    }

    public static int weakness(LivingEntity entity) {
        return level(entity, MobEffects.WEAKNESS);
    }

    public static int resistance(LivingEntity entity) {
        return level(entity, MobEffects.DAMAGE_RESISTANCE);
    }

    public static Registry<Enchantment> enchantments(Minecraft mc) {
        return mc.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    /** The enchantment holder, or {@code null} when the registry has no such entry. */
    public static Holder<Enchantment> enchantment(Registry<Enchantment> registry, ResourceKey<Enchantment> key) {
        return registry.getHolder(key).orElse(null);
    }

    private static int level(LivingEntity entity, MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }
}
