package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll(IEventBus modEventBus) {
        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeClientEvents());
        modEventBus.addListener(FancyMenuForgeClientEvents::onRegisterClientReloadListeners);
    }

    private static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.FORGE, reloadAction -> event.registerReloadListener(createReloadListener(reloadAction)))) {
            LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via Forge API.");
        }
    }

    private static SimplePreparableReloadListener<String> createReloadListener(Runnable reloadAction) {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected String prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return "FANCYMENU RESOURCE RELOAD LISTENER";
            }

            @Override
            protected void apply(String prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
                reloadAction.run();
            }
        };
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
        Minecraft.getInstance().execute(PacketHandler::sendHandshakeToServer);
    }
}
