package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgeChunkPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeMessageSender;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketsNeoForge {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void init(IEventBus eventBus) {

        Packets.registerAll();

        eventBus.addListener(PacketsNeoForge::registerBridgePacketNeoForge);

        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactPlayer -> PacketHandlerNeoForge.canSendToClient(BridgeChunkPayload.TYPE, exactPlayer), (exactPlayer, payload) -> PacketHandlerNeoForge.sendToClient(payload, exactPlayer), (exactPlayer, payload) -> PacketHandlerNeoForge.sendToClient(payload, exactPlayer)));

        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactConnection -> PacketHandlerNeoForge.canSendToServer(BridgeChunkPayload.TYPE, exactConnection), (exactConnection, payload) -> PacketHandlerNeoForge.sendToServer(payload, exactConnection), (exactConnection, payload) -> PacketHandlerNeoForge.sendToServer(payload, exactConnection)));

    }

    public static void registerBridgePacketNeoForge(RegisterPayloadHandlersEvent e) {

        //using the optional() registrar is important to be able to connect to servers without FM installed
        PayloadRegistrar registrar = e.registrar("fancymenu").optional();

        registrar.playBidirectional(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC, PacketsNeoForge::handleServerboundBridgePacket, PacketsNeoForge::handleClientboundBridgePacket);
        registrar.playBidirectional(BridgeChunkPayload.TYPE, BridgeChunkPayload.CODEC, PacketsNeoForge::handleServerboundBridgeChunk, PacketsNeoForge::handleClientboundBridgeChunk);

    }

    private static void handleServerboundBridgePacket(BridgePacketPayload payload, IPayloadContext context) {
        try {
            if (context.player() instanceof ServerPlayer sender) {
                payload.handle(sender, PacketHandler.PacketDirection.TO_SERVER);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge packet!", ex);
        }
    }

    private static void handleClientboundBridgePacket(BridgePacketPayload payload, IPayloadContext context) {
        try {
            payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.connection());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge packet!", ex);
        }
    }

    private static void handleServerboundBridgeChunk(BridgeChunkPayload payload, IPayloadContext context) {
        try {
            if (context.player() instanceof ServerPlayer sender) payload.handle(sender, PacketHandler.PacketDirection.TO_SERVER);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge chunk!", ex);
        }
    }

    private static void handleClientboundBridgeChunk(BridgeChunkPayload payload, IPayloadContext context) {
        try {
            payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.connection());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge chunk!", ex);
        }
    }

}
