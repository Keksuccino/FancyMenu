package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayloadNeoForge;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

        registrar.playBidirectional(
                BridgePacketPayloadNeoForge.TYPE,
                BridgePacketPayloadNeoForge.CODEC,
                PacketsNeoForge::handleServerboundBridgePacket,
                PacketsNeoForge::handleClientboundBridgePacket
        );

    }

    private static void handleServerboundBridgePacket(BridgePacketPayloadNeoForge payload, IPayloadContext context) {
        try {
            if (context.player() instanceof ServerPlayer sender) {
                payload.handle(sender, PacketHandler.PacketDirection.TO_SERVER);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge packet!", ex);
        }
    }

    private static void handleClientboundBridgePacket(BridgePacketPayloadNeoForge payload, IPayloadContext context) {
        try {
            payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge packet!", ex);
        }
    }

}
