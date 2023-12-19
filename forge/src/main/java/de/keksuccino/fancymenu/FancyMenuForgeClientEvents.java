package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.commands.client.CloseGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.OpenGuiScreenCommand;
import de.keksuccino.fancymenu.commands.client.VariableCommand;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FancyMenuForgeClientEvents {

    public static void registerAll() {

        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeClientEvents());

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
