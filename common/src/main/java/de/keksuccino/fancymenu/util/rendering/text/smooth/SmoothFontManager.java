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

    @Nullable
    public static SmoothFont getFont(@Nonnull ResourceLocation fontLocation, float baseSize) {
        return getFont(List.of(fontLocation), baseSize, null);
    }

    @Nullable
    public static SmoothFont getFont(@Nonnull List<ResourceLocation> fontLocations, float baseSize) {
        return getFont(fontLocations, baseSize, null);
    }

    @Nullable
    public static SmoothFont getFont(@Nonnull List<ResourceLocation> fontLocations, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides) {
        Objects.requireNonNull(fontLocations);
        registerReloadListener();
        List<ResourceLocation> uniqueLocations = dedupe(fontLocations);
        if (uniqueLocations.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth fonts provided.");
            return null;
        }
        String key = buildResourceKey("reslist", uniqueLocations, baseSize, languageOverrides);
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
        SmoothFont created = buildFont(key, fontBytes, baseSize, languageOrders);
        if (created != null) {
            FONT_CACHE.put(key, created);
        }
        return created;
    }

    @Nullable
    public static SmoothFont getFontByPrefix(@Nonnull ResourceLocation folder, @Nonnull String filenamePrefix, float baseSize) {
        return getFontByPrefix(folder, filenamePrefix, baseSize, null);
    }

    @Nullable
    public static SmoothFont getFontByPrefix(@Nonnull ResourceLocation folder, @Nonnull String filenamePrefix, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides) {
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
        return getFont(locations, baseSize, languageOverrides);
    }

    @Nullable
    public static SmoothFont getFontFromFolder(@Nonnull ResourceLocation folder, float baseSize) {
        return getFontFromFolder(folder, baseSize, null);
    }

    @Nullable
    public static SmoothFont getFontFromFolder(@Nonnull ResourceLocation folder, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides) {
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
        return getFont(locations, baseSize, languageOverrides);
    }

    @Nullable
    public static SmoothFont getFontFromFolder(@Nonnull Path folder, float baseSize) {
        return getFontFromFolder(folder, baseSize, null);
    }

    @Nullable
    public static SmoothFont getFontFromFolder(@Nonnull Path folder, float baseSize, @Nullable Map<String, List<Path>> languageOverrides) {
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
        List<Path> uniqueLocations = dedupePaths(locations);
        String key = buildFileFolderKey(folder, uniqueLocations, baseSize, languageOverrides);
        SmoothFont cached = FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        List<byte[]> fontBytes = new ArrayList<>();
        List<Path> loadedLocations = new ArrayList<>();
        for (Path path : uniqueLocations) {
            try {
                fontBytes.add(Files.readAllBytes(path));
                loadedLocations.add(path);
            } catch (IOException ex) {
                LOGGER.error("[FANCYMENU] Failed to read smooth font from path: {}", path, ex);
            }
        }
        if (fontBytes.isEmpty()) {
            return null;
        }
        Map<String, int[]> languageOrders = buildLanguageOrdersForPaths(loadedLocations, languageOverrides);
        SmoothFont created = buildFont(key, fontBytes, baseSize, languageOrders);
        if (created != null) {
            FONT_CACHE.put(key, created);
        }
        return created;
    }

    @Nullable
    public static SmoothFont getFont(@Nonnull Path fontPath, float baseSize) {
        Objects.requireNonNull(fontPath);
        registerReloadListener();
        String key = "file:" + fontPath.toAbsolutePath() + ":" + baseSize;
        SmoothFont cached = FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            byte[] bytes = Files.readAllBytes(fontPath);
            SmoothFont created = buildFont(key, bytes, baseSize);
            if (created != null) {
                FONT_CACHE.put(key, created);
            }
            return created;
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to read smooth font from path: {}", fontPath, ex);
            return null;
        }
    }

    @Nullable
    public static SmoothFont getFontFromPaths(@Nonnull List<Path> fontPaths, float baseSize) {
        Objects.requireNonNull(fontPaths);
        registerReloadListener();
        List<Path> uniquePaths = dedupePaths(fontPaths);
        if (uniquePaths.isEmpty()) {
            LOGGER.error("[FANCYMENU] No smooth font paths provided.");
            return null;
        }
        String key = "filelist:" + baseSize + ":" + uniquePaths;
        SmoothFont cached = FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        List<byte[]> fontBytes = new ArrayList<>();
        for (Path path : uniquePaths) {
            try {
                fontBytes.add(Files.readAllBytes(path));
            } catch (IOException ex) {
                LOGGER.error("[FANCYMENU] Failed to read smooth font from path: {}", path, ex);
            }
        }
        if (fontBytes.isEmpty()) {
            return null;
        }
        SmoothFont created = buildFont(key, fontBytes, baseSize, null);
        if (created != null) {
            FONT_CACHE.put(key, created);
        }
        return created;
    }

    public static void clear() {
        FONT_CACHE.values().forEach(SmoothFont::close);
        FONT_CACHE.clear();
    }

    @Nullable
    private static SmoothFont buildFont(String key, byte[] fontBytes, float baseSize) {
        return buildFont(key, List.of(fontBytes), baseSize, null);
    }

    @Nullable
    private static SmoothFont buildFont(String key, List<byte[]> fontBytesList, float baseSize, @Nullable Map<String, int[]> languageOrders) {
        List<Font> fonts = new ArrayList<>();
        for (int i = 0; i < fontBytesList.size(); i++) {
            byte[] bytes = fontBytesList.get(i);
            try {
                fonts.add(Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes)));
            } catch (FontFormatException | IOException ex) {
                LOGGER.error("[FANCYMENU] Failed to parse smooth font entry {}: {}", i, key, ex);
            }
        }
        if (fonts.isEmpty()) {
            return null;
        }
        // SDF range is 1.0 for all LODs to ensure consistent raster-like behavior in the shader
        float sdfRange = 1.0F;
        return new SmoothFont(sanitizeKey(key), fonts, baseSize, sdfRange, languageOrders);
    }

    private static String sanitizeKey(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    private static void registerReloadListener() {
        if (reloadListenerRegistered) {
            return;
        }
        reloadListenerRegistered = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderSystem.recordRenderCall(SmoothFontManager::clear);
            }
        });
    }

    private static List<ResourceLocation> dedupe(List<ResourceLocation> locations) {
        Set<ResourceLocation> unique = new LinkedHashSet<>(locations);
        return new ArrayList<>(unique);
    }

    private static List<Path> dedupePaths(List<Path> paths) {
        Set<Path> unique = new LinkedHashSet<>(paths);
        return new ArrayList<>(unique);
    }

    private static String buildFileFolderKey(Path folder, List<Path> locations, float baseSize, @Nullable Map<String, List<Path>> languageOverrides) {
        StringBuilder builder = new StringBuilder("filefolder:")
                .append(baseSize)
                .append(":")
                .append(folder.toAbsolutePath())
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

    private static String buildResourceKey(String prefix, List<ResourceLocation> locations, float baseSize, @Nullable Map<String, List<ResourceLocation>> languageOverrides) {
        StringBuilder builder = new StringBuilder(prefix)
                .append(":")
                .append(baseSize)
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
                        LOGGER.warn("[FANCYMENU] Language override '{}' references missing font path: {}", entry.getKey(), location);
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
