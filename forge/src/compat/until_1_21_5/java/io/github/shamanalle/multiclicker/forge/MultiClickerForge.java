package io.github.shamanalle.multiclicker.forge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import io.github.shamanalle.multiclicker.MultiClicker;
import io.github.shamanalle.multiclicker.gui.ConfigScreen;

/** Forge entrypoint: wires the platform independent core into Forge events (Forge's first event bus). */
@Mod(MultiClicker.MOD_ID)
public class MultiClickerForge {
    public MultiClickerForge(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        ModLoadingContext loading = ModLoadingContext.get();
        MultiClicker mod = MultiClicker.init(FMLPaths.CONFIGDIR.get(), loading.getActiveContainer().getModInfo().getVersion().toString());

        IEventBus modBus = context.getModEventBus();
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(mod.toggleKey);
            event.register(mod.menuKey);
        });
        HudHook.register(modBus, mod);
        loading.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));

        // Pre runs before vanilla handles key presses, so simulated clicks are processed the same tick.
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Pre event) -> mod.onClientTick(Minecraft.getInstance()));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> Minecraft.getInstance().execute(mod::onDisconnect));
        MinecraftForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> mod.onClientStopping(Minecraft.getInstance()));
        WorldHook.register();
    }
}
