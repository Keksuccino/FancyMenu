package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.ParticleLayer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The pixel art of the Fireworks overlay: eight four-color ramps the stars burn down through, the cooling colors of rocket sparks,
 * the burst flash, the glitter sparkle and the rocket. Everything is drawn on a grid of art pixels that are {@code scale} GUI pixels big.
 */
final class FireworksArt {

    /** Star ramps from the hot head color down to the dark ember: red, gold, green, blue, purple, pink, teal and silver */
    static final int[][] RAMPS = {
            {0xFFF1D6, 0xFF5A4D, 0xC2283A, 0x6B1836},
            {0xFFFBE0, 0xFFD84A, 0xE08A1E, 0x7A3A14},
            {0xF0FFD6, 0x7BE04A, 0x2E9E4E, 0x14524A},
            {0xE6F7FF, 0x5AC8FF, 0x2F6FE0, 0x1B2E7A},
            {0xFFE6FF, 0xD46BFF, 0x8436D0, 0x3A1870},
            {0xFFF0F6, 0xFF7AB8, 0xD0387E, 0x6B1848},
            {0xE6FFF8, 0x4AE8C8, 0x1E9E9A, 0x124A5E},
            {0xFFFFFF, 0xDCE8FF, 0x8FA6D0, 0x44547A}
    };
    static final int GOLD = 1;
    static final int RED = 0;
    static final int PINK = 5;
    /** Rocket sparks cool from white through yellow and orange to dark red */
    static final int[] HEAT = {0xFFF6C8, 0xFFC857, 0xF07F2A, 0x8A2F2A};

    /** The first frame of a burst flash: a white disc, drawn 3 pixels up and left of the burst */
    static final PixelSprite FLASH_DISC = buildFlashDisc();
    /** The second frame of a burst flash: a dithered ring in the head color of the shell, drawn 7 pixels up and left of the burst */
    static final PixelSprite FLASH_RING = buildFlashRing();
    /** A glitter star at the end of its life: a white spark with arms in the head color, drawn one pixel up and left of the star */
    static final PixelSprite SPARKLE = PixelSprite.of(
            ".a.",
            "aca",
            ".a.");
    /** A climbing rocket: the white head over a warm pixel */
    static final PixelSprite ROCKET = PixelSprite.of("w", "y");

    static final PixelPalette FLASH_DISC_PALETTE = PixelPalette.builder().rgb('w', 0xFFFFFF).build();
    static final PixelPalette ROCKET_PALETTE = PixelPalette.builder().rgb('w', 0xFFFFFF).rgb('y', 0xFFE9A0).build();
    static final PixelPalette[] FLASH_RING_PALETTES = new PixelPalette[RAMPS.length];
    static final PixelPalette[] SPARKLE_PALETTES = new PixelPalette[RAMPS.length];
    /** The ramps and heat colors as opaque ARGB */
    static final int[][] RAMPS_ARGB = new int[RAMPS.length][];
    static final int[] HEAT_ARGB = new int[HEAT.length];
    static final int WHITE = 0xFFFFFFFF;

    static {
        for (int r = 0; r < RAMPS.length; r++) {
            FLASH_RING_PALETTES[r] = PixelPalette.builder().rgb('c', RAMPS[r][0]).build();
            SPARKLE_PALETTES[r] = PixelPalette.builder().rgb('c', 0xFFFFFF).rgb('a', RAMPS[r][0]).build();
            RAMPS_ARGB[r] = new int[RAMPS[r].length];
            for (int k = 0; k < RAMPS[r].length; k++) RAMPS_ARGB[r][k] = PixelMath.opaque(RAMPS[r][k]);
        }
        for (int k = 0; k < HEAT.length; k++) HEAT_ARGB[k] = PixelMath.opaque(HEAT[k]);
    }

    private FireworksArt() {
    }

    @NotNull
    private static PixelSprite buildFlashDisc() {
        char[] codes = new char[7 * 7];
        for (int y = -3; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) codes[(y + 3) * 7 + (x + 3)] = (x * x + y * y <= 10) ? 'w' : '.';
        }
        return PixelSprite.fromCodes(7, 7, codes);
    }

    @NotNull
    private static PixelSprite buildFlashRing() {
        char[] codes = new char[15 * 15];
        for (int y = -7; y <= 7; y++) {
            for (int x = -7; x <= 7; x++) {
                double distance = Math.hypot(x, y);
                boolean on = (Math.abs(distance - 6.0) < 0.8) && (((x + y) & 1) == 0);
                codes[(y + 7) * 15 + (x + 7)] = on ? 'c' : '.';
            }
        }
        return PixelSprite.fromCodes(15, 15, codes);
    }

    /**
     * Draws one art pixel.
     */
    static void dot(@NotNull ParticleLayer layer, int x, int y, int argb, int scale) {
        layer.rect(x * scale, y * scale, scale, scale, argb);
    }

    /**
     * Draws a one-art-pixel line between two art pixels, both included, with the steps of the mockup's {@code line}.
     * Every straight run of pixels is one quad.
     */
    static void line(@NotNull ParticleLayer layer, int x0, int y0, int x1, int y1, int argb, int scale) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int error = dx + dy;
        boolean flat = dx >= -dy;
        int runStartX = x0;
        int runStartY = y0;
        while ((x0 != x1) || (y0 != y1)) {
            int doubled = 2 * error;
            int nextX = x0;
            int nextY = y0;
            if (doubled >= dy) {
                error += dy;
                nextX += sx;
            }
            if (doubled <= dx) {
                error += dx;
                nextY += sy;
            }
            // A run ends where the line steps across its main direction
            if (flat ? (nextY != y0) : (nextX != x0)) {
                run(layer, runStartX, runStartY, x0, y0, argb, scale);
                runStartX = nextX;
                runStartY = nextY;
            }
            x0 = nextX;
            y0 = nextY;
        }
        run(layer, runStartX, runStartY, x0, y0, argb, scale);
    }

    private static void run(@NotNull ParticleLayer layer, int xa, int ya, int xb, int yb, int argb, int scale) {
        int left = Math.min(xa, xb);
        int top = Math.min(ya, yb);
        layer.rect(left * scale, top * scale, (Math.abs(xb - xa) + 1) * scale, (Math.abs(yb - ya) + 1) * scale, argb);
    }

}
