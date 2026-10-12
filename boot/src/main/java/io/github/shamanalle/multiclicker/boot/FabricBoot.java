package io.github.shamanalle.multiclicker.boot;

import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint: starts the variant for the running Minecraft version. */
public final class FabricBoot implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Object entry = Variants.create("client");
        if (entry != null) {
            ((ClientModInitializer) entry).onInitializeClient();
        }
    }
}
