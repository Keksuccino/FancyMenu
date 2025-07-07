package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketHandlerForge;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketMessageForge;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;

public class PacketsForge {

    public static void init() {

        Packets.registerAll();

        registerForgeBridgePacket();

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketMessageForge msg = new BridgePacketMessageForge();
            msg.direction = "client";
            msg.dataWithIdentifier = s;
            PacketHandlerForge.send(PacketDistributor.PLAYER.with(player), msg);
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketMessageForge msg = new BridgePacketMessageForge();
            msg.direction = "server";
            msg.dataWithIdentifier = s;
            PacketHandlerForge.sendToServer(msg);
        });

    }

    private static void registerForgeBridgePacket() {

        PacketHandlerForge.registerMessage(BridgePacketMessageForge.class, (msg, buf) -> {

            //Write data from message to byte buf
            buf.writeUtf(msg.direction);
            buf.writeUtf(msg.dataWithIdentifier);

        }, (buf) -> {

            //Write data from byte buf to msg
            BridgePacketMessageForge msg = new BridgePacketMessageForge();
            msg.direction = buf.readUtf();
            msg.dataWithIdentifier = buf.readUtf();
            return msg;

        }, (msg, context) -> {

            //Handle packet
            context.enqueueWork(() -> {
                //Handle on client
                if (FMLEnvironment.dist.isClient()) {
                    //Handle both sides on client, because integrated server needs handling too
                    if (msg.direction.equals("server")) {
                        BridgePacketHandlerForge.handle(context.getSender(), msg, PacketHandler.PacketDirection.TO_SERVER);
                    } else if (msg.direction.equals("client")) {
                        BridgePacketHandlerForge.handle(null, msg, PacketHandler.PacketDirection.TO_CLIENT);
                    }
                }
                //Handle on server
                if (FMLEnvironment.dist.isDedicatedServer()) {
                    if (msg.direction.equals("server")) {
                        BridgePacketHandlerForge.handle(context.getSender(), msg, PacketHandler.PacketDirection.TO_SERVER);
                    }
                }
            });
            context.setPacketHandled(true);

        });

    }

}
