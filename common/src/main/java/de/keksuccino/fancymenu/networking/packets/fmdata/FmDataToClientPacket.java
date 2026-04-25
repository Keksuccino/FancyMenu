package de.keksuccino.fancymenu.networking.packets.fmdata;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class FmDataToClientPacket extends Packet {

    public String data_identifier;
    public String data;
    public String sent_by;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender != null) {
            return false;
        }
        return ClientSideFmDataToClientPacketLogic.handle(this);
    }

}
