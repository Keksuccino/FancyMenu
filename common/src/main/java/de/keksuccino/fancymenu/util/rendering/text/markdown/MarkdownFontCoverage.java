package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows which characters a font of Minecraft has glyphs for. Minecraft draws a box for every character a font has no glyph for,
 * so text that gets another font has to keep its own font for those characters.<br><br>
 *
 * The characters get read from the definition files of the font, because that works the same way in every Minecraft version, which asking
 * the font itself does not. Only bitmap and space providers list their characters, so a font with any other provider, like a TrueType
 * font or a reference to another font, counts as having every character.
 */
final class MarkdownFontCoverage {

    private static final Logger LOGGER = LogManager.getLogger();
    /** The coverage of a font that has every character. */
    static final MarkdownFontCoverage EVERYTHING = new MarkdownFontCoverage(null);
    private static final String DEFAULT_NAMESPACE_PREFIX = "minecraft:";
    /** The fonts that got read since the last resource reload. Only the render thread uses it. */
    private static final Map<ResourceLocation, MarkdownFontCoverage> LOADED = new HashMap<>();
    /** Counts finished Minecraft resource reloads, which can replace every font. */
    private static volatile int resourceReloads = 0;
    private static int loadedResourceReloads = 0;

    /** The characters of the font. NULL if the font has every character. */
    @Nullable
    private final IntSet codePoints;

    static {
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.FINISHED) resourceReloads++;
        });
    }

    private MarkdownFontCoverage(@Nullable IntSet codePoints) {
        this.codePoints = codePoints;
    }

    /**
     * Returns the characters of the given font. Has to get called on the render thread.
     */
    @NotNull
    static MarkdownFontCoverage of(@NotNull ResourceLocation font) {
        int reloads = resourceReloads;
        if (reloads != loadedResourceReloads) {
            LOADED.clear();
            loadedResourceReloads = reloads;
        }
        MarkdownFontCoverage coverage = LOADED.get(font);
        if (coverage == null) {
            coverage = read(font);
            LOADED.put(font, coverage);
        }
        return coverage;
    }

    @NotNull
    private static MarkdownFontCoverage read(@NotNull ResourceLocation font) {
        List<JsonObject> definitions = new ArrayList<>();
        try {
            ResourceLocation file = new ResourceLocation(font.getNamespace(), "font/" + font.getPath() + ".json");
            // Every resource pack can add providers to a font
            for (Resource resource : Minecraft.getInstance().getResourceManager().getResourceStack(file)) {
                try (Reader reader = resource.openAsReader()) {
                    definitions.add(JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read the definition of the font " + font, ex);
            return EVERYTHING;
        }
        return fromDefinitions(definitions);
    }

    /**
     * Returns the characters of a font with the given definition files. A font without definition files has no characters.
     */
    @NotNull
    static MarkdownFontCoverage fromDefinitions(@NotNull List<JsonObject> definitions) {
        IntSet codePoints = new IntOpenHashSet();
        try {
            for (JsonObject definition : definitions) {
                if (!definition.has("providers")) continue;
                for (JsonElement element : definition.getAsJsonArray("providers")) {
                    JsonObject provider = element.getAsJsonObject();
                    String type = provider.get("type").getAsString();
                    if (type.startsWith(DEFAULT_NAMESPACE_PREFIX)) type = type.substring(DEFAULT_NAMESPACE_PREFIX.length());
                    if (type.equals("bitmap")) {
                        for (JsonElement row : provider.getAsJsonArray("chars")) {
                            addCodePoints(codePoints, row.getAsString());
                        }
                    } else if (type.equals("space")) {
                        for (String character : provider.getAsJsonObject("advances").keySet()) {
                            addCodePoints(codePoints, character);
                        }
                    } else {
                        return EVERYTHING;
                    }
                }
            }
        } catch (RuntimeException ex) {
            // Minecraft decides what a definition it can't read makes of the font
            return EVERYTHING;
        }
        return new MarkdownFontCoverage(codePoints);
    }

    private static void addCodePoints(@NotNull IntSet codePoints, @NotNull String characters) {
        for (int index = 0; index < characters.length(); ) {
            int codePoint = characters.codePointAt(index);
            index += Character.charCount(codePoint);
            // Bitmap fonts mark the empty slots of their texture with this character
            if (codePoint != 0) codePoints.add(codePoint);
        }
    }

    boolean has(int codePoint) {
        return (this.codePoints == null) || this.codePoints.contains(codePoint);
    }

    boolean hasAll(@NotNull String text) {
        if (this.codePoints == null) return true;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (!this.codePoints.contains(codePoint)) return false;
        }
        return true;
    }

}
