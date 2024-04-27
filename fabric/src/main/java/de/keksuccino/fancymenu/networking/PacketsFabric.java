package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayloadFabric;
import de.keksuccino.fancymenu.networking.packets.Packets;
import de.keksuccino.fancymenu.platform.Services;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PacketsFabric {

    public static void init() {

        Packets.registerAll();

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketPayloadFabric payload = new BridgePacketPayloadFabric("client", s);
            ServerPlayNetworking.send(player, payload);
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketPayloadFabric payload = new BridgePacketPayloadFabric("server", s);
            ClientPlayNetworking.send(payload);
        });

        registerFabricBridgePacket();

    }

    private static void registerFabricBridgePacket() {

        PayloadTypeRegistry.playC2S().register(BridgePacketPayloadFabric.TYPE, BridgePacketPayloadFabric.CODEC);
        PayloadTypeRegistry.playS2C().register(BridgePacketPayloadFabric.TYPE, BridgePacketPayloadFabric.CODEC);

        //ON SERVER
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketPayloadFabric.TYPE, (payload, context) -> {
            payload.handle(context.player(), PacketHandler.PacketDirection.TO_SERVER);
        });

        //ON CLIENT
        if (Services.PLATFORM.isOnClient()) {
            ClientPlayNetworking.registerGlobalReceiver(BridgePacketPayloadFabric.TYPE, (payload, context) -> {
                payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT);
            });
        }

    }

}
