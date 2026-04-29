package de.keksuccino.fancymenu.networking.packets.structure.playerpos;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.level.StructureUtils;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public class PlayerPosStructuresPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handleOnServer(@NotNull PlayerPosStructuresPacket packet, @NotNull ServerPlayer sender) {
        ServerLevel level = sender.getLevel();
        PlayerPosStructuresPacket answer = new PlayerPosStructuresPacket();
        answer.structures = StructureUtils.convertStructureKeysToStrings(StructureUtils.getAllStructuresAt(level, sender.blockPosition()));
        PacketHandler.sendToClient(sender, answer);
        return true;
    }

    protected static boolean handleOnClient(@NotNull PlayerPosStructuresPacket packet) {
        MainThreadTaskExecutor.executeInMainThread(() -> {
            PlayerPosStructuresPacket.CACHED_CURRENT_STRUCTURES.clear();
            PlayerPosStructuresPacket.CACHED_CURRENT_STRUCTURES.addAll(Objects.requireNonNullElse(packet.structures, new ArrayList<>()));
        }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        return true;
    }

}
