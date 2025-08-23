package de.keksuccino.fancymenu.networking;

import de.keksuccino.fancymenu.networking.packets.handshake.HandshakePacket;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.keksuccino.fancymenu.networking.PacketHandler.FANCYMENU_SERVERS;

public class ClientPacketUtils {

    protected static boolean shouldSendToServer(@NotNull Packet packet) {
        if (packet instanceof HandshakePacket) return true;
        String ip = getConnectedServerIp();
        if (ip == null) return false;
        if (!FANCYMENU_SERVERS.contains(ip)) return false;
        return true;
    }

    @Nullable
    public static String getConnectedServerIp() {
        if (Minecraft.getInstance().getConnection() == null) return null;
        if (Minecraft.getInstance().getConnection().getServerData() == null) return "local_lan_world";
        return Minecraft.getInstance().getConnection().getServerData().ip;
    }

}
