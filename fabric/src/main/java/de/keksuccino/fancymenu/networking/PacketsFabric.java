package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import de.keksuccino.fancymenu.platform.Services;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PacketsFabric {

    public static void init() {

        Packets.registerAll();

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketPayload payload = new BridgePacketPayload("client", s);
            ServerPlayNetworking.send(player, payload);
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketPayload payload = new BridgePacketPayload("server", s);
            ClientPlayNetworking.send(payload);
        });

        registerFabricBridgePacket();

    }

    private static void registerFabricBridgePacket() {

        PayloadTypeRegistry.playC2S().register(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC);

        //ON SERVER
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketPayload.TYPE, (payload, context) -> {
            payload.handle(context.player(), PacketHandler.PacketDirection.TO_SERVER);
        });

        //ON CLIENT
        if (Services.PLATFORM.isOnClient()) {
            ClientPlayNetworking.registerGlobalReceiver(BridgePacketPayload.TYPE, (payload, context) -> {
                payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT);
            });
        }

    }

}
