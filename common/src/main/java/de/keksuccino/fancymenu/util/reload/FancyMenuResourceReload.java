package de.keksuccino.fancymenu.util.reload;

import com.mojang.logging.LogUtils;
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

public class FancyMenuResourceReload {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DUMMY_RETURN = "FANCYMENU RESOURCE RELOAD LISTENER";
    private static final Map<Long, Runnable> LISTENERS = new HashMap<>();
    private static long id = 0;

    public static final Identifier FANCYMENU_RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath("fancymenu", "fancymenu_reload_listener");

    static {

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
    @NotNull
    public static SimplePreparableReloadListener<String> createMinecraftPreparableReloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected @NotNull String prepare(@NotNull ResourceManager var1, @NotNull ProfilerFiller var2) {
                return DUMMY_RETURN;
            }
            @Override
            protected void apply(@NotNull String prepareReturnValue, @NotNull ResourceManager var2, @NotNull ProfilerFiller var3) {
                LISTENERS.forEach((aLong, runnable) -> runnable.run());
            }
        };
    }

}
