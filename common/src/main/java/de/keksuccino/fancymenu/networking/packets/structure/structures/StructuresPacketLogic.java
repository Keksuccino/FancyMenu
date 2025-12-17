package de.keksuccino.fancymenu.networking.packets.structure.structures;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.spiffyhud.util.level.StructureUtils;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StructuresPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handleOnServer(@NotNull StructuresPacket packet, @NotNull ServerPlayer sender) {
        StructuresPacket answer = new StructuresPacket();
        List<String> keys = new ArrayList<>();
        StructureUtils.getAllStructureKeys(sender.level().registryAccess()).forEach(structureResourceKey -> keys.add(structureResourceKey.location().toString()));
        answer.keys = keys;
        PacketHandler.sendToClient(sender, answer);
        return true;
    }

    protected static boolean handleOnClient(@NotNull StructuresPacket packet) {
        if (packet.keys != null) {
            MainThreadTaskExecutor.executeInMainThread(() -> {
                StructuresPacket.CACHED_SERVER_STRUCTURE_KEYS.clear();
                StructuresPacket.CACHED_SERVER_STRUCTURE_KEYS.addAll(packet.keys);
            }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            return true;
        }
        return false;
    }

}
