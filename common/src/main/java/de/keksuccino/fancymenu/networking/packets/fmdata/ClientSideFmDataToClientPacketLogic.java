package de.keksuccino.fancymenu.networking.packets.fmdata;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ClientSideFmDataToClientPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull FmDataToClientPacket packet) {
        try {
            String identifier = Objects.requireNonNullElse(packet.data_identifier, "");
            String data = Objects.requireNonNullElse(packet.data, "");
            String sentBy = Objects.requireNonNullElse(packet.sent_by, "unknown_server");

            Listeners.ON_FM_DATA_RECEIVED.onDataReceived(identifier, data, sentBy);
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to process FMData packet sent to client!", ex);
        }
        return false;
    }

}
