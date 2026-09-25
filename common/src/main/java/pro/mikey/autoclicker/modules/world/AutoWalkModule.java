package pro.mikey.autoclicker.modules.world;

import net.minecraft.client.Minecraft;
import pro.mikey.autoclicker.core.Module;
import pro.mikey.autoclicker.core.Setting;
import pro.mikey.autoclicker.core.Setting.*;

import java.util.List;

/**
 * Auto-Walk: holds the forward key, optional bunny-hop.
 */
public class AutoWalkModule implements Module {

    public final BooleanSetting enabled = new BooleanSetting("auto_walk.enabled", "Авто-Ходьба", false)
            .withDescription("Удерживает клавишу W. Персонаж идёт вперёд без участия игрока");
    public final BooleanSetting jump = new BooleanSetting("auto_walk.jump", "Прыжки", false)
            .withDescription("Bunny-hop: прыгает при касании земли. Ускоряет перемещение + расходует больше голода");

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, jump);
    }

    @Override
    public String getId() {
        return "auto_walk";
    }

    @Override
    public String getDisplayName() {
        return "Авто-Ходьба";
    }

    @Override
    public Category getCategory() {
        return Category.WORLD;
    }

    @Override
    public int tickPriority() {
        return 50;
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
        }
    }

    @Override
    public boolean onTick(Minecraft mc) {
        if (!enabled.get() || mc.player == null)
            return false;

        if (mc.screen != null) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            return false;
        }

        mc.options.keyUp.setDown(true);
        if (jump.get()) {
            mc.options.keyJump.setDown(mc.player.onGround());
        } else {
            mc.options.keyJump.setDown(false);
        }
        return false;
    }
}
