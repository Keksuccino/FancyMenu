package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

public class PacketsForge {

    public static void init() {

        Packets.registerAll();

        registerForgeBridgePacket();

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, s);
            PacketHandlerForge.send(PacketDistributor.PLAYER.with(() -> player), payload);
        });

        PacketHandler.setSendToServerLogic((connection, s) -> {
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, s);
            PacketHandlerForge.sendToServer(payload, connection);
        });

    }

    private static void registerForgeBridgePacket() {

        PacketHandlerForge.registerMessage(BridgePacketPayload.class, BridgePacketPayload::write, BridgePacketPayload::new, (payload, context) -> {
            context.get().enqueueWork(() -> {
                PacketHandler.PacketDirection direction = context.get().getDirection() == NetworkDirection.PLAY_TO_SERVER ? PacketHandler.PacketDirection.TO_SERVER : PacketHandler.PacketDirection.TO_CLIENT;
                payload.handle(context.get().getSender(), direction, direction == PacketHandler.PacketDirection.TO_CLIENT ? context.get().getNetworkManager() : null);
            });
            context.get().setPacketHandled(true);
        });

    }

}
