package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;

/**
 * Maps the characters of a {@link PixelSprite} to ARGB colors, the mockup's {@code P({...})}.<br>
 * Characters without a color are transparent, so {@code '.'} is the usual "empty" character of sprites.
 * Colors can be translucent: the alpha byte is part of the color.<br><br>
 *
 * Palettes are immutable and compare by their colors, so two palettes with the same colors share the same baked sprites.
 * Build palettes when their colors change (for example when the user picks another color), never once per frame.
 * Baked sprites of palettes nobody draws anymore get released after a few seconds.
 *
 * <pre>{@code
 * PixelPalette flake = PixelPalette.builder()
 *         .rgb('1', 0x93B0DC)        // opaque
 *         .rgb('2', 0xD3E3F7, 130)   // translucent, alpha 130 of 255
 *         .build();
 * }</pre>
 */
public final class PixelPalette {

    /** Sprite characters are 7-bit ASCII. */
    public static final int CODE_COUNT = 128;
    public static final PixelPalette EMPTY = new PixelPalette(new int[CODE_COUNT]);

    private final int[] colors;
    private final int hash;

    private PixelPalette(@NotNull int[] colors) {
        this.colors = colors;
        this.hash = Arrays.hashCode(colors);
    }

    @NotNull
    public static Builder builder() {
        return new Builder(null);
    }

    /**
     * Returns the ARGB color of a sprite character, 0 (transparent) if it has none.
     */
    public int color(char code) {
        return (code < CODE_COUNT) ? this.colors[code] : 0;
    }

    /**
     * Returns a copy of this palette with one color replaced.
     */
    @NotNull
    public PixelPalette withColor(char code, int argb) {
        return new Builder(this).argb(code, argb).build();
    }

    /**
     * Returns a builder that starts with all colors of this palette.
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    int colorAt(int code) {
        return this.colors[code];
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof PixelPalette other)) return false;
        return (this.hash == other.hash) && Arrays.equals(this.colors, other.colors);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    public static final class Builder {

        private final int[] colors = new int[CODE_COUNT];

        private Builder(@Nullable PixelPalette base) {
            if (base != null) System.arraycopy(base.colors, 0, this.colors, 0, CODE_COUNT);
        }

        /**
         * Sets an opaque color. The alpha byte of {@code rgb} is ignored.
         */
        @NotNull
        public Builder rgb(char code, int rgb) {
            return this.argb(code, PixelMath.opaque(rgb));
        }

        /**
         * Sets a color with the given alpha from 0 to 255, like {@code [0xD3E3F7, 130]} in the mockup. The alpha byte of {@code rgb} is ignored.
         */
        @NotNull
        public Builder rgb(char code, int rgb, int alpha) {
            return this.argb(code, PixelMath.withAlpha(rgb, alpha));
        }

        @NotNull
        public Builder argb(char code, int argb) {
            if (code >= CODE_COUNT) throw new IllegalArgumentException("Palette characters have to be 7-bit ASCII: " + code);
            this.colors[code] = argb;
            return this;
        }

        @NotNull
        public PixelPalette build() {
            return new PixelPalette(this.colors.clone());
        }

    }

}
