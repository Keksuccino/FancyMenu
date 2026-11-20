package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * Paints pixels and sprites into a {@link TiledCanvas} while it repaints an area, clipped to that area, with the same drawing rules
 * as the overlay mockup's {@code Buf}: opaque colors replace the pixel, translucent ones get blended over it.<br>
 * The canvas hands it to its content during {@link TiledCanvas#repaint}. Content should skip items that don't {@link #overlaps overlap}
 * the clip area, which is what keeps repaints cheap.<br><br>
 *
 * A repainted area always lies inside one tile, so the painter writes straight into the pixels of that tile and marks the area as
 * changed once at the end. The tile only gets created when something actually gets painted into it.
 */
public final class CanvasPainter {

    @Nullable
    private TiledCanvas canvas;
    private int tileIndex;
    private int clipX0;
    private int clipY0;
    private int clipX1;
    private int clipY1;
    @Nullable
    private PixelCanvas tile;
    @Nullable
    private int[] pixels;
    private int tileX;
    private int tileY;
    private int stride;

    void begin(@NotNull TiledCanvas canvas, int tileIndex, int x0, int y0, int x1, int y1) {
        this.canvas = canvas;
        this.tileIndex = tileIndex;
        this.clipX0 = x0;
        this.clipY0 = y0;
        this.clipX1 = x1;
        this.clipY1 = y1;
        this.tile = null;
        this.pixels = null;
        this.tileX = canvas.tileX(tileIndex);
        this.tileY = canvas.tileY(tileIndex);
    }

    void end() {
        if (this.tile != null) this.tile.markDirty(this.clipX0 - this.tileX, this.clipY0 - this.tileY, this.clipX1 - this.clipX0, this.clipY1 - this.clipY0);
        this.canvas = null;
        this.tile = null;
        this.pixels = null;
    }

    public int clipX0() {
        return this.clipX0;
    }

    public int clipY0() {
        return this.clipY0;
    }

    /**
     * First column right of the clip area.
     */
    public int clipX1() {
        return this.clipX1;
    }

    /**
     * First row below the clip area.
     */
    public int clipY1() {
        return this.clipY1;
    }

    /**
     * Whether the area (x0, y0 inclusive, x1, y1 exclusive) overlaps the clip area.
     */
    public boolean overlaps(int x0, int y0, int x1, int y1) {
        return (x0 < this.clipX1) && (x1 > this.clipX0) && (y0 < this.clipY1) && (y1 > this.clipY0);
    }

    /**
     * Paints one pixel. Fully transparent colors paint nothing.
     */
    public void pixel(int x, int y, int argb) {
        if ((x < this.clipX0) || (y < this.clipY0) || (x >= this.clipX1) || (y >= this.clipY1)) return;
        int alpha = argb >>> 24;
        if (alpha == 0) return;
        int[] target = this.pixels;
        if (target == null) target = this.openTile();
        int i = (y - this.tileY) * this.stride + (x - this.tileX);
        target[i] = (alpha == 255) ? argb : PixelMath.blendOver(target[i], argb);
    }

    @NotNull
    private int[] openTile() {
        PixelCanvas opened = Objects.requireNonNull(this.canvas, "CanvasPainter can only paint during a repaint").tile(this.tileIndex);
        this.tile = opened;
        this.stride = opened.width();
        this.pixels = opened.pixels();
        return this.pixels;
    }

    /**
     * Paints a sprite at its natural size with its top left corner at the given position, mirrored if asked.
     */
    public void sprite(@NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y, boolean flipX) {
        int w = sprite.width();
        int h = sprite.height();
        if (!this.overlaps(x, y, x + w, y + h)) return;
        int fromY = Math.max(0, this.clipY0 - y);
        int toY = Math.min(h, this.clipY1 - y);
        int fromX = Math.max(0, this.clipX0 - x);
        int toX = Math.min(w, this.clipX1 - x);
        for (int sy = fromY; sy < toY; sy++) {
            for (int sx = fromX; sx < toX; sx++) {
                int color = palette.color(sprite.codeAt(flipX ? (w - 1 - sx) : sx, sy));
                if ((color >>> 24) != 0) this.pixel(x + sx, y + sy, color);
            }
        }
    }

    /**
     * Paints a filled rectangle.
     */
    public void rect(int x, int y, int width, int height, int argb) {
        int x0 = Math.max(x, this.clipX0);
        int y0 = Math.max(y, this.clipY0);
        int x1 = Math.min(x + width, this.clipX1);
        int y1 = Math.min(y + height, this.clipY1);
        for (int py = y0; py < y1; py++) {
            for (int px = x0; px < x1; px++) this.pixel(px, py, argb);
        }
    }

}
