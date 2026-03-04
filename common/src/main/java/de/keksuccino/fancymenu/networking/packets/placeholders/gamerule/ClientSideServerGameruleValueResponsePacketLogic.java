package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.customization.placeholder.placeholders.world.GameruleValuePlaceholder;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClientSideServerGameruleValueResponsePacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerGameruleValueResponsePacket packet) {
        if (Minecraft.getInstance().player == null) {
            LOGGER.warn("[FANCYMENU] Ignoring server gamerule value response because no client player is present.");
            return false;
        }

        if (packet.placeholder == null) {
            LOGGER.warn("[FANCYMENU] Received server gamerule value response without placeholder key.");
            return false;
        }

        String data = packet.data == null ? "" : packet.data;
        GameruleValuePlaceholder.handleServerResponse(packet.placeholder, data);
        return true;
    }

}
