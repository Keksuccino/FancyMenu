package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * Dust motes that drift on slow air, rise a little and only show inside the light, in the brightness of the light they float in.
 * In a shade they go dark. The pointer stirs them up.<br>
 * Most motes are single pixels, some twinkle white, and a few bigger ones are small crosses that flash a glint now and then.
 */
final class SunbeamsDust {

    /**
     * The mockup stage holds 260 motes. Smaller screens get as many per pixel. Bigger screens (small GUI scales) get more motes
     * by the square root of their extra area only, because every visible mote is one quad.
     */
    private static final int STAGE_MOTES = 260;
    static final float STAGE_AREA = 480.0F * 270.0F;
    /** Pixels around the pointer that stir the dust, and how hard. */
    static final float STIR_RADIUS = 24.0F;
    private static final float STIR_STRENGTH = 90.0F;
    /** How fast a stirred mote calms down, per second. */
    private static final float STIR_DAMPING = 1.6F;
    static final byte KIND_SPECK = 0;
    static final byte KIND_TWINKLE = 1;
    static final byte KIND_CROSS = 2;

    static final PixelSprite MOTE = PixelSprite.of(
            ".1.",
            "121",
            ".1.");
    /** The three glint frames, from the smallest to the biggest. */
    static final PixelSprite[] GLINTS = {
            PixelSprite.of(
                    ".2.",
                    "232",
                    ".2."),
            PixelSprite.of(
                    "..1..",
                    "..2..",
                    "12321",
                    "..2..",
                    "..1.."),
            PixelSprite.of(
                    "...1...",
                    "...2...",
                    "..121..",
                    "1223221",
                    "..121..",
                    "...2...",
                    "...1...")
    };

    int count = 0;
    float[] x = new float[0];
    float[] y = new float[0];
    private float[] velocityX = new float[0];
    private float[] velocityY = new float[0];
    /** Push from the pointer, which calms down over time */
    float[] stirX = new float[0];
    float[] stirY = new float[0];
    private float[] sway = new float[0];
    private float[] twinkle = new float[0];
    private float[] twinkleSpeed = new float[0];
    private byte[] kind = new byte[0];

    /**
     * Scatters new motes over the screen.
     */
    void setup(int width, int height, @NotNull PixelRandom random) {
        float area = width * height / STAGE_AREA;
        int motes = Math.max(1, Math.round(STAGE_MOTES * ((area <= 1.0F) ? area : (float)Math.sqrt(area))));
        this.count = motes;
        this.x = new float[motes];
        this.y = new float[motes];
        this.velocityX = new float[motes];
        this.velocityY = new float[motes];
        this.stirX = new float[motes];
        this.stirY = new float[motes];
        this.sway = new float[motes];
        this.twinkle = new float[motes];
        this.twinkleSpeed = new float[motes];
        this.kind = new byte[motes];
        for (int i = 0; i < motes; i++) {
            this.x[i] = random.range(0.0F, width);
            this.y[i] = random.range(0.0F, height);
            this.velocityX[i] = random.range(-2.5F, 2.5F);
            this.velocityY[i] = random.range(-3.0F, 1.2F);
            this.sway[i] = random.range(0.0F, PixelMath.TAU);
            this.twinkle[i] = random.range(0.0F, PixelMath.TAU);
            this.twinkleSpeed[i] = random.range(1.2F, 3.2F);
            this.kind[i] = random.chance(0.1F) ? KIND_CROSS : (random.chance(0.35F) ? KIND_TWINKLE : KIND_SPECK);
        }
    }

    void update(float deltaSeconds, @NotNull PixelPointer pointer, int width, int height) {
        float damping = (float)Math.exp(-STIR_DAMPING * deltaSeconds);
        boolean stirring = pointer.isInside();
        float pointerX = pointer.x();
        float pointerY = pointer.y();
        for (int i = 0; i < this.count; i++) {
            this.sway[i] += deltaSeconds;
            this.twinkle[i] += this.twinkleSpeed[i] * deltaSeconds;
            if (stirring) {
                float dx = this.x[i] - pointerX;
                float dy = this.y[i] - pointerY;
                float q = dx * dx + dy * dy;
                if ((q < STIR_RADIUS * STIR_RADIUS) && (q > 0.01F)) {
                    float d = (float)Math.sqrt(q);
                    float push = (1.0F - d / STIR_RADIUS) * STIR_STRENGTH * deltaSeconds;
                    this.stirX[i] += dx / d * push;
                    this.stirY[i] += dy / d * push;
                }
            }
            this.stirX[i] *= damping;
            this.stirY[i] *= damping;
            float nx = this.x[i] + (this.velocityX[i] + this.stirX[i] + (float)Math.sin(this.sway[i] * 0.7F) * 3.0F) * deltaSeconds;
            float ny = this.y[i] + (this.velocityY[i] + this.stirY[i] + (float)Math.cos(this.sway[i] * 0.5F) * 2.0F) * deltaSeconds;
            if (nx < 0.0F) nx += width;
            else if (nx >= width) nx -= width;
            if (ny < 0.0F) ny += height;
            else if (ny >= height) ny -= height;
            // Wrapping can land exactly on the far edge through rounding, which would be outside of the screen
            this.x[i] = PixelMath.clamp(nx, 0.0F, Math.nextDown((float)width));
            this.y[i] = PixelMath.clamp(ny, 0.0F, Math.nextDown((float)height));
        }
    }

    /**
     * Draws the motes that float in light.
     *
     * @param levels light step of every screen pixel
     */
    void draw(@NotNull PixelGraphics graphics, @NotNull byte[] levels, int width, @NotNull SunbeamsPalette palette) {
        for (int i = 0; i < this.count; i++) {
            int px = (int)this.x[i];
            int py = (int)this.y[i];
            int index = py * width + px;
            if ((index < 0) || (index >= levels.length)) continue;
            int level = levels[index];
            if (level <= 0) continue;
            float wave = (float)Math.sin(this.twinkle[i]);
            byte k = this.kind[i];
            if (k == KIND_CROSS) {
                if ((wave > 0.93F) && (level > 1)) {
                    PixelSprite glint = GLINTS[1];
                    graphics.sprite(glint, palette.glint, px - (glint.width() >> 1), py - (glint.height() >> 1));
                } else {
                    graphics.sprite(MOTE, palette.mote[level], PixelMath.floor(this.x[i] - 1.0F), PixelMath.floor(this.y[i] - 1.0F));
                }
            } else if ((k == KIND_TWINKLE) && (wave > 0.6F)) {
                graphics.pixel(px, py, 0xFFFFFFFF);
            } else {
                graphics.pixel(px, py, palette.moteDot[level]);
            }
        }
    }

}
