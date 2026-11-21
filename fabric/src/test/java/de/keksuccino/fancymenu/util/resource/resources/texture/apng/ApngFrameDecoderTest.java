package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApngFrameDecoderTest {

    private static final int[][] FORMATS = {
            {ApngImage.COLOR_TYPE_GRAYSCALE, 1}, {ApngImage.COLOR_TYPE_GRAYSCALE, 2}, {ApngImage.COLOR_TYPE_GRAYSCALE, 4}, {ApngImage.COLOR_TYPE_GRAYSCALE, 8}, {ApngImage.COLOR_TYPE_GRAYSCALE, 16},
            {ApngImage.COLOR_TYPE_RGB, 8}, {ApngImage.COLOR_TYPE_RGB, 16},
            {ApngImage.COLOR_TYPE_PALETTE, 1}, {ApngImage.COLOR_TYPE_PALETTE, 2}, {ApngImage.COLOR_TYPE_PALETTE, 4}, {ApngImage.COLOR_TYPE_PALETTE, 8},
            {ApngImage.COLOR_TYPE_GRAYSCALE_ALPHA, 8}, {ApngImage.COLOR_TYPE_GRAYSCALE_ALPHA, 16},
            {ApngImage.COLOR_TYPE_RGBA, 8}, {ApngImage.COLOR_TYPE_RGBA, 16}
    };
    private static final int[][] SIZES = {{1, 1}, {3, 5}, {17, 9}, {33, 2}, {2, 33}};

    static Stream<Arguments> everyFormat() {
        List<Arguments> arguments = new ArrayList<>();
        for (int[] format : FORMATS) {
            for (int[] size : SIZES) {
                arguments.add(Arguments.of(format[0], format[1], size[0], size[1], false));
                arguments.add(Arguments.of(format[0], format[1], size[0], size[1], true));
            }
        }
        return arguments.stream();
    }

    @ParameterizedTest(name = "color type {0}, {1} bit, {2}x{3}, interlaced {4}")
    @MethodSource("everyFormat")
    void decodesEveryColorTypeBitDepthAndInterlacing(int colorType, int bitDepth, int width, int height, boolean interlaced) throws IOException {
        Random random = new Random(colorType * 1000L + bitDepth * 100L + width * 10L + height + (interlaced ? 7L : 0L));
        int[] samples = ApngTestFiles.randomSamples(width, height, bitDepth, colorType, random);
        int[] palette = randomPalette(random);
        ApngTestFiles.Png png = ApngTestFiles.png(width, height, bitDepth, colorType).interlaced(interlaced).header();
        if (colorType == ApngImage.COLOR_TYPE_PALETTE) png.palette(palette);
        byte[] file = png.imageData(samples).end();

        // ImageIO is an independent decoder, it proves that the test file holds exactly these samples
        assertArrayEquals(samples, readSamplesWithImageIO(file, colorType));
        int[] expected = new int[width * height];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = expectedPixel(colorType, bitDepth, samples, i, palette);
        }
        assertArrayEquals(expected, decodeFirstFrame(file));
    }

    @Test
    void decodesEveryFilterTypeOnItsOwn() throws IOException {
        for (int filter = 0; filter <= 4; filter++) {
            for (int[] format : new int[][] {{ApngImage.COLOR_TYPE_RGB, 8}, {ApngImage.COLOR_TYPE_RGBA, 8}, {ApngImage.COLOR_TYPE_GRAYSCALE, 8}, {ApngImage.COLOR_TYPE_RGBA, 16}, {ApngImage.COLOR_TYPE_GRAYSCALE_ALPHA, 16}}) {
                Random random = new Random(filter);
                int[] samples = ApngTestFiles.randomSamples(23, 7, format[1], format[0], random);
                byte[] file = ApngTestFiles.png(23, 7, format[1], format[0]).filter(filter).header().imageData(samples).end();
                int[] decoded = decodeFirstFrame(file);
                for (int i = 0; i < decoded.length; i++) {
                    assertEquals(expectedPixel(format[0], format[1], samples, i, null), decoded[i], "filter " + filter + ", color type " + format[0] + ", pixel " + i);
                }
            }
        }
    }

    @Test
    void transparentGraySampleOnlyHidesExactMatches() throws IOException {
        int[] samples = {0, 1000, 1001, 65535};
        byte[] file = ApngTestFiles.png(4, 1, 16, ApngImage.COLOR_TYPE_GRAYSCALE).header().transparency(1000).imageData(samples).end();

        int[] decoded = decodeFirstFrame(file);

        assertEquals(0xFF000000, decoded[0]);
        assertEquals(0, decoded[1] >>> 24);
        // 1001 and 1000 become the same 8-bit value, but only the exact 16-bit sample is transparent
        assertEquals(0xFF, decoded[2] >>> 24);
        assertEquals(0xFFFFFFFF, decoded[3]);
    }

    @Test
    void transparentSampleOnlyUsesTheBitsOfTheImage() throws IOException {
        // Out of range for 8 bits, which breaks the specification, libpng and browsers only use the low byte
        byte[] file = ApngTestFiles.png(2, 1, 8, ApngImage.COLOR_TYPE_GRAYSCALE).header().transparency(0x0105).imageData(new int[] {5, 6}).end();

        int[] decoded = decodeFirstFrame(file);

        assertEquals(0, decoded[0] >>> 24);
        assertEquals(0xFF, decoded[1] >>> 24);
    }

    @Test
    void transparentRgbColorOnlyHidesExactMatches() throws IOException {
        int[] samples = {10, 20, 30, 10, 20, 31, 0, 0, 0};
        byte[] file = ApngTestFiles.png(3, 1, 8, ApngImage.COLOR_TYPE_RGB).header().transparency(10, 20, 30).imageData(samples).end();

        int[] decoded = decodeFirstFrame(file);

        assertEquals(ApngTestFiles.abgr(10, 20, 30, 0), decoded[0]);
        assertEquals(ApngTestFiles.abgr(10, 20, 31, 255), decoded[1]);
        assertEquals(ApngTestFiles.abgr(0, 0, 0, 255), decoded[2]);
    }

    @Test
    void paletteTransparencyCoversOnlyItsEntriesAndMissingEntriesAreOpaqueBlack() throws IOException {
        int[] samples = {0, 1, 2, 3};
        // Two palette entries, only the first has a tRNS alpha; index 2 and 3 are outside the palette
        byte[] file = ApngTestFiles.png(4, 1, 8, ApngImage.COLOR_TYPE_PALETTE).header().palette(new int[] {255, 0, 0, 0, 255, 0}).transparency(128).imageData(samples).end();

        int[] decoded = decodeFirstFrame(file);

        assertEquals(ApngTestFiles.abgr(255, 0, 0, 128), decoded[0]);
        assertEquals(ApngTestFiles.abgr(0, 255, 0, 255), decoded[1]);
        assertEquals(0xFF000000, decoded[2]);
        assertEquals(0xFF000000, decoded[3]);
    }

    @Test
    void sixteenBitSamplesRoundToTheNearestEightBitValue() {
        for (int value = 0; value <= 255; value++) {
            assertEquals(value, ApngFrameDecoder.scale16(value * 257));
        }
        for (int sample = 0; sample <= 65535; sample++) {
            assertEquals(Math.round(sample * 255.0 / 65535.0), ApngFrameDecoder.scale16(sample), "sample " + sample);
        }
    }

    @Test
    void truncatedFrameDataFails() throws IOException {
        int[] samples = ApngTestFiles.randomSamples(16, 16, 8, ApngImage.COLOR_TYPE_RGBA, new Random(5));
        byte[] zlib = ApngTestFiles.encodeImageData(16, 16, 8, ApngImage.COLOR_TYPE_RGBA, samples, false, ApngTestFiles.RANDOM_FILTERS, new Random(5));
        byte[] file = ApngTestFiles.png(16, 16, 8, ApngImage.COLOR_TYPE_RGBA).header().chunk("IDAT", Arrays.copyOf(zlib, zlib.length / 2)).end();

        assertThrows(IOException.class, () -> decodeFirstFrame(file));
    }

    @Test
    void invalidFilterTypeFails() throws IOException {
        byte[] file = ApngTestFiles.png(2, 1, 8, ApngImage.COLOR_TYPE_GRAYSCALE).header().chunk("IDAT", ApngTestFiles.deflate(new byte[] {5, 1, 2})).end();

        assertThrows(IOException.class, () -> decodeFirstFrame(file));
    }

    static int[] decodeFirstFrame(byte[] file) throws IOException {
        ApngImage image = ApngReader.read(new ByteArrayInputStream(file), "test.png");
        ApngCompositor compositor = new ApngCompositor(image);
        try (ApngFrameDecoder decoder = new ApngFrameDecoder(image)) {
            ApngFrameUpdate update = new ApngFrameUpdate(0, 0, 0L);
            compositor.composeFrame(0, decoder, update);
            return readCanvas(compositor, image.getWidth(), image.getHeight());
        }
    }

    static int[] readCanvas(ApngCompositor compositor, int width, int height) {
        ApngFrameUpdate fullCanvas = new ApngFrameUpdate(0, 0, 0L);
        fullCanvas.setFullCanvas(width, height);
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        compositor.writePixels(fullCanvas, buffer);
        int[] pixels = new int[width * height];
        buffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels);
        return pixels;
    }

    private static int expectedPixel(int colorType, int bitDepth, int[] samples, int pixel, int[] palette) {
        int channels = ApngTestFiles.channels(colorType);
        int base = pixel * channels;
        if (colorType == ApngImage.COLOR_TYPE_PALETTE) {
            int index = samples[base];
            return ApngTestFiles.abgr(palette[index * 3], palette[index * 3 + 1], palette[index * 3 + 2], 255);
        }
        int[] values = new int[channels];
        for (int channel = 0; channel < channels; channel++) {
            int sample = samples[base + channel];
            values[channel] = (bitDepth == 16) ? (int) Math.round(sample * 255.0 / 65535.0) : (sample * 255) / ((1 << bitDepth) - 1);
        }
        return switch (colorType) {
            case ApngImage.COLOR_TYPE_GRAYSCALE -> ApngTestFiles.abgr(values[0], values[0], values[0], 255);
            case ApngImage.COLOR_TYPE_GRAYSCALE_ALPHA -> ApngTestFiles.abgr(values[0], values[0], values[0], values[1]);
            case ApngImage.COLOR_TYPE_RGB -> ApngTestFiles.abgr(values[0], values[1], values[2], 255);
            default -> ApngTestFiles.abgr(values[0], values[1], values[2], values[3]);
        };
    }

    private static int[] randomPalette(Random random) {
        int[] palette = new int[256 * 3];
        for (int i = 0; i < palette.length; i++) palette[i] = random.nextInt(256);
        return palette;
    }

    private static int[] readSamplesWithImageIO(byte[] file, int colorType) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file));
        Raster raster = image.getRaster();
        int channels = ApngTestFiles.channels(colorType);
        int[] samples = new int[image.getWidth() * image.getHeight() * channels];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                for (int channel = 0; channel < channels; channel++) {
                    samples[(y * image.getWidth() + x) * channels + channel] = raster.getSample(x, y, channel);
                }
            }
        }
        return samples;
    }

}
