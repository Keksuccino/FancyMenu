package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The pixel art of the Leaves overlay and its five color ramps.<br>
 * Detailed leaves use five tones per ramp: highlight (h), base (b), shade (d), vein (v) and stem (s). Their darker underside shifts every
 * tone one step down. Simple leaves are flat two-tone shapes in the base and shade of the same ramps.
 */
final class LeafArt {

    /** Ovate leaf, seen diagonally */
    static final PixelSprite OVATE = PixelSprite.of(
            ".....hb",
            "...hhvb",
            "..hbvbd",
            ".hbvbd.",
            ".bvbd..",
            ".sdd...",
            "s......");
    /** Ovate leaf, flat in the glide, also how landed leaves lie */
    static final PixelSprite FLAT = PixelSprite.of(
            "..hhhb.",
            "ssvvvvb",
            "..bddd.");
    /** Ovate leaf, edge-on while it flips */
    static final PixelSprite EDGE = PixelSprite.of(
            "....d",
            "...d.",
            "..d..",
            ".d...",
            "s....");
    /** Far leaf behind the elements */
    static final PixelSprite TINY = PixelSprite.of(
            ".hb",
            "hbd",
            "s..");
    private static final PixelSprite MAPLE = PixelSprite.of(
            "...h...",
            ".h.h.h.",
            ".hhbhh.",
            "hbbvbbd",
            ".bbvbd.",
            "..dvd..",
            "...s...");
    private static final PixelSprite BIG_MAPLE = PixelSprite.of(
            ".....h.....",
            "....hbh....",
            ".h..hbb..h.",
            ".hh.hvb.bd.",
            ".hbhbvbbbd.",
            "hbbbbvbbbbd",
            ".hbbvvvbbd.",
            "..bbbvbbd..",
            ".hb.bvb.bd.",
            ".....v.....",
            ".....s.....");
    /** Maple leaves spin in quarter turns */
    static final PixelSprite[] MAPLE_TURNS = quarterTurns(MAPLE);
    /** The 11 px maples that drift in the foreground */
    static final PixelSprite[] BIG_MAPLE_TURNS = quarterTurns(BIG_MAPLE);
    /** A landed leaf dissolving: first every second pixel goes, then most of the rest */
    static final PixelSprite FLAT_DISSOLVING = dissolved(FLAT, false);
    static final PixelSprite FLAT_DISSOLVED = dissolved(FLAT, true);

    /** Simple look: a flat two-tone 5 px leaf, gliding */
    static final PixelSprite SIMPLE_LEAF = PixelSprite.of(
            "...bb",
            "..bbb",
            ".bbbd",
            ".bbd.",
            "s....");
    /** Simple look: the leaf flat in the glide, also how landed leaves lie */
    static final PixelSprite SIMPLE_FLAT = PixelSprite.of(
            ".bbb.",
            "sbbbb",
            ".bdd.");
    /** Simple look: far leaf behind the elements */
    static final PixelSprite SIMPLE_TINY = PixelSprite.of(
            ".bb",
            "bbd",
            "s..");
    static final PixelSprite SIMPLE_FLAT_DISSOLVING = dissolved(SIMPLE_FLAT, false);
    static final PixelSprite SIMPLE_FLAT_DISSOLVED = dissolved(SIMPLE_FLAT, true);

    /** The five built-in autumn ramps (highlight, base, shade, vein, stem): red, orange, yellow, green, brown */
    static final int[][] AUTUMN_RAMPS = {
            {0xF2704A, 0xD2402F, 0x8E2334, 0xA82C30, 0x5A2A22},
            {0xFFB84A, 0xEE8527, 0xB4521E, 0xC9651F, 0x5A3418},
            {0xFFE98A, 0xF2C335, 0xC08A1E, 0xD9A52A, 0x6A4A18},
            {0xC9DB6E, 0x94AE3C, 0x5E7C2C, 0x7A963A, 0x3E4A1E},
            {0xC98A4A, 0x9A6030, 0x6A3E22, 0x7E4E28, 0x3E2616}
    };
    static final int RAMP_COUNT = 5;
    static final LeafArt AUTUMN = new LeafArt(AUTUMN_RAMPS);

    /** Face palettes of the five ramps */
    final PixelPalette[] face = new PixelPalette[RAMP_COUNT];
    /** Darker underside palettes of the five ramps */
    final PixelPalette[] back = new PixelPalette[RAMP_COUNT];
    /** Palettes of the simple look */
    final PixelPalette[] simple = new PixelPalette[RAMP_COUNT];

    LeafArt(@NotNull int[][] ramps) {
        for (int i = 0; i < RAMP_COUNT; i++) {
            int[] r = ramps[i];
            this.face[i] = PixelPalette.builder().rgb('h', r[0]).rgb('b', r[1]).rgb('d', r[2]).rgb('v', r[3]).rgb('s', r[4]).build();
            this.back[i] = PixelPalette.builder().rgb('h', r[1]).rgb('b', r[2]).rgb('d', mix(r[2], 0x1A0E0A, 0.35F)).rgb('v', r[2]).rgb('s', r[4]).build();
            this.simple[i] = PixelPalette.builder().rgb('b', r[1]).rgb('d', r[2]).rgb('s', r[4]).build();
        }
    }

    /**
     * The art of the Custom Range colors: five ramps for colors at 0, 25, 50, 75 and 100 percent from the start to the end color.
     */
    @NotNull
    static LeafArt ofRange(int startRgb, int endRgb) {
        return new LeafArt(customRamps(startRgb, endRgb));
    }

    @NotNull
    static int[][] customRamps(int startRgb, int endRgb) {
        int[][] ramps = new int[RAMP_COUNT][];
        for (int i = 0; i < RAMP_COUNT; i++) {
            ramps[i] = rampOf(mix(startRgb, endRgb, i / (float)(RAMP_COUNT - 1)));
        }
        return ramps;
    }

    /**
     * Shading tones for a leaf of any base color: a warmer highlight, a cooler shade, a vein between the two and a dark stem.
     */
    @NotNull
    static int[] rampOf(int baseRgb) {
        int shade = mix(baseRgb, 0x3A1030, 0.38F);
        return new int[]{mix(baseRgb, 0xFFE9A8, 0.38F), baseRgb & 0xFFFFFF, shade, mix(baseRgb, shade, 0.55F), mix(baseRgb, 0x2A1A10, 0.68F)};
    }

    private static int mix(int rgbFrom, int rgbTo, float t) {
        return PixelMath.mix(PixelMath.opaque(rgbFrom), PixelMath.opaque(rgbTo), t) & 0xFFFFFF;
    }

    @NotNull
    private static PixelSprite[] quarterTurns(@NotNull PixelSprite sprite) {
        PixelSprite one = sprite.rotatedClockwise();
        PixelSprite two = one.rotatedClockwise();
        return new PixelSprite[]{sprite, one, two, two.rotatedClockwise()};
    }

    /**
     * The sprite with a checkerboard of its pixels left out, and with {@code thin} also every second column of the rest.
     * The pattern follows the unflipped sprite, so a flipped leaf dissolves in the mirrored pattern.
     */
    @NotNull
    static PixelSprite dissolved(@NotNull PixelSprite sprite, boolean thin) {
        int width = sprite.width();
        int height = sprite.height();
        char[] codes = new char[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean kept = (((x + y) & 1) != 0) && (!thin || ((x & 1) == 0));
                codes[y * width + x] = kept ? sprite.codeAt(x, y) : '.';
            }
        }
        return PixelSprite.fromCodes(width, height, codes);
    }

}
