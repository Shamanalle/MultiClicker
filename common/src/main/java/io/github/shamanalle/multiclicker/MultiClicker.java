package io.github.shamanalle.multiclicker;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.shamanalle.multiclicker.compat.Keys;
import io.github.shamanalle.multiclicker.compat.Messages;
import io.github.shamanalle.multiclicker.compat.Screens;
import io.github.shamanalle.multiclicker.config.ConfigManager;
import io.github.shamanalle.multiclicker.gui.ConfigScreen;
import io.github.shamanalle.multiclicker.module.Module;
import io.github.shamanalle.multiclicker.module.automation.AntiAfkModule;
import io.github.shamanalle.multiclicker.module.automation.AutoFarmModule;
import io.github.shamanalle.multiclicker.module.automation.AutoFishModule;
import io.github.shamanalle.multiclicker.module.automation.HotbarRefillModule;
import io.github.shamanalle.multiclicker.module.automation.AutoWalkModule;
import io.github.shamanalle.multiclicker.module.automation.InventoryCleanerModule;
import io.github.shamanalle.multiclicker.module.clicker.ClickChannel;
import io.github.shamanalle.multiclicker.module.clicker.ClickerModule;
import io.github.shamanalle.multiclicker.module.combat.TargetFilterModule;
import io.github.shamanalle.multiclicker.module.mining.AutoToolModule;
import io.github.shamanalle.multiclicker.module.mining.MiningModule;
import io.github.shamanalle.multiclicker.module.survival.AutoEatModule;
import io.github.shamanalle.multiclicker.module.survival.OffhandModule;
import io.github.shamanalle.multiclicker.module.survival.SafetyModule;
import io.github.shamanalle.multiclicker.module.visual.HighlightModule;
import io.github.shamanalle.multiclicker.module.visual.HudModule;
import io.github.shamanalle.multiclicker.module.visual.InterfaceModule;
import io.github.shamanalle.multiclicker.module.visual.NotificationsModule;
import io.github.shamanalle.multiclicker.setting.BoolSetting;
import io.github.shamanalle.multiclicker.setting.KeySetting;
import io.github.shamanalle.multiclicker.setting.Setting;
import io.github.shamanalle.multiclicker.util.Hotkeys;
import io.github.shamanalle.multiclicker.util.ServerStats;
import io.github.shamanalle.multiclicker.util.SessionStats;

import java.nio.file.Path;
import java.util.List;

/**
 * Platform independent core of the mod: owns the modules, the configuration and the global
 * on/off state. The loader entrypoint forwards client ticks and render callbacks here.
 */
public final class MultiClicker {
    public static final String MOD_ID = "multiclicker";
    public static final Logger LOGGER = LoggerFactory.getLogger("MultiClicker");
    /** Settings are written to disk this many ticks after the last change. */
    private static final int SAVE_DELAY = 40;

    @Nullable
    private static MultiClicker instance;

    public final KeyMapping toggleKey = Keys.keyMapping("key.multiclicker.toggle", InputConstants.KEY_I);
    public final KeyMapping menuKey = Keys.keyMapping("key.multiclicker.menu", InputConstants.KEY_O);

    private final ClickerModule clicker = new ClickerModule();
    private final TargetFilterModule targetFilter = new TargetFilterModule();
    private final SafetyModule safety = new SafetyModule();
    private final OffhandModule offhand = new OffhandModule();
    private final AutoEatModule autoEat = new AutoEatModule();
    private final AutoFishModule autoFish = new AutoFishModule();
    private final AntiAfkModule antiAfk = new AntiAfkModule();
    private final AutoWalkModule autoWalk = new AutoWalkModule();
    private final AutoFarmModule autoFarm = new AutoFarmModule();
    private final HotbarRefillModule hotbarRefill = new HotbarRefillModule();
    private final InventoryCleanerModule inventoryCleaner = new InventoryCleanerModule();
    private final MiningModule mining = new MiningModule();
    private final AutoToolModule autoTool = new AutoToolModule();
    private final HudModule hud = new HudModule();
    private final HighlightModule highlight = new HighlightModule();
    private final NotificationsModule notifications = new NotificationsModule();
    private final InterfaceModule ui = new InterfaceModule();

    /** Display order (grouped by category in the menu). */
    private final List<Module> modules = List.of(clicker, targetFilter, safety, offhand, autoEat, autoFarm,
            autoFish, hotbarRefill, antiAfk, autoWalk, inventoryCleaner, mining, autoTool, hud, highlight, notifications, ui);
    /**
     * Tick order. Safety first, so nothing else runs once it stops the mod. Auto eat decides before
     * the clicker, so the clicker already pauses on the tick a meal starts. Hotbar refill swaps a
     * worn tool before the clicker would refuse to mine with it. Auto tool runs after the clicker,
     * so the tool is selected before vanilla processes the click of the same tick. Notifications
     * run last and see what the other modules did.
     */
    private final List<Module> tickOrder = List.of(safety, autoEat, hotbarRefill, clicker, offhand, autoFish,
            autoFarm, autoTool, inventoryCleaner, antiAfk, autoWalk, notifications);

    private final ConfigManager config;
    private final SessionStats stats = new SessionStats();
    private final String version;
    private boolean active;
    private int saveCountdown = -1;
    /** The connection the server profile was last chosen for, to notice joining another world. */
    @Nullable
    private Object profileConnection;

    private MultiClicker(Path configDir, String version) {
        this.version = version;
        this.config = new ConfigManager(configDir, modules);
        config.load();
        for (Module module : modules) {
            for (Setting<?> setting : module.settings()) {
                setting.onChange(() -> {
                    saveCountdown = SAVE_DELAY;
                    // A setting the player changed by hand: the settings no longer match the profile.
                    if (!setting.isGlobal() && !config.isApplying()) {
                        config.clearActiveProfile();
                    }
                });
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

    public AutoWalkModule autoWalk() {
        return autoWalk;
    }

    public AutoFarmModule autoFarm() {
        return autoFarm;
    }

    public HotbarRefillModule hotbarRefill() {
        return hotbarRefill;
    }

    public OffhandModule offhand() {
        return offhand;
    }

    public SafetyModule safety() {
        return safety;
    }

    public InventoryCleanerModule inventoryCleaner() {
        return inventoryCleaner;
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

    public NotificationsModule notifications() {
        return notifications;
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
            Screens.open(mc, new ConfigScreen(null));
        }
        while (toggleKey.consumeClick()) {
            setActive(!active, null);
        }
        for (InputConstants.Key key : Hotkeys.drain(mc)) {
            onHotkey(mc, key);
        }
        if (mc.player != null && mc.getConnection() != profileConnection) {
            profileConnection = mc.getConnection();
            loadServerProfile(mc);
        }
        if (!ui.welcomeShown.get() && mc.player != null && Screens.current(mc) == null) {
            showWelcome(mc);
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

    /** First time in a world: tell the player which keys open the menu and start the mod. */
    private void showWelcome(Minecraft mc) {
        ui.welcomeShown.set(true);
        Messages.chat(mc.player, Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("multiclicker.message.welcome",
                        menuKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW),
                        toggleKey.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GRAY)));
    }

    // --- Hotkeys and profiles -------------------------------------------------------------------

    /** A key or mouse button was pressed while playing: toggle what is bound to it. */
    private void onHotkey(Minecraft mc, InputConstants.Key key) {
        if (mc.player == null) {
            return;
        }
        for (Module module : modules) {
            KeySetting moduleKey = module.keySetting();
            if (moduleKey != null && moduleKey.matches(key) && module.enabledSetting() != null) {
                toggleBound(mc, module.enabledSetting(), module.name());
            }
        }
        for (ClickChannel channel : clicker.channels()) {
            if (channel.key.matches(key)) {
                toggleBound(mc, channel.enabled, Component.translatable(channel.nameKey()));
            }
        }
        String profile = config.profileForKey(key.getName());
        if (profile != null) {
            loadProfile(mc, profile, "multiclicker.message.profile_loaded");
        }
    }

    private void toggleBound(Minecraft mc, BoolSetting setting, Component name) {
        setting.toggle();
        if (!ui.toggleMessage.get()) {
            return;
        }
        boolean on = setting.get();
        Component state = Component.translatable(on ? "multiclicker.message.module_on" : "multiclicker.message.module_off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component message = Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                .append(name.copy().withStyle(ChatFormatting.WHITE)).append(" ").append(state);
        if (on && !active) {
            // Without this the player would wonder why nothing happens.
            message = message.copy().append(Component.translatable("multiclicker.message.mod_off_hint",
                    toggleKey.getTranslatedKeyMessage()).withStyle(ChatFormatting.GRAY));
        }
        Messages.overlay(mc.player, message);
    }

    /** Joined a world: load the profile chosen for this server, if there is one. */
    private void loadServerProfile(Minecraft mc) {
        String server = ServerStats.address(mc);
        String profile = server == null ? null : config.profileForServer(server);
        if (profile != null && !profile.equals(config.activeProfile())) {
            loadProfile(mc, profile, "multiclicker.message.server_profile_loaded");
        }
    }

    /** Loads a profile and says so above the hotbar. */
    public boolean loadProfile(Minecraft mc, String name, String messageKey) {
        boolean[] loaded = new boolean[1];
        restartModules(mc, () -> loaded[0] = config.loadProfile(name));
        saveConfig();
        if (mc.player != null) {
            Messages.overlay(mc.player, Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(loaded[0] ? messageKey : "multiclicker.gui.profiles.error",
                            Component.literal(name).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.YELLOW)));
        }
        return loaded[0];
    }

    /**
     * Changes many settings at once (a profile or preset). While the mod is on, the modules are
     * stopped first and started again afterwards, so each one starts cleanly with its new settings
     * and none is left holding a key.
     */
    public void restartModules(Minecraft mc, Runnable change) {
        if (!active || mc.player == null) {
            change.run();
            return;
        }
        forEachEnabled(mc, false);
        active = false;
        try {
            change.run();
        } finally {
            active = true;
            forEachEnabled(mc, true);
        }
    }

    private void forEachEnabled(Minecraft mc, boolean start) {
        for (Module module : modules) {
            if (!module.isEnabled()) {
                continue;
            }
            try {
                if (start) {
                    module.start(mc);
                } else {
                    module.stop(mc);
                }
            } catch (RuntimeException e) {
                LOGGER.error("Failed to {} module {}", start ? "start" : "stop", module.id(), e);
            }
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
        forEachEnabled(mc, value);
        feedback(mc, value, reason);
    }

    private void feedback(Minecraft mc, boolean value, @Nullable Component reason) {
        if (mc.player == null) {
            return;
        }
        if (reason != null) {
            if (!notifications.stopped(mc, reason)) {
                Messages.overlay(mc.player, Component.literal("MultiClicker: ").withStyle(ChatFormatting.GOLD)
                        .append(reason.copy().withStyle(ChatFormatting.YELLOW)));
            }
        } else if (ui.toggleMessage.get()) {
            Messages.overlay(mc.player, Component.translatable(value ? "multiclicker.message.on" : "multiclicker.message.off")
                    .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (ui.toggleSound.get() || reason != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, value ? 1.3F : 0.8F));
        }
    }

    public void onDisconnect() {
        if (active) {
            setActive(false, null);
        }
        profileConnection = null;
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
