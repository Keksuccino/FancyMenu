package de.keksuccino.fancymenu.networking.packets.commands.opengui;

import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ClientSideOpenGuiCommandPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull OpenGuiCommandPacket packet) {
        if (Minecraft.getInstance().player == null) return false;
        try {
            Objects.requireNonNull(packet.screen_identifier);
            if (CustomGuiHandler.guiExists(packet.screen_identifier)) {
                Screen custom = CustomGuiHandler.constructInstance(packet.screen_identifier, Minecraft.getInstance().screen, null);
                if (custom != null) Minecraft.getInstance().setScreen(custom);
                return true;
            } else {
                Screen s = ScreenInstanceFactory.tryConstruct(ScreenIdentifierHandler.getBestIdentifier(packet.screen_identifier));
                if (s != null) {
                    Minecraft.getInstance().setScreen(s);
                    return true;
                } else {
                    packet.sendChatFeedback(Components.translatable("fancymenu.commmands.openguiscreen.unable_to_open_gui", packet.screen_identifier), true);
                }
            }
        } catch (Exception ex) {
            packet.sendChatFeedback(Components.translatable("fancymenu.commands.openguiscreen.error"), true);
            LOGGER.error("[FANCYMENU] An error happened while trying to open a GUI via the /openguiscreen command!", ex);
        }
        return false;
    }

}
