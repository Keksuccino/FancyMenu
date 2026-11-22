package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.customization.placeholder.placeholders.world.GameruleValueResolver;
import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ServerSideServerGameruleValueRequestPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerPlayer sender, @NotNull ServerGameruleValueRequestPacket packet) {
        if ((packet.placeholder == null) || packet.placeholder.isEmpty()) {
            LOGGER.warn("[FANCYMENU] Received malformed gamerule placeholder request without placeholder string.");
            return false;
        }

        String result = "";
        try {
            MinecraftServer server = sender.level().getServer();
            if (server != null) {
                String gameruleValue = GameruleValueResolver.getValue(server.getGameRules(), packet.gamerule);
                if (gameruleValue != null) {
                    result = gameruleValue;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to resolve server-side gamerule placeholder.", ex);
        }

        PacketHandler.sendToClient(sender, new ServerGameruleValueResponsePacket(packet.placeholder, result));
        return true;
    }

}
