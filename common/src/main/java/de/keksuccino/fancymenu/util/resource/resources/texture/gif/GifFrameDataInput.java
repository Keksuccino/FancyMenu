package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSource;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSourceChangedException;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Reads the data of GIF frames from the source of their image: the local color table, the LZW minimum code size and the LZW data of the sub-blocks.<br>
 * The data of a frame always lies in one piece, so it gets read with as few reads as the reusable buffer allows: one read for frames that fit the buffer.
 * The LZW data gets handed out without the length bytes of its sub-blocks, so the LZW decoder sees one continuous stream and can read it 8 bytes at a time.
 * Bytes outside the data of a frame never get read.<br><br>
 *
 * GIF images have no checksums, so every frame got a CRC32 of its data when the image got parsed, and all data of a frame runs through a CRC again while it gets read.
 * The LZW decoder stops at the last pixel it needs, so {@link #finish(int)} must get called once decoding a frame stopped, no matter why: it reads the rest of the data,
 * checks that the sub-blocks still end where they got parsed and compares the CRC, so changed bytes never go unnoticed and never get mistaken for broken data.
 * A mismatch, or a source that ends too early or can't get read, throws an {@link AnimatedImageSourceChangedException}.<br>
 * Not thread-safe, every decoder has its own instance.
 */
final class GifFrameDataInput {

    /** Biggest read buffer. Images with smaller frames get a buffer that just fits the data of their biggest frame. **/
    private static final int MAX_BUFFER_SIZE = 64 * 1024;
    /**
     * Smallest read buffer. The first read of a frame always has to bring the whole local color table (up to 768 bytes) and the LZW minimum code size byte after it,
     * which {@link #start(GifFrame, int[])} reads straight from the buffer.
     */
    private static final int MIN_BUFFER_SIZE = 1024;

    @NotNull
    private final AnimatedImageSource source;
    @NotNull
    private final byte[] buffer;
    @NotNull
    private final ByteBuffer bufferView;
    private final CRC32 crc = new CRC32();

    /** The source position of the next byte that gets read. **/
    private long readPosition;
    /** The source position right after the block terminator of the frame data. Reads never go past it. **/
    private long end;
    /** The source position of the first byte in the buffer. **/
    private long bufferStart;
    /** The next byte of the buffer that did not get looked at yet. **/
    private int rawPosition;
    private int rawLimit;
    /** Bytes of the current sub-block that did not get handed out yet, they can continue in the next read. **/
    private int subBlockRemaining;
    private boolean terminated;

    GifFrameDataInput(@NotNull GifImage image) {
        this.source = image.getSource();
        this.buffer = new byte[resolveBufferSize(image)];
        this.bufferView = ByteBuffer.wrap(this.buffer);
    }

    /**
     * Starts reading the data of a frame. The local color table of frames that have one gets converted into the palette, the palettes of other frames stay untouched.
     */
    void start(@NotNull GifFrame frame, @NotNull int[] palette) throws IOException {
        this.readPosition = frame.dataPosition();
        this.end = frame.dataEnd();
        this.crc.reset();
        this.subBlockRemaining = 0;
        this.terminated = false;
        this.fillBuffer();
        // The buffer always fits the color table and the code size byte, and the frame data is always longer than them, because its block terminator comes after them
        int entries = frame.localColorTableSize();
        if (entries > 0) GifReader.fillPalette(this.buffer, entries, palette);
        this.rawPosition = entries * 3;
        if ((this.buffer[this.rawPosition++] & 0xFF) != frame.lzwMinimumCodeSize()) {
            throw new AnimatedImageSourceChangedException("The LZW minimum code size of a GIF frame does not match the one that got parsed anymore");
        }
    }

    /**
     * The buffer that {@link #nextData()} puts the LZW data into.
     */
    @NotNull
    byte[] buffer() {
        return this.buffer;
    }

    /**
     * Puts the next LZW data of the frame at the start of the buffer, without the length bytes of the sub-blocks. Must only get called once all data it handed out before got used,
     * because it reuses the buffer.
     *
     * @return How many bytes of LZW data the buffer holds now, or 0 if the frame has no data left.
     */
    int nextData() throws IOException {
        byte[] data = this.buffer;
        while (!this.terminated) {
            if (this.rawPosition == this.rawLimit) this.fillBuffer();
            int out = 0;
            int position = this.rawPosition;
            int limit = this.rawLimit;
            int remaining = this.subBlockRemaining;
            while (position < limit) {
                if (remaining == 0) {
                    remaining = data[position++] & 0xFF;
                    if (remaining == 0) {
                        // The block terminator has to be the last byte of the parsed frame data
                        if ((this.bufferStart + position) != this.end) throw new AnimatedImageSourceChangedException("The data sub-blocks of a GIF frame don't end where they got parsed anymore");
                        this.terminated = true;
                        break;
                    }
                    continue;
                }
                // Moves the data of the sub-block over the length bytes before it. The target never lies after the source, so the bytes that still get looked at stay intact
                int count = Math.min(remaining, limit - position);
                if (out != position) System.arraycopy(data, position, data, out, count);
                out += count;
                position += count;
                remaining -= count;
            }
            this.rawPosition = position;
            this.subBlockRemaining = remaining;
            if (out > 0) return out;
        }
        return 0;
    }

    /**
     * Reads the rest of the frame data that the decoder didn't need and checks that the data still matches the data that got parsed. Must get called once decoding the frame stopped.
     */
    void finish(int expectedCrc) throws IOException {
        while (this.nextData() > 0) {
            // Only runs the rest of the data through the CRC and the sub-block check
        }
        if ((int) this.crc.getValue() != expectedCrc) throw new AnimatedImageSourceChangedException("The data of a GIF frame does not match the data that got parsed anymore");
    }

    /**
     * Reads as much of the rest of the frame data into the buffer as fits, and runs it through the CRC.
     */
    private void fillBuffer() throws IOException {
        long remaining = this.end - this.readPosition;
        // The parsed sub-blocks ended right before the end, so needing more data means they got longer
        if (remaining <= 0L) throw new AnimatedImageSourceChangedException("The data sub-blocks of a GIF frame don't end where they got parsed anymore");
        int wanted = (int) Math.min(this.buffer.length, remaining);
        this.bufferView.limit(wanted).position(0);
        int filled = 0;
        while (filled < wanted) {
            int read;
            try {
                read = this.source.read(this.bufferView, this.readPosition + filled);
            } catch (IOException ex) {
                throw new AnimatedImageSourceChangedException("Failed to read the frame data of the GIF image from its source", ex);
            }
            if (read <= 0) throw new AnimatedImageSourceChangedException("The source of the GIF image ends before the frame data, so it got shorter");
            filled += read;
        }
        this.crc.update(this.buffer, 0, wanted);
        this.bufferStart = this.readPosition;
        this.readPosition += wanted;
        this.rawPosition = 0;
        this.rawLimit = wanted;
    }

    private static int resolveBufferSize(@NotNull GifImage image) {
        long biggestFrame = 0L;
        for (int i = 0; (i < image.getFrameCount()) && (biggestFrame < MAX_BUFFER_SIZE); i++) {
            GifFrame frame = image.getFrame(i);
            // Frames without visible pixels never get decoded
            if (frame.hasVisiblePixels()) biggestFrame = Math.max(biggestFrame, frame.dataLength());
        }
        return (int) Math.max(MIN_BUFFER_SIZE, Math.min(MAX_BUFFER_SIZE, biggestFrame));
    }

}
