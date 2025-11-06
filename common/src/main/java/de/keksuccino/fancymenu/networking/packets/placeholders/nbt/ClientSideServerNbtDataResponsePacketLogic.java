package de.keksuccino.fancymenu.networking.packets.placeholders.nbt;

import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.ServerSideNbtDataGetPlaceholder;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ClientSideServerNbtDataResponsePacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerNbtDataResponsePacket packet) {
        if (Minecraft.getInstance().player == null) {
            LOGGER.warn("[FANCYMENU] Ignoring server NBT data response because no client player is present.");
            return false;
        }

        if (packet.placeholder == null) {
            LOGGER.warn("[FANCYMENU] Received server NBT data response without placeholder key.");
            return false;
        }

        String data = packet.data == null ? "" : packet.data;
        boolean success = packet.resultType == ServerNbtDataResponsePacket.ResultType.SUCCESS;
        ServerSideNbtDataGetPlaceholder.handleServerResponse(packet.placeholder, success ? data : "");
        return true;
    }

}
