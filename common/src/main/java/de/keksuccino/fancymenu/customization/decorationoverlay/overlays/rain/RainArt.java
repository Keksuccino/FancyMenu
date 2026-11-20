package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.OverlayColorTint;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The pixel art of the Rain overlay: the far and near streaks, the hanging and falling drips, and all colors in the current rain color.
 * A custom rain color recolors the built-in blue palette, see {@link OverlayColorTint}. The lightning stays in its own colors.<br><br>
 *
 * Every streak follows the wind angle, which changes all the time. A streak is one sprite, so the sprites get built per streak length and
 * per slope step ({@value #SLOPE_STEPS} steps per pixel of sideways offset per row) the first time they are needed, and are kept after that.
 * All rain overlays share them, so they share their baked atlas variants too. Like all drawing, this only runs on the render thread.
 */
final class RainArt {

    /** The default rain color, which keeps the built-in palette */
    static final int REFERENCE_COLOR = 0xCFE7FF;
    static final int SLOPE_STEPS = 64;
    static final int FAR_MIN_LENGTH = 4;
    static final int FAR_MAX_LENGTH = 6;
    static final int NEAR_MIN_LENGTH = 9;
    static final int NEAR_MAX_LENGTH = 13;

    /** Hanging drip under an element in its three stages: a bead, then stretching with a bright tip */
    static final PixelSprite[] HANGING_DRIP = {
            PixelSprite.of("b"),
            PixelSprite.of("b", "t"),
            PixelSprite.of("b", "b", "t")
    };
    /** Falling drip: a body pixel over the bright head */
    static final PixelSprite FALLING_DRIP = PixelSprite.of("b", "h");

    private static final int NEAR_HEAD = 0xF0F8FF;
    private static final int NEAR_BODY = 0xB8D6F4;
    private static final int NEAR_TAIL = 0x86AEDD;
    private static final int FAR = 0x9DBBE0;
    private static final int FILM = 0x6F97CB;
    private static final int FILM_GLINT = 0xD6EBFF;
    private static final int DROP = 0xD6EBFF;
    private static final int HIT = 0xF0F8FF;
    private static final int RIPPLE = 0xEAF6FF;
    private static final int DRIP_BODY = 0x8FB4DF;
    private static final int DRIP_TIP = 0xE4F1FF;

    private static final PixelSprite[][] NEAR_STREAKS = new PixelSprite[NEAR_MAX_LENGTH - NEAR_MIN_LENGTH + 1][SLOPE_STEPS];
    private static final PixelSprite[][] FAR_STREAKS = new PixelSprite[FAR_MAX_LENGTH - FAR_MIN_LENGTH + 1][SLOPE_STEPS];
    PixelPalette nearPalette = PixelPalette.EMPTY;
    PixelPalette farPalette = PixelPalette.EMPTY;
    PixelPalette hangingDripPalette = PixelPalette.EMPTY;
    PixelPalette fallingDripPalette = PixelPalette.EMPTY;
    int film;
    int filmGlint;
    int drop;
    int hit;
    final int[] ripple = new int[3];

    RainArt(@NotNull OverlayColorTint tint) {
        this.rebuild(tint);
    }

    /**
     * Builds all colors and palettes for the current rain color. Only call it when the color changed.
     */
    void rebuild(@NotNull OverlayColorTint tint) {
        this.nearPalette = PixelPalette.builder()
                .argb('h', tint.tint(NEAR_HEAD, 245))
                .argb('a', tint.tint(NEAR_BODY, 200))
                .argb('b', tint.tint(NEAR_TAIL, 140))
                .argb('c', tint.tint(NEAR_TAIL, 90))
                .build();
        this.farPalette = PixelPalette.builder().argb('a', tint.tint(FAR, 150)).argb('b', tint.tint(FAR, 100)).build();
        this.hangingDripPalette = PixelPalette.builder().argb('b', tint.tint(DRIP_BODY, 200)).argb('t', tint.tint(DRIP_TIP, 240)).build();
        this.fallingDripPalette = PixelPalette.builder().argb('b', tint.tint(NEAR_BODY, 200)).argb('h', tint.tint(NEAR_HEAD, 240)).build();
        this.film = tint.tint(FILM, 105);
        this.filmGlint = tint.tint(FILM_GLINT, 175);
        this.drop = tint.tint(DROP, 210);
        this.hit = tint.tint(HIT, 220);
        this.ripple[0] = tint.tint(RIPPLE, 200);
        this.ripple[1] = tint.tint(RIPPLE, 130);
        this.ripple[2] = tint.tint(RIPPLE, 70);
    }

    /**
     * The slope step closest to a slope (sideways pixels per row).
     */
    static int slopeIndex(float slope) {
        return Math.max(0, Math.min(SLOPE_STEPS - 1, Math.round(slope * SLOPE_STEPS)));
    }

    /**
     * How far the tail end of a streak lies left of its head, in art pixels.
     */
    static int tailOffset(int length, int slopeIndex) {
        return Math.round((length - 1) * (slopeIndex / (float)SLOPE_STEPS));
    }

    @NotNull
    static PixelSprite nearStreak(int length, int slopeIndex) {
        PixelSprite[] row = NEAR_STREAKS[length - NEAR_MIN_LENGTH];
        PixelSprite sprite = row[slopeIndex];
        if (sprite == null) {
            sprite = buildStreak(length, slopeIndex, true);
            row[slopeIndex] = sprite;
        }
        return sprite;
    }

    @NotNull
    static PixelSprite farStreak(int length, int slopeIndex) {
        PixelSprite[] row = FAR_STREAKS[length - FAR_MIN_LENGTH];
        PixelSprite sprite = row[slopeIndex];
        if (sprite == null) {
            sprite = buildStreak(length, slopeIndex, false);
            row[slopeIndex] = sprite;
        }
        return sprite;
    }

    /**
     * Builds a streak with its head in the bottom right corner. Pixel {@code i} (0 is the head) sits {@code i} rows above the head and
     * {@code round(i * slope)} columns left of it. A near streak has a bright head pixel, three fixed opacity steps and, from the ninth
     * pixel on, a tail that only keeps every second pixel. A far streak has two opacity steps.
     */
    @NotNull
    static PixelSprite buildStreak(int length, int slopeIndex, boolean near) {
        float slope = slopeIndex / (float)SLOPE_STEPS;
        int maxOffset = tailOffset(length, slopeIndex);
        int width = maxOffset + 1;
        char[] codes = new char[width * length];
        java.util.Arrays.fill(codes, '.');
        for (int i = 0; i < length; i++) {
            char code;
            if (near) {
                if ((i >= 8) && ((i & 1) == 1)) continue;
                code = (i == 0) ? 'h' : ((i < 4) ? 'a' : ((i < 8) ? 'b' : 'c'));
            } else {
                code = (i < 2) ? 'a' : 'b';
            }
            int column = maxOffset - Math.round(i * slope);
            int row = length - 1 - i;
            codes[row * width + column] = code;
        }
        return PixelSprite.fromCodes(width, length, codes);
    }

}
