package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuFabricClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll() {

        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.FABRIC, FancyMenuFabricClientEvents::registerReloadListener)) {
            LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via Fabric API.");
        }

        ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> PacketHandler.onClientConnected(clientPacketListener.getConnection()));

        // The server registers its channels only after JOIN, and Fabric applies that registration on the game thread right before
        // firing this event. This is the first moment FancyMenu's channel is guaranteed to be sendable, so the handshake waits for it.
        C2SPlayChannelEvents.REGISTER.register((clientPacketListener, packetSender, minecraft, channels) -> {
            if (channels.contains(BridgePacketPayload.ID)) PacketHandler.sendHandshakeToServer(clientPacketListener.getConnection());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPacketListener, minecraft) -> PacketHandler.onClientDisconnected(clientPacketListener.getConnection()));

    }

    private static void registerReloadListener(java.util.function.Consumer<ResourceManager> reloadAction) {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return FancyMenuResourceReload.FANCYMENU_RELOAD_LISTENER_ID;
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                reloadAction.accept(resourceManager);
            }
        });
    }
}
