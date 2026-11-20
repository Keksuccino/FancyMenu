package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The pixel art of the Fireflies overlay. A firefly glows in three brightness steps: 1 is a faint glow, 2 the fading light, 3 the flash.
 * Its colors follow the Firefly Color option. The default color gets the hand-picked colors, other colors a ramp derived from them.
 */
final class FireflyArt {

    /** The default Firefly Color */
    static final int DEFAULT_RGB = 0xFFE08A;

    /** Detailed look, indexed by brightness: a hot core (c), a plus-shaped body light (y) and a dithered halo (g, h, o). */
    static final PixelSprite[] HALOS = {null, PixelSprite.of(
            ".y.",
            "ycy",
            ".y."), PixelSprite.of(
            "..g..",
            "..y..",
            "gycyg",
            "..y..",
            "..g.."), PixelSprite.of(
            "....o....",
            "....h....",
            "..o.g.o..",
            "..hgygh..",
            "o.gycyg.o",
            "..hgygh..",
            "..o.g.o..",
            "....h....",
            "....o....")};
    /** Simple look, indexed by [big][brightness]: a 1 px or 2 px square (c) with a soft stepped square halo (a, b). */
    static final PixelSprite[][] SIMPLE = {{null, PixelSprite.of(
            "c"), PixelSprite.of(
            "aaa",
            "aca",
            "aaa"), PixelSprite.of(
            ".bbb.",
            "baaab",
            "bacab",
            "baaab",
            ".bbb.")}, {null, PixelSprite.of(
            "cc",
            "cc"), PixelSprite.of(
            "aaaa",
            "acca",
            "acca",
            "aaaa"), PixelSprite.of(
            ".bbbb.",
            "baaaab",
            "baccab",
            "baccab",
            "baaaab",
            ".bbbb.")}};

    /** The dark body of an unlit firefly and its flickering wings */
    static final int UNLIT_BODY = PixelMath.withAlpha(0x171F0E, 215);
    static final int UNLIT_WINGS = PixelMath.withAlpha(0x55664A, 150);
    /** Alphas of the three trail dots, newest first */
    static final int[] TRAIL_ALPHAS = {120, 75, 40};

    static final FireflyArt DEFAULT = new FireflyArt(PixelMath.opaque(DEFAULT_RGB),
            new PixelPalette[]{null,
                    PixelPalette.builder().rgb('c', 0xD9DC66).rgb('y', 0x97B845, 130).build(),
                    PixelPalette.builder().rgb('c', 0xFFF7B8).rgb('y', 0xEEE266, 225).rgb('g', 0xB8DC58, 120).build(),
                    PixelPalette.builder().rgb('c', 0xFFFDE4).rgb('y', 0xFFF27E).rgb('g', 0xD9F06A, 170).rgb('h', 0xA8D24E, 105).rgb('o', 0xA8D24E, 60).build()},
            new PixelPalette[]{null,
                    PixelPalette.builder().rgb('c', 0xD9C46A, 215).build(),
                    PixelPalette.builder().rgb('c', 0xFFE08A).rgb('a', 0xFFE08A, 60).build(),
                    PixelPalette.builder().rgb('c', 0xFFFDE4).rgb('a', 0xFFE08A, 100).rgb('b', 0xFFE08A, 42).build()},
            0xA9C44A, PixelMath.opaque(0xE6E67A), PixelMath.withAlpha(0x9BAF4A, 200));

    /** The Firefly Color this art was made for, ARGB */
    final int color;
    /** Sprite tint for translucent colors, -1 for none */
    final int tint;
    final PixelPalette[] detailed;
    final PixelPalette[] simple;
    /** RGB of the trail of the detailed look */
    final int trailRgb;
    /** Far fireflies: one pixel, bright while flashing and dimmer while fading */
    final int farBright;
    final int farLit;

    private FireflyArt(int color, @NotNull PixelPalette[] detailed, @NotNull PixelPalette[] simple, int trailRgb, int farBright, int farLit) {
        this.color = color;
        int alpha = color >>> 24;
        this.tint = ((alpha == 0) || (alpha == 255)) ? -1 : PixelMath.withAlpha(0xFFFFFF, alpha);
        this.detailed = detailed;
        this.simple = simple;
        this.trailRgb = trailRgb;
        this.farBright = farBright;
        this.farLit = farLit;
    }

    /**
     * The art for a Firefly Color. The alpha of the color fades the fireflies, an alpha of 0 counts as opaque (like it always did).
     */
    @NotNull
    static FireflyArt of(int argb) {
        int rgb = argb & 0xFFFFFF;
        if (rgb == DEFAULT_RGB) {
            if (((argb >>> 24) == 255) || ((argb >>> 24) == 0)) return DEFAULT;
            return new FireflyArt(argb, DEFAULT.detailed, DEFAULT.simple, DEFAULT.trailRgb, DEFAULT.farBright, DEFAULT.farLit);
        }
        PixelPalette[] detailed = {null,
                PixelPalette.builder().rgb('c', shade(rgb, 0.15F)).rgb('y', shade(rgb, 0.4F), 130).build(),
                PixelPalette.builder().rgb('c', light(rgb, 0.55F)).rgb('y', light(rgb, 0.1F), 225).rgb('g', shade(rgb, 0.2F), 120).build(),
                PixelPalette.builder().rgb('c', light(rgb, 0.85F)).rgb('y', light(rgb, 0.3F)).rgb('g', rgb, 170).rgb('h', shade(rgb, 0.25F), 105).rgb('o', shade(rgb, 0.25F), 60).build()};
        PixelPalette[] simple = {null,
                PixelPalette.builder().rgb('c', mix(rgb, 0x80831F, 0.3F), 215).build(),
                PixelPalette.builder().rgb('c', rgb).rgb('a', rgb, 60).build(),
                PixelPalette.builder().rgb('c', light(rgb, 0.8F)).rgb('a', rgb, 100).rgb('b', rgb, 42).build()};
        return new FireflyArt(argb, detailed, simple, shade(rgb, 0.3F), PixelMath.opaque(light(rgb, 0.1F)), PixelMath.withAlpha(shade(rgb, 0.35F), 200));
    }

    private static int light(int rgb, float t) {
        return mix(rgb, 0xFFFFFF, t);
    }

    private static int shade(int rgb, float t) {
        return mix(rgb, 0x000000, t);
    }

    private static int mix(int rgbFrom, int rgbTo, float t) {
        return PixelMath.mix(PixelMath.opaque(rgbFrom), PixelMath.opaque(rgbTo), t) & 0xFFFFFF;
    }

}
