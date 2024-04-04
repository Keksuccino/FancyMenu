package de.keksuccino.fancymenu.networking.packets.commands.opengui;

import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Objects;

public class OpenGuiCommandPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public String screen_identifier;

    @Override
    public boolean processPacket() {
        if (Minecraft.getInstance().player == null) return false;
        try {
            Objects.requireNonNull(this.screen_identifier);
            if (this.screen_identifier.equalsIgnoreCase(CreateWorldScreen.class.getName())) {
                CreateWorldScreen.openFresh(Minecraft.getInstance(), Minecraft.getInstance().screen);
                return true;
            }
            if (CustomGuiHandler.guiExists(this.screen_identifier)) {
                Screen custom = CustomGuiHandler.constructInstance(this.screen_identifier, Minecraft.getInstance().screen, null);
                if (custom != null) Minecraft.getInstance().setScreen(custom);
                return true;
            } else {
                Screen s = ScreenInstanceFactory.tryConstruct(ScreenIdentifierHandler.getBestIdentifier(this.screen_identifier));
                if (s != null) {
                    Minecraft.getInstance().setScreen(s);
                    return true;
                } else {
                    this.sendChatFeedback(Component.translatable("fancymenu.commmands.openguiscreen.unable_to_open_gui", this.screen_identifier), true);
                }
            }
        } catch (Exception ex) {
            this.sendChatFeedback(Component.translatable("fancymenu.commands.openguiscreen.error"), true);
            LOGGER.error("[FANCYMENU] An error happened while trying to open a GUI via the /openguiscreen command!", ex);
        }
        return false;
    }

}
