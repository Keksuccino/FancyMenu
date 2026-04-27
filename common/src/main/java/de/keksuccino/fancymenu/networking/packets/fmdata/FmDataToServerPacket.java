package de.keksuccino.fancymenu.networking.packets.fmdata;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class FmDataToServerPacket extends Packet {

    public String data_identifier;
    public String data;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) {
            return false;
        }
        return ServerSideFmDataToServerPacketLogic.handle(sender, this);
    }

}
