package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayloadNeoForge;
import de.keksuccino.fancymenu.networking.bridge.BridgePacketPayloadHandlerNeoForge;
import de.keksuccino.fancymenu.networking.packets.Packets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

public class PacketsNeoForge {

    public static void init(IEventBus eventBus) {

        Packets.registerAll();

        eventBus.addListener(PacketsNeoForge::registerBridgePacketNeoForge);

        PacketHandler.setSendToClientLogic((player, s) -> {
            BridgePacketPayloadNeoForge msg = new BridgePacketPayloadNeoForge(s);
            PacketHandlerNeoForge.sendToClient(msg, player);
        });

        PacketHandler.setSendToServerLogic(s -> {
            BridgePacketPayloadNeoForge msg = new BridgePacketPayloadNeoForge(s);
            PacketHandlerNeoForge.sendToServer(msg);
        });

    }

    public static void registerBridgePacketNeoForge(RegisterPayloadHandlerEvent e) {

        //using the optional() registrar is important to be able to connect to servers without FM installed
        IPayloadRegistrar registrar = e.registrar("fancymenu").optional();

        registrar.play(BridgePacketPayloadNeoForge.ID, BridgePacketPayloadNeoForge::new, handlerBuilder -> {
            handlerBuilder.server((payload, context) -> BridgePacketPayloadHandlerNeoForge.getInstance().handleData(payload, PacketHandler.PacketDirection.TO_SERVER, context));
            handlerBuilder.client((payload, context) -> BridgePacketPayloadHandlerNeoForge.getInstance().handleData(payload, PacketHandler.PacketDirection.TO_CLIENT, context));
        });

    }

}
