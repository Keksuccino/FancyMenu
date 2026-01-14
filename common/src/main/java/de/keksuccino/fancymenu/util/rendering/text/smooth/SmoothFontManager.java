package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SmoothFontManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, SmoothFont> FONT_CACHE = new HashMap<>();
    private static boolean reloadListenerRegistered;

    private SmoothFontManager() {
    }

    @Nullable
    public static SmoothFont getFont(@Nonnull ResourceLocation fontLocation, float baseSize) {
        Objects.requireNonNull(fontLocation);
        registerReloadListener();
        String key = "res:" + fontLocation + ":" + baseSize;
        SmoothFont cached = FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(fontLocation).orElse(null);
            if (resource == null) {
                LOGGER.error("[FANCYMENU] Smooth font not found: {}", fontLocation);
                return null;
            }
            try (InputStream in = resource.open()) {
                SmoothFont created = buildFont(key, in.readAllBytes(), baseSize);
                if (created != null) {
                    FONT_CACHE.put(key, created);
                }
                return created;
            }
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to read smooth font: {}", fontLocation, ex);
            return null;
        }
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

    public static void clear() {
        FONT_CACHE.values().forEach(SmoothFont::close);
        FONT_CACHE.clear();
    }

    @Nullable
    private static SmoothFont buildFont(String key, byte[] fontBytes, float baseSize) {
        try {
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes));
            float sdfRange = Math.max(4.0F, baseSize * 0.125F);
            return new SmoothFont(sanitizeKey(key), baseFont, baseSize, sdfRange);
        } catch (FontFormatException | IOException ex) {
            LOGGER.error("[FANCYMENU] Failed to parse smooth font: {}", key, ex);
            return null;
        }
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

}
