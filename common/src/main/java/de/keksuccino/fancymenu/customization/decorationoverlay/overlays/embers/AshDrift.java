package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.embers;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * A soft drift of ash on top of an element or along the bottom of the screen: one height per column. On elements it has rounded
 * ends and hangs over both ends by one pixel. A deposit spreads over three columns and then slumps until no step is steeper than
 * about one pixel. Fresh ash piles up on the settled shape of the drift and slowly sinks back to it.<br>
 * It remembers the height it shows per column, so it only invalidates the canvas where a column changed.
 */
final class AshDrift {

    /** Ash tones from the pale top edge to the shaded underside */
    static final int[] ASH = {0xFFD0C8C2, 0xFFA69D9A, 0xFF7A7173, 0xFF54474C};
    private static final float SINK_SPEED = 0.012F;
    /** Tallest drift of the floor and of elements */
    private static final float FLOOR_MAX = 7.0F;
    private static final float ELEMENT_MAX = 5.4F;
    /** Rows a drift can cover above its surface */
    static final int MAX_ROWS = 8;

    final boolean floor;
    /** First column of the drift, one left of the element (or the left screen edge for the floor) */
    final int x0;
    /** The surface row: the top row of the element, the screen height for the floor */
    final int top;
    final int columns;
    private final float max;
    final float[] heights;
    private final float[] rest;
    private final byte[] shown;
    private boolean visible = false;

    AshDrift(@NotNull PixelRandom random, boolean floor, int left, int top, int width) {
        this.floor = floor;
        this.columns = floor ? width : (width + 2);
        this.x0 = floor ? 0 : (left - 1);
        this.top = top;
        this.max = floor ? FLOOR_MAX : ELEMENT_MAX;
        this.heights = new float[this.columns];
        this.rest = new float[this.columns];
        this.shown = new byte[this.columns];
        double phase1 = random.range(0.0F, 9.0F);
        double phase2 = random.range(0.0F, 9.0F);
        for (int i = 0; i < this.columns; i++) {
            double start = floor ? (4.6 + 1.5 * Math.sin(i * 0.05 + phase1) + 1.1 * Math.sin(i * 0.14 + phase2)) : (3.6 + 1.4 * Math.sin(i * 0.19 + phase1) + 0.8 * Math.sin(i * 0.47 + phase2));
            this.heights[i] = (float)Math.min(this.limit(i), start);
            // The settled shape fresh ash sinks back to
            this.rest[i] = this.heights[i] * 0.9F;
        }
    }

    /**
     * The tallest the drift may get in a column: rounded ends on elements.
     */
    private float limit(int column) {
        if (this.floor) return this.max;
        return Math.min(this.max, 0.8F + Math.min(column, this.columns - 1 - column) * 0.8F);
    }

    /**
     * Drops some ash onto a column. It spreads over three columns and slumps until no step is steeper than about one pixel.
     */
    void deposit(int column, float amount) {
        this.raise(column, amount * 0.5F);
        this.raise(column - 1, amount * 0.25F);
        this.raise(column + 1, amount * 0.25F);
        for (int j = Math.max(0, column - 4); j < Math.min(this.columns - 1, column + 4); j++) {
            float step = this.heights[j] - this.heights[j + 1];
            if (Math.abs(step) > 1.1F) {
                float move = (Math.abs(step) - 1.1F) / 2.0F * Math.signum(step);
                this.heights[j] -= move;
                this.heights[j + 1] += move;
            }
        }
    }

    private void raise(int column, float amount) {
        if ((column >= 0) && (column < this.columns)) this.heights[column] = Math.min(this.heights[column] + amount, this.limit(column));
    }

    /**
     * Lets fresh ash sink back toward the settled shape.
     */
    void sink(float deltaSeconds) {
        float sunk = SINK_SPEED * deltaSeconds;
        for (int i = 0; i < this.columns; i++) this.heights[i] = Math.max(this.rest[i], this.heights[i] - sunk);
    }

    /**
     * The whole-pixel height of a column, as it gets drawn.
     */
    int height(int column) {
        return Math.round(this.heights[column]);
    }

    /**
     * The row a falling flake comes to rest on above a column.
     */
    int surfaceRow(int column) {
        return this.top - this.height(column);
    }

    /**
     * Invalidates the columns whose drawn height changed, or the whole drift if it got shown or hidden.
     */
    void sync(boolean show, @NotNull TiledCanvas canvas) {
        if (show != this.visible) {
            this.visible = show;
            for (int i = 0; i < this.columns; i++) this.shown[i] = (byte)this.height(i);
            canvas.invalidate(this.x0, this.top - MAX_ROWS, this.x0 + this.columns, this.top + 1);
            return;
        }
        if (!show) return;
        int from = Integer.MAX_VALUE;
        int to = Integer.MIN_VALUE;
        for (int i = 0; i < this.columns; i++) {
            int h = this.height(i);
            if (h == this.shown[i]) continue;
            this.shown[i] = (byte)h;
            from = Math.min(from, i);
            to = Math.max(to, i);
        }
        // One row more covers the overhang at the ends, which shows only while the end column has ash
        if (from <= to) canvas.invalidate(this.x0 + from, this.top - MAX_ROWS, this.x0 + to + 1, this.top + 1);
    }

    /**
     * Invalidates everything the drift covers, so it gets erased when its element is gone.
     */
    void invalidateAll(@NotNull TiledCanvas canvas) {
        canvas.invalidate(this.x0, this.top - MAX_ROWS, this.x0 + this.columns, this.top + 1);
    }

    /**
     * Paints the drift in clean layers, like a snow cap: a pale top edge, a lighter band under it on tall heaps, the grey body and a
     * warm dark underside, with a few charcoal flecks. On an element it hangs over both ends by one pixel.
     */
    void paint(@NotNull CanvasPainter painter) {
        if (!this.visible || !painter.overlaps(this.x0, this.top - MAX_ROWS, this.x0 + this.columns, this.top + 1)) return;
        int from = Math.max(0, painter.clipX0() - this.x0);
        int to = Math.min(this.columns, painter.clipX1() - this.x0);
        for (int i = from; i < to; i++) {
            int h = this.shown[i];
            if (h <= 0) continue;
            int x = this.x0 + i;
            for (int r = 0; r < h; r++) {
                int color;
                if (r == 0) {
                    color = ASH[0];
                } else if ((r == h - 1) && (h >= 2)) {
                    color = ASH[3];
                } else if (PixelMath.hash(i * 5 + this.top, r) > 0.93F) {
                    color = ASH[3];
                } else {
                    color = ((r == 1) && (h >= 4)) ? ASH[1] : ASH[2];
                }
                painter.pixel(x, this.top - h + r, color);
            }
            if (!this.floor && ((i == 0) || (i == this.columns - 1))) painter.pixel(x, this.top, ASH[3]);
        }
    }

    boolean isVisible() {
        return this.visible;
    }

}
