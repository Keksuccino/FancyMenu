package de.keksuccino.fancymenu.networking.packets.entities;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientSideEntityEventPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull EntityEventPacket packet) {
        try {
            if (packet.event_type == null) {
                return false;
            }

            UUID entityUuid = parseUuid(packet.entity_uuid);

            switch (packet.event_type) {
                case SPAWN -> Listeners.ON_ENTITY_SPAWNED.onEntitySpawned(
                        packet.entity_key,
                        entityUuid,
                        packet.pos_x,
                        packet.pos_y,
                        packet.pos_z,
                        packet.level_identifier
                );
                case DEATH -> Listeners.ON_ENTITY_DIED.onEntityDied(
                        packet.entity_key,
                        entityUuid,
                        packet.pos_x,
                        packet.pos_y,
                        packet.pos_z,
                        packet.level_identifier
                );
            }
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to process entity event packet!", ex);
        }
        return false;
    }

    @Nullable
    private static UUID parseUuid(@Nullable String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("[FANCYMENU] Received entity event packet with invalid UUID: {}", uuidString, ex);
            return null;
        }
    }
}
