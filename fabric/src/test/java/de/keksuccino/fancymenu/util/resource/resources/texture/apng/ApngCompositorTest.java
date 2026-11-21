package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import static de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngTestFiles.abgr;
import static de.keksuccino.fancymenu.util.resource.resources.texture.apng.ApngTestFiles.solid;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApngCompositorTest {

    private static final int RED = abgr(255, 0, 0, 255);
    private static final int GREEN = abgr(0, 255, 0, 255);
    private static final int BLUE = abgr(0, 0, 255, 255);
    private static final int CLEAR = 0;

    @Test
    void disposeNoneKeepsTheFrameForTheNextOne() throws IOException {
        int[][] canvases = composeAll(animation(ApngFrame.DISPOSE_OP_NONE, ApngFrame.BLEND_OP_SOURCE));

        assertEquals(RED, canvases[1][0]);
        assertEquals(GREEN, canvases[1][1 * 4 + 1]);
        assertEquals(GREEN, canvases[2][1 * 4 + 1]);
        assertEquals(BLUE, canvases[2][3 * 4 + 3]);
    }

    @Test
    void disposeBackgroundClearsTheFrameRegionBeforeTheNextFrame() throws IOException {
        int[][] canvases = composeAll(animation(ApngFrame.DISPOSE_OP_BACKGROUND, ApngFrame.BLEND_OP_SOURCE));

        assertEquals(GREEN, canvases[1][1 * 4 + 1]);
        // The region of the green frame got cleared, the red default image around it stays
        assertEquals(CLEAR, canvases[2][1 * 4 + 1]);
        assertEquals(CLEAR, canvases[2][2 * 4 + 2]);
        assertEquals(RED, canvases[2][0]);
        assertEquals(BLUE, canvases[2][3 * 4 + 3]);
    }

    @Test
    void disposePreviousRevertsTheFrameRegion() throws IOException {
        int[][] canvases = composeAll(animation(ApngFrame.DISPOSE_OP_PREVIOUS, ApngFrame.BLEND_OP_SOURCE));

        assertEquals(GREEN, canvases[1][1 * 4 + 1]);
        assertEquals(RED, canvases[2][1 * 4 + 1]);
        assertEquals(RED, canvases[2][2 * 4 + 2]);
        assertEquals(BLUE, canvases[2][3 * 4 + 3]);
    }

    @Test
    void revertingTheFirstFrameClearsIt() throws IOException {
        byte[] file = rgba().animationControl(2, 0)
                .frameControl(4, 4, 0, 0, 1, 10, ApngFrame.DISPOSE_OP_PREVIOUS, ApngFrame.BLEND_OP_SOURCE).imageData(solid(4, 4, 255, 0, 0, 255))
                .frame(1, 1, 0, 0, 1, 10, ApngFrame.DISPOSE_OP_NONE, ApngFrame.BLEND_OP_SOURCE, solid(1, 1, 0, 255, 0, 255))
                .end();

        int[][] canvases = composeAll(file);

        assertEquals(GREEN, canvases[1][0]);
        assertEquals(CLEAR, canvases[1][5]);
    }

    @Test
    void blendOverCompositesOverTheCanvas() throws IOException {
        byte[] file = rgba().animationControl(2, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(solid(4, 4, 200, 100, 0, 255))
                .frame(2, 2, 0, 0, 1, 10, 0, ApngFrame.BLEND_OP_OVER, solid(2, 2, 0, 0, 255, 128))
                .end();

        int[][] canvases = composeAll(file);

        assertEquals(ApngCompositor.blendOver(abgr(0, 0, 255, 128), abgr(200, 100, 0, 255)), canvases[1][0]);
        assertEquals(abgr(200, 100, 0, 255), canvases[1][3]);
    }

    @Test
    void blendOverMatchesTheExactFormula() {
        Random random = new Random(42);
        for (int i = 0; i < 200_000; i++) {
            int source = random.nextInt();
            int destination = random.nextInt();
            int result = ApngCompositor.blendOver(source, destination);
            double sourceAlpha = (source >>> 24) / 255.0;
            double destinationAlpha = (destination >>> 24) / 255.0;
            double outAlpha = sourceAlpha + destinationAlpha * (1.0 - sourceAlpha);
            assertEquals(outAlpha * 255.0, result >>> 24, 0.5 + 1e-9);
            if (outAlpha == 0.0) continue;
            for (int shift = 0; shift < 24; shift += 8) {
                double expected = (((source >>> shift) & 0xFF) * sourceAlpha + ((destination >>> shift) & 0xFF) * destinationAlpha * (1.0 - sourceAlpha)) / outAlpha;
                assertEquals(expected, (result >>> shift) & 0xFF, 0.5 + 1e-9);
            }
        }
        assertEquals(0x12345678, ApngCompositor.blendOver(abgr(9, 9, 9, 0), 0x12345678));
        assertEquals(abgr(1, 2, 3, 255), ApngCompositor.blendOver(abgr(1, 2, 3, 255), abgr(9, 9, 9, 9)));
        assertEquals(abgr(1, 2, 3, 77), ApngCompositor.blendOver(abgr(1, 2, 3, 77), CLEAR));
    }

    @Test
    void everyPlayStartsOnATransparentCanvas() throws IOException {
        // The first frame does not cover the canvas, so the second play must not see the pixels of the first play
        byte[] file = rgba().animationControl(2, 0).imageData(solid(4, 4, 255, 0, 0, 255))
                .frame(2, 2, 0, 0, 1, 10, 0, ApngFrame.BLEND_OP_OVER, solid(2, 2, 0, 255, 0, 255))
                .frame(2, 2, 2, 2, 1, 10, 0, 0, solid(2, 2, 0, 0, 255, 255))
                .end();
        ApngImage image = read(file);
        ApngCompositor compositor = new ApngCompositor(image);
        try (ApngFrameDecoder decoder = new ApngFrameDecoder(image)) {
            compositor.composeFrame(0, decoder, new ApngFrameUpdate(0, 0, 0L));
            compositor.composeFrame(1, decoder, new ApngFrameUpdate(1, 0, 0L));
            ApngFrameUpdate secondPlay = new ApngFrameUpdate(0, 1, 0L);
            compositor.composeFrame(0, decoder, secondPlay);

            int[] canvas = ApngFrameDecoderTest.readCanvas(compositor, 4, 4);
            assertEquals(GREEN, canvas[0]);
            assertEquals(CLEAR, canvas[3 * 4 + 3]);
            assertEquals(1, secondPlay.getRectangleCount());
            assertEquals(16, secondPlay.getPixelCount());
        }
    }

    @Test
    void updatesOnlyCoverWhatChanged() throws IOException {
        ApngImage image = read(animation(ApngFrame.DISPOSE_OP_BACKGROUND, ApngFrame.BLEND_OP_SOURCE));
        ApngCompositor compositor = new ApngCompositor(image);
        try (ApngFrameDecoder decoder = new ApngFrameDecoder(image)) {
            ApngFrameUpdate first = new ApngFrameUpdate(0, 0, 0L);
            compositor.composeFrame(0, decoder, first);
            ApngFrameUpdate second = new ApngFrameUpdate(1, 0, 0L);
            compositor.composeFrame(1, decoder, second);
            ApngFrameUpdate third = new ApngFrameUpdate(2, 0, 0L);
            compositor.composeFrame(2, decoder, third);

            assertEquals(16, first.getPixelCount());
            // Only the green frame region
            assertEquals(1, second.getRectangleCount());
            assertEquals(4, second.getPixelCount());
            // The cleared green region and the blue pixel are small, so they get merged
            assertEquals(1, third.getRectangleCount());
            assertEquals(1, third.getRectangleX(0));
            assertEquals(9, third.getPixelCount());
        }
    }

    @Test
    void restoringTheFirstFrameContinuesExactlyLikeDecodingIt() throws IOException {
        byte[] file = randomAnimation(new Random(11), 12, 10, 8);
        ApngImage image = read(file);
        int[][] expected = composeAll(file);
        ApngCompositor compositor = new ApngCompositor(image);
        try (ApngFrameDecoder decoder = new ApngFrameDecoder(image)) {
            ApngFrameUpdate first = new ApngFrameUpdate(0, 0, 0L);
            compositor.composeFrame(0, decoder, first);
            ByteBuffer snapshot = ByteBuffer.allocate(12 * 10 * 4);
            compositor.writePixels(first, snapshot);
            compositor.composeFrame(1, decoder, new ApngFrameUpdate(1, 0, 0L));

            ApngCompositor restored = new ApngCompositor(image);
            restored.restoreFirstFrame(snapshot);
            for (int i = 1; i < image.getFrameCount(); i++) {
                restored.composeFrame(i, decoder, new ApngFrameUpdate(i, 0, 0L));
                assertArrayEquals(expected[i], ApngFrameDecoderTest.readCanvas(restored, 12, 10), "frame " + i);
            }
        }
    }

    @Test
    void interlacedFramesComposeLikeNonInterlacedOnes() throws IOException {
        Random random = new Random(99);
        for (int round = 0; round < 20; round++) {
            long seed = random.nextLong();
            int[][] plain = composeAll(randomAnimation(new Random(seed), 13, 11, 6, false));
            int[][] interlaced = composeAll(randomAnimation(new Random(seed), 13, 11, 6, true));
            for (int frame = 0; frame < plain.length; frame++) {
                assertArrayEquals(plain[frame], interlaced[frame], "round " + round + ", frame " + frame);
            }
        }
    }

    @Test
    void releasedCanvasStartsTheNextPlayFromScratch() throws IOException {
        ApngImage image = read(animation(ApngFrame.DISPOSE_OP_NONE, ApngFrame.BLEND_OP_SOURCE));
        ApngCompositor compositor = new ApngCompositor(image);
        try (ApngFrameDecoder decoder = new ApngFrameDecoder(image)) {
            compositor.composeFrame(0, decoder, new ApngFrameUpdate(0, 0, 0L));
            compositor.release();
            compositor.composeFrame(0, decoder, new ApngFrameUpdate(0, 1, 0L));

            int[] canvas = ApngFrameDecoderTest.readCanvas(compositor, 4, 4);
            for (int pixel : canvas) assertEquals(RED, pixel);
        }
    }

    /**
     * A red 4x4 default image as first frame, then a green 2x2 frame at 1,1 with the given ops, then a blue pixel at 3,3.
     */
    private static byte[] animation(int greenDisposeOp, int greenBlendOp) {
        return rgba().animationControl(3, 0)
                .frameControl(4, 4, 0, 0, 1, 10, 0, 0).imageData(solid(4, 4, 255, 0, 0, 255))
                .frame(2, 2, 1, 1, 1, 10, greenDisposeOp, greenBlendOp, solid(2, 2, 0, 255, 0, 255))
                .frame(1, 1, 3, 3, 1, 10, 0, 0, solid(1, 1, 0, 0, 255, 255))
                .end();
    }

    static byte[] randomAnimation(Random random, int width, int height, int frames) {
        return randomAnimation(random, width, height, frames, false);
    }

    static byte[] randomAnimation(Random random, int width, int height, int frames, boolean interlaced) {
        ApngTestFiles.Png png = ApngTestFiles.png(width, height, 8, ApngImage.COLOR_TYPE_RGBA).interlaced(interlaced).header().animationControl(frames, 0);
        png.frameControl(width, height, 0, 0, 1, 20, random.nextInt(3), random.nextInt(2)).imageData(randomPixels(random, width, height));
        for (int i = 1; i < frames; i++) {
            int frameWidth = 1 + random.nextInt(width);
            int frameHeight = 1 + random.nextInt(height);
            int x = random.nextInt(width - frameWidth + 1);
            int y = random.nextInt(height - frameHeight + 1);
            png.frame(frameWidth, frameHeight, x, y, 1, 20, random.nextInt(3), random.nextInt(2), randomPixels(random, frameWidth, frameHeight));
        }
        return png.end();
    }

    private static int[] randomPixels(Random random, int width, int height) {
        int[] samples = new int[width * height * 4];
        for (int i = 0; i < width * height; i++) {
            samples[i * 4] = random.nextInt(256);
            samples[i * 4 + 1] = random.nextInt(256);
            samples[i * 4 + 2] = random.nextInt(256);
            // Mostly opaque or clear pixels, like real animations, plus some in between
            int alphaKind = random.nextInt(4);
            samples[i * 4 + 3] = (alphaKind == 0) ? 0 : ((alphaKind == 1) ? random.nextInt(256) : 255);
        }
        return samples;
    }

    static int[][] composeAll(byte[] file) throws IOException {
        return composeAll(read(file));
    }

    /**
     * Composes every frame of the first play and returns the canvas after each frame.
     */
    static int[][] composeAll(ApngImage image) throws IOException {
        ApngCompositor compositor = new ApngCompositor(image);
        int[][] canvases = new int[image.getFrameCount()][];
        try (ApngFrameDecoder decoder = new ApngFrameDecoder(image)) {
            for (int i = 0; i < image.getFrameCount(); i++) {
                ApngFrameUpdate update = new ApngFrameUpdate(i, 0, 0L);
                compositor.composeFrame(i, decoder, update);
                assertTrue(update.getPixelCount() > 0);
                canvases[i] = ApngFrameDecoderTest.readCanvas(compositor, image.getWidth(), image.getHeight());
            }
        }
        return canvases;
    }

    private static ApngTestFiles.Png rgba() {
        return ApngTestFiles.png(4, 4, 8, ApngImage.COLOR_TYPE_RGBA).header();
    }

    private static ApngImage read(byte[] file) throws IOException {
        return ApngTestFiles.read(file);
    }

}
