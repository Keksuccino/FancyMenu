package de.keksuccino.fancymenu.util.reload;

import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.preload.ResourcePreLoader;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class FancyMenuResourceReload {

    public static final ResourceLocation FANCYMENU_RELOAD_LISTENER_ID = new ResourceLocation("fancymenu", "fancymenu_reload_listener");

    private static final ClientReloadListenerRegistration<Runnable> CLIENT_LISTENER_REGISTRATION = new ClientReloadListenerRegistration<>(() -> FancyMenuResourceReload::reload);

    private FancyMenuResourceReload() {
    }

    public static boolean registerClientReloadListener(@NotNull ClientLoader loader, @NotNull Consumer<? super Runnable> registrar) {
        return CLIENT_LISTENER_REGISTRATION.register(loader, registrar);
    }

    private static void reload() {
        ResourceHandlers.reloadAll();
        ResourcePreLoader.preLoadAll(120000);
    }

    public enum ClientLoader {
        FABRIC,
        FORGE
    }
}
