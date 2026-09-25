package pro.mikey.autoclicker;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum Language {
    HUD_HOLDING("autoclicker-fabric.hud.holding"),
    MSG_HOLDING_KEYS("autoclicker-fabric.msg.holding-keys"),
    MSG_RELEASED_KEYS("autoclicker-fabric.msg.released-keys"),
    GUI_SPEED("autoclicker-fabric.gui.speed"),
    GUI_ACTIVE("autoclicker-fabric.gui.active"),
    GUI_SPAMMING("autoclicker-fabric.gui.spamming"),
    GUI_ATTACK("autoclicker-fabric.gui.attack"),
    GUI_USE("autoclicker-fabric.gui.use"),
    GUI_JUMP("autoclicker-fabric.gui.jump"),
    GUI_RESPECT_COOLDOWN("autoclicker-fabric.gui.respect"),
    GUI_RESPECT_SHIELD("autoclicker-fabric.gui.shield"),
    GUI_MOB_MODE("autoclicker-fabric.gui.mob-mode"),
    GUI_HUD_ENABLED("autoclicker-fabric.gui.hud-enabled"),
    GUI_HUD_LOCATION("autoclicker-fabric.gui.hud-location"),
    GUI_RANDOMIZE("autoclicker-fabric.gui.randomize"),
    GUI_RANDOMIZE_RANGE("autoclicker-fabric.gui.randomize-range"),
    // Phase 1-2 features
    GUI_SKIP_CHANCE("autoclicker-fabric.gui.skip-chance"),
    GUI_HEALTH_THRESHOLD("autoclicker-fabric.gui.health-threshold"),
    GUI_MOVEMENT_MODE("autoclicker-fabric.gui.movement-mode"),
    GUI_MOVEMENT_DELAY("autoclicker-fabric.gui.movement-delay"),
    GUI_TARGET_DELAY("autoclicker-fabric.gui.target-delay"),
    GUI_ROTATION_PROTECTION("autoclicker-fabric.gui.rotation-protection"),
    GUI_ENTITY_PROTECTION("autoclicker-fabric.gui.entity-protection"),
    GUI_HELP_BUTTON("autoclicker-fabric.gui.help.button"),
    GUI_HOLD_DURATION("autoclicker-fabric.gui.hold-duration"),
    GUI_ENTITY_PROTECTION_TIMEOUT("autoclicker-fabric.gui.entity-protection-timeout"),
    // Phase 4 features
    HUD_STATS("autoclicker-fabric.hud.stats"),
    GUI_STATS_ENABLED("autoclicker-fabric.gui.stats-enabled"),
    GUI_PROFILE_INFO("autoclicker-fabric.gui.profile-info"),
    HUD_PROFILE_INFO("autoclicker-fabric.hud.profile-info"),
    // Phase 3 features
    GUI_GAUSSIAN_RANDOM("autoclicker-fabric.gui.gaussian-random"),
    GUI_RANDOM_SIGMA("autoclicker-fabric.gui.random-sigma"),
    GUI_ANTI_AFK("autoclicker-fabric.gui.anti-afk"),
    GUI_ANTI_AFK_INTERVAL("autoclicker-fabric.gui.anti-afk-interval"),
    GUI_ANTI_AFK_MODE("autoclicker-fabric.gui.anti-afk-mode"),
    GUI_SMART_TRIGGER("autoclicker-fabric.gui.smart-trigger"),
    GUI_SMART_TRIGGER_MOB_COUNT("autoclicker-fabric.gui.smart-trigger-count"),
    GUI_LOOTING_SWAPPER("autoclicker-fabric.gui.looting-swapper"),
    GUI_LOW_HP_ACTION("autoclicker-fabric.gui.low-hp-action"),
    GUI_SAVE_TOOL("autoclicker-fabric.gui.save-tool"),
    GUI_SAVE_TOOL_THRESHOLD("autoclicker-fabric.gui.save-tool-threshold"),
    GUI_AUTO_EAT("autoclicker-fabric.gui.auto-eat"),
    GUI_AUTO_EAT_THRESHOLD("autoclicker-fabric.gui.auto-eat-threshold"),
    GUI_TRASH_DROP("autoclicker-fabric.gui.trash-drop"),
    GUI_TRASH_ITEMS("autoclicker-fabric.gui.trash-items"),
    GUI_CAMERA_LOCK("autoclicker-fabric.gui.camera-lock"),
    GUI_AUTO_TOOL("autoclicker-fabric.gui.auto-tool"),
    GUI_AFK_HOLOGRAM("autoclicker-fabric.gui.afk-hologram");

    private final String key;
    MutableComponent text;

    Language(String langKey) {
        this.text = Component.translatable(langKey);
        this.key = langKey;
    }

    public MutableComponent getText() {
        return this.text;
    }

    public Component getText(Object... args) {
        return Component.translatable(this.key, args);
    }
}
