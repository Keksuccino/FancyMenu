package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Writes PNG and APNG files for tests, with full control over chunks, scanline filters, interlacing and chunk splitting.
 */
final class ApngTestFiles {

    static final int[] ADAM7_X_START = {0, 4, 0, 2, 0, 1, 0};
    static final int[] ADAM7_Y_START = {0, 0, 4, 0, 2, 0, 1};
    static final int[] ADAM7_X_STEP = {8, 8, 4, 4, 2, 2, 1};
    static final int[] ADAM7_Y_STEP = {8, 8, 8, 4, 4, 2, 2};
    /** Picks a random filter type for every row. **/
    static final int RANDOM_FILTERS = -1;

    private ApngTestFiles() {
    }

    static int channels(int colorType) {
        return switch (colorType) {
            case ApngImage.COLOR_TYPE_RGB -> 3;
            case ApngImage.COLOR_TYPE_GRAYSCALE_ALPHA -> 2;
            case ApngImage.COLOR_TYPE_RGBA -> 4;
            default -> 1;
        };
    }

    /**
     * Encodes raw samples (channels interleaved, row by row) to a zlib stream of filtered scanlines.
     */
    static byte[] encodeImageData(int width, int height, int bitDepth, int colorType, int[] samples, boolean interlaced, int filterType, Random random) {
        int channels = channels(colorType);
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        if (!interlaced) {
            writePass(raw, width, height, 0, 0, 1, 1, width, bitDepth, channels, samples, filterType, random);
        } else {
            for (int pass = 0; pass < 7; pass++) {
                int passWidth = (width > ADAM7_X_START[pass]) ? (width - ADAM7_X_START[pass] + ADAM7_X_STEP[pass] - 1) / ADAM7_X_STEP[pass] : 0;
                int passHeight = (height > ADAM7_Y_START[pass]) ? (height - ADAM7_Y_START[pass] + ADAM7_Y_STEP[pass] - 1) / ADAM7_Y_STEP[pass] : 0;
                if ((passWidth == 0) || (passHeight == 0)) continue;
                writePass(raw, passWidth, passHeight, ADAM7_X_START[pass], ADAM7_Y_START[pass], ADAM7_X_STEP[pass], ADAM7_Y_STEP[pass], width, bitDepth, channels, samples, filterType, random);
            }
        }
        return deflate(raw.toByteArray());
    }

    private static void writePass(ByteArrayOutputStream out, int passWidth, int passHeight, int xStart, int yStart, int xStep, int yStep, int imageWidth, int bitDepth, int channels, int[] samples, int filterType, Random random) {
        int bitsPerPixel = bitDepth * channels;
        int rowBytes = (passWidth * bitsPerPixel + 7) / 8;
        int distance = Math.max(1, bitsPerPixel / 8);
        byte[] previous = new byte[rowBytes];
        for (int row = 0; row < passHeight; row++) {
            byte[] current = new byte[rowBytes];
            int y = yStart + row * yStep;
            int bit = 0;
            for (int column = 0; column < passWidth; column++) {
                int x = xStart + column * xStep;
                for (int channel = 0; channel < channels; channel++) {
                    int sample = samples[(y * imageWidth + x) * channels + channel];
                    if (bitDepth == 16) {
                        current[bit >> 3] = (byte) (sample >>> 8);
                        current[(bit >> 3) + 1] = (byte) sample;
                    } else if (bitDepth == 8) {
                        current[bit >> 3] = (byte) sample;
                    } else {
                        current[bit >> 3] |= (byte) (sample << (8 - bitDepth - (bit & 7)));
                    }
                    bit += bitDepth;
                }
            }
            int filter = (filterType == RANDOM_FILTERS) ? random.nextInt(5) : filterType;
            out.write(filter);
            for (int i = 0; i < rowBytes; i++) {
                int left = (i >= distance) ? (current[i - distance] & 0xFF) : 0;
                int above = previous[i] & 0xFF;
                int upperLeft = (i >= distance) ? (previous[i - distance] & 0xFF) : 0;
                int predictor = switch (filter) {
                    case 1 -> left;
                    case 2 -> above;
                    case 3 -> (left + above) >>> 1;
                    case 4 -> paeth(left, above, upperLeft);
                    default -> 0;
                };
                out.write((current[i] - predictor) & 0xFF);
            }
            previous = current;
        }
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if ((pa <= pb) && (pa <= pc)) return a;
        if (pb <= pc) return b;
        return c;
    }

    static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(6);
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return out.toByteArray();
    }

    static int[] randomSamples(int width, int height, int bitDepth, int colorType, Random random) {
        int[] samples = new int[width * height * channels(colorType)];
        int max = (1 << bitDepth) - 1;
        for (int i = 0; i < samples.length; i++) {
            samples[i] = random.nextInt(max + 1);
        }
        return samples;
    }

    /**
     * Builds an RGBA8 frame of a single color.
     */
    static int[] solid(int width, int height, int red, int green, int blue, int alpha) {
        int[] samples = new int[width * height * 4];
        for (int i = 0; i < width * height; i++) {
            samples[i * 4] = red;
            samples[i * 4 + 1] = green;
            samples[i * 4 + 2] = blue;
            samples[i * 4 + 3] = alpha;
        }
        return samples;
    }

    /**
     * ABGR pixel of 8-bit RGBA values, as the decoder produces it.
     */
    static int abgr(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    static Png png(int width, int height, int bitDepth, int colorType) {
        return new Png(width, height, bitDepth, colorType);
    }

    /**
     * A PNG or APNG file under construction. Chunks get written in the order the methods get called.
     */
    static final class Png {

        final int width;
        final int height;
        final int bitDepth;
        final int colorType;
        boolean interlaced;
        int filterType = RANDOM_FILTERS;
        /** Bytes per IDAT/fdAT chunk, so data gets split over several chunks. **/
        int maxDataChunkSize = Integer.MAX_VALUE;
        final Random random = new Random(1234L);
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private int sequenceNumber;

        private Png(int width, int height, int bitDepth, int colorType) {
            this.width = width;
            this.height = height;
            this.bitDepth = bitDepth;
            this.colorType = colorType;
            this.out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        }

        Png interlaced(boolean interlaced) {
            this.interlaced = interlaced;
            return this;
        }

        Png filter(int filterType) {
            this.filterType = filterType;
            return this;
        }

        Png splitDataChunks(int maxDataChunkSize) {
            this.maxDataChunkSize = maxDataChunkSize;
            return this;
        }

        Png header() {
            byte[] data = new byte[13];
            putInt(data, 0, this.width);
            putInt(data, 4, this.height);
            data[8] = (byte) this.bitDepth;
            data[9] = (byte) this.colorType;
            data[12] = (byte) (this.interlaced ? 1 : 0);
            return this.chunk("IHDR", data);
        }

        Png palette(int[] rgb) {
            byte[] data = new byte[rgb.length];
            for (int i = 0; i < rgb.length; i++) data[i] = (byte) rgb[i];
            return this.chunk("PLTE", data);
        }

        Png transparency(int... values) {
            byte[] data;
            if (this.colorType == ApngImage.COLOR_TYPE_PALETTE) {
                data = new byte[values.length];
                for (int i = 0; i < values.length; i++) data[i] = (byte) values[i];
            } else {
                data = new byte[values.length * 2];
                for (int i = 0; i < values.length; i++) {
                    data[i * 2] = (byte) (values[i] >>> 8);
                    data[i * 2 + 1] = (byte) values[i];
                }
            }
            return this.chunk("tRNS", data);
        }

        Png animationControl(int frameCount, int numPlays) {
            byte[] data = new byte[8];
            putInt(data, 0, frameCount);
            putInt(data, 4, numPlays);
            return this.chunk("acTL", data);
        }

        Png frameControl(int width, int height, int x, int y, int delayNumerator, int delayDenominator, int disposeOp, int blendOp) {
            byte[] data = new byte[26];
            putInt(data, 0, this.sequenceNumber++);
            putInt(data, 4, width);
            putInt(data, 8, height);
            putInt(data, 12, x);
            putInt(data, 16, y);
            data[20] = (byte) (delayNumerator >>> 8);
            data[21] = (byte) delayNumerator;
            data[22] = (byte) (delayDenominator >>> 8);
            data[23] = (byte) delayDenominator;
            data[24] = (byte) disposeOp;
            data[25] = (byte) blendOp;
            return this.chunk("fcTL", data);
        }

        /**
         * Writes the default image (IDAT chunks) from raw samples of the whole canvas.
         */
        Png imageData(int[] samples) {
            byte[] zlib = encodeImageData(this.width, this.height, this.bitDepth, this.colorType, samples, this.interlaced, this.filterType, this.random);
            for (byte[] part : this.split(zlib)) this.chunk("IDAT", part);
            return this;
        }

        /**
         * Writes the data of an animation frame (fdAT chunks) from raw samples of the frame region.
         */
        Png frameData(int width, int height, int[] samples) {
            byte[] zlib = encodeImageData(width, height, this.bitDepth, this.colorType, samples, this.interlaced, this.filterType, this.random);
            for (byte[] part : this.split(zlib)) {
                byte[] data = new byte[part.length + 4];
                putInt(data, 0, this.sequenceNumber++);
                System.arraycopy(part, 0, data, 4, part.length);
                this.chunk("fdAT", data);
            }
            return this;
        }

        /**
         * Writes a frame control and its data chunks. Frames before the first IDAT chunk must use {@link #imageData(int[])} instead.
         */
        Png frame(int width, int height, int x, int y, int delayNumerator, int delayDenominator, int disposeOp, int blendOp, int[] samples) {
            return this.frameControl(width, height, x, y, delayNumerator, delayDenominator, disposeOp, blendOp).frameData(width, height, samples);
        }

        Png chunk(String type, byte[] data) {
            byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
            byte[] length = new byte[4];
            putInt(length, 0, data.length);
            this.out.writeBytes(length);
            this.out.writeBytes(typeBytes);
            this.out.writeBytes(data);
            CRC32 crc = new CRC32();
            crc.update(typeBytes);
            crc.update(data);
            byte[] crcBytes = new byte[4];
            putInt(crcBytes, 0, (int) crc.getValue());
            this.out.writeBytes(crcBytes);
            return this;
        }

        byte[] end() {
            this.chunk("IEND", new byte[0]);
            return this.out.toByteArray();
        }

        /**
         * The file so far, without IEND, like a download that got cut off.
         */
        byte[] truncated() {
            return this.out.toByteArray();
        }

        private List<byte[]> split(byte[] data) {
            List<byte[]> parts = new ArrayList<>();
            for (int offset = 0; offset < data.length; offset += this.maxDataChunkSize) {
                int length = Math.min(this.maxDataChunkSize, data.length - offset);
                byte[] part = new byte[length];
                System.arraycopy(data, offset, part, 0, length);
                parts.add(part);
            }
            return parts;
        }

    }

    static void putInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }

}
