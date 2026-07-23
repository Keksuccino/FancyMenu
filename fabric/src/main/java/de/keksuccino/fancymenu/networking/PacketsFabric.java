package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgeChunkPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeMessageSender;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import de.keksuccino.fancymenu.platform.Services;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class PacketsFabric {

    public static void init() {

        Packets.registerAll();

        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactPlayer -> ServerPlayNetworking.canSend(exactPlayer, BridgeChunkPayload.TYPE), PacketsFabric::sendToClientIfNegotiated, PacketsFabric::sendToClientIfNegotiated));

        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactConnection -> canClientSend(exactConnection, BridgeChunkPayload.TYPE), PacketsFabric::sendToServerIfNegotiated, PacketsFabric::sendToServerIfNegotiated));

        registerFabricBridgePacket();

    }

    private static void registerFabricBridgePacket() {

        PayloadTypeRegistry.serverboundPlay().register(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BridgeChunkPayload.TYPE, BridgeChunkPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BridgeChunkPayload.TYPE, BridgeChunkPayload.CODEC);

        //ON SERVER
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketPayload.TYPE, (payload, context) -> payload.handle(context.player(), PacketHandler.PacketDirection.TO_SERVER));
        ServerPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.TYPE, (payload, context) -> payload.handle(context.player(), PacketHandler.PacketDirection.TO_SERVER));

        //ON CLIENT
        if (Services.PLATFORM.isOnClient()) {
            ClientPlayNetworking.registerGlobalReceiver(BridgePacketPayload.TYPE, (payload, context) -> payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.player().connection.getConnection()));
            ClientPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.TYPE, (payload, context) -> payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.player().connection.getConnection()));
        }

    }

    private static boolean canClientSend(Connection exactConnection, CustomPacketPayload.Type<?> type) {
        if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().getConnection().getConnection() != exactConnection) return false;
        return ClientPlayNetworking.canSend(type);
    }

    private static boolean sendToServerIfNegotiated(Connection exactConnection, CustomPacketPayload payload) {
        if (!canClientSend(exactConnection, payload.type())) return false;
        exactConnection.send(ClientPlayNetworking.createServerboundPacket(payload));
        return true;
    }

    private static boolean sendToClientIfNegotiated(ServerPlayer exactPlayer, CustomPacketPayload payload) {
        if (!ServerPlayNetworking.canSend(exactPlayer, payload.type())) return false;
        ServerPlayNetworking.send(exactPlayer, payload);
        return true;
    }

}
