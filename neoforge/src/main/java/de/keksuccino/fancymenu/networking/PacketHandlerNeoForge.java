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

    public static boolean canSendToClient(@NotNull CustomPacketPayload.Type<?> type, @NotNull ServerPlayer toPlayer) {
        ServerGamePacketListenerImpl connection = Objects.requireNonNull(toPlayer).connection;
        return NetworkRegistry.hasChannel(connection, Objects.requireNonNull(type).id());
    }

    public static boolean sendToClient(@NotNull CustomPacketPayload packet, @NotNull ServerPlayer toPlayer) {
        ServerGamePacketListenerImpl connection = Objects.requireNonNull(toPlayer).connection;
        return OptionalPayloadSender.sendIfSupported(connection, Objects.requireNonNull(packet), (listener, payload) -> NetworkRegistry.hasChannel(listener, payload.type().id()), ServerGamePacketListenerImpl::send);
    }

    public static boolean canSendToServer(@NotNull CustomPacketPayload.Type<?> type, @NotNull Connection connection) {
        return NetworkRegistry.hasChannel(Objects.requireNonNull(connection), ConnectionProtocol.PLAY, Objects.requireNonNull(type).id());
    }

    public static boolean sendToServer(@NotNull CustomPacketPayload packet, @NotNull Connection connection) {
        return OptionalPayloadSender.sendIfSupported(Objects.requireNonNull(connection), Objects.requireNonNull(packet), (exactConnection, payload) -> NetworkRegistry.hasChannel(exactConnection, ConnectionProtocol.PLAY, payload.type().id()), (exactConnection, payload) -> exactConnection.send(new ServerboundCustomPayloadPacket(payload)));
    }

}
