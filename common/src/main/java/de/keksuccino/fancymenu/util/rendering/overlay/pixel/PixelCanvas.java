package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;

/**
 * A CPU-side ARGB image that gets drawn as one textured quad, for large content that would need thousands of rects:
 * fog banks, aurora curtains, sunbeams, frost. One canvas pixel is one GUI pixel.<br><br>
 *
 * Write pixels with the methods of this class, or directly into {@link #pixels()} followed by {@link #markDirty}. The canvas
 * tracks the changed rows and uploads only those, only when something changed, the next time it gets drawn.<br>
 * Canvases made with {@code PixelDecorationOverlay#createScreenCanvas} follow the screen size (screen resizes and GUI scale
 * changes) on their own. A resize clears the canvas, so draw its content again after one.<br><br>
 *
 * The GPU texture holds one image per frame: draw a canvas once per frame, and use one canvas per part
 * (for example one for the back part and one for the front part), because a later change in the same frame would show in both.
 * Everything here runs on the render thread.
 */
public final class PixelCanvas {

    @NotNull
    private final String name;
    @NotNull
    private final PixelTexture.Factory textureFactory;
    private int width = 0;
    private int height = 0;
    @NotNull
    private int[] pixels = new int[0];
    @Nullable
    private PixelTexture texture = null;
    /** Changed area since the last upload: minX, minY inclusive and maxX, maxY exclusive. Empty when minX >= maxX. */
    private int dirtyMinX = 0;
    private int dirtyMinY = 0;
    private int dirtyMaxX = 0;
    private int dirtyMaxY = 0;
    /** True while every pixel is known to be transparent, so clearing again needs no upload. */
    private boolean blank = true;

    /**
     * @param name a short name for debugging
     */
    public PixelCanvas(@NotNull String name) {
        this(name, PixelTextures.FACTORY);
    }

    PixelCanvas(@NotNull String name, @NotNull PixelTexture.Factory textureFactory) {
        this.name = name;
        this.textureFactory = textureFactory;
    }

    /**
     * Sets the size of the canvas. A new size clears all pixels and returns true; the same size changes nothing and returns false.
     */
    public boolean resize(int width, int height) {
        width = Math.max(0, width);
        height = Math.max(0, height);
        if ((width == this.width) && (height == this.height)) return false;
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
        this.blank = true;
        this.releaseTexture();
        this.clearDirty();
        return true;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /**
     * The row-major ARGB pixels ({@code y * width() + x}). After writing to it directly, call {@link #markDirty} for the changed area.
     * The array gets replaced when the size changes.
     */
    @NotNull
    public int[] pixels() {
        return this.pixels;
    }

    public int get(int x, int y) {
        if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) return 0;
        return this.pixels[y * this.width + x];
    }

    /**
     * Makes every pixel transparent.
     */
    public void clear() {
        if (this.blank) return;
        Arrays.fill(this.pixels, 0);
        this.extendDirty(0, 0, this.width, this.height);
        this.blank = true;
    }

    /**
     * Sets every pixel to one ARGB color.
     */
    public void fill(int argb) {
        if (argb == 0) {
            this.clear();
            return;
        }
        Arrays.fill(this.pixels, argb);
        this.blank = false;
        this.extendDirty(0, 0, this.width, this.height);
    }

    /**
     * Replaces one pixel. Pixels outside of the canvas are ignored.
     */
    public void set(int x, int y, int argb) {
        if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) return;
        this.pixels[y * this.width + x] = argb;
        if (argb != 0) this.blank = false;
        this.extendDirty(x, y, 1, 1);
    }

    /**
     * Paints a translucent ARGB color over one pixel (alpha blending). Pixels outside of the canvas are ignored.
     */
    public void blend(int x, int y, int argb) {
        if ((argb >>> 24) == 0 || (x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) return;
        int i = y * this.width + x;
        this.pixels[i] = PixelMath.blendOver(this.pixels[i], argb);
        this.blank = false;
        this.extendDirty(x, y, 1, 1);
    }

    /**
     * Replaces a rectangle of pixels, clipped to the canvas.
     */
    public void fillRect(int x, int y, int width, int height, int argb) {
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(this.width, x + width);
        int y1 = Math.min(this.height, y + height);
        if ((x1 <= x0) || (y1 <= y0)) return;
        for (int row = y0; row < y1; row++) {
            int start = row * this.width;
            Arrays.fill(this.pixels, start + x0, start + x1, argb);
        }
        if (argb != 0) this.blank = false;
        this.extendDirty(x0, y0, x1 - x0, y1 - y0);
    }

    /**
     * Paints a translucent ARGB color over a rectangle of pixels, clipped to the canvas.
     */
    public void blendRect(int x, int y, int width, int height, int argb) {
        if ((argb >>> 24) == 255) {
            this.fillRect(x, y, width, height, argb);
            return;
        }
        if ((argb >>> 24) == 0) return;
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(this.width, x + width);
        int y1 = Math.min(this.height, y + height);
        if ((x1 <= x0) || (y1 <= y0)) return;
        for (int row = y0; row < y1; row++) {
            int start = row * this.width;
            for (int i = start + x0; i < start + x1; i++) this.pixels[i] = PixelMath.blendOver(this.pixels[i], argb);
        }
        this.blank = false;
        this.extendDirty(x0, y0, x1 - x0, y1 - y0);
    }

    /**
     * Paints a sprite into the canvas (alpha blending), flipped and whole-number scaled like {@link PixelGraphics#sprite} draws it.
     * Good for content that gets built up once and then barely changes, like frost or moss.
     */
    public void stamp(@NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y, boolean flipX, boolean flipY, int scale) {
        if (scale < 1) return;
        int spriteWidth = sprite.width();
        int spriteHeight = sprite.height();
        int outWidth = spriteWidth * scale;
        int outHeight = spriteHeight * scale;
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(this.width, x + outWidth);
        int y1 = Math.min(this.height, y + outHeight);
        if ((x1 <= x0) || (y1 <= y0)) return;
        boolean painted = false;
        for (int py = y0; py < y1; py++) {
            int sy = (py - y) / scale;
            if (flipY) sy = spriteHeight - 1 - sy;
            int spriteRow = sy * spriteWidth;
            int canvasRow = py * this.width;
            for (int px = x0; px < x1; px++) {
                int sx = (px - x) / scale;
                if (flipX) sx = spriteWidth - 1 - sx;
                int color = palette.colorAt(sprite.codeIndexAt(spriteRow + sx));
                if ((color >>> 24) == 0) continue;
                int i = canvasRow + px;
                this.pixels[i] = PixelMath.blendOver(this.pixels[i], color);
                painted = true;
            }
        }
        if (!painted) return;
        this.blank = false;
        this.extendDirty(x0, y0, x1 - x0, y1 - y0);
    }

    /**
     * Marks a rectangle as changed, after writing to {@link #pixels()} directly.
     */
    public void markDirty(int x, int y, int width, int height) {
        this.blank = false;
        this.extendDirty(x, y, width, height);
    }

    /**
     * Marks the whole canvas as changed, after writing to {@link #pixels()} directly.
     */
    public void markAllDirty() {
        this.markDirty(0, 0, this.width, this.height);
    }

    private void extendDirty(int x, int y, int width, int height) {
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(this.width, x + width);
        int y1 = Math.min(this.height, y + height);
        if ((x1 <= x0) || (y1 <= y0)) return;
        if (this.dirtyMinX >= this.dirtyMaxX) {
            this.dirtyMinX = x0;
            this.dirtyMinY = y0;
            this.dirtyMaxX = x1;
            this.dirtyMaxY = y1;
            return;
        }
        if (x0 < this.dirtyMinX) this.dirtyMinX = x0;
        if (y0 < this.dirtyMinY) this.dirtyMinY = y0;
        if (x1 > this.dirtyMaxX) this.dirtyMaxX = x1;
        if (y1 > this.dirtyMaxY) this.dirtyMaxY = y1;
    }

    public boolean isDirty() {
        return this.dirtyMinX < this.dirtyMaxX;
    }

    private void clearDirty() {
        this.dirtyMinX = 0;
        this.dirtyMinY = 0;
        this.dirtyMaxX = 0;
        this.dirtyMaxY = 0;
    }

    /**
     * Brings the GPU texture up to date and returns it, or NULL for an empty (0x0) canvas.
     * Creates the texture if there is none (first draw, after a resize or after it got released), which uploads everything.
     */
    @Nullable
    PixelTexture flush() {
        if ((this.width <= 0) || (this.height <= 0)) return null;
        PixelTexture current = this.texture;
        if ((current == null) || current.isClosed()) {
            current = this.textureFactory.create("canvas_" + this.name, this.width, this.height);
            this.texture = current;
            // A new texture starts transparent, so a blank canvas needs no upload
            if (this.blank) {
                this.clearDirty();
                return current;
            }
            this.dirtyMinX = 0;
            this.dirtyMinY = 0;
            this.dirtyMaxX = this.width;
            this.dirtyMaxY = this.height;
        }
        if (this.isDirty()) {
            int x0 = this.dirtyMinX;
            int y0 = this.dirtyMinY;
            current.write(this.pixels, y0 * this.width + x0, this.width, x0, y0, this.dirtyMaxX - x0, this.dirtyMaxY - y0);
            current.upload(y0, this.dirtyMaxY);
            this.clearDirty();
        }
        return current;
    }

    /**
     * Releases the GPU texture. The pixels stay, and the next draw uploads them to a new texture.
     */
    public void releaseTexture() {
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }
    }

    /**
     * Releases the GPU texture and drops the pixels. Drawing again needs a {@link #resize} first.
     */
    public void close() {
        this.releaseTexture();
        this.width = 0;
        this.height = 0;
        this.pixels = new int[0];
        this.blank = true;
        this.clearDirty();
    }

}
