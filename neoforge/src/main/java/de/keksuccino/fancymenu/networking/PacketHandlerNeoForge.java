package de.keksuccino.fancymenu.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class PacketHandlerNeoForge {

    public static void sendToClient(@NotNull CustomPacketPayload packet, @NotNull ServerPlayer toPlayer) {
        toPlayer.connection.send(Objects.requireNonNull(packet));
    }

    public static void  sendToServer(@NotNull CustomPacketPayload packet) {
        if (Minecraft.getInstance().getConnection() == null) return;
        Minecraft.getInstance().getConnection().send(Objects.requireNonNull(packet));
    }

}
