package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuNeoForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(new FancyMenuNeoForgeClientEvents());

        modEventBus.addListener(FancyMenuNeoForgeClientEvents::onRegisterClientReloadListeners);

    }

    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.NEOFORGE, event::registerReloadListener)) {
            LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via NeoForge API.");
        }
    }

    @SubscribeEvent
    public void afterScreenKeyPress(ScreenEvent.KeyPressed.Post e) {
        ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) {
            o.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers());
        }
    }

    @SubscribeEvent
    public void afterScreenKeyRelease(ScreenEvent.KeyReleased.Post e) {
        ScreenKeyReleasedEvent event = new ScreenKeyReleasedEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);
    }

    @SubscribeEvent
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        Connection connection = e.getConnection();
        PacketHandler.onClientConnected(connection);
        Minecraft.getInstance().execute(() -> PacketHandler.sendHandshakeToServer(connection));
    }

    @SubscribeEvent
    public void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
        PacketHandler.onClientDisconnected(e.getConnection());
    }
}
