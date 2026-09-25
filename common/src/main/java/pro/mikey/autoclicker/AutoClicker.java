package pro.mikey.autoclicker;

import com.google.common.base.Suppliers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import pro.mikey.autoclicker.core.ConfigManager;
import pro.mikey.autoclicker.core.ModuleManager;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import pro.mikey.autoclicker.modules.automation.*;
import pro.mikey.autoclicker.modules.combat.*;
import pro.mikey.autoclicker.modules.world.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * AutoClicker — the mod entrypoint.
 * All business logic lives in modules. This class only handles:
 * - Singleton lifecycle
 * - Keybinding registration
 * - ModuleManager init/tick dispatch
 * - Config save/load via ConfigManager
 */
public class AutoClicker {
    public static final String MOD_ID = "multiclicker";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final KeyMapping openConfig = new KeyMapping("keybinding.open-gui", GLFW.GLFW_KEY_O,
            KeyMapping.Category.MISC);
    public static final KeyMapping toggleHolding = new KeyMapping("keybinding.toggle-hold", GLFW.GLFW_KEY_I,
            KeyMapping.Category.MISC);

    private static AutoClicker INSTANCE;

    private final ModuleManager moduleManager = new ModuleManager();
    private final CombatStats combatStats = new CombatStats();
    private ConfigManager configManager;
    private boolean isActive = false;
    private long startTime = 0;
    private boolean initialized = false;

    // ── Singleton ─────────────────────────────────────────────────

    public void onInitialize() {
        INSTANCE = this;
    }

    public static AutoClicker getInstance() {
        return INSTANCE;
    }

    // ── Getters ───────────────────────────────────────────────────

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CombatStats getCombatStats() {
        return combatStats;
    }

    public boolean isActiveState() {
        return isActive;
    }

    public long getStartTime() {
        return startTime;
    }

    // ── Theme ─────────────────────────────────────────────────────
    private int accentColor = 0xFF5599FF; // default blue

    public int getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
    }

    // ── Init ──────────────────────────────────────────────────────

    public void clientReady(Minecraft client) {
        if (!initialized) {
            // First join: register all modules and init
            registerModules();
            moduleManager.init(client);
            configManager = new ConfigManager(moduleManager);
            Path settingsPath = Paths.get(client.gameDirectory.getPath() + "/config/multiclicker-settings.json");
            configManager.setConfigPath(settingsPath);
            initialized = true;
        }
        // Always reload settings (handles server-switch scenarios)
        if (configManager != null) {
            configManager.load();
        }
    }

    private void registerModules() {
        // Clicker — Mining mode switch first, then companion modules, then clicker
        // engine
        moduleManager.register(new MiningModule());
        moduleManager.register(new SmartCombatModule());
        moduleManager.register(new AntiCheatModule());
        moduleManager.register(new NetworkGuardModule());
        moduleManager.register(new CombatClickerModule());
        moduleManager.register(new MobFilterModule());
        moduleManager.register(new CombatFeedbackModule());

        // Protection
        moduleManager.register(new PanicModeModule());

        // Automation
        moduleManager.register(new AntiAfkModule());
        moduleManager.register(new AutoTotemModule());
        moduleManager.register(new SmartOffhandModule());
        moduleManager.register(new AutoEatModule());
        moduleManager.register(new AutoToolModule());
        moduleManager.register(new TrashDropModule());

        // World
        moduleManager.register(new AutoFishModule());
        moduleManager.register(new AutoWalkModule());
        moduleManager.register(new MiningFilterModule());

        // Render
        moduleManager.register(new HudModule());
    }

    // ── Tick ──────────────────────────────────────────────────────

    public void clientTickEvent(Minecraft mc) {
        if (mc.player == null || mc.level == null)
            return;

        // Keybinds always processed first
        keyInputEvent(mc);

        if (!mc.player.isAlive()) {
            isActive = false;
        }

        if (isActive) {
            // All modules dispatch in priority order
            moduleManager.dispatchTick(mc);
        }
    }

    private void keyInputEvent(Minecraft mc) {
        while (toggleHolding.consumeClick()) {
            isActive = !isActive;
            if (isActive) {
                startTime = System.currentTimeMillis();
            }
            combatStats.reset();

            mc.player.displayClientMessage(
                    (isActive ? Language.MSG_HOLDING_KEYS : Language.MSG_RELEASED_KEYS)
                            .getText().withStyle(isActive ? ChatFormatting.GREEN : ChatFormatting.RED),
                    true);

            moduleManager.<CombatClickerModule>get("combat_clicker").ifPresent(c -> {
                // #4 Toggle sound
                if (c.toggleSound.get()) {
                    mc.player.playSound(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                            0.3f, isActive ? 1.2f : 0.8f);
                }
                if (isActive) {
                    c.resetTimeouts();
                } else {
                    c.releaseAllKeys(mc);
                }
            });

            if (isActive) {
                moduleManager.enable();
            } else {
                moduleManager.disable();
                stopAutoWalk(mc);
            }
        }

        while (openConfig.consumeClick()) {
            mc.setScreen(new OptionsScreen());
        }
    }

    /**
     * Centralized safe disconnect: disables all modules, releases keys, then
     * disconnects.
     * Always use this instead of raw disconnect() to prevent NPE and stuck keys.
     */
    public void safeDisconnect(Minecraft mc, String reason) {
        if (mc == null)
            return;
        isActive = false;
        moduleManager.disable();
        moduleManager.<CombatClickerModule>get("combat_clicker")
                .ifPresent(c -> c.releaseAllKeys(mc));
        stopAutoWalk(mc);
        // #E Save config before disconnecting to prevent data loss
        saveConfig();
        if (mc.getConnection() != null) {
            mc.getConnection().getConnection()
                    .disconnect(Component.literal("§c[MultiClicker] " + reason));
        }
        LOGGER.info("Safe disconnect: {}", reason);
    }

    // ── Save/Load ─────────────────────────────────────────────────

    public void saveConfig() {
        if (configManager != null) {
            configManager.save();
        }
    }

    public void saveConfigTo(java.io.File file) {
        if (configManager != null) {
            configManager.saveTo(file.toPath());
        }
    }

    public void loadConfigFrom(java.io.File file) {
        if (configManager != null) {
            configManager.loadFrom(file.toPath());
            // Refresh screen if open
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof OptionsScreen) {
                mc.setScreen(new OptionsScreen());
            }
        }
    }

    public void stopAutoWalk(Minecraft mc) {
        if (mc == null)
            return;
        mc.options.keyUp.setDown(false);
        mc.options.keyJump.setDown(false);
    }
}
