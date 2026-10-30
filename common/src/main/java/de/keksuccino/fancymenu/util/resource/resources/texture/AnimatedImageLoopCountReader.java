package de.keksuccino.fancymenu.util.resource.resources.texture;

import org.jetbrains.annotations.NotNull;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads how often animated GIF and APNG images should play, straight from their raw data.<br>
 * Only the structure of the image gets read and all image data gets skipped, so this stays cheap for big images and for streams that should only get read partially.
 */
public final class AnimatedImageLoopCountReader {

    /** Loop count of images that loop forever. **/
    public static final int INFINITE_LOOPS = -1;
    /** Web sources only get read up to this many bytes. Loop counts are near the start of real-world images, while reading further could download the whole image a second time. **/
    public static final long MAX_WEB_SOURCE_BYTES = 1024L * 1024L;

    private static final int APNG_ACTL_CHUNK = 0x6163544C;
    private static final int APNG_IDAT_CHUNK = 0x49444154;

    private AnimatedImageLoopCountReader() {
    }

    /**
     * Returns the loop count of the GIF, {@link #INFINITE_LOOPS} if it loops forever, or 1 if it has no loop extension, which means it only plays once.
     */
    public static int readGifLoopCount(@NotNull byte[] gifData) {
        try {
            return readGifLoopCount(new ByteArrayInputStream(gifData), gifData.length);
        } catch (IOException ex) {
            // Byte array streams never fail
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns the loop count of the GIF, {@link #INFINITE_LOOPS} if it loops forever, or 1 if it has no loop extension, which means it only plays once.<br>
     * Stops reading as soon as the loop extension got found, and never reads more than the given number of bytes.
     */
    public static int readGifLoopCount(@NotNull InputStream in, long maxBytes) throws IOException {
        LimitedReader reader = new LimitedReader(in, maxBytes);
        try {
            reader.skip(10); // header and logical screen size
            int packed = reader.read();
            reader.skip(2); // background color index and pixel aspect ratio
            if ((packed & 0x80) != 0) reader.skip(3L * (1 << ((packed & 0x07) + 1)));
            while (true) {
                int block = reader.read();
                if (block == 0x2C) { // image descriptor
                    reader.skip(8);
                    int localPacked = reader.read();
                    if ((localPacked & 0x80) != 0) reader.skip(3L * (1 << ((localPacked & 0x07) + 1)));
                    reader.skip(1); // LZW minimum code size
                    skipGifSubBlocks(reader);
                } else if (block == 0x21) { // extension
                    int label = reader.read();
                    boolean loopExtension = false;
                    if (label == 0xFF) {
                        int appBlockSize = reader.read();
                        if (appBlockSize == 0) return 1;
                        String appIdentifier = new String(reader.readBytes(appBlockSize), StandardCharsets.US_ASCII);
                        loopExtension = "NETSCAPE2.0".equals(appIdentifier) || "ANIMEXTS1.0".equals(appIdentifier);
                    }
                    int size;
                    while ((size = reader.read()) != 0) {
                        if (loopExtension && (size >= 3)) {
                            byte[] subBlock = reader.readBytes(size);
                            if ((subBlock[0] & 255) == 1) {
                                int loops = (subBlock[1] & 255) | ((subBlock[2] & 255) << 8);
                                return (loops == 0) ? INFINITE_LOOPS : loops;
                            }
                        } else {
                            reader.skip(size);
                        }
                    }
                } else {
                    return 1; // trailer or unknown block
                }
            }
        } catch (EOFException ex) {
            // End of the data or the read limit reached without finding a loop extension
            return 1;
        }
    }

    /**
     * Returns the number of plays of the APNG, or {@link #INFINITE_LOOPS} if it loops forever or has no animation control chunk.
     */
    public static int readApngLoopCount(@NotNull byte[] apngData) {
        try {
            return readApngLoopCount(new ByteArrayInputStream(apngData), apngData.length);
        } catch (IOException ex) {
            // Byte array streams never fail
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns the number of plays of the APNG, or {@link #INFINITE_LOOPS} if it loops forever or has no animation control chunk.<br>
     * The animation control chunk is only valid before the first image data chunk, so reading stops there, and it never reads more than the given number of bytes.
     */
    public static int readApngLoopCount(@NotNull InputStream in, long maxBytes) throws IOException {
        LimitedReader reader = new LimitedReader(in, maxBytes);
        try {
            reader.skip(8); // PNG signature
            while (true) {
                long chunkLength = reader.readInt() & 0xFFFFFFFFL;
                int chunkType = reader.readInt();
                if (chunkType == APNG_ACTL_CHUNK) {
                    reader.skip(4); // number of frames
                    int plays = reader.readInt();
                    return (plays == 0) ? INFINITE_LOOPS : Math.max(1, plays);
                }
                if (chunkType == APNG_IDAT_CHUNK) return INFINITE_LOOPS;
                reader.skip(chunkLength + 4L); // chunk data and CRC
            }
        } catch (EOFException ex) {
            return INFINITE_LOOPS;
        }
    }

    private static void skipGifSubBlocks(@NotNull LimitedReader reader) throws IOException {
        int size;
        while ((size = reader.read()) != 0) {
            reader.skip(size);
        }
    }

    /**
     * Reads from a stream, but never more than its byte limit. Reaching the end of the stream or the limit throws an {@link EOFException}.
     */
    private static final class LimitedReader {

        @NotNull
        private final InputStream in;
        private long remaining;

        private LimitedReader(@NotNull InputStream in, long maxBytes) {
            this.in = in;
            this.remaining = maxBytes;
        }

        private int read() throws IOException {
            if (this.remaining <= 0L) throw new EOFException();
            int value = this.in.read();
            if (value < 0) throw new EOFException();
            this.remaining--;
            return value;
        }

        private int readInt() throws IOException {
            return (this.read() << 24) | (this.read() << 16) | (this.read() << 8) | this.read();
        }

        @NotNull
        private byte[] readBytes(int count) throws IOException {
            if (count > this.remaining) throw new EOFException();
            byte[] bytes = this.in.readNBytes(count);
            if (bytes.length < count) throw new EOFException();
            this.remaining -= count;
            return bytes;
        }

        private void skip(long count) throws IOException {
            if (count > this.remaining) throw new EOFException();
            this.in.skipNBytes(count);
            this.remaining -= count;
        }

    }

}
