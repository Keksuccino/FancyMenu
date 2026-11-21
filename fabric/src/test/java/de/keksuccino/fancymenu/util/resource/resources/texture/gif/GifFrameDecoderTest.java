package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedFrameDecoder;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageTestSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GifFrameDecoderTest {

    private static final int[] SUB_BLOCK_SIZES = {1, 2, 7, 100, 254, 255};

    static LongStream seeds() {
        return LongStream.range(0, 250);
    }

    /**
     * Random images with every code size, color table kind, interlacing, sub-block sizes and clear code timing. ImageIO proves that the test files hold exactly the
     * written indices, so the decoder gets compared with an independent decoder. The JDK decoder can't read every valid file, so it only checks the files it can read:
     * it misreads the minimum code size 1, sub-blocks of 1 or 2 bytes and interlaced images that are 2 to 4 rows high.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void decodesRandomImagesLikeImageIO(long seed) throws IOException {
        Random random = new Random(seed);
        int minimumCodeSize = 1 + random.nextInt(8);
        int colors = 1 << minimumCodeSize;
        boolean big = random.nextInt(4) == 0;
        int width = 1 + random.nextInt(big ? 200 : (random.nextBoolean() ? 8 : 70));
        int height = 1 + random.nextInt(big ? 200 : (random.nextBoolean() ? 8 : 70));
        int[] indices = random.nextBoolean() ? GifTestFiles.randomIndices(width * height, colors, random) : GifTestFiles.runIndices(width * height, colors, 1 + random.nextInt(300), random);
        int[] table = GifTestFiles.randomColorTable(Math.max(2, colors), random);
        boolean local = random.nextBoolean();
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw();
        lzw.clearAtStart = random.nextInt(4) != 0;
        lzw.endCode = random.nextInt(4) != 0;
        if (random.nextInt(3) == 0) lzw.clearInterval = 1 + random.nextInt(600);
        if (random.nextInt(3) == 0) lzw.codesWithFullTable = (random.nextInt(4) == 0) ? Integer.MAX_VALUE : random.nextInt(400);
        int[] subBlockSizes = new int[1 + random.nextInt(4)];
        for (int i = 0; i < subBlockSizes.length; i++) subBlockSizes[i] = random.nextBoolean() ? SUB_BLOCK_SIZES[random.nextInt(SUB_BLOCK_SIZES.length)] : 1 + random.nextInt(255);
        GifTestFiles.Frame frame = new GifTestFiles.Frame(width, height, indices).minimumCodeSize(minimumCodeSize).lzw(lzw).subBlockSizes(subBlockSizes);
        if (random.nextInt(3) == 0) frame.interlaced();
        if (local) frame.localColorTable(table);
        byte[] file = (local ? GifTestFiles.gif(width, height) : GifTestFiles.gif(width, height, table)).image(frame).end();

        if (readableByImageIO(minimumCodeSize, subBlockSizes, frame.interlaced, height)) assertArrayEquals(indices, readIndicesWithImageIO(file)[0]);
        assertArrayEquals(GifTestFiles.expectedPixels(indices, table), decodeFrame(file, 0));
    }

    /**
     * Files of the JDK encoder, which has its own LZW encoder, code size choice and interlacing.
     */
    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31})
    void decodesFilesOfTheJdkEncoder(long seed) throws IOException {
        Random random = new Random(seed);
        int bits = new int[] {1, 2, 4, 8}[random.nextInt(4)];
        int colors = 1 << bits;
        int width = 1 + random.nextInt(150);
        int height = 1 + random.nextInt(150);
        int[] table = GifTestFiles.randomColorTable(colors, random);
        int[] indices = random.nextBoolean() ? GifTestFiles.randomIndices(width * height, colors, random) : GifTestFiles.runIndices(width * height, colors, 50, random);
        byte[] file = writeWithImageIO(width, height, bits, table, indices, random.nextBoolean());

        assertArrayEquals(GifTestFiles.expectedPixels(indices, table), decodeFrame(file, 0));
    }

    @Test
    void decodesInterlacedFramesOfEveryHeight() throws IOException {
        for (int height = 1; height <= 33; height++) {
            Random random = new Random(height);
            int[] table = GifTestFiles.randomColorTable(16, random);
            int[] indices = GifTestFiles.randomIndices(3 * height, 16, random);
            byte[] file = GifTestFiles.gif(3, height, table).image(new GifTestFiles.Frame(3, height, indices).minimumCodeSize(4).interlaced()).end();

            if (readableByImageIO(4, new int[] {255}, true, height)) assertArrayEquals(indices, readIndicesWithImageIO(file)[0], "height " + height);
            assertArrayEquals(GifTestFiles.expectedPixels(indices, table), decodeFrame(file, 0), "height " + height);
        }
    }

    @Test
    void interlacedRowsComeInTheOrderOfTheFourPasses() throws IOException {
        int[] indices = GifTestFiles.randomIndices(2 * 11, 4, new Random(4));
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 11).image(new GifTestFiles.Frame(2, 11, indices).minimumCodeSize(2).interlaced()).end());
        GifTestFiles.RecordingConsumer consumer = new GifTestFiles.RecordingConsumer(2, 11);

        GifTestFiles.decode(image, 0, consumer);

        assertArrayEquals(new int[] {0, 8, 4, 2, 6, 10, 1, 3, 5, 7, 9}, consumer.rows());
        assertArrayEquals(GifTestFiles.expectedPixels(indices, GifTestFiles.PALETTE), consumer.pixels);
    }

    @Test
    void selfReferencingCodesGetDecoded() throws IOException {
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw();
        GifTestFiles.Frame frame = GifTestFiles.solidFrame(64, 64, 3).minimumCodeSize(2).lzw(lzw);
        byte[] file = GifTestFiles.paletteGif(64, 64).image(frame).end();

        // The 4096 pixels are strings of 1 to 90 pixels and a last single pixel, and every string after the first one is the code that it defines itself
        assertEquals(89, lzw.selfReferencingCodes);
        assertArrayEquals(GifTestFiles.expectedPixels(frame.indices, GifTestFiles.PALETTE), decodeFrame(file, 0));
    }

    /**
     * The random data fills the table 6 times, and every time the given number of codes follows before the clear code, only the end of the data can cut the last ones short
     * (1792 = 5 * 300 + 292). Integer.MAX_VALUE never sends a clear code, so the table gets full once and all codes after that come with a full table.
     */
    @ParameterizedTest
    @CsvSource({"0, 6, 0", "1, 6, 6", "300, 6, 1792", "2147483647, 1, 20513"})
    void fullTableKeepsTwelveBitCodesUntilTheNextClearCode(int codesWithFullTable, int tableFills, int fullTableCodes) throws IOException {
        Random random = new Random(codesWithFullTable);
        int[] table = GifTestFiles.randomColorTable(256, random);
        int[] indices = GifTestFiles.randomIndices(160 * 160, 256, random);
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().codesWithFullTable(codesWithFullTable);
        byte[] file = GifTestFiles.gif(160, 160, table).image(new GifTestFiles.Frame(160, 160, indices).lzw(lzw)).end();

        assertEquals(tableFills, lzw.tableFills);
        assertEquals(fullTableCodes, lzw.fullTableCodes);
        assertArrayEquals(indices, readIndicesWithImageIO(file)[0]);
        assertArrayEquals(GifTestFiles.expectedPixels(indices, table), decodeFrame(file, 0));
    }

    @Test
    void codeSizeGrowsToTwelveBitsWithSingleColorCodes() throws IOException {
        Random random = new Random(7);
        int[] indices = GifTestFiles.randomIndices(100 * 100, 4, random);
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().literalsOnly().codesWithFullTable(2000);
        byte[] file = GifTestFiles.paletteGif(100, 100).image(new GifTestFiles.Frame(100, 100, indices).minimumCodeSize(2).lzw(lzw)).end();

        // Every pixel is a code of its own, so the table gets full once, after 4091 of the 10000 codes, and the next 2000 codes come with a full table
        assertEquals(1, lzw.tableFills);
        assertEquals(2000, lzw.fullTableCodes);
        assertArrayEquals(indices, readIndicesWithImageIO(file)[0]);
        assertArrayEquals(GifTestFiles.expectedPixels(indices, GifTestFiles.PALETTE), decodeFrame(file, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 7, 254, 255, 256, 511})
    void clearCodesCanComeAnywhere(int clearInterval) throws IOException {
        Random random = new Random(clearInterval);
        int[] table = GifTestFiles.randomColorTable(8, random);
        int[] indices = GifTestFiles.runIndices(90 * 90, 8, 20, random);
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().clearInterval(clearInterval);
        byte[] file = GifTestFiles.gif(90, 90, table).image(new GifTestFiles.Frame(90, 90, indices).minimumCodeSize(3).lzw(lzw)).end();

        assertArrayEquals(indices, readIndicesWithImageIO(file)[0]);
        assertArrayEquals(GifTestFiles.expectedPixels(indices, table), decodeFrame(file, 0));
    }

    @Test
    void handlesConsecutiveClearCodesAndDataWithoutClearCodes() throws IOException {
        // Clear, clear, 1, clear, clear, 2, 1, end; all codes have 9 bits, because no table entry ever fills the code size
        byte[] lzw = GifTestFiles.packCodes(256, 9, 256, 9, 1, 9, 256, 9, 256, 9, 2, 9, 1, 9, 257, 9);
        byte[] withoutClear = GifTestFiles.packCodes(3, 9, 2, 9, 258, 9, 257, 9);
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(4, 1).image(GifTestFiles.solidFrame(3, 1, 0).rawLzw(lzw)).image(GifTestFiles.solidFrame(4, 1, 0).rawLzw(withoutClear)).end());

        int[][] canvases = AnimatedImageTestSupport.composeAll(image);

        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[1], GifTestFiles.PALETTE_PIXELS[2], GifTestFiles.PALETTE_PIXELS[1], 0}, canvases[0]);
        // 258 is the string of 3 and 2
        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[3], GifTestFiles.PALETTE_PIXELS[2], GifTestFiles.PALETTE_PIXELS[3], GifTestFiles.PALETTE_PIXELS[2]}, canvases[1]);
    }

    @Test
    void firstCodeAfterAClearCodeMustBeASingleColor() throws IOException {
        byte[] lzw = GifTestFiles.packCodes(256, 9, 3, 9, 256, 9, 258, 9, 1, 9, 257, 9);

        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[3], 0, 0, 0}, decodeFrame(GifTestFiles.paletteGif(4, 1).image(GifTestFiles.solidFrame(4, 1, 0).rawLzw(lzw)).end(), 0));
    }

    @Test
    void codesThatAreNotInTheTableYetStopDecoding() throws IOException {
        // After 1 and 2, the next free code is 259, so 260 is not in the table yet
        byte[] lzw = GifTestFiles.packCodes(256, 9, 1, 9, 2, 9, 260, 9, 1, 9, 257, 9);

        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[1], GifTestFiles.PALETTE_PIXELS[2], 0, 0}, decodeFrame(GifTestFiles.paletteGif(4, 1).image(GifTestFiles.solidFrame(4, 1, 0).rawLzw(lzw)).end(), 0));
    }

    @Test
    void invalidCodeKeepsThePixelsBeforeItAndTheDecodedPartOfItsRow() throws IOException {
        int[] table = GifTestFiles.randomColorTable(256, new Random(1));
        int[] indices = GifTestFiles.randomIndices(37 * 20, 256, new Random(2));
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().invalidCodeAfter(301);
        byte[] file = GifTestFiles.gif(37, 20, table).image(new GifTestFiles.Frame(37, 20, indices).lzw(lzw)).end();

        int[] canvas = decodeFrame(file, 0);

        assertTrue((lzw.encodedPixels % 37) != 0);
        assertPartialFrame(indices, table, lzw.encodedPixels, canvas, 0);
    }

    @Test
    void earlyEndCodeKeepsThePixelsBeforeIt() throws IOException {
        int[] table = GifTestFiles.randomColorTable(256, new Random(3));
        int[] indices = GifTestFiles.randomIndices(37 * 20, 256, new Random(4));
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().maxCodes(200);
        byte[] file = GifTestFiles.gif(37, 20, table).image(new GifTestFiles.Frame(37, 20, indices).lzw(lzw)).end();

        assertPartialFrame(indices, table, lzw.encodedPixels, decodeFrame(file, 0), 0);
    }

    @Test
    void dataThatEndsBeforeTheLastPixelKeepsThePixelsBeforeIt() throws IOException {
        int[] table = GifTestFiles.randomColorTable(256, new Random(5));
        int[] indices = GifTestFiles.randomIndices(37 * 20, 256, new Random(6));
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().maxCodes(450).noEndCode();
        // Codes have at least 9 bits, so the padding bits of the last byte never hold a whole code
        byte[] file = GifTestFiles.gif(37, 20, table).image(new GifTestFiles.Frame(37, 20, indices).lzw(lzw)).end();

        assertPartialFrame(indices, table, lzw.encodedPixels, decodeFrame(file, 0), 0);
    }

    @Test
    void brokenDataOfLaterFramesKeepsWhatTheCanvasShows() throws IOException {
        int[] table = GifTestFiles.randomColorTable(256, new Random(7));
        int[] indices = GifTestFiles.randomIndices(30 * 30, 256, new Random(8));
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().invalidCodeAfter(250);
        GifTestFiles.Frame background = GifTestFiles.solidFrame(30, 30, 9);
        GifImage image = GifTestFiles.read(GifTestFiles.gif(30, 30, table).image(background).image(new GifTestFiles.Frame(30, 30, indices).lzw(lzw)).end());

        int[][] canvases = AnimatedImageTestSupport.composeAll(image);

        assertPartialFrame(indices, table, lzw.encodedPixels, canvases[1], GifTestFiles.color(table, 9));
    }

    @Test
    void brokenInterlacedFramesKeepTheRowsTheyGot() throws IOException {
        int[] indices = GifTestFiles.randomIndices(10 * 16, 256, new Random(9));
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().maxCodes(43);
        GifImage image = GifTestFiles.read(GifTestFiles.gif(10, 16).image(new GifTestFiles.Frame(10, 16, indices).interlaced().lzw(lzw)).end());
        GifTestFiles.RecordingConsumer consumer = new GifTestFiles.RecordingConsumer(10, 16);

        GifTestFiles.decode(image, 0, consumer);

        int complete = lzw.encodedPixels / 10;
        assertTrue((lzw.encodedPixels % 10) != 0);
        int[] rows = consumer.rows();
        // Complete rows of the first pass (0 and 8), then of the second pass (4 and 12) and so on, plus the partial row
        assertArrayEquals(Arrays.copyOf(new int[] {0, 8, 4, 12, 2, 6, 10, 14}, complete + 1), rows);
        assertEquals(lzw.encodedPixels % 10, consumer.counts.get(complete));
    }

    @Test
    void dataAfterTheLastPixelGetsIgnored() throws IOException {
        int[] indices = GifTestFiles.randomIndices(20 * 20, 4, new Random(10));
        byte[] afterEnd = GifTestFiles.paletteGif(20, 20).image(new GifTestFiles.Frame(20, 20, indices).minimumCodeSize(2).lzw(new GifTestFiles.Lzw().codesAfterEnd(4095, 0, 5, 1000))).end();
        byte[] encoded = new GifTestFiles.Lzw().noEndCode().encode(indices, 2);
        byte[] garbage = new byte[3000];
        new Random(11).nextBytes(garbage);
        byte[] withGarbage = new byte[encoded.length + garbage.length];
        System.arraycopy(encoded, 0, withGarbage, 0, encoded.length);
        System.arraycopy(garbage, 0, withGarbage, encoded.length, garbage.length);
        byte[] garbageWithoutEndCode = GifTestFiles.paletteGif(20, 20).image(new GifTestFiles.Frame(20, 20, indices).minimumCodeSize(2).rawLzw(withGarbage)).end();
        byte[] withoutEndCode = GifTestFiles.paletteGif(20, 20).image(new GifTestFiles.Frame(20, 20, indices).minimumCodeSize(2).lzw(new GifTestFiles.Lzw().noEndCode())).end();

        int[] expected = GifTestFiles.expectedPixels(indices, GifTestFiles.PALETTE);
        assertArrayEquals(expected, decodeFrame(afterEnd, 0));
        assertArrayEquals(expected, decodeFrame(garbageWithoutEndCode, 0));
        assertArrayEquals(expected, decodeFrame(withoutEndCode, 0));
    }

    @Test
    void decodesLongStringsInNarrowTallImages() throws IOException {
        Random random = new Random(12);
        int[] indices = GifTestFiles.runIndices(3 * 16384, 2, 20000, random);
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(3, 16384).image(new GifTestFiles.Frame(3, 16384, indices).minimumCodeSize(1)).end());

        assertArrayEquals(GifTestFiles.expectedPixels(indices, GifTestFiles.PALETTE), AnimatedImageTestSupport.composeSingleFrame(image, 0));
    }

    @Test
    void decodesTheLongestPossibleStrings() throws IOException {
        // A single color with code size 1 and no clear code makes every string one byte longer than the one before, up to 4093 bytes once the table is full
        int width = 2048;
        int height = 4096;
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().codesWithFullTable(Integer.MAX_VALUE);
        GifTestFiles.Frame frame = GifTestFiles.solidFrame(width, height, 1).minimumCodeSize(1).lzw(lzw);
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(width, height).image(frame).end());
        int expected = GifTestFiles.PALETTE_PIXELS[1];
        int[] rows = new int[1];

        GifTestFiles.decode(image, 0, (y, x, step, pixels, count) -> {
            assertEquals(rows[0]++, y);
            assertEquals(width, count);
            for (int i = 0; i < count; i++) {
                if (pixels[i] != expected) throw new AssertionError("pixel " + i + " of row " + y);
            }
        });

        // Strings of 1 to 4092 pixels fill the table of the encoder, the other 14330 pixels are three strings of 4093 pixels and one of 2051, and the decoder table is full for the last three
        assertEquals(1, lzw.tableFills);
        assertEquals(3, lzw.fullTableCodes);
        assertEquals(height, rows[0]);
    }

    @Test
    void localColorTableReplacesTheGlobalOne() throws IOException {
        int[] local = GifTestFiles.randomColorTable(8, new Random(13));
        int[] indices = GifTestFiles.randomIndices(6 * 4, 8, new Random(14));
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(6, 4).image(new GifTestFiles.Frame(6, 4, indices).localColorTable(local)).image(new GifTestFiles.Frame(6, 4, indices)).end());

        int[][] canvases = AnimatedImageTestSupport.composeAll(image);

        assertArrayEquals(GifTestFiles.expectedPixels(indices, local), canvases[0]);
        // The next frame uses the global color table again, which has only 4 entries
        assertArrayEquals(GifTestFiles.expectedPixels(indices, GifTestFiles.PALETTE), canvases[1]);
    }

    @Test
    void indicesOutsideTheColorTableAndImagesWithoutColorTableAreOpaqueBlack() throws IOException {
        int[] indices = {0, 3, 4, 200, 255, 1};
        byte[] withTable = GifTestFiles.paletteGif(6, 1).image(new GifTestFiles.Frame(6, 1, indices)).end();
        byte[] withoutTable = GifTestFiles.gif(6, 1).image(new GifTestFiles.Frame(6, 1, indices)).end();

        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[0], GifTestFiles.PALETTE_PIXELS[3], 0xFF000000, 0xFF000000, 0xFF000000, GifTestFiles.PALETTE_PIXELS[1]}, decodeFrame(withTable, 0));
        int[] black = new int[6];
        Arrays.fill(black, 0xFF000000);
        assertArrayEquals(black, decodeFrame(withoutTable, 0));
    }

    @Test
    void transparentIndexBecomesFullyTransparentEvenOutsideTheColorTable() throws IOException {
        int[] indices = {0, 1, 2, 3, 77, 1};
        byte[] inside = GifTestFiles.paletteGif(6, 1).graphicControl(0, 0, 1).image(new GifTestFiles.Frame(6, 1, indices)).end();
        byte[] outside = GifTestFiles.paletteGif(6, 1).graphicControl(0, 0, 77).image(new GifTestFiles.Frame(6, 1, indices)).end();

        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[0], 0, GifTestFiles.PALETTE_PIXELS[2], GifTestFiles.PALETTE_PIXELS[3], 0xFF000000, 0}, decodeFrame(inside, 0));
        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[0], GifTestFiles.PALETTE_PIXELS[1], GifTestFiles.PALETTE_PIXELS[2], GifTestFiles.PALETTE_PIXELS[3], 0, GifTestFiles.PALETTE_PIXELS[1]}, decodeFrame(outside, 0));
    }

    @Test
    void transparentIndexOnlyAppliesToItsFrame() throws IOException {
        int[] indices = {1, 2};
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 1).graphicControl(1, 0, 1).image(new GifTestFiles.Frame(2, 1, indices)).image(new GifTestFiles.Frame(2, 1, indices)).end());

        // The same decoder decodes both frames, so the transparent entry of the first frame must not stay in the palette
        assertArrayEquals(new int[] {GifTestFiles.PALETTE_PIXELS[1], GifTestFiles.PALETTE_PIXELS[2]}, AnimatedImageTestSupport.composeAll(image)[1]);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pixelsOutsideTheCanvasDontGetHandedOver(boolean interlaced) throws IOException {
        int[] indices = GifTestFiles.randomIndices(9 * 13, 256, new Random(15));
        GifTestFiles.Frame clipped = new GifTestFiles.Frame(9, 13, indices).at(6, 4);
        if (interlaced) clipped.interlaced();
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(10, 10).image(GifTestFiles.solidFrame(10, 10, 0)).image(clipped).end());
        GifFrame frame = image.getFrame(1);
        GifTestFiles.RecordingConsumer consumer = new GifTestFiles.RecordingConsumer(4, 6);

        GifTestFiles.decode(image, 1, consumer);

        assertEquals(4, frame.width());
        assertEquals(6, frame.height());
        int[] rows = consumer.rows();
        int[] sorted = rows.clone();
        Arrays.sort(sorted);
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, sorted);
        for (int i = 0; i < rows.length; i++) assertEquals(4, consumer.counts.get(i));
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 4; x++) {
                assertEquals(GifTestFiles.color(GifTestFiles.PALETTE, indices[y * 9 + x]), consumer.pixels[y * 4 + x]);
            }
        }
    }

    @Test
    void framesWithoutVisiblePixelsReadNothing() throws IOException {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(GifTestFiles.paletteGif(4, 4).image(GifTestFiles.solidFrame(4, 4, 1)).image(GifTestFiles.solidFrame(3, 3, 2).at(4, 0)).image(GifTestFiles.solidFrame(0, 3, 2)).end());
        GifImage image = GifReader.read(source, "test.gif");
        source.reads.clear();
        GifTestFiles.RecordingConsumer consumer = new GifTestFiles.RecordingConsumer(1, 1);

        try (AnimatedFrameDecoder decoder = image.createFrameDecoder()) {
            decoder.decode(1, consumer);
            decoder.decode(2, consumer);
        }

        assertTrue(source.reads.isEmpty());
        assertEquals(0, consumer.rows().length);
    }

    @Test
    void oneDecoderHandlesFramesWithDifferentCodeSizes() throws IOException {
        Random random = new Random(16);
        int[] table = GifTestFiles.randomColorTable(256, random);
        int[][] frames = new int[6][];
        GifTestFiles.Gif gif = GifTestFiles.gif(40, 30, table);
        int[] codeSizes = {8, 2, 8, 1, 5, 8};
        for (int i = 0; i < frames.length; i++) {
            frames[i] = GifTestFiles.runIndices(40 * 30, 1 << codeSizes[i], 1 + random.nextInt(40), random);
            gif.image(new GifTestFiles.Frame(40, 30, frames[i]).minimumCodeSize(codeSizes[i]));
        }
        GifImage image = GifTestFiles.read(gif.end());

        int[][] canvases = AnimatedImageTestSupport.composeAll(image);

        for (int i = 0; i < frames.length; i++) assertArrayEquals(GifTestFiles.expectedPixels(frames[i], table), canvases[i], "frame " + i);
    }

    private static boolean readableByImageIO(int minimumCodeSize, int[] subBlockSizes, boolean interlaced, int height) {
        if ((minimumCodeSize < 2) || (interlaced && (height >= 2) && (height <= 4))) return false;
        for (int size : subBlockSizes) {
            if (size < 3) return false;
        }
        return true;
    }

    private static void assertPartialFrame(int[] indices, int[] table, int decodedPixels, int[] canvas, int untouched) {
        assertTrue((decodedPixels > 0) && (decodedPixels < indices.length));
        for (int i = 0; i < indices.length; i++) {
            int expected = (i < decodedPixels) ? GifTestFiles.color(table, indices[i]) : untouched;
            assertEquals(expected, canvas[i], "pixel " + i);
        }
    }

    /**
     * Parses the file and composes only the given frame on a transparent canvas.
     */
    @NotNull
    private static int[] decodeFrame(@NotNull byte[] file, int frameIndex) throws IOException {
        return AnimatedImageTestSupport.composeSingleFrame(GifTestFiles.read(file), frameIndex);
    }

    /**
     * Reads the color indices of every frame with the GIF decoder of the JDK, every frame as an image of its own size.
     */
    @NotNull
    private static int[][] readIndicesWithImageIO(@NotNull byte[] file) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(file))) {
            reader.setInput(in, false, true);
            int count = reader.getNumImages(true);
            int[][] frames = new int[count][];
            for (int i = 0; i < count; i++) {
                Raster raster = reader.read(i).getRaster();
                frames[i] = raster.getPixels(0, 0, raster.getWidth(), raster.getHeight(), (int[]) null);
            }
            return frames;
        } finally {
            reader.dispose();
        }
    }

    private static byte[] writeWithImageIO(int width, int height, int bits, int[] table, int[] indices, boolean interlaced) throws IOException {
        int colors = 1 << bits;
        byte[] red = new byte[colors];
        byte[] green = new byte[colors];
        byte[] blue = new byte[colors];
        for (int i = 0; i < colors; i++) {
            red[i] = (byte) table[i * 3];
            green[i] = (byte) table[i * 3 + 1];
            blue[i] = (byte) table[i * 3 + 2];
        }
        IndexColorModel colorModel = new IndexColorModel(bits, colors, red, green, blue);
        BufferedImage image = new BufferedImage(width, height, (bits == 8) ? BufferedImage.TYPE_BYTE_INDEXED : BufferedImage.TYPE_BYTE_BINARY, colorModel);
        image.getRaster().setPixels(0, 0, width, height, indices);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setProgressiveMode(interlaced ? ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

}
