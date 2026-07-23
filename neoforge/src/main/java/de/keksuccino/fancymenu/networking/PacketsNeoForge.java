package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayload;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.minecraft.network.protocol.PacketFlow;
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
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_CLIENT_WIRE_DIRECTION, s);
            PacketHandlerNeoForge.sendToClient(payload, player);
        });

        PacketHandler.setSendToServerLogic((connection, s) -> {
            BridgePacketPayload payload = new BridgePacketPayload(BridgePacketPayload.TO_SERVER_WIRE_DIRECTION, s);
            PacketHandlerNeoForge.sendToServer(payload, connection);
        });

    }

    public static void registerBridgePacketNeoForge(RegisterPayloadHandlersEvent e) {

        //using the optional() registrar is important to be able to connect to servers without FM installed
        PayloadRegistrar registrar = e.registrar("fancymenu").optional();

        registrar.playBidirectional(BridgePacketPayload.TYPE, BridgePacketPayload.CODEC, PacketsNeoForge::handleBridgePacket);

    }

    private static void handleBridgePacket(BridgePacketPayload payload, IPayloadContext context) {
        try {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                payload.handle(null, PacketHandler.PacketDirection.TO_CLIENT, context.connection());
            } else if (context.player() instanceof ServerPlayer sender) {
                payload.handle(sender, PacketHandler.PacketDirection.TO_SERVER);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle NeoForge bridge packet!", ex);
        }
    }
}
