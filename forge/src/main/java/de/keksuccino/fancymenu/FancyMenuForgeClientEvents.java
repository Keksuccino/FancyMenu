package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;

public class FancyMenuForgeClientEvents {

    public static void registerAll() {

        ScreenEvent.KeyPressed.Post.BUS.addListener(FancyMenuForgeClientEvents::afterScreenKeyPress);

    }

    private static void afterScreenKeyPress(ScreenEvent.KeyPressed.Post e) {

        ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) o.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers());

    }

}
