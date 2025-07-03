package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayloadNeoForge;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketsNeoForge {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void init(IEventBus eventBus) {

        Packets.registerAll();

        eventBus.addListener(PacketsNeoForge::registerBridgePacketNeoForge);

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketPayloadNeoForge payload = new BridgePacketPayloadNeoForge("client", s);
            PacketHandlerNeoForge.sendToClient(payload, player);
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketPayloadNeoForge payload = new BridgePacketPayloadNeoForge("server", s);
            PacketHandlerNeoForge.sendToServer(payload);
        });

    }

    public static void registerBridgePacketNeoForge(RegisterPayloadHandlersEvent e) {

        //using the optional() registrar is important to be able to connect to servers without FM installed
        PayloadRegistrar registrar = e.registrar("fancymenu").optional();

        IPayloadHandler<BridgePacketPayloadNeoForge> handler = (payload, context) -> {
            try {
                if (context.flow() == PacketFlow.CLIENTBOUND) {
                    payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT);
                } else {
                    payload.handle((ServerPlayer) context.player(), PacketHandler.PacketDirection.TO_SERVER);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge packet!", ex);
            }
        };

        registrar.playBidirectional(BridgePacketPayloadNeoForge.TYPE, BridgePacketPayloadNeoForge.CODEC, handler, handler);

    }

}
