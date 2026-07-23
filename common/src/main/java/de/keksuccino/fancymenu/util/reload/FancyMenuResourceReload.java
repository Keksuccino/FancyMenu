package de.keksuccino.fancymenu.util.reload;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import de.keksuccino.fancymenu.util.resource.ClientResourceIndex;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.preload.ResourcePreLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class FancyMenuResourceReload {

    public static final ResourceLocation FANCYMENU_RELOAD_LISTENER_ID = new ResourceLocation("fancymenu", "fancymenu_reload_listener");

    private static final ClientReloadListenerRegistration<Consumer<ResourceManager>> CLIENT_LISTENER_REGISTRATION = new ClientReloadListenerRegistration<>(() -> FancyMenuResourceReload::reload);

    static {
        MinecraftResourceReloadObserver.addReloadListener(ClientResourceIndex::onMinecraftResourceReload);
    }

    private FancyMenuResourceReload() {
    }

    public static boolean registerClientReloadListener(@NotNull ClientLoader loader, @NotNull Consumer<? super Consumer<ResourceManager>> registrar) {
        return CLIENT_LISTENER_REGISTRATION.register(loader, registrar);
    }

    private static void reload(@NotNull ResourceManager resourceManager) {
        ClientResourceIndex.stage(ClientResourceIndex.prepare(resourceManager));
        ResourceHandlers.reloadAll();
        ResourcePreLoader.preLoadAll(120000);
    }

    public enum ClientLoader {
        FABRIC,
        FORGE
    }
}
