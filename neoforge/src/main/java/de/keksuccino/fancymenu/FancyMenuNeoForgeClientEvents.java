package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.client.CloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.VariableCommand;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class FancyMenuNeoForgeClientEvents {

    public static void registerAll() {

        NeoForge.EVENT_BUS.register(new FancyMenuNeoForgeClientEvents());

    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent e) {
        OpenGuiScreenCommand.register(e.getDispatcher());
        CloseGuiScreenCommand.register(e.getDispatcher());
        VariableCommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public void afterScreenKeyPress(ScreenEvent.KeyPressed.Post e) {

        ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) o.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers());

    }

}
