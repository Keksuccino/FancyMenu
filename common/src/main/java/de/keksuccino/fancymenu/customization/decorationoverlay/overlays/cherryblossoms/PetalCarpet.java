package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;

/**
 * The loose carpet of petals on top of one surface: one height per column, up to 2 pixels on elements and 3 on the floor.
 * Every column shows a few petal pixels in the three carpet tones, picked by a hash so they stay put while the carpet changes.<br>
 * The carpet lives in a small canvas of its own (as wide as the surface, 3 rows high), so a change only uploads those 3 rows.
 */
final class PetalCarpet {

    static final int ROWS = 3;

    final int x;
    /** The row the carpet rests on, the bottom row of its canvas */
    final int restY;
    final int width;
    final int cap;
    /** True for a carpet on an element, false for the one on the floor */
    final boolean onElement;
    private final int seed;
    private final byte[] heights;
    private final PixelCanvas canvas;
    /** Fractional columns that thinned out, see {@link #thin} */
    private float thinning = 0.0F;
    /** True until the overlay checked whether the carpet takes over the petals of a carpet that vanished */
    boolean fresh = true;

    /**
     * @param canvas a canvas to paint into, for example the one of a carpet that vanished (it gets resized and painted over)
     */
    PetalCarpet(@NotNull PixelSurfaces.Surface<PetalCarpet> surface, @NotNull PixelRandom random, @NotNull PixelCanvas canvas) {
        this.x = surface.x();
        this.restY = surface.restY();
        this.width = Math.max(0, surface.width());
        this.onElement = !surface.isFloor();
        this.cap = this.onElement ? 2 : 3;
        // Keeps the tones of a surface's columns the same as long as the surface stays where it is
        this.seed = surface.isFloor() ? 4 : Math.floorMod(surface.x() * 7 + surface.y() * 13, 50);
        this.heights = new byte[this.width];
        this.canvas = canvas;
        this.canvas.resize(this.width, ROWS);
        for (int i = 0; i < this.width; i++) {
            float r = random.next();
            this.heights[i] = (byte)((r < 0.3F) ? 0 : ((r < 0.8F) ? 1 : 2));
            this.paintColumn(i);
        }
    }

    /**
     * Takes over the petals of a carpet of the same size whose surface vanished, so a carpet on an element that moves stays the same.
     */
    void inherit(@NotNull PetalCarpet vanished) {
        int columns = Math.min(this.width, vanished.width);
        for (int i = 0; i < columns; i++) {
            this.heights[i] = (byte)Math.min(this.cap, vanished.heights[i]);
            this.paintColumn(i);
        }
        this.thinning = vanished.thinning;
    }

    boolean canInherit(@NotNull PetalCarpet vanished) {
        return (vanished.width == this.width) && (vanished.onElement == this.onElement);
    }

    int height(int column) {
        return this.heights[column];
    }

    /**
     * Adds a petal to a column, if the column is not full yet. Returns true if it fit.
     */
    boolean add(int column) {
        if ((column < 0) || (column >= this.width) || (this.heights[column] >= this.cap)) return false;
        this.heights[column]++;
        this.paintColumn(column);
        return true;
    }

    /**
     * Takes a petal away from a column, if it has one. Returns true if it had one.
     */
    boolean remove(int column) {
        if ((column < 0) || (column >= this.width) || (this.heights[column] <= 0)) return false;
        this.heights[column]--;
        this.paintColumn(column);
        return true;
    }

    /**
     * Petals slowly blow away or wither: per second, 2 percent of the surface width in random columns lose a petal.
     */
    void thin(float deltaSeconds, @NotNull PixelRandom random) {
        if (this.width <= 0) return;
        this.thinning += this.width * 0.02F * deltaSeconds;
        while (this.thinning >= 1.0F) {
            this.thinning -= 1.0F;
            this.remove(random.rangeInt(0, this.width - 1));
        }
    }

    private void paintColumn(int column) {
        int h = this.heights[column];
        int[] tones = CherryBlossomArt.CARPET;
        this.canvas.set(column, ROWS - 1, (h >= 1) ? tones[toneIndex(column, this.seed)] : 0);
        this.canvas.set(column, ROWS - 2, ((h >= 2) && (PixelMath.hash(column, this.seed + 5) > 0.35F)) ? tones[toneIndex(column, this.seed + 9)] : 0);
        this.canvas.set(column, ROWS - 3, ((h >= 3) && (PixelMath.hash(column, this.seed + 13) > 0.6F)) ? tones[toneIndex(column, this.seed + 17)] : 0);
    }

    private static int toneIndex(int column, int seed) {
        return Math.min(2, (int)(PixelMath.hash(column, seed) * 3.0F));
    }

    void draw(@NotNull PixelGraphics graphics) {
        graphics.canvas(this.canvas, this.x, this.restY - (ROWS - 1));
    }

    void releaseTexture() {
        this.canvas.releaseTexture();
    }

    /**
     * The canvas of the carpet, so a new carpet can reuse it (with its texture) once this one's surface is gone.
     */
    @NotNull
    PixelCanvas canvas() {
        return this.canvas;
    }

}
