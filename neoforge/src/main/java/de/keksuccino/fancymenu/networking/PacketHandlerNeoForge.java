package de.keksuccino.fancymenu.networking;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PacketHandlerNeoForge {

    public static void sendToClient(@NotNull CustomPacketPayload packet, @NotNull ServerPlayer toPlayer) {
        toPlayer.connection.send(Objects.requireNonNull(packet));
    }

    public static void sendToServer(@NotNull CustomPacketPayload packet, @NotNull Connection connection) {
        Objects.requireNonNull(connection).send(new ServerboundCustomPayloadPacket(Objects.requireNonNull(packet)));
    }

}
