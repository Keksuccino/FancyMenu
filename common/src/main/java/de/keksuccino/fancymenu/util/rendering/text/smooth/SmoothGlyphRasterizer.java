package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Draws single glyphs into pixel buffers for the {@link SmoothGlyphAtlas}.
 * <p>
 * Glyphs get rasterized at the exact device pixel size they are shown at, so they map 1:1 to screen pixels and stay sharp.
 * Every bitmap has a {@value #PADDING} pixel transparent border, so filtered sampling never picks up neighboring atlas entries.
 * </p>
 */
final class SmoothGlyphRasterizer {

    private static final Logger LOGGER = LogManager.getLogger();

    static final int PADDING = 1;
    static final GlyphBitmap EMPTY = new GlyphBitmap(0, 0, 0, 0, false, null, null);
    /** Float error that gets ignored when rounding ink sizes up to whole pixels, so an ink box of exactly 20 pixels stays 20 pixels wide. */
    private static final float INK_EDGE_TOLERANCE = 1.0E-3F;

    private SmoothGlyphRasterizer() {
    }

    /**
     * Rasterizes an outline glyph as coverage values.
     *
     * @param subpixelX horizontal pen offset in pixels ({@code 0 <= subpixelX < 1}), so glyphs can be placed between pixels
     */
    @NotNull
    static GlyphBitmap rasterizeOutline(@NotNull Font rasterFont, int glyphId, float subpixelX) {
        Shape outline = rasterFont.createGlyphVector(SmoothFontSource.RENDER_CONTEXT, new int[]{glyphId}).getGlyphOutline(0, subpixelX, 0.0F);
        Rectangle bounds = outline.getBounds();
        if (bounds.isEmpty()) {
            return EMPTY;
        }
        return fillCoverage(outline, bounds.x - PADDING, bounds.y - PADDING, bounds.width + (PADDING * 2), bounds.height + (PADDING * 2));
    }

    /**
     * Rasterizes an icon glyph so the top-left corner of its ink box lands exactly on a pixel corner, which keeps the outer edges
     * of icons sharp. The bitmap's {@code left}/{@code top} place it relative to that corner.
     *
     * @param pixelsPerEm scale of the glyph; fractional values are fine, because the outline gets scaled directly
     */
    @NotNull
    static GlyphBitmap rasterizeIcon(@NotNull SmoothFont.IconGlyph glyph, float pixelsPerEm) {
        int inkWidth = (int) Math.ceil(glyph.inkWidth() * pixelsPerEm - INK_EDGE_TOLERANCE);
        int inkHeight = (int) Math.ceil(glyph.inkHeight() * pixelsPerEm - INK_EDGE_TOLERANCE);
        if (inkWidth <= 0 || inkHeight <= 0) {
            return EMPTY;
        }
        double scale = pixelsPerEm / SmoothFontSource.LAYOUT_SIZE;
        AffineTransform transform = AffineTransform.getTranslateInstance(-glyph.minX() * pixelsPerEm, -glyph.minY() * pixelsPerEm);
        transform.scale(scale, scale);
        return fillCoverage(transform.createTransformedShape(glyph.outline()), -PADDING, -PADDING, inkWidth + (PADDING * 2), inkHeight + (PADDING * 2));
    }

    /**
     * Fills a shape white into a fresh image covering the given pixel area and returns the coverage of every pixel.
     */
    @NotNull
    private static GlyphBitmap fillCoverage(@NotNull Shape shape, int left, int top, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = createGraphics(image);
        graphics.setColor(Color.WHITE);
        graphics.translate(-left, -top);
        graphics.fill(shape);
        graphics.dispose();
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        byte[] coverage = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            coverage[i] = (byte) (pixels[i] >>> 24);
        }
        return new GlyphBitmap(width, height, left, top, false, coverage, null);
    }

    /**
     * Rasterizes a color glyph, centered vertically on the cap height of the text it is part of.
     *
     * @param emPixels pixel size of the color glyph's em square
     * @param centerAboveBaseline distance in pixels between the baseline and the vertical center of the glyph
     */
    @NotNull
    static GlyphBitmap rasterizeColor(@NotNull SmoothFontSource.Loaded source, int glyphId, float emPixels, float centerAboveBaseline) {
        ColorGlyphReader reader = source.colorGlyphs;
        if (reader == null || emPixels <= 0.0F) {
            return EMPTY;
        }
        int[] layers = reader.findLayers(glyphId);
        if (layers != null) {
            return rasterizeLayers(source, layers, emPixels, centerAboveBaseline);
        }
        ColorGlyphReader.EmbeddedBitmap bitmap = reader.findBitmap(glyphId, emPixels);
        if (bitmap == null) {
            return EMPTY;
        }
        BufferedImage decoded = decode(bitmap.imageData());
        if (decoded == null) {
            return EMPTY;
        }
        float scale = emPixels / (float) bitmap.ppem();
        int width = Math.max(1, Math.round(decoded.getWidth() * scale));
        int height = Math.max(1, Math.round(decoded.getHeight() * scale));
        int[] scaled = scaleToStraightArgb(decoded, width, height);
        int left = Math.round(bitmap.offsetX() * scale) - PADDING;
        int top = Math.round(-centerAboveBaseline - (height / 2.0F)) - PADDING;
        return padAndBleed(scaled, width, height, left, top);
    }

    @NotNull
    private static GlyphBitmap rasterizeLayers(@NotNull SmoothFontSource.Loaded source, @NotNull int[] layers, float emPixels, float centerAboveBaseline) {
        Font font = source.rasterFont(0, Math.max(1, Math.round(emPixels)));
        Shape[] shapes = new Shape[layers.length / 2];
        Rectangle2D union = null;
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = font.createGlyphVector(SmoothFontSource.RENDER_CONTEXT, new int[]{layers[i * 2]}).getGlyphOutline(0);
            Rectangle2D layerBounds = shapes[i].getBounds2D();
            if (!layerBounds.isEmpty()) {
                union = (union == null) ? layerBounds : union.createUnion(layerBounds);
            }
        }
        if (union == null) {
            return EMPTY;
        }
        // Center the color font's em box (ascender to descender), so all glyphs of the font share one baseline.
        OpenTypeTables tables = source.tables;
        float unitsPerEm = (tables != null) ? tables.unitsPerEm() : 1.0F;
        float ascender = (tables != null) ? (tables.ascender() / unitsPerEm) * emPixels : emPixels * 0.8F;
        float descender = (tables != null) ? (-tables.descender() / unitsPerEm) * emPixels : emPixels * 0.2F;
        float colorBaseline = -centerAboveBaseline - ((ascender + descender) / 2.0F) + ascender;
        int baselineOffset = Math.round(colorBaseline);
        int minX = (int) Math.floor(union.getMinX());
        int minY = (int) Math.floor(union.getMinY());
        int width = (int) Math.ceil(union.getMaxX()) - minX;
        int height = (int) Math.ceil(union.getMaxY()) - minY;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = createGraphics(image);
        graphics.translate(-minX, -minY);
        for (int i = 0; i < shapes.length; i++) {
            graphics.setColor(new Color(layers[(i * 2) + 1], true));
            graphics.fill(shapes[i]);
        }
        graphics.dispose();
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        return padAndBleed(pixels, width, height, minX - PADDING, minY + baselineOffset - PADDING);
    }

    @Nullable
    private static BufferedImage decode(@NotNull byte[] imageData) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imageData));
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("[FANCYMENU] Failed to decode an embedded color glyph bitmap.", ex);
            return null;
        }
    }

    /**
     * Scales an image to the target size in premultiplied space (halving with bilinear filtering until the last step, which
     * keeps fine details of large emoji bitmaps), then returns straight-alpha ARGB pixels.
     */
    @NotNull
    private static int[] scaleToStraightArgb(@NotNull BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage current = copyToPremultiplied(source, source.getWidth(), source.getHeight(), null);
        while (current.getWidth() / 2 >= targetWidth && current.getHeight() / 2 >= targetHeight) {
            current = copyToPremultiplied(current, current.getWidth() / 2, current.getHeight() / 2, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        if (current.getWidth() != targetWidth || current.getHeight() != targetHeight) {
            current = copyToPremultiplied(current, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }
        int[] pixels = ((DataBufferInt) current.getRaster().getDataBuffer()).getData();
        int[] straight = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int alpha = argb >>> 24;
            if (alpha == 0) {
                continue;
            }
            int red = Math.min(255, (((argb >> 16) & 0xFF) * 255 + (alpha / 2)) / alpha);
            int green = Math.min(255, (((argb >> 8) & 0xFF) * 255 + (alpha / 2)) / alpha);
            int blue = Math.min(255, ((argb & 0xFF) * 255 + (alpha / 2)) / alpha);
            straight[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        return straight;
    }

    @NotNull
    private static BufferedImage copyToPremultiplied(@NotNull BufferedImage source, int width, int height, @Nullable Object interpolation) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = target.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        if (interpolation != null) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        graphics.drawImage(source, AffineTransform.getScaleInstance((double) width / source.getWidth(), (double) height / source.getHeight()), null);
        graphics.dispose();
        return target;
    }

    /**
     * Adds the transparent border and copies the color of opaque pixels into their transparent neighbors. Filtered sampling
     * blends edge pixels with their neighbors, and transparent neighbors without a matching color would leave dark fringes.
     */
    @NotNull
    static GlyphBitmap padAndBleed(@NotNull int[] pixels, int width, int height, int left, int top) {
        int paddedWidth = width + (PADDING * 2);
        int paddedHeight = height + (PADDING * 2);
        int[] padded = new int[paddedWidth * paddedHeight];
        for (int y = 0; y < height; y++) {
            System.arraycopy(pixels, y * width, padded, ((y + PADDING) * paddedWidth) + PADDING, width);
        }
        int[] source = padded.clone();
        for (int y = 0; y < paddedHeight; y++) {
            for (int x = 0; x < paddedWidth; x++) {
                int index = (y * paddedWidth) + x;
                if ((source[index] >>> 24) != 0) {
                    continue;
                }
                int red = 0;
                int green = 0;
                int blue = 0;
                int count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= paddedHeight) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= paddedWidth) continue;
                        int neighbor = source[(ny * paddedWidth) + nx];
                        if ((neighbor >>> 24) != 0) {
                            red += (neighbor >> 16) & 0xFF;
                            green += (neighbor >> 8) & 0xFF;
                            blue += neighbor & 0xFF;
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    padded[index] = ((red / count) << 16) | ((green / count) << 8) | (blue / count);
                }
            }
        }
        return new GlyphBitmap(paddedWidth, paddedHeight, left, top, true, null, padded);
    }

    @NotNull
    private static Graphics2D createGraphics(@NotNull BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return graphics;
    }

    /**
     * A rasterized glyph. {@code left}/{@code top} place the bitmap relative to the pen position on the baseline (y grows down).
     * Outline glyphs carry {@code coverage} (drawn in the text color), color glyphs carry straight-alpha {@code argb} pixels.
     */
    record GlyphBitmap(int width, int height, int left, int top, boolean color, @Nullable byte[] coverage, @Nullable int[] argb) {

        boolean isEmpty() {
            return this.width <= 0 || this.height <= 0;
        }

    }

}
