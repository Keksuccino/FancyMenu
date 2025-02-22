package de.keksuccino.fancymenu.networking.packets.commands.layout.command;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class LayoutCommandPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public String layout_name;
    public boolean enabled;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return ClientSideLayoutCommandPacketLogic.handle(this);
    }

}
