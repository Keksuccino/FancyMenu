package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageTestSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.util.Random;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Composes complete GIF animations with the real compositor and checks the canvas after every frame.
 */
class GifCompositionTest {

    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    private static final int WHITE = 3;
    private static final int RED_PIXEL = GifTestFiles.PALETTE_PIXELS[RED];
    private static final int GREEN_PIXEL = GifTestFiles.PALETTE_PIXELS[GREEN];
    private static final int BLUE_PIXEL = GifTestFiles.PALETTE_PIXELS[BLUE];
    private static final int WHITE_PIXEL = GifTestFiles.PALETTE_PIXELS[WHITE];

    static LongStream seeds() {
        return LongStream.range(0, 120);
    }

    @Test
    void disposalNoneKeepsTheFrame() throws IOException {
        int[][] canvases = compose(GifTestFiles.paletteGif(3, 1).image(GifTestFiles.solidPaletteFrame(3, 1, BLUE)).graphicControl(1, 0, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(1, 1, GREEN).at(1, 0)).image(GifTestFiles.solidPaletteFrame(1, 1, WHITE)).end());

        assertArrayEquals(new int[] {WHITE_PIXEL, GREEN_PIXEL, BLUE_PIXEL}, canvases[2]);
    }

    @Test
    void disposalBackgroundClearsToTransparentInsteadOfTheBackgroundColor() throws IOException {
        // The background color index of the logical screen is 0 (red), which browsers never use for disposal
        int[][] canvases = compose(GifTestFiles.paletteGif(4, 1).image(GifTestFiles.solidPaletteFrame(4, 1, BLUE)).graphicControl(2, 0, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 1, GREEN).at(1, 0)).image(GifTestFiles.solidPaletteFrame(1, 1, WHITE)).end());

        assertArrayEquals(new int[] {BLUE_PIXEL, GREEN_PIXEL, GREEN_PIXEL, BLUE_PIXEL}, canvases[1]);
        assertArrayEquals(new int[] {WHITE_PIXEL, 0, 0, BLUE_PIXEL}, canvases[2]);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4})
    void disposalPreviousRevertsTheRegion(int disposal) throws IOException {
        byte[] file = GifTestFiles.paletteGif(4, 1).image(GifTestFiles.solidPaletteFrame(4, 1, BLUE)).image(GifTestFiles.solidPaletteFrame(2, 1, WHITE).at(2, 0))
                .graphicControl(disposal, 0, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(3, 1, GREEN).at(1, 0))
                .image(GifTestFiles.solidPaletteFrame(1, 1, RED)).end();

        int[][] canvases = compose(file);

        assertArrayEquals(new int[] {BLUE_PIXEL, GREEN_PIXEL, GREEN_PIXEL, GREEN_PIXEL}, canvases[2]);
        assertArrayEquals(new int[] {RED_PIXEL, BLUE_PIXEL, WHITE_PIXEL, WHITE_PIXEL}, canvases[3]);
    }

    @Test
    void revertingTheFirstFrameClearsIt() throws IOException {
        int[][] canvases = compose(GifTestFiles.paletteGif(3, 1).graphicControl(3, 0, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(3, 1, BLUE)).image(GifTestFiles.solidPaletteFrame(1, 1, GREEN)).end());

        assertArrayEquals(new int[] {GREEN_PIXEL, 0, 0}, canvases[1]);
    }

    @Test
    void transparentPixelsKeepTheCanvas() throws IOException {
        GifTestFiles.Frame holes = new GifTestFiles.Frame(4, 1, new int[] {WHITE, GREEN, WHITE, RED});
        int[][] canvases = compose(GifTestFiles.paletteGif(4, 1).image(GifTestFiles.solidPaletteFrame(4, 1, BLUE)).graphicControl(0, 0, WHITE).image(holes).end());

        assertArrayEquals(new int[] {BLUE_PIXEL, GREEN_PIXEL, BLUE_PIXEL, RED_PIXEL}, canvases[1]);
    }

    @Test
    void transparentFirstFrameShowsTransparentPixels() throws IOException {
        int[][] canvases = compose(GifTestFiles.paletteGif(3, 1).graphicControl(0, 0, GREEN).image(new GifTestFiles.Frame(3, 1, new int[] {RED, GREEN, BLUE})).end());

        assertArrayEquals(new int[] {RED_PIXEL, 0, BLUE_PIXEL}, canvases[0]);
    }

    @Test
    void framesThatReachPastTheCanvasGetClipped() throws IOException {
        GifTestFiles.Frame big = new GifTestFiles.Frame(3, 3, new int[] {RED, GREEN, BLUE, WHITE, RED, GREEN, BLUE, WHITE, RED}).at(2, 1);
        int[][] canvases = compose(GifTestFiles.paletteGif(3, 3).image(GifTestFiles.solidPaletteFrame(3, 3, WHITE)).graphicControl(2, 0, GifTestFiles.NO_TRANSPARENCY).image(big).image(GifTestFiles.solidPaletteFrame(1, 1, BLUE)).end());

        assertArrayEquals(new int[] {WHITE_PIXEL, WHITE_PIXEL, WHITE_PIXEL, WHITE_PIXEL, WHITE_PIXEL, RED_PIXEL, WHITE_PIXEL, WHITE_PIXEL, WHITE_PIXEL}, canvases[1]);
        assertArrayEquals(new int[] {BLUE_PIXEL, WHITE_PIXEL, WHITE_PIXEL, WHITE_PIXEL, WHITE_PIXEL, 0, WHITE_PIXEL, WHITE_PIXEL, 0}, canvases[2]);
    }

    @Test
    void framesOutsideTheCanvasKeepItAsItIs() throws IOException {
        int[][] canvases = compose(GifTestFiles.paletteGif(2, 1).image(GifTestFiles.solidPaletteFrame(2, 1, GREEN)).graphicControl(2, 0, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 1, RED).at(5, 0)).image(GifTestFiles.solidPaletteFrame(1, 1, BLUE)).end());

        assertArrayEquals(new int[] {GREEN_PIXEL, GREEN_PIXEL}, canvases[1]);
        assertArrayEquals(new int[] {BLUE_PIXEL, GREEN_PIXEL}, canvases[2]);
    }

    /**
     * A first frame with zero width or height inside the logical screen leaves the logical screen as canvas. It shows the canvas fully transparent,
     * and reverting its empty region (which clears it for the first frame) doesn't change the frames after it.
     */
    @ParameterizedTest
    @CsvSource({"0, 0, 0, 0", "0, 3, 2, 0", "4, 0, 0, 1", "0, 0, 3, 2"})
    void firstFrameWithoutVisiblePixelsShowsATransparentCanvas(int width, int height, int x, int y) throws IOException {
        byte[] file = GifTestFiles.paletteGif(4, 3).graphicControl(3, 0, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(width, height, RED).at(x, y))
                .graphicControl(2, 0, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 2, GREEN).at(1, 1))
                .image(GifTestFiles.solidPaletteFrame(1, 1, BLUE)).end();
        GifImage image = GifTestFiles.read(file);

        int[][] canvases = AnimatedImageTestSupport.composeAll(image);

        assertEquals(4, image.getWidth());
        assertEquals(3, image.getHeight());
        assertFalse(image.getFrame(0).hasVisiblePixels());
        assertArrayEquals(new int[4 * 3], canvases[0]);
        assertArrayEquals(new int[] {0, 0, 0, 0, 0, GREEN_PIXEL, GREEN_PIXEL, 0, 0, GREEN_PIXEL, GREEN_PIXEL, 0}, canvases[1]);
        assertArrayEquals(new int[] {BLUE_PIXEL, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, canvases[2]);
    }

    /**
     * Random animations with every disposal, transparency, interlacing, local color tables and frames that reach past or lie outside the canvas,
     * compared with a straightforward reference compositor that works on whole canvases.
     */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void randomAnimationsMatchAReferenceCompositor(long seed) throws IOException {
        Random random = new Random(seed);
        int screenWidth = random.nextInt(30);
        int screenHeight = random.nextInt(30);
        int[] global = GifTestFiles.randomColorTable(16, random);
        GifTestFiles.Gif gif = GifTestFiles.gif(screenWidth, screenHeight, global);
        int frameCount = 2 + random.nextInt(6);
        int[][] indices = new int[frameCount][];
        int[][] rects = new int[frameCount][];
        int[][] tables = new int[frameCount][];
        int[] disposals = new int[frameCount];
        int[] transparent = new int[frameCount];
        for (int f = 0; f < frameCount; f++) {
            int width = ((f == 0) ? 1 : 0) + random.nextInt(25);
            int height = ((f == 0) ? 1 : 0) + random.nextInt(25);
            int x = random.nextInt(25);
            int y = random.nextInt(25);
            indices[f] = random.nextBoolean() ? GifTestFiles.randomIndices(width * height, 16, random) : GifTestFiles.runIndices(width * height, 16, 30, random);
            rects[f] = new int[] {x, y, width, height};
            disposals[f] = random.nextInt(8);
            transparent[f] = random.nextBoolean() ? random.nextInt(16) : GifTestFiles.NO_TRANSPARENCY;
            tables[f] = random.nextInt(4) == 0 ? GifTestFiles.randomColorTable(16, random) : global;
            GifTestFiles.Frame frame = new GifTestFiles.Frame(width, height, indices[f]).at(x, y).minimumCodeSize(4);
            if (random.nextInt(3) == 0) frame.interlaced();
            if (tables[f] != global) frame.localColorTable(tables[f]);
            gif.graphicControl(disposals[f], 1 + random.nextInt(20), transparent[f]).image(frame);
        }
        int canvasWidth = Math.max(screenWidth, rects[0][0] + rects[0][2]);
        int canvasHeight = Math.max(screenHeight, rects[0][1] + rects[0][3]);
        GifImage image = GifTestFiles.read(gif.end());
        assertEquals(canvasWidth, image.getWidth());
        assertEquals(canvasHeight, image.getHeight());

        int[][] canvases = AnimatedImageTestSupport.composeAll(image);

        int[] canvas = new int[canvasWidth * canvasHeight];
        int[] saved = null;
        int pendingDisposal = 0;
        int[] pendingRect = null;
        for (int f = 0; f < frameCount; f++) {
            if (f > 0) {
                for (int y = 0; y < canvasHeight; y++) {
                    for (int x = 0; x < canvasWidth; x++) {
                        if (!inside(pendingRect, x, y)) continue;
                        if (pendingDisposal == 2) canvas[y * canvasWidth + x] = 0;
                        if ((pendingDisposal == 3) || (pendingDisposal == 4)) canvas[y * canvasWidth + x] = saved[y * canvasWidth + x];
                    }
                }
            }
            int disposal = disposals[f];
            // Reverting the first frame clears it, there was nothing before it
            if ((f == 0) && ((disposal == 3) || (disposal == 4))) disposal = 2;
            if ((disposal == 3) || (disposal == 4)) saved = canvas.clone();
            int[] rect = rects[f];
            for (int y = 0; y < rect[3]; y++) {
                for (int x = 0; x < rect[2]; x++) {
                    int canvasX = rect[0] + x;
                    int canvasY = rect[1] + y;
                    int index = indices[f][y * rect[2] + x];
                    if ((canvasX >= canvasWidth) || (canvasY >= canvasHeight) || (index == transparent[f])) continue;
                    canvas[canvasY * canvasWidth + canvasX] = GifTestFiles.color(tables[f], index);
                }
            }
            assertArrayEquals(canvas, canvases[f], "frame " + f);
            pendingDisposal = disposal;
            pendingRect = rect;
        }
    }

    private static boolean inside(int[] rect, int x, int y) {
        return (x >= rect[0]) && (y >= rect[1]) && (x < rect[0] + rect[2]) && (y < rect[1] + rect[3]);
    }

    /**
     * Parses the file and composes all frames of the first play.
     */
    @NotNull
    private static int[][] compose(@NotNull byte[] file) throws IOException {
        return AnimatedImageTestSupport.composeAll(GifTestFiles.read(file));
    }

}
