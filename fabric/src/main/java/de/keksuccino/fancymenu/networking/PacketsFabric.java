package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketHandlerFabric;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketMessageFabric;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class PacketsFabric {

    public static void init() {

        Packets.registerAll();

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketMessageFabric msg = new BridgePacketMessageFabric();
            msg.direction = "client";
            msg.dataWithIdentifier = s;
            ServerPlayNetworking.send(player, BridgePacketHandlerFabric.PACKET_ID, BridgePacketHandlerFabric.writeToByteBuf(msg));
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketMessageFabric msg = new BridgePacketMessageFabric();
            msg.direction = "server";
            msg.dataWithIdentifier = s;
            ClientPlayNetworking.send(BridgePacketHandlerFabric.PACKET_ID, BridgePacketHandlerFabric.writeToByteBuf(msg));
        });

        registerFabricBridgePacket();

    }

    private static void registerFabricBridgePacket() {

        //ON SERVER
        ServerPlayNetworking.registerGlobalReceiver(BridgePacketHandlerFabric.PACKET_ID, (server, player, handler, buf, responseSender) -> {
            BridgePacketMessageFabric msg = BridgePacketHandlerFabric.readFromByteBuf(buf);
            BridgePacketHandlerFabric.handle(player, msg, PacketHandler.PacketDirection.TO_SERVER);
        });

        //ON CLIENT
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(BridgePacketHandlerFabric.PACKET_ID, (client, handler, buf, responseSender) -> {
                BridgePacketMessageFabric msg = BridgePacketHandlerFabric.readFromByteBuf(buf);
                BridgePacketHandlerFabric.handle(null, msg, PacketHandler.PacketDirection.TO_CLIENT);
            });
        }

    }

}
