package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FancyMenuFabricClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll() {

        if (FancyMenuResourceReload.registerClientReloadListener(FancyMenuResourceReload.ClientLoader.FABRIC, FancyMenuFabricClientEvents::registerReloadListener)) {
            LOGGER.info("[FANCYMENU] Registered FancyMenu's resource reload listener via Fabric API.");
        }

        registerScreenEvents();

        ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
            Connection connection = clientPacketListener.getConnection();
            PacketHandler.onClientConnected(connection);
            minecraft.execute(() -> PacketHandler.sendHandshakeToServer(connection));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPacketListener, minecraft) -> PacketHandler.onClientDisconnected(clientPacketListener.getConnection()));

    }

    private static void registerReloadListener(Runnable reloadAction) {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return FancyMenuResourceReload.FANCYMENU_RELOAD_LISTENER_ID;
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                reloadAction.run();
            }
        });
    }

    private static void registerScreenEvents() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {

            ScreenKeyboardEvents.afterKeyPress(screen).register((screen1, key, scancode, modifiers) -> {

                ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(screen1, key, scancode, modifiers);
                EventHandler.INSTANCE.postEvent(event);

                if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) o.keyPressed(key, scancode, modifiers);

            });

            ScreenKeyboardEvents.afterKeyRelease(screen).register((screen1, key, scancode, modifiers) -> {

                ScreenKeyReleasedEvent event = new ScreenKeyReleasedEvent(screen1, key, scancode, modifiers);
                EventHandler.INSTANCE.postEvent(event);

            });

        });
    }

}
