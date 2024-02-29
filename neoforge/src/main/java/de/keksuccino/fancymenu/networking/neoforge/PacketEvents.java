package de.keksuccino.fancymenu.networking.neoforge;

import de.keksuccino.fancymenu.networking.neoforge.packets.execute.ClientboundExecutePacketPayload;
import de.keksuccino.fancymenu.networking.neoforge.packets.execute.ClientboundExecutePacketPayloadHandler;
import de.keksuccino.fancymenu.networking.neoforge.packets.variablesuggestions.ServerboundVariableSuggestionsPacketPayload;
import de.keksuccino.fancymenu.networking.neoforge.packets.variablesuggestions.ServerboundVariableSuggestionsPacketPayloadHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

public class PacketEvents {

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlerEvent e) {

        IPayloadRegistrar registrar = e.registrar("fancymenu");

        registrar.play(ClientboundExecutePacketPayload.ID, ClientboundExecutePacketPayload::new, handler -> handler
                .client(ClientboundExecutePacketPayloadHandler.getInstance()::handleData));

        registrar.play(ServerboundVariableSuggestionsPacketPayload.ID, ServerboundVariableSuggestionsPacketPayload::new, handler -> handler
                .server(ServerboundVariableSuggestionsPacketPayloadHandler.getInstance()::handleData));

    }

}
