package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuNeoForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(new FancyMenuNeoForgeClientEvents());

        modEventBus.addListener(FancyMenuNeoForgeClientEvents::onRegisterClientReloadListeners);

    }

    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.NEOFORGE, event::registerReloadListener)) {
            LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via NeoForge API.");
        }
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
