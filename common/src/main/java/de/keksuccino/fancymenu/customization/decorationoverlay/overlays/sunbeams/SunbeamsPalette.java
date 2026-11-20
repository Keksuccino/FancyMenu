package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import org.jetbrains.annotations.NotNull;

/**
 * Every color of the sunbeams, made from the sunlight color option.<br><br>
 *
 * The natural colors are warm tones of one hue that only differ in how saturated they are: the faint outer haze, the white core,
 * the gold edges of the elements, the glints and the dust. Another sunlight color keeps those differences: each tone gets the new color's
 * hue and brightness, with its saturation scaled like the natural tone's against the natural sunlight color. The natural sunlight
 * color gives exactly the natural tones.
 */
final class SunbeamsPalette {

    static final int NATURAL_LIGHT = 0xFFE9A6;
    /** Light tones from the faint outer haze to the white core, and their opacities. */
    private static final int[] NATURAL_TONES = {0xFFEDB4, 0xFFE9A6, 0xFFEBAA, 0xFFF2C4, 0xFFFAE2};
    private static final int[] TONE_ALPHAS = {28, 48, 70, 94, 122};
    private static final int NATURAL_RIM = 0xFFE08A;
    /** Opacity of a lit element edge at every light level, its bevel pixel gets 35% of it. */
    private static final int[] RIM_ALPHAS = {0, 150, 185, 215, 240, 255};
    private static final int NATURAL_GLINT_OUTER = 0xFFE9B0;
    private static final int NATURAL_GLINT_INNER = 0xFFF6D8;
    private static final int NATURAL_MOTE_HALO = 0xFFEFC0;
    private static final int NATURAL_MOTE_CORE = 0xFFF8E0;
    private static final int NATURAL_MOTE_DOT = 0xFFF1C4;
    /** Opacity of a dust mote's core at light levels 1 to 5, its halo gets 42% of it. */
    private static final int[] MOTE_ALPHAS = {70, 105, 140, 175, 205};
    /** Opacity of a one-pixel dust mote at every light level. */
    private static final int[] MOTE_DOT_ALPHAS = {0, 110, 160, 205, 235, 255};

    static final SunbeamsPalette NATURAL = new SunbeamsPalette(NATURAL_LIGHT);

    final int rgb;
    /** ARGB of the light at every level, 0 for none */
    final int[] light = new int[6];
    /** ARGB of a lit edge pixel and of the bevel pixel under it at every level */
    final int[] rim = new int[6];
    final int[] rimBevel = new int[6];
    @NotNull
    final PixelPalette glint;
    /** Palettes of the three-pixel dust mote at every level, NULL at level 0 */
    final PixelPalette[] mote = new PixelPalette[6];
    /** ARGB of a one-pixel dust mote at every level */
    final int[] moteDot = new int[6];

    private SunbeamsPalette(int rgb) {
        this.rgb = rgb;
        for (int l = 1; l <= 5; l++) this.light[l] = PixelMath.withAlpha(this.tone(NATURAL_TONES[l - 1]), TONE_ALPHAS[l - 1]);
        int rimColor = this.tone(NATURAL_RIM);
        for (int l = 1; l <= 5; l++) {
            this.rim[l] = PixelMath.withAlpha(rimColor, RIM_ALPHAS[l]);
            this.rimBevel[l] = PixelMath.withAlpha(rimColor, Math.round(RIM_ALPHAS[l] * 0.35F));
        }
        this.glint = PixelPalette.builder().rgb('1', this.tone(NATURAL_GLINT_OUTER), 150).rgb('2', this.tone(NATURAL_GLINT_INNER), 235).rgb('3', 0xFFFFFF).build();
        int moteHalo = this.tone(NATURAL_MOTE_HALO);
        int moteCore = this.tone(NATURAL_MOTE_CORE);
        int moteDot = this.tone(NATURAL_MOTE_DOT);
        for (int l = 1; l <= 5; l++) {
            int alpha = MOTE_ALPHAS[l - 1];
            this.mote[l] = PixelPalette.builder().rgb('1', moteHalo, Math.round(alpha * 0.42F)).rgb('2', moteCore, alpha).build();
            this.moteDot[l] = PixelMath.withAlpha(moteDot, MOTE_DOT_ALPHAS[l]);
        }
    }

    /**
     * Returns the palette of a sunlight color (RGB, alpha ignored).
     */
    @NotNull
    static SunbeamsPalette of(int rgb) {
        rgb &= 0xFFFFFF;
        return (rgb == NATURAL_LIGHT) ? NATURAL : new SunbeamsPalette(rgb);
    }

    /**
     * Moves a natural tone to this palette's sunlight color.
     */
    private int tone(int naturalTone) {
        return tint(naturalTone, this.rgb);
    }

    /**
     * Gives a natural tone the hue and brightness of a sunlight color. The tone keeps how much more or less saturated it is than the
     * natural sunlight color: scaling every channel's distance to the brightest channel scales the saturation and keeps hue and brightness.
     */
    static int tint(int naturalTone, int lightRgb) {
        if (lightRgb == NATURAL_LIGHT) return naturalTone;
        float ratio = saturation(naturalTone) / saturation(NATURAL_LIGHT);
        int max = Math.max(PixelMath.red(lightRgb), Math.max(PixelMath.green(lightRgb), PixelMath.blue(lightRgb)));
        int r = Math.round(max - (max - PixelMath.red(lightRgb)) * ratio);
        int g = Math.round(max - (max - PixelMath.green(lightRgb)) * ratio);
        int b = Math.round(max - (max - PixelMath.blue(lightRgb)) * ratio);
        return PixelMath.argb(0, r, g, b);
    }

    private static float saturation(int rgb) {
        int max = Math.max(PixelMath.red(rgb), Math.max(PixelMath.green(rgb), PixelMath.blue(rgb)));
        int min = Math.min(PixelMath.red(rgb), Math.min(PixelMath.green(rgb), PixelMath.blue(rgb)));
        return (max > 0) ? ((max - min) / (float)max) : 0.0F;
    }

}
