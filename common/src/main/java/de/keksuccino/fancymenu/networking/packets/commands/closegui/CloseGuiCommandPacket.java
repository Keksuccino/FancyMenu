package de.keksuccino.fancymenu.networking.packets.commands.closegui;

import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CloseGuiCommandPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean processPacket() {
        if (Minecraft.getInstance().player == null) return false;
        try {
            Minecraft.getInstance().setScreen(null);
            return true;
        } catch (Exception ex) {
            this.sendChatFeedback(Component.translatable("fancymenu.commands.closeguiscreen.error"), true);
            LOGGER.error("[FANCYMENU] Failed to close GUI screen via /closeguiscreen command!", ex);
        }
        return false;
    }

}
