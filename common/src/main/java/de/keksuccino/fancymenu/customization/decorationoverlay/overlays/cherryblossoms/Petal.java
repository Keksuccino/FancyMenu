package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * One falling petal of the Cherry Blossoms overlay. Petals fall slower than leaves, sway on a pendulum path and ride a steady breeze.
 */
final class Petal {

    /** Far specks behind the elements */
    static final int SPECK = 0;
    static final int SMALL = 1;
    static final int LARGE = 2;
    static final int BLOSSOM = 3;
    /** How much of the breeze reaches the petals of each layer */
    private static final float[] BREEZE_FACTORS = {0.5F, 0.8F, 1.0F, 1.0F};
    /** Size of each layer in pixels */
    private static final int[] SIZES = {1, 3, 5, 7};

    int layer;
    float x;
    float y;
    float fallSpeed;
    float amplitude;
    float phase;
    float swing;
    /** Frames of the flutter per second */
    float flutterSpeed;
    float flutterOffset;
    /** Which quarter turn the petal shows */
    int turn;
    /** Sideways speed from a gust, decays over time */
    float kick;
    /** Upward speed from a gust, decays over time */
    float lift;

    /**
     * Gives the petal a new start.
     *
     * @param anywhere true to place it anywhere on the screen (for the start), false to let it start above the screen
     * @param tipX x of the branch tip it lets go of, NaN to start without a branch
     */
    void respawn(int layer, boolean anywhere, float tipX, float tipY, int screenWidth, int screenHeight, @NotNull PixelRandom random) {
        this.layer = layer;
        boolean fromTip = !Float.isNaN(tipX);
        // The breeze blows to the right, so petals start a bit further left
        this.x = fromTip ? tipX : random.range(-80.0F, screenWidth);
        this.y = fromTip ? tipY : (anywhere ? random.range(-10.0F, screenHeight) : -random.range(6.0F, 40.0F));
        this.fallSpeed = (layer == SPECK) ? random.range(8.0F, 14.0F) : ((layer == SMALL) ? random.range(12.0F, 20.0F) : random.range(16.0F, 26.0F));
        this.amplitude = (layer == SPECK) ? random.range(2.0F, 6.0F) : random.range(6.0F, 16.0F);
        this.phase = random.range(0.0F, PixelMath.TAU);
        this.swing = random.range(1.4F, 2.8F);
        this.flutterSpeed = random.range(5.0F, 8.0F);
        this.flutterOffset = random.range(0.0F, 9.0F);
        this.turn = random.rangeInt(0, 3);
        this.kick = 0.0F;
        this.lift = 0.0F;
    }

    void move(float deltaSeconds, float breeze) {
        this.phase += this.swing * deltaSeconds;
        this.y += (this.fallSpeed * (0.5F + 0.5F * Math.abs((float)Math.cos(this.phase))) - this.lift) * deltaSeconds;
        this.x += (breeze * BREEZE_FACTORS[this.layer] + this.kick) * deltaSeconds;
        this.kick *= (float)Math.exp(-1.6F * deltaSeconds);
        this.lift *= (float)Math.exp(-2.5F * deltaSeconds);
    }

    /**
     * Where the petal is drawn sideways: its position plus the sway.
     */
    float swayX() {
        return this.x + (float)Math.sin(this.phase) * this.amplitude;
    }

    int size() {
        return SIZES[this.layer];
    }

}
