package pro.mikey.autoclicker.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.world.entity.Entity;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import pro.mikey.autoclicker.modules.world.HudModule;
import pro.mikey.autoclicker.ui.NotificationRenderer;

/**
 * Fabric client entrypoint for Minecraft 1.21.9.
 * Note: 1.21.9 Fabric API omitted WorldRenderEvents during Mojang's RenderPipeline rewrite.
 * GLOW ESP is handled via MixinEntity; target tracking is registered on ClientTickEvents.
 */
public class AutoClickerFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(AutoClicker.openConfig);
        KeyBindingHelper.registerKeyBinding(AutoClicker.toggleHolding);

        AutoClicker instance = new AutoClicker();
        instance.onInitialize();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> instance.clientReady(client));
        });

        // HUD overlay
        HudRenderCallback.EVENT.register((context, delta) -> {
            instance.getModuleManager().<HudModule>get("hud")
                    .ifPresent(hud -> hud.render(context, delta));
            // #16 Notifications
            NotificationRenderer.getInstance().render(context);
        });

        // ESP target tracking via ClientTickEvents (GLOW ESP is handled via MixinEntity)
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            HudModule hud = instance.getModuleManager()
                    .<HudModule>get("hud").orElse(null);
            if (hud == null)
                return;

            HudModule.EspMode espMode = hud.espMode.get();
            if (espMode == HudModule.EspMode.OFF)
                return;

            if (mc.player == null || mc.level == null)
                return;

            CombatClickerModule clicker = instance.getModuleManager()
                    .<CombatClickerModule>get("combat_clicker").orElse(null);

            Entity target = null;
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                Entity e = ehr.getEntity();
                if (e != null && e.isAlive())
                    target = e;
            }
            if (target == null && clicker != null)
                target = clicker.getLastTarget();

            if (clicker != null) {
                clicker.setEspTarget((target != null && target.isAlive()) ? target : null);
            }
        });
    }
}
