package de.keksuccino.fancymenu.networking.packets.commands.layout.suggestions;

import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.networking.PacketCodec;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class LayoutCommandSuggestionsPacketCodec extends PacketCodec<LayoutCommandSuggestionsPacket> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Screen lastScreen;

    public LayoutCommandSuggestionsPacketCodec() {
        super("layout_command_suggestions", LayoutCommandSuggestionsPacket.class);
        if (Services.PLATFORM.isOnClient()) {
            EventHandler.INSTANCE.registerListenersOf(this);
        }
    }

    @EventListener
    public void onClientTick(ClientTickEvent.Post e) {
        try {
            Screen s = Minecraft.getInstance().screen;
            if ((s instanceof ChatScreen) && ((lastScreen == null) || (lastScreen != s))) {
                LayoutCommandSuggestionsPacket packet = new LayoutCommandSuggestionsPacket();
                packet.layout_suggestions = getLayoutNameSuggestions();
                PacketHandler.sendToServer(packet);
            }
            lastScreen = s;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to send layout command suggestions packet to server!", ex);
        }
    }

    protected static List<String> getLayoutNameSuggestions() {
        List<String> l = new ArrayList<>();
        LayoutHandler.getAllLayouts().forEach(layout -> l.add(layout.getLayoutName()));
        if (l.isEmpty()) {
            l.add("<no_layouts_found>");
        }
        return l;
    }

}
