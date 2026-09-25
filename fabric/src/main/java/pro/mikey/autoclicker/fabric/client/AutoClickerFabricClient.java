package pro.mikey.autoclicker.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import pro.mikey.autoclicker.AutoClicker;
import pro.mikey.autoclicker.modules.combat.CombatClickerModule;
import pro.mikey.autoclicker.modules.world.HudModule;
import pro.mikey.autoclicker.ui.NotificationRenderer;

/**
 * Fabric client entrypoint for Minecraft 26.1.
 */
public class AutoClickerFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(AutoClicker.openConfig);
        KeyMappingHelper.registerKeyMapping(AutoClicker.toggleHolding);

        AutoClicker instance = new AutoClicker();
        instance.onInitialize();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> instance.clientReady(client));
        });

        // HUD overlay
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("multiclicker", "hud"),
            (extractor, delta) -> {
                instance.getModuleManager().<HudModule>get("hud")
                        .ifPresent(hud -> hud.render(extractor, delta));
                // #16 Notifications
                NotificationRenderer.getInstance().render(extractor);
            }
        );

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
