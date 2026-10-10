package io.github.shamanalle.multiclicker.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import io.github.shamanalle.multiclicker.compat.Shapes;
import io.github.shamanalle.multiclicker.render.HighlightRenderer;

/** Hooks the target highlight into world rendering. */
final class WorldHook {
    private WorldHook() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent event) ->
                HighlightRenderer.render(new Shapes.Sink(event.getPoseStack(), event.getSubmitNodeCollector()),
                        event.getLevelRenderState().cameraRenderState.pos, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
    }
}
