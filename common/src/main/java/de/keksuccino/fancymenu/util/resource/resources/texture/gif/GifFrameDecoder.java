package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * Decodes the LZW data of GIF frames row by row: reads it from the source of the image, decodes the color indices, walks the four passes of interlaced frames
 * and converts the indices through the color table to 8-bit ABGR pixels (the pixel layout of RGBA textures in memory).<br>
 * Every decoded row goes straight to a {@link RowConsumer}, so decoding needs no memory for whole frames: the LZW tables, one row of indices (plus room for the longest LZW string),
 * one row of pixels and a small read buffer.<br><br>
 *
 * Broken LZW data gets decoded as far as it goes, like browsers do it: invalid codes, an early end code or data that ends before the last pixel keep all pixels decoded so far,
 * the decoded part of a partial row included, and leave the rest of the frame untouched. Data after the last pixel gets ignored.
 * Columns right of the canvas get decoded (LZW data can't get skipped), but never handed over, and decoding stops right after the last visible row,
 * so rows below the canvas of frames that are not interlaced don't even get decoded. The last visible rows of interlaced frames come in their last pass,
 * so they get decoded almost completely, which the frame size limit of {@link GifReader} keeps in bounds.<br>
 * Not thread-safe, every decoding thread needs its own instance.
 */
final class GifFrameDecoder implements AnimatedFrameDecoder {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_CODE_SIZE = 12;
    private static final int TABLE_SIZE = 1 << MAX_CODE_SIZE;
    /** Strings get written in 8-byte stores, so the store of the last chunk of a string can write up to 7 bytes after its end, which this margin leaves room for. **/
    private static final int STORE_OVERRUN = 8;
    private static final int[] PASS_START = {0, 4, 2, 1};
    private static final int[] PASS_STEP = {8, 8, 4, 2};
    private static final VarHandle LONG_LITTLE_ENDIAN = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    @NotNull
    private final GifImage image;
    @NotNull
    private final GifFrameDataInput input;
    /*
     * The string table. Strings get split into chunks of 8 bytes, counted from their start, so the last chunk of a string has 1 to 8 bytes.
     * Every code knows the bytes of its last chunk (packed little-endian into a long) and the code whose string is everything before that chunk.
     * Writing a string then takes one 8-byte store per chunk while walking back through the prefixes, instead of one store and one dependent load per byte,
     * and strings of up to 8 bytes (most strings of photos) need no walk at all.
     * A new string is always an existing string plus one byte, so it either fills up the last chunk of that string (same prefix) or starts a new chunk (that string becomes its prefix).
     */
    private final long[] suffixes = new long[TABLE_SIZE];
    /** Only meaningful for strings longer than 8 bytes. A prefix is always a smaller code that got added after the same clear code, so it never points to an outdated entry. **/
    private final short[] prefixes = new short[TABLE_SIZE];
    private final short[] lengths = new short[TABLE_SIZE];
    /** The color table of the frame that gets decoded right now as ABGR pixels, with its transparent index applied. **/
    private final int[] palette = new int[256];
    /**
     * Color indices of the current row. Strings get written straight to their place, so a string that crosses the end of the row continues after it,
     * and only the part after the last complete row gets moved back to the start. That part is never longer than the string, so narrow images with long strings stay linear.
     */
    @NotNull
    private final byte[] indices;
    @NotNull
    private final int[] rowPixels;

    private int visibleWidth;
    private int visibleHeight;
    private int remainingVisibleRows;
    private int imageHeight;
    private boolean interlaced;
    private int pass;
    /** The image row the next complete row of indices belongs to. **/
    private int row;

    GifFrameDecoder(@NotNull GifImage image) {
        this.image = image;
        this.input = new GifFrameDataInput(image);
        int maxImageWidth = 1;
        int maxVisibleWidth = 1;
        for (int i = 0; i < image.getFrameCount(); i++) {
            GifFrame frame = image.getFrame(i);
            if (!frame.hasVisiblePixels()) continue;
            maxImageWidth = Math.max(maxImageWidth, frame.imageWidth());
            maxVisibleWidth = Math.max(maxVisibleWidth, frame.width());
        }
        // A row has less than one row width of indices before a string gets written, and no string is longer than the table
        this.indices = new byte[maxImageWidth + TABLE_SIZE + STORE_OVERRUN];
        this.rowPixels = new int[maxVisibleWidth];
    }

    @Override
    public void decode(int frameIndex, @NotNull RowConsumer consumer) throws IOException {
        GifFrame frame = this.image.getFrame(frameIndex);
        // Frames outside the canvas or without pixels show nothing, so their data doesn't need to get read
        if (!frame.hasVisiblePixels()) return;
        int[] colors = this.palette;
        if (frame.localColorTableSize() == 0) System.arraycopy(this.image.getGlobalPalette(), 0, colors, 0, colors.length);
        this.input.start(frame, colors);
        if (frame.transparentIndex() != GifFrame.NO_TRANSPARENT_INDEX) colors[frame.transparentIndex()] = 0;
        this.visibleWidth = frame.width();
        this.visibleHeight = frame.height();
        this.remainingVisibleRows = frame.height();
        this.imageHeight = frame.imageHeight();
        this.interlaced = frame.interlaced();
        this.pass = 0;
        this.row = 0;
        boolean complete = this.decodeData(frame.lzwMinimumCodeSize(), frame.imageWidth(), consumer);
        // Checks the data before reporting it as broken, because changed bytes look like broken data
        this.input.finish(frame.dataCrc());
        if (!complete && this.image.markBrokenFrameDataReported()) {
            LOGGER.warn("[FANCYMENU] GIF image has broken frame data, so parts of frame {} don't get drawn: {}", frameIndex, this.image.getSourceName());
        }
    }

    /**
     * Decodes the LZW data until all visible rows got handed over. Everything that runs per code stays in local variables of this loop, because it is the hot path.
     *
     * @return False if the data is broken or ended before all visible rows were complete.
     */
    private boolean decodeData(int minimumCodeSize, int width, @NotNull RowConsumer consumer) throws IOException {
        final long[] suffixes = this.suffixes;
        final short[] prefixes = this.prefixes;
        final short[] lengths = this.lengths;
        final byte[] indices = this.indices;
        final byte[] data = this.input.buffer();
        final int clearCode = 1 << minimumCodeSize;
        // Earlier frames with a smaller code size used these entries for strings
        for (int i = 0; i < clearCode; i++) {
            suffixes[i] = i;
            lengths[i] = 1;
        }
        int position = 0;
        int limit = 0;
        // Codes are packed starting at the lowest bit. Bits above bitCount can hold bits of the next byte already, which is fine because they get OR-ed with the same bits again
        long bits = 0L;
        int bitCount = 0;
        int codeSize = minimumCodeSize + 1;
        int codeMask = (1 << codeSize) - 1;
        int nextCode = clearCode + 2;
        // The previous code and what adding a string after it needs, kept here so adding a string reads nothing from the table
        int previous = -1;
        int previousFirst = 0;
        int previousLength = 0;
        long previousLastChunk = 0L;
        int previousPrefix = 0;
        int rowFill = 0;
        while (true) {
            if (bitCount < codeSize) {
                if ((limit - position) >= 8) {
                    bits |= ((long) LONG_LITTLE_ENDIAN.get(data, position)) << bitCount;
                    int taken = (63 - bitCount) >>> 3;
                    position += taken;
                    bitCount += taken << 3;
                } else {
                    do {
                        if (position == limit) {
                            limit = this.input.nextData();
                            position = 0;
                            if (limit == 0) {
                                this.emitPartialRow(rowFill, consumer);
                                return false;
                            }
                        }
                        bits |= (data[position++] & 0xFFL) << bitCount;
                        bitCount += 8;
                    } while (bitCount < codeSize);
                }
            }
            int code = (int) bits & codeMask;
            bits >>>= codeSize;
            bitCount -= codeSize;
            // True for the clear code and the end code only, in a single check
            if (((code - clearCode) & ~1) == 0) {
                if (code == clearCode) {
                    codeSize = minimumCodeSize + 1;
                    codeMask = (1 << codeSize) - 1;
                    nextCode = clearCode + 2;
                    previous = -1;
                    continue;
                }
                // The end code came before the last pixel
                this.emitPartialRow(rowFill, consumer);
                return false;
            }
            if (code >= nextCode) {
                if ((code > nextCode) || (previous < 0)) {
                    // Codes that are not in the table yet, and codes after a clear code that are no single colors, are invalid
                    this.emitPartialRow(rowFill, consumer);
                    return false;
                }
                // The KwKwK case: the code is the one that this very code defines, the previous string plus its own first byte, which is the first byte of the previous string.
                // It gets added here already so it can get written, and adding it again below writes the exact same entry
                addString(suffixes, prefixes, lengths, nextCode, previous, previousLength, previousLastChunk, previousPrefix, previousFirst);
            }
            // Writes the string backwards, one 8-byte chunk after the other, straight to its place in the row
            int length = lengths[code];
            int start = rowFill;
            int chunkPosition = start + ((length - 1) & ~7);
            long lastChunk = suffixes[code];
            long chunk = lastChunk;
            LONG_LITTLE_ENDIAN.set(indices, chunkPosition, chunk);
            // Only strings longer than 8 bytes have a prefix, the value stays unused for all others
            int codePrefix = 0;
            if (chunkPosition > start) {
                codePrefix = prefixes[code];
                int prefix = codePrefix;
                do {
                    chunkPosition -= 8;
                    chunk = suffixes[prefix];
                    LONG_LITTLE_ENDIAN.set(indices, chunkPosition, chunk);
                    prefix = prefixes[prefix];
                } while (chunkPosition > start);
            }
            int first = (int) chunk & 0xFF;
            rowFill = start + length;
            // When the table is full, codes keep their 12 bits and no strings get added until the next clear code, which some encoders send much later
            if ((previous >= 0) && (nextCode < TABLE_SIZE)) {
                addString(suffixes, prefixes, lengths, nextCode, previous, previousLength, previousLastChunk, previousPrefix, first);
                nextCode++;
            }
            // The code size grows once the next free code doesn't fit anymore, checked after every code like wuffs (Chromium) and ffmpeg do it, not only after adding a string.
            // That only makes a difference for the minimum code size 1, whose first free code needs one more bit right after the first code
            if ((nextCode > codeMask) && (codeSize < MAX_CODE_SIZE)) {
                codeSize++;
                codeMask = (codeMask << 1) | 1;
            }
            previous = code;
            previousFirst = first;
            previousLength = length;
            previousLastChunk = lastChunk;
            previousPrefix = codePrefix;
            if (rowFill >= width) {
                int offset = 0;
                do {
                    if (this.emitRow(offset, width, consumer)) return true;
                    offset += width;
                } while ((rowFill - offset) >= width);
                rowFill -= offset;
                if (rowFill > 0) System.arraycopy(indices, offset, indices, 0, rowFill);
            }
        }
    }

    /**
     * Adds the string of the previous code plus one byte to the table.
     *
     * @param previousLastChunk The last chunk of the previous string.
     * @param previousPrefix The prefix of the previous string, only meaningful if it is longer than 8 bytes.
     */
    private static void addString(@NotNull long[] suffixes, @NotNull short[] prefixes, @NotNull short[] lengths, int code, int previous, int previousLength, long previousLastChunk, int previousPrefix, int nextByte) {
        // If the last chunk of the previous string is full, the byte starts a new chunk and the previous string becomes the prefix.
        // Written as selects instead of branches, because whether a chunk is full is random for photos
        int lastChunkLength = previousLength & 7;
        boolean newChunk = lastChunkLength == 0;
        prefixes[code] = (short) (newChunk ? previous : previousPrefix);
        suffixes[code] = (newChunk ? 0L : previousLastChunk) | (((long) nextByte) << (lastChunkLength << 3));
        lengths[code] = (short) (previousLength + 1);
    }

    /**
     * Hands over the row of indices that starts at the offset, if it is visible, and moves on to the next row.
     *
     * @return True if all visible rows got handed over, so decoding can stop.
     */
    private boolean emitRow(int offset, int count, @NotNull RowConsumer consumer) {
        if (this.row < this.visibleHeight) {
            this.convertRow(offset, count, consumer);
            if (--this.remainingVisibleRows == 0) return true;
        }
        if (!this.interlaced) {
            this.row++;
        } else {
            this.row += PASS_STEP[this.pass];
            while ((this.row >= this.imageHeight) && (this.pass < PASS_START.length - 1)) {
                this.pass++;
                this.row = PASS_START[this.pass];
            }
        }
        return false;
    }

    /**
     * Hands over the decoded part of the row that broken data cut off. The rest of the row keeps what the canvas shows.
     */
    private void emitPartialRow(int count, @NotNull RowConsumer consumer) {
        if ((count > 0) && (this.row < this.visibleHeight)) this.convertRow(0, count, consumer);
    }

    private void convertRow(int offset, int count, @NotNull RowConsumer consumer) {
        int visible = Math.min(count, this.visibleWidth);
        byte[] source = this.indices;
        int[] colors = this.palette;
        int[] out = this.rowPixels;
        for (int i = 0; i < visible; i++) {
            out[i] = colors[source[offset + i] & 0xFF];
        }
        consumer.acceptRow(this.row, 0, 1, out, visible);
    }

    /**
     * Holds no resources besides memory.
     */
    @Override
    public void close() {
    }

}
