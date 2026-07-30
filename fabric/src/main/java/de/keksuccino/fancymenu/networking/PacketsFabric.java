package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class PacketsFabric {

    public static void init() {

        Packets.registerAll();

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, s);
            ServerPlayNetworking.send(player, BridgePacketPayload.ID, payload.writeToNewBuffer());
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, s);
            ClientPlayNetworking.send(BridgePacketPayload.ID, payload.writeToNewBuffer());
        });

        registerFabricBridgePacket();

    }

    private static void registerFabricBridgePacket() {

        //ON SERVER
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketPayload.ID, (server, player, handler, buf, responseSender) -> {
            BridgePacketPayload payload = BridgePacketPayload.read(buf);
            payload.handle(player, PacketHandler.PacketDirection.TO_SERVER);
        });

        //ON CLIENT
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(BridgePacketPayload.ID, (client, handler, buf, responseSender) -> {
                BridgePacketPayload payload = BridgePacketPayload.read(buf);
                payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT);
            });
        }

    }

}
