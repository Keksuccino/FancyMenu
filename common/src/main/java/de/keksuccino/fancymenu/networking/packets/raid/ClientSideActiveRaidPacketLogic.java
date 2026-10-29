package de.keksuccino.fancymenu.networking.packets.raid;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.level.raid.ActiveRaidClientState;
import net.minecraft.network.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClientSideActiveRaidPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ActiveRaidPacket packet, @NotNull Connection connection) {
        return handle(packet, connection, ActiveRaidClientState.INSTANCE);
    }

    static boolean handle(@NotNull ActiveRaidPacket packet, @NotNull Object connection, @NotNull ActiveRaidClientState state) {
        try {
            if (packet.event_type == null) return false;
            // Update the state first, so placeholders and requirements already return the new values in the listeners' actions
            state.apply(connection, packet.event_type, packet.raid_id, packet.toSnapshot());
            switch (packet.event_type) {
                case STARTED -> Listeners.ON_RAID_STARTED.onRaidStarted();
                case ENTERED -> Listeners.ON_ENTER_ACTIVE_RAID_AREA.onActiveRaidAreaEntered();
                case LEFT -> Listeners.ON_LEAVE_ACTIVE_RAID_AREA.onActiveRaidAreaLeft();
                case FINISHED -> Listeners.ON_RAID_FINISHED.onRaidFinished(packet.victory);
                case UPDATED, REMOVED -> {
                }
            }
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to process active raid packet!", ex);
        }
        return false;
    }

}
