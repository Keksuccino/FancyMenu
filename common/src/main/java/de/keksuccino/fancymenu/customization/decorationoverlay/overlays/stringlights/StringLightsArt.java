package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The pixel art of the String Lights overlay: bulbs and halos of both looks, baubles, the pine garland greens and the wire tones.
 */
final class StringLightsArt {

    /** Detailed bulb, 5 x 8: socket (k, K), three-tone glass (l light side, b base, d shade) and a highlight (h). It hangs from its socket. */
    static final PixelSprite BULB = PixelSprite.of(
            ".kkk.",
            ".kKk.",
            ".lbb.",
            "lhbbd",
            "lbbbd",
            "lbbdd",
            ".bdd.",
            "..d..");
    /** Dithered diamond halos of the detailed bulb for the lit and the bright level, centered on the glass. */
    static final PixelSprite[] HALOS = {null, PixelSprite.of(
            "....a....",
            "...a.a...",
            "..a.a.a..",
            ".a.a.a.a.",
            "a.a.a.a.a",
            ".a.a.a.a.",
            "..a.a.a..",
            "...a.a...",
            "....a...."), PixelSprite.of(
            "...............",
            ".......b.......",
            "...............",
            ".....b.a.b.....",
            "......a.a......",
            "...b.a.a.a.b...",
            "....a.a.a.a....",
            ".b.a.a.a.a.a.b.",
            "....a.a.a.a....",
            "...b.a.a.a.b...",
            "......a.a......",
            ".....b.a.b.....",
            "...............",
            ".......b.......",
            "...............")};
    /** Simple look: a flat 5 px lamp that sits on the wire. */
    static final PixelSprite SIMPLE_BULB = PixelSprite.of(
            ".bbb.",
            "bhbbb",
            "bbbbb",
            "bbbbd",
            ".bbb.");
    /** Soft stepped square halos of the simple lamp for the lit and the bright level. */
    static final PixelSprite[] SIMPLE_HALOS = {null, PixelSprite.of(
            ".aaaaa.",
            "aaaaaaa",
            "aaaaaaa",
            "aaaaaaa",
            "aaaaaaa",
            "aaaaaaa",
            ".aaaaa."), PixelSprite.of(
            "..bbbbb..",
            ".baaaaab.",
            "baaaaaaab",
            "baaaaaaab",
            "baaaaaaab",
            "baaaaaaab",
            "baaaaaaab",
            ".baaaaab.",
            "..bbbbb..")};
    /** A glass bauble of the pine garland, hanging from its cap (k). */
    static final PixelSprite BAUBLE = PixelSprite.of(
            "..k..",
            ".lbb.",
            "lhbbd",
            "lbbbd",
            ".bdd.");

    static final PixelPalette BAUBLE_RED = baublePalette(0xF0483E, 0xFF9A8A, 0xA82238);
    static final PixelPalette BAUBLE_GOLD = baublePalette(0xE0A53A, 0xFFDD86, 0x9A6420);
    static final PixelPalette BAUBLE_SILVER = baublePalette(0xAEB9C8, 0xE4EAF2, 0x6E7A8E);

    /** Needle greens of the pine garland: shade, mid, lit */
    static final int PINE_SHADE = 0xFF1E5A34;
    static final int PINE_MID = 0xFF2E7A44;
    static final int PINE_LIT = 0xFF56A85A;

    /** The two greens of the twisted wire of the detailed look */
    static final int WIRE_DARK = 0xFF2C3F33;
    static final int WIRE_LIGHT = 0xFF4A6652;

    /** The default light color, warm white. Strings in this color get the hand-picked warm ramp. */
    static final int WARM_RGB = 0xFFD27A;
    static final BulbRamp WARM = new BulbRamp(WARM_RGB, 0xFFF3C4, 0xE09A3A, -1);
    /** The Christmas colors, in the fixed order they repeat in along a string: red, green, blue, amber, pink. */
    static final BulbRamp[] CHRISTMAS = {
            new BulbRamp(0xF0483E, 0xFF9A8A, 0xA82238, -1),
            new BulbRamp(0x3CC860, 0xA8F08A, 0x1E8452, -1),
            new BulbRamp(0x3E8CF0, 0x9AD2FF, 0x2A4EB0, -1),
            new BulbRamp(0xFFC23C, 0xFFE9A0, 0xD97A1E, -1),
            new BulbRamp(0xF05CA8, 0xFFB0DA, 0xA82E7E, -1)
    };

    /** Unlit bulbs blend into this dusky purple */
    private static final int DIM_MIX = 0x1C1828;
    private static final int DIM_SHADE_MIX = 0x120F1C;
    private static final int SOCKET_DARK = 0x1E2A22;
    private static final int SOCKET_LIGHT = 0x4A6650;

    private StringLightsArt() {
    }

    @NotNull
    private static PixelPalette baublePalette(int base, int light, int dark) {
        return PixelPalette.builder().rgb('b', base).rgb('l', light).rgb('h', 0xFFFFFF).rgb('d', dark).rgb('k', 0x3A2A1E).build();
    }

    /**
     * Builds the ramp of a light color picked by the user. The hand-picked warm white ramp is used for the default color,
     * other colors get a lighter and a deeper tone derived from them. The alpha of the color fades the whole bulb,
     * an alpha of 0 counts as opaque (like it always did for this option).
     */
    @NotNull
    static BulbRamp rampOf(int argb) {
        int rgb = argb & 0xFFFFFF;
        int alpha = argb >>> 24;
        int tint = ((alpha == 0) || (alpha == 255)) ? -1 : PixelMath.withAlpha(0xFFFFFF, alpha);
        if ((rgb == WARM_RGB) && (tint == -1)) return WARM;
        if (rgb == WARM_RGB) return new BulbRamp(WARM_RGB, 0xFFF3C4, 0xE09A3A, tint);
        int light = PixelMath.mix(PixelMath.opaque(rgb), 0xFFFFFFFF, 0.5F) & 0xFFFFFF;
        int dark = PixelMath.mix(PixelMath.opaque(rgb), 0xFF200030, 0.35F) & 0xFFFFFF;
        return new BulbRamp(rgb, light, dark, tint);
    }

    /**
     * All palettes of one bulb color: bulb and halo of both looks for the three brightness levels.
     * Dim bulbs keep their color, darkened, and have no halo.
     */
    static final class BulbRamp {

        final int base;
        /** Sprite tint, -1 for none */
        final int tint;
        final PixelPalette[] bulb = new PixelPalette[3];
        /** Index 0 (dim) has no halo */
        final PixelPalette[] halo = new PixelPalette[3];
        final PixelPalette[] simpleBulb = new PixelPalette[3];
        final PixelPalette[] simpleHalo = new PixelPalette[3];

        BulbRamp(int base, int light, int dark, int tint) {
            this.base = base;
            this.tint = tint;
            this.bulb[0] = socket().rgb('h', mix(base, DIM_MIX, 0.45F)).rgb('l', mix(base, DIM_MIX, 0.5F)).rgb('b', mix(base, DIM_MIX, 0.62F)).rgb('d', mix(dark, DIM_SHADE_MIX, 0.7F)).build();
            this.bulb[1] = socket().rgb('h', 0xFFFFFF).rgb('l', light).rgb('b', base).rgb('d', dark).build();
            this.bulb[2] = socket().rgb('h', 0xFFFFFF).rgb('l', mix(light, 0xFFFFFF, 0.55F)).rgb('b', light).rgb('d', base).build();
            this.halo[1] = PixelPalette.builder().rgb('a', base, 52).rgb('b', base, 30).build();
            this.halo[2] = PixelPalette.builder().rgb('a', base, 84).rgb('b', base, 46).build();
            this.simpleBulb[0] = PixelPalette.builder().rgb('b', mix(base, DIM_MIX, 0.55F)).rgb('h', mix(base, DIM_MIX, 0.45F)).rgb('d', mix(dark, DIM_SHADE_MIX, 0.65F)).build();
            this.simpleBulb[1] = PixelPalette.builder().rgb('b', base).rgb('h', light).rgb('d', dark).build();
            this.simpleBulb[2] = PixelPalette.builder().rgb('b', light).rgb('h', 0xFFFFFF).rgb('d', base).build();
            this.simpleHalo[1] = PixelPalette.builder().rgb('a', base, 62).build();
            this.simpleHalo[2] = PixelPalette.builder().rgb('a', base, 88).rgb('b', base, 44).build();
        }

        @NotNull
        private static PixelPalette.Builder socket() {
            return PixelPalette.builder().rgb('k', SOCKET_DARK).rgb('K', SOCKET_LIGHT);
        }

        private static int mix(int rgbFrom, int rgbTo, float t) {
            return PixelMath.mix(PixelMath.opaque(rgbFrom), PixelMath.opaque(rgbTo), t) & 0xFFFFFF;
        }

    }

}
