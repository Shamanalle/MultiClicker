package io.github.shamanalle.multiclicker.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import io.github.shamanalle.multiclicker.MultiClicker;

/** Fabric entrypoint: wires the platform independent core into Fabric API events. */
public class MultiClickerFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer(MultiClicker.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        MultiClicker mod = MultiClicker.init(FabricLoader.getInstance().getConfigDir(), version);

        KeyHook.register(mod.toggleKey);
        KeyHook.register(mod.menuKey);

        // START runs before vanilla handles key presses, so simulated clicks are processed the same tick.
        ClientTickEvents.START_CLIENT_TICK.register(mod::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(mod::onDisconnect));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mod::onClientStopping);

        HudHook.register(mod);
        WorldHook.register();
    }
}
