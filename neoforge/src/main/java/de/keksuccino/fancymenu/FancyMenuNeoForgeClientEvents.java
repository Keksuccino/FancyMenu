package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FancyMenuNeoForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll(@NotNull IEventBus bus) {

        NeoForge.EVENT_BUS.register(new FancyMenuNeoForgeClientEvents());

        bus.addListener(FancyMenuNeoForgeClientEvents::onAddClientReloadListeners);

    }

    public static void onAddClientReloadListeners(AddClientReloadListenersEvent e) {
        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.NEOFORGE, listener -> e.addListener(FancyMenuResourceReload.FANCYMENU_RELOAD_LISTENER_ID, listener))) {
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
