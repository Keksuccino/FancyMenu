package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cobwebs;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * A spider of the Cobwebs overlay and where it is on its thread: it waits, drops down, hangs for a moment and climbs back up,
 * and it scurries up quickly when it gets scared.
 */
final class CobwebSpider {

    static final int WAITING = 0;
    static final int DROPPING = 1;
    static final int HANGING = 2;
    static final int CLIMBING = 3;
    static final int SCURRYING = 4;
    private static final float DROP_SPEED = 26.0F;
    private static final float CLIMB_SPEED = 18.0F;
    private static final float SCURRY_SPEED = 75.0F;

    /** The web it lives in, or -1 for the spider that comes down from the top edge */
    final int web;
    /** Distance from its side of the screen, or for the spider of the top edge its place as a fraction of the screen width */
    final float anchor;
    /** Row its thread hangs from */
    final int anchorY;
    int state = WAITING;
    float timer;
    /** How far it hangs below its anchor */
    float drop = 0.0F;
    private float depth = 0.0F;

    CobwebSpider(int web, float anchor, int anchorY, float timer) {
        this.web = web;
        this.anchor = anchor;
        this.anchorY = anchorY;
        this.timer = timer;
    }

    /**
     * Moves the spider on by one frame.
     *
     * @param scared whether something came too close, which sends it scurrying up
     */
    void advance(float deltaSeconds, boolean scared, @NotNull PixelRandom random) {
        if (scared) this.state = SCURRYING;
        switch (this.state) {
            case WAITING -> {
                if ((this.timer -= deltaSeconds) <= 0.0F) {
                    this.state = DROPPING;
                    this.depth = random.range(34.0F, 78.0F);
                }
            }
            case DROPPING -> {
                this.drop += DROP_SPEED * deltaSeconds;
                if (this.drop >= this.depth) {
                    this.drop = this.depth;
                    this.state = HANGING;
                    this.timer = random.range(2.0F, 4.5F);
                }
            }
            case HANGING -> {
                if ((this.timer -= deltaSeconds) <= 0.0F) this.state = CLIMBING;
            }
            default -> {
                this.drop -= ((this.state == SCURRYING) ? SCURRY_SPEED : CLIMB_SPEED) * deltaSeconds;
                if (this.drop <= 0.0F) {
                    this.drop = 0.0F;
                    this.state = WAITING;
                    this.timer = random.range(5.0F, 11.0F);
                }
            }
        }
    }

    /**
     * Whether its legs move: while it drops, climbs or scurries.
     */
    boolean isMoving() {
        return (this.state == DROPPING) || (this.state >= CLIMBING);
    }

}
