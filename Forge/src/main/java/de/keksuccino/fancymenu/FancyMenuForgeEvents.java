package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.client.CloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.VariableCommand;
import de.keksuccino.fancymenu.commands.server.ServerCloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerOpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import de.keksuccino.fancymenu.events.screen.KeyPressedScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FancyMenuForgeEvents {

    public static void registerAll() {

        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeEvents());

    }

    @SubscribeEvent
    public void afterScreenKeyPress(ScreenEvent.KeyPressed.Post e) {
        KeyPressedScreenEvent event = new KeyPressedScreenEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent e) {
        OpenGuiScreenCommand.register(e.getDispatcher());
        CloseGuiScreenCommand.register(e.getDispatcher());
        VariableCommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public void onRegisterServerCommands(RegisterCommandsEvent e) {
        ServerOpenGuiScreenCommand.register(e.getDispatcher());
        ServerCloseGuiScreenCommand.register(e.getDispatcher());
        ServerVariableCommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
//        for (KeyMapping m : KeyMappings.KEY_MAPPINGS) {
//            e.register(m);
//        }
//        KeyMappings.init();
    }

}
