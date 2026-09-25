package pro.mikey.autoclicker;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.mikey.autoclicker.config.ConfigManager;
import pro.mikey.autoclicker.gui.ConfigScreen;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.module.automation.AntiAfkModule;
import pro.mikey.autoclicker.module.automation.AutoFishModule;
import pro.mikey.autoclicker.module.automation.AutoWalkModule;
import pro.mikey.autoclicker.module.automation.InventoryCleanerModule;
import pro.mikey.autoclicker.module.clicker.ClickerModule;
import pro.mikey.autoclicker.module.combat.TargetFilterModule;
import pro.mikey.autoclicker.module.mining.AutoToolModule;
import pro.mikey.autoclicker.module.mining.MiningModule;
import pro.mikey.autoclicker.module.survival.AutoEatModule;
import pro.mikey.autoclicker.module.survival.AutoTotemModule;
import pro.mikey.autoclicker.module.survival.SafetyModule;
import pro.mikey.autoclicker.module.visual.HighlightModule;
import pro.mikey.autoclicker.module.visual.HudModule;
import pro.mikey.autoclicker.module.visual.InterfaceModule;
import pro.mikey.autoclicker.setting.Setting;
import pro.mikey.autoclicker.util.ServerStats;
import pro.mikey.autoclicker.util.SessionStats;

import java.nio.file.Path;
import java.util.List;

/**
 * Platform independent core of the mod: owns the modules, the configuration and the global
 * on/off state. The loader entrypoint forwards client ticks and render callbacks here.
 */
public final class MultiClicker {
    public static final String MOD_ID = "multiclicker";
    public static final Logger LOGGER = LoggerFactory.getLogger("MultiClicker");
    private static final String KEY_CATEGORY = "key.categories.multiclicker";
    /** Settings are written to disk this many ticks after the last change. */
    private static final int SAVE_DELAY = 40;

    @Nullable
    private static MultiClicker instance;

    public final KeyMapping toggleKey = new KeyMapping("key.multiclicker.toggle", GLFW.GLFW_KEY_I, KEY_CATEGORY);
    public final KeyMapping menuKey = new KeyMapping("key.multiclicker.menu", GLFW.GLFW_KEY_O, KEY_CATEGORY);

    private final ClickerModule clicker = new ClickerModule();
    private final TargetFilterModule targetFilter = new TargetFilterModule();
    private final SafetyModule safety = new SafetyModule();
    private final AutoTotemModule autoTotem = new AutoTotemModule();
    private final AutoEatModule autoEat = new AutoEatModule();
    private final AutoFishModule autoFish = new AutoFishModule();
    private final AntiAfkModule antiAfk = new AntiAfkModule();
    private final AutoWalkModule autoWalk = new AutoWalkModule();
    private final InventoryCleanerModule inventoryCleaner = new InventoryCleanerModule();
    private final MiningModule mining = new MiningModule();
    private final AutoToolModule autoTool = new AutoToolModule();
    private final HudModule hud = new HudModule();
    private final HighlightModule highlight = new HighlightModule();
    private final InterfaceModule ui = new InterfaceModule();

    /** Display order (grouped by category in the menu). */
    private final List<Module> modules = List.of(clicker, targetFilter, safety, autoTotem, autoEat, autoFish,
            antiAfk, autoWalk, inventoryCleaner, mining, autoTool, hud, highlight, ui);
    /**
     * Tick order. The clicker runs before the modules that borrow its keys (auto eat, auto fish),
     * so their input wins within the same tick.
     */
    private final List<Module> tickOrder = List.of(safety, clicker, autoTotem, autoEat, autoFish, autoTool,
            inventoryCleaner, antiAfk, autoWalk);

    private final ConfigManager config;
    private final SessionStats stats = new SessionStats();
    private final String version;
    private boolean active;
    private int saveCountdown = -1;

    private MultiClicker(Path configDir, String version) {
        this.version = version;
        this.config = new ConfigManager(configDir, modules);
        config.load();
        for (Module module : modules) {
            for (Setting<?> setting : module.settings()) {
                setting.onChange(() -> saveCountdown = SAVE_DELAY);
            }
        }
        saveCountdown = -1;
    }

    public static MultiClicker init(Path configDir, String version) {
        instance = new MultiClicker(configDir, version);
        LOGGER.info("MultiClicker {} initialized with {} modules", version, instance.modules.size());
        return instance;
    }

    /** The mod instance; {@code null} only before the client entrypoint has run. */
    public static MultiClicker get() {
        return instance;
    }

    // --- Accessors ------------------------------------------------------------------------------

    public List<Module> modules() {
        return modules;
    }

    public ClickerModule clicker() {
        return clicker;
    }

    public TargetFilterModule targetFilter() {
        return targetFilter;
    }

    public AutoEatModule autoEat() {
        return autoEat;
    }

    public AutoFishModule autoFish() {
        return autoFish;
    }

    public AntiAfkModule antiAfk() {
        return antiAfk;
    }

    public MiningModule mining() {
        return mining;
    }

    public AutoToolModule autoTool() {
        return autoTool;
    }

    public HudModule hud() {
        return hud;
    }

    public HighlightModule highlight() {
        return highlight;
    }

    public InterfaceModule ui() {
        return ui;
    }

    public ConfigManager config() {
        return config;
    }

    public SessionStats stats() {
        return stats;
    }

    public String version() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    // --- Lifecycle ------------------------------------------------------------------------------

    /** Called at the start of every client tick, before vanilla processes key presses. */
    public void onClientTick(Minecraft mc) {
        while (menuKey.consumeClick()) {
            mc.setScreen(new ConfigScreen(null));
        }
        while (toggleKey.consumeClick()) {
            setActive(!active, null);
        }
        if (active && (mc.player == null || mc.level == null)) {
            setActive(false, null);
        }
        if (active) {
            stats.tick(mc);
            for (Module module : tickOrder) {
                if (!active) {
                    break; // a module (safety, limits) switched the mod off
                }
                if (module.isEnabled()) {
                    tickModule(mc, module);
                }
            }
        }
        if (saveCountdown > 0 && --saveCountdown == 0) {
            saveConfig();
        }
    }

    private void tickModule(Minecraft mc, Module module) {
        try {
            module.tick(mc);
        } catch (RuntimeException e) {
            // Never leave keys stuck because of a bug: stop everything and report it.
            LOGGER.error("Module {} failed, deactivating MultiClicker", module.id(), e);
            setActive(false, Component.translatable("multiclicker.message.module_error", module.name()));
        }
    }

    /**
     * Turns the mod on or off.
     *
     * @param reason shown instead of the usual on/off message, e.g. why the mod stopped by itself
     */
    public void setActive(boolean value, @Nullable Component reason) {
        Minecraft mc = Minecraft.getInstance();
        if (value == active || value && mc.player == null) {
            return;
        }
        active = value;
        if (value) {
            stats.reset();
        }
        for (Module module : modules) {
            if (!module.isEnabled()) {
                continue;
            }
            try {
                if (value) {
                    module.start(mc);
                } else {
                    module.stop(mc);
                }
            } catch (RuntimeException e) {
                LOGGER.error("Failed to {} module {}", value ? "start" : "stop", module.id(), e);
            }
        }
        feedback(mc, value, reason);
    }

    private void feedback(Minecraft mc, boolean value, @Nullable Component reason) {
        if (mc.player == null) {
            return;
        }
        if (reason != null) {
            mc.player.displayClientMessage(Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                    .append(reason.copy().withStyle(ChatFormatting.YELLOW)), true);
        } else if (ui.toggleMessage.get()) {
            mc.player.displayClientMessage(Component.translatable(value ? "multiclicker.message.on" : "multiclicker.message.off")
                    .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        }
        if (ui.toggleSound.get() || reason != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, value ? 1.3F : 0.8F));
        }
    }

    public void onDisconnect() {
        if (active) {
            setActive(false, null);
        }
        ServerStats.reset();
    }

    /** Called when the game closes: never leave the player's vanilla options modified. */
    public void onClientStopping(Minecraft mc) {
        clicker.restorePauseOnLostFocus(mc);
        saveConfig();
    }

    public void saveConfig() {
        saveCountdown = -1;
        config.save();
    }
}
