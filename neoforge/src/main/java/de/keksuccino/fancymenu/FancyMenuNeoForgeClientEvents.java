package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FancyMenuNeoForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll(@NotNull IEventBus bus) {

        NeoForge.EVENT_BUS.register(new FancyMenuNeoForgeClientEvents());

        bus.addListener(FancyMenuNeoForgeClientEvents::onAddClientReloadListeners);

    }

    public static void onAddClientReloadListeners(AddClientReloadListenersEvent e) {
        LOGGER.info("[FANCYMENU] Registering FancyMenu's resource reload listener via NeoForge API..");
        e.addListener(FancyMenuResourceReload.FANCYMENU_RELOAD_LISTENER_ID, FancyMenuResourceReload.createMinecraftPreparableReloadListener());
    }

    @SubscribeEvent
    public void afterScreenKeyPress(ScreenEvent.KeyPressed.Post e) {
        ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(e.getScreen(), e.getKeyEvent());
        EventHandler.INSTANCE.postEvent(event);

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) {
            o.keyPressed(e.getKeyEvent());
        }
    }

    @SubscribeEvent
    public void afterScreenKeyRelease(ScreenEvent.KeyReleased.Post e) {
        ScreenKeyReleasedEvent event = new ScreenKeyReleasedEvent(e.getScreen(), e.getKeyEvent());
        EventHandler.INSTANCE.postEvent(event);
    }

    @SubscribeEvent
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        Minecraft.getInstance().execute(PacketHandler::sendHandshakeToServer);
    }

}
