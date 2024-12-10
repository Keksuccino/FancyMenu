package de.keksuccino.fancymenu.networking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * @param sender The sender of the packet in case it was sent from client to server. This is NULL if the packet was sent by the server to the client!
     */
    public abstract boolean processPacket(@Nullable ServerPlayer sender);

    public void sendChatFeedback(@NotNull MutableComponent message, boolean failure) {
        try {
            if ((Minecraft.getInstance().player != null) && (Minecraft.getInstance().level != null)) {
                if (failure) message = message.withStyle(ChatFormatting.RED);
                Minecraft.getInstance().player.displayClientMessage(message, false);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to send packet chat feedback!", ex);
        }
    }

}
