package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuFabricClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll() {

        registerScreenEvents();

        ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
            Minecraft.getInstance().execute(PacketHandler::sendHandshakeToServer);
        });

    }

    private static void registerScreenEvents() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {

            ScreenKeyboardEvents.afterKeyPress(screen).register((screen1, event) ->  {

                EventHandler.INSTANCE.postEvent(new ScreenKeyPressedEvent(screen1, event));

                if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) o.keyPressed(event);

            });

            ScreenKeyboardEvents.afterKeyRelease(screen).register((screen1, event) -> {

                EventHandler.INSTANCE.postEvent(new ScreenKeyReleasedEvent(screen1, event));

            });

        });
    }

}
