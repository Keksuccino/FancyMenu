package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * A gust of wind that sweeps across the screen at {@value #SPEED} GUI pixels per second and draws three wind strokes,
 * shared by the Leaves and the Cherry Blossoms overlay. One instance gets reused for all gusts.
 */
public final class Gust {

    public static final float SPEED = 300.0F;
    private static final int STROKES = 3;
    /** Strokes start somewhere between these fractions of the screen height (40 and 215 of 270 GUI pixels) */
    private static final float STROKE_TOP = 40.0F / 270.0F;
    private static final float STROKE_BOTTOM = 215.0F / 270.0F;

    private boolean active = false;
    private int id = 0;
    private int direction = 1;
    private float time = 0.0F;
    private float x = 0.0F;
    private final int[] strokeY = new int[STROKES];
    private final float[] strokeOffset = new float[STROKES];
    private final float[] strokeDelay = new float[STROKES];

    /**
     * Starts a new gust at {@code startX}, running in the given direction.
     */
    public void start(int direction, float startX, int screenHeight, @NotNull PixelRandom random) {
        this.active = true;
        this.id++;
        this.direction = direction;
        this.time = 0.0F;
        this.x = startX;
        for (int i = 0; i < STROKES; i++) {
            this.strokeY[i] = PixelMath.floor(random.range(screenHeight * STROKE_TOP, screenHeight * STROKE_BOTTOM));
            this.strokeOffset[i] = random.range(0.0F, 70.0F);
            this.strokeDelay[i] = random.range(0.0F, 0.25F);
        }
    }

    /**
     * Moves the gust on. It ends once it is {@code endMargin} pixels past the screen edge it runs to.
     */
    public void advance(float deltaSeconds, int screenWidth, float endMargin) {
        if (!this.active) return;
        this.time += deltaSeconds;
        this.x += this.direction * SPEED * deltaSeconds;
        if ((this.x < -endMargin) || (this.x > screenWidth + endMargin)) this.active = false;
    }

    public boolean isActive() {
        return this.active;
    }

    /**
     * Identifies the gust, so things it touched once can ignore it from then on.
     */
    public int id() {
        return this.id;
    }

    public int direction() {
        return this.direction;
    }

    /**
     * Seconds since the gust started.
     */
    public float time() {
        return this.time;
    }

    /**
     * Where the front of the gust is.
     */
    public float x() {
        return this.x;
    }

    /**
     * Draws the wind strokes of the gust, curling ones or plain dashes. Each trails the front of the gust a bit and starts a bit later.
     */
    public void drawStrokes(@NotNull PixelGraphics graphics, boolean dashes, int rgb) {
        if (!this.active) return;
        for (int i = 0; i < STROKES; i++) {
            int originX = PixelMath.floor(this.x - this.direction * (this.strokeOffset[i] + 30.0F));
            float seconds = this.time - this.strokeDelay[i];
            if (dashes) {
                WindStrokes.drawDashes(graphics, originX, this.strokeY[i], this.direction, seconds, rgb);
            } else {
                WindStrokes.drawCurl(graphics, originX, this.strokeY[i], this.direction, seconds, rgb);
            }
        }
    }

}
