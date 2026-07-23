package de.keksuccino.fancymenu.util.reload;

import com.mojang.logging.LogUtils;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import de.keksuccino.fancymenu.util.resource.ClientResourceIndex;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.preload.ResourcePreLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FancyMenuResourceReload {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Long, Runnable> LISTENERS = new HashMap<>();
    private static final ClientReloadListenerRegistration<SimplePreparableReloadListener<ClientResourceIndex.PreparedIndex>> CLIENT_LISTENER_REGISTRATION = new ClientReloadListenerRegistration<>(FancyMenuResourceReload::createMinecraftPreparableReloadListener);
    private static long id = 0;

    public static final Identifier FANCYMENU_RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath("fancymenu", "fancymenu_reload_listener");

    static {

        MinecraftResourceReloadObserver.addReloadListener(ClientResourceIndex::onMinecraftResourceReload);

        registerReloadListener(ResourceHandlers::reloadAll);

        registerReloadListener(() -> ResourcePreLoader.preLoadAll(120000)); //waits for 120 seconds per resource

    }

    public static long registerReloadListener(@NotNull Runnable runnable) {
        id++;
        LISTENERS.put(id, runnable);
        return id;
    }

    public static void removeReloadListener(long listenerId) {
        LISTENERS.remove(listenerId);
    }

    @ApiStatus.Internal
    public static boolean registerClientReloadListener(@NotNull ClientLoader loader, @NotNull Consumer<? super SimplePreparableReloadListener<ClientResourceIndex.PreparedIndex>> registrar) {
        return CLIENT_LISTENER_REGISTRATION.register(loader, registrar);
    }

    @ApiStatus.Internal
    @NotNull
    public static SimplePreparableReloadListener<ClientResourceIndex.PreparedIndex> createMinecraftPreparableReloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected @NotNull ClientResourceIndex.PreparedIndex prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                return ClientResourceIndex.prepare(resourceManager);
            }
            @Override
            protected void apply(@NotNull ClientResourceIndex.PreparedIndex preparedIndex, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                // Stage here, but publish only after ResourceLoadStateTracker confirms the entire reload succeeded. A later listener can still fail after this apply step.
                ClientResourceIndex.stage(preparedIndex);
                LISTENERS.forEach((aLong, runnable) -> runnable.run());
            }
        };
    }

    public enum ClientLoader {
        FABRIC,
        NEOFORGE
    }

}
