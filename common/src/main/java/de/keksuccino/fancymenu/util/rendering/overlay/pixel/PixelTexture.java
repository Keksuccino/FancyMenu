package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * A GPU texture of the pixel toolkit with a CPU-side image, filtered with nearest neighbor so art pixels stay sharp.<br>
 * The atlas pages and canvases of the toolkit own one each. {@link PixelTextures} creates the game's implementation;
 * it is the only place that knows how the game version creates, uploads and draws textures.<br><br>
 *
 * A texture can get closed from outside: {@link PixelTextures} releases textures nobody drew for a while, so their owners have to
 * check {@link #isClosed()} before using them and create a new one if needed.
 */
public interface PixelTexture {

    int width();

    int height();

    /**
     * Copies a block of ARGB pixels into the CPU-side image, without uploading it.
     *
     * @param source row-major ARGB pixels
     * @param sourceOffset index of the first pixel of the block in {@code source}
     * @param sourceStride pixels per row of {@code source}
     */
    void write(@NotNull int[] source, int sourceOffset, int sourceStride, int x, int y, int width, int height);

    /**
     * Uploads the rows {@code firstRow} (inclusive) to {@code endRow} (exclusive) of the CPU-side image to the GPU.
     */
    void upload(int firstRow, int endRow);

    /**
     * Draws a region of the texture as one quad. The quad goes into the toolkit's batch of {@link PixelTextures} and keeps its place
     * in the drawing order; {@link PixelGraphics} draws the batch at the end of every part.
     *
     * @param x left edge of the quad in GUI pixels
     * @param y top edge of the quad in GUI pixels
     * @param width width of the quad in GUI pixels
     * @param height height of the quad in GUI pixels
     * @param u left edge of the region in texture pixels
     * @param v top edge of the region in texture pixels
     * @param regionWidth width of the region in texture pixels
     * @param regionHeight height of the region in texture pixels
     * @param color ARGB color the texture gets multiplied with, -1 (opaque white) for none
     */
    void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, boolean flipX, boolean flipY, int color);

    boolean isClosed();

    /**
     * Releases the GPU texture and its CPU-side image. Does nothing if it is closed already.
     */
    void close();

    @FunctionalInterface
    interface Factory {

        @NotNull
        PixelTexture create(@NotNull String name, int width, int height);

    }

}
