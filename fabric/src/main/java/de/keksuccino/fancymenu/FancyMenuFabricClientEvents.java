package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.input.ScreenKeyEventDispatcher;
import de.keksuccino.fancymenu.util.reload.FancyMenuResourceReload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

    private static <T> void registerReloadListener(SimplePreparableReloadListener<T> listener) {
        // Fabric API 0.92 only accepts identifiable listeners, so retain its ID contract while delegating the actual reload lifecycle unchanged.
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return FancyMenuResourceReload.FANCYMENU_RELOAD_LISTENER_ID;
            }

            @Override
            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return listener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
            }
        });
    }

    private static void registerScreenEvents() {
        // Fabric API 0.92 invokes these callbacks after the screen call even when it returns handled. Its allow-event
        // cancellation skips both the screen call and this callback, matching the Forge wrapper's ordering.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register(ScreenKeyEventDispatcher::postKeyPressed);
            ScreenKeyboardEvents.afterKeyRelease(screen).register(ScreenKeyEventDispatcher::postKeyReleased);
        });
    }
}
