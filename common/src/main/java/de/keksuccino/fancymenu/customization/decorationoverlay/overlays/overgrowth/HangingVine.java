package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * A short vine hanging from an element: a stem that wiggles on its way down and sways at its tip, with small leaves on alternating
 * sides. A leaf starts as a bud and opens once the stem has grown past it, and a finished vine ends in a bud.<br>
 * It remembers where its pixels are, so it only invalidates the canvas when it grew or swayed to other pixels.
 */
final class HangingVine {

    private static final double SWAY_SPEED = 1.25;
    private static final double SWAY_STEP = 0.06;
    private static final double SWAY_STEP_SIN = Math.sin(SWAY_STEP);
    private static final double SWAY_STEP_COS = Math.cos(SWAY_STEP);

    private final int x;
    private final int y;
    private final int length;
    private final float birth;
    /** Pixels per clock unit */
    private final float speed;
    /** How far the tip sways */
    private final float amplitude;
    private final double phase;
    /** The fixed wiggle of every pixel */
    private final float[] wiggle;
    private final int[] leafIndex;
    private final boolean[] leafRight;
    private final byte[] leafKind;
    private final byte[] leafPalette;
    /** Column of every grown pixel right now */
    private final int[] columns;
    private int grown = 0;
    private boolean visible = false;
    private int boundsX0;
    private int boundsY0;
    private int boundsX1;
    private int boundsY1;

    private HangingVine(int x, int y, int length, float birth, float speed, float amplitude, double phase, @NotNull int[] leafIndex, @NotNull boolean[] leafRight, @NotNull byte[] leafKind, @NotNull byte[] leafPalette) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.birth = birth;
        this.speed = speed;
        this.amplitude = amplitude;
        this.phase = phase;
        this.leafIndex = leafIndex;
        this.leafRight = leafRight;
        this.leafKind = leafKind;
        this.leafPalette = leafPalette;
        this.wiggle = new float[length];
        for (int i = 0; i < length; i++) this.wiggle[i] = (float)(Math.sin(i * 0.2 + phase) * 1.1);
        this.columns = new int[length];
    }

    /**
     * Makes a vine that hangs from x, y.
     *
     * @param ivy whether its upper leaves are ivy leaves instead of small ones
     * @param speed pixels it grows per clock unit
     * @param amplitude how far its tip sways
     */
    @NotNull
    static HangingVine make(@NotNull PixelRandom random, int x, int y, int length, float birth, boolean ivy, float speed, float amplitude) {
        int count = 0;
        int[] index = new int[Math.max(1, length / 2)];
        boolean[] right = new boolean[index.length];
        byte[] kind = new byte[index.length];
        byte[] palette = new byte[index.length];
        for (int i = 2 + (int)Math.floor(random.next() * 2.0), side = (random.next() < 0.5) ? 1 : -1; i < length - 1; i += (ivy ? 4 : 3) + (int)Math.floor(random.next() * 2.0), side = -side) {
            index[count] = i;
            right[count] = side > 0;
            kind[count] = (byte)((ivy && (i < length * 0.75)) ? OvergrowthArt.LEAF_IVY : OvergrowthArt.LEAF_SMALL);
            palette[count] = (byte)((random.next() < 0.6) ? 0 : 1);
            count++;
        }
        double phase = random.next() * 6.0;
        return new HangingVine(x, y, length, birth, speed, amplitude, phase, Arrays.copyOf(index, count), Arrays.copyOf(right, count), Arrays.copyOf(kind, count), Arrays.copyOf(palette, count));
    }

    /**
     * Brings the vine to clock time {@code clock} and invalidates the canvas if it looks different now.
     *
     * @param time seconds the overlay has been running, for the sway
     */
    void update(float clock, float time, @NotNull TiledCanvas canvas) {
        int newGrown = Math.max(0, Math.min(this.length, (int)Math.floor((clock - this.birth) * this.speed)));
        boolean grew = newGrown != this.grown;
        // The part that swayed: the rows from the first to the last pixel that moved, and the columns they moved between
        int firstMoved = Integer.MAX_VALUE;
        int lastMoved = -1;
        int movedX0 = Integer.MAX_VALUE;
        int movedX1 = Integer.MIN_VALUE;
        if (newGrown > 0) {
            // sin(time * 1.25 + phase + i * 0.06) by angle addition, one step per pixel
            double angle = time * SWAY_SPEED + this.phase;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            double scale = (double)this.amplitude / this.length;
            for (int i = 0; i < newGrown; i++) {
                int column = this.x + (int)Math.round(this.wiggle[i] + sin * scale * i);
                int old = this.columns[i];
                if (column != old) {
                    if (i < this.grown) {
                        firstMoved = Math.min(firstMoved, i);
                        lastMoved = i;
                        movedX0 = Math.min(movedX0, Math.min(old, column));
                        movedX1 = Math.max(movedX1, Math.max(old, column));
                    }
                    this.columns[i] = column;
                }
                double nextSin = sin * SWAY_STEP_COS + cos * SWAY_STEP_SIN;
                cos = cos * SWAY_STEP_COS - sin * SWAY_STEP_SIN;
                sin = nextSin;
            }
        }
        if (grew) {
            if (this.visible) canvas.invalidate(this.boundsX0, this.boundsY0, this.boundsX1, this.boundsY1);
            this.grown = newGrown;
            this.updateBounds();
            if (this.visible) canvas.invalidate(this.boundsX0, this.boundsY0, this.boundsX1, this.boundsY1);
            return;
        }
        if (lastMoved < 0) return;
        // Leaves hang beside the moved pixels and reach a few rows down, and the bud sits under the tip
        int leafWidth = OvergrowthArt.LEAF[OvergrowthArt.LEAF_IVY].width();
        canvas.invalidate(movedX0 - leafWidth, this.y + firstMoved - 1, movedX1 + leafWidth + 1, this.y + lastMoved + leafWidth);
        this.updateBounds();
    }

    @NotNull
    private PixelSprite leafSprite(int leaf) {
        // A leaf the stem has just passed is still a bud
        return (this.grown - this.leafIndex[leaf] < 3) ? OvergrowthArt.LEAF[OvergrowthArt.LEAF_BUD] : OvergrowthArt.LEAF[this.leafKind[leaf]];
    }

    private int leafX(int leaf, @NotNull PixelSprite sprite) {
        int column = this.columns[this.leafIndex[leaf]];
        return this.leafRight[leaf] ? (column + 1) : (column - sprite.width());
    }

    private void updateBounds() {
        int n = this.grown;
        this.visible = n > 0;
        if (n <= 0) return;
        int x0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            x0 = Math.min(x0, this.columns[i]);
            x1 = Math.max(x1, this.columns[i] + 1);
        }
        int y1 = this.y + n;
        for (int l = 0; l < this.leafIndex.length; l++) {
            if (this.leafIndex[l] >= n) continue;
            PixelSprite sprite = this.leafSprite(l);
            int lx = this.leafX(l, sprite);
            x0 = Math.min(x0, lx);
            x1 = Math.max(x1, lx + sprite.width());
            y1 = Math.max(y1, this.y + this.leafIndex[l] - 1 + sprite.height());
        }
        if (n >= this.length) {
            int tipX = this.columns[this.length - 1] - 1;
            x0 = Math.min(x0, tipX);
            x1 = Math.max(x1, tipX + 2);
            y1 = Math.max(y1, this.y + this.length + 1);
        }
        this.boundsX0 = x0;
        this.boundsY0 = this.y - 1;
        this.boundsX1 = x1;
        this.boundsY1 = y1;
    }

    void paint(@NotNull CanvasPainter painter) {
        if (!this.visible || !painter.overlaps(this.boundsX0, this.boundsY0, this.boundsX1, this.boundsY1)) return;
        int[] green = OvergrowthArt.GREEN;
        int n = this.grown;
        for (int i = 0; i < n; i++) painter.pixel(this.columns[i], this.y + i, (i % 3 != 0) ? green[3] : green[2]);
        for (int l = 0; l < this.leafIndex.length; l++) {
            if (this.leafIndex[l] >= n) continue;
            PixelSprite sprite = this.leafSprite(l);
            painter.sprite(sprite, OvergrowthArt.LEAF_PALETTE[this.leafPalette[l]], this.leafX(l, sprite), this.y + this.leafIndex[l] - 1, !this.leafRight[l]);
        }
        if (n >= this.length) painter.sprite(OvergrowthArt.LEAF[OvergrowthArt.LEAF_BUD], OvergrowthArt.LEAF_PALETTE[0], this.columns[this.length - 1] - 1, this.y + this.length - 1, false);
    }

    /**
     * The widest area the vine can ever cover, swaying included, as x0, y0, x1, y1 (exclusive) widened into {@code bounds}.
     */
    void extendMaxBounds(@NotNull int[] bounds) {
        int reach = 3 + OvergrowthArt.LEAF[OvergrowthArt.LEAF_IVY].width() + 1;
        bounds[0] = Math.min(bounds[0], this.x - reach);
        bounds[1] = Math.min(bounds[1], this.y - 1);
        bounds[2] = Math.max(bounds[2], this.x + reach + 1);
        bounds[3] = Math.max(bounds[3], this.y + this.length + 5);
    }

}
