package io.github.shamanalle.multiclicker.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Ids;
import io.github.shamanalle.multiclicker.gui.ConfigScreen;

/** NeoForge entrypoint: wires the platform independent core into NeoForge events. NeoForge 20.5 has no {@code dist} in {@code @Mod}. */
@Mod(MultiClicker.MOD_ID)
public class MultiClickerNeoForge {
    public MultiClickerNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
        if (dist != Dist.CLIENT) {
            return;
        }
        MultiClicker mod = MultiClicker.init(FMLPaths.CONFIGDIR.get(), container.getModInfo().getVersion().toString());

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(mod.toggleKey);
            event.register(mod.menuKey);
        });
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAboveAll(Ids.mod("hud"), (graphics, deltaTracker) -> mod.hud().render(new Canvas(graphics))));
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new ConfigScreen(parent));

        // Pre runs before vanilla handles key presses, so simulated clicks are processed the same tick.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> mod.onClientTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> Minecraft.getInstance().execute(mod::onDisconnect));
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> mod.onClientStopping(Minecraft.getInstance()));
        WorldHook.register();
    }
}
