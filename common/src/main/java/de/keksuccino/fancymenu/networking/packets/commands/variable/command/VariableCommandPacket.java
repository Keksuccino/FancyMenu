package de.keksuccino.fancymenu.networking.packets.commands.variable.command;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class VariableCommandPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public boolean set;
    public String variable_name;
    public String set_to_value;
    public boolean feedback;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        return ClientSideVariableCommandPacketLogic.handle(this);
    }

}
