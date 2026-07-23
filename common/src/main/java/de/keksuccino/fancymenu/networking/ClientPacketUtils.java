package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.packets.handshake.HandshakePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientPacketUtils {

    protected static boolean shouldSendToServer(@NotNull Packet packet, @NotNull Connection connection) {
        if (!PacketHandler.isClientSessionActive(connection)) return false;
        return packet instanceof HandshakePacket || PacketHandler.isFancyMenuServer(connection);
    }

    @Nullable
    public static Connection getConnectedConnection() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener == null ? null : listener.getConnection();
    }

    @Nullable
    public static String getConnectedServerIp() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) return null;
        if (listener.getServerData() != null) return listener.getServerData().ip;
        Connection connection = listener.getConnection();
        return connection.getRemoteAddress() == null ? null : connection.getRemoteAddress().toString();
    }

}
