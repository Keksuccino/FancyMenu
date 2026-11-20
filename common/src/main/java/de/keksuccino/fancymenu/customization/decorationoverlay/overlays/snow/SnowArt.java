package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.OverlayColorTint;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The pixel art of the Snow overlay: flake sprites with their two tumble frames, the cap glint, and all colors in the current snow color.
 * The built-in colors are a cold white with blue shade; a custom snow color recolors them, see {@link OverlayColorTint}.
 */
final class SnowArt {

    /** White, the snow color that keeps the built-in palette */
    static final int REFERENCE_COLOR = 0xFFFFFF;

    /**
     * Flake sprites per depth layer (the dust layer 0 is a plain pixel): the "+" frame and the "×" frame. Switching between them makes the
     * flake look like it tumbles without ever leaving the pixel grid.
     */
    static final PixelSprite[][] FLAKES = {
            {},
            {
                    PixelSprite.of(
                            ".1.",
                            "121",
                            ".1."),
                    PixelSprite.of(
                            "1.1",
                            ".2.",
                            "1.1")
            },
            {
                    PixelSprite.of(
                            "..2..",
                            ".1.1.",
                            "2.3.2",
                            ".1.1.",
                            "..2.."),
                    PixelSprite.of(
                            "2...2",
                            "..1..",
                            ".131.",
                            "..1..",
                            "2...2")
            },
            {
                    PixelSprite.of(
                            "...2...",
                            ".1.2.1.",
                            "..121..",
                            "2223222",
                            "..121..",
                            ".1.2.1.",
                            "...2..."),
                    PixelSprite.of(
                            "2.....2",
                            ".2.1.2.",
                            "..212..",
                            ".11311.",
                            "..212..",
                            ".2.1.2.",
                            "2.....2")
            }
    };

    /** The glint of a cap at its brightest: the spark with arms to the sides and up. Drawn one pixel up and left of the spark. */
    static final PixelSprite GLINT_CROSS = PixelSprite.of(
            ".a.",
            "aca");

    private static final int[][] FLAKE_COLORS = {
            {},
            {0x93B0DC, 0xD3E3F7},
            {0xA6C4EC, 0xEDF4FF, 0xFFFFFF},
            {0xB2CDF0, 0xF4F9FF, 0xFFFFFF}
    };
    private static final int DUST = 0x7F9DCB;
    private static final int CAP_TOP = 0xFFFFFF;
    private static final int CAP_BODY = 0xDCE8F8;
    private static final int CAP_SHADE = 0xA9C4E8;
    private static final int ICE_ROOT = 0xD3E3F7;
    private static final int ICE_BODY = 0xA6C4EC;
    private static final int ICE_TIP = 0xFFFFFF;
    private static final int PUFF = 0xE6EFFA;
    private static final int GLINT_CORE = 0xFFFFFF;
    private static final int GLINT_ARM = 0xD3E3F7;

    final PixelPalette[] flakePalettes = new PixelPalette[FLAKES.length];
    PixelPalette glintPalette = PixelPalette.EMPTY;
    int dust;
    int capTop;
    int capBody;
    int capShade;
    int iceRoot;
    int iceBody;
    int iceTip;
    int puff;
    int puffFaded;
    int glintCore;

    SnowArt(@NotNull OverlayColorTint tint) {
        this.rebuild(tint);
    }

    /**
     * Builds all colors and palettes for the current snow color. Only call it when the color changed.
     */
    void rebuild(@NotNull OverlayColorTint tint) {
        this.flakePalettes[0] = PixelPalette.EMPTY;
        for (int layer = 1; layer < FLAKE_COLORS.length; layer++) {
            PixelPalette.Builder builder = PixelPalette.builder();
            int[] colors = FLAKE_COLORS[layer];
            for (int i = 0; i < colors.length; i++) builder.argb((char)('1' + i), tint.tint(colors[i]));
            this.flakePalettes[layer] = builder.build();
        }
        this.glintPalette = PixelPalette.builder().argb('c', tint.tint(GLINT_CORE)).argb('a', tint.tint(GLINT_ARM)).build();
        this.dust = tint.tint(DUST);
        this.capTop = tint.tint(CAP_TOP);
        this.capBody = tint.tint(CAP_BODY);
        this.capShade = tint.tint(CAP_SHADE);
        this.iceRoot = tint.tint(ICE_ROOT);
        this.iceBody = tint.tint(ICE_BODY);
        this.iceTip = tint.tint(ICE_TIP);
        this.puff = tint.tint(PUFF);
        this.puffFaded = tint.tint(PUFF, 140);
        this.glintCore = tint.tint(GLINT_CORE);
    }

}
