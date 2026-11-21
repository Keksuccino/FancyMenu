package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;

/**
 * Decodes the pixels of the frames of one {@link AnimatedImage}, reading their compressed data from the source of the image.<br>
 * Every decoded row goes straight to a {@link RowConsumer}, so decoders only need a few rows of memory, no matter how big a frame is.
 * Not thread-safe, every decoding thread needs its own decoder.
 */
public interface AnimatedFrameDecoder extends AutoCloseable {

    /**
     * Decodes the pixels of the frame and hands them row by row to the consumer, in the coordinates of the frame region.
     * Rows can come in any order (like the rows of interlaced frames), and pixels the decoder never hands over keep what the canvas shows.
     *
     * @throws AnimatedImageSourceChangedException If the source does not hold the data anymore that got parsed from it.
     * @throws IOException If the frame data is corrupted, so the frame can't get shown.
     */
    void decode(int frameIndex, @NotNull RowConsumer consumer) throws IOException;

    /**
     * Releases the resources of the decoder, like native zlib memory.
     */
    @Override
    void close();

    /**
     * Receives the decoded rows of a frame.
     */
    interface RowConsumer {

        /**
         * @param y The row of the pixels, relative to the frame region.
         * @param x The column of the first pixel, relative to the frame region.
         * @param step The column distance of consecutive pixels. Rows of interlaced frames can have only every n-th pixel of their row.
         * @param pixels The pixels of the row as 8-bit ABGR ints with straight alpha (RGBA bytes in little-endian order, which is what textures use).
         *               The array gets reused for the next row, so it must not be kept.
         * @param count How many pixels of the array belong to the row.
         */
        void acceptRow(int y, int x, int step, @NotNull int[] pixels, int count);

    }

}
