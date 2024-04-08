package de.keksuccino.fancymenu.networking.packets.commands.closegui;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class CloseGuiCommandPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return ClientSideCloseGuiCommandPacketLogic.handle(this);
    }

}
