package de.keksuccino.fancymenu.networking.packets.structure.structures;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StructuresPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final List<String> CACHED_SERVER_STRUCTURE_KEYS = Collections.synchronizedList(new ArrayList<>());

    public boolean get;
    public List<String> keys;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) {
            return StructuresPacketLogic.handleOnClient(this);
        } else {
            return StructuresPacketLogic.handleOnServer(this, Objects.requireNonNull(sender));
        }
    }

}
