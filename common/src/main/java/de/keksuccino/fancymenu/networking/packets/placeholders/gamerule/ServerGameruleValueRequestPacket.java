package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ServerGameruleValueRequestPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public String placeholder;
    public String gamerule;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) {
            LOGGER.warn("[FANCYMENU] Received ServerGameruleValueRequestPacket on client side. Discarding.");
            return false;
        }
        return ServerSideServerGameruleValueRequestPacketLogic.handle(sender, this);
    }

}
