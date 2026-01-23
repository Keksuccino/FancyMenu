package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MaterialIcon {

    private final String name;
    private final int codepoint;
    private final Map<Integer, SizeCache> sizeCache = new HashMap<>();

    MaterialIcon(@Nonnull String name, int codepoint) {
        this.name = Objects.requireNonNull(name);
        this.codepoint = codepoint;
    }

    @Nonnull
    public String getName() {
        return this.name;
    }

    public int getCodepoint() {
        return this.codepoint;
    }

    public int getWidth(int sizePx) {
        return ensureLoaded(sizePx).width;
    }

    public int getHeight(int sizePx) {
        return ensureLoaded(sizePx).height;
    }

    public boolean isLoaded(int sizePx) {
        return getSizeCache(MaterialIcons.normalizeSize(sizePx)).loaded;
    }

    public boolean isFailed(int sizePx) {
        return getSizeCache(MaterialIcons.normalizeSize(sizePx)).failed;
    }

    @Nullable
    public ResourceLocation getTextureLocation(int sizePx) {
        return ensureLoaded(sizePx).textureLocation;
    }

    void assign(@Nonnull SizeCache cache, @Nonnull ResourceLocation location, int width, int height) {
        cache.textureLocation = location;
        cache.width = width;
        cache.height = height;
        cache.loaded = true;
        cache.failed = false;
    }

    void markFailed(@Nonnull SizeCache cache) {
        cache.failed = true;
    }

    void clearCache(@Nonnull Consumer<ResourceLocation> releaser) {
        synchronized (sizeCache) {
            for (SizeCache cache : sizeCache.values()) {
                if (cache.textureLocation != null) {
                    releaser.accept(cache.textureLocation);
                }
                cache.reset();
            }
        }
    }

    @Nonnull
    SizeCache getSizeCache(int sizePx) {
        synchronized (sizeCache) {
            return sizeCache.computeIfAbsent(sizePx, SizeCache::new);
        }
    }

    private SizeCache ensureLoaded(int sizePx) {
        int normalizedSize = MaterialIcons.normalizeSize(sizePx);
        SizeCache cache = getSizeCache(normalizedSize);
        if (cache.loaded || cache.failed) {
            return cache;
        }
        synchronized (cache) {
            if (cache.loaded || cache.failed) {
                return cache;
            }
            MaterialIcons.loadIcon(this, cache, normalizedSize);
        }
        return cache;
    }

    static final class SizeCache {
        final int sizePx;
        volatile int width;
        volatile int height;
        volatile boolean loaded;
        volatile boolean failed;
        volatile ResourceLocation textureLocation;

        SizeCache(int sizePx) {
            this.sizePx = sizePx;
        }

        void reset() {
            this.textureLocation = null;
            this.width = 0;
            this.height = 0;
            this.loaded = false;
            this.failed = false;
        }
    }

}
