package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.aurora;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * One aurora curtain. Its lower edge waves slowly, and every column is one ray that rises from it: brightness comes in slow patches
 * along the curtain, ray height and strength vary from column to column, and where the edge runs steeply the curtain is seen edge-on,
 * so it gets brighter and taller. Toward its ends a curtain tapers out. Opacity uses six flat steps over a faint wash.<br><br>
 *
 * Vertical sizes are given for the 270 px high mockup stage and get scaled to the screen height. Horizontal sizes are GUI pixels,
 * so wider screens show more of the curtain instead of stretching its rays.
 */
final class AuroraCurtain {

    /** Resolution of the tables over the height of a ray. */
    private static final int RAY_STEPS = 4096;
    /** {@code pow(1 - u, 1.2)}: how the strength of a ray falls off toward its tip, at {@code u * RAY_STEPS}. */
    private static final float[] RAY_FALLOFF = new float[RAY_STEPS + 1];
    /** The ray state of the tone at a height of a ray ({@code min(8, floor(pow(u, 0.8) * 9))}) minus its step, at {@code u * RAY_STEPS}. */
    private static final int[] RAY_TONE_STATE = new int[RAY_STEPS + 1];

    static {
        for (int i = 0; i <= RAY_STEPS; i++) {
            double u = i / (double)RAY_STEPS;
            RAY_FALLOFF[i] = (float)Math.pow(1.0 - u, 1.2);
            RAY_TONE_STATE[i] = AuroraPalette.rayState(Math.min(8, (int)Math.floor(Math.pow(u, 0.8) * 9.0)), 0);
        }
    }

    /** Stage row of the lower edge */
    private final float baseY;
    /** Stage rows the edge waves up and down */
    private final float amplitude;
    /** Radians per pixel of the edge wave */
    private final float frequency;
    /** Radians per second the edge wave moves */
    private final float speed;
    /** Stage rows of the tallest rays */
    private final float rayHeight;
    private final float phase;
    /** 1 or -1: which way the bright patches and the ray pattern travel */
    private final int direction;
    private final float strength;

    AuroraCurtain(float baseY, float amplitude, float frequency, float speed, float rayHeight, float phase, int direction, float strength) {
        this.baseY = baseY;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.speed = speed;
        this.rayHeight = rayHeight;
        this.phase = phase;
        this.direction = direction;
        this.strength = strength;
    }

    /**
     * The lowest stage row the curtain's glow can reach.
     */
    float lowestStageRow() {
        return this.baseY + this.amplitude * 1.45F;
    }

    /**
     * Replaces the {@code width} by {@code rows} ARGB pixels with the curtain at a time.
     *
     * @param scaleY screen height divided by the stage height
     */
    void render(@NotNull int[] pixels, int width, int rows, float time, float scaleY, @NotNull AuroraPalette palette) {
        Arrays.fill(pixels, 0, width * rows, 0);
        int[] composite = palette.composite;
        float scaledHeight = this.rayHeight * scaleY;
        // The edge is two sine waves along the columns. Stepping their angles with the angle addition formulas
        // replaces two sine calls per column with a few multiplications.
        double mainAngle = time * this.speed + this.phase;
        double rippleAngle = -time * this.speed * 1.3F + this.phase * 2.3F;
        double mainSin = Math.sin(mainAngle);
        double mainCos = Math.cos(mainAngle);
        double rippleSin = Math.sin(rippleAngle);
        double rippleCos = Math.cos(rippleAngle);
        double mainStepSin = Math.sin(this.frequency);
        double mainStepCos = Math.cos(this.frequency);
        double rippleStepSin = Math.sin(this.frequency * 2.7F);
        double rippleStepCos = Math.cos(this.frequency * 2.7F);
        float rippleAmplitude = this.amplitude * 0.45F;
        float nextEdge = this.baseY + this.amplitude * (float)mainSin + rippleAmplitude * (float)rippleSin;
        for (int x = 0; x < width; x++) {
            float edge = nextEdge;
            double sin = mainSin * mainStepCos + mainCos * mainStepSin;
            mainCos = mainCos * mainStepCos - mainSin * mainStepSin;
            mainSin = sin;
            sin = rippleSin * rippleStepCos + rippleCos * rippleStepSin;
            rippleCos = rippleCos * rippleStepCos - rippleSin * rippleStepSin;
            rippleSin = sin;
            nextEdge = this.baseY + this.amplitude * (float)mainSin + rippleAmplitude * (float)rippleSin;
            float intensity = PixelMath.clamp((PixelMath.valueNoise(x * 0.011F + time * 0.045F * this.direction, this.phase * 3.1F) - 0.14F) * 2.3F, 0.0F, 1.0F) * this.strength;
            if (intensity <= 0.0F) continue;
            float fold = PixelMath.clamp(Math.abs(nextEdge - edge) * 1.7F, 0.0F, 1.0F);
            float ray = 0.55F * PixelMath.valueNoise(x * 0.42F + time * 0.3F * this.direction, this.phase + 7.0F) + 0.45F * PixelMath.valueNoise(x * 0.11F - time * 0.07F, this.phase + 13.0F);
            float brightness = intensity * (0.55F + 0.75F * ray) * (1.0F + fold * 0.8F);
            int base = Math.round(edge * scaleY);
            // Where a curtain runs out, its rays sink toward the edge instead of ending in a straight vertical cut. tail is 1 in the strong
            // part and falls to 0 at the very end. Inside that stretch the lower part of every ray keeps the faintest opacity step.
            float tail = PixelMath.smoothstep(intensity / 0.45F);
            float height = scaledHeight * (0.5F + 0.5F * intensity) * (0.55F + 0.8F * ray) * (1.0F + fold * 0.4F) * tail;
            if (height < 1.0F) continue;
            float keep = 0.45F * PixelMath.clamp((1.0F - tail) * 3.0F, 0.0F, 1.0F);
            // A faint wash behind the rays gives the curtain body. Its denser core only rises where the curtain is strong.
            float wash = height * 1.15F;
            float core = wash * PixelMath.clamp((intensity - 0.42F) / 0.25F, 0.0F, 1.0F);
            // Rows above the edge where the wash changes: its bottom and top, its upper half, the end of its core and its faint top
            int washFrom = -Math.min(2, (int)wash);
            int washTo = (int)Math.ceil(wash);
            int washUpper = (int)Math.ceil(height * 0.5F);
            int washCoreEnd = (int)Math.ceil(core);
            int washFaint = (int)Math.floor(wash * 0.85F) + 1;
            int rayTo = (int)Math.ceil(height);
            float inverseHeight = 1.0F / height;
            float steps = brightness * 6.2F;
            int glowNear = (brightness > 0.5F) ? AuroraPalette.RAY_GLOW_NEAR : 0;
            int glowFar = (brightness > 0.8F) ? AuroraPalette.RAY_GLOW_FAR : 0;
            // k counts rows up from the edge. The loops only visit rows inside of the pixels, and each one covers a stretch of rows
            // where the same things can show, so they need no checks for the others.
            int kFrom = Math.max(-2, base - rows + 1);
            int kTo = Math.min(Math.max(washTo, rayTo), base + 1);
            int index = (base - kFrom) * width + x;
            int k = kFrom;
            // The two glow pixels under the edge
            for (; (k < 0) && (k < kTo); k++, index -= width) {
                int washState = (k >= washFrom) ? washState(k, washUpper, washCoreEnd, washFaint) : 0;
                pixels[index] = composite[washState * AuroraPalette.RAY_STATES + ((k == -1) ? glowNear : glowFar)];
            }
            // The two edge rows are the brightest and whitest part of a ray
            for (; (k < 2) && (k < kTo); k++, index -= width) {
                int rayState = 0;
                if (k < rayTo) {
                    int step = Math.min(6, (int)(steps * 1.35F));
                    if ((step < 1) && (k * inverseHeight < keep)) step = 1;
                    if (step > 0) rayState = AuroraPalette.rayState((step >= 5) ? 0 : ((step >= 3) ? 1 : 2), step);
                }
                int washState = (k < washTo) ? washState(k, washUpper, washCoreEnd, washFaint) : 0;
                pixels[index] = composite[washState * AuroraPalette.RAY_STATES + rayState];
            }
            // The ray over the wash, the ray always ends below the top of the wash
            int rayEnd = Math.min(rayTo, kTo);
            for (; k < rayEnd; k++, index -= width) {
                float u = k * inverseHeight;
                int at = (int)(u * RAY_STEPS);
                int step = (int)(steps * RAY_FALLOFF[at]);
                if (step > 6) step = 6;
                if ((step < 1) && (u < keep)) step = 1;
                int rayState = (step > 0) ? (RAY_TONE_STATE[at] + step) : 0;
                pixels[index] = composite[washState(k, washUpper, washCoreEnd, washFaint) * AuroraPalette.RAY_STATES + rayState];
            }
            // Only the wash above the ray
            for (; k < kTo; k++, index -= width) {
                pixels[index] = composite[washState(k, washUpper, washCoreEnd, washFaint) * AuroraPalette.RAY_STATES];
            }
        }
    }

    /**
     * The wash state of a row inside of the wash, from the rows where its upper half, the end of its core and its faint top begin.
     */
    private static int washState(int k, int upper, int coreEnd, int faint) {
        return 1 + ((k >= upper) ? 4 : 0) + ((k >= coreEnd) ? 1 : 0) + ((k >= faint) ? 2 : 0);
    }

}
