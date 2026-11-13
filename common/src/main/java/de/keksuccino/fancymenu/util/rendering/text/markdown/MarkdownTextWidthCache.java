package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Remembers the text widths a {@link MarkdownRenderer} measured for its fragments, so parsing a text again only measures the words that are new.
 * That matters for texts with placeholders, which get parsed again whenever a placeholder value changes while most of their words stay the same.<br><br>
 *
 * The cache only works between {@link #beginParse()} and {@link #endParse()}. It keeps the widths of exactly one parse: a width that the following
 * parse didn't ask for gets dropped at the end of that parse, so texts that keep changing can't make the cache grow.<br><br>
 *
 * A cached width is only correct while text gets measured the same way, so every parse starts with checking that:
 * <ul>
 *     <li>Minecraft resource reloads can replace fonts, so each one makes the cache start over.</li>
 *     <li>Everything else that changes text measuring without a reload (the renderer switching between the Minecraft font and the UI font,
 *     a different UI text size, Minecraft's font options) changes the width and height of normal text, so a probe text gets measured
 *     and the cache starts over when its size differs from the last parse.</li>
 * </ul>
 */
final class MarkdownTextWidthCache {

    private static final String PROBE_TEXT = "The quick brown fox jumps over the lazy dog, 0123456789!";
    /** Counts finished Minecraft resource reloads. */
    private static volatile int resourceReloads = 0;

    private final MarkdownRenderer renderer;
    private Object2FloatOpenHashMap<Key> widths = new Object2FloatOpenHashMap<>();
    private Object2FloatOpenHashMap<Key> lastParseWidths = new Object2FloatOpenHashMap<>();
    private boolean parsing = false;
    private int measuredResourceReloads = -1;
    private float measuredProbeWidth = Float.NaN;
    private float measuredProbeHeight = Float.NaN;

    static {
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.FINISHED) resourceReloads++;
        });
    }

    MarkdownTextWidthCache(@NotNull MarkdownRenderer renderer) {
        this.renderer = renderer;
        this.widths.defaultReturnValue(Float.NaN);
        this.lastParseWidths.defaultReturnValue(Float.NaN);
    }

    void beginParse() {
        // The widths of the last parse become the ones this parse can take over
        Object2FloatOpenHashMap<Key> reused = this.lastParseWidths;
        this.lastParseWidths = this.widths;
        this.widths = reused;
        this.widths.clear();
        int reloads = resourceReloads;
        float probeWidth = this.renderer.textFont.width(PROBE_TEXT);
        float probeHeight = this.renderer.textFont.lineHeight();
        if ((reloads != this.measuredResourceReloads) || (probeWidth != this.measuredProbeWidth) || (probeHeight != this.measuredProbeHeight)) {
            this.lastParseWidths.clear();
            this.measuredResourceReloads = reloads;
            this.measuredProbeWidth = probeWidth;
            this.measuredProbeHeight = probeHeight;
        }
        this.parsing = true;
    }

    void endParse() {
        this.parsing = false;
        this.lastParseWidths.clear();
    }

    /**
     * Returns the unscaled width of the text of a fragment, built from the given parts like {@link MarkdownTextFragment#createRenderComponent(Style, String, boolean)} builds it.
     */
    float getWidth(@NotNull Style style, @NotNull String renderText, boolean separateTrailingSpace) {
        if (!this.parsing) return this.measure(style, renderText, separateTrailingSpace);
        Key key = new Key(style, renderText, separateTrailingSpace);
        float width = this.widths.getFloat(key);
        if (!Float.isNaN(width)) return width;
        width = this.lastParseWidths.getFloat(key);
        if (Float.isNaN(width)) width = this.measure(style, renderText, separateTrailingSpace);
        this.widths.put(key, width);
        return width;
    }

    private float measure(@NotNull Style style, @NotNull String renderText, boolean separateTrailingSpace) {
        return this.renderer.textFont.width(MarkdownTextFragment.createRenderComponent(style, renderText, separateTrailingSpace));
    }

    private static final class Key {

        private final Style style;
        private final String renderText;
        private final boolean separateTrailingSpace;
        // Styles compute their hash on every call, and a key gets looked up in both maps
        private final int hash;

        private Key(@NotNull Style style, @NotNull String renderText, boolean separateTrailingSpace) {
            this.style = style;
            this.renderText = renderText;
            this.separateTrailingSpace = separateTrailingSpace;
            this.hash = (((style.hashCode() * 31) + renderText.hashCode()) * 31) + (separateTrailingSpace ? 1 : 0);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return (this.hash == key.hash) && (this.separateTrailingSpace == key.separateTrailingSpace) && this.renderText.equals(key.renderText) && this.style.equals(key.style);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

    }

}
