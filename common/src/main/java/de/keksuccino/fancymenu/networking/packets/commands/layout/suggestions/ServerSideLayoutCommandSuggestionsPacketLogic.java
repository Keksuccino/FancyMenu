package de.keksuccino.fancymenu.networking.packets.commands.layout.suggestions;

import de.keksuccino.fancymenu.commands.LayoutCommand;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ServerSideLayoutCommandSuggestionsPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerPlayer sender, @NotNull LayoutCommandSuggestionsPacket packet) {
        try {
            String uuid = sender.getUUID().toString();
            LayoutCommand.CACHED_LAYOUT_SUGGESTIONS.put(uuid, packet.layout_suggestions);
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to process /fmlayout command suggestions packet!", ex);
        }
        return false;
    }

}
