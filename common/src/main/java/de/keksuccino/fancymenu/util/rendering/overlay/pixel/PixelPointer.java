package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.jetbrains.annotations.ApiStatus;

/**
 * Where the mouse pointer is, in GUI pixels, for overlays that react to it (the mockup's {@code mouse}).<br>
 * The overlay base class updates it once per frame, right before the overlay's update. Clicks reach overlays through their
 * click hook instead, which never takes a click away from the widgets below.
 */
public final class PixelPointer {

    private float x = -1.0F;
    private float y = -1.0F;
    private boolean inside = false;

    @ApiStatus.Internal
    public void update(float x, float y, int screenWidth, int screenHeight) {
        this.x = x;
        this.y = y;
        this.inside = (x >= 0.0F) && (y >= 0.0F) && (x < screenWidth) && (y < screenHeight);
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    /**
     * Whether the pointer is on the screen. Overlays should only react to it while this is true.
     */
    public boolean isInside() {
        return this.inside;
    }

    public float distanceSquared(float toX, float toY) {
        float dx = toX - this.x;
        float dy = toY - this.y;
        return dx * dx + dy * dy;
    }

    /**
     * Whether the pointer is on the screen and at most {@code radius} GUI pixels away from the point.
     */
    public boolean isWithin(float toX, float toY, float radius) {
        return this.inside && (this.distanceSquared(toX, toY) <= radius * radius);
    }

}
