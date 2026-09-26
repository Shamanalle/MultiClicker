package pro.mikey.autoclicker.module.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import pro.mikey.autoclicker.module.Category;
import pro.mikey.autoclicker.module.Module;
import pro.mikey.autoclicker.setting.BoolSetting;
import pro.mikey.autoclicker.util.Input;

/** Holds the forward key, optionally sprinting and jumping over obstacles. */
public class AutoWalkModule extends Module {
    public final BoolSetting sprint = add(new BoolSetting("sprint", false));
    public final BoolSetting jumpObstacles = add(new BoolSetting("jump_obstacles", true));

    private boolean walking;
    private boolean jumping;

    public AutoWalkModule() {
        super("auto_walk", Category.AUTOMATION, true, false);
    }

    @Override
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (mc.screen != null) {
            stop(mc);
            return;
        }
        walking = true;
        Input.hold(mc.options.keyUp);
        if (sprint.get()) {
            Input.hold(mc.options.keySprint);
        }
        boolean shouldJump = jumpObstacles.get() && player.horizontalCollision && player.onGround();
        if (shouldJump) {
            Input.hold(mc.options.keyJump);
            jumping = true;
        } else if (jumping) {
            Input.release(mc.options.keyJump);
            jumping = false;
        }
    }

    @Override
    public void stop(Minecraft mc) {
        if (walking) {
            Input.release(mc.options.keyUp);
            Input.release(mc.options.keySprint);
            walking = false;
        }
        if (jumping) {
            Input.release(mc.options.keyJump);
            jumping = false;
        }
    }
}
