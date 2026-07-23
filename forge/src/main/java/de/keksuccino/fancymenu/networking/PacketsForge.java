package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeChunkPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeMessageSender;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraftforge.network.NetworkDirection;

public class PacketsForge {

    public static void init() {

        Packets.registerAll();

        registerForgeBridgePacket();

        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, PacketHandlerForge::canSendToClient, PacketHandlerForge::sendToClientIfNegotiated, PacketHandlerForge::sendToClientIfNegotiated));

        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, PacketHandlerForge::canSendToServer, PacketHandlerForge::sendToServerIfNegotiated, PacketHandlerForge::sendToServerIfNegotiated));

    }

    private static void registerForgeBridgePacket() {

        PacketHandlerForge.registerMessage(BridgePacketPayload.class, BridgePacketPayload::write, BridgePacketPayload::new, (payload, context) -> {
            context.get().enqueueWork(() -> {
                PacketHandler.PacketDirection direction = context.get().getDirection() == NetworkDirection.PLAY_TO_SERVER ? PacketHandler.PacketDirection.TO_SERVER : PacketHandler.PacketDirection.TO_CLIENT;
                payload.handle(context.get().getSender(), direction, direction == PacketHandler.PacketDirection.TO_CLIENT ? context.get().getNetworkManager() : null);
            });
            context.get().setPacketHandled(true);
        });

        PacketHandlerForge.registerMessage(BridgeChunkPayload.class, BridgeChunkPayload::write, BridgeChunkPayload::new, (payload, context) -> {
            context.get().enqueueWork(() -> {
                PacketHandler.PacketDirection direction = context.get().getDirection() == NetworkDirection.PLAY_TO_SERVER ? PacketHandler.PacketDirection.TO_SERVER : PacketHandler.PacketDirection.TO_CLIENT;
                payload.handle(context.get().getSender(), direction, direction == PacketHandler.PacketDirection.TO_CLIENT ? context.get().getNetworkManager() : null);
            });
            context.get().setPacketHandled(true);
        });

    }

}
