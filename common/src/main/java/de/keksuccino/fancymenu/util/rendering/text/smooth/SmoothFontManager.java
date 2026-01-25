package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class SmoothFontManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, SmoothFont> FONT_CACHE = new HashMap<>();
    private static boolean reloadListenerRegistered;

    private SmoothFontManager() {
    }

    /**
     * Starts a resource-based font builder from a single resource location.
     * <p>
     * The {@code baseSize} sets the scale reference used for all metric corrections
     * (line height override/offset and y-offset). Values you pass later are interpreted
     * in this base-size space and are automatically scaled with the final text size.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * SmoothFont font = SmoothFontManager
     *         .fontBuilder(ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans.ttf"), 32.0F)
     *         .lineHeightOffset(-2.0F)
     *         .yOffset(-3.0F)
     *         .build();
     * }</pre>
     *
     * @param fontLocation font file resource location
     * @param baseSize base font size used for scaling metrics
     * @return builder configured for the provided resource
     */
    @Nonnull
    public static ResourceFontBuilder fontBuilder(@Nonnull ResourceLocation fontLocation, float baseSize) {
        return new ResourceFontBuilder(ResourceSourceType.LIST, baseSize, List.of(fontLocation), null, null);
    }

    /**
     * Starts a resource-based font builder from an explicit list of resource locations.
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * List<ResourceLocation> fonts = List.of(
     *         ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans.ttf"),
     *         ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_jp.ttf")
     * );
     * SmoothFont font = SmoothFontManager
     *         .fontBuilderFromResources(fonts, 32.0F)
     *         .build();
     * }</pre>
     *
     * @param fontLocations font file resource locations
     * @param baseSize base font size used for scaling metrics
     * @return builder configured for the provided resources
     */
    @Nonnull
    public static ResourceFontBuilder fontBuilderFromResources(@Nonnull List<ResourceLocation> fontLocations, float baseSize) {
        return new ResourceFontBuilder(ResourceSourceType.LIST, baseSize, fontLocations, null, null);
    }

    /**
     * Starts a resource-based font builder from a folder that contains .ttf files.
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * SmoothFont font = SmoothFontManager
     *         .fontBuilderFromFolder(ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans"), 32.0F)
     *         .build();
     * }</pre>
     *
     * @param folder folder resource location
     * @param baseSize base font size used for scaling metrics
     * @return builder configured for the provided folder
     */
    @Nonnull
    public static ResourceFontBuilder fontBuilderFromFolder(@Nonnull ResourceLocation folder, float baseSize) {
        return new ResourceFontBuilder(ResourceSourceType.FOLDER, baseSize, null, folder, null);
    }

    /**
     * Starts a resource-based font builder from a folder and filename prefix.
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * SmoothFont font = SmoothFontManager
     *         .fontBuilderFromPrefix(ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans"), "noto_sans_", 32.0F)
     *         .build();
     * }</pre>
     *
     * @param folder folder resource location
     * @param filenamePrefix filename prefix filter
     * @param baseSize base font size used for scaling metrics
     * @return builder configured for the provided prefix
     */
    @Nonnull
    public static ResourceFontBuilder fontBuilderFromPrefix(@Nonnull ResourceLocation folder, @Nonnull String filenamePrefix, float baseSize) {
        return new ResourceFontBuilder(ResourceSourceType.PREFIX, baseSize, null, folder, filenamePrefix);
    }

    /**
     * Starts a path-based font builder from a single font file.
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * SmoothFont font = SmoothFontManager
     *         .fontBuilder(Path.of("config/fancymenu/assets/fonts/myfont.ttf"), 32.0F)
     *         .lineHeightOverride(12.0F)
     *         .build();
     * }</pre>
     *
     * @param fontPath font file path
     * @param baseSize base font size used for scaling metrics
     * @return builder configured for the provided path
     */
    @Nonnull
    public static PathFontBuilder fontBuilder(@Nonnull Path fontPath, float baseSize) {
        return new PathFontBuilder(PathSourceType.LIST, baseSize, List.of(fontPath), null);
    }

    /**
     * Starts a path-based font builder from an explicit list of font file paths.
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * List<Path> fonts = List.of(
     *         Path.of("config/fancymenu/assets/fonts/font_a.ttf"),
     *         Path.of("config/fancymenu/assets/fonts/font_b.ttf")
     * );
     * SmoothFont font = SmoothFontManager
     *         .fontBuilderFromPaths(fonts, 32.0F)
     *         .build();
     * }</pre>
     *
     * @param fontPaths font file paths
     * @param baseSize base font size used for scaling metrics
     * @return builder configured for the provided paths
     */
    @Nonnull
    public static PathFontBuilder fontBuilderFromPaths(@Nonnull List<Path> fontPaths, float baseSize) {
        return new PathFontBuilder(PathSourceType.LIST, baseSize, fontPaths, null);
    }

    /**
     * Starts a path-based font builder from a folder that contains .ttf files.
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * SmoothFont font = SmoothFontManager
     *         .fontBuilderFromFolder(Path.of("config/fancymenu/assets/fonts"), 32.0F)
     *         .build();
     * }</pre>
     *
     * @param folder font folder path
     * @param baseSize base font size used for scaling metrics
     * @return builder configured for the provided folder
     */
    @Nonnull
    public static PathFontBuilder fontBuilderFromFolder(@Nonnull Path folder, float baseSize) {
        return new PathFontBuilder(PathSourceType.FOLDER, baseSize, null, folder);
    }

    public static void clear() {
        FONT_CACHE.values().forEach(SmoothFont::close);
        FONT_CACHE.clear();
    }

    public static void registerReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        reloadListenerRegistered = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderSystem.recordRenderCall(() -> {
                    clear();
                    SmoothFonts.clearCache();
                    SmoothTextRenderer.clearCaches();
                });
            }
        });
    }

    public static final class ResourceFontBuilder {
        private final ResourceSourceType sourceType;
        private final float baseSize;
        @Nullable
        private final List<ResourceLocation> locations;
        @Nullable
        private final ResourceLocation folder;
        @Nullable
        private final String filenamePrefix;
        @Nullable
        private Map<String, List<ResourceLocation>> languageOverrides;
        private float lineHeightOverride = -1.0F;
        private float lineHeightOffset = 0.0F;
        private float yOffset = 0.0F;

        /**
         * Creates a resource-based smooth font builder for the given source type.
         * <p>
         * This is an internal constructor used by the static factory methods on
         * {@link SmoothFontManager}. It captures the source selector and base size
         * so all later configuration values can be applied consistently.
         * </p>
         * <p>
         * Example (via factory method):
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromFolder(ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans"), 32.0F)
         *         .lineHeightOffset(-2.0F)
         *         .yOffset(-3.0F)
         *         .build();
         * }</pre>
         *
         * @param sourceType source selector (list, folder, or prefix)
         * @param baseSize base font size used for scaling metrics
         * @param locations explicit font locations (for list sources)
         * @param folder folder root (for folder/prefix sources)
         * @param filenamePrefix prefix filter (for prefix sources)
         */
        private ResourceFontBuilder(ResourceSourceType sourceType, float baseSize, @Nullable List<ResourceLocation> locations, @Nullable ResourceLocation folder, @Nullable String filenamePrefix) {
            this.sourceType = sourceType;
            this.baseSize = baseSize;
            this.locations = locations;
            this.folder = folder;
            this.filenamePrefix = filenamePrefix;
        }

        /**
         * Sets language-specific font order overrides.
         * <p>
         * The map key must be a Minecraft language code (e.g. {@code ja_jp}, {@code zh_cn}).
         * The list defines a preferred order of font resources to use when rendering for that language.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * Map<String, List<ResourceLocation>> overrides = Map.of(
         *         "ja_jp", List.of(base, jp, sc, tc),
         *         "zh_cn", List.of(base, sc, tc, jp)
         * );
         *
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromResources(List.of(base, jp, sc, tc), 32.0F)
         *         .languageOverrides(overrides)
         *         .build();
         * }</pre>
         *
         * @param languageOverrides map of language code to font order list
         * @return this builder for chaining
         */
        public ResourceFontBuilder languageOverrides(@Nullable Map<String, List<ResourceLocation>> languageOverrides) {
            this.languageOverrides = languageOverrides;
            return this;
        }

        /**
         * Overrides the computed line height (base-size units).
         * <p>
         * Use this to force a specific line height at the base size. The value
         * scales with text size at render time.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilder(ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans.ttf"), 32.0F)
         *         .lineHeightOverride(12.0F)
         *         .build();
         * }</pre>
         *
         * @param lineHeightOverride line height at base size, or &lt;= 0 to disable
         * @return this builder for chaining
         */
        public ResourceFontBuilder lineHeightOverride(float lineHeightOverride) {
            this.lineHeightOverride = lineHeightOverride;
            return this;
        }

        /**
         * Applies an additive line-height correction (base-size units).
         * <p>
         * Positive values increase the line height; negative values decrease it.
         * The correction is applied after any override and scales with text size.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromFolder(ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans"), 32.0F)
         *         .lineHeightOffset(-1.5F)
         *         .build();
         * }</pre>
         *
         * @param lineHeightOffset amount to add to line height (can be negative)
         * @return this builder for chaining
         */
        public ResourceFontBuilder lineHeightOffset(float lineHeightOffset) {
            this.lineHeightOffset = lineHeightOffset;
            return this;
        }

        /**
         * Applies a vertical glyph offset (base-size units).
         * <p>
         * This shifts glyphs within the line box without changing line height.
         * Positive values move glyphs down; negative values move them up.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromFolder(ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans"), 32.0F)
         *         .yOffset(-2.0F)
         *         .build();
         * }</pre>
         *
         * @param yOffset vertical offset for glyphs; positive moves down
         * @return this builder for chaining
         */
        public ResourceFontBuilder yOffset(float yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        /**
         * Builds (and caches) the smooth font instance.
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromResources(List.of(base, jp, sc), 32.0F)
         *         .lineHeightOffset(-1.0F)
         *         .yOffset(-2.0F)
         *         .build();
         * }</pre>
         *
         * @return built font, or null if loading fails
         */
        @Nullable
        public SmoothFont build() {
            return switch (sourceType) {
                case LIST -> buildFromResourceList(locations, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
                case FOLDER -> buildFromResourceFolder(folder, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
                case PREFIX -> buildFromResourcePrefix(folder, filenamePrefix, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
            };
        }
    }

    public static final class PathFontBuilder {
        private final PathSourceType sourceType;
        private final float baseSize;
        @Nullable
        private final List<Path> paths;
        @Nullable
        private final Path folder;
        @Nullable
        private Map<String, List<Path>> languageOverrides;
        private float lineHeightOverride = -1.0F;
        private float lineHeightOffset = 0.0F;
        private float yOffset = 0.0F;

        /**
         * Creates a path-based smooth font builder for the given source type.
         * <p>
         * This is an internal constructor used by the static factory methods on
         * {@link SmoothFontManager}. It captures the source selector and base size
         * so all later configuration values can be applied consistently.
         * </p>
         * <p>
         * Example (via factory method):
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromFolder(Path.of("config/fancymenu/assets/fonts"), 32.0F)
         *         .lineHeightOffset(-1.0F)
         *         .yOffset(-2.0F)
         *         .build();
         * }</pre>
         *
         * @param sourceType source selector (list or folder)
         * @param baseSize base font size used for scaling metrics
         * @param paths explicit font paths (for list sources)
         * @param folder folder root (for folder sources)
         */
        private PathFontBuilder(PathSourceType sourceType, float baseSize, @Nullable List<Path> paths, @Nullable Path folder) {
            this.sourceType = sourceType;
            this.baseSize = baseSize;
            this.paths = paths;
            this.folder = folder;
        }

        /**
         * Sets language-specific font order overrides.
         * <p>
         * The map key must be a Minecraft language code (e.g. {@code ja_jp}, {@code zh_cn}).
         * The list defines a preferred order of font files to use when rendering for that language.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * Map<String, List<Path>> overrides = Map.of(
         *         "ja_jp", List.of(basePath, jpPath, scPath),
         *         "zh_cn", List.of(basePath, scPath, tcPath)
         * );
         *
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromPaths(List.of(basePath, jpPath, scPath, tcPath), 32.0F)
         *         .languageOverrides(overrides)
         *         .build();
         * }</pre>
         *
         * @param languageOverrides map of language code to font order list
         * @return this builder for chaining
         */
        public PathFontBuilder languageOverrides(@Nullable Map<String, List<Path>> languageOverrides) {
            this.languageOverrides = languageOverrides;
            return this;
        }

        /**
         * Overrides the computed line height (base-size units).
         * <p>
         * Use this to force a specific line height at the base size. The value
         * scales with text size at render time.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilder(Path.of("config/fancymenu/assets/fonts/myfont.ttf"), 32.0F)
         *         .lineHeightOverride(12.0F)
         *         .build();
         * }</pre>
         *
         * @param lineHeightOverride line height at base size, or &lt;= 0 to disable
         * @return this builder for chaining
         */
        public PathFontBuilder lineHeightOverride(float lineHeightOverride) {
            this.lineHeightOverride = lineHeightOverride;
            return this;
        }

        /**
         * Applies an additive line-height correction (base-size units).
         * <p>
         * Positive values increase the line height; negative values decrease it.
         * The correction is applied after any override and scales with text size.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromFolder(Path.of("config/fancymenu/assets/fonts"), 32.0F)
         *         .lineHeightOffset(-1.5F)
         *         .build();
         * }</pre>
         *
         * @param lineHeightOffset amount to add to line height (can be negative)
         * @return this builder for chaining
         */
        public PathFontBuilder lineHeightOffset(float lineHeightOffset) {
            this.lineHeightOffset = lineHeightOffset;
            return this;
        }

        /**
         * Applies a vertical glyph offset (base-size units).
         * <p>
         * This shifts glyphs within the line box without changing line height.
         * Positive values move glyphs down; negative values move them up.
         * </p>
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilder(Path.of("config/fancymenu/assets/fonts/myfont.ttf"), 32.0F)
         *         .yOffset(-2.0F)
         *         .build();
         * }</pre>
         *
         * @param yOffset vertical offset for glyphs; positive moves down
         * @return this builder for chaining
         */
        public PathFontBuilder yOffset(float yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        /**
         * Builds (and caches) the smooth font instance.
         * <p>
         * Example:
         * </p>
         * <pre>{@code
         * SmoothFont font = SmoothFontManager
         *         .fontBuilderFromPaths(List.of(basePath, jpPath), 32.0F)
         *         .lineHeightOffset(-1.0F)
         *         .yOffset(-2.0F)
         *         .build();
         * }</pre>
         *
         * @return built font, or null if loading fails
         */
        @Nullable
        public SmoothFont build() {
            return switch (sourceType) {
                case LIST -> buildFromPathList(paths, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
                case FOLDER -> buildFromPathFolder(folder, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
            };
        }
    }

    private enum ResourceSourceType {
        LIST,
        FOLDER,
        PREFIX
    }

    private enum PathSourceType {
        LIST,
        FOLDER
    }

    @Nullable
    private static SmoothFont buildFromResourceList(@Nullable List<ResourceLocation> fontLocations, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides, float lineHeightOverride, float lineHeightOffset, float yOffset) {
        Objects.requireNonNull(fontLocations);
        registerReloadListener();
        List<ResourceLocation> uniqueLocations = dedupe(fontLocations);
        if (uniqueLocations.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth fonts provided.");
            return null;
        }
        float resolvedOverride = sanitizeLineHeightOverride(lineHeightOverride);
        float resolvedOffset = sanitizeLineHeightOffset(lineHeightOffset);
        float resolvedYOffset = sanitizeYOffset(yOffset);
        String key = buildResourceKey("reslist", uniqueLocations, baseSize, languageOverrides, resolvedOverride, resolvedOffset, resolvedYOffset);
        SmoothFont cached = FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        List<byte[]> fontBytes = new ArrayList<>();
        List<ResourceLocation> loadedLocations = new ArrayList<>();

        for (ResourceLocation location : uniqueLocations) {
            Resource resource = resourceManager.getResource(location).orElse(null);
            if (resource == null) {
                LOGGER.error("[FANCYMENU] Smooth font not found: {}", location);
                continue;
            }
            try (InputStream in = resource.open()) {
                fontBytes.add(in.readAllBytes());
                loadedLocations.add(location);
            } catch (IOException ex) {
                LOGGER.error("[FANCYMENU] Failed to read smooth font: {}", location, ex);
            }
        }

        if (fontBytes.isEmpty()) {
            return null;
        }

        Map<String, int[]> languageOrders = buildLanguageOrders(loadedLocations, languageOverrides);
        List<String> sourceLabels = buildLabelsForLocations(loadedLocations);
        SmoothFont created = buildFont(key, fontBytes, baseSize, resolvedOverride, resolvedOffset, resolvedYOffset, languageOrders, sourceLabels);
        if (created != null) {
            FONT_CACHE.put(key, created);
        }
        return created;
    }

    @Nullable
    private static SmoothFont buildFromResourceFolder(@Nullable ResourceLocation folder, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides, float lineHeightOverride, float lineHeightOffset, float yOffset) {
        Objects.requireNonNull(folder);
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folderPath = folder.getPath();
        String prefix = folderPath.endsWith("/") ? folderPath : (folderPath + "/");
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(folderPath, location ->
                location.getNamespace().equals(folder.getNamespace())
                        && location.getPath().startsWith(prefix)
                        && location.getPath().endsWith(".ttf"));
        if (resources.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth fonts found in folder {}.", folder);
            return null;
        }
        List<ResourceLocation> locations = new ArrayList<>(resources.keySet());
        locations.sort(Comparator.comparing(ResourceLocation::toString));
        return buildFromResourceList(locations, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
    }

    @Nullable
    private static SmoothFont buildFromResourcePrefix(@Nullable ResourceLocation folder, @Nullable String filenamePrefix, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides, float lineHeightOverride, float lineHeightOffset, float yOffset) {
        Objects.requireNonNull(folder);
        Objects.requireNonNull(filenamePrefix);
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folderPath = folder.getPath();
        String prefix = folderPath.endsWith("/") ? folderPath : (folderPath + "/");
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(folderPath, location ->
                location.getNamespace().equals(folder.getNamespace())
                        && location.getPath().startsWith(prefix + filenamePrefix)
                        && location.getPath().endsWith(".ttf"));
        if (resources.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth fonts found in {} with prefix '{}'.", folder, filenamePrefix);
            return null;
        }
        List<ResourceLocation> locations = new ArrayList<>(resources.keySet());
        locations.sort(Comparator.comparing(ResourceLocation::toString));
        return buildFromResourceList(locations, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
    }

    @Nullable
    private static SmoothFont buildFromPathList(@Nullable List<Path> fontPaths, float baseSize, @Nullable Map<String, List<Path>> languageOverrides, float lineHeightOverride, float lineHeightOffset, float yOffset) {
        Objects.requireNonNull(fontPaths);
        registerReloadListener();
        List<Path> uniquePaths = dedupePaths(fontPaths);
        if (uniquePaths.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth font paths provided.");
            return null;
        }
        float resolvedOverride = sanitizeLineHeightOverride(lineHeightOverride);
        float resolvedOffset = sanitizeLineHeightOffset(lineHeightOffset);
        float resolvedYOffset = sanitizeYOffset(yOffset);
        String key = buildFileListKey(uniquePaths, baseSize, languageOverrides, resolvedOverride, resolvedOffset, resolvedYOffset);
        SmoothFont cached = FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        List<byte[]> fontBytes = new ArrayList<>();
        List<Path> loadedPaths = new ArrayList<>();
        for (Path path : uniquePaths) {
            try {
                fontBytes.add(Files.readAllBytes(path));
                loadedPaths.add(path);
            } catch (IOException ex) {
                LOGGER.error("[FANCYMENU] Failed to read smooth font from path: {}", path, ex);
            }
        }
        if (fontBytes.isEmpty()) {
            return null;
        }
        Map<String, int[]> languageOrders = buildLanguageOrdersForPaths(loadedPaths, languageOverrides);
        List<String> sourceLabels = buildLabelsForPaths(loadedPaths);
        SmoothFont created = buildFont(key, fontBytes, baseSize, resolvedOverride, resolvedOffset, resolvedYOffset, languageOrders, sourceLabels);
        if (created != null) {
            FONT_CACHE.put(key, created);
        }
        return created;
    }

    @Nullable
    private static SmoothFont buildFromPathFolder(@Nullable Path folder, float baseSize, @Nullable Map<String, List<Path>> languageOverrides, float lineHeightOverride, float lineHeightOffset, float yOffset) {
        Objects.requireNonNull(folder);
        registerReloadListener();
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            LOGGER.error("[FANCYMENU] Smooth font folder not found: {}", folder);
            return null;
        }
        List<Path> locations = listFontFiles(folder);
        if (locations.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth fonts found in folder: {}", folder);
            return null;
        }
        locations.sort(Comparator.comparing(Path::toString));
        return buildFromPathList(locations, baseSize, languageOverrides, lineHeightOverride, lineHeightOffset, yOffset);
    }

    @Nullable
    private static SmoothFont buildFont(String key, List<byte[]> fontBytesList, float baseSize, float lineHeightOverride, float lineHeightOffset, float yOffset, @Nullable Map<String, int[]> languageOrders, @Nullable List<String> sourceLabels) {
        List<Font> fonts = new ArrayList<>();
        List<String> resolvedLabels = new ArrayList<>();
        for (int i = 0; i < fontBytesList.size(); i++) {
            byte[] bytes = fontBytesList.get(i);
            try {
                fonts.add(Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes)));
                resolvedLabels.add(resolveLabel(sourceLabels, i, key));
            } catch (FontFormatException | IOException ex) {
                LOGGER.error("[FANCYMENU] Failed to parse smooth font entry {}: {}", i, key, ex);
            }
        }
        if (fonts.isEmpty()) {
            return null;
        }
        // Keep the base SDF range small; per-LOD scaling is handled in SmoothFont.
        float sdfRange = 1.0F;
        return new SmoothFont(sanitizeKey(key), fonts, baseSize, sdfRange, lineHeightOverride, lineHeightOffset, yOffset, languageOrders, resolvedLabels);
    }

    private static String sanitizeKey(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    private static float sanitizeLineHeightOverride(float lineHeightOverride) {
        return lineHeightOverride > 0.0F ? lineHeightOverride : -1.0F;
    }

    private static float sanitizeLineHeightOffset(float lineHeightOffset) {
        return lineHeightOffset;
    }

    private static float sanitizeYOffset(float yOffset) {
        return yOffset;
    }

    private static List<ResourceLocation> dedupe(List<ResourceLocation> locations) {
        Set<ResourceLocation> unique = new LinkedHashSet<>(locations);
        return new ArrayList<>(unique);
    }

    private static List<Path> dedupePaths(List<Path> paths) {
        Set<Path> unique = new LinkedHashSet<>(paths);
        return new ArrayList<>(unique);
    }

    private static String buildFileListKey(List<Path> locations, float baseSize, @Nullable Map<String, List<Path>> languageOverrides, float lineHeightOverride, float lineHeightOffset, float yOffset) {
        StringBuilder builder = new StringBuilder("filelist:")
                .append(baseSize)
                .append(":")
                .append(Float.floatToIntBits(lineHeightOverride))
                .append(":")
                .append(Float.floatToIntBits(lineHeightOffset))
                .append(":")
                .append(Float.floatToIntBits(yOffset))
                .append(":");
        for (Path location : locations) {
            builder.append(location.toAbsolutePath()).append(';');
        }
        if (languageOverrides != null && !languageOverrides.isEmpty()) {
            builder.append("|lang:");
            Map<String, List<Path>> ordered = new TreeMap<>(languageOverrides);
            for (Map.Entry<String, List<Path>> entry : ordered.entrySet()) {
                builder.append(entry.getKey()).append('=');
                List<Path> overrideList = entry.getValue();
                if (overrideList != null) {
                    for (Path location : overrideList) {
                        builder.append(location.toAbsolutePath()).append(',');
                    }
                }
                builder.append('|');
            }
        }
        return builder.toString();
    }

    private static List<Path> listFontFiles(Path folder) {
        List<Path> results = new ArrayList<>();
        try (var stream = Files.list(folder)) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().toLowerCase(Locale.ROOT).endsWith(".ttf"))
                    .forEach(results::add);
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to list smooth fonts in folder: {}", folder, ex);
        }
        return results;
    }

    private static String buildResourceKey(String prefix, List<ResourceLocation> locations, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides, float lineHeightOverride, float lineHeightOffset, float yOffset) {
        StringBuilder builder = new StringBuilder(prefix)
                .append(":")
                .append(baseSize)
                .append(":")
                .append(Float.floatToIntBits(lineHeightOverride))
                .append(":")
                .append(Float.floatToIntBits(lineHeightOffset))
                .append(":")
                .append(Float.floatToIntBits(yOffset))
                .append(":");
        for (ResourceLocation location : locations) {
            builder.append(location).append(';');
        }
        if (languageOverrides != null && !languageOverrides.isEmpty()) {
            builder.append("|lang:");
            Map<String, List<ResourceLocation>> ordered = new TreeMap<>(languageOverrides);
            for (Map.Entry<String, List<ResourceLocation>> entry : ordered.entrySet()) {
                builder.append(entry.getKey()).append('=');
                List<ResourceLocation> overrideList = entry.getValue();
                if (overrideList != null) {
                    for (ResourceLocation location : overrideList) {
                        builder.append(location).append(',');
                    }
                }
                builder.append('|');
            }
        }
        return builder.toString();
    }

    private static List<String> buildLabelsForLocations(List<ResourceLocation> locations) {
        List<String> labels = new ArrayList<>(locations.size());
        for (ResourceLocation location : locations) {
            labels.add(extractFileName(location.getPath()));
        }
        return labels;
    }

    private static List<String> buildLabelsForPaths(List<Path> paths) {
        List<String> labels = new ArrayList<>(paths.size());
        for (Path path : paths) {
            Path filename = path.getFileName();
            labels.add(filename == null ? "unknown" : filename.toString());
        }
        return labels;
    }

    private static String extractFileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash == -1 ? path : path.substring(slash + 1);
    }

    private static String resolveLabel(@Nullable List<String> sourceLabels, int index, String key) {
        if (sourceLabels != null && index >= 0 && index < sourceLabels.size()) {
            String label = sourceLabels.get(index);
            if (label != null && !label.isBlank()) {
                return label;
            }
        }
        return "unknown";
    }

    @Nullable
    private static Map<String, int[]> buildLanguageOrders(List<ResourceLocation> locations, @Nullable Map<String, List<ResourceLocation>> languageOverrides) {
        if (languageOverrides == null || languageOverrides.isEmpty()) {
            return null;
        }
        Map<ResourceLocation, Integer> lookup = new HashMap<>();
        for (int i = 0; i < locations.size(); i++) {
            lookup.put(locations.get(i), i);
        }
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, List<ResourceLocation>> entry : languageOverrides.entrySet()) {
            List<Integer> order = new ArrayList<>();
            Set<Integer> seen = new LinkedHashSet<>();
            List<ResourceLocation> overrideList = entry.getValue();
            if (overrideList != null) {
                for (ResourceLocation location : overrideList) {
                    Integer index = lookup.get(location);
                    if (index == null) {
                        LOGGER.warn("[FANCYMENU] Language override '{}' references missing font: {}", entry.getKey(), location);
                        continue;
                    }
                    if (seen.add(index)) {
                        order.add(index);
                    }
                }
            }
            for (int i = 0; i < locations.size(); i++) {
                if (seen.add(i)) {
                    order.add(i);
                }
            }
            int[] orderArray = order.stream().mapToInt(Integer::intValue).toArray();
            result.put(entry.getKey().toLowerCase(Locale.ROOT), orderArray);
        }
        return result;
    }

    @Nullable
    private static Map<String, int[]> buildLanguageOrdersForPaths(List<Path> locations, @Nullable Map<String, List<Path>> languageOverrides) {
        if (languageOverrides == null || languageOverrides.isEmpty()) {
            return null;
        }
        Map<Path, Integer> lookup = new HashMap<>();
        for (int i = 0; i < locations.size(); i++) {
            lookup.put(locations.get(i), i);
        }
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, List<Path>> entry : languageOverrides.entrySet()) {
            List<Integer> order = new ArrayList<>();
            Set<Integer> seen = new LinkedHashSet<>();
            List<Path> overrideList = entry.getValue();
            if (overrideList != null) {
                for (Path location : overrideList) {
                    Integer index = lookup.get(location);
                    if (index == null) {
                        LOGGER.warn("[FANCYMENU] Language override '{}' references missing font: {}", entry.getKey(), location);
                        continue;
                    }
                    if (seen.add(index)) {
                        order.add(index);
                    }
                }
            }
            for (int i = 0; i < locations.size(); i++) {
                if (seen.add(i)) {
                    order.add(i);
                }
            }
            int[] orderArray = order.stream().mapToInt(Integer::intValue).toArray();
            result.put(entry.getKey().toLowerCase(Locale.ROOT), orderArray);
        }
        return result;
    }
}
