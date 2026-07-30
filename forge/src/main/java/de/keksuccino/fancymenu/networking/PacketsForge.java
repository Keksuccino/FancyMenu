package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;

public class PacketsForge {

    public static void init() {

        Packets.registerAll();

        registerForgeBridgePacket();

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, s);
            PacketHandlerForge.send(PacketDistributor.PLAYER.with(() -> player), payload);
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, s);
            PacketHandlerForge.sendToServer(payload);
        });

    }

    private static void registerForgeBridgePacket() {

        PacketHandlerForge.registerMessage(BridgePacketPayload.class, BridgePacketPayload::write, BridgePacketPayload::read, (payload, context) -> {

            //Handle packet
            context.get().enqueueWork(() -> {
                //Handle on client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    //Handle both sides on client, because integrated server needs handling too
                    if (payload.direction().equals(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION)) {
                        payload.handle(context.get().getSender(), PacketHandler.PacketDirection.TO_SERVER);
                    } else if (payload.direction().equals(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION)) {
                        payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT);
                    }
                });
                //Handle on server
                DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
                    if (payload.direction().equals(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION)) {
                        payload.handle(context.get().getSender(), PacketHandler.PacketDirection.TO_SERVER);
                    }
                });
            });
            context.get().setPacketHandled(true);

        });

    }

}
