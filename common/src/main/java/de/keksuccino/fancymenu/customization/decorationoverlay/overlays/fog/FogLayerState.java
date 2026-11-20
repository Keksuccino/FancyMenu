package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fog;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;

/**
 * What one fog layer of an overlay shows right now, and what it got painted with: its canvas, the density levels of its pixels,
 * the texture offsets and the pointer position. A layer only gets painted again when an offset changed (all of it)
 * or when the pointer moved through it (only the area around the old and the new pointer position).
 */
final class FogLayerState {

    @NotNull
    private final FogLayer layer;
    @NotNull
    final PixelCanvas canvas;
    private int width = 0;
    private int rows = 0;
    private int top = 0;
    @NotNull
    private byte[] levels = new byte[0];
    /** True when everything has to be painted again with the next update. */
    private boolean stale = true;
    private int offsetA = 0;
    private int offsetB = 0;
    private float thresholdWave = 0.0F;
    /** The pointer position the layer got painted with, NaN for none */
    private float clearX = Float.NaN;
    private float clearY = Float.NaN;

    FogLayerState(@NotNull FogLayer layer, int index) {
        this.layer = layer;
        this.canvas = new PixelCanvas("fog_" + index);
    }

    void resize(int screenWidth, int screenHeight) {
        this.width = screenWidth;
        this.top = this.layer.top(screenHeight);
        this.rows = Math.max(0, this.layer.bottom(screenHeight) - this.top);
        this.canvas.resize(screenWidth, this.rows);
        this.levels = new byte[screenWidth * this.rows];
        this.layer.prepareNoise(this.rows);
        this.stale = true;
    }

    /**
     * Paints the layer again where it changed.
     *
     * @param mayPaintAll false when another layer got painted completely in this frame already. A layer that has to be painted
     *                    completely then waits for a later frame, so at most one layer gets painted completely per frame.
     * @return true if the layer got painted completely
     */
    boolean update(float time, float newClearX, float newClearY, boolean mayPaintAll) {
        if ((this.width <= 0) || (this.rows <= 0) || !this.layer.prepareNoise(this.rows)) return false;
        int newOffsetA = this.layer.offsetA(time);
        if ((this.stale || (newOffsetA != this.offsetA)) && mayPaintAll) {
            this.stale = false;
            this.offsetA = newOffsetA;
            // The slower texture only moves on together with the faster one. It moves at less than half of its speed,
            // so its steps come at most one step of the faster texture late, which saves a third of the repaints.
            this.offsetB = this.layer.offsetB(time);
            this.thresholdWave = this.layer.thresholdWave(time);
            this.clearX = newClearX;
            this.clearY = newClearY;
            this.paint(0, this.rows, 0, this.width);
            return true;
        }
        if (this.stale) return false;
        boolean wasClearing = !Float.isNaN(this.clearX);
        boolean isClearing = !Float.isNaN(newClearX);
        if ((wasClearing == isClearing) && (!isClearing || ((newClearX == this.clearX) && (newClearY == this.clearY)))) return false;
        float oldX = this.clearX;
        float oldY = this.clearY;
        this.clearX = newClearX;
        this.clearY = newClearY;
        // Only the areas around the old and the new pointer position change. Areas far apart from each other can't reach into each other,
        // so they get painted one by one instead of as one big area.
        if (wasClearing && isClearing && clearAreasTouch(oldX, oldY, newClearX, newClearY)) {
            this.paintAround(Math.min(oldX, newClearX), Math.min(oldY, newClearY), Math.max(oldX, newClearX), Math.max(oldY, newClearY));
            return false;
        }
        if (wasClearing) this.paintAround(oldX, oldY, oldX, oldY);
        if (isClearing) this.paintAround(newClearX, newClearY, newClearX, newClearY);
        return false;
    }

    private static boolean clearAreasTouch(float x1, float y1, float x2, float y2) {
        float reach = 2.0F * FogLayer.CLEAR_RADIUS + 4.0F;
        return (Math.abs(x1 - x2) <= reach) && (Math.abs(y1 - y2) <= reach);
    }

    /**
     * Paints the area that a clearing around any point between two corners can reach, plus the row below it,
     * whose top edge look depends on the last row of the area.
     */
    private void paintAround(float minX, float minY, float maxX, float maxY) {
        int x0 = Math.max(0, PixelMath.floor(minX - FogLayer.CLEAR_RADIUS) - 1);
        int x1 = Math.min(this.width, PixelMath.floor(maxX + FogLayer.CLEAR_RADIUS) + 2);
        int r0 = Math.max(0, PixelMath.floor(minY - FogLayer.CLEAR_RADIUS) - 1 - this.top);
        int r1 = Math.min(this.rows, PixelMath.floor(maxY + FogLayer.CLEAR_RADIUS) + 3 - this.top);
        if ((x1 <= x0) || (r1 <= r0)) return;
        this.paint(r0, r1, x0, x1);
    }

    private void paint(int r0, int r1, int x0, int x1) {
        this.layer.paint(this.canvas.pixels(), this.levels, this.width, this.rows, r0, r1, x0, x1, this.offsetA, this.offsetB, this.thresholdWave, this.top, this.clearX, this.clearY);
        this.canvas.markDirty(x0, r0, x1 - x0, r1 - r0);
    }

    /**
     * The screen row of the layer's top edge.
     */
    int top() {
        return this.top;
    }

}
