package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Builds the textured quads of laid out text, grouped by atlas page, so each page becomes one batch (one draw call).
 * <p>
 * When the text is drawn without rotation or skew, glyph origins get snapped to the device pixel grid (horizontally to quarter
 * pixels, matching the subpixel bins of the rasterizer) and every glyph texel covers exactly one screen pixel, which keeps text
 * sharp at every GUI scale. Otherwise glyphs get drawn as scaled quads with filtered sampling.
 * </p>
 * The instance is reused for every draw call, so it must only be used on the render thread.
 */
final class SmoothTextMesh {

    static final int FLOATS_PER_QUAD = 8;

    private static final int MAX_RASTER_SIZE = 256;
    private static final int SUBPIXEL_BINS = 4;
    private static final float TRANSFORM_EPSILON = 1.0E-4F;
    private static final float SHADOW_OFFSET_PER_SIZE = 0.1F;

    private final List<Batch> shadowBatches = new ArrayList<>();
    private final List<Batch> mainBatches = new ArrayList<>();
    private int shadowBatchCount;
    private int mainBatchCount;
    private final Random obfuscationRandom = new Random();

    private SmoothFont font;
    private float m00;
    private float m01;
    private float m10;
    private float m11;
    private float m20;
    private float m21;
    private float guiScale;
    private boolean pixelAligned;
    private float deviceScale;

    /**
     * Starts a new mesh for text drawn with the given 2D pose ({@code x' = m00*x + m10*y + m20}, {@code y' = m01*x + m11*y + m21}).
     *
     * @param guiScale device pixels per GUI unit
     */
    void begin(@NotNull SmoothFont font, float m00, float m01, float m10, float m11, float m20, float m21, float guiScale) {
        this.font = font;
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        this.m20 = m20;
        this.m21 = m21;
        this.guiScale = (guiScale > 0.0F && Float.isFinite(guiScale)) ? guiScale : 1.0F;
        boolean axisAligned = Math.abs(m01) < TRANSFORM_EPSILON && Math.abs(m10) < TRANSFORM_EPSILON;
        boolean uniform = Math.abs(m00 - m11) <= TRANSFORM_EPSILON * Math.max(1.0F, Math.abs(m00));
        this.pixelAligned = axisAligned && uniform && m00 > 0.0F;
        float poseScale = (float) Math.sqrt(Math.abs((m00 * m11) - (m01 * m10)));
        this.deviceScale = this.guiScale * ((Float.isFinite(poseScale) && poseScale > 0.0F) ? poseScale : 1.0F);
        this.shadowBatchCount = 0;
        this.mainBatchCount = 0;
    }

    /**
     * Adds the quads of a layout whose top-left corner is at {@code (x, y)} in local (pose) space.
     */
    void addLayout(@NotNull SmoothTextLayout layout, float x, float y, float size, int baseColor, boolean shadow) {
        SmoothFont font = this.font;
        SmoothGlyphAtlas atlas = font.atlas();
        float pixelSize = size * this.deviceScale;
        int rasterSize = quantizeRasterSize(pixelSize);
        boolean aligned = this.pixelAligned && pixelSize <= MAX_RASTER_SIZE;
        float localPerPixel = aligned ? (1.0F / (this.guiScale * this.m00)) : (size / rasterSize);
        float offset = shadow ? (size * SHADOW_OFFSET_PER_SIZE) : 0.0F;
        float originX = x + offset;
        float firstBaseline = y + offset + font.getAscent(size) + font.getYOffset(size);
        float lineHeight = font.getLineHeight(size);
        float colorEmPixels = rasterSize * font.colorGlyphScale();
        float colorCenter = font.capHeightEm() * rasterSize * 0.5F;
        long now = System.nanoTime();
        int alpha = baseColor & 0xFF000000;
        SmoothGlyphAtlas.Page lastPage = null;

        for (int i = 0; i < layout.glyphCount; i++) {
            byte flags = layout.glyphFlags[i];
            boolean colorGlyph = (flags & SmoothTextLayout.GLYPH_COLOR) != 0;
            int sourceIndex = layout.glyphSources[i];
            SmoothFontSource.Loaded source = font.source(sourceIndex).get();
            if (source == null) {
                continue;
            }
            int styleIndex = colorGlyph ? 0 : (((flags & SmoothTextLayout.GLYPH_BOLD) != 0) ? 1 : 0) | (((flags & SmoothTextLayout.GLYPH_ITALIC) != 0) ? 2 : 0);
            int glyphId = layout.glyphIds[i];
            if ((flags & SmoothTextLayout.GLYPH_OBFUSCATED) != 0) {
                glyphId = source.obfuscatedGlyph(styleIndex, glyphId, layout.glyphAdvances[i], this.obfuscationRandom);
            }
            float penX = originX + (layout.glyphX[i] * size);
            float baseline = firstBaseline + (layout.glyphLines[i] * lineHeight);

            int bin = 0;
            float devicePenX = 0.0F;
            float deviceBaseline = 0.0F;
            if (aligned) {
                float deviceX = this.toDeviceX(penX);
                if (colorGlyph) {
                    devicePenX = Math.round(deviceX);
                } else {
                    float snapped = Math.round(deviceX * SUBPIXEL_BINS) / (float) SUBPIXEL_BINS;
                    devicePenX = (float) Math.floor(snapped);
                    bin = Math.round((snapped - devicePenX) * SUBPIXEL_BINS) & (SUBPIXEL_BINS - 1);
                }
                deviceBaseline = Math.round(this.toDeviceY(baseline));
            }

            long key = SmoothGlyphAtlas.key(sourceIndex, glyphId, styleIndex, rasterSize, bin);
            SmoothGlyphAtlas.Glyph glyph = atlas.find(key);
            if (glyph == null) {
                SmoothGlyphRasterizer.GlyphBitmap bitmap = colorGlyph
                        ? SmoothGlyphRasterizer.rasterizeColor(source, glyphId, colorEmPixels, colorCenter)
                        : SmoothGlyphRasterizer.rasterizeOutline(source.rasterFont(styleIndex, rasterSize), glyphId, bin / (float) SUBPIXEL_BINS);
                glyph = atlas.add(key, bitmap);
            }
            SmoothGlyphAtlas.Page page = glyph.page();
            if (page == null) {
                continue;
            }
            float x0;
            float y0;
            float x1;
            float y1;
            if (aligned) {
                x0 = this.toLocalX(devicePenX + glyph.left());
                y0 = this.toLocalY(deviceBaseline + glyph.top());
                x1 = this.toLocalX(devicePenX + glyph.left() + glyph.width());
                y1 = this.toLocalY(deviceBaseline + glyph.top() + glyph.height());
            } else {
                x0 = penX + (glyph.left() * localPerPixel);
                y0 = baseline + (glyph.top() * localPerPixel);
                x1 = x0 + (glyph.width() * localPerPixel);
                y1 = y0 + (glyph.height() * localPerPixel);
            }
            int color = colorGlyph ? (alpha | 0xFFFFFF) : SmoothStyledText.resolveColor(layout.glyphStyles[i], baseColor);
            if (shadow) {
                color = darken(color);
            }
            if (page != lastPage) {
                page.lastUsedNanos = now;
                lastPage = page;
            }
            this.batch(page, shadow).add(x0, y0, x1, y1, glyph.u0(), glyph.v0(), glyph.u1(), glyph.v1(), color);
        }

        for (int i = 0; i < layout.decorationCount; i++) {
            boolean underline = layout.decorationKinds[i] == SmoothTextLayout.DECORATION_UNDERLINE;
            float lineBaseline = firstBaseline + (layout.decorationLines[i] * lineHeight);
            float lineY = lineBaseline + (underline ? font.getUnderlineOffset(size) : font.getStrikethroughOffset(size));
            float thickness = underline ? font.getUnderlineThickness(size) : font.getStrikethroughThickness(size);
            float x0 = originX + (layout.decorationStarts[i] * size);
            float x1 = originX + (layout.decorationEnds[i] * size);
            float y0 = lineY;
            float y1 = lineY + thickness;
            if (aligned) {
                float deviceThickness = Math.max(1.0F, Math.round(thickness * this.guiScale * this.m00));
                float deviceY = Math.round(this.toDeviceY(lineY));
                x0 = this.toLocalX(Math.round(this.toDeviceX(x0)));
                x1 = this.toLocalX(Math.round(this.toDeviceX(x1)));
                y0 = this.toLocalY(deviceY);
                y1 = this.toLocalY(deviceY + deviceThickness);
            }
            if (x1 <= x0) {
                continue;
            }
            SmoothGlyphAtlas.Page page = (lastPage != null) ? lastPage : atlas.anyPage();
            int color = SmoothStyledText.resolveColor(layout.decorationStyles[i], baseColor);
            if (shadow) {
                color = darken(color);
            }
            page.lastUsedNanos = now;
            this.batch(page, shadow).add(x0, y0, x1, y1, page.whiteU, page.whiteV, page.whiteU, page.whiteV, color);
        }
    }

    /**
     * Returns the batches in draw order: all shadow batches first, so shadows never cover the text of another batch.
     */
    @NotNull
    Batch batchAt(int index) {
        return (index < this.shadowBatchCount) ? this.shadowBatches.get(index) : this.mainBatches.get(index - this.shadowBatchCount);
    }

    int batchCount() {
        return this.shadowBatchCount + this.mainBatchCount;
    }

    @NotNull
    private Batch batch(@NotNull SmoothGlyphAtlas.Page page, boolean shadow) {
        List<Batch> batches = shadow ? this.shadowBatches : this.mainBatches;
        int count = shadow ? this.shadowBatchCount : this.mainBatchCount;
        for (int i = 0; i < count; i++) {
            Batch batch = batches.get(i);
            if (batch.page == page) {
                return batch;
            }
        }
        Batch batch;
        if (count < batches.size()) {
            batch = batches.get(count);
        } else {
            batch = new Batch();
            batches.add(batch);
        }
        batch.reset(page);
        if (shadow) {
            this.shadowBatchCount++;
        } else {
            this.mainBatchCount++;
        }
        return batch;
    }

    private float toDeviceX(float localX) {
        return ((this.m00 * localX) + this.m20) * this.guiScale;
    }

    private float toDeviceY(float localY) {
        return ((this.m11 * localY) + this.m21) * this.guiScale;
    }

    private float toLocalX(float deviceX) {
        return ((deviceX / this.guiScale) - this.m20) / this.m00;
    }

    private float toLocalY(float deviceY) {
        return ((deviceY / this.guiScale) - this.m21) / this.m11;
    }

    /**
     * Rounds the device pixel size of the text to the sizes glyphs get rasterized at. Larger sizes use coarser steps, so smooth
     * zoom animations don't rasterize every glyph again on every frame.
     */
    static int quantizeRasterSize(float pixelSize) {
        if (!Float.isFinite(pixelSize) || pixelSize <= 1.0F) {
            return 1;
        }
        if (pixelSize >= MAX_RASTER_SIZE) {
            return MAX_RASTER_SIZE;
        }
        int step = (pixelSize <= 48.0F) ? 1 : ((pixelSize <= 96.0F) ? 2 : 4);
        return Math.min(MAX_RASTER_SIZE, Math.max(1, Math.round(pixelSize / step) * step));
    }

    /**
     * Shadow color like vanilla text shadows: a quarter of the brightness, same alpha.
     */
    static int darken(int argb) {
        int red = ((argb >> 16) & 0xFF) / 4;
        int green = ((argb >> 8) & 0xFF) / 4;
        int blue = (argb & 0xFF) / 4;
        return (argb & 0xFF000000) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Quads of one atlas page: {@value #FLOATS_PER_QUAD} floats per quad ({@code x0, y0, x1, y1, u0, v0, u1, v1}) and one ARGB color.
     * Also tracks the local-space bounds of all quads.
     */
    static final class Batch {

        @Nullable
        SmoothGlyphAtlas.Page page;
        float[] quads = new float[FLOATS_PER_QUAD * 32];
        int[] colors = new int[32];
        int quadCount;
        float minX;
        float minY;
        float maxX;
        float maxY;

        private void reset(@NotNull SmoothGlyphAtlas.Page page) {
            this.page = page;
            this.quadCount = 0;
            this.minX = Float.POSITIVE_INFINITY;
            this.minY = Float.POSITIVE_INFINITY;
            this.maxX = Float.NEGATIVE_INFINITY;
            this.maxY = Float.NEGATIVE_INFINITY;
        }

        private void add(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int color) {
            if (this.quadCount == this.colors.length) {
                this.quads = Arrays.copyOf(this.quads, this.quads.length * 2);
                this.colors = Arrays.copyOf(this.colors, this.colors.length * 2);
            }
            int offset = this.quadCount * FLOATS_PER_QUAD;
            this.quads[offset] = x0;
            this.quads[offset + 1] = y0;
            this.quads[offset + 2] = x1;
            this.quads[offset + 3] = y1;
            this.quads[offset + 4] = u0;
            this.quads[offset + 5] = v0;
            this.quads[offset + 6] = u1;
            this.quads[offset + 7] = v1;
            this.colors[this.quadCount] = color;
            this.quadCount++;
            this.minX = Math.min(this.minX, Math.min(x0, x1));
            this.minY = Math.min(this.minY, Math.min(y0, y1));
            this.maxX = Math.max(this.maxX, Math.max(x0, x1));
            this.maxY = Math.max(this.maxY, Math.max(y0, y1));
        }

    }

}
