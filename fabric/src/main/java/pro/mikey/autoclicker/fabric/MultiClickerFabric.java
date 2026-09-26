package pro.mikey.autoclicker.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import pro.mikey.autoclicker.MultiClicker;
import pro.mikey.autoclicker.render.HighlightRenderer;

/** Fabric entrypoint: wires the platform independent core into Fabric API events. */
public class MultiClickerFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer(MultiClicker.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        MultiClicker mod = MultiClicker.init(FabricLoader.getInstance().getConfigDir(), version);

        KeyBindingHelper.registerKeyBinding(mod.toggleKey);
        KeyBindingHelper.registerKeyBinding(mod.menuKey);

        // START runs before vanilla handles key presses, so simulated clicks are processed the same tick.
        ClientTickEvents.START_CLIENT_TICK.register(mod::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(mod::onDisconnect));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mod::onClientStopping);

        HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath(MultiClicker.MOD_ID, "hud"),
                (graphics, deltaTracker) -> mod.hud().render(graphics, deltaTracker));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.matrixStack() != null && context.consumers() != null) {
                HighlightRenderer.render(context.matrixStack(), context.consumers(), context.camera(),
                        context.tickCounter().getGameTimeDeltaPartialTick(false));
            }
        });
    }
}
