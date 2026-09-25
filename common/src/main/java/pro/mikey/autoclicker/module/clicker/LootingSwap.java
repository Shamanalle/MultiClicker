package pro.mikey.autoclicker.module.clicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import pro.mikey.autoclicker.util.Input;
import pro.mikey.autoclicker.util.Inventories;

/**
 * Deals the killing blow with a Looting weapon from the hotbar.
 *
 * <p>Switching items resets the attack charge, so the swap happens <i>before</i> the finishing
 * hit: when the next hit is predicted to kill the target, the Looting weapon is selected, the
 * mod waits until it is fully charged, hits once and switches back.</p>
 *
 * <p>The damage prediction mirrors vanilla melee damage at full charge without a critical hit:
 * attack damage attribute (including Strength / Weakness), Sharpness / Smite / Bane of Arthropods,
 * then the target's armor, toughness and Resistance. Health includes absorption hearts.</p>
 */
final class LootingSwap {
    enum Result {
        /** Not involved this tick; the normal attack channel runs. */
        IDLE,
        /** Busy (waiting for the charge or switching back); the normal attack channel stays quiet. */
        BUSY,
        /** Attacked the target this tick. */
        ATTACKED
    }

    private enum State {
        IDLE, CHARGING, RESTORE
    }

    /** Give up if the weapon is not charged or the target is not in the crosshair by then. */
    private static final int TIMEOUT = 40;
    /** After a swap that did not land a hit, skip this many attempts so slots do not flicker. */
    private static final int RETRY_DELAY = 20;

    private State state = State.IDLE;
    private int previousSlot = -1;
    private int lootingSlot = -1;
    private int targetId;
    private int timer;
    private boolean pressed;
    private int retryDelay;

    boolean isActive() {
        return state != State.IDLE;
    }

    /** Starts a swap if the next hit on {@code target} would kill it and a better Looting weapon exists. */
    boolean tryStart(Minecraft mc, LocalPlayer player, LivingEntity target) {
        if (retryDelay > 0) {
            retryDelay--;
            return false;
        }
        Registry<Enchantment> enchantments = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> looting = holder(enchantments, Enchantments.LOOTING);
        if (looting == null) {
            return false;
        }
        int selected = player.getInventory().getSelectedSlot();
        int bestLevel = EnchantmentHelper.getItemEnchantmentLevel(looting, player.getMainHandItem());
        int bestSlot = -1;
        float bestDamage = 0;
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (slot == selected || stack.isEmpty() || Inventories.isNearlyBroken(stack, 1)) {
                continue;
            }
            int level = EnchantmentHelper.getItemEnchantmentLevel(looting, stack);
            if (level <= 0 || level < bestLevel) {
                continue;
            }
            float damage = estimateDamage(player, stack, target, enchantments);
            if (level > bestLevel || damage > bestDamage) {
                bestSlot = slot;
                bestLevel = level;
                bestDamage = damage;
            }
        }
        if (bestSlot == -1 || target.getHealth() + target.getAbsorptionAmount() > bestDamage) {
            return false;
        }
        previousSlot = selected;
        lootingSlot = bestSlot;
        targetId = target.getId();
        timer = 0;
        Inventories.selectSlot(player, bestSlot);
        state = State.CHARGING;
        return true;
    }

    Result tick(Minecraft mc, LocalPlayer player, boolean suppressed) {
        switch (state) {
            case CHARGING -> {
                // The player scrolled to another slot: respect that and stop without switching back.
                if (player.getInventory().getSelectedSlot() != lootingSlot) {
                    reset();
                    return Result.IDLE;
                }
                Entity target = mc.level.getEntity(targetId);
                boolean targetGone = !(target instanceof LivingEntity living) || !living.isAlive() || living.isDeadOrDying();
                if (targetGone || suppressed || ++timer > TIMEOUT) {
                    retryDelay = targetGone ? 0 : RETRY_DELAY;
                    state = State.RESTORE;
                    return Result.BUSY;
                }
                boolean inCrosshair = mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == target;
                if (inCrosshair && player.getAttackStrengthScale(0.5F) >= 1.0F) {
                    Input.click(mc.options.keyAttack);
                    pressed = true;
                    state = State.RESTORE;
                    return Result.ATTACKED;
                }
                return Result.BUSY;
            }
            case RESTORE -> {
                restore(mc, player);
                return Result.BUSY;
            }
            default -> {
                return Result.IDLE;
            }
        }
    }

    /** Switches back to the previous slot (only if the Looting weapon is still selected). */
    void restore(Minecraft mc, LocalPlayer player) {
        if (pressed) {
            Input.release(mc.options.keyAttack);
        }
        if (player != null && previousSlot != -1 && player.getInventory().getSelectedSlot() == lootingSlot) {
            Inventories.selectSlot(player, previousSlot);
        }
        reset();
    }

    private void reset() {
        state = State.IDLE;
        previousSlot = -1;
        lootingSlot = -1;
        pressed = false;
        timer = 0;
    }

    // --- Damage prediction ----------------------------------------------------------------------

    static float estimateDamage(LocalPlayer player, ItemStack weapon, LivingEntity target,
                                Registry<Enchantment> enchantments) {
        // The attribute already includes the held item and effects; swap the held item's part for the weapon's.
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                - addedAttackDamage(player.getMainHandItem()) + addedAttackDamage(weapon);
        float damage = (float) Math.max(0, base) + enchantmentBonus(weapon, target, enchantments);

        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float toughnessFactor = 2.0F + toughness / 4.0F;
        float effectiveArmor = Mth.clamp(armor - damage / toughnessFactor, armor * 0.2F, 20.0F);
        damage *= 1.0F - effectiveArmor / 25.0F;

        MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) {
            damage *= Math.max(0.0F, 1.0F - (resistance.getAmplifier() + 1) * 0.2F);
        }
        return damage;
    }

    private static double addedAttackDamage(ItemStack stack) {
        double[] total = {0};
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                    if (attribute.value() == Attributes.ATTACK_DAMAGE.value()
                            && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                        total[0] += modifier.amount();
                    }
                });
        return total[0];
    }

    private static float enchantmentBonus(ItemStack weapon, LivingEntity target, Registry<Enchantment> enchantments) {
        float bonus = 0;
        int sharpness = level(enchantments, Enchantments.SHARPNESS, weapon);
        if (sharpness > 0) {
            bonus += 0.5F * sharpness + 0.5F;
        }
        if (target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
            bonus += 2.5F * level(enchantments, Enchantments.SMITE, weapon);
        }
        if (target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            bonus += 2.5F * level(enchantments, Enchantments.BANE_OF_ARTHROPODS, weapon);
        }
        return bonus;
    }

    private static int level(Registry<Enchantment> enchantments, ResourceKey<Enchantment> key, ItemStack stack) {
        Holder<Enchantment> holder = holder(enchantments, key);
        return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    private static Holder<Enchantment> holder(Registry<Enchantment> enchantments, ResourceKey<Enchantment> key) {
        return enchantments.get(key).orElse(null);
    }
}
