package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FancyMenuForgeClientEvents {

    public static void registerAll() {

        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeClientEvents());

    }

    @SubscribeEvent
    public void afterScreenKeyPress(ScreenEvent.KeyboardKeyPressedEvent.Post e) {

        ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) o.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers());

    }

    @SubscribeEvent
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        Minecraft.getInstance().execute(PacketHandler::sendHandshakeToServer);
    }

}
