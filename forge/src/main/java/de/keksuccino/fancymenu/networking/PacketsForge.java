package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgeChunkPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeMessageSender;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraftforge.network.NetworkDirection;

public class PacketsForge {

    public static void init() {

        Packets.registerAll();

        registerForgeBridgePacket();

        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactPlayer -> PacketHandlerForge.isRemotePresent(exactPlayer.connection.connection), PacketHandlerForge::sendToClient, PacketHandlerForge::sendToClient));

        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, PacketHandlerForge::isRemotePresent, PacketHandlerForge::sendToServer, PacketHandlerForge::sendToServer));

    }

    private static void registerForgeBridgePacket() {

        PacketHandlerForge.registerMessage(BridgePacketPayload.class, BridgePacketPayload::write, BridgePacketPayload::read, (payload, context) -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) payload.handle(context.get().getSender(), PacketHandler.PacketDirection.TO_SERVER, null);
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.get().getNetworkManager());
            context.get().setPacketHandled(true);
        });

        PacketHandlerForge.registerMessage(BridgeChunkPayload.class, BridgeChunkPayload::write, BridgeChunkPayload::read, (payload, context) -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) payload.handle(context.get().getSender(), PacketHandler.PacketDirection.TO_SERVER);
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.get().getNetworkManager());
            context.get().setPacketHandled(true);
        });
    }

}
