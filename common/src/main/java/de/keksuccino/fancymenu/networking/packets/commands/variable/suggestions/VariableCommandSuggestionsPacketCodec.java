package de.keksuccino.fancymenu.networking.packets.commands.variable.suggestions;

import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.PacketCodec;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

public class VariableCommandSuggestionsPacketCodec extends PacketCodec<VariableCommandSuggestionsPacket> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Screen lastScreen;

    public VariableCommandSuggestionsPacketCodec() {
        super("variable_command_suggestions", VariableCommandSuggestionsPacket.class);
        if (Services.PLATFORM.isOnClient()) {
            EventHandler.INSTANCE.registerListenersOf(this);
        }
    }

    @EventListener
    public void onClientTick(ClientTickEvent.Post e) {
        try {
            Screen s = Minecraft.getInstance().screen;
            if ((s instanceof ChatScreen) && ((lastScreen == null) || (lastScreen != s))) {
                VariableCommandSuggestionsPacket packet = new VariableCommandSuggestionsPacket();
                packet.variable_suggestions = getVariableNameSuggestions();
                PacketHandler.sendToServer(packet);
            }
            lastScreen = s;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to send variable command suggestions packet to server!", ex);
        }
    }

    protected static List<String> getVariableNameSuggestions() {
        List<String> l = VariableHandler.getVariableNames();
        if (l.isEmpty()) {
            l.add("<no_variables_found>");
        }
        return l;
    }

}
