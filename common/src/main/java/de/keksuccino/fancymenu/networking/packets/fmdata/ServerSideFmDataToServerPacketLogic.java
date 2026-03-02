package de.keksuccino.fancymenu.networking.packets.fmdata;

import de.keksuccino.fancymenu.fmdata.FmDataServerListenerHandler;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ServerSideFmDataToServerPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerPlayer sender, @NotNull FmDataToServerPacket packet) {
        try {
            String dataIdentifier = Objects.requireNonNullElse(packet.data_identifier, "");
            String data = Objects.requireNonNullElse(packet.data, "");
            FmDataServerListenerHandler.onClientDataReceived(sender, dataIdentifier, data);
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to process FMData packet sent to server!", ex);
        }
        return false;
    }

}
