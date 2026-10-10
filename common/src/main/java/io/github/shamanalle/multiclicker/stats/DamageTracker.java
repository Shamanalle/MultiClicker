package io.github.shamanalle.multiclicker.stats;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Iterator;

/**
 * Measures the damage we deal as the health an entity really lost: its health is noted when it is
 * hit and compared once the server sends the new value. Overkill and the hits armor absorbed are
 * thus never counted, only what left the health bar.
 */
final class DamageTracker {
    private static final class Pending {
        final float before;
        int ticksLeft;
        /** Whether a hit was certain to land, so no health change means the server hides health. */
        boolean expectsChange;

        Pending(float before, int ticksLeft, boolean expectsChange) {
            this.before = before;
            this.ticksLeft = ticksLeft;
            this.expectsChange = expectsChange;
        }
    }

    private final Statistics stats;
    private final Int2ObjectMap<Pending> pending = new Int2ObjectOpenHashMap<>();

    DamageTracker(Statistics stats) {
        this.stats = stats;
    }

    void clear() {
        pending.clear();
    }

    /**
     * The entity was hit by us; its new health should arrive within {@code ticks}. Several hits
     * before it arrives add up, since the health noted is from before the first.
     */
    void expect(LivingEntity entity, int ticks, boolean expectsChange) {
        Pending entry = pending.get(entity.getId());
        if (entry == null) {
            pending.put(entity.getId(), new Pending(health(entity), ticks, expectsChange));
        } else {
            entry.ticksLeft = Math.max(entry.ticksLeft, ticks);
            entry.expectsChange |= expectsChange;
        }
    }

    /** Something else hurt the entity: what it lost so far was ours, the rest is not. */
    void settle(LivingEntity entity) {
        Pending entry = pending.remove(entity.getId());
        if (entry != null) {
            resolve(entry, health(entity));
        }
    }

    void tick(Level level) {
        Iterator<Int2ObjectMap.Entry<Pending>> iterator = pending.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<Pending> entry = iterator.next();
            Pending hit = entry.getValue();
            Entity entity = level.getEntity(entry.getIntKey());
            if (!(entity instanceof LivingEntity living)) {
                iterator.remove(); // gone before its health was seen
                continue;
            }
            float health = health(living);
            if (health != hit.before) {
                resolve(hit, health);
                iterator.remove();
            } else if (--hit.ticksLeft <= 0) {
                if (hit.expectsChange) {
                    stats.damageUnseen();
                }
                iterator.remove();
            }
        }
    }

    private void resolve(Pending hit, float health) {
        float lost = hit.before - health;
        if (lost > 0) {
            stats.damageDealt(lost);
        } else if (hit.expectsChange) {
            // Health went up right after our hit: regeneration at best, made-up values at worst.
            stats.damageUnseen();
        }
    }

    private static float health(LivingEntity entity) {
        return Math.max(0, entity.getHealth()) + entity.getAbsorptionAmount();
    }
}
