package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedFrameDecoder;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageTestSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Writes GIF files for tests, with full control over all blocks, color tables, interlacing, sub-block sizes and the LZW stream (code sizes, clear codes, end codes),
 * reads them from memory and records the rows the frame decoder hands over.
 */
public final class GifTestFiles {

    public static final int NO_TRANSPARENCY = -1;
    /** A color table with the four colors red, green, blue and white, as RGB triples. **/
    public static final int[] PALETTE = {255, 0, 0, 0, 255, 0, 0, 0, 255, 255, 255, 255};
    /** The opaque ABGR pixels of the four {@link #PALETTE} entries, like the decoder produces them. **/
    public static final int[] PALETTE_PIXELS = expectedPixels(new int[] {0, 1, 2, 3}, PALETTE);

    private static final int[] PASS_START = {0, 4, 2, 1};
    private static final int[] PASS_STEP = {8, 8, 4, 2};

    private GifTestFiles() {
    }

    /**
     * Parses a file from memory.
     */
    @NotNull
    public static GifImage read(@NotNull byte[] file) throws IOException {
        return GifReader.read(new AnimatedImageTestSupport.MemorySource(file), "test.gif");
    }

    /**
     * Starts a GIF89a file without global color table.
     */
    @NotNull
    public static Gif gif(int screenWidth, int screenHeight) {
        return new Gif("GIF89a", screenWidth, screenHeight, null);
    }

    /**
     * Starts a GIF89a file with a global color table of RGB triples. Its size field gets the smallest table that fits all colors, missing entries are black.
     */
    @NotNull
    public static Gif gif(int screenWidth, int screenHeight, @NotNull int[] globalColorTable) {
        return new Gif("GIF89a", screenWidth, screenHeight, globalColorTable);
    }

    /**
     * Starts a file with any 6-byte signature and version, which also allows invalid ones.
     */
    @NotNull
    public static Gif gif(@NotNull String version, int screenWidth, int screenHeight, @Nullable int[] globalColorTable) {
        return new Gif(version, screenWidth, screenHeight, globalColorTable);
    }

    /**
     * Starts a GIF89a file with {@link #PALETTE} as global color table.
     */
    @NotNull
    public static Gif paletteGif(int screenWidth, int screenHeight) {
        return new Gif("GIF89a", screenWidth, screenHeight, PALETTE);
    }

    /**
     * A frame of the given size whose pixels all have the same color index.
     */
    @NotNull
    public static Frame solidFrame(int width, int height, int index) {
        int[] indices = new int[width * height];
        Arrays.fill(indices, index);
        return new Frame(width, height, indices);
    }

    /**
     * A frame of the given size whose pixels all have the same color index, with the minimum code size 3, which fits the {@link #PALETTE} indices and local color tables of up to 8 entries.
     */
    @NotNull
    public static Frame solidPaletteFrame(int width, int height, int index) {
        return solidFrame(width, height, index).minimumCodeSize(3);
    }

    /**
     * Random color indices below the given number of colors.
     */
    @NotNull
    public static int[] randomIndices(int count, int colors, @NotNull Random random) {
        int[] indices = new int[count];
        for (int i = 0; i < indices.length; i++) indices[i] = random.nextInt(colors);
        return indices;
    }

    /**
     * Indices in runs of random lengths, which give long LZW strings like flat graphics do.
     */
    @NotNull
    public static int[] runIndices(int count, int colors, int maxRun, @NotNull Random random) {
        int[] indices = new int[count];
        int i = 0;
        while (i < count) {
            int index = random.nextInt(colors);
            int run = 1 + random.nextInt(maxRun);
            for (int j = 0; (j < run) && (i < count); j++) indices[i++] = index;
        }
        return indices;
    }

    /**
     * Random RGB triples for a color table with the given number of entries.
     */
    @NotNull
    public static int[] randomColorTable(int entries, @NotNull Random random) {
        int[] table = new int[entries * 3];
        for (int i = 0; i < table.length; i++) table[i] = random.nextInt(256);
        return table;
    }

    /**
     * The opaque ABGR pixel of a color table entry, or opaque black for entries outside the table, like the decoder produces it.
     */
    public static int color(@Nullable int[] table, int index) {
        if ((table == null) || ((index * 3 + 2) >= table.length)) return 0xFF000000;
        return AnimatedImageTestSupport.abgr(table[index * 3], table[index * 3 + 1], table[index * 3 + 2], 255);
    }

    /**
     * The opaque ABGR pixels of the color table entries of all indices, like the decoder produces them.
     */
    @NotNull
    public static int[] expectedPixels(@NotNull int[] indices, @Nullable int[] table) {
        int[] pixels = new int[indices.length];
        for (int i = 0; i < indices.length; i++) pixels[i] = color(table, indices[i]);
        return pixels;
    }

    /**
     * Packs codes into an LZW stream by hand, for streams no encoder would write. Takes pairs of code and code size.
     */
    @NotNull
    public static byte[] packCodes(int... codesAndSizes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long buffer = 0L;
        int count = 0;
        for (int i = 0; i < codesAndSizes.length; i += 2) {
            buffer |= ((long) codesAndSizes[i]) << count;
            count += codesAndSizes[i + 1];
            while (count >= 8) {
                out.write((int) (buffer & 0xFF));
                buffer >>>= 8;
                count -= 8;
            }
        }
        if (count > 0) out.write((int) (buffer & 0xFF));
        return out.toByteArray();
    }

    /**
     * Decodes one frame of the image with a new frame decoder.
     */
    public static void decode(@NotNull GifImage image, int frameIndex, @NotNull AnimatedFrameDecoder.RowConsumer consumer) throws IOException {
        try (AnimatedFrameDecoder decoder = image.createFrameDecoder()) {
            decoder.decode(frameIndex, consumer);
        }
    }

    /**
     * The size field of the smallest color table that fits the given number of entries.
     */
    private static int tableSizeField(int entries) {
        int field = 0;
        while ((2 << field) < entries) field++;
        return field;
    }

    /**
     * Reorders rows from top-to-bottom order into the order of the four interlacing passes.
     */
    @NotNull
    private static int[] interlace(@NotNull int[] indices, int width, int height) {
        int[] result = new int[indices.length];
        int target = 0;
        for (int pass = 0; pass < 4; pass++) {
            for (int y = PASS_START[pass]; y < height; y += PASS_STEP[pass]) {
                System.arraycopy(indices, y * width, result, target, width);
                target += width;
            }
        }
        return result;
    }

    /**
     * Splits data into sub-blocks with the given sizes (used in a cycle) and adds the block terminator.
     */
    @NotNull
    private static byte[] subBlocks(@NotNull byte[] data, @NotNull int[] sizes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        int block = 0;
        while (offset < data.length) {
            int size = Math.min(sizes[block++ % sizes.length], data.length - offset);
            out.write(size);
            out.write(data, offset, size);
            offset += size;
        }
        out.write(0);
        return out.toByteArray();
    }

    /**
     * A GIF file under construction. Blocks get written in the order the methods get called.
     */
    public static final class Gif {

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        private Gif(@NotNull String version, int screenWidth, int screenHeight, @Nullable int[] globalColorTable) {
            this.out.writeBytes(version.getBytes(StandardCharsets.US_ASCII));
            this.short16(screenWidth).short16(screenHeight);
            if (globalColorTable == null) {
                this.out.write(0);
            } else {
                int field = tableSizeField(globalColorTable.length / 3);
                this.out.write(0x80 | 0x70 | field);
            }
            this.out.write(0);
            this.out.write(0);
            if (globalColorTable != null) this.colorTable(globalColorTable, tableSizeField(globalColorTable.length / 3));
        }

        @NotNull
        public Gif graphicControl(int disposal, int delayCentiseconds, int transparentIndex) {
            int flags = (disposal << 2) | ((transparentIndex != NO_TRANSPARENCY) ? 1 : 0);
            byte[] data = {(byte) flags, (byte) delayCentiseconds, (byte) (delayCentiseconds >>> 8), (byte) ((transparentIndex != NO_TRANSPARENCY) ? transparentIndex : 0)};
            return this.extension(0xF9, data);
        }

        /**
         * Writes an extension with the given sub-blocks and the block terminator.
         */
        @NotNull
        public Gif extension(int label, @NotNull byte[]... subBlocks) {
            this.out.write(0x21);
            this.out.write(label);
            for (byte[] block : subBlocks) {
                this.out.write(block.length);
                this.out.writeBytes(block);
            }
            this.out.write(0);
            return this;
        }

        @NotNull
        public Gif application(@NotNull String identifier, @NotNull byte[]... subBlocks) {
            byte[][] blocks = new byte[subBlocks.length + 1][];
            blocks[0] = identifier.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(subBlocks, 0, blocks, 1, subBlocks.length);
            return this.extension(0xFF, blocks);
        }

        @NotNull
        public Gif netscapeLoop(int loopCount) {
            return this.application("NETSCAPE2.0", new byte[] {1, (byte) loopCount, (byte) (loopCount >>> 8)});
        }

        @NotNull
        public Gif animextsLoop(int loopCount) {
            return this.application("ANIMEXTS1.0", new byte[] {1, (byte) loopCount, (byte) (loopCount >>> 8)});
        }

        @NotNull
        public Gif comment(@NotNull String text) {
            return this.extension(0xFE, text.getBytes(StandardCharsets.US_ASCII));
        }

        /**
         * A plain text extension with a grid over the whole screen. Browsers don't render plain text, and it must not take the graphic control extension of the next image.
         */
        @NotNull
        public Gif plainText(@NotNull String text) {
            byte[] header = {0, 0, 0, 0, 100, 0, 100, 0, 8, 8, 1, 0};
            return this.extension(0x01, header, text.getBytes(StandardCharsets.US_ASCII));
        }

        /**
         * Writes an image descriptor, its local color table and its data.
         */
        @NotNull
        public Gif image(@NotNull Frame frame) {
            this.out.write(0x2C);
            this.short16(frame.x).short16(frame.y).short16(frame.width).short16(frame.height);
            int flags = frame.interlaced ? 0x40 : 0;
            if (frame.localColorTable == null) {
                this.out.write(flags);
            } else {
                int sizeField = tableSizeField(frame.localColorTable.length / 3);
                this.out.write(flags | 0x80 | sizeField);
                this.colorTable(frame.localColorTable, sizeField);
            }
            this.out.write((frame.minimumCodeSizeByte >= 0) ? frame.minimumCodeSizeByte : frame.minimumCodeSize);
            this.out.writeBytes(subBlocks(frame.lzwData(), frame.subBlockSizes));
            return this;
        }

        @NotNull
        public Gif raw(int... bytes) {
            for (int value : bytes) this.out.write(value);
            return this;
        }

        @NotNull
        public Gif raw(@NotNull byte[] bytes) {
            this.out.writeBytes(bytes);
            return this;
        }

        /**
         * How many bytes the file has so far, which is the position of the next block.
         */
        public int size() {
            return this.out.size();
        }

        /**
         * The file with its trailer.
         */
        @NotNull
        public byte[] end() {
            this.out.write(0x3B);
            return this.out.toByteArray();
        }

        /**
         * The file so far, without trailer.
         */
        @NotNull
        public byte[] bytes() {
            return this.out.toByteArray();
        }

        private void colorTable(@NotNull int[] rgb, int sizeField) {
            int entries = 2 << sizeField;
            for (int i = 0; i < entries * 3; i++) {
                this.out.write((i < rgb.length) ? rgb[i] : 0);
            }
        }

        @NotNull
        private Gif short16(int value) {
            this.out.write(value & 0xFF);
            this.out.write((value >>> 8) & 0xFF);
            return this;
        }

    }

    /**
     * An image block: its descriptor, its local color table and how its indices get encoded. Indices are always given in top-to-bottom row order.
     */
    public static final class Frame {

        public final int width;
        public final int height;
        @NotNull
        public final int[] indices;
        public int x;
        public int y;
        public boolean interlaced;
        @Nullable
        public int[] localColorTable;
        public int minimumCodeSize = 8;
        /** Written instead of the minimum code size, for invalid values, or -1. **/
        public int minimumCodeSizeByte = -1;
        @NotNull
        public Lzw lzw = new Lzw();
        @NotNull
        public int[] subBlockSizes = {255};
        /** LZW stream that gets written instead of encoding the indices, or NULL. **/
        @Nullable
        public byte[] rawLzw;

        public Frame(int width, int height, @NotNull int[] indices) {
            this.width = width;
            this.height = height;
            this.indices = indices;
        }

        @NotNull
        public Frame at(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        @NotNull
        public Frame interlaced() {
            this.interlaced = true;
            return this;
        }

        @NotNull
        public Frame localColorTable(@NotNull int[] rgb) {
            this.localColorTable = rgb;
            return this;
        }

        @NotNull
        public Frame minimumCodeSize(int minimumCodeSize) {
            this.minimumCodeSize = minimumCodeSize;
            return this;
        }

        @NotNull
        public Frame lzw(@NotNull Lzw lzw) {
            this.lzw = lzw;
            return this;
        }

        @NotNull
        public Frame subBlockSizes(int... sizes) {
            this.subBlockSizes = sizes;
            return this;
        }

        @NotNull
        public Frame rawLzw(@NotNull byte[] data) {
            this.rawLzw = data;
            return this;
        }

        /**
         * The LZW stream of the frame, in file order (interlaced rows already reordered).
         */
        @NotNull
        private byte[] lzwData() {
            if (this.rawLzw != null) return this.rawLzw;
            int[] ordered = this.interlaced ? interlace(this.indices, this.width, this.height) : this.indices;
            return this.lzw.encode(ordered, this.minimumCodeSize);
        }

    }

    /**
     * An LZW encoder that produces the GIF variant (codes packed from the lowest bit, code size growth right when the decoder's table needs one more bit),
     * with control over where clear codes go, what happens when the table is full, and how the stream ends.
     */
    public static final class Lzw {

        public boolean clearAtStart = true;
        /** Sends a clear code after this many codes since the last clear code, or never if 0. **/
        public int clearInterval;
        /** How many codes get sent while the table is full before a clear code, -1 sends the clear code right away, Integer.MAX_VALUE never. **/
        public int codesWithFullTable = -1;
        public boolean endCode = true;
        /** Stops after this many codes, which leaves the rest of the pixels out. **/
        public int maxCodes = Integer.MAX_VALUE;
        /** Sends a code that is not in the table yet after this many codes and stops, or never if -1. **/
        public int invalidCodeAfter = -1;
        /** Codes that get sent after the end code. **/
        @NotNull
        public int[] codesAfterEnd = new int[0];
        /** Sends only single-color codes, so the table never gets used, but it still fills up. **/
        public boolean literalsOnly;
        /** How many pixels the last stream holds. **/
        public int encodedPixels;
        /** How many times the last stream used a code that its own code defines (KwKwK). **/
        public int selfReferencingCodes;
        /** How many times the decoder table got full in the last stream. **/
        public int tableFills;
        /** How many codes the last stream sent while the decoder table was full. **/
        public int fullTableCodes;

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private long bitBuffer;
        private int bitCount;
        private int codeSize;
        private int minimumCodeSize;
        private int decoderNextCode;
        private boolean decoderHasPrevious;
        private int codes;

        @NotNull
        public Lzw clearInterval(int codes) {
            this.clearInterval = codes;
            return this;
        }

        @NotNull
        public Lzw codesWithFullTable(int codes) {
            this.codesWithFullTable = codes;
            return this;
        }

        @NotNull
        public Lzw noEndCode() {
            this.endCode = false;
            return this;
        }

        @NotNull
        public Lzw maxCodes(int codes) {
            this.maxCodes = codes;
            return this;
        }

        @NotNull
        public Lzw invalidCodeAfter(int codes) {
            this.invalidCodeAfter = codes;
            return this;
        }

        @NotNull
        public Lzw codesAfterEnd(int... codes) {
            this.codesAfterEnd = codes;
            return this;
        }

        @NotNull
        public Lzw literalsOnly() {
            this.literalsOnly = true;
            return this;
        }

        @NotNull
        public byte[] encode(@NotNull int[] indices, int minimumCodeSize) {
            this.out.reset();
            this.bitBuffer = 0L;
            this.bitCount = 0;
            this.minimumCodeSize = minimumCodeSize;
            this.encodedPixels = 0;
            this.selfReferencingCodes = 0;
            this.tableFills = 0;
            this.fullTableCodes = 0;
            this.codes = 0;
            int clearCode = 1 << minimumCodeSize;
            this.resetCodeSize();
            Map<Integer, Integer> table = new HashMap<>();
            int nextCode = clearCode + 2;
            if (this.clearAtStart) this.writeClear();
            int prefix = -1;
            int prefixPixels = 0;
            int codesSinceClear = 0;
            int codesWhileFull = 0;
            for (int i = 0; i <= indices.length; i++) {
                if (i < indices.length) {
                    int index = indices[i];
                    if (index >= clearCode) throw new IllegalArgumentException("Index " + index + " does not fit code size " + minimumCodeSize);
                    if (prefix < 0) {
                        prefix = index;
                        prefixPixels = 1;
                        continue;
                    }
                    Integer known = this.literalsOnly ? null : table.get((prefix << 8) | index);
                    if (known != null) {
                        prefix = known;
                        prefixPixels++;
                        continue;
                    }
                }
                if (prefix < 0) break;
                if (this.codes == this.invalidCodeAfter) {
                    this.write(Math.min(this.decoderNextCode + 1, 4095));
                    this.flush();
                    return this.out.toByteArray();
                }
                if (this.codes >= this.maxCodes) break;
                if (this.decoderHasPrevious && (prefix == this.decoderNextCode)) this.selfReferencingCodes++;
                if (this.decoderNextCode >= 4096) this.fullTableCodes++;
                this.writeCode(prefix);
                this.encodedPixels += prefixPixels;
                codesSinceClear++;
                if (i == indices.length) break;
                boolean clear = false;
                if (nextCode < 4096) {
                    table.put((prefix << 8) | indices[i], nextCode++);
                } else if ((this.codesWithFullTable < 0) || (codesWhileFull++ >= this.codesWithFullTable)) {
                    clear = true;
                }
                if ((this.clearInterval > 0) && (codesSinceClear >= this.clearInterval)) clear = true;
                if (clear) {
                    this.writeClear();
                    table.clear();
                    nextCode = clearCode + 2;
                    codesSinceClear = 0;
                    codesWhileFull = 0;
                }
                prefix = indices[i];
                prefixPixels = 1;
            }
            if (this.endCode) {
                this.write(clearCode + 1);
                for (int code : this.codesAfterEnd) this.write(code);
            }
            this.flush();
            return this.out.toByteArray();
        }

        private void resetCodeSize() {
            this.codeSize = this.minimumCodeSize + 1;
            this.decoderNextCode = (1 << this.minimumCodeSize) + 2;
            this.decoderHasPrevious = false;
        }

        private void writeClear() {
            this.write(1 << this.minimumCodeSize);
            this.resetCodeSize();
        }

        /**
         * Writes a string code and mirrors what the decoder does after reading it, so the code size always matches the decoder.
         */
        private void writeCode(int code) {
            this.write(code);
            this.codes++;
            if (this.decoderHasPrevious && (this.decoderNextCode < 4096)) {
                this.decoderNextCode++;
                if (this.decoderNextCode == 4096) this.tableFills++;
            }
            // Decoders check the code size after every code, so with the minimum code size 1 it already grows after the first code
            if ((this.decoderNextCode >= (1 << this.codeSize)) && (this.codeSize < 12)) this.codeSize++;
            this.decoderHasPrevious = true;
        }

        private void write(int code) {
            this.bitBuffer |= ((long) code) << this.bitCount;
            this.bitCount += this.codeSize;
            while (this.bitCount >= 8) {
                this.out.write((int) (this.bitBuffer & 0xFF));
                this.bitBuffer >>>= 8;
                this.bitCount -= 8;
            }
        }

        private void flush() {
            if (this.bitCount > 0) this.out.write((int) (this.bitBuffer & 0xFF));
            this.bitBuffer = 0L;
            this.bitCount = 0;
        }

    }

    /**
     * Keeps every handed over row in a frame-sized pixel array and remembers the order and length of all rows.
     * Checks that every row starts at the first column of the frame and has no gaps, which is how the GIF frame decoder hands over rows.
     */
    public static final class RecordingConsumer implements AnimatedFrameDecoder.RowConsumer {

        private final int width;
        @NotNull
        public final int[] pixels;
        /** How many pixels every handed over row has, in the order of {@link #rows()}. **/
        @NotNull
        public final List<Integer> counts = new ArrayList<>();
        private final List<Integer> rowList = new ArrayList<>();

        public RecordingConsumer(int width, int height) {
            this.width = width;
            this.pixels = new int[width * height];
        }

        @Override
        public void acceptRow(int y, int x, int step, @NotNull int[] rowPixels, int count) {
            assertEquals(0, x);
            assertEquals(1, step);
            this.rowList.add(y);
            this.counts.add(count);
            System.arraycopy(rowPixels, 0, this.pixels, y * this.width, count);
        }

        /**
         * The frame rows in the order they got handed over.
         */
        @NotNull
        public int[] rows() {
            return this.rowList.stream().mapToInt(Integer::intValue).toArray();
        }

    }

}
