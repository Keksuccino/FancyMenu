package de.keksuccino.fancymenu.networking;

import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PacketHandlerNeoForge {

    public static void sendToClient(@NotNull CustomPacketPayload packet, @NotNull ServerPlayer toPlayer) {
        ServerGamePacketListenerImpl connection = Objects.requireNonNull(toPlayer).connection;
        OptionalPayloadSender.sendIfSupported(connection, Objects.requireNonNull(packet), (listener, payload) -> NetworkRegistry.hasChannel(listener, payload.type().id()), ServerGamePacketListenerImpl::send);
    }

    public static void sendToServer(@NotNull CustomPacketPayload packet, @NotNull Connection connection) {
        OptionalPayloadSender.sendIfSupported(Objects.requireNonNull(connection), Objects.requireNonNull(packet), (exactConnection, payload) -> NetworkRegistry.hasChannel(exactConnection, ConnectionProtocol.PLAY, payload.type().id()), (exactConnection, payload) -> exactConnection.send(new ServerboundCustomPayloadPacket(payload)));
    }

}
