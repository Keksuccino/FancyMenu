package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgeChunkPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeMessageSender;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketsFabric {

    public static void init() {

        Packets.registerAll();

        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactPlayer -> ServerPlayNetworking.canSend(exactPlayer, BridgeChunkPayload.ID), PacketsFabric::sendLegacyToClientIfNegotiated, PacketsFabric::sendChunkToClientIfNegotiated));

        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactConnection -> canClientSend(exactConnection, BridgeChunkPayload.ID), PacketsFabric::sendLegacyToServerIfNegotiated, PacketsFabric::sendChunkToServerIfNegotiated));

        registerFabricBridgePacket();

    }

    private static void registerFabricBridgePacket() {

        //ON SERVER
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketPayload.ID, (server, player, handler, buf, responseSender) -> {
            BridgePacketPayload payload = BridgePacketPayload.read(buf);
            payload.handle(player, PacketHandler.PacketDirection.TO_SERVER, null);
        });
        ServerPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.ID, (server, player, handler, buf, responseSender) -> {
            BridgeChunkPayload payload = BridgeChunkPayload.read(buf);
            payload.handle(player, PacketHandler.PacketDirection.TO_SERVER);
        });

        //ON CLIENT
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(BridgePacketPayload.ID, (client, handler, buf, responseSender) -> {
                BridgePacketPayload payload = BridgePacketPayload.read(buf);
                payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, handler.getConnection());
            });
            ClientPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.ID, (client, handler, buf, responseSender) -> {
                BridgeChunkPayload payload = BridgeChunkPayload.read(buf);
                payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, handler.getConnection());
            });
        }

    }

    private static boolean canClientSend(Connection exactConnection, ResourceLocation channel) {
        if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().getConnection().getConnection() != exactConnection) return false;
        return ClientPlayNetworking.canSend(channel);
    }

    private static boolean sendLegacyToServerIfNegotiated(Connection exactConnection, BridgePacketPayload payload) {
        if (!canClientSend(exactConnection, BridgePacketPayload.ID)) return false;
        exactConnection.send(ClientPlayNetworking.createC2SPacket(BridgePacketPayload.ID, payload.writeToNewBuffer()));
        return true;
    }

    private static boolean sendChunkToServerIfNegotiated(Connection exactConnection, BridgeChunkPayload payload) {
        if (!canClientSend(exactConnection, BridgeChunkPayload.ID)) return false;
        exactConnection.send(ClientPlayNetworking.createC2SPacket(BridgeChunkPayload.ID, payload.writeToNewBuffer()));
        return true;
    }

    private static boolean sendLegacyToClientIfNegotiated(ServerPlayer exactPlayer, BridgePacketPayload payload) {
        if (!ServerPlayNetworking.canSend(exactPlayer, BridgePacketPayload.ID)) return false;
        ServerPlayNetworking.send(exactPlayer, BridgePacketPayload.ID, payload.writeToNewBuffer());
        return true;
    }

    private static boolean sendChunkToClientIfNegotiated(ServerPlayer exactPlayer, BridgeChunkPayload payload) {
        if (!ServerPlayNetworking.canSend(exactPlayer, BridgeChunkPayload.ID)) return false;
        ServerPlayNetworking.send(exactPlayer, BridgeChunkPayload.ID, payload.writeToNewBuffer());
        return true;
    }

}
