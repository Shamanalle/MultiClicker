package pro.mikey.autoclicker.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A feature of the mod. Modules only tick while the mod is <b>active</b> (toggled with the
 * activation key) and the module itself is enabled.
 *
 * <p>Lifecycle: {@link #start} runs when the module begins working, {@link #tick} every client
 * tick after that, and {@link #stop} when it stops working (mod deactivated, module disabled,
 * disconnect). {@code stop} must release every key and restore every slot the module touched.</p>
 */
public abstract class Module {
    private final String id;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    @Nullable
    private final BoolSetting enabled;

    protected Module(String id, Category category, boolean toggleable, boolean enabledByDefault) {
        this.id = id;
        this.category = category;
        this.enabled = toggleable ? add(new BoolSetting("enabled", enabledByDefault)) : null;
        if (enabled != null) {
            enabled.onChange(this::onEnabledChanged);
        }
    }

    protected final <S extends Setting<?>> S add(S setting) {
        setting.attach(this);
        settings.add(setting);
        return setting;
    }

    public String id() {
        return id;
    }

    public Category category() {
        return category;
    }

    public Component name() {
        return Component.translatable("multiclicker.module." + id);
    }

    public Component description() {
        return Component.translatable("multiclicker.module." + id + ".desc");
    }

    public List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    @Nullable
    public BoolSetting enabledSetting() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled == null || enabled.get();
    }

    /** Whether the module is currently doing its job (mod active and module enabled). */
    public boolean isRunning() {
        return isEnabled() && MultiClicker.get().isActive();
    }

    /** Short status for the HUD module list, or {@code null} to show only the name. */
    @Nullable
    public String hudInfo() {
        return null;
    }

    public void start(Minecraft mc) {
    }

    public void tick(Minecraft mc) {
    }

    public void stop(Minecraft mc) {
    }

    private void onEnabledChanged() {
        MultiClicker mod = MultiClicker.get();
        if (mod == null || !mod.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (isEnabled()) {
            start(mc);
        } else {
            stop(mc);
        }
    }
}
