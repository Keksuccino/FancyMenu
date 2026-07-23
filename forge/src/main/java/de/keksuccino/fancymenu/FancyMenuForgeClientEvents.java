package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll() {
        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeClientEvents());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(FancyMenuForgeClientEvents::registerReloadListener);
    }

    private static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.FORGE, event::registerReloadListener)) LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via Forge.");
    }

    @SubscribeEvent
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        Connection connection = e.getConnection();
        PacketHandler.onClientConnected(connection);
        Minecraft.getInstance().execute(() -> PacketHandler.sendHandshakeToServer(connection));
    }

    @SubscribeEvent
    public void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
        PacketHandler.onClientDisconnected(e.getConnection());
    }
}
