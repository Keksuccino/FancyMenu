package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.SurfaceHeadroom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * The snow cap on top of one surface (an element or the floor), plus the icicles under an element.<br>
 * The cap is one height per column, in art pixels. On elements it reaches one column past each end and its ends are rounded, so it
 * overhangs the element a little. A landing flake adds snow over three columns, then the cap slumps until no step between two columns is
 * steeper than about one pixel, so it stays soft and rounded. The snow slowly melts away again, which keeps a steady snowfall in balance.<br><br>
 *
 * Columns and rows are {@link #scale} GUI pixels big, so the cap grows with the Size option like the flakes.
 * A cap never grows into an element right above it and icicles never reach into an element right below, see {@link #fitBetweenElements}.
 */
final class SnowPile {

    static final float ELEMENT_MAX_HEIGHT = 5.0F;
    static final float FLOOR_MAX_HEIGHT = 7.0F;
    static final float MELT_PER_SECOND = 0.012F;
    static final int ICICLES_PER_ELEMENT = 6;
    static final int ICICLE_MAX_LENGTH = 5;
    static final float ICICLE_GROWTH_PER_SECOND = 0.08F;
    /** Steps between neighbor columns get evened out down to this height difference */
    private static final float MAX_STEP = 1.1F;

    final boolean floor;
    final int scale;
    /** Top row of the surface: the cap sits on the rows above it */
    final int surfaceY;
    /** First row below the element, where icicles start */
    final int surfaceBottom;
    /** GUI x of column 0 */
    final int x0;
    final int columns;
    final float maxHeight;
    /** The highest the snow can get per column, see {@link #limit} */
    private final float[] columnLimits;
    final float[] heights;
    final float[] icicleLength;
    final int[] icicleMaxLength;
    /** How long the icicles can get before they reach an element below */
    final int[] icicleRoom;
    final int[] icicleX;
    /** What the canvas shows right now, to know when the pile has to be drawn again */
    private final int[] drawnHeights;
    private final int[] drawnIcicles;
    /** Set while the pile waits to be drawn into the canvas again */
    boolean needsDraw = true;

    SnowPile(@NotNull PixelSurfaces.Surface<?> surface, int scale, @NotNull PixelRandom random) {
        this.floor = surface.isFloor();
        this.scale = Math.max(1, scale);
        this.surfaceY = surface.y();
        this.surfaceBottom = surface.bottom();
        int spanColumns = Math.max(1, (surface.width() + this.scale - 1) / this.scale);
        this.columns = this.floor ? spanColumns : (spanColumns + 2);
        this.x0 = this.floor ? surface.x() : (surface.x() - this.scale);
        this.maxHeight = this.floor ? FLOOR_MAX_HEIGHT : ELEMENT_MAX_HEIGHT;
        this.heights = new float[this.columns];
        this.columnLimits = new float[this.columns];
        for (int i = 0; i < this.columns; i++) this.columnLimits[i] = this.shapeLimit(i);
        this.drawnHeights = new int[this.columns];
        float p1 = random.range(0.0F, 9.0F);
        float p2 = random.range(0.0F, 9.0F);
        for (int i = 0; i < this.columns; i++) {
            float start = this.floor
                    ? 4.2F + 1.6F * (float)Math.sin(i * 0.045F + p1) + 1.1F * (float)Math.sin(i * 0.13F + p2)
                    : 3.4F + 1.0F * (float)Math.sin(i * 0.19F + p1) + 0.6F * (float)Math.sin(i * 0.47F + p2);
            this.heights[i] = Math.min(this.limit(i), start);
        }
        int icicles = this.floor ? 0 : ICICLES_PER_ELEMENT;
        this.icicleX = new int[icicles];
        this.icicleLength = new float[icicles];
        this.icicleMaxLength = new int[icicles];
        this.icicleRoom = new int[icicles];
        this.drawnIcicles = new int[icicles];
        for (int k = 0; k < icicles; k++) {
            int column = random.rangeInt(3, spanColumns - 4);
            this.icicleX[k] = surface.x() + Math.max(0, Math.min(spanColumns - 1, column)) * this.scale;
            this.icicleLength[k] = random.range(0.0F, 4.0F);
            this.icicleMaxLength[k] = random.rangeInt(2, ICICLE_MAX_LENGTH);
            this.icicleRoom[k] = ICICLE_MAX_LENGTH;
        }
    }

    /**
     * The highest the snow can get in a column: the full height on the floor and rounded ends on elements, but never more than fits
     * under an element right above.
     */
    float limit(int column) {
        return this.columnLimits[column];
    }

    private float shapeLimit(int column) {
        if (this.floor) return this.maxHeight;
        return Math.min(this.maxHeight, 0.8F + Math.min(column, this.columns - 1 - column) * 0.8F);
    }

    /**
     * Keeps the cap out of the elements above it and the icicles out of the elements below: every column only gets as high as the free
     * rows above it allow, and every icicle only as long as the free rows below it. Call it whenever the elements of the screen changed.
     *
     * @param surface the surface this pile lies on
     * @param elements all element surfaces of the screen
     */
    void fitBetweenElements(@NotNull PixelSurfaces.Surface<?> surface, @NotNull List<? extends PixelSurfaces.Surface<?>> elements) {
        for (int k = 0; k < this.icicleX.length; k++) {
            // An icicle of 4 or more pixels gets a second pixel at its root, so it needs the room of two columns
            int freeRows = SurfaceHeadroom.freeRowsBelow(surface, this.icicleX[k], 2 * this.scale, elements);
            this.icicleRoom[k] = (freeRows == Integer.MAX_VALUE) ? ICICLE_MAX_LENGTH : Math.min(ICICLE_MAX_LENGTH, freeRows / this.scale);
            if (this.icicleLength[k] > this.icicleRoom[k]) {
                this.icicleLength[k] = this.icicleRoom[k];
                this.needsDraw = true;
            }
        }
        for (int i = 0; i < this.columns; i++) {
            int freeRows = SurfaceHeadroom.freeRowsAbove(surface, this.x0 + i * this.scale, this.scale, elements);
            this.columnLimits[i] = Math.min(this.shapeLimit(i), freeRows / this.scale);
            if (this.heights[i] > this.columnLimits[i]) {
                this.heights[i] = this.columnLimits[i];
                this.needsDraw = true;
            }
        }
    }

    /**
     * The column under a GUI x position, which can be outside of the pile (below 0 or at least {@link #columns}).
     */
    int columnAt(float x) {
        return (int)Math.floor((x - this.x0) / this.scale);
    }

    /**
     * The row a flake touches when it lands on the column: the row of the top of the snow there.
     */
    int topY(int column) {
        return this.surfaceY - Math.round(this.heights[column]) * this.scale;
    }

    /**
     * Adds snow of a landing flake: half on the column, a quarter on each neighbor, then lets the cap around it slump.
     */
    void deposit(int column, float amount) {
        this.addTo(column, amount * 0.5F);
        this.addTo(column - 1, amount * 0.25F);
        this.addTo(column + 1, amount * 0.25F);
        int from = Math.max(0, column - 4);
        int to = Math.min(this.columns - 1, column + 4);
        for (int j = from; j < to; j++) {
            float step = this.heights[j] - this.heights[j + 1];
            float size = Math.abs(step);
            if (size > MAX_STEP) {
                float moved = (size - MAX_STEP) / 2.0F * Math.signum(step);
                this.heights[j] -= moved;
                this.heights[j + 1] += moved;
            }
        }
    }

    private void addTo(int column, float amount) {
        if ((column < 0) || (column >= this.columns)) return;
        this.heights[column] = Math.min(this.heights[column] + amount, this.limit(column));
    }

    /**
     * Melts the cap a little and lets the icicles grow toward their full length.
     */
    void advance(float deltaSeconds) {
        float melt = MELT_PER_SECOND * deltaSeconds;
        for (int i = 0; i < this.columns; i++) this.heights[i] = Math.max(0.0F, this.heights[i] - melt);
        float growth = ICICLE_GROWTH_PER_SECOND * deltaSeconds;
        for (int k = 0; k < this.icicleLength.length; k++) {
            if (this.icicleLength[k] < Math.min(this.icicleMaxLength[k], this.icicleRoom[k])) this.icicleLength[k] += growth;
        }
        if (this.needsDraw) return;
        for (int i = 0; i < this.columns; i++) {
            if (Math.round(this.heights[i]) != this.drawnHeights[i]) {
                this.needsDraw = true;
                return;
            }
        }
        for (int k = 0; k < this.icicleLength.length; k++) {
            if ((int)this.icicleLength[k] != this.drawnIcicles[k]) {
                this.needsDraw = true;
                return;
            }
        }
    }

    /**
     * Left edge of the area the pile draws into, in GUI pixels. The area covers the cap, the overhang and the icicles.
     */
    int areaX() {
        return this.x0;
    }

    int areaY() {
        return this.surfaceY - Math.round(this.maxHeight) * this.scale;
    }

    int areaWidth() {
        return this.columns * this.scale;
    }

    int areaHeight() {
        int bottom = this.floor ? this.surfaceY : (this.surfaceBottom + ICICLE_MAX_LENGTH * this.scale);
        return bottom - this.areaY();
    }

    boolean overlapsArea(@NotNull SnowPile other) {
        return (this.areaX() < other.areaX() + other.areaWidth()) && (other.areaX() < this.areaX() + this.areaWidth())
                && (this.areaY() < other.areaY() + other.areaHeight()) && (other.areaY() < this.areaY() + this.areaHeight());
    }

    void clearArea(@NotNull PixelCanvas canvas) {
        canvas.fillRect(this.areaX(), this.areaY(), this.areaWidth(), this.areaHeight(), 0);
    }

    /**
     * Draws the cap and the icicles into the canvas, replacing what is there: a white top edge, the body and a blue shaded underside,
     * one shaded overhang pixel at each end of an element, and icicles with a light root and a bright tip.
     */
    void draw(@NotNull PixelCanvas canvas, @NotNull SnowArt art) {
        int n = this.scale;
        for (int i = 0; i < this.columns; i++) {
            int h = Math.round(this.heights[i]);
            this.drawnHeights[i] = h;
            if (h <= 0) continue;
            int x = this.x0 + i * n;
            int top = this.surfaceY - h * n;
            canvas.fillRect(x, top, n, n, art.capTop);
            if (h >= 2) {
                if (h > 2) canvas.fillRect(x, top + n, n, (h - 2) * n, art.capBody);
                canvas.fillRect(x, this.surfaceY - n, n, n, art.capShade);
            }
            if (!this.floor && ((i == 0) || (i == this.columns - 1))) canvas.fillRect(x, this.surfaceY, n, n, art.capShade);
        }
        for (int k = 0; k < this.icicleLength.length; k++) {
            int length = (int)this.icicleLength[k];
            this.drawnIcicles[k] = length;
            if (length < 1) continue;
            int x = this.icicleX[k];
            for (int r = 0; r < length; r++) {
                int color = (r == length - 1) ? art.iceTip : ((r == 0) ? art.iceRoot : art.iceBody);
                canvas.fillRect(x, this.surfaceBottom + r * n, n, n, color);
            }
            if (length >= 4) canvas.fillRect(x + n, this.surfaceBottom, n, n, art.iceRoot);
        }
        this.needsDraw = false;
    }

}
