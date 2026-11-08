package de.keksuccino.fancymenu.util.rendering.text.smooth;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Builds and caches {@link SmoothFont}s from font files in resources or on disk. Cached fonts get closed on every resource reload.
 * <p>
 * Example:
 * </p>
 * <pre>{@code
 * SmoothFont font = SmoothFontManager
 *         .fontBuilderFromResources(List.of(baseFont, japaneseFont, emojiFont), 32.0F)
 *         .emojiFonts(List.of(emojiFont))
 *         .languageOverrides(Map.of("ja_jp", List.of(baseFont, japaneseFont)))
 *         .lineHeightOffset(-2.0F)
 *         .yOffset(-3.0F)
 *         .build();
 * }</pre>
 */
public final class SmoothFontManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, SmoothFont> FONT_CACHE = new HashMap<>();
    private static boolean reloadListenerRegistered;

    private SmoothFontManager() {
    }

    /**
     * Starts a builder for a single font file resource.
     *
     * @param baseSize size the metric corrections of the builder (line height, y offset) are defined at; they scale with the text size
     */
    @NotNull
    public static Builder<ResourceLocation> fontBuilder(@NotNull ResourceLocation fontLocation, float baseSize) {
        return new Builder<>(SourceKind.RESOURCE, baseSize, () -> List.of(fontLocation));
    }

    /**
     * Starts a builder for a list of font file resources. The first file is the primary font, the others are fallbacks in order.
     */
    @NotNull
    public static Builder<ResourceLocation> fontBuilderFromResources(@NotNull List<ResourceLocation> fontLocations, float baseSize) {
        return new Builder<>(SourceKind.RESOURCE, baseSize, () -> fontLocations);
    }

    /**
     * Starts a builder for all {@code .ttf} resources in a folder, ordered by file name.
     */
    @NotNull
    public static Builder<ResourceLocation> fontBuilderFromFolder(@NotNull ResourceLocation folder, float baseSize) {
        return new Builder<>(SourceKind.RESOURCE, baseSize, () -> listFontResources(folder));
    }

    /**
     * Starts a builder for a single font file on disk.
     */
    @NotNull
    public static Builder<Path> fontBuilder(@NotNull Path fontPath, float baseSize) {
        return new Builder<>(SourceKind.PATH, baseSize, () -> List.of(fontPath));
    }

    /**
     * Starts a builder for a list of font files on disk. The first file is the primary font, the others are fallbacks in order.
     */
    @NotNull
    public static Builder<Path> fontBuilderFromPaths(@NotNull List<Path> fontPaths, float baseSize) {
        return new Builder<>(SourceKind.PATH, baseSize, () -> fontPaths);
    }

    /**
     * Starts a builder for all {@code .ttf} files in a folder on disk, ordered by file name.
     */
    @NotNull
    public static Builder<Path> fontBuilderFromFolder(@NotNull Path folder, float baseSize) {
        return new Builder<>(SourceKind.PATH, baseSize, () -> listFontFiles(folder));
    }

    /**
     * Closes all cached fonts and frees their GPU resources.
     */
    public static void clear() {
        FONT_CACHE.values().forEach(SmoothFont::close);
        FONT_CACHE.clear();
    }

    static void clearLayoutCaches() {
        FONT_CACHE.values().forEach(SmoothFont::clearLayouts);
    }

    public static void registerReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        reloadListenerRegistered = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                Minecraft.getInstance().execute(() -> {
                    clear();
                    SmoothFonts.clearCache();
                });
            }
        });
    }

    @NotNull
    private static String activeLanguageCode() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null || minecraft.options.languageCode == null) {
            return "";
        }
        return minecraft.options.languageCode.toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static List<ResourceLocation> listFontResources(@NotNull ResourceLocation folder) {
        String folderPath = folder.getPath();
        String prefix = folderPath.endsWith("/") ? folderPath : (folderPath + "/");
        Map<ResourceLocation, Resource> resources = Minecraft.getInstance().getResourceManager().listResources(folderPath, location ->
                location.getNamespace().equals(folder.getNamespace()) && location.getPath().startsWith(prefix) && location.getPath().endsWith(".ttf"));
        if (resources.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth fonts found in folder {}.", folder);
            return null;
        }
        List<ResourceLocation> locations = new ArrayList<>(resources.keySet());
        locations.sort(Comparator.comparing(ResourceLocation::toString));
        return locations;
    }

    @Nullable
    private static List<Path> listFontFiles(@NotNull Path folder) {
        if (!Files.isDirectory(folder)) {
            LOGGER.error("[FANCYMENU] Smooth font folder not found: {}", folder);
            return null;
        }
        try (Stream<Path> stream = Files.list(folder)) {
            List<Path> files = new ArrayList<>(stream.filter(path -> Files.isRegularFile(path) && path.toString().toLowerCase(Locale.ROOT).endsWith(".ttf")).toList());
            files.sort(Comparator.comparing(Path::toString));
            if (files.isEmpty()) {
                LOGGER.error("[FANCYMENU] No smooth fonts found in folder: {}", folder);
                return null;
            }
            return files;
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to list smooth fonts in folder: {}", folder, ex);
            return null;
        }
    }

    @NotNull
    private static String fileName(@NotNull String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (slash == -1) ? path : path.substring(slash + 1);
    }

    /**
     * How font files of one kind (resources or files on disk) get identified, named and read.
     */
    private interface SourceKind<S> {

        SourceKind<ResourceLocation> RESOURCE = new SourceKind<>() {
            @Override
            public @NotNull String id(@NotNull ResourceLocation file) {
                return "res:" + file;
            }
            @Override
            public @NotNull String label(@NotNull ResourceLocation file) {
                return fileName(file.getPath());
            }
            @Override
            public @NotNull SmoothFontSource.ByteLoader loader(@NotNull ResourceLocation file) {
                return () -> {
                    Resource resource = Minecraft.getInstance().getResourceManager().getResource(file).orElseThrow(() -> new IOException("Smooth font not found: " + file));
                    try (InputStream in = resource.open()) {
                        return in.readAllBytes();
                    }
                };
            }
        };

        SourceKind<Path> PATH = new SourceKind<>() {
            @Override
            public @NotNull String id(@NotNull Path file) {
                return "file:" + file.toAbsolutePath();
            }
            @Override
            public @NotNull String label(@NotNull Path file) {
                return fileName(file.toString());
            }
            @Override
            public @NotNull SmoothFontSource.ByteLoader loader(@NotNull Path file) {
                return () -> Files.readAllBytes(file);
            }
        };

        @NotNull String id(@NotNull S file);

        @NotNull String label(@NotNull S file);

        @NotNull SmoothFontSource.ByteLoader loader(@NotNull S file);

    }

    /**
     * Configures and builds a cached {@link SmoothFont}. All size values are in base-size units and scale with the text size.
     *
     * @param <S> how font files are referenced ({@link ResourceLocation} for resources, {@link Path} for files on disk)
     */
    public static final class Builder<S> {

        private final SourceKind<S> kind;
        private final float baseSize;
        private final Supplier<List<S>> files;
        @Nullable
        private Map<String, List<S>> languageOverrides;
        private final Set<S> emojiFonts = new HashSet<>();
        private float colorGlyphScale = 1.0F;
        private float lineHeightOverride = -1.0F;
        private float lineHeightOffset = 0.0F;
        private float yOffset = 0.0F;
        private int atlasPageSize = SmoothGlyphAtlas.DEFAULT_PAGE_SIZE;

        private Builder(@NotNull SourceKind<S> kind, float baseSize, @NotNull Supplier<List<S>> files) {
            this.kind = kind;
            this.baseSize = baseSize;
            this.files = files;
        }

        /**
         * Sets per-language search orders. Keys are Minecraft language codes (like {@code ja_jp}); files missing from a list
         * get searched after the listed ones, in their default order. The second file of a language's list gets loaded ahead of
         * time while that language is active.
         */
        @NotNull
        public Builder<S> languageOverrides(@Nullable Map<String, List<S>> languageOverrides) {
            this.languageOverrides = languageOverrides;
            return this;
        }

        /**
         * Marks font files as emoji fonts. Emoji prefer these files, while all other characters only use them as a last resort,
         * so loading text in other scripts never loads an emoji font (and the other way around).
         */
        @NotNull
        public Builder<S> emojiFonts(@NotNull Collection<S> emojiFonts) {
            this.emojiFonts.addAll(emojiFonts);
            return this;
        }

        /**
         * Size of color glyphs (emoji) relative to the text size.
         */
        @NotNull
        public Builder<S> colorGlyphScale(float colorGlyphScale) {
            this.colorGlyphScale = colorGlyphScale;
            return this;
        }

        /**
         * Overrides the line height of the primary font, or disables the override with a value {@code <= 0}.
         */
        @NotNull
        public Builder<S> lineHeightOverride(float lineHeightOverride) {
            this.lineHeightOverride = lineHeightOverride;
            return this;
        }

        /**
         * Adds to the line height (after the override); negative values make lines tighter.
         */
        @NotNull
        public Builder<S> lineHeightOffset(float lineHeightOffset) {
            this.lineHeightOffset = lineHeightOffset;
            return this;
        }

        /**
         * Moves glyphs within their line box without changing the line height; positive values move them down.
         */
        @NotNull
        public Builder<S> yOffset(float yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        /**
         * Edge length of the font's glyph atlas pages in pixels ({@code 512..4096}; smaller pages couldn't hold the largest glyph
         * bitmaps). Fonts that only draw a few small glyphs at a time, like icon fonts, save GPU memory with smaller pages.
         */
        @NotNull
        public Builder<S> atlasPageSize(int atlasPageSize) {
            this.atlasPageSize = Math.max(512, Math.min(4096, atlasPageSize));
            return this;
        }

        /**
         * Builds the font, or returns the cached instance built from the same configuration.
         *
         * @return the font, or {@code null} if there are no font files or the primary file can't be loaded
         */
        @Nullable
        public SmoothFont build() {
            registerReloadListener();
            List<S> listed = this.files.get();
            if (listed == null || listed.isEmpty()) {
                LOGGER.error("[FANCYMENU] No smooth fonts provided.");
                return null;
            }
            List<S> files = new ArrayList<>(new LinkedHashSet<>(listed));
            float override = (this.lineHeightOverride > 0.0F) ? this.lineHeightOverride : -1.0F;
            String key = this.cacheKey(files, override);
            SmoothFont cached = FONT_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            List<SmoothFontSource> sources = new ArrayList<>(files.size());
            for (int i = 0; i < files.size(); i++) {
                S file = files.get(i);
                sources.add(new SmoothFontSource(this.kind.label(file), i, this.emojiFonts.contains(file), this.kind.loader(file)));
            }
            String debugName = this.kind.label(files.get(0)).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_") + "_" + Integer.toHexString(key.hashCode());
            try {
                SmoothFont font = new SmoothFont(debugName, sources, this.baseSize, override, this.lineHeightOffset, this.yOffset, this.languageOrders(files), this.colorGlyphScale, SmoothFontManager::activeLanguageCode, this.atlasPageSize);
                FONT_CACHE.put(key, font);
                return font;
            } catch (IllegalStateException ex) {
                LOGGER.error("[FANCYMENU] Failed to build smooth font: {}", debugName, ex);
                return null;
            }
        }

        @Nullable
        private Map<String, int[]> languageOrders(@NotNull List<S> files) {
            if (this.languageOverrides == null || this.languageOverrides.isEmpty()) {
                return null;
            }
            Map<S, Integer> indices = new HashMap<>();
            for (int i = 0; i < files.size(); i++) {
                indices.put(files.get(i), i);
            }
            Map<String, int[]> result = new HashMap<>();
            for (Map.Entry<String, List<S>> entry : this.languageOverrides.entrySet()) {
                LinkedHashSet<Integer> order = new LinkedHashSet<>();
                for (S file : entry.getValue()) {
                    Integer index = indices.get(file);
                    if (index == null) {
                        LOGGER.warn("[FANCYMENU] Language override '{}' references a font that is not part of the font: {}", entry.getKey(), this.kind.label(file));
                    } else {
                        order.add(index);
                    }
                }
                for (int i = 0; i < files.size(); i++) {
                    order.add(i);
                }
                result.put(entry.getKey().toLowerCase(Locale.ROOT), order.stream().mapToInt(Integer::intValue).toArray());
            }
            return result;
        }

        @NotNull
        private String cacheKey(@NotNull List<S> files, float override) {
            StringBuilder builder = new StringBuilder()
                    .append(this.baseSize).append(':')
                    .append(Float.floatToIntBits(override)).append(':')
                    .append(Float.floatToIntBits(this.lineHeightOffset)).append(':')
                    .append(Float.floatToIntBits(this.yOffset)).append(':')
                    .append(Float.floatToIntBits(this.colorGlyphScale)).append(':')
                    .append(this.atlasPageSize).append(':');
            for (S file : files) {
                builder.append(this.kind.id(file)).append(this.emojiFonts.contains(file) ? "!;" : ";");
            }
            if (this.languageOverrides != null) {
                for (Map.Entry<String, List<S>> entry : new TreeMap<>(this.languageOverrides).entrySet()) {
                    builder.append('|').append(entry.getKey()).append('=');
                    for (S file : entry.getValue()) {
                        builder.append(this.kind.id(file)).append(',');
                    }
                }
            }
            return builder.toString();
        }

    }

}
