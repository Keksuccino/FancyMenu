package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.jetbrains.annotations.NotNull;

/**
 * Scrolls the content of a scrollable area on its own, at a steady speed, the way credits roll.<br>
 * At the end it stops, or waits a moment and starts over. When the user scrolls, it waits a moment before it goes on from where they left the content.
 * Plain math on the axis it gets, so it knows nothing about elements or the game.
 */
public final class ScrollAreaAutoScroll {

    /** How long the content rests at its end before it starts over, and at its start before it moves again. */
    private static final double END_REST_SECONDS = 1.5D;
    private static final double START_REST_SECONDS = 0.75D;
    /** How long the content rests after the user scrolled it. */
    private static final double USER_REST_SECONDS = 2.0D;

    private double restSeconds = START_REST_SECONDS;
    private boolean restartsAfterRest = false;

    /**
     * Moves the content on for one frame.
     *
     * @param deltaSeconds The time since the last frame.
     * @param pixelsPerSecond How fast the content scrolls.
     * @param loop If the content starts over when it reached its end.
     */
    public void update(@NotNull ScrollAreaScroller.Axis axis, double deltaSeconds, double pixelsPerSecond, boolean loop) {
        if (!axis.canScroll() || (deltaSeconds <= 0.0D) || (pixelsPerSecond <= 0.0D)) return;
        if (this.restSeconds > 0.0D) {
            this.restSeconds -= deltaSeconds;
            if (this.restSeconds > 0.0D) return;
            if (this.restartsAfterRest) {
                this.restartsAfterRest = false;
                axis.scrollTo(0.0D, false);
                this.restSeconds = START_REST_SECONDS;
                return;
            }
        }
        double notMoved = axis.drift(pixelsPerSecond * deltaSeconds);
        if ((notMoved > 0.0D) && loop) {
            this.restartsAfterRest = true;
            this.restSeconds = END_REST_SECONDS;
        }
    }

    /**
     * Tells the auto-scroll that the user moved the content, which it leaves alone for a moment then.
     */
    public void onUserScrolled() {
        this.restartsAfterRest = false;
        this.restSeconds = USER_REST_SECONDS;
    }

}
