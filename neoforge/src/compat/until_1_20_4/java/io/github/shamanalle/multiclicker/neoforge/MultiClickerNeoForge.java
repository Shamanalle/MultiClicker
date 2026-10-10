package io.github.shamanalle.multiclicker.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.TickEvent;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.compat.Canvas;
import io.github.shamanalle.multiclicker.compat.Ids;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.gui.ConfigScreen;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** NeoForge entrypoint: wires the platform independent core into NeoForge events. */
@Mod(MultiClicker.MOD_ID)
public class MultiClickerNeoForge {
    public MultiClickerNeoForge(IEventBus modBus) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        ModLoadingContext context = ModLoadingContext.get();
        MultiClicker mod = MultiClicker.init(FMLPaths.CONFIGDIR.get(), context.getActiveContainer().getModInfo().getVersion().toString());

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(mod.toggleKey);
            event.register(mod.menuKey);
        });
        modBus.addListener((RegisterGuiOverlaysEvent event) ->
                event.registerAboveAll(Ids.mod("hud"), (gui, graphics, partialTick, width, height) -> mod.hud().render(new Canvas(graphics))));
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));

        // The start phase runs before vanilla handles key presses, so simulated clicks are processed the same tick.
        NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                mod.onClientTick(Minecraft.getInstance());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> Minecraft.getInstance().execute(mod::onDisconnect));
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> mod.onClientStopping(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                Minecraft mc = Minecraft.getInstance();
                HighlightRenderer.render(new Shapes.Sink(event.getPoseStack(), mc.renderBuffers().bufferSource()),
                        Shapes.cameraPosition(event.getCamera()), event.getPartialTick());
            }
        });
    }
}
