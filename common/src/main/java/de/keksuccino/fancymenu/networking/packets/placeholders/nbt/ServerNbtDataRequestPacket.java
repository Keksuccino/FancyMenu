package de.keksuccino.fancymenu.networking.packets.placeholders.nbt;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ServerNbtDataRequestPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public String placeholder;
    public String source_type;
    public String entity_selector;
    public String block_pos;
    public String storage_id;
    public String nbt_path;
    public String return_type;
    public Double scale;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) {
            LOGGER.warn("[FANCYMENU] Received ServerNbtDataRequestPacket on client side. Discarding.");
            return false;
        }
        return ServerSideServerNbtDataRequestPacketLogic.handle(sender, this);
    }

}
