package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.aurora;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;

/**
 * The colors of the aurora and the finished pixel colors of every combination of wash and ray a curtain pixel can have.<br><br>
 *
 * The natural aurora runs from a mint green lower edge through teal and blue up to violet ray tips. Custom colors keep all of its
 * relationships: every tone gets moved along the color wheel by how far it sits between the natural edge and tip colors, and its
 * saturation and brightness get scaled the same way. With the natural edge and tip colors, the tones are exactly the natural ones.
 */
final class AuroraPalette {

    /** Natural tones from the bright lower edge (0, 1 are its highlights, 2 is the edge itself) up to the violet ray tips (8). */
    static final int[] NATURAL_RAMP = {0xDCFFEA, 0x9CFFC6, 0x58F2A2, 0x30D79E, 0x28B4AE, 0x3A8ECC, 0x5A6AD6, 0x8656CE, 0xAA52BC};
    /** The faint wash behind the rays: its lower and its upper half. */
    static final int NATURAL_WASH_LOW = 0x1FA98A;
    static final int NATURAL_WASH_HIGH = 0x2A6FB0;
    static final int NATURAL_EDGE = NATURAL_RAMP[2];
    static final int NATURAL_TIP = NATURAL_RAMP[8];

    /** Wash states: none, then lower and upper half, each in its core, thin, core-at-top and thin-at-top opacity. */
    static final int WASH_STATES = 9;
    private static final int[] WASH_ALPHAS = {26, 15, 13, 8};
    /** Ray states: none, the 9 tones in 6 opacity steps each, then the two glow pixels under the edge. */
    static final int RAY_STATES = 1 + NATURAL_RAMP.length * 6 + 2;
    static final int RAY_GLOW_NEAR = RAY_STATES - 2;
    static final int RAY_GLOW_FAR = RAY_STATES - 1;
    private static final int RAY_ALPHA_STEP = 34;

    static final AuroraPalette NATURAL = new AuroraPalette(NATURAL_RAMP.clone(), NATURAL_WASH_LOW, NATURAL_WASH_HIGH);

    /** The tones in use, RGB */
    final int[] ramp;
    final int washLow;
    final int washHigh;
    /** ARGB of a pixel with wash state {@code w} and ray state {@code r} at {@code w * RAY_STATES + r}, the ray painted over the wash. */
    final int[] composite = new int[WASH_STATES * RAY_STATES];

    private AuroraPalette(@NotNull int[] ramp, int washLow, int washHigh) {
        this.ramp = ramp;
        this.washLow = washLow;
        this.washHigh = washHigh;
        for (int w = 0; w < WASH_STATES; w++) {
            int wash = this.washColor(w);
            for (int r = 0; r < RAY_STATES; r++) {
                this.composite[w * RAY_STATES + r] = PixelMath.blendOver(wash, this.rayColor(r));
            }
        }
    }

    /**
     * Returns the palette for an edge and a tip color (RGB, alpha ignored).
     */
    @NotNull
    static AuroraPalette of(int edgeRgb, int tipRgb) {
        edgeRgb &= 0xFFFFFF;
        tipRgb &= 0xFFFFFF;
        if ((edgeRgb == NATURAL_EDGE) && (tipRgb == NATURAL_TIP)) return NATURAL;
        Shift shift = new Shift(edgeRgb, tipRgb);
        int[] ramp = new int[NATURAL_RAMP.length];
        for (int i = 0; i < ramp.length; i++) ramp[i] = shift.apply(NATURAL_RAMP[i]);
        return new AuroraPalette(ramp, shift.apply(NATURAL_WASH_LOW), shift.apply(NATURAL_WASH_HIGH));
    }

    /**
     * The wash state of a pixel {@code k} rows above the curtain's edge.
     */
    static int washState(boolean upperHalf, boolean inCore, boolean nearTop) {
        return 1 + (upperHalf ? 4 : 0) + (inCore ? 0 : 1) + (nearTop ? 2 : 0);
    }

    /**
     * The ray state of a tone (0 to 8) in an opacity step (1 to 6).
     */
    static int rayState(int tone, int step) {
        return 1 + tone * 6 + (step - 1);
    }

    private int washColor(int state) {
        if (state == 0) return 0;
        int s = state - 1;
        return PixelMath.withAlpha((s < 4) ? this.washLow : this.washHigh, WASH_ALPHAS[s & 3]);
    }

    private int rayColor(int state) {
        if (state == 0) return 0;
        if (state == RAY_GLOW_NEAR) return PixelMath.withAlpha(this.ramp[2], 66);
        if (state == RAY_GLOW_FAR) return PixelMath.withAlpha(this.ramp[3], 33);
        int s = state - 1;
        return PixelMath.withAlpha(this.ramp[s / 6], ((s % 6) + 1) * RAY_ALPHA_STEP);
    }

    /**
     * Moves natural tones to custom edge and tip colors in HSV space.
     */
    private static final class Shift {

        private final float naturalEdgeHue;
        private final float naturalSpan;
        private final float edgeHue;
        private final float span;
        private final float edgeSaturationRatio;
        private final float tipSaturationRatio;
        private final float edgeValueRatio;
        private final float tipValueRatio;

        private Shift(int edgeRgb, int tipRgb) {
            float[] naturalEdge = toHsv(NATURAL_EDGE);
            float[] naturalTip = toHsv(NATURAL_TIP);
            float[] edge = toHsv(edgeRgb);
            float[] tip = toHsv(tipRgb);
            this.naturalEdgeHue = naturalEdge[0];
            this.naturalSpan = hueDifference(naturalEdge[0], naturalTip[0]);
            this.edgeHue = edge[0];
            // Custom colors go the shorter way around the color wheel, like the natural ones do
            this.span = hueDifference(edge[0], tip[0]);
            this.edgeSaturationRatio = edge[1] / naturalEdge[1];
            this.tipSaturationRatio = tip[1] / naturalTip[1];
            this.edgeValueRatio = edge[2] / naturalEdge[2];
            this.tipValueRatio = tip[2] / naturalTip[2];
        }

        private int apply(int naturalRgb) {
            float[] hsv = toHsv(naturalRgb);
            float offset = hueDifference(this.naturalEdgeHue, hsv[0]);
            float progress = PixelMath.clamp(offset / this.naturalSpan, 0.0F, 1.0F);
            float hue = this.edgeHue + offset * (this.span / this.naturalSpan);
            float saturation = PixelMath.clamp(hsv[1] * PixelMath.lerp(this.edgeSaturationRatio, this.tipSaturationRatio, progress), 0.0F, 1.0F);
            float value = PixelMath.clamp(hsv[2] * PixelMath.lerp(this.edgeValueRatio, this.tipValueRatio, progress), 0.0F, 1.0F);
            return fromHsv(hue, saturation, value);
        }

    }

    /**
     * The signed shortest way from one hue to another, in degrees from -180 to 180.
     */
    static float hueDifference(float fromHue, float toHue) {
        float d = (toHue - fromHue) % 360.0F;
        if (d > 180.0F) d -= 360.0F;
        if (d <= -180.0F) d += 360.0F;
        return d;
    }

    /**
     * Returns hue (degrees, 0 to 360), saturation and value (0 to 1) of an RGB color.
     */
    @NotNull
    static float[] toHsv(int rgb) {
        float r = PixelMath.red(rgb) / 255.0F;
        float g = PixelMath.green(rgb) / 255.0F;
        float b = PixelMath.blue(rgb) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0.0F;
        if (delta > 0.0F) {
            if (max == r) hue = 60.0F * (((g - b) / delta) % 6.0F);
            else if (max == g) hue = 60.0F * ((b - r) / delta + 2.0F);
            else hue = 60.0F * ((r - g) / delta + 4.0F);
        }
        if (hue < 0.0F) hue += 360.0F;
        return new float[]{hue, (max > 0.0F) ? (delta / max) : 0.0F, max};
    }

    /**
     * Returns the RGB color of a hue (degrees, any value), saturation and value (0 to 1).
     */
    static int fromHsv(float hue, float saturation, float value) {
        float h = ((hue % 360.0F) + 360.0F) % 360.0F / 60.0F;
        float c = value * saturation;
        float x = c * (1.0F - Math.abs((h % 2.0F) - 1.0F));
        float m = value - c;
        float r;
        float g;
        float b;
        switch ((int)h) {
            case 0 -> { r = c; g = x; b = 0.0F; }
            case 1 -> { r = x; g = c; b = 0.0F; }
            case 2 -> { r = 0.0F; g = c; b = x; }
            case 3 -> { r = 0.0F; g = x; b = c; }
            case 4 -> { r = x; g = 0.0F; b = c; }
            default -> { r = c; g = 0.0F; b = x; }
        }
        return (Math.round((r + m) * 255.0F) << 16) | (Math.round((g + m) * 255.0F) << 8) | Math.round((b + m) * 255.0F);
    }

}
