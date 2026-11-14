package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import org.jetbrains.annotations.NotNull;

/**
 * Where the content of a scrollable area is, where it is scrolling to and how it gets there.<br>
 * Everything is in screen pixels and plain math, so it knows nothing about elements or the game.<br><br>
 *
 * Every axis has a position (where the content is right now) and a target (where it is going). Scrolling moves the target,
 * and {@link #update(double)} moves the position towards it. A scroll that comes in while another one is still running
 * just moves the target on, so nothing ever waits for an animation to finish.
 */
public final class ScrollAreaScroller {

    /** How fast the position closes the distance to the target. After one second, e^-RATE of the distance is left. */
    private static final double SMOOTH_SCROLL_RATE = 16.0D;
    /** The distance to the target at which the position is set onto it, so smooth scrolling comes to an end. */
    private static final double REST_DISTANCE = 0.05D;
    /** How close to an end of the range the position has to be to count as being at that end. */
    private static final double END_TOLERANCE = 0.5D;

    private final Axis x = new Axis();
    private final Axis y = new Axis();
    private boolean smooth = true;

    @NotNull
    public Axis x() {
        return this.x;
    }

    @NotNull
    public Axis y() {
        return this.y;
    }

    @NotNull
    public Axis axis(boolean horizontal) {
        return horizontal ? this.x : this.y;
    }

    public boolean isSmooth() {
        return this.smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    /**
     * Sets how far the content can be scrolled on both axes. Positions and targets beyond the new range get pulled into it,
     * so a scroll that is running ends at the new end instead of where the content was before it changed.
     */
    public void setRange(int maxX, int maxY) {
        this.x.setMax(maxX);
        this.y.setMax(maxY);
    }

    /**
     * Moves the positions towards their targets.
     *
     * @param deltaSeconds The time since the last update.
     */
    public void update(double deltaSeconds) {
        this.x.update(deltaSeconds, this.smooth);
        this.y.update(deltaSeconds, this.smooth);
    }

    /**
     * Returns if any axis is still on its way to its target.
     */
    public boolean isScrolling() {
        return this.x.isScrolling() || this.y.isScrolling();
    }

    /**
     * One axis of a {@link ScrollAreaScroller}.
     */
    public final class Axis {

        private double position = 0.0D;
        private double target = 0.0D;
        private int max = 0;

        private Axis() {
        }

        public double getPosition() {
            return this.position;
        }

        public double getTarget() {
            return this.target;
        }

        public int getMax() {
            return this.max;
        }

        /**
         * Returns how far the content is scrolled, between 0 (start) and 1 (end). Content that can't be scrolled is at 0.
         */
        public double getProgress() {
            return (this.max > 0) ? Math.min(1.0D, Math.max(0.0D, this.position / this.max)) : 0.0D;
        }

        public boolean canScroll() {
            return this.max > 0;
        }

        /**
         * Returns if the content is at its start. Content that can't be scrolled is at its start and its end at once.
         */
        public boolean isAtStart() {
            return this.position <= END_TOLERANCE;
        }

        /**
         * Returns if the content is at its end. Content that can't be scrolled is at its start and its end at once.
         */
        public boolean isAtEnd() {
            return this.position >= (this.max - END_TOLERANCE);
        }

        public boolean isScrolling() {
            return this.position != this.target;
        }

        private void setMax(int max) {
            this.max = Math.max(0, max);
            if (this.target > this.max) this.target = this.max;
            if (this.position > this.max) this.position = this.max;
        }

        /**
         * Scrolls by the given distance from where the axis is scrolling to right now.
         *
         * @return If this changed where the axis is scrolling to. It does not when the axis is at the end it got scrolled towards.
         */
        public boolean scrollBy(double distance) {
            return this.scrollTo(this.target + distance, true);
        }

        /**
         * Scrolls to the given position.
         *
         * @param animate If the position moves there over time, as long as smooth scrolling is on. Without it, the position is there right away.
         * @return If this changed where the axis is scrolling to.
         */
        public boolean scrollTo(double position, boolean animate) {
            // A position that is no number would stay in the axis forever, because nothing can be compared to it
            if (Double.isNaN(position)) return false;
            double clamped = Math.min(this.max, Math.max(0.0D, position));
            boolean changed = clamped != this.target;
            this.target = clamped;
            if (!animate || !ScrollAreaScroller.this.smooth) this.position = clamped;
            return changed;
        }

        /**
         * Puts the content where it was before, without knowing yet how far it can be scrolled.
         * The next {@link ScrollAreaScroller#setRange(int, int)} pulls it into the range.
         */
        public void restore(double position) {
            if (Double.isNaN(position)) return;
            this.position = Math.max(0.0D, position);
            this.target = this.position;
        }

        /**
         * Moves the content by the given distance without an animation, the way something that scrolls on its own does.
         * A target the axis is scrolling to moves along, so the two never fight over the position.
         *
         * @return How much of the distance could not be moved, because the content reached an end of its range.
         */
        public double drift(double distance) {
            if (Double.isNaN(distance)) return 0.0D;
            double wanted = this.position + distance;
            double clamped = Math.min(this.max, Math.max(0.0D, wanted));
            double moved = clamped - this.position;
            this.position = clamped;
            this.target = Math.min(this.max, Math.max(0.0D, this.target + moved));
            return wanted - clamped;
        }

        private void update(double deltaSeconds, boolean smooth) {
            if (this.position == this.target) return;
            if (!smooth || (deltaSeconds <= 0.0D)) {
                if (!smooth) this.position = this.target;
                return;
            }
            double distance = this.target - this.position;
            distance *= Math.exp(-SMOOTH_SCROLL_RATE * deltaSeconds);
            this.position = (Math.abs(distance) < REST_DISTANCE) ? this.target : (this.target - distance);
        }

    }

}
