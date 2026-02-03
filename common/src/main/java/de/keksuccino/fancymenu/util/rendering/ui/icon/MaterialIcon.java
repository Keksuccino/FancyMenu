package de.keksuccino.fancymenu.util.rendering.ui.icon;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MaterialIcon {

    private static final int BASELINE_ICON_DP = 24;
    private static final float BASELINE_QUALITY_SCALE = 2.0F;
    private static final float GLYPH_COVERAGE_COMPENSATION = 1.2F;
    private static final int DEFAULT_FALLBACK_TEXTURE_SIZE = 96;
    private static final float MAX_MINIFICATION_RATIO_DEFAULT = 2.75F;
    private static final float MAX_MINIFICATION_RATIO_HIGH_SCALE = 2.4F;

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

    /**
     * Resolves a texture location for UI rendering that balances crispness and scaling quality.
     * The size is derived from the render area and FancyMenu's logical UI scale.
     */
    @Nullable
    public ResourceLocation getTextureLocationForUI(float renderWidth, float renderHeight) {
        return getTextureLocation(calculateBestTextureSizeForUI(renderWidth, renderHeight));
    }

    /**
     * Resolves a texture location for rendering that balances crispness and scaling quality.
     * The size is derived from the render area and rendering scale.
     */
    @Nullable
    public ResourceLocation getTextureLocation(float renderWidth, float renderHeight, float renderScale) {
        return getTextureLocation(calculateBestTextureSize(renderWidth, renderHeight, renderScale));
    }

    /**
     * Calculates the best texture size (in pixels) for rendering based on the render area and render scale.
     */
    public int calculateBestTextureSizeForUI(float renderWidth, float renderHeight) {
        return this.calculateBestTextureSize(renderWidth, renderHeight, UIBase.getUIScale());
    }

    /**
     * Calculates the best texture size (in pixels) for UI rendering based on the render area and UI scale.
     */
    public int calculateBestTextureSize(float renderWidth, float renderHeight, float renderScale) {
        float maxRenderSize = Math.max(renderWidth, renderHeight);
        if (!Float.isFinite(maxRenderSize) || maxRenderSize <= 0.0F) {
            return DEFAULT_FALLBACK_TEXTURE_SIZE;
        }

        if (!Float.isFinite(renderScale) || renderScale <= 0.0F) {
            renderScale = 1.0F;
        }

        float densityBucket = resolveDensityBucket(renderScale);
        float renderPixelSize = maxRenderSize * renderScale;
        float qualityScale = resolveQualityScale(renderPixelSize);
        float minTextureSize = BASELINE_ICON_DP * densityBucket * qualityScale;
        float oversample = resolveOversampleFactor(renderPixelSize);
        float desiredSize = renderPixelSize * oversample * GLYPH_COVERAGE_COMPENSATION;

        float targetSize = Math.max(minTextureSize, desiredSize);
        float maxMinification = resolveMaxMinificationRatio(renderScale);
        float maxAllowedSize = renderPixelSize * maxMinification;
        if (Float.isFinite(maxAllowedSize) && maxAllowedSize > 0.0F) {
            targetSize = Math.min(targetSize, maxAllowedSize);
        }
        int resolvedSize = quantizeTextureSize(targetSize);
        return MaterialIcons.normalizeSize(resolvedSize);
    }

    private static float resolveDensityBucket(float uiScale) {
        if (uiScale <= 1.0F) {
            return 1.0F;
        }
        float bucket = (float) Math.ceil(uiScale * 2.0F) / 2.0F;
        return Math.min(4.0F, Math.max(1.0F, bucket));
    }

    private static float resolveOversampleFactor(float logicalSize) {
        if (logicalSize <= 12.0F) {
            return 2.2F;
        }
        if (logicalSize <= 16.0F) {
            return 2.0F;
        }
        if (logicalSize <= 20.0F) {
            return 1.8F;
        }
        if (logicalSize <= 24.0F) {
            return 1.65F;
        }
        if (logicalSize <= 32.0F) {
            return 1.45F;
        }
        if (logicalSize <= 48.0F) {
            return 1.3F;
        }
        if (logicalSize <= 64.0F) {
            return 1.2F;
        }
        if (logicalSize <= 96.0F) {
            return 1.12F;
        }
        return 1.08F;
    }

    private static float resolveQualityScale(float renderPixelSize) {
        if (renderPixelSize <= 18.0F) {
            return 1.0F;
        }
        if (renderPixelSize <= 24.0F) {
            return 1.2F;
        }
        if (renderPixelSize <= 32.0F) {
            return 1.5F;
        }
        if (renderPixelSize <= 48.0F) {
            return 1.75F;
        }
        return BASELINE_QUALITY_SCALE;
    }

    private static float resolveMaxMinificationRatio(float uiScale) {
        if (uiScale >= 3.0F) {
            return MAX_MINIFICATION_RATIO_HIGH_SCALE;
        }
        return MAX_MINIFICATION_RATIO_DEFAULT;
    }

    private static int quantizeTextureSize(float desiredSize) {
        int size = Math.max(1, (int) Math.ceil(desiredSize));
        int step = resolveQuantizationStep(size);
        return roundUpToStep(size, step);
    }

    private static int resolveQuantizationStep(int size) {
        if (size <= 24) {
            return 1;
        }
        if (size <= 64) {
            return 2;
        }
        if (size <= 160) {
            return 4;
        }
        if (size <= 320) {
            return 8;
        }
        return 16;
    }

    private static int roundUpToStep(int value, int step) {
        return ((value + step - 1) / step) * step;
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
