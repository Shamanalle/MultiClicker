package io.github.shamanalle.multiclicker.module.clicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import io.github.shamanalle.multiclicker.compat.Enchants;
import io.github.shamanalle.multiclicker.compat.Lookups;
import io.github.shamanalle.multiclicker.compat.Slots;
import io.github.shamanalle.multiclicker.compat.Stacks;
import io.github.shamanalle.multiclicker.util.Input;
import io.github.shamanalle.multiclicker.util.Inventories;

/**
 * Deals the killing blow with a Looting weapon from the hotbar.
 *
 * <p>Switching items resets the attack charge, so the swap happens <i>before</i> the finishing
 * hit: when the next hit is predicted to kill the target, the Looting weapon is selected, the
 * mod waits until it is fully charged, hits once and switches back.</p>
 *
 * <p>The damage prediction mirrors vanilla melee damage at full charge without a critical hit:
 * base attack damage, the weapon's attack damage, Strength / Weakness, Sharpness / Smite / Bane of Arthropods,
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
        Enchants enchantments = Enchants.of(mc);
        if (!enchantments.hasLooting()) {
            return false;
        }
        int selected = Slots.selected(player.getInventory());
        int currentLevel = enchantments.looting(player.getMainHandItem());
        float health = target.getHealth() + target.getAbsorptionAmount();
        // Among the weapons with more Looting than the held item that kill with one hit: highest
        // Looting first, then the highest damage.
        int bestSlot = -1;
        int bestLevel = currentLevel;
        float bestDamage = 0;
        for (int slot = 0; slot < Inventories.HOTBAR_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (slot == selected || stack.isEmpty() || Inventories.isNearlyBroken(stack, 1)) {
                continue;
            }
            int level = enchantments.looting(stack);
            if (level <= currentLevel || level < bestLevel) {
                continue;
            }
            float damage = estimateDamage(player, stack, target, enchantments);
            if (damage >= health && (level > bestLevel || damage > bestDamage)) {
                bestSlot = slot;
                bestLevel = level;
                bestDamage = damage;
            }
        }
        if (bestSlot == -1) {
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
                if (Slots.selected(player.getInventory()) != lootingSlot) {
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
        if (player != null && previousSlot != -1 && Slots.selected(player.getInventory()) == lootingSlot) {
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
                                Enchants enchantments) {
        // Built from parts: the client never receives the attack damage attribute with the held
        // item's modifiers (only the server applies them), so the attribute value alone is wrong.
        double base = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + Stacks.attackDamage(weapon)
                + Lookups.strength(player) * 3.0 - Lookups.weakness(player) * 4.0;
        float damage = (float) Math.max(0, base) + enchantments.damageBonus(weapon, target);

        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float toughnessFactor = 2.0F + toughness / 4.0F;
        float effectiveArmor = Mth.clamp(armor - damage / toughnessFactor, armor * 0.2F, 20.0F);
        damage *= 1.0F - effectiveArmor / 25.0F;

        damage *= Math.max(0.0F, 1.0F - Lookups.resistance(target) * 0.2F);
        return damage;
    }
}
