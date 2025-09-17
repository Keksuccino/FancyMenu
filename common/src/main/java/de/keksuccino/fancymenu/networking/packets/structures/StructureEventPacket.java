package de.keksuccino.fancymenu.networking.packets.structures;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class StructureEventPacket extends Packet {

    public StructureEventType event_type;
    public String structure_identifier;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender != null) {
            return false;
        }
        return ClientSideStructureEventPacketLogic.handle(this);
    }

    public enum StructureEventType {
        ENTER,
        LEAVE
    }
}
