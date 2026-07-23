package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll(IEventBus modEventBus) {
        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeClientEvents());
        modEventBus.addListener(FancyMenuForgeClientEvents::onRegisterClientReloadListeners);
    }

    private static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.FORGE, reloadAction -> event.registerReloadListener(createReloadListener(reloadAction)))) {
            LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via Forge API.");
        }
    }

    private static SimplePreparableReloadListener<String> createReloadListener(java.util.function.Consumer<ResourceManager> reloadAction) {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected String prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return "FANCYMENU RESOURCE RELOAD LISTENER";
            }

            @Override
            protected void apply(String prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
                reloadAction.accept(resourceManager);
            }
        };
    }

    @SubscribeEvent
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        Minecraft.getInstance().execute(PacketHandler::sendHandshakeToServer);
    }
}
