package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

/** Publishes completed Minecraft resource reloads so per-element parent caches never outlive their ResourceManager data. */
final class JsonModelResourceReloadGeneration {

    private static final AtomicLong GENERATION = new AtomicLong();

    static {
        MinecraftResourceReloadObserver.addReloadListener(JsonModelResourceReloadGeneration::onReload);
    }

    private JsonModelResourceReloadGeneration() {
    }

    static long current() {
        return GENERATION.get();
    }

    static void onReload(@NotNull MinecraftResourceReloadObserver.ReloadAction action) {
        if (action == MinecraftResourceReloadObserver.ReloadAction.FINISHED) GENERATION.incrementAndGet();
    }

}
