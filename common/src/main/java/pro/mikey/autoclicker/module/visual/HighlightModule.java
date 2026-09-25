package pro.mikey.autoclicker.module.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.setting.EnumSetting;

/** Highlights the entity under the crosshair when the clicker would attack it. */
public class HighlightModule extends Module {
    public enum Style {
        OUTLINE, FILLED, GLOW
    }

    public final EnumSetting<Style> style = add(new EnumSetting<>("style", Style.OUTLINE));
    public final EnumSetting<Palette> color = add(new EnumSetting<>("color", Palette.RED));
    public final BoolSetting onlyWhenActive = add(new BoolSetting("only_when_active", true));

    public HighlightModule() {
        super("highlight", Category.VISUAL, true, true);
    }

    /** The entity to highlight this frame, if any. Works regardless of the mod being active. */
    @Nullable
    public Entity target(Minecraft mc) {
        if (!isEnabled() || mc.player == null) {
            return null;
        }
        MultiClicker mod = MultiClicker.get();
        if (onlyWhenActive.get() && !mod.isActive()) {
            return null;
        }
        if (mc.hitResult instanceof EntityHitResult hit && mod.targetFilter().isValidTarget(mc, hit.getEntity())) {
            return hit.getEntity();
        }
        return null;
    }

    /** Called for every rendered entity, so it first does the cheap identity check. */
    public boolean shouldGlow(Entity entity) {
        if (style.get() != Style.GLOW || !isEnabled()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof EntityHitResult hit) || hit.getEntity() != entity) {
            return false;
        }
        return target(mc) == entity;
    }

    public int color() {
        return color.get().argb();
    }
}
