package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.Connection;
import net.minecraft.server.packs.PackType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuFabricClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll() {

        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(FancyMenuResourceReload.FANCYMENU_RELOAD_LISTENER_ID, listener))) {
            LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via Fabric API.");
        }

        ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
            Connection connection = clientPacketListener.getConnection();
            PacketHandler.onClientConnected(connection);
            minecraft.execute(() -> PacketHandler.sendHandshakeToServer(connection));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPacketListener, minecraft) -> PacketHandler.onClientDisconnected(clientPacketListener.getConnection()));

    }

}
