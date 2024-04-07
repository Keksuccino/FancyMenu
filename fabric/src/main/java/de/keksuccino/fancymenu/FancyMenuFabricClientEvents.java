package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;

public class FancyMenuFabricClientEvents {

    public static void registerAll() {

        registerScreenEvents();

    }

    private static void registerScreenEvents() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register((screen1, key, scancode, modifiers) -> {

                ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(screen1, key, scancode, modifiers);
                EventHandler.INSTANCE.postEvent(event);

                if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) o.keyPressed(key, scancode, modifiers);

            });
        });
    }

}
