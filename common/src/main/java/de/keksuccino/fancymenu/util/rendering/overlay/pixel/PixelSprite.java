package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * A pixel-art sprite defined in code as rows of characters, the mockup's {@code S(...)}.<br>
 * Every character is one art pixel. A {@link PixelPalette} gives the characters their colors when the sprite gets drawn,
 * so one sprite can be drawn in many color variants. Characters without a palette color are transparent.<br><br>
 *
 * Drawing it with {@link PixelGraphics#sprite} bakes the sprite with that palette into a shared GPU atlas once and then draws
 * one textured quad per call, flipped and whole-number scaled as needed. Sprites are immutable, so keep them in static constants.
 *
 * <pre>{@code
 * static final PixelSprite FLAKE = PixelSprite.of(
 *         "..2..",
 *         ".1.1.",
 *         "2.3.2",
 *         ".1.1.",
 *         "..2..");
 * }</pre>
 */
public final class PixelSprite {

    private static final PixelAtlas.Entry[] NO_VARIANTS = new PixelAtlas.Entry[0];

    private final int width;
    private final int height;
    /** Row-major 7-bit ASCII codes */
    private final byte[] codes;

    /** The baked palette variants of this sprite in the atlas. Only touched by {@link PixelAtlas} on the render thread. */
    PixelAtlas.Entry[] variants = NO_VARIANTS;
    int variantCount = 0;

    private PixelSprite(int width, int height, @NotNull byte[] codes) {
        this.width = width;
        this.height = height;
        this.codes = codes;
    }

    /**
     * Builds a sprite from rows of characters. All rows need the same length.
     */
    @NotNull
    public static PixelSprite of(@NotNull String... rows) {
        if (rows.length == 0) throw new IllegalArgumentException("A sprite needs at least one row");
        int width = rows[0].length();
        if (width == 0) throw new IllegalArgumentException("Sprite rows can't be empty");
        byte[] codes = new byte[width * rows.length];
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            if (row.length() != width) throw new IllegalArgumentException("Sprite row " + y + " is " + row.length() + " characters long, but the first row is " + width);
            for (int x = 0; x < width; x++) codes[y * width + x] = toCode(row.charAt(x));
        }
        return new PixelSprite(width, rows.length, codes);
    }

    /**
     * Builds a sprite from row-major characters, for sprites made by code (for example clouds built from noise).
     */
    @NotNull
    public static PixelSprite fromCodes(int width, int height, @NotNull char[] codes) {
        if ((width <= 0) || (height <= 0)) throw new IllegalArgumentException("Sprites need a size of at least 1x1");
        if (codes.length != width * height) throw new IllegalArgumentException("Expected " + (width * height) + " characters, got " + codes.length);
        byte[] bytes = new byte[codes.length];
        for (int i = 0; i < codes.length; i++) bytes[i] = toCode(codes[i]);
        return new PixelSprite(width, height, bytes);
    }

    private static byte toCode(char c) {
        if (c >= PixelPalette.CODE_COUNT) throw new IllegalArgumentException("Sprite characters have to be 7-bit ASCII: " + c);
        return (byte)c;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public char codeAt(int x, int y) {
        return (char)this.codes[y * this.width + x];
    }

    /**
     * Returns this sprite turned a quarter turn clockwise, the mockup's {@code rot}.
     */
    @NotNull
    public PixelSprite rotatedClockwise() {
        int w = this.width;
        int h = this.height;
        byte[] rotated = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                rotated[x * h + (h - 1 - y)] = this.codes[y * w + x];
            }
        }
        return new PixelSprite(h, w, rotated);
    }

    /**
     * Returns the ARGB pixels of this sprite in the given palette, row-major, one int per art pixel.
     */
    @NotNull
    public int[] bake(@NotNull PixelPalette palette) {
        return this.bake(palette, false, false, 1);
    }

    /**
     * Returns the ARGB pixels of this sprite in the given palette, flipped and scaled like {@link PixelGraphics#sprite} draws it.
     * The result is {@code width() * scale} by {@code height() * scale} pixels, row-major.
     */
    @NotNull
    public int[] bake(@NotNull PixelPalette palette, boolean flipX, boolean flipY, int scale) {
        if (scale < 1) throw new IllegalArgumentException("The scale has to be at least 1");
        int outWidth = this.width * scale;
        int outHeight = this.height * scale;
        int[] out = new int[outWidth * outHeight];
        for (int y = 0; y < outHeight; y++) {
            int sy = y / scale;
            if (flipY) sy = this.height - 1 - sy;
            int row = sy * this.width;
            for (int x = 0; x < outWidth; x++) {
                int sx = x / scale;
                if (flipX) sx = this.width - 1 - sx;
                out[y * outWidth + x] = palette.colorAt(this.codes[row + sx]);
            }
        }
        return out;
    }

    /**
     * Writes the unscaled pixels in the given palette into a row-major ARGB buffer and returns whether any of them is visible.
     */
    boolean bakeInto(@NotNull PixelPalette palette, @NotNull int[] target, int offset, int stride) {
        boolean visible = false;
        for (int y = 0; y < this.height; y++) {
            int row = y * this.width;
            int out = offset + y * stride;
            for (int x = 0; x < this.width; x++) {
                int color = palette.colorAt(this.codes[row + x]);
                target[out + x] = color;
                if ((color >>> 24) != 0) visible = true;
            }
        }
        return visible;
    }

    /**
     * Returns the code of an art pixel by its index in row-major order.
     */
    int codeIndexAt(int index) {
        return this.codes[index];
    }

    @Override
    public String toString() {
        return "PixelSprite{" + this.width + "x" + this.height + ", " + Arrays.hashCode(this.codes) + "}";
    }

}
