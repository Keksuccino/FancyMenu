package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.ByteBuffer;

/**
 * The canvas pixels that changed from one composed frame to the next, ready to get uploaded to the texture.<br>
 * The changed region is made of up to two rectangles. Their RGBA pixels lie in {@link #getPixels()} one rectangle after the other, row by row, without padding.
 */
final class AnimatedFrameUpdate {

    private static final int MAX_RECTANGLES = 2;
    /** Rectangles get merged if their union has at most this many pixels, because every upload has a fixed cost that is bigger than uploading a few extra pixels. **/
    private static final long ALWAYS_MERGED_PIXELS = 64L * 64L;

    private final int frameIndex;
    private final int play;
    private final long delayNanos;
    private final int[] rectangles = new int[MAX_RECTANGLES * 4];
    private int rectangleCount;
    @Nullable
    private ByteBuffer pixels;
    private boolean sharedPixels;
    private boolean contentUnchanged;

    AnimatedFrameUpdate(int frameIndex, int play, long delayNanos) {
        this.frameIndex = frameIndex;
        this.play = play;
        this.delayNanos = delayNanos;
    }

    int getFrameIndex() {
        return this.frameIndex;
    }

    /**
     * Which play of the animation the frame belongs to, starting at 0.
     */
    int getPlay() {
        return this.play;
    }

    /**
     * How long the frame stays visible, never shorter than {@link AnimatedFrameStream#MIN_FRAME_DELAY_NANOS}.
     */
    long getDelayNanos() {
        return this.delayNanos;
    }

    void setFullCanvas(int canvasWidth, int canvasHeight) {
        this.rectangleCount = 0;
        this.setRectangle(0, 0, 0, canvasWidth, canvasHeight);
        this.rectangleCount = 1;
    }

    /**
     * Adds a rectangle to the changed region. Rectangles that are small or lie close together get merged, because one bigger upload is cheaper than two small ones then.
     */
    void addRectangle(int x, int y, int width, int height) {
        if ((width <= 0) || (height <= 0)) return;
        if (this.rectangleCount == 0) {
            this.setRectangle(0, x, y, width, height);
            this.rectangleCount = 1;
            return;
        }
        int minX = Math.min(x, this.rectangles[0]);
        int minY = Math.min(y, this.rectangles[1]);
        int maxX = Math.max(x + width, this.rectangles[0] + this.rectangles[2]);
        int maxY = Math.max(y + height, this.rectangles[1] + this.rectangles[3]);
        if (this.rectangleCount == 2) {
            minX = Math.min(minX, this.rectangles[4]);
            minY = Math.min(minY, this.rectangles[5]);
            maxX = Math.max(maxX, this.rectangles[4] + this.rectangles[6]);
            maxY = Math.max(maxY, this.rectangles[5] + this.rectangles[7]);
            this.setRectangle(0, minX, minY, maxX - minX, maxY - minY);
            this.rectangleCount = 1;
            return;
        }
        long unionArea = (long) (maxX - minX) * (maxY - minY);
        long separateArea = (long) width * height + (long) this.rectangles[2] * this.rectangles[3];
        // Small, overlapping or nearby rectangles get merged, big rectangles far apart (like sprites in different corners) stay separate
        if ((unionArea <= ALWAYS_MERGED_PIXELS) || ((unionArea * 4L) <= (separateArea * 5L))) {
            this.setRectangle(0, minX, minY, maxX - minX, maxY - minY);
        } else {
            this.setRectangle(1, x, y, width, height);
            this.rectangleCount = 2;
        }
    }

    private void setRectangle(int index, int x, int y, int width, int height) {
        int offset = index * 4;
        this.rectangles[offset] = x;
        this.rectangles[offset + 1] = y;
        this.rectangles[offset + 2] = width;
        this.rectangles[offset + 3] = height;
    }

    int getRectangleCount() {
        return this.rectangleCount;
    }

    int getRectangleX(int index) {
        return this.rectangles[index * 4];
    }

    int getRectangleY(int index) {
        return this.rectangles[index * 4 + 1];
    }

    int getRectangleWidth(int index) {
        return this.rectangles[index * 4 + 2];
    }

    int getRectangleHeight(int index) {
        return this.rectangles[index * 4 + 3];
    }

    long getPixelCount() {
        long count = 0L;
        for (int i = 0; i < this.rectangleCount; i++) {
            count += (long) this.getRectangleWidth(i) * this.getRectangleHeight(i);
        }
        return count;
    }

    /**
     * If the update replaces every pixel of the canvas, so updates before it don't need to get uploaded when both get shown in the same render.
     */
    boolean replacesCanvas(int canvasWidth, int canvasHeight) {
        return !this.contentUnchanged && (this.rectangleCount == 1) && (this.rectangles[0] == 0) && (this.rectangles[1] == 0) && (this.rectangles[2] == canvasWidth) && (this.rectangles[3] == canvasHeight);
    }

    /**
     * The RGBA pixels of all rectangles, or NULL if the update has no pixels (yet).
     */
    @Nullable
    ByteBuffer getPixels() {
        return this.pixels;
    }

    /**
     * @param shared If the buffer is owned by someone else (the stored first frame), so it must never get recycled together with the update.
     */
    void setPixels(@NotNull ByteBuffer pixels, boolean shared) {
        this.pixels = pixels;
        this.sharedPixels = shared;
    }

    boolean hasSharedPixels() {
        return this.sharedPixels;
    }

    /**
     * Removes and returns the buffer if it belongs to the update, so it can get reused.
     */
    @Nullable
    ByteBuffer takeOwnedPixels() {
        ByteBuffer owned = this.sharedPixels ? null : this.pixels;
        this.pixels = null;
        return owned;
    }

    /**
     * If the update shows exactly what the texture already shows, so it needs no upload. Animations with a single frame only change their texture once.
     */
    boolean isContentUnchanged() {
        return this.contentUnchanged;
    }

    void setContentUnchanged(boolean contentUnchanged) {
        this.contentUnchanged = contentUnchanged;
    }

}
