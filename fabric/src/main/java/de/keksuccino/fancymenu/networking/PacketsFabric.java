package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgeChunkPayload;
import de.keksuccino.fancymenu.networking.bridge.BridgeMessageSender;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public class PacketsFabric {

    public static void init() {

        Packets.registerAll();

        PacketHandler.setSendToClientLogic((player, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(player, BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactPlayer -> ServerPlayNetworking.canSend(exactPlayer, BridgeChunkPayload.ID), PacketsFabric::sendLegacyToClientIfNegotiated, PacketsFabric::sendChunkToClientIfNegotiated));

        PacketHandler.setSendToServerLogic((connection, data, bridgeProtocolV1Advertised) -> BridgeMessageSender.send(connection, BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, data, bridgeProtocolV1Advertised, exactConnection -> canClientSend(exactConnection, BridgeChunkPayload.ID), PacketsFabric::sendLegacyToServerIfNegotiated, PacketsFabric::sendChunkToServerIfNegotiated));

        registerFabricBridgePacket();

    }

    private static void registerFabricBridgePacket() {

        //ON SERVER
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketPayload.ID, (server, player, handler, buf, responseSender) -> new BridgePacketPayload(buf).handle(player, PacketHandler.PacketDirection.TO_SERVER));
        ServerPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.ID, (server, player, handler, buf, responseSender) -> new BridgeChunkPayload(buf).handle(player, PacketHandler.PacketDirection.TO_SERVER));

        //ON CLIENT
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(BridgePacketPayload.ID, (client, handler, buf, responseSender) -> new BridgePacketPayload(buf).handle(null, PacketHandler.PacketDirection.TO_CLIENT, handler.getConnection()));
            ClientPlayNetworking.registerGlobalReceiver(BridgeChunkPayload.ID, (client, handler, buf, responseSender) -> new BridgeChunkPayload(buf).handle(null, PacketHandler.PacketDirection.TO_CLIENT, handler.getConnection()));
        }

    }

    private static boolean canClientSend(Connection exactConnection, ResourceLocation channel) {
        if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().getConnection().getConnection() != exactConnection) return false;
        return ClientPlayNetworking.canSend(channel);
    }

    private static boolean sendLegacyToServerIfNegotiated(Connection exactConnection, BridgePacketPayload payload) {
        return sendToServerIfNegotiated(exactConnection, BridgePacketPayload.ID, payload::write);
    }

    private static boolean sendChunkToServerIfNegotiated(Connection exactConnection, BridgeChunkPayload payload) {
        return sendToServerIfNegotiated(exactConnection, BridgeChunkPayload.ID, payload::write);
    }

    private static boolean sendToServerIfNegotiated(Connection exactConnection, ResourceLocation channel, Consumer<FriendlyByteBuf> encoder) {
        if (!canClientSend(exactConnection, channel)) return false;
        exactConnection.send(new ServerboundCustomPayloadPacket(channel, encode(encoder)));
        return true;
    }

    private static boolean sendLegacyToClientIfNegotiated(ServerPlayer exactPlayer, BridgePacketPayload payload) {
        return sendToClientIfNegotiated(exactPlayer, BridgePacketPayload.ID, payload::write);
    }

    private static boolean sendChunkToClientIfNegotiated(ServerPlayer exactPlayer, BridgeChunkPayload payload) {
        return sendToClientIfNegotiated(exactPlayer, BridgeChunkPayload.ID, payload::write);
    }

    private static boolean sendToClientIfNegotiated(ServerPlayer exactPlayer, ResourceLocation channel, Consumer<FriendlyByteBuf> encoder) {
        if (!ServerPlayNetworking.canSend(exactPlayer, channel)) return false;
        ServerPlayNetworking.send(exactPlayer, channel, encode(encoder));
        return true;
    }

    private static FriendlyByteBuf encode(Consumer<FriendlyByteBuf> encoder) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            encoder.accept(byteBuf);
            return byteBuf;
        } catch (RuntimeException ex) {
            byteBuf.release();
            throw ex;
        }
    }
}
