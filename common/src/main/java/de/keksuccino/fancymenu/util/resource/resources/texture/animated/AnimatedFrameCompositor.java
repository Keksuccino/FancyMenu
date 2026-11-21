package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Composes the frames of an animated image onto its canvas, the way the APNG specification and browsers (for APNG and GIF images) do it: every play starts on a fully transparent canvas,
 * a frame disposes its region (keep, clear or revert) before the next frame gets composed, and frames either replace their region or get alpha-composited over it.<br>
 * The canvas holds 8-bit ABGR pixels with straight alpha, which is what textures use, so the composed pixels can get uploaded without converting them.<br>
 * Not thread-safe, a compositor belongs to one decoding thread at a time.
 */
final class AnimatedFrameCompositor implements AnimatedFrameDecoder.RowConsumer {

    @NotNull
    private final AnimatedImage image;
    private final int canvasWidth;
    private final int canvasHeight;
    @Nullable
    private int[] canvas;
    /** What the region of a frame with {@link AnimatedImageFrame#DISPOSE_OP_PREVIOUS} showed before that frame got composed. **/
    @Nullable
    private int[] savedRegion;
    private boolean canvasCleared = true;
    private int pendingDisposeOp = AnimatedImageFrame.DISPOSE_OP_NONE;
    private int pendingX;
    private int pendingY;
    private int pendingWidth;
    private int pendingHeight;
    private int frameX;
    private int frameY;
    private int frameBlendOp;

    AnimatedFrameCompositor(@NotNull AnimatedImage image) {
        this.image = image;
        this.canvasWidth = image.getWidth();
        this.canvasHeight = image.getHeight();
    }

    /**
     * Disposes the previously composed frame, composes the given frame on the canvas and stores the canvas region that changed into the update.<br>
     * The first frame always starts a new play on a transparent canvas, so its update covers the whole canvas.
     *
     * @throws IOException If the frame data is corrupted. The canvas then shows a partially composed frame, so the next composed frame has to be the first one.
     */
    void composeFrame(int frameIndex, @NotNull AnimatedFrameDecoder decoder, @NotNull AnimatedFrameUpdate update) throws IOException {
        AnimatedImageFrame frame = this.image.getFrame(frameIndex);
        int[] pixels = this.ensureCanvas();
        if (frameIndex == 0) {
            // Even frames that cover the canvas need a cleared one, because decoders leave the pixels that broken frame data doesn't reach untouched
            if (!this.canvasCleared) Arrays.fill(pixels, 0);
            update.setFullCanvas(this.canvasWidth, this.canvasHeight);
        } else {
            if (this.pendingDisposeOp == AnimatedImageFrame.DISPOSE_OP_BACKGROUND) {
                this.fillPendingRegion(pixels);
                update.addRectangle(this.pendingX, this.pendingY, this.pendingWidth, this.pendingHeight);
            } else if (this.pendingDisposeOp == AnimatedImageFrame.DISPOSE_OP_PREVIOUS) {
                this.restorePendingRegion(pixels);
                update.addRectangle(this.pendingX, this.pendingY, this.pendingWidth, this.pendingHeight);
            }
            update.addRectangle(frame.x(), frame.y(), frame.width(), frame.height());
        }
        int disposeOp = resolveDisposeOp(frameIndex, frame);
        if (disposeOp == AnimatedImageFrame.DISPOSE_OP_PREVIOUS) this.saveRegion(pixels, frame);
        this.pendingDisposeOp = AnimatedImageFrame.DISPOSE_OP_NONE;
        this.canvasCleared = false;
        this.frameX = frame.x();
        this.frameY = frame.y();
        this.frameBlendOp = frame.blendOp();
        decoder.decode(frameIndex, this);
        this.setPendingDispose(disposeOp, frame);
    }

    /**
     * Sets the canvas to the state right after the first frame got composed, from a snapshot of the whole canvas.
     */
    void restoreFirstFrame(@NotNull ByteBuffer snapshot) {
        int[] pixels = this.ensureCanvas();
        snapshot.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels, 0, pixels.length);
        AnimatedImageFrame first = this.image.getFrame(0);
        this.canvasCleared = false;
        this.setPendingDispose(resolveDisposeOp(0, first), first);
    }

    /**
     * Copies the canvas pixels of all rectangles of the update into the buffer, starting at its position 0.
     */
    void writePixels(@NotNull AnimatedFrameUpdate update, @NotNull ByteBuffer target) {
        int[] pixels = this.ensureCanvas();
        IntBuffer out = target.duplicate().order(ByteOrder.LITTLE_ENDIAN).clear().asIntBuffer();
        for (int i = 0; i < update.getRectangleCount(); i++) {
            int x = update.getRectangleX(i);
            int y = update.getRectangleY(i);
            int width = update.getRectangleWidth(i);
            int height = update.getRectangleHeight(i);
            if (width == this.canvasWidth) {
                out.put(pixels, y * this.canvasWidth, width * height);
                continue;
            }
            for (int row = 0; row < height; row++) {
                out.put(pixels, (y + row) * this.canvasWidth + x, width);
            }
        }
    }

    /**
     * Frees the canvas. The next composed frame has to be the first one, or the canvas has to get restored from a snapshot first.
     */
    void release() {
        this.canvas = null;
        this.savedRegion = null;
        this.canvasCleared = true;
        this.pendingDisposeOp = AnimatedImageFrame.DISPOSE_OP_NONE;
    }

    @Override
    public void acceptRow(int y, int x, int step, @NotNull int[] rowPixels, int count) {
        int[] pixels = this.canvas;
        int index = (this.frameY + y) * this.canvasWidth + this.frameX + x;
        if (this.frameBlendOp == AnimatedImageFrame.BLEND_OP_SOURCE) {
            if (step == 1) {
                System.arraycopy(rowPixels, 0, pixels, index, count);
                return;
            }
            for (int i = 0; i < count; i++, index += step) {
                pixels[index] = rowPixels[i];
            }
            return;
        }
        for (int i = 0; i < count; i++, index += step) {
            pixels[index] = blendOver(rowPixels[i], pixels[index]);
        }
    }

    /**
     * Composites a straight-alpha ABGR source pixel over a straight-alpha ABGR destination pixel, with the formulas of the APNG specification, rounded to the nearest value.
     * Fully transparent source pixels keep the destination, like transparent GIF pixels do.
     */
    static int blendOver(int source, int destination) {
        int sourceAlpha = source >>> 24;
        if (sourceAlpha == 255) return source;
        if (sourceAlpha == 0) return destination;
        int destinationAlpha = destination >>> 24;
        if (destinationAlpha == 0) return source;
        // All values are scaled by 255 * 255, so the math stays in integers
        int sourceWeight = sourceAlpha * 255;
        int destinationWeight = destinationAlpha * (255 - sourceAlpha);
        int totalWeight = sourceWeight + destinationWeight;
        int half = totalWeight >>> 1;
        int red = ((source & 0xFF) * sourceWeight + (destination & 0xFF) * destinationWeight + half) / totalWeight;
        int green = (((source >>> 8) & 0xFF) * sourceWeight + ((destination >>> 8) & 0xFF) * destinationWeight + half) / totalWeight;
        int blue = (((source >>> 16) & 0xFF) * sourceWeight + ((destination >>> 16) & 0xFF) * destinationWeight + half) / totalWeight;
        int alpha = (totalWeight + 127) / 255;
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    /**
     * Reverting the first frame means clearing it, because the canvas was fully transparent before it. The APNG specification says so, and browsers do the same for GIF images.
     */
    private static int resolveDisposeOp(int frameIndex, @NotNull AnimatedImageFrame frame) {
        int disposeOp = frame.disposeOp();
        return ((frameIndex == 0) && (disposeOp == AnimatedImageFrame.DISPOSE_OP_PREVIOUS)) ? AnimatedImageFrame.DISPOSE_OP_BACKGROUND : disposeOp;
    }

    @NotNull
    private int[] ensureCanvas() {
        int[] pixels = this.canvas;
        if (pixels == null) {
            pixels = new int[this.canvasWidth * this.canvasHeight];
            this.canvas = pixels;
            this.canvasCleared = true;
        }
        return pixels;
    }

    private void setPendingDispose(int disposeOp, @NotNull AnimatedImageFrame frame) {
        this.pendingDisposeOp = disposeOp;
        this.pendingX = frame.x();
        this.pendingY = frame.y();
        this.pendingWidth = frame.width();
        this.pendingHeight = frame.height();
    }

    private void fillPendingRegion(@NotNull int[] pixels) {
        for (int row = 0; row < this.pendingHeight; row++) {
            int start = (this.pendingY + row) * this.canvasWidth + this.pendingX;
            Arrays.fill(pixels, start, start + this.pendingWidth, 0);
        }
    }

    private void saveRegion(@NotNull int[] pixels, @NotNull AnimatedImageFrame frame) {
        int size = frame.width() * frame.height();
        int[] saved = this.savedRegion;
        if ((saved == null) || (saved.length < size)) {
            saved = new int[size];
            this.savedRegion = saved;
        }
        for (int row = 0; row < frame.height(); row++) {
            System.arraycopy(pixels, (frame.y() + row) * this.canvasWidth + frame.x(), saved, row * frame.width(), frame.width());
        }
    }

    private void restorePendingRegion(@NotNull int[] pixels) {
        int[] saved = this.savedRegion;
        if (saved == null) return;
        for (int row = 0; row < this.pendingHeight; row++) {
            System.arraycopy(saved, row * this.pendingWidth, pixels, (this.pendingY + row) * this.canvasWidth + this.pendingX, this.pendingWidth);
        }
    }

}
