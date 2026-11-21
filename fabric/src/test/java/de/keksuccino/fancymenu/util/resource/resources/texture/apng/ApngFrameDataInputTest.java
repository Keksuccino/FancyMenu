package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedFrameDecoder;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSourceChangedException;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApngFrameDataInputTest {

    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;

    @Test
    void decodingReadsTheContiguousChunksOfAFrameWithOneRead() throws IOException {
        byte[] file = animation(5);
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        ApngImage image = ApngReader.read(source, "test.png");
        ApngFrame.DataChunks chunks = image.getFrame(1).dataChunks();
        assertTrue(chunks.count() > 1);
        source.reads.clear();

        try (AnimatedFrameDecoder decoder = image.createFrameDecoder()) {
            decoder.decode(1, (y, x, step, pixels, count) -> {});
        }

        // One read that covers all chunks of the frame and nothing else
        assertEquals(1, source.reads.size());
        assertArrayEquals(new long[] {chunks.positions()[0], chunks.end(chunks.count() - 1) - chunks.positions()[0]}, source.reads.get(0));
        assertArrayEquals(ApngTestFiles.composeAll(file), AnimatedImageTestSupport.composeAll(image));
    }

    @Test
    void chunksWithOtherChunksBetweenThemGetReadOnTheirOwn() throws IOException {
        int[] samples = ApngTestFiles.randomSamples(WIDTH, HEIGHT, 8, ApngImage.COLOR_TYPE_RGBA, new Random(2));
        byte[] zlib = ApngTestFiles.encodeImageData(WIDTH, HEIGHT, 8, ApngImage.COLOR_TYPE_RGBA, samples, false, 0, new Random());
        ApngTestFiles.Png png = rgba().header().animationControl(2, 0)
                .frameControl(WIDTH, HEIGHT, 0, 0, 1, 10, 0, 0).imageData(new int[WIDTH * HEIGHT * 4])
                .frameControl(WIDTH, HEIGHT, 0, 0, 1, 10, 0, 0);
        png.fdAT(Arrays.copyOf(zlib, zlib.length / 2)).chunk("tEXt", new byte[300]).fdAT(Arrays.copyOfRange(zlib, zlib.length / 2, zlib.length));
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(png.end());
        ApngImage image = ApngReader.read(source, "test.png");
        ApngFrame.DataChunks chunks = image.getFrame(1).dataChunks();
        source.reads.clear();

        int[] canvas = decode(image, 1);

        for (int i = 0; i < canvas.length; i++) {
            assertEquals(AnimatedImageTestSupport.abgr(samples[i * 4], samples[i * 4 + 1], samples[i * 4 + 2], samples[i * 4 + 3]), canvas[i], "pixel " + i);
        }
        assertEquals(2, source.reads.size());
        assertArrayEquals(new long[] {chunks.positions()[0], chunks.end(0) - chunks.positions()[0]}, source.reads.get(0));
        assertArrayEquals(new long[] {chunks.positions()[1], chunks.end(1) - chunks.positions()[1]}, source.reads.get(1));
    }

    @Test
    void bigFramesGetReadInPiecesThatFitTheBuffer() throws IOException {
        // Random pixels barely compress, so the single IDAT chunk is much bigger than the read buffer
        int[] samples = ApngTestFiles.randomSamples(160, 160, 8, ApngImage.COLOR_TYPE_RGBA, new Random(3));
        byte[] file = ApngTestFiles.png(160, 160, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(samples).end();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        ApngImage image = ApngReader.read(source, "test.png");
        source.reads.clear();

        int[] canvas = decode(image, 0);

        for (int i = 0; i < canvas.length; i++) {
            assertEquals(AnimatedImageTestSupport.abgr(samples[i * 4], samples[i * 4 + 1], samples[i * 4 + 2], samples[i * 4 + 3]), canvas[i], "pixel " + i);
        }
        assertTrue(source.reads.size() > 1);
        for (long[] read : source.reads) assertTrue(read[1] <= 64 * 1024, "read of " + read[1] + " bytes");
    }

    @Test
    void changedByteAtTheEndOfABigChunkGetsNoticed() throws IOException {
        int[] samples = ApngTestFiles.randomSamples(160, 160, 8, ApngImage.COLOR_TYPE_RGBA, new Random(4));
        byte[] file = ApngTestFiles.png(160, 160, 8, ApngImage.COLOR_TYPE_RGBA).header().imageData(samples).end();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        ApngImage image = ApngReader.read(source, "test.png");
        ApngFrame.DataChunks chunks = image.getFrame(0).dataChunks();
        // The last byte of the zlib checksum, which the inflater never needs for the pixels
        source.data = file.clone();
        source.data[(int) chunks.end(0) - 5] ^= 0x01;

        assertThrows(AnimatedImageSourceChangedException.class, () -> decode(image, 0));
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    void changedSourceThrowsTheSourceChangedException(Change change) throws IOException {
        byte[] file = animation(4);
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        ApngImage image = ApngReader.read(source, "test.png");
        ApngFrame.DataChunks chunks = image.getFrame(1).dataChunks();

        source.data = change.apply(file.clone(), chunks);

        assertThrows(AnimatedImageSourceChangedException.class, () -> decode(image, 1));
    }

    @Test
    void changesOutsideTheFrameDataDontMatter() throws IOException {
        ApngTestFiles.Png png = rgba().header().chunk("tEXt", new byte[50]).animationControl(2, 0)
                .frameControl(WIDTH, HEIGHT, 0, 0, 1, 10, 0, 0).imageData(randomPixels(5));
        int textStart = 8 + 25;
        byte[] file = png.frame(4, 4, 2, 2, 1, 10, 0, 0, ApngTestFiles.randomSamples(4, 4, 8, ApngImage.COLOR_TYPE_RGBA, new Random(6))).end();
        int[][] expected = ApngTestFiles.composeAll(file);
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        ApngImage image = ApngReader.read(source, "test.png");

        source.data = file.clone();
        source.data[textStart + 8 + 20] ^= 0x01;
        source.data[source.data.length - 1] ^= 0x01;

        assertArrayEquals(expected, AnimatedImageTestSupport.composeAll(image));
    }

    @Test
    void decodingAFrameAgainReadsItAgain() throws IOException {
        byte[] file = animation(3);
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        ApngImage image = ApngReader.read(source, "test.png");
        source.reads.clear();

        decode(image, 1);
        int readsOfOnePass = source.reads.size();
        decode(image, 1);

        assertTrue(readsOfOnePass > 0);
        assertEquals(readsOfOnePass * 2, source.reads.size());
    }

    private static int[] decode(ApngImage image, int frameIndex) throws IOException {
        return AnimatedImageTestSupport.composeSingleFrame(image, frameIndex);
    }

    /**
     * An 8x8 animation with random pixels, whose frame data is split over chunks of at most 40 bytes.
     */
    private static byte[] animation(int frames) {
        ApngTestFiles.Png png = rgba().splitDataChunks(40).header().animationControl(frames, 0)
                .frameControl(WIDTH, HEIGHT, 0, 0, 1, 10, 0, 0).imageData(randomPixels(1));
        for (int i = 1; i < frames; i++) png.frame(WIDTH, HEIGHT, 0, 0, 1, 10, 0, 0, randomPixels(10 + i));
        return png.end();
    }

    private static int[] randomPixels(long seed) {
        return ApngTestFiles.randomSamples(WIDTH, HEIGHT, 8, ApngImage.COLOR_TYPE_RGBA, new Random(seed));
    }

    private static ApngTestFiles.Png rgba() {
        return ApngTestFiles.png(WIDTH, HEIGHT, 8, ApngImage.COLOR_TYPE_RGBA);
    }

    /**
     * Ways a file can change after it got parsed, all in or around the second chunk of a frame.
     */
    enum Change {

        DATA_BYTE {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                file[(int) chunks.positions()[1] + 12 + 10] ^= 0x01;
                return file;
            }
        },
        /** Breaks the zlib stream right away, before the CRC of the chunk can get checked. **/
        ZLIB_HEADER {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                file[(int) chunks.positions()[0] + 12] = 0;
                return file;
            }
        },
        LENGTH_FIELD {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                file[(int) chunks.positions()[1] + 3] ^= 0x01;
                return file;
            }
        },
        TYPE_FIELD {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                file[(int) chunks.positions()[1] + 4] = 'X';
                return file;
            }
        },
        SEQUENCE_NUMBER {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                file[(int) chunks.positions()[1] + 11] ^= 0x01;
                return file;
            }
        },
        CRC {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                file[(int) chunks.end(1) - 1] ^= 0x01;
                return file;
            }
        },
        CUT_IN_THE_FRAME_DATA {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                return Arrays.copyOf(file, (int) chunks.positions()[1] + 20);
            }
        },
        CUT_BEFORE_THE_FRAME_DATA {
            @Override
            byte[] apply(byte[] file, ApngFrame.DataChunks chunks) {
                return Arrays.copyOf(file, (int) chunks.positions()[0] - 10);
            }
        };

        abstract byte[] apply(byte[] file, ApngFrame.DataChunks chunks);

    }

}
