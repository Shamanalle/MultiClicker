package pro.mikey.autoclicker.core;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal, type-safe internal event bus.
 *
 * <p>
 * Used by mixins to fire lightweight events (AttackEvent, FishBiteEvent,
 * EntityHealthEvent, etc.) that only specific modules need. Modules
 * subscribe in their {@link Module#onInit} method.
 *
 * <p>
 * This is <b>not</b> a replacement for {@link ModuleManager#onTick} —
 * regular per-tick logic goes through the module lifecycle. The EventBus
 * is for specialized, infrequent events.
 *
 * <p>
 * Thread safety: uses {@link CopyOnWriteArrayList} for handler lists —
 * safe for concurrent subscribe + emit on different threads (render vs tick).
 */
public final class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<EventHandler<?>>> handlers = new HashMap<>();

    public static EventBus get() {
        return INSTANCE;
    }

    // ── Subscription ──────────────────────────────────────────────────

    @FunctionalInterface
    public interface EventHandler<T> {
        void handle(T event);
    }

    /** Subscribe a handler for a specific event type. */
    public <T> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    /** Unsubscribe a handler. */
    public <T> void unsubscribe(Class<T> eventType, EventHandler<T> handler) {
        List<EventHandler<?>> list = handlers.get(eventType);
        if (list != null) {
            list.remove(handler);
        }
    }

    // ── Emission ──────────────────────────────────────────────────────

    /** Fire an event to all subscribed handlers. */
    @SuppressWarnings("unchecked")
    public <T> void emit(T event) {
        List<EventHandler<?>> list = handlers.get(event.getClass());
        if (list == null)
            return;
        for (EventHandler<?> h : list) {
            ((EventHandler<T>) h).handle(event);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    /** Clear all subscriptions (e.g., on world leave / mod reload). */
    public void clear() {
        handlers.clear();
    }

    // ── Event records ─────────────────────────────────────────────────
    // Lightweight event types used across the mod.

    /** Fired by MixinMultiPlayerGameMode when an attack packet is sent. */
    public record AttackEvent(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.Entity target) {
    }

    /** Fired by MixinLivingEntity when an entity loses HP. */
    public record EntityDamagedEvent(net.minecraft.world.entity.LivingEntity entity,
            float realDamage) {
    }

    /** Fired by MixinLivingEntity when an entity's HP drops to 0. */
    public record EntityDeathEvent(net.minecraft.world.entity.LivingEntity entity) {
    }

    /** Fired by MixinFishingHook when a fish bite is detected. */
    public record FishBiteEvent() {
    }

    /** Fired by MixinGameRenderer after pick() on every render frame. */
    public record FramePickEvent(net.minecraft.world.entity.Entity entity) {
    }

    /** Fired by MixinMouseHandler when camera rotation is attempted. */
    public record CameraRotateEvent() {
    }
}
