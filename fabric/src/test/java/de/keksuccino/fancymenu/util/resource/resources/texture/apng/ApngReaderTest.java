package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApngReaderTest {

    private static final int[] RED = ApngTestFiles.solid(4, 4, 255, 0, 0, 255);
    private static final int[] GREEN_2X2 = ApngTestFiles.solid(2, 2, 0, 255, 0, 255);
    private static final int[] BLUE_1X3 = ApngTestFiles.solid(1, 3, 0, 0, 255, 255);

    @Test
    void plainPngBecomesOneStaticFrame() throws IOException {
        ApngImage image = read(rgba(4, 4).header().imageData(RED).end());

        assertFalse(image.isAnimated());
        assertEquals(1, image.getFrameCount());
        ApngFrame frame = image.getFrame(0);
        assertTrue(frame.coversCanvas(4, 4));
        assertEquals(ApngFrame.BLEND_OP_SOURCE, frame.blendOp());
        assertEquals(ApngFrame.DISPOSE_OP_NONE, frame.disposeOp());
    }

    @Test
    void readsAllFrameControlValuesAndThePlayCount() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(3, 7)
                .frameControl(4, 4, 0, 0, 1, 25, ApngFrame.DISPOSE_OP_NONE, ApngFrame.BLEND_OP_SOURCE).imageData(RED)
                .frame(2, 2, 1, 2, 3, 10, ApngFrame.DISPOSE_OP_BACKGROUND, ApngFrame.BLEND_OP_OVER, GREEN_2X2)
                .frame(1, 3, 3, 1, 1, 1000, ApngFrame.DISPOSE_OP_PREVIOUS, ApngFrame.BLEND_OP_SOURCE, BLUE_1X3)
                .end();

        ApngImage image = read(file);

        assertTrue(image.isAnimated());
        assertEquals(7, image.getNumPlays());
        assertEquals(3, image.getFrameCount());
        assertEquals(40_000_000L, image.getFrame(0).delayNanos());
        ApngFrame second = image.getFrame(1);
        assertEquals(1, second.x());
        assertEquals(2, second.y());
        assertEquals(2, second.width());
        assertEquals(2, second.height());
        assertEquals(300_000_000L, second.delayNanos());
        assertEquals(ApngFrame.DISPOSE_OP_BACKGROUND, second.disposeOp());
        assertEquals(ApngFrame.BLEND_OP_OVER, second.blendOp());
        ApngFrame third = image.getFrame(2);
        assertEquals(3, third.x());
        assertEquals(1, third.y());
        assertEquals(1_000_000L, third.delayNanos());
        assertEquals(ApngFrame.DISPOSE_OP_PREVIOUS, third.disposeOp());
    }

    @Test
    void zeroDelayDenominatorMeansHundredthsOfASecond() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(1, 0).frameControl(4, 4, 0, 0, 5, 0, 0, 0).imageData(RED).end();

        assertEquals(50_000_000L, read(file).getFrame(0).delayNanos());
    }

    @Test
    void hiddenDefaultImageIsNoFrame() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(1, 0).imageData(RED)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2)
                .end();

        ApngImage image = read(file);

        assertTrue(image.isAnimated());
        assertEquals(1, image.getFrameCount());
        assertEquals(2, image.getFrame(0).width());
        assertEquals(ApngImage.INFINITE_PLAYS, image.getNumPlays());
    }

    @Test
    void dataSplitOverManySmallChunksGetsJoined() throws IOException {
        int[] samples = ApngTestFiles.randomSamples(9, 9, 8, ApngImage.COLOR_TYPE_RGBA, new java.util.Random(3));
        byte[] whole = rgba(9, 9).header().imageData(samples).end();
        byte[] split = rgba(9, 9).splitDataChunks(1).header().imageData(samples).end();

        assertArrayEquals(ApngFrameDecoderTest.decodeFirstFrame(whole), ApngFrameDecoderTest.decodeFirstFrame(split));
    }

    @Test
    void frameOutsideTheCanvasEndsTheAnimationBeforeIt() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(4, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2)
                .frame(2, 2, 3, 3, 1, 10, 0, 0, GREEN_2X2)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2)
                .end();

        ApngImage image = read(file);

        assertTrue(image.isAnimated());
        assertEquals(2, image.getFrameCount());
        assertEquals(2, image.getFrame(1).width());
    }

    @Test
    void brokenFirstAnimationFrameKeepsTheDefaultImage() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(2, 0).imageData(RED)
                .frame(2, 2, 3, 3, 1, 10, 0, 0, GREEN_2X2)
                .end();

        ApngImage image = read(file);

        assertFalse(image.isAnimated());
        assertTrue(image.getFrame(0).coversCanvas(4, 4));
    }

    @Test
    void corruptedFrameDataEndsTheAnimationBeforeThatFrame() throws IOException {
        ApngTestFiles.Png png = rgba(4, 4).header().animationControl(3, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2);
        byte[] file = png.frame(2, 2, 2, 2, 1, 10, 0, 0, GREEN_2X2).end();
        // The CRC of the last fdAT chunk, which comes right before the 12 bytes of IEND
        file[file.length - 13] ^= 0x01;

        ApngImage image = read(file);

        assertTrue(image.isAnimated());
        assertEquals(2, image.getFrameCount());
    }

    @Test
    void corruptedFrameControlEndsTheAnimationBeforeIt() throws IOException {
        ApngTestFiles.Png png = rgba(4, 4).header().animationControl(3, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2);
        int frameControlStart = png.truncated().length;
        byte[] file = png.frame(2, 2, 2, 2, 1, 10, 0, 0, GREEN_2X2).end();
        // A byte in the data of the last fcTL chunk
        file[frameControlStart + 8 + 6] ^= 0x01;

        ApngImage image = read(file);

        assertTrue(image.isAnimated());
        assertEquals(2, image.getFrameCount());
    }

    @Test
    void corruptedAnimationControlKeepsTheDefaultImage() throws IOException {
        ApngTestFiles.Png png = rgba(4, 4).header();
        int animationControlStart = png.truncated().length;
        byte[] file = png.animationControl(2, 0).frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED).frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2).end();
        file[animationControlStart + 8 + 3] ^= 0x01;

        ApngImage image = read(file);

        assertFalse(image.isAnimated());
        assertEquals(1, image.getFrameCount());
    }

    @Test
    void missingEndChunkKeepsTheLastFrame() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(3, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2)
                .frame(2, 2, 2, 2, 1, 10, 0, 0, GREEN_2X2)
                .truncated();

        ApngImage image = read(file);

        assertEquals(3, image.getFrameCount());
    }

    @Test
    void hugeDeclaredChunkLengthOnlyReadsWhatIsThere() {
        byte[] header = rgba(4, 4).header().truncated();
        byte[] file = Arrays.copyOf(header, header.length + 20);
        ApngTestFiles.putInt(file, header.length, 0x7FFFFFF0);
        file[header.length + 4] = 'I';
        file[header.length + 5] = 'D';
        file[header.length + 6] = 'A';
        file[header.length + 7] = 'T';

        // Allocating the declared 2 GB would fail with an OutOfMemoryError in the test JVM instead
        assertThrows(IOException.class, () -> read(file));
    }

    @Test
    void firstFrameThatDoesNotMatchTheDefaultImageBreaksTheAnimation() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(1, 0).frameControl(2, 2, 0, 0, 1, 10, 0, 0).imageData(RED).end();

        assertFalse(read(file).isAnimated());
    }

    @Test
    void invalidDisposeOperationBreaksTheAnimation() throws IOException {
        byte[] file = rgba(4, 4).header().animationControl(2, 0).imageData(RED).frame(2, 2, 0, 0, 1, 10, 3, 0, GREEN_2X2).end();

        assertFalse(read(file).isAnimated());
    }

    @Test
    void animationControlAfterTheImageDataGetsIgnored() throws IOException {
        byte[] file = rgba(4, 4).header().imageData(RED).animationControl(1, 0).frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2).end();

        ApngImage image = read(file);

        assertFalse(image.isAnimated());
        assertEquals(4, image.getFrame(0).width());
    }

    @Test
    void wrongSequenceNumbersStillUseTheChunkOrder() throws IOException {
        ApngTestFiles.Png png = rgba(4, 4).header().animationControl(2, 0).frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED);
        // Chunks written by hand, with sequence numbers that skip and repeat
        png.chunk("fcTL", fcTL(7, 2, 2, 1, 1));
        byte[] data = ApngTestFiles.encodeImageData(2, 2, 8, ApngImage.COLOR_TYPE_RGBA, GREEN_2X2, false, 0, new java.util.Random());
        byte[] fdAT = new byte[data.length + 4];
        ApngTestFiles.putInt(fdAT, 0, 3);
        System.arraycopy(data, 0, fdAT, 4, data.length);
        png.chunk("fdAT", fdAT);

        ApngImage image = read(png.end());

        assertTrue(image.isAnimated());
        assertEquals(2, image.getFrameCount());
        assertEquals(1, image.getFrame(1).x());
    }

    @Test
    void truncatedFileDropsOnlyTheFrameThatGotCutOff() throws IOException {
        byte[] complete = rgba(4, 4).header().animationControl(3, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2)
                .frame(2, 2, 2, 2, 1, 10, 0, 0, GREEN_2X2)
                .truncated();

        ApngImage image = read(Arrays.copyOf(complete, complete.length - 6));

        assertTrue(image.isAnimated());
        assertEquals(2, image.getFrameCount());
    }

    @Test
    void fileWithoutImageDataFails() {
        assertThrows(IOException.class, () -> read(rgba(4, 4).header().end()));
    }

    @Test
    void invalidSignatureFails() {
        byte[] file = rgba(4, 4).header().imageData(RED).end();
        file[1] = 'X';

        assertThrows(IOException.class, () -> read(file));
    }

    @Test
    void corruptedCriticalChunkFails() {
        byte[] file = rgba(4, 4).header().imageData(RED).end();
        // A byte of the IHDR data, so its CRC does not match anymore
        file[20] ^= 0x01;

        assertThrows(IOException.class, () -> read(file));
    }

    @Test
    void unknownAncillaryChunksGetSkippedAndUnknownCriticalChunksFail() throws IOException {
        byte[] ancillary = rgba(4, 4).header().chunk("teXt", new byte[] {'a', 0, 'b'}).chunk("zzZz", new byte[100]).imageData(RED).end();
        byte[] critical = rgba(4, 4).header().chunk("ABCD", new byte[4]).imageData(RED).end();

        assertEquals(1, read(ancillary).getFrameCount());
        assertThrows(IOException.class, () -> read(critical));
    }

    @Test
    void paletteImageWithoutPaletteFails() {
        byte[] file = ApngTestFiles.png(2, 2, 8, ApngImage.COLOR_TYPE_PALETTE).header().imageData(new int[4]).end();

        assertThrows(IOException.class, () -> read(file));
    }

    @Test
    void invalidBitDepthAndOversizedImagesFail() {
        assertThrows(IOException.class, () -> read(ApngTestFiles.png(2, 2, 4, ApngImage.COLOR_TYPE_RGB).header().end()));
        assertThrows(IOException.class, () -> read(rgba(ApngReader.MAX_DIMENSION + 1, 1).header().end()));
        assertThrows(IOException.class, () -> read(rgba(0, 4).header().end()));
    }

    @Test
    void compressedDataIsAllTheImageKeeps() throws IOException {
        byte[] file = rgba(4, 4).header().chunk("tEXt", new byte[5000]).animationControl(2, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(RED)
                .frame(2, 2, 0, 0, 1, 10, 0, 0, GREEN_2X2)
                .end();

        ApngImage image = read(file);

        assertTrue(image.getCompressedDataSize() < 200, "kept " + image.getCompressedDataSize() + " bytes");
    }

    @Test
    void corruptedTransparencyChunkGetsIgnored() throws IOException {
        ApngTestFiles.Png png = ApngTestFiles.png(2, 1, 8, ApngImage.COLOR_TYPE_GRAYSCALE).header();
        int transparencyStart = png.truncated().length;
        byte[] file = png.transparency(7).imageData(new int[] {7, 8}).end();
        file[transparencyStart + 8 + 1] ^= 0x01;

        int[] pixels = ApngFrameDecoderTest.decodeFirstFrame(file);

        assertEquals(0xFF, pixels[0] >>> 24);
    }

    private static ApngTestFiles.Png rgba(int width, int height) {
        return ApngTestFiles.png(width, height, 8, ApngImage.COLOR_TYPE_RGBA);
    }

    private static ApngImage read(byte[] file) throws IOException {
        return ApngReader.read(new ByteArrayInputStream(file), "test.png");
    }

    private static byte[] fcTL(int sequenceNumber, int width, int height, int x, int y) {
        byte[] data = new byte[26];
        ApngTestFiles.putInt(data, 0, sequenceNumber);
        ApngTestFiles.putInt(data, 4, width);
        ApngTestFiles.putInt(data, 8, height);
        ApngTestFiles.putInt(data, 12, x);
        ApngTestFiles.putInt(data, 16, y);
        data[21] = 1;
        data[23] = 10;
        return data;
    }

}
