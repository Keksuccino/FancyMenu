package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageFrame;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageTestSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GifReaderTest {

    @ParameterizedTest
    @ValueSource(strings = {"GIF87a", "GIF89a"})
    void acceptsBothVersions(String version) throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.gif(version, 3, 2, GifTestFiles.PALETTE).image(GifTestFiles.solidPaletteFrame(3, 2, 1)).end());

        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(1, image.getFrameCount());
        assertFalse(image.isAnimated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GIF88a", "GIF89b", "gif89a", "GIF8 a", "XIF89a"})
    void rejectsOtherSignatures(String version) {
        byte[] file = GifTestFiles.gif(version, 3, 2, GifTestFiles.PALETTE).image(GifTestFiles.solidPaletteFrame(3, 2, 1)).end();

        assertThrows(IOException.class, () -> GifTestFiles.read(file));
    }

    @Test
    void rejectsFilesThatEndInTheHeader() {
        byte[] file = GifTestFiles.paletteGif(3, 2).image(GifTestFiles.solidPaletteFrame(3, 2, 1)).end();

        assertThrows(IOException.class, () -> GifTestFiles.read(Arrays.copyOf(file, 12)));
        // The global color table is cut off
        assertThrows(IOException.class, () -> GifTestFiles.read(Arrays.copyOf(file, 20)));
    }

    @Test
    void imagesWithoutFramesFail() {
        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(4, 4).end()));
        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(4, 4).comment("no frames").bytes()));
    }

    @Test
    void canvasGrowsToFitTheFirstFrame() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(4, 4).image(GifTestFiles.solidPaletteFrame(3, 3, 1).at(2, 3)).end());

        assertEquals(5, image.getWidth());
        assertEquals(6, image.getHeight());
        GifFrame frame = image.getFrame(0);
        assertEquals(2, frame.x());
        assertEquals(3, frame.y());
        assertEquals(3, frame.width());
        assertEquals(3, frame.height());
    }

    @Test
    void canvasKeepsTheLogicalScreenIfTheFirstFrameIsSmaller() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(9, 7).image(GifTestFiles.solidPaletteFrame(2, 2, 1).at(1, 1)).end());

        assertEquals(9, image.getWidth());
        assertEquals(7, image.getHeight());
    }

    @Test
    void zeroLogicalScreenTakesTheSizeOfTheFirstFrame() throws IOException {
        GifImage atOrigin = GifTestFiles.read(GifTestFiles.paletteGif(0, 0).image(GifTestFiles.solidPaletteFrame(5, 7, 1)).end());
        GifImage withOffset = GifTestFiles.read(GifTestFiles.paletteGif(0, 0).image(GifTestFiles.solidPaletteFrame(5, 7, 1).at(2, 1)).end());

        assertEquals(5, atOrigin.getWidth());
        assertEquals(7, atOrigin.getHeight());
        assertEquals(7, withOffset.getWidth());
        assertEquals(8, withOffset.getHeight());
    }

    @Test
    void emptyCanvasFails() {
        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(0, 0).image(GifTestFiles.solidPaletteFrame(0, 0, 1)).end()));
        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(0, 5).image(GifTestFiles.solidPaletteFrame(0, 3, 1)).end()));
    }

    @Test
    void tooBigCanvasFails() throws IOException {
        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(AnimatedImage.MAX_DIMENSION + 1, 1).image(GifTestFiles.solidPaletteFrame(1, 1, 1)).end()));
        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(1, 1).image(GifTestFiles.solidPaletteFrame(1, 1, 1).at(0, AnimatedImage.MAX_DIMENSION)).end()));
        int height = (int) (AnimatedImage.MAX_PIXEL_COUNT / AnimatedImage.MAX_DIMENSION);
        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(AnimatedImage.MAX_DIMENSION, height + 1).image(GifTestFiles.solidPaletteFrame(1, 1, 1)).end()));

        GifImage biggest = GifTestFiles.read(GifTestFiles.paletteGif(AnimatedImage.MAX_DIMENSION, height).image(GifTestFiles.solidPaletteFrame(1, 1, 1)).end());

        assertEquals(AnimatedImage.MAX_PIXEL_COUNT, (long) biggest.getWidth() * biggest.getHeight());
    }

    @Test
    void laterFramesGetClippedToTheCanvas() throws IOException {
        // The second frame is bigger than the canvas, but only the first frame grows it
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(10, 10).image(GifTestFiles.solidPaletteFrame(10, 10, 0)).image(GifTestFiles.solidPaletteFrame(6, 6, 1).at(7, 8)).end());

        assertEquals(10, image.getWidth());
        GifFrame frame = image.getFrame(1);
        assertRegion(frame, 7, 8, 3, 2);
        assertEquals(7, frame.imageX());
        assertEquals(8, frame.imageY());
        assertEquals(6, frame.imageWidth());
        assertEquals(6, frame.imageHeight());
        assertTrue(frame.hasVisiblePixels());
    }

    @Test
    void framesOutsideTheCanvasHaveAnEmptyRegionButKeepTheirTimingAndDisposal() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(10, 10).image(GifTestFiles.solidPaletteFrame(10, 10, 0))
                .graphicControl(2, 7, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 2, 1).at(10, 3))
                .graphicControl(3, 9, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 2, 1).at(300, 400))
                .end());

        assertEquals(3, image.getFrameCount());
        // Empty regions always lie at the canvas origin, no matter where the image is
        GifFrame right = image.getFrame(1);
        assertFalse(right.hasVisiblePixels());
        assertRegion(right, 0, 0, 0, 0);
        assertEquals(70_000_000L, right.delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_BACKGROUND, right.disposeOp());
        GifFrame far = image.getFrame(2);
        assertFalse(far.hasVisiblePixels());
        assertRegion(far, 0, 0, 0, 0);
        assertEquals(90_000_000L, far.delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_PREVIOUS, far.disposeOp());
    }

    @Test
    void framesWithZeroWidthOrHeightAreValid() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(4, 4).image(GifTestFiles.solidPaletteFrame(4, 4, 0)).image(GifTestFiles.solidPaletteFrame(0, 3, 1)).image(GifTestFiles.solidPaletteFrame(3, 0, 1)).image(GifTestFiles.solidPaletteFrame(1, 1, 2)).end());

        assertEquals(4, image.getFrameCount());
        assertFalse(image.getFrame(1).hasVisiblePixels());
        assertRegion(image.getFrame(1), 0, 0, 0, 0);
        assertFalse(image.getFrame(2).hasVisiblePixels());
        assertRegion(image.getFrame(2), 0, 0, 0, 0);
        assertTrue(image.getFrame(3).hasVisiblePixels());
    }

    @ParameterizedTest
    @CsvSource({"0, 100", "1, 100", "2, 20", "7, 70", "10, 100", "250, 2500", "65535, 655350"})
    void delaysAreCentisecondsAndTinyDelaysBecomeHundredMilliseconds(int centiseconds, long millis) throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).graphicControl(0, centiseconds, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        assertEquals(millis * 1_000_000L, image.getFrame(0).delayNanos());
    }

    @Test
    void framesWithoutGraphicControlShowForHundredMilliseconds() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());

        GifFrame frame = image.getFrame(1);
        assertEquals(100_000_000L, frame.delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_NONE, frame.disposeOp());
        assertEquals(AnimatedImageFrame.BLEND_OP_SOURCE, frame.blendOp());
        assertEquals(GifFrame.NO_TRANSPARENT_INDEX, frame.transparentIndex());
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 0", "2, 1", "3, 2", "4, 2", "5, 0", "6, 0", "7, 0"})
    void disposalMethodsMapLikeInBrowsers(int disposal, int disposeOp) throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).graphicControl(disposal, 5, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        assertEquals(disposeOp, image.getFrame(0).disposeOp());
    }

    @Test
    void transparentIndexMakesTheFrameBlend() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).graphicControl(0, 5, 3).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).graphicControl(0, 5, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        assertEquals(3, image.getFrame(0).transparentIndex());
        assertEquals(AnimatedImageFrame.BLEND_OP_OVER, image.getFrame(0).blendOp());
        assertEquals(AnimatedImageFrame.BLEND_OP_SOURCE, image.getFrame(1).blendOp());
    }

    @Test
    void graphicControlOnlyAppliesToTheNextImage() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).graphicControl(2, 30, 1).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());

        assertEquals(300_000_000L, image.getFrame(0).delayNanos());
        GifFrame second = image.getFrame(1);
        assertEquals(100_000_000L, second.delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_NONE, second.disposeOp());
        assertEquals(GifFrame.NO_TRANSPARENT_INDEX, second.transparentIndex());
    }

    @Test
    void laterGraphicControlBeforeTheSameImageWins() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).graphicControl(2, 30, 1).graphicControl(3, 40, GifTestFiles.NO_TRANSPARENCY).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        GifFrame frame = image.getFrame(0);
        assertEquals(400_000_000L, frame.delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_PREVIOUS, frame.disposeOp());
        assertEquals(GifFrame.NO_TRANSPARENT_INDEX, frame.transparentIndex());
    }

    @Test
    void tooShortGraphicControlGetsIgnored() throws IOException {
        GifImage alone = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).extension(0xF9, new byte[] {0x09, 50, 0}).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());
        GifImage afterValid = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).graphicControl(2, 30, GifTestFiles.NO_TRANSPARENCY).extension(0xF9, new byte[] {0x0D, 50, 0}).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());
        GifImage empty = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).extension(0xF9).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        assertEquals(100_000_000L, alone.getFrame(0).delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_NONE, alone.getFrame(0).disposeOp());
        assertEquals(300_000_000L, afterValid.getFrame(0).delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_BACKGROUND, afterValid.getFrame(0).disposeOp());
        assertEquals(100_000_000L, empty.getFrame(0).delayNanos());
    }

    @Test
    void longGraphicControlUsesItsFirstFourBytes() throws IOException {
        // Disposal 2 with transparency, 25 centiseconds, transparent index 2, then bytes and a sub-block that don't belong to the graphic control
        byte[] first = {0x09, 25, 0, 2, 0x7F, 0x7F};
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).extension(0xF9, first, new byte[] {1, 2, 3}).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        GifFrame frame = image.getFrame(0);
        assertEquals(250_000_000L, frame.delayNanos());
        assertEquals(AnimatedImageFrame.DISPOSE_OP_BACKGROUND, frame.disposeOp());
        assertEquals(2, frame.transparentIndex());
    }

    @Test
    void extensionsThatDontChangePixelsGetSkipped() throws IOException {
        byte[] xmp = new byte[255];
        Arrays.fill(xmp, (byte) 'x');
        byte[] file = GifTestFiles.paletteGif(2, 2).comment("A comment that is longer than nothing")
                .application("XMP DataXMP", xmp, xmp, new byte[] {1, 1, 0})
                .application("ICCRGBG1012", new byte[200])
                .extension(0x99, new byte[] {1, 2, 3}, new byte[0x40])
                .graphicControl(0, 20, GifTestFiles.NO_TRANSPARENCY)
                .plainText("Plain text")
                .comment("between the graphic control and its image")
                .image(GifTestFiles.solidPaletteFrame(2, 2, 1))
                .comment("after the image")
                .end();

        GifImage image = GifTestFiles.read(file);

        assertEquals(1, image.getFrameCount());
        // XMP data with a sub-block that looks like a loop count is no loop extension
        assertEquals(1, image.getNumPlays());
        // Plain text is a graphic block in the specification, but browsers ignore it, so the graphic control belongs to the image
        assertEquals(200_000_000L, image.getFrame(0).delayNanos());
    }

    @Test
    void imagesWithoutLoopExtensionPlayOnce() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());

        assertTrue(image.isAnimated());
        assertEquals(1, image.getNumPlays());
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 2", "3, 4", "65535, 65536"})
    void loopCountCountsRepetitions(int loopCount, int plays) throws IOException {
        GifImage netscape = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).netscapeLoop(loopCount).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());
        GifImage animexts = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).animextsLoop(loopCount).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());

        assertEquals(plays, netscape.getNumPlays());
        assertEquals(plays, animexts.getNumPlays());
        if (loopCount == 0) assertEquals(AnimatedImage.INFINITE_PLAYS, netscape.getNumPlays());
    }

    @Test
    void firstLoopCountWinsAndCanComeAnywhere() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).netscapeLoop(2).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).netscapeLoop(0).end());

        assertEquals(3, image.getNumPlays());
    }

    @Test
    void loopExtensionWithoutLoopSubBlockGetsSkipped() throws IOException {
        // Only the buffering sub-block (id 2) and a too short sub-block, then a real loop count in a later extension
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).application("NETSCAPE2.0", new byte[] {2, 0, 0, 1, 0}, new byte[] {1, 5})
                .image(GifTestFiles.solidPaletteFrame(2, 2, 1)).application("NETSCAPE2.0", new byte[] {2, 9, 9, 9, 9}, new byte[] {1, 4, 0}).end());

        assertEquals(5, image.getNumPlays());
    }

    @Test
    void loopSubBlockAfterBufferingSubBlockCounts() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).application("NETSCAPE2.0", new byte[] {2, 0, 16, 0, 0}, new byte[] {1, 7, 0, 0}).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        assertEquals(8, image.getNumPlays());
    }

    @Test
    void otherApplicationIdentifiersAreNoLoopExtension() throws IOException {
        GifImage wrongVersion = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).application("NETSCAPE2.1", new byte[] {1, 3, 0}).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());
        GifImage shortIdentifier = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).application("NETSCAPE", new byte[] {1, 3, 0}).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        assertEquals(1, wrongVersion.getNumPlays());
        assertEquals(1, shortIdentifier.getNumPlays());
    }

    @Test
    void parsesTheFrameDataLayout() throws IOException {
        int[] localTable = GifTestFiles.randomColorTable(8, new Random(1));
        GifTestFiles.Gif gif = GifTestFiles.paletteGif(6, 5).image(GifTestFiles.solidPaletteFrame(6, 5, 1));
        int descriptor = gif.size();
        byte[] file = gif.image(GifTestFiles.solidPaletteFrame(4, 3, 5).at(1, 2).interlaced().localColorTable(localTable).minimumCodeSize(3)).end();

        GifFrame frame = GifTestFiles.read(file).getFrame(1);

        assertTrue(frame.interlaced());
        assertEquals(8, frame.localColorTableSize());
        assertEquals(3, frame.lzwMinimumCodeSize());
        // The data starts with the local color table right after the 10 bytes of the image descriptor and ends with the block terminator right before the trailer
        assertEquals(descriptor + 10, frame.dataPosition());
        assertEquals(file.length - 1, frame.dataEnd());
        assertEquals(0, file[(int) frame.dataEnd() - 1]);
        assertEquals(3, file[(int) frame.dataPosition() + 24]);
        CRC32 crc = new CRC32();
        crc.update(file, (int) frame.dataPosition(), (int) frame.dataLength());
        assertEquals((int) crc.getValue(), frame.dataCrc());
        assertFalse(GifTestFiles.read(file).getFrame(0).interlaced());
        assertEquals(0, GifTestFiles.read(file).getFrame(0).localColorTableSize());
    }

    @Test
    void globalColorTableEntriesAfterTheTableAreOpaqueBlack() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());
        GifImage withoutTable = GifTestFiles.read(GifTestFiles.gif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        int[] palette = image.getGlobalPalette();
        assertEquals(256, palette.length);
        assertEquals(AnimatedImageTestSupport.abgr(255, 0, 0, 255), palette[0]);
        assertEquals(AnimatedImageTestSupport.abgr(255, 255, 255, 255), palette[3]);
        for (int i = 4; i < 256; i++) assertEquals(0xFF000000, palette[i], "entry " + i);
        for (int i = 0; i < 256; i++) assertEquals(0xFF000000, withoutTable.getGlobalPalette()[i], "entry " + i);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 12, 255})
    void invalidMinimumCodeSizeEndsTheAnimationBeforeThatFrame(int minimumCodeSize) throws IOException {
        GifTestFiles.Frame broken = GifTestFiles.solidPaletteFrame(2, 2, 1);
        broken.minimumCodeSizeByte = minimumCodeSize;
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).netscapeLoop(0).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).image(broken).image(GifTestFiles.solidPaletteFrame(2, 2, 3)).end());

        assertEquals(2, image.getFrameCount());
        assertEquals(AnimatedImage.INFINITE_PLAYS, image.getNumPlays());
    }

    @Test
    void invalidMinimumCodeSizeOfTheFirstFrameFails() {
        GifTestFiles.Frame broken = GifTestFiles.solidPaletteFrame(2, 2, 1);
        broken.minimumCodeSizeByte = 0;

        assertThrows(IOException.class, () -> GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(broken).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end()));
    }

    @ParameterizedTest
    @CsvSource({"65535, 65535", "16385, 4096", "4096, 16385", "65535, 1025"})
    void laterFramesOverThePixelLimitEndTheAnimationBeforeThem(int width, int height) throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).netscapeLoop(0).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(frameWithoutPixelData(width, height)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());

        assertEquals(1, image.getFrameCount());
        assertEquals(AnimatedImage.INFINITE_PLAYS, image.getNumPlays());
    }

    @ParameterizedTest
    @CsvSource({"16384, 4096", "2048, 32768", "65535, 1024"})
    void laterFramesUpToThePixelLimitGetParsed(int width, int height) throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(frameWithoutPixelData(width, height)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());

        assertEquals(3, image.getFrameCount());
        GifFrame big = image.getFrame(1);
        assertEquals(width, big.imageWidth());
        assertEquals(height, big.imageHeight());
        assertRegion(big, 0, 0, 2, 2);
    }

    @Test
    void truncatedFileKeepsAllCompleteFrames() throws IOException {
        GifTestFiles.Gif gif = GifTestFiles.paletteGif(16, 16).netscapeLoop(0).image(randomFrame(16, 16, 1)).graphicControl(0, 5, GifTestFiles.NO_TRANSPARENCY);
        int secondStart = gif.size();
        gif.image(randomFrame(16, 16, 2));
        int secondEnd = gif.size();
        byte[] file = gif.comment("after the frames").end();

        // Cut in the middle of the data of the second frame, right before its terminator, and right after its terminator
        assertEquals(1, GifTestFiles.read(Arrays.copyOf(file, (secondStart + secondEnd) / 2)).getFrameCount());
        assertEquals(1, GifTestFiles.read(Arrays.copyOf(file, secondEnd - 1)).getFrameCount());
        assertEquals(2, GifTestFiles.read(Arrays.copyOf(file, secondEnd)).getFrameCount());
        // Cut in the comment after the last frame
        assertEquals(2, GifTestFiles.read(Arrays.copyOf(file, secondEnd + 4)).getFrameCount());
    }

    @Test
    void truncatedFirstFrameFails() {
        byte[] file = GifTestFiles.paletteGif(16, 16).image(randomFrame(16, 16, 1)).end();

        assertThrows(IOException.class, () -> GifTestFiles.read(Arrays.copyOf(file, file.length - 2)));
        assertThrows(IOException.class, () -> GifTestFiles.read(Arrays.copyOf(file, 13 + 12 + 5)));
    }

    @Test
    void missingTrailerIsFine() throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).bytes());

        assertEquals(2, image.getFrameCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x42, 0xFF, 0x2D})
    void unknownBlockEndsTheFileLikeATrailer(int introducer) throws IOException {
        GifImage image = GifTestFiles.read(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).raw(introducer).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).end());

        assertEquals(1, image.getFrameCount());
    }

    @Test
    void bytesAfterTheTrailerGetIgnored() throws IOException {
        byte[] file = GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).raw(0x3B).image(GifTestFiles.solidPaletteFrame(2, 2, 2)).raw("garbage".getBytes(StandardCharsets.US_ASCII)).end();

        assertEquals(1, GifTestFiles.read(file).getFrameCount());
    }

    @Test
    void readsTheSourceOnceFromStartToEnd() throws IOException {
        byte[] file = GifTestFiles.paletteGif(200, 200).image(randomFrame(200, 200, 3)).image(randomFrame(200, 200, 4)).end();
        assertTrue(file.length > 64 * 1024);
        AnimatedImageTestSupport.MemorySource source = new AnimatedImageTestSupport.MemorySource(file);

        GifImage image = GifReader.read(source, "test.gif");

        assertEquals(2, image.getFrameCount());
        long next = 0L;
        for (long[] read : source.reads) {
            assertEquals(next, read[0]);
            next = read[0] + read[1];
        }
        assertEquals(file.length, next);
    }

    @Test
    void closesTheSourceOnlyWhenReadingFailsOrTheImageGetsClosed() throws IOException {
        AnimatedImageTestSupport.MemorySource broken = new AnimatedImageTestSupport.MemorySource(new byte[] {'G', 'I', 'F'});
        AnimatedImageTestSupport.MemorySource valid = new AnimatedImageTestSupport.MemorySource(GifTestFiles.paletteGif(2, 2).image(GifTestFiles.solidPaletteFrame(2, 2, 1)).end());

        assertThrows(IOException.class, () -> GifReader.read(broken, "test.gif"));
        GifImage image = GifReader.read(valid, "test.gif");

        assertTrue(broken.isClosed());
        assertFalse(valid.isClosed());
        image.close();
        assertTrue(valid.isClosed());
    }

    /**
     * Checks the canvas region of a frame.
     */
    private static void assertRegion(@NotNull GifFrame frame, int x, int y, int width, int height) {
        assertEquals(x, frame.x(), "x");
        assertEquals(y, frame.y(), "y");
        assertEquals(width, frame.width(), "width");
        assertEquals(height, frame.height(), "height");
    }

    /**
     * A frame whose LZW data is only a clear code and an end code, so even frames far bigger than any canvas need just a few bytes.
     */
    @NotNull
    private static GifTestFiles.Frame frameWithoutPixelData(int width, int height) {
        return new GifTestFiles.Frame(width, height, new int[0]).rawLzw(GifTestFiles.packCodes(256, 9, 257, 9));
    }

    /**
     * A frame of random indices for the whole 256-entry color table.
     */
    @NotNull
    private static GifTestFiles.Frame randomFrame(int width, int height, long seed) {
        return new GifTestFiles.Frame(width, height, GifTestFiles.randomIndices(width * height, 256, new Random(seed)));
    }

}
