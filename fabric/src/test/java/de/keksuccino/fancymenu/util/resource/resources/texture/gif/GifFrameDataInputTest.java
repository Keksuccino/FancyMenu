package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GifFrameDataInputTest {

    private static final int[] LOCAL_TABLE = GifTestFiles.randomColorTable(256, new Random(1));
    private static final int[] INDICES = GifTestFiles.randomIndices(40 * 40, 256, new Random(2));

    @Test
    void framesThatFitTheBufferGetReadWithOneRead() throws IOException {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(animation());
        GifImage image = GifReader.read(source, "test.gif");
        GifFrame frame = image.getFrame(1);
        source.reads.clear();

        GifTestFiles.RecordingConsumer consumer = new GifTestFiles.RecordingConsumer(40, 40);
        GifTestFiles.decode(image, 1, consumer);

        assertArrayEquals(GifTestFiles.expectedPixels(INDICES, LOCAL_TABLE), consumer.pixels);
        assertEquals(1, source.reads.size());
        assertArrayEquals(new long[] {frame.dataPosition(), frame.dataLength()}, source.reads.get(0));
    }

    @Test
    void bigFramesGetReadInPiecesThatFitTheBuffer() throws IOException {
        int[] indices = GifTestFiles.randomIndices(400 * 400, 256, new Random(3));
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(GifTestFiles.gif(400, 400, LOCAL_TABLE).image(new GifTestFiles.Frame(400, 400, indices)).end());
        GifImage image = GifReader.read(source, "test.gif");
        GifFrame frame = image.getFrame(0);
        assertTrue(frame.dataLength() > 3 * 64 * 1024);
        source.reads.clear();

        GifTestFiles.RecordingConsumer consumer = new GifTestFiles.RecordingConsumer(400, 400);
        GifTestFiles.decode(image, 0, consumer);

        assertArrayEquals(GifTestFiles.expectedPixels(indices, LOCAL_TABLE), consumer.pixels);
        long next = frame.dataPosition();
        for (long[] read : source.reads) {
            assertEquals(next, read[0]);
            assertTrue(read[1] <= 64 * 1024, "read of " + read[1] + " bytes");
            next = read[0] + read[1];
        }
        assertEquals(frame.dataEnd(), next);
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    void changedFrameDataThrowsTheSourceChangedException(Change change) throws IOException {
        byte[] file = animation();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        GifImage image = GifReader.read(source, "test.gif");

        source.data = change.apply(file.clone(), image.getFrame(1));

        assertThrows(AnimatedImageSourceChangedException.class, () -> GifTestFiles.decode(image, 1, new GifTestFiles.RecordingConsumer(40, 40)));
    }

    @Test
    void changedDataAfterTheLastPixelGetsNoticed() throws IOException {
        byte[] lzw = new GifTestFiles.Lzw().encode(INDICES, 8);
        byte[] withExtraData = Arrays.copyOf(lzw, lzw.length + 2000);
        byte[] file = GifTestFiles.gif(40, 40, LOCAL_TABLE).image(new GifTestFiles.Frame(40, 40, INDICES).rawLzw(withExtraData)).end();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        GifImage image = GifReader.read(source, "test.gif");
        assertDoesNotThrow(() -> GifTestFiles.decode(image, 0, new GifTestFiles.RecordingConsumer(40, 40)));

        source.data = file.clone();
        source.data[(int) image.getFrame(0).dataEnd() - 3] ^= 0x01;

        assertThrows(AnimatedImageSourceChangedException.class, () -> GifTestFiles.decode(image, 0, new GifTestFiles.RecordingConsumer(40, 40)));
    }

    @Test
    void changedDataBelowTheVisibleRowsGetsNoticed() throws IOException {
        // Only the first 5 rows of the second frame are on the canvas, so decoding stops after them
        byte[] file = GifTestFiles.gif(40, 40, LOCAL_TABLE).image(GifTestFiles.solidFrame(40, 40, 1)).image(new GifTestFiles.Frame(40, 40, INDICES).at(0, 35)).end();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        GifImage image = GifReader.read(source, "test.gif");
        GifFrame frame = image.getFrame(1);
        assertEquals(5, frame.height());

        source.data = file.clone();
        source.data[(int) frame.dataEnd() - 10] ^= 0x01;

        assertThrows(AnimatedImageSourceChangedException.class, () -> GifTestFiles.decode(image, 1, new GifTestFiles.RecordingConsumer(40, 5)));
    }

    @Test
    void sourceThatCantGetReadAnymoreThrowsTheSourceChangedException() throws IOException {
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(animation());
        GifImage image = GifReader.read(source, "test.gif");

        // The memory source fails all reads once it is closed
        source.close();

        assertThrows(AnimatedImageSourceChangedException.class, () -> GifTestFiles.decode(image, 1, new GifTestFiles.RecordingConsumer(40, 40)));
    }

    @Test
    void changesOutsideTheFrameDataDontMatter() throws IOException {
        GifTestFiles.Gif gif = GifTestFiles.gif(40, 40).comment("A comment that gets changed");
        int commentStart = 13;
        gif.graphicControl(0, 10, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidFrame(40, 40, 7).localColorTable(LOCAL_TABLE));
        int secondControl = gif.size();
        gif.graphicControl(2, 10, GifTestFiles.NO_TRANSPARENCY);
        byte[] file = gif.image(new GifTestFiles.Frame(40, 40, INDICES).localColorTable(LOCAL_TABLE)).end();
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);
        GifImage image = GifReader.read(source, "test.gif");
        GifFrame first = image.getFrame(0);

        source.data = file.clone();
        source.data[commentStart + 5] ^= 0x01;
        source.data[secondControl + 4] ^= 0x01;
        source.data[(int) first.dataEnd() - 2] ^= 0x01;
        source.data[source.data.length - 1] = 0x00;
        GifTestFiles.RecordingConsumer consumer = new GifTestFiles.RecordingConsumer(40, 40);
        GifTestFiles.decode(image, 1, consumer);

        assertArrayEquals(GifTestFiles.expectedPixels(INDICES, LOCAL_TABLE), consumer.pixels);
    }

    @Test
    void brokenFrameDataIsNoSourceChange() throws IOException {
        GifTestFiles.Lzw lzw = new GifTestFiles.Lzw().invalidCodeAfter(100);
        GifImage image = GifTestFiles.read(GifTestFiles.gif(40, 40, LOCAL_TABLE).image(new GifTestFiles.Frame(40, 40, INDICES).lzw(lzw)).end());

        assertDoesNotThrow(() -> GifTestFiles.decode(image, 0, new GifTestFiles.RecordingConsumer(40, 40)));
    }

    @Test
    void decoderDecodesTheSameFramesAgainForEveryLoop() throws IOException {
        GifImage image = GifTestFiles.read(animation());
        GifTestFiles.RecordingConsumer[] first = new GifTestFiles.RecordingConsumer[image.getFrameCount()];
        GifTestFiles.RecordingConsumer[] second = new GifTestFiles.RecordingConsumer[image.getFrameCount()];

        try (AnimatedFrameDecoder decoder = image.createFrameDecoder()) {
            for (GifTestFiles.RecordingConsumer[] play : new GifTestFiles.RecordingConsumer[][] {first, second}) {
                for (int i = 0; i < image.getFrameCount(); i++) {
                    play[i] = new GifTestFiles.RecordingConsumer(40, 40);
                    decoder.decode(i, play[i]);
                }
            }
        }

        for (int i = 0; i < image.getFrameCount(); i++) {
            assertArrayEquals(first[i].pixels, second[i].pixels, "frame " + i);
            assertArrayEquals(first[i].rows(), second[i].rows(), "frame " + i);
        }
    }

    /**
     * Three frames, the second one with a local color table and its data split into sub-blocks of 100 bytes.
     */
    private static byte[] animation() {
        return GifTestFiles.paletteGif(40, 40).netscapeLoop(0)
                .image(GifTestFiles.solidFrame(40, 40, 1))
                .image(new GifTestFiles.Frame(40, 40, INDICES).localColorTable(LOCAL_TABLE).subBlockSizes(100))
                .image(GifTestFiles.solidFrame(20, 20, 2).at(10, 10))
                .end();
    }

    private enum Change {

        LOCAL_COLOR_TABLE {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[(int) frame.dataPosition() + 100] ^= 0x01;
                return file;
            }
        },
        MINIMUM_CODE_SIZE {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[codeSizePosition(frame)] = 7;
                return file;
            }
        },
        FIRST_DATA_BYTE {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[codeSizePosition(frame) + 2] ^= 0x01;
                return file;
            }
        },
        LAST_DATA_BYTE {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[(int) frame.dataEnd() - 2] ^= 0x01;
                return file;
            }
        },
        SHORTER_SUB_BLOCK {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[codeSizePosition(frame) + 1] = 99;
                return file;
            }
        },
        LONGER_SUB_BLOCK {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[codeSizePosition(frame) + 1] = 101;
                return file;
            }
        },
        EARLY_BLOCK_TERMINATOR {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[codeSizePosition(frame) + 1 + 101] = 0;
                return file;
            }
        },
        MISSING_BLOCK_TERMINATOR {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                file[(int) frame.dataEnd() - 1] = 5;
                return file;
            }
        },
        SHORTENED_IN_THE_DATA {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                return Arrays.copyOf(file, (int) frame.dataEnd() - 10);
            }
        },
        SHORTENED_BEFORE_THE_DATA {
            @Override
            byte[] apply(byte[] file, GifFrame frame) {
                return Arrays.copyOf(file, (int) frame.dataPosition() - 1);
            }
        };

        abstract byte[] apply(byte[] file, GifFrame frame);

        private static int codeSizePosition(GifFrame frame) {
            return (int) frame.dataPosition() + frame.localColorTableSize() * 3;
        }

    }

}
