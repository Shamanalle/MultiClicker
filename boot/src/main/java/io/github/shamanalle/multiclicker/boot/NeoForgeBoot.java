package io.github.shamanalle.multiclicker.boot;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint: starts the variant for the running Minecraft version, which takes the mod container and side too. */
@Mod("multiclicker")
public final class NeoForgeBoot {
    public NeoForgeBoot(IEventBus modBus) {
        if (!Variants.client()) {
            return;
        }
        Object loading = Variants.call("net.neoforged.fml.ModLoadingContext", "get");
        Object container = null;
        try {
            container = loading.getClass().getMethod("getActiveContainer").invoke(loading);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Variants.LOGGER.error("MultiClicker cannot find its mod container", e);
        }
        Object dist = Variants.call("net.neoforged.fml.loading.FMLEnvironment", "getDist");
        if (dist == null) {
            try {
                dist = Class.forName("net.neoforged.fml.loading.FMLEnvironment").getField("dist").get(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Variants.LOGGER.error("MultiClicker cannot find the game side", e);
            }
        }
        Variants.create("entry", modBus, container, dist);
    }
}
