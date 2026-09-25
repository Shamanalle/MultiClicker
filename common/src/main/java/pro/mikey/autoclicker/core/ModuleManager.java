package pro.mikey.autoclicker.core;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Central registry and lifecycle controller for all modules.
 *
 * <p>
 * Single entry point for tick and render events. Mixins call
 * {@link #onTick}, {@link #onHudRender}, and {@link #onWorldRender};
 * the manager dispatches to active modules in priority order.
 *
 * <p>
 * When a module returns {@code true} from {@link Module#onTick},
 * all subsequent lower-priority modules are skipped for that tick
 * (replaces the old scattered {@code return;} pattern).
 */
public final class ModuleManager {

    private static final Logger LOGGER = LogManager.getLogger("ModuleManager");

    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> byId = new LinkedHashMap<>();
    private boolean active = false;

    // ── Registration ──────────────────────────────────────────────────

    /** Register a module. Call during mod init, before {@link #init}. */
    public void register(Module module) {
        if (byId.containsKey(module.getId())) {
            throw new IllegalArgumentException("Duplicate module ID: " + module.getId());
        }
        modules.add(module);
        byId.put(module.getId(), module);
        LOGGER.debug("Registered module: {} ({})", module.getId(), module.getDisplayName());
    }

    /**
     * Sort by tickPriority and call {@link Module#onInit} on every module.
     * Must be called once after all modules are registered.
     */
    public void init(Minecraft mc) {
        modules.sort(Comparator.comparingInt(Module::tickPriority));
        for (Module m : modules) {
            try {
                m.onInit(mc);
            } catch (Exception e) {
                LOGGER.error("Failed to init module: {}", m.getId(), e);
            }
        }
        LOGGER.info("ModuleManager initialized with {} modules", modules.size());
    }

    // ── Global toggle ─────────────────────────────────────────────────

    /** Activate all modules (global toggle ON). */
    public void enable() {
        this.active = true;
        for (Module m : modules) {
            try {
                m.onEnable();
            } catch (Exception e) {
                LOGGER.error("Error enabling module: {}", m.getId(), e);
            }
        }
    }

    /** Deactivate all modules (global toggle OFF). */
    public void disable() {
        this.active = false;
        for (Module m : modules) {
            try {
                m.onDisable();
            } catch (Exception e) {
                LOGGER.error("Error disabling module: {}", m.getId(), e);
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (active)
            enable();
        else
            disable();
    }

    public void toggle() {
        setActive(!active);
    }

    /** Get a module by its class type. Returns null if not found. */
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (clazz.isInstance(m)) {
                return (T) m;
            }
        }
        return null;
    }

    /** Returns an unmodifiable view of all registered modules. */
    public List<Module> getModules() {
        return java.util.Collections.unmodifiableList(modules);
    }

    // ── Tick dispatch ─────────────────────────────────────────────────

    /**
     * Called from MixinMinecraft every game tick.
     * Dispatches {@link Module#onTick} to each module in priority order.
     * If a module returns {@code true}, lower-priority modules are skipped.
     */
    public void onTick(Minecraft mc) {
        dispatchTick(mc);
    }

    /**
     * Dispatches tick to all modules and returns whether any module consumed the
     * tick.
     * Used by AutoClicker.clientTickEvent to decide whether attack handling should
     * run.
     *
     * @return true if any module consumed the tick
     */
    public boolean dispatchTick(Minecraft mc) {
        for (Module m : modules) {
            try {
                boolean consumed = m.onTick(mc);
                if (consumed)
                    return true;
            } catch (Exception e) {
                LOGGER.error("Error in onTick for module: {}", m.getId(), e);
            }
        }
        return false;
    }

    // ── Render dispatch ───────────────────────────────────────────────

    /** Dispatch HUD overlay rendering (every render frame). */
    public void onHudRender(Minecraft mc, net.minecraft.client.gui.GuiGraphics graphics,
            net.minecraft.client.DeltaTracker delta) {
        for (Module m : modules) {
            try {
                m.onHudRender(mc, graphics, delta);
            } catch (Exception e) {
                LOGGER.error("Error in onHudRender for module: {}", m.getId(), e);
            }
        }
        // Render notification toasts on top of everything
        try {
            pro.mikey.autoclicker.ui.NotificationRenderer.getInstance().render(graphics);
        } catch (Exception e) {
            LOGGER.error("Error rendering notifications", e);
        }
    }

    /** Dispatch 3D world rendering (every render frame). */
    public void onWorldRender(Minecraft mc, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack) {
        for (Module m : modules) {
            try {
                m.onWorldRender(mc, partialTick, poseStack);
            } catch (Exception e) {
                LOGGER.error("Error in onWorldRender for module: {}", m.getId(), e);
            }
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────

    /** Find a module by its ID. */
    @SuppressWarnings("unchecked")
    public <T extends Module> Optional<T> get(String id) {
        return Optional.ofNullable((T) byId.get(id));
    }

    /** All modules in a given category (for UI tab construction). */
    public List<Module> getByCategory(Module.Category category) {
        List<Module> result = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                result.add(m);
            }
        }
        return result;
    }

    /** All registered modules (unmodifiable). */
    public List<Module> getAll() {
        return Collections.unmodifiableList(modules);
    }
}
