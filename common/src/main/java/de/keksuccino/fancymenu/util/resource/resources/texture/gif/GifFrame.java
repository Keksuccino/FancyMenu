package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageFrame;

/**
 * One frame (image descriptor) of a GIF image: the canvas region it covers, how long it stays visible, how it gets composed onto the canvas and where its compressed data lies in the source.<br>
 * Frames keep no pixels, no local color table and no LZW data, so a parsed image needs only a few bytes per frame, no matter how big its file is.<br><br>
 *
 * GIF frames can reach past the canvas, so the region (x, y, width, height) is the image rectangle clipped to the canvas. Image offsets are never negative,
 * so clipping only cuts off the right and bottom part: a visible region always starts at the image offset, and the visible pixels are the first columns of the first rows of the image.
 * Frames that lie completely outside the canvas, and frames with zero width or height, have an empty region at 0,0.
 *
 * @param delayNanos How long the frame stays visible, with tiny delays already replaced by 100 ms, like browsers do it.
 * @param imageX Left offset of the unclipped image rectangle on the logical screen.
 * @param imageY Top offset of the unclipped image rectangle on the logical screen.
 * @param imageWidth Width of the unclipped image, which is the length of every row of the LZW data.
 * @param imageHeight Height of the unclipped image, which is how many rows the LZW data has.
 * @param interlaced If the rows of the LZW data come in the four passes of GIF interlacing.
 * @param transparentIndex The color index that is fully transparent, or {@link #NO_TRANSPARENT_INDEX}.
 * @param localColorTableSize How many entries the local color table has, or 0 if the frame uses the global color table.
 * @param lzwMinimumCodeSize The LZW minimum code size, between 1 and 8.
 * @param dataPosition Source position of the frame data, which starts with the local color table (if the frame has one), followed by the LZW minimum code size byte and the data sub-blocks.
 * @param dataEnd Source position right after the block terminator of the data sub-blocks.
 * @param dataCrc CRC32 of all bytes from {@code dataPosition} to {@code dataEnd}, so decoding notices when the source changed. GIF images have no checksums of their own.
 */
public record GifFrame(int x, int y, int width, int height, long delayNanos, int disposeOp, int blendOp, int imageX, int imageY, int imageWidth, int imageHeight, boolean interlaced, int transparentIndex, int localColorTableSize, int lzwMinimumCodeSize, long dataPosition, long dataEnd, int dataCrc) implements AnimatedImageFrame {

    /** Transparent index of frames without a transparent color. **/
    public static final int NO_TRANSPARENT_INDEX = -1;

    /**
     * If the frame has visible pixels on the canvas. Frames without any only show the canvas for their delay, so their data never needs to get decoded.
     */
    public boolean hasVisiblePixels() {
        return (this.width > 0) && (this.height > 0);
    }

    /**
     * How many bytes the frame data has in the source, local color table and block terminator included.
     */
    public long dataLength() {
        return this.dataEnd - this.dataPosition;
    }

}
