package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * The rays of the sun and the small tables over the direction they fill every frame: how strong the light is in every direction bin
 * of the {@link SunbeamsRig} and how far it reaches.<br><br>
 *
 * Each ray keeps to its own slice of the fan. It swells, fades out completely and comes back at a new angle with a new width and reach,
 * so the picture keeps changing. A slow noise splits the rays into finer streaks that wander, like light through moving leaves.
 * Every ray is a spear of nested flat tones: the light steps of a pixel come from its strength, which falls off toward the ray's
 * reach and toward its sides, so each ray ends in a point.
 */
final class SunbeamsRays {

    /** The mockup's fan of 67.5 degrees holds 14 rays, other fans get as many per degree. */
    private static final int STAGE_RAYS = 14;
    private static final float STAGE_FAN = (float)Math.toRadians(67.5);
    private static final float DEGREES = (float)(Math.PI / 180.0);
    /** The light steps start at 0.14, 0.3, 0.48, 0.68 and 0.9. All of them are whole fiftieths, so a table over fiftieths gives the step. */
    private static final int STEP_SCALE = 50;
    private static final byte[] STEPS = new byte[STEP_SCALE + 1];
    /** Resolution of the power tables, which replace two square roots per pixel. */
    private static final int POWER_STEPS = 4096;
    /** {@code f ^ 1.25}: how a ray fades toward its reach, at {@code f * POWER_STEPS}. */
    private static final float[] FADE = new float[POWER_STEPS + 1];

    static {
        for (int i = 0; i <= POWER_STEPS; i++) {
            double f = i / (double)POWER_STEPS;
            FADE[i] = (float)(f * Math.sqrt(Math.sqrt(f)));
        }
        int[] starts = {7, 15, 24, 34, 45};
        for (int i = 0; i <= STEP_SCALE; i++) {
            int step = 0;
            for (int start : starts) {
                if (i >= start) step++;
            }
            STEPS[i] = (byte)step;
        }
    }

    /** Light strength of every direction bin, 0 where no ray shines */
    final float[] strength = new float[SunbeamsRig.BINS];
    /** 1 divided by how far the light of every direction bin reaches */
    final float[] inverseReach = new float[SunbeamsRig.BINS];

    private int count = 0;
    /** Per ray: direction from the start of the fan, half width (radians), strength, reach (pixels), seconds per life, wobble phase,
     * progress through its life (0 to 1) and the current envelope (0 to 1) */
    private float[] angle = new float[0];
    private float[] halfWidth = new float[0];
    private float[] rayStrength = new float[0];
    private float[] reach = new float[0];
    private float[] period = new float[0];
    private float[] phase = new float[0];
    private float[] progress = new float[0];
    private float[] envelope = new float[0];

    /**
     * Starts new rays for a fan of directions, each at a random point of its life.
     *
     * @param scale screen diagonal divided by the mockup stage's diagonal
     */
    void setup(float fanWidth, float scale, @NotNull PixelRandom random) {
        int rays = Math.max(4, Math.round(STAGE_RAYS * fanWidth / STAGE_FAN));
        this.count = rays;
        this.angle = new float[rays];
        this.halfWidth = new float[rays];
        this.rayStrength = new float[rays];
        this.reach = new float[rays];
        this.period = new float[rays];
        this.phase = new float[rays];
        this.progress = new float[rays];
        this.envelope = new float[rays];
        for (int i = 0; i < rays; i++) {
            this.roll(i, fanWidth, scale, random);
            this.progress[i] = random.next();
        }
    }

    int count() {
        return this.count;
    }

    /**
     * Gives a ray a new angle in its slice of the fan, a new width, strength and reach. Wide rays are a bit weaker but reach further.
     */
    private void roll(int i, float fanWidth, float scale, @NotNull PixelRandom random) {
        float kind = random.next();
        this.angle[i] = fanWidth * (i + 0.5F) / this.count + random.range(-2.0F, 2.0F) * DEGREES;
        this.period[i] = random.range(16.0F, 34.0F);
        this.phase[i] = random.range(0.0F, PixelMath.TAU);
        if (kind < 0.25F) {
            this.halfWidth[i] = random.range(2.6F, 3.6F) * DEGREES;
            this.rayStrength[i] = random.range(0.75F, 1.0F);
            this.reach[i] = random.range(760.0F, 1150.0F) * scale;
        } else if (kind < 0.65F) {
            this.halfWidth[i] = random.range(1.5F, 2.3F) * DEGREES;
            this.rayStrength[i] = random.range(0.6F, 0.9F);
            this.reach[i] = random.range(560.0F, 1000.0F) * scale;
        } else {
            this.halfWidth[i] = random.range(0.7F, 1.1F) * DEGREES;
            this.rayStrength[i] = random.range(0.7F, 1.05F);
            this.reach[i] = random.range(460.0F, 820.0F) * scale;
        }
    }

    /**
     * Advances the rays and refreshes the tables over the direction.
     *
     * @param intensity factor of the Intensity option
     */
    void update(float deltaSeconds, float time, @NotNull SunbeamsRig rig, float scale, float intensity, @NotNull PixelRandom random) {
        for (int i = 0; i < this.count; i++) {
            this.progress[i] += deltaSeconds / this.period[i];
            if (this.progress[i] >= 1.0F) {
                this.progress[i] -= 1.0F;
                this.roll(i, rig.fanWidth, scale, random);
            }
            float e = Math.min(1.0F, 1.5F * (float)Math.sin(Math.PI * this.progress[i]));
            this.envelope[i] = e * e * (3.0F - 2.0F * e);
        }
        float[] amp = this.strength;
        // Sums up strength times reach first and divides by the strength afterwards, which gives the strength-weighted reach
        float[] reachSum = this.inverseReach;
        Arrays.fill(amp, 0.0F);
        Arrays.fill(reachSum, 0.0F);
        int last = SunbeamsRig.BINS - 1;
        for (int i = 0; i < this.count; i++) {
            float rayIntensity = this.rayStrength[i] * this.envelope[i];
            if (rayIntensity <= 0.01F) continue;
            float center = (this.angle[i] + (float)Math.sin(time * 0.11F + this.phase[i]) * 0.012F) * rig.binScale;
            float half = this.halfWidth[i] * rig.binScale;
            int from = Math.max(0, (int)Math.ceil(center - half));
            int to = Math.min(last, PixelMath.floor(center + half));
            for (int b = from; b <= to; b++) {
                float u = (b - center) / half;
                float falloff = 1.0F - u * u;
                float v = rayIntensity * falloff * falloff;
                amp[b] += v;
                reachSum[b] += v * this.reach[i];
            }
        }
        for (int b = 0; b <= last; b++) {
            if (amp[b] <= 0.0F) continue;
            float streaks = 0.62F * PixelMath.valueNoise(b * 0.05F + time * 0.05F, 1.5F) + 0.38F * PixelMath.valueNoise(b * 0.11F - time * 0.08F, 7.5F);
            this.inverseReach[b] = amp[b] / reachSum[b];
            amp[b] *= (0.76F + 0.48F * streaks) * intensity;
        }
    }

    /**
     * Light arriving at one pixel of the rig before any shade, rays and sun glow together.
     */
    float lightAt(@NotNull SunbeamsRig rig, int index) {
        float light = rig.glow[index];
        int bin = rig.bins[index];
        float amp = this.strength[bin];
        if (amp > 0.0F) {
            float f = 1.0F - rig.distances[index] * this.inverseReach[bin];
            if (f > 0.0F) light += amp * FADE[(int)(f * POWER_STEPS)];
        }
        return light;
    }

    /**
     * Fills the light steps of the pixels {@code from} to {@code to} (exclusive, indices of the rig's pixel tables) and their colors.
     * The rays stop at the elements when {@code shaded} is true.
     *
     * @param levels gets the light step of every pixel, 0 to 5
     * @param pixels gets the ARGB color of every pixel
     * @param colors ARGB color of every light step
     */
    void paint(@NotNull SunbeamsRig rig, int from, int to, boolean shaded, @NotNull byte[] levels, @NotNull int[] pixels, @NotNull int[] colors) {
        byte[] shade = rig.shade;
        for (int j = from; j < to; j++) {
            float light = this.lightAt(rig, j);
            if (shaded) light *= SunbeamsRig.SHADE_LIGHT[shade[j]];
            int level = step(light);
            levels[j] = (byte)level;
            pixels[j] = colors[level];
        }
    }

    /**
     * The light step of a light value: 0 is no light, 1 to 5 are the opacity steps.
     */
    static int step(float light) {
        return STEPS[Math.min(STEP_SCALE, (int)(light * STEP_SCALE))];
    }

}
