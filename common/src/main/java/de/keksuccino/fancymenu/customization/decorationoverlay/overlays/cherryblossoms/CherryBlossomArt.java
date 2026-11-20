package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The pixel art of the Cherry Blossoms overlay. Petals are heart-shaped with a notched tip, in four tones from near-white (h)
 * over the petal (b) and its shade (d) down to deep pink at the petal base (e). Whole blossoms have a yellow heart (y).
 */
final class CherryBlossomArt {

    /** The 5 px petal */
    private static final PixelSprite PETAL = PixelSprite.of(
            ".h.h.",
            "hhbhh",
            "hbbbd",
            ".bbd.",
            "..e..");
    /** The 3 px petal */
    private static final PixelSprite SMALL_PETAL = PixelSprite.of(
            "h.h",
            "hbd",
            ".e.");
    /** The 5 px petal seen edge-on, a thin sliver */
    static final PixelSprite SLIVER = PixelSprite.of(
            "h....",
            ".bb..",
            "...bd");
    /** The 3 px petal seen edge-on */
    static final PixelSprite SMALL_SLIVER = PixelSprite.of(
            "hb.",
            ".bd");
    /** A whole 7 px blossom */
    private static final PixelSprite BLOSSOM = PixelSprite.of(
            "...h...",
            "..hbh..",
            "hh.e.hh",
            "hbeyebd",
            ".bd.bd.",
            ".hb.bd.",
            "..b.d..");
    /** A blossom cluster on a branch */
    static final PixelSprite CLUSTER = PixelSprite.of(
            ".hh..",
            "hbbh.",
            "hbebd",
            ".bbd.",
            "..d..");
    /** Petals in four quarter turns */
    static final PixelSprite[] PETAL_TURNS = quarterTurns(PETAL);
    static final PixelSprite[] SMALL_PETAL_TURNS = quarterTurns(SMALL_PETAL);
    /** A whole blossom, straight and turned a quarter */
    static final PixelSprite[] BLOSSOM_TURNS = {BLOSSOM, BLOSSOM.rotatedClockwise()};

    static final PixelPalette FACE = PixelPalette.builder().rgb('h', 0xFFF2F6).rgb('b', 0xFFC4D8).rgb('d', 0xF08DB2).rgb('e', 0xC9587F).rgb('y', 0xF6D365).build();
    /** The darker underside of a petal */
    static final PixelPalette BACK = PixelPalette.builder().rgb('h', 0xFFC4D8).rgb('b', 0xF5A3C2).rgb('d', 0xDB7AA2).rgb('e', 0xB04A72).rgb('y', 0xE0B050).build();
    private static final PixelPalette PALE = PixelPalette.builder().rgb('h', 0xFFFFFF).rgb('b', 0xFFE1EB).rgb('d', 0xFFC4D8).rgb('e', 0xF08DB2).rgb('y', 0xF6D365).build();
    private static final PixelPalette DEEP = PixelPalette.builder().rgb('h', 0xFFC4D8).rgb('b', 0xF08DB2).rgb('d', 0xD9648F).rgb('e', 0xA83E68).rgb('y', 0xE0B050).build();
    /** Branch clusters in three tones, indexed by tone 0 to 3 (deep, two normal ones, pale) */
    static final PixelPalette[] CLUSTER_TONES = {DEEP, FACE, FACE, PALE};
    /** Simple look: petals in two flat tones */
    static final PixelPalette SIMPLE_FACE = PixelPalette.builder().rgb('h', 0xFFC4D8).rgb('b', 0xFFC4D8).rgb('d', 0xF08DB2).rgb('e', 0xF08DB2).build();
    /** Simple look: flat clusters, indexed like {@link #CLUSTER_TONES} */
    static final PixelPalette[] SIMPLE_CLUSTER_TONES = {flatCluster(0xF08DB2), flatCluster(0xFFC4D8), flatCluster(0xFFC4D8), flatCluster(0xFFE1EB)};

    /** The pink carpet on surfaces */
    static final int[] CARPET = {PixelMath.opaque(0xFFF2F6), PixelMath.opaque(0xFFC4D8), PixelMath.opaque(0xF08DB2)};
    /** The two tones of the far specks */
    static final int SPECK_DARK = PixelMath.opaque(0xF2A9C4);
    static final int SPECK_LIGHT = PixelMath.opaque(0xFFD3E1);
    /** Bark: lit top row, body, shaded bottom row */
    static final int BARK_LIT = PixelMath.opaque(0x7A4A58);
    static final int BARK = PixelMath.opaque(0x5A3644);
    static final int BARK_SHADE = PixelMath.opaque(0x3A2230);
    /** Color of the wind strokes */
    static final int STROKE_RGB = 0xFFF0F5;

    private CherryBlossomArt() {
    }

    @NotNull
    private static PixelPalette flatCluster(int rgb) {
        return PixelPalette.builder().rgb('h', rgb).rgb('b', rgb).rgb('d', rgb).rgb('e', 0xC9587F).build();
    }

    @NotNull
    private static PixelSprite[] quarterTurns(@NotNull PixelSprite sprite) {
        PixelSprite one = sprite.rotatedClockwise();
        PixelSprite two = one.rotatedClockwise();
        return new PixelSprite[]{sprite, one, two, two.rotatedClockwise()};
    }

}
