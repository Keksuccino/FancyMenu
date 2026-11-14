package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import java.util.Arrays;

/**
 * The positions the content of a scrollable area comes to rest at on one axis, when the area snaps: the start and the end
 * of the range, and everything in between that got added. Plain math, so it knows nothing about elements or the game.
 */
public final class ScrollAreaSnapPoints {

    /** How far apart two positions have to be to count as two. Closer ones are the same place for the eye. */
    private static final double MIN_DISTANCE = 0.5D;

    private double[] points = new double[16];
    private int count = 0;

    /**
     * Starts over with no points.
     */
    public void clear() {
        this.count = 0;
    }

    public void add(double position) {
        if (Double.isNaN(position)) return;
        if (this.count == this.points.length) this.points = Arrays.copyOf(this.points, this.count * 2);
        this.points[this.count++] = position;
    }

    /**
     * Adds a point for every page: the size of what the area shows, over and over, until the end of the range.
     */
    public void addPages(int pageSize, int max) {
        if (pageSize <= 0) return;
        for (long position = pageSize; position < max; position += pageSize) {
            this.add(position);
        }
    }

    /**
     * Has to be called after the last point got added and before the points get used. Pulls all points into the range,
     * adds its start and its end, and puts them in order without any that are in the same place.
     *
     * @param max How far the content can be scrolled.
     */
    public void finish(int max) {
        int limit = Math.max(0, max);
        this.add(0.0D);
        this.add(limit);
        for (int i = 0; i < this.count; i++) {
            this.points[i] = Math.min(limit, Math.max(0.0D, this.points[i]));
        }
        Arrays.sort(this.points, 0, this.count);
        int kept = 1;
        for (int i = 1; i < this.count; i++) {
            if ((this.points[i] - this.points[kept - 1]) >= MIN_DISTANCE) this.points[kept++] = this.points[i];
        }
        // The end of the range always is a point, also when another one was right in front of it
        this.points[kept - 1] = (kept > 1) && ((limit - this.points[kept - 1]) < MIN_DISTANCE) ? limit : this.points[kept - 1];
        this.count = kept;
    }

    /**
     * Returns the next point from the given position in the given direction, or the position itself if there is none.
     */
    public double next(double position, boolean forward) {
        if (forward) {
            for (int i = 0; i < this.count; i++) {
                if (this.points[i] >= (position + MIN_DISTANCE)) return this.points[i];
            }
        } else {
            for (int i = this.count - 1; i >= 0; i--) {
                if (this.points[i] <= (position - MIN_DISTANCE)) return this.points[i];
            }
        }
        return position;
    }

    /**
     * Returns the point that is closest to the given position, or the position itself if there are no points.
     */
    public double nearest(double position) {
        if (this.count == 0) return position;
        double nearest = this.points[0];
        for (int i = 1; i < this.count; i++) {
            if (Math.abs(this.points[i] - position) < Math.abs(nearest - position)) nearest = this.points[i];
        }
        return nearest;
    }

}
