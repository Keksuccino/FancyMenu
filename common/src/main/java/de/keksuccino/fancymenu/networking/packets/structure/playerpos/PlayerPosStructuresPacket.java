package de.keksuccino.fancymenu.networking.packets.structure.playerpos;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PlayerPosStructuresPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final List<String> CACHED_CURRENT_STRUCTURES = Collections.synchronizedList(new ArrayList<>());

    public List<String> structures;

    @Override
    public boolean processPacket(@Nullable ServerPlayer sender) {
        if (sender == null) {
            return PlayerPosStructuresPacketLogic.handleOnClient(this);
        } else {
            return PlayerPosStructuresPacketLogic.handleOnServer(this, Objects.requireNonNull(sender));
        }
    }

}
