package de.keksuccino.fancymenu.networking.packets.structures;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClientSideStructureEventPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull StructureEventPacket packet) {
        try {
            if (packet.structure_identifier == null || packet.structure_identifier.isBlank() || packet.event_type == null) {
                return false;
            }

            switch (packet.event_type) {
                case ENTER -> Listeners.ON_ENTER_STRUCTURE.onStructureEntered(packet.structure_identifier);
                case LEAVE -> Listeners.ON_LEAVE_STRUCTURE.onStructureLeft(packet.structure_identifier);
                case ENTER_HIGH_PRECISION -> Listeners.ON_ENTER_STRUCTURE_HIGH_PRECISION.onStructureEntered(packet.structure_identifier);
                case LEAVE_HIGH_PRECISION -> Listeners.ON_LEAVE_STRUCTURE_HIGH_PRECISION.onStructureLeft(packet.structure_identifier);
            }
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to process structure event packet!", ex);
        }
        return false;
    }
}
