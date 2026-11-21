package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageFrame;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Reads GIF87a and GIF89a images block by block from a source. The source gets read once, from start to end.<br>
 * Frames only remember where their data (local color table and LZW sub-blocks) lies in the source and the CRC32 of that data, so parsing keeps no image data in memory,
 * no matter how big the file is. Frames read their data from the source again whenever they get decoded.<br><br>
 *
 * Everything that has no defined meaning gets handled like browsers (Chromium, Firefox and Safari) do it:
 * <ul>
 *     <li>The canvas is the logical screen grown to also fit the first frame (the Chromium/wuffs policy), later frames get clipped to it.</li>
 *     <li>Delays of 0 or 1 centiseconds, and frames without graphic control extension, become 100 ms.</li>
 *     <li>Frames get cleared to transparent for the "restore to background" disposal, never to the background color, and the undefined disposal 4 reverts like disposal 3.</li>
 *     <li>The first loop count of a NETSCAPE2.0 or ANIMEXTS1.0 extension counts repetitions, so a count of N plays N + 1 times. Images without one play once.</li>
 *     <li>Truncated files keep all complete frames, files without trailer are fine, and an unknown block ends the file like a trailer.</li>
 * </ul>
 * Comments, plain text, XMP, ICC and all other extensions don't change the shown pixels and get skipped.
 */
public final class GifReader {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int READ_BUFFER_SIZE = 64 * 1024;
    private static final int HEADER_LENGTH = 13;
    private static final int IMAGE_DESCRIPTOR_LENGTH = 9;
    private static final int APPLICATION_IDENTIFIER_LENGTH = 11;
    private static final int BLOCK_IMAGE = 0x2C;
    private static final int BLOCK_EXTENSION = 0x21;
    private static final int BLOCK_TRAILER = 0x3B;
    private static final int EXTENSION_GRAPHIC_CONTROL = 0xF9;
    private static final int EXTENSION_APPLICATION = 0xFF;
    private static final int GRAPHIC_CONTROL_LENGTH = 4;
    private static final int LOOP_SUB_BLOCK_ID = 1;
    private static final int LOOP_SUB_BLOCK_LENGTH = 3;
    private static final byte[] NETSCAPE_IDENTIFIER = "NETSCAPE2.0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ANIMEXTS_IDENTIFIER = "ANIMEXTS1.0".getBytes(StandardCharsets.US_ASCII);
    private static final long NANOS_PER_CENTISECOND = 10_000_000L;
    /** Delays up to this many centiseconds are too short for browsers, so they show these frames for {@link #DEFAULT_DELAY_NANOS}. **/
    private static final int MAX_TOO_SHORT_DELAY_CENTISECONDS = 1;
    private static final long DEFAULT_DELAY_NANOS = 100_000_000L;
    private static final int MIN_LZW_CODE_SIZE = 1;
    private static final int MAX_LZW_CODE_SIZE = 8;
    private static final int OPAQUE_BLACK = 0xFF000000;

    @NotNull
    private final AnimatedImageSource source;
    @NotNull
    private final String sourceName;
    private final CRC32 crc = new CRC32();
    /** Holds the fixed-size fields of blocks, of which the logical screen descriptor is the longest. **/
    private final byte[] scratch = new byte[HEADER_LENGTH];
    /** Holds the next bytes of the source. **/
    private final byte[] buffer = new byte[READ_BUFFER_SIZE];
    @NotNull
    private final ByteBuffer bufferView = ByteBuffer.wrap(this.buffer);
    @NotNull
    private final List<GifFrame> frames = new ArrayList<>();

    /** The source position of the first byte in the buffer. **/
    private long bufferStart;
    private int bufferPosition;
    private int bufferLimit;
    /** If the bytes that get consumed run through the CRC. **/
    private boolean crcActive;
    /** The first byte of the buffer that got consumed while the CRC is active, but did not run through it yet. **/
    private int crcMark;
    private int screenWidth;
    private int screenHeight;
    private int canvasWidth;
    private int canvasHeight;
    @NotNull
    private final int[] globalPalette = new int[256];
    private boolean loopCountRead;
    private int loopCount;
    /** The graphic control extension that applies to the next image, it gets reset after every image. **/
    private boolean graphicControlRead;
    private int graphicControlFlags;
    private int graphicControlDelay;
    private int graphicControlTransparentIndex;

    private GifReader(@NotNull AnimatedImageSource source, @NotNull String sourceName) {
        this.source = source;
        this.sourceName = sourceName;
    }

    /**
     * Reads a complete GIF image, starting at position 0 of the source.<br>
     * Takes ownership of the source: the returned image closes it when it gets closed, and if reading fails, the source gets closed right away.
     *
     * @param sourceName Name of the image source, only used for log messages.
     * @throws IOException If the source is no valid GIF image, has no intact frame or reading it failed.
     */
    @NotNull
    public static GifImage read(@NotNull AnimatedImageSource source, @NotNull String sourceName) throws IOException {
        try {
            return new GifReader(source, sourceName).readImage();
        } catch (Throwable throwable) {
            source.close();
            throw throwable;
        }
    }

    @NotNull
    private GifImage readImage() throws IOException {
        try {
            this.readFully(this.scratch, 0, HEADER_LENGTH);
        } catch (EOFException ex) {
            throw new IOException("The GIF image ends before its logical screen descriptor", ex);
        }
        if ((this.scratch[0] != 'G') || (this.scratch[1] != 'I') || (this.scratch[2] != 'F') || (this.scratch[3] != '8') || ((this.scratch[4] != '7') && (this.scratch[4] != '9')) || (this.scratch[5] != 'a')) {
            throw new IOException("Not a GIF image, its signature is invalid");
        }
        this.screenWidth = readShort(this.scratch, 6);
        this.screenHeight = readShort(this.scratch, 8);
        int flags = this.scratch[10] & 0xFF;
        Arrays.fill(this.globalPalette, OPAQUE_BLACK);
        try {
            if ((flags & 0x80) != 0) {
                int entries = 2 << (flags & 0x07);
                byte[] table = new byte[entries * 3];
                this.readFully(table, 0, table.length);
                fillPalette(table, entries, this.globalPalette);
            }
            this.readBlocks();
        } catch (EOFException ex) {
            // Truncated images keep every frame that got read completely, like browsers show partially downloaded images
            if (this.frames.isEmpty()) throw new IOException("The GIF image ends before its first complete frame", ex);
            LOGGER.warn("[FANCYMENU] GIF image ends in the middle of a block, so only its first {} frames get shown: {}", this.frames.size(), this.sourceName);
        }
        if (this.frames.isEmpty()) throw new IOException("The GIF image has no frames");
        int numPlays = !this.loopCountRead ? 1 : ((this.loopCount == 0) ? AnimatedImage.INFINITE_PLAYS : this.loopCount + 1);
        return new GifImage(this.canvasWidth, this.canvasHeight, numPlays, this.globalPalette, this.frames.toArray(new GifFrame[0]), this.source, this.sourceName);
    }

    /**
     * Reads all blocks until the trailer, an unknown block, a broken frame or the end of the source between two blocks.
     *
     * @throws EOFException If the source ends in the middle of a block.
     */
    private void readBlocks() throws IOException {
        while (true) {
            int introducer = this.readByteOrEnd();
            if (introducer < 0) {
                if (!this.frames.isEmpty()) LOGGER.warn("[FANCYMENU] GIF image has no trailer: {}", this.sourceName);
                return;
            }
            if (introducer == BLOCK_IMAGE) {
                if (!this.readImageBlock()) return;
            } else if (introducer == BLOCK_EXTENSION) {
                this.readExtension();
            } else if (introducer == BLOCK_TRAILER) {
                return;
            } else {
                // Browsers stop at bytes that start no known block, so everything after them is no part of the image
                LOGGER.warn("[FANCYMENU] GIF image has an unknown block (0x{}), so everything after it gets ignored: {}", Integer.toHexString(introducer), this.sourceName);
                return;
            }
        }
    }

    /**
     * Reads an image descriptor and runs the frame data after it through the CRC. The data itself is not needed now, frames read it again when they get decoded.
     *
     * @return False if the frame is broken, which ends the animation before it.
     */
    private boolean readImageBlock() throws IOException {
        this.readFully(this.scratch, 0, IMAGE_DESCRIPTOR_LENGTH);
        int imageX = readShort(this.scratch, 0);
        int imageY = readShort(this.scratch, 2);
        int imageWidth = readShort(this.scratch, 4);
        int imageHeight = readShort(this.scratch, 6);
        int flags = this.scratch[8] & 0xFF;
        boolean interlaced = (flags & 0x40) != 0;
        int localColorTableSize = ((flags & 0x80) != 0) ? (2 << (flags & 0x07)) : 0;
        if (this.frames.isEmpty()) {
            this.initCanvas(imageX, imageY, imageWidth, imageHeight);
        } else if (((long) imageWidth * imageHeight) > AnimatedImage.MAX_PIXEL_COUNT) {
            // Decoding goes through all rows up to the last visible one, which are nearly all rows of interlaced frames, so frames bigger than any canvas
            // would waste a lot of decoding time on every play. The first frame always fits the limit, because the canvas has to fit it.
            LOGGER.warn("[FANCYMENU] The animation of a GIF image is broken, because one of its frames is too big ({}x{}), so it only plays the {} frames before that: {}", imageWidth, imageHeight, this.frames.size(), this.sourceName);
            return false;
        }
        long dataPosition = this.position();
        this.startCrc();
        this.skip(localColorTableSize * 3);
        int lzwMinimumCodeSize = this.readByte();
        if ((lzwMinimumCodeSize < MIN_LZW_CODE_SIZE) || (lzwMinimumCodeSize > MAX_LZW_CODE_SIZE)) {
            // Parsing stops here, so the CRC that is still running never gets used
            if (this.frames.isEmpty()) throw new IOException("The first frame of the GIF image has an invalid LZW minimum code size: " + lzwMinimumCodeSize);
            LOGGER.warn("[FANCYMENU] The animation of a GIF image is broken, because one of its frames has an invalid LZW minimum code size ({}), so it only plays the {} frames before that: {}", lzwMinimumCodeSize, this.frames.size(), this.sourceName);
            return false;
        }
        this.skipSubBlocks();
        int dataCrc = this.finishCrc();
        this.frames.add(this.buildFrame(imageX, imageY, imageWidth, imageHeight, interlaced, localColorTableSize, lzwMinimumCodeSize, dataPosition, this.position(), dataCrc));
        this.graphicControlRead = false;
        return true;
    }

    /**
     * The canvas is the logical screen, grown to the bottom right corner of the first frame, like in Chromium (wuffs). Some encoders write a logical screen that is too small,
     * or even 0x0, and expect the first frame to define the size. Image offsets are relative to the top left corner of the logical screen, so the canvas always starts there.
     */
    private void initCanvas(int imageX, int imageY, int imageWidth, int imageHeight) throws IOException {
        this.canvasWidth = Math.max(this.screenWidth, imageX + imageWidth);
        this.canvasHeight = Math.max(this.screenHeight, imageY + imageHeight);
        if ((this.canvasWidth <= 0) || (this.canvasHeight <= 0)) throw new IOException("The GIF image has an empty canvas: " + this.canvasWidth + "x" + this.canvasHeight);
        if ((this.canvasWidth > AnimatedImage.MAX_DIMENSION) || (this.canvasHeight > AnimatedImage.MAX_DIMENSION) || (((long) this.canvasWidth * this.canvasHeight) > AnimatedImage.MAX_PIXEL_COUNT)) {
            throw new IOException("GIF image is too big to get used as texture: " + this.canvasWidth + "x" + this.canvasHeight);
        }
    }

    @NotNull
    private GifFrame buildFrame(int imageX, int imageY, int imageWidth, int imageHeight, boolean interlaced, int localColorTableSize, int lzwMinimumCodeSize, long dataPosition, long dataEnd, int dataCrc) {
        // Image offsets are never negative, so clipping only cuts off the right and bottom part of frames
        int x = Math.min(imageX, this.canvasWidth);
        int y = Math.min(imageY, this.canvasHeight);
        int width = Math.min(imageX + imageWidth, this.canvasWidth) - x;
        int height = Math.min(imageY + imageHeight, this.canvasHeight) - y;
        if ((width == 0) || (height == 0)) {
            x = 0;
            y = 0;
            width = 0;
            height = 0;
        }
        long delayNanos = DEFAULT_DELAY_NANOS;
        int disposeOp = AnimatedImageFrame.DISPOSE_OP_NONE;
        int transparentIndex = GifFrame.NO_TRANSPARENT_INDEX;
        if (this.graphicControlRead) {
            if (this.graphicControlDelay > MAX_TOO_SHORT_DELAY_CENTISECONDS) delayNanos = this.graphicControlDelay * NANOS_PER_CENTISECOND;
            disposeOp = toDisposeOp((this.graphicControlFlags >>> 2) & 0x07);
            if ((this.graphicControlFlags & 0x01) != 0) transparentIndex = this.graphicControlTransparentIndex;
        }
        // Only frames with transparent pixels need blending, all others take the fast path that copies whole rows
        int blendOp = (transparentIndex != GifFrame.NO_TRANSPARENT_INDEX) ? AnimatedImageFrame.BLEND_OP_OVER : AnimatedImageFrame.BLEND_OP_SOURCE;
        return new GifFrame(x, y, width, height, delayNanos, disposeOp, blendOp, imageX, imageY, imageWidth, imageHeight, interlaced, transparentIndex, localColorTableSize, lzwMinimumCodeSize, dataPosition, dataEnd, dataCrc);
    }

    /**
     * Disposal 2 clears to transparent, because browsers never use the background color. Disposal 4 is undefined, Firefox and Skia handle it like 3, and the other undefined values keep the frame.
     */
    private static int toDisposeOp(int disposalMethod) {
        return switch (disposalMethod) {
            case 2 -> AnimatedImageFrame.DISPOSE_OP_BACKGROUND;
            case 3, 4 -> AnimatedImageFrame.DISPOSE_OP_PREVIOUS;
            default -> AnimatedImageFrame.DISPOSE_OP_NONE;
        };
    }

    private void readExtension() throws IOException {
        int label = this.readByte();
        if (label == EXTENSION_GRAPHIC_CONTROL) {
            this.readGraphicControl();
        } else if (label == EXTENSION_APPLICATION) {
            this.readApplicationExtension();
        } else {
            // Comments, plain text and unknown extensions don't change the shown pixels
            this.skipSubBlocks();
        }
    }

    /**
     * Graphic control extensions with less than 4 bytes get ignored. Longer ones only use their first 4 bytes, everything after them gets skipped.
     */
    private void readGraphicControl() throws IOException {
        int length = this.readByte();
        if (length == 0) return;
        if (length < GRAPHIC_CONTROL_LENGTH) {
            this.skip(length);
            this.skipSubBlocks();
            return;
        }
        this.readFully(this.scratch, 0, GRAPHIC_CONTROL_LENGTH);
        this.skip(length - GRAPHIC_CONTROL_LENGTH);
        this.skipSubBlocks();
        this.graphicControlRead = true;
        this.graphicControlFlags = this.scratch[0] & 0xFF;
        this.graphicControlDelay = readShort(this.scratch, 1);
        this.graphicControlTransparentIndex = this.scratch[3] & 0xFF;
    }

    /**
     * Reads the loop count from the first looping sub-block of a NETSCAPE2.0 or ANIMEXTS1.0 extension. All other application extensions (XMP, ICC profiles, etc.) and sub-blocks get skipped.
     */
    private void readApplicationExtension() throws IOException {
        int length = this.readByte();
        if (length == 0) return;
        boolean loopExtension = false;
        if (length == APPLICATION_IDENTIFIER_LENGTH) {
            this.readFully(this.scratch, 0, APPLICATION_IDENTIFIER_LENGTH);
            loopExtension = Arrays.equals(this.scratch, 0, APPLICATION_IDENTIFIER_LENGTH, NETSCAPE_IDENTIFIER, 0, APPLICATION_IDENTIFIER_LENGTH) || Arrays.equals(this.scratch, 0, APPLICATION_IDENTIFIER_LENGTH, ANIMEXTS_IDENTIFIER, 0, APPLICATION_IDENTIFIER_LENGTH);
        } else {
            this.skip(length);
        }
        while ((length = this.readByte()) != 0) {
            // Sub-block 1 holds the loop count, sub-block 2 (buffering hints) has no meaning for already loaded images
            if (!loopExtension || this.loopCountRead || (length < LOOP_SUB_BLOCK_LENGTH)) {
                this.skip(length);
                continue;
            }
            this.readFully(this.scratch, 0, LOOP_SUB_BLOCK_LENGTH);
            this.skip(length - LOOP_SUB_BLOCK_LENGTH);
            if (this.scratch[0] == LOOP_SUB_BLOCK_ID) {
                this.loopCount = readShort(this.scratch, 1);
                this.loopCountRead = true;
            }
        }
    }

    /**
     * Skips data sub-blocks up to and including the block terminator.
     */
    private void skipSubBlocks() throws IOException {
        int length;
        while ((length = this.readByte()) != 0) {
            this.skip(length);
        }
    }

    /**
     * Runs every byte that gets consumed from now on through the CRC, until {@link #finishCrc()}.
     */
    private void startCrc() {
        this.crc.reset();
        this.crcActive = true;
        this.crcMark = this.bufferPosition;
    }

    private int finishCrc() {
        this.crc.update(this.buffer, this.crcMark, this.bufferPosition - this.crcMark);
        this.crcActive = false;
        return (int) this.crc.getValue();
    }

    private int readByte() throws IOException {
        if ((this.bufferPosition == this.bufferLimit) && !this.fillBuffer()) throw new EOFException();
        return this.buffer[this.bufferPosition++] & 0xFF;
    }

    /**
     * @return The next byte, or -1 if the source has no more bytes.
     */
    private int readByteOrEnd() throws IOException {
        if ((this.bufferPosition == this.bufferLimit) && !this.fillBuffer()) return -1;
        return this.buffer[this.bufferPosition++] & 0xFF;
    }

    private void readFully(@NotNull byte[] target, int offset, int length) throws IOException {
        int done = 0;
        while (done < length) {
            if ((this.bufferPosition == this.bufferLimit) && !this.fillBuffer()) throw new EOFException();
            int count = Math.min(length - done, this.bufferLimit - this.bufferPosition);
            System.arraycopy(this.buffer, this.bufferPosition, target, offset + done, count);
            this.bufferPosition += count;
            done += count;
        }
    }

    /**
     * Skips bytes by consuming them from the buffer. GIF images have a length byte every 255 bytes or less, so the source gets read completely anyway.
     */
    private void skip(int count) throws IOException {
        int remaining = count;
        while (remaining > 0) {
            if ((this.bufferPosition == this.bufferLimit) && !this.fillBuffer()) throw new EOFException();
            int step = Math.min(remaining, this.bufferLimit - this.bufferPosition);
            this.bufferPosition += step;
            remaining -= step;
        }
    }

    /**
     * Reads the next bytes of the source into the buffer, once all bytes in it got consumed. The consumed bytes run through the CRC first if it is active.
     *
     * @return False if the source has no more bytes.
     */
    private boolean fillBuffer() throws IOException {
        if (this.crcActive) {
            this.crc.update(this.buffer, this.crcMark, this.bufferLimit - this.crcMark);
            this.crcMark = 0;
        }
        this.bufferStart += this.bufferLimit;
        this.bufferPosition = 0;
        this.bufferLimit = 0;
        this.bufferView.clear();
        int read = this.source.read(this.bufferView, this.bufferStart);
        if (read <= 0) return false;
        this.bufferLimit = read;
        return true;
    }

    /**
     * The source position of the next byte that gets read.
     */
    private long position() {
        return this.bufferStart + this.bufferPosition;
    }

    /**
     * Converts the RGB entries of a color table to opaque ABGR pixels. All entries of the 256-entry palette after them become opaque black, because browsers show
     * indices outside the color table as black.
     */
    static void fillPalette(@NotNull byte[] rgb, int entries, @NotNull int[] palette) {
        for (int i = 0, source = 0; i < entries; i++, source += 3) {
            palette[i] = OPAQUE_BLACK | ((rgb[source + 2] & 0xFF) << 16) | ((rgb[source + 1] & 0xFF) << 8) | (rgb[source] & 0xFF);
        }
        Arrays.fill(palette, entries, palette.length, OPAQUE_BLACK);
    }

    /**
     * Reads an unsigned little-endian 16-bit value, the byte order of all GIF numbers.
     */
    private static int readShort(@NotNull byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

}
