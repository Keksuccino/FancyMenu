package de.keksuccino.fancymenu.networking.packets.handshake;

import de.keksuccino.fancymenu.customization.fmdata.FmDataWelcomeDataHandler;
import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ServerSideHandshakePacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerPlayer sender, @NotNull HandshakePacket packet) {
        if (!PacketHandler.addFancyMenuClient(sender, packet.bridgeProtocolVersion())) return true;
        LOGGER.info("[FANCYMENU] A client with FancyMenu installed joined the server: " + sender.getScoreboardName());
        MinecraftServer server = sender.level().getServer();
        if (server != null) {
            // PacketHandler normally reaches this on the server thread, where execute runs inline. Keeping the executor
            // boundary also makes direct off-thread invocations dispatch welcome data on the correct thread.
            server.execute(() -> FmDataWelcomeDataHandler.onFancyMenuClientJoined(sender));
        }
        return true;
    }

}
