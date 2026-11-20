package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * One leaf of the Leaves overlay. Leaves fall like real ones: they glide sideways on a pendulum path ({@link #phase}),
 * stall at the turning points, flip to their darker underside there and glide back.
 */
final class Leaf {

    static final int OVATE = 0;
    static final int MAPLE = 1;

    boolean far;
    /** One of the 11 px foreground maples */
    boolean big;
    int type;
    /** Index of the color ramp */
    int ramp;
    float x;
    float y;
    /** Sideways reach of the glide */
    float amplitude;
    /** Pendulum phase of the glide */
    float phase;
    /** How fast the glide swings */
    float swing;
    float fallSpeed;
    /** True while the underside faces the viewer */
    boolean back;
    /** Seconds left of the edge-on frame after a flip */
    float edgeTime;
    /** Sideways speed a gust gave the leaf, decays over time */
    float kick;
    /** Upward speed a gust gave the leaf, decays over time */
    float lift;
    /** Quarter turns per second of a spinning maple */
    float spinRate;
    int spinOffset;
    /** The last gust that touched the leaf, so every gust only touches it once */
    int lastGust = -1;
    boolean landed;
    /** True for a leaf lying on an element, false for one lying on the floor */
    boolean landedOnElement;
    int landX;
    int landY;
    float landTime;
    /** Seconds a landed leaf lies before it dissolves (which takes 0.6 seconds) */
    float landDuration;
    boolean landFlip;

    /**
     * Gives the leaf a new start, like a leaf that just let go of its tree.
     *
     * @param anywhere true to place it anywhere on the screen (for the start), false to place it above the screen
     */
    void respawn(boolean anywhere, boolean far, int screenWidth, int screenHeight, @NotNull PixelRandom random) {
        this.far = far;
        this.big = !far && random.chance(0.16F);
        this.type = random.chance(0.6F) ? OVATE : MAPLE;
        this.ramp = random.rangeInt(0, LeafArt.RAMP_COUNT - 1);
        this.x = random.range(-10.0F, screenWidth + 10.0F);
        this.y = anywhere ? random.range(-10.0F, screenHeight) : -random.range(8.0F, 60.0F);
        this.amplitude = far ? random.range(4.0F, 9.0F) : random.range(10.0F, 22.0F);
        this.phase = random.range(0.0F, PixelMath.TAU);
        this.swing = random.range(1.6F, 3.0F);
        this.fallSpeed = far ? random.range(10.0F, 16.0F) : random.range(18.0F, 30.0F);
        this.back = false;
        this.edgeTime = 0.0F;
        this.kick = 0.0F;
        this.lift = 0.0F;
        this.landed = false;
        this.spinRate = random.range(2.0F, 4.0F);
        this.spinOffset = random.rangeInt(0, 3);
        this.lastGust = -1;
    }

    /**
     * Where the leaf is drawn sideways: its position plus the swing of the glide.
     */
    float glideX() {
        return this.x + (float)Math.sin(this.phase) * this.amplitude;
    }

}
