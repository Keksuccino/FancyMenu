package de.keksuccino.fancymenu.networking.packets.commands.layout.command;

import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ClientSideLayoutCommandPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull LayoutCommandPacket packet) {
        if (Minecraft.getInstance().player == null) return false;
        try {
            Objects.requireNonNull(packet.layout_name);
            Layout layout = LayoutHandler.getLayout(packet.layout_name);
            if (layout != null) {
                layout.setEnabled(packet.enabled, true);
                return true;
            } else {
                packet.sendChatFeedback(Components.translatable("fancymenu.commmands.layout.unable_to_set_state"), true);
            }
        } catch (Exception ex) {
            packet.sendChatFeedback(Components.translatable("fancymenu.commmands.layout.error"), true);
            LOGGER.error("[FANCYMENU] An error happened while trying to set a layout state via the /fmlayout command!", ex);
        }
        return false;
    }

}
