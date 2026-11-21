package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Decodes the compressed data of APNG frames row by row: inflates it, reverses the scanline filters, walks the seven Adam7 passes of interlaced images
 * and converts the pixels of all PNG color types and bit depths to 8-bit ABGR (the pixel layout of RGBA textures in memory).<br>
 * Every decoded row goes straight to a {@link RowConsumer}, so decoding a frame only needs two rows of memory, no matter how big the frame is.<br><br>
 *
 * 16-bit samples get rounded to the nearest 8-bit value. tRNS colors of 16-bit images get compared with the full 16-bit samples, as the PNG specification says.
 * Not thread-safe, every decoding thread needs its own instance.
 */
final class ApngFrameDecoder implements AutoCloseable {

    private static final int[] ADAM7_X_START = {0, 4, 0, 2, 0, 1, 0};
    private static final int[] ADAM7_Y_START = {0, 0, 4, 0, 2, 0, 1};
    private static final int[] ADAM7_X_STEP = {8, 8, 4, 4, 2, 2, 1};
    private static final int[] ADAM7_Y_STEP = {8, 8, 8, 4, 4, 2, 2};
    /** Reads 4 bytes R, G, B, A as one little-endian int, which is exactly the ABGR pixel. **/
    private static final VarHandle INT_LITTLE_ENDIAN = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    private static final int FORMAT_LOOKUP_PACKED = 0;
    private static final int FORMAT_LOOKUP_BYTE = 1;
    private static final int FORMAT_GRAY_16 = 2;
    private static final int FORMAT_RGB_8 = 3;
    private static final int FORMAT_RGB_16 = 4;
    private static final int FORMAT_GRAY_ALPHA_8 = 5;
    private static final int FORMAT_GRAY_ALPHA_16 = 6;
    private static final int FORMAT_RGBA_8 = 7;
    private static final int FORMAT_RGBA_16 = 8;

    @NotNull
    private final Inflater inflater = new Inflater();
    private final int format;
    private final int bitDepth;
    private final int bitsPerPixel;
    /** Distance in bytes to the corresponding byte of the previous pixel, as used by the scanline filters. **/
    private final int filterDistance;
    private final boolean interlaced;
    /** ABGR pixel of every sample value of palette images and grayscale images with 8 bits or less. **/
    @Nullable
    private final int[] lookup;
    private final int transparentGray;
    private final int transparentRed;
    private final int transparentGreen;
    private final int transparentBlue;
    /** Row buffers with the filter type byte at index 0. They swap after every row, so the previous row stays available for unfiltering. **/
    @NotNull
    private byte[] currentRow;
    @NotNull
    private byte[] previousRow;
    @NotNull
    private final int[] rowPixels;

    ApngFrameDecoder(@NotNull ApngImage image) {
        this.bitDepth = image.getBitDepth();
        this.bitsPerPixel = image.getBitsPerPixel();
        this.filterDistance = Math.max(1, this.bitsPerPixel >>> 3);
        this.interlaced = image.isInterlaced();
        this.transparentGray = image.getTransparentGray();
        this.transparentRed = image.getTransparentRed();
        this.transparentGreen = image.getTransparentGreen();
        this.transparentBlue = image.getTransparentBlue();
        this.format = resolveFormat(image.getColorType(), this.bitDepth);
        this.lookup = buildLookup(image);
        int maxRowBytes = (int) (((long) image.getWidth() * this.bitsPerPixel + 7L) >>> 3);
        this.currentRow = new byte[maxRowBytes + 1];
        this.previousRow = new byte[maxRowBytes + 1];
        this.rowPixels = new int[image.getWidth()];
    }

    /**
     * Decodes all pixels of the frame and hands them row by row to the consumer.
     *
     * @throws IOException If the frame data is corrupted or ends too early.
     */
    void decode(@NotNull ApngFrame frame, @NotNull RowConsumer consumer) throws IOException {
        this.inflater.reset();
        this.inflater.setInput(frame.data());
        try {
            if (!this.interlaced) {
                this.decodePass(frame.width(), frame.height(), 0, 0, 1, 1, consumer);
                return;
            }
            for (int pass = 0; pass < 7; pass++) {
                int passWidth = passSize(frame.width(), ADAM7_X_START[pass], ADAM7_X_STEP[pass]);
                int passHeight = passSize(frame.height(), ADAM7_Y_START[pass], ADAM7_Y_STEP[pass]);
                // Passes without pixels have no rows at all in the data, not even filter type bytes
                if ((passWidth > 0) && (passHeight > 0)) {
                    this.decodePass(passWidth, passHeight, ADAM7_X_START[pass], ADAM7_Y_START[pass], ADAM7_X_STEP[pass], ADAM7_Y_STEP[pass], consumer);
                }
            }
        } catch (DataFormatException ex) {
            throw new IOException("Corrupted APNG frame data", ex);
        }
    }

    private void decodePass(int passWidth, int passHeight, int xStart, int yStart, int xStep, int yStep, @NotNull RowConsumer consumer) throws IOException, DataFormatException {
        int rowBytes = (int) (((long) passWidth * this.bitsPerPixel + 7L) >>> 3);
        byte[] current = this.currentRow;
        byte[] previous = this.previousRow;
        // The first row of every pass gets unfiltered against a row of zeros
        Arrays.fill(previous, 0, rowBytes + 1, (byte) 0);
        try {
            for (int row = 0; row < passHeight; row++) {
                this.inflateFully(current, rowBytes + 1);
                unfilter(current[0] & 0xFF, current, previous, rowBytes, this.filterDistance);
                this.convertRow(current, passWidth);
                consumer.acceptRow(yStart + row * yStep, xStart, xStep, this.rowPixels, passWidth);
                byte[] swap = current;
                current = previous;
                previous = swap;
            }
        } finally {
            this.currentRow = current;
            this.previousRow = previous;
        }
    }

    private void inflateFully(@NotNull byte[] target, int length) throws IOException, DataFormatException {
        int offset = 0;
        while (offset < length) {
            int read = this.inflater.inflate(target, offset, length - offset);
            if (read == 0) {
                if (this.inflater.needsDictionary()) throw new IOException("APNG frame data needs a preset dictionary, which PNG does not allow");
                if (this.inflater.finished() || this.inflater.needsInput()) throw new IOException("APNG frame data ends before all rows of the frame");
            }
            offset += read;
        }
    }

    /**
     * Reverses the scanline filter of a row in place. The row data starts at index 1 of both arrays.
     */
    static void unfilter(int filterType, @NotNull byte[] row, @NotNull byte[] previousRow, int rowBytes, int distance) throws IOException {
        switch (filterType) {
            case 0 -> {
            }
            case 1 -> {
                for (int i = 1 + distance; i <= rowBytes; i++) {
                    row[i] += row[i - distance];
                }
            }
            case 2 -> {
                for (int i = 1; i <= rowBytes; i++) {
                    row[i] += previousRow[i];
                }
            }
            case 3 -> {
                if (distance == 4) {
                    unfilterAverage4(row, previousRow, rowBytes);
                } else if (distance == 3) {
                    unfilterAverage3(row, previousRow, rowBytes);
                } else {
                    int firstPixelEnd = Math.min(distance, rowBytes);
                    for (int i = 1; i <= firstPixelEnd; i++) {
                        row[i] += (byte) ((previousRow[i] & 0xFF) >>> 1);
                    }
                    for (int i = 1 + distance; i <= rowBytes; i++) {
                        row[i] += (byte) (((row[i - distance] & 0xFF) + (previousRow[i] & 0xFF)) >>> 1);
                    }
                }
            }
            case 4 -> {
                if (distance == 4) {
                    unfilterPaeth4(row, previousRow, rowBytes);
                } else if (distance == 3) {
                    unfilterPaeth3(row, previousRow, rowBytes);
                } else {
                    // Without a left neighbor, Paeth always predicts the byte above
                    int firstPixelEnd = Math.min(distance, rowBytes);
                    for (int i = 1; i <= firstPixelEnd; i++) {
                        row[i] += previousRow[i];
                    }
                    for (int i = 1 + distance; i <= rowBytes; i++) {
                        row[i] += (byte) paeth(row[i - distance] & 0xFF, previousRow[i] & 0xFF, previousRow[i - distance] & 0xFF);
                    }
                }
            }
            default -> throw new IOException("Invalid PNG scanline filter type: " + filterType);
        }
    }

    /*
     * The filters of 3- and 4-byte pixels keep the left and upper left pixel of every channel in local variables instead of reading them from the rows again.
     * Every channel is its own dependency chain then, which the CPU can work on in parallel. This makes Paeth, the most used and slowest filter, about a third faster.
     * Rows always hold whole pixels at these distances (8-bit RGB, 8-bit RGBA and 16-bit grayscale with alpha), so the loops never read past the row.
     */

    private static void unfilterPaeth3(@NotNull byte[] row, @NotNull byte[] previousRow, int rowBytes) {
        int left0 = 0, left1 = 0, left2 = 0;
        int upperLeft0 = 0, upperLeft1 = 0, upperLeft2 = 0;
        for (int i = 1; i <= rowBytes; i += 3) {
            int above0 = previousRow[i] & 0xFF;
            int above1 = previousRow[i + 1] & 0xFF;
            int above2 = previousRow[i + 2] & 0xFF;
            left0 = (row[i] + paeth(left0, above0, upperLeft0)) & 0xFF;
            left1 = (row[i + 1] + paeth(left1, above1, upperLeft1)) & 0xFF;
            left2 = (row[i + 2] + paeth(left2, above2, upperLeft2)) & 0xFF;
            row[i] = (byte) left0;
            row[i + 1] = (byte) left1;
            row[i + 2] = (byte) left2;
            upperLeft0 = above0;
            upperLeft1 = above1;
            upperLeft2 = above2;
        }
    }

    private static void unfilterPaeth4(@NotNull byte[] row, @NotNull byte[] previousRow, int rowBytes) {
        int left0 = 0, left1 = 0, left2 = 0, left3 = 0;
        int upperLeft0 = 0, upperLeft1 = 0, upperLeft2 = 0, upperLeft3 = 0;
        for (int i = 1; i <= rowBytes; i += 4) {
            int above0 = previousRow[i] & 0xFF;
            int above1 = previousRow[i + 1] & 0xFF;
            int above2 = previousRow[i + 2] & 0xFF;
            int above3 = previousRow[i + 3] & 0xFF;
            left0 = (row[i] + paeth(left0, above0, upperLeft0)) & 0xFF;
            left1 = (row[i + 1] + paeth(left1, above1, upperLeft1)) & 0xFF;
            left2 = (row[i + 2] + paeth(left2, above2, upperLeft2)) & 0xFF;
            left3 = (row[i + 3] + paeth(left3, above3, upperLeft3)) & 0xFF;
            row[i] = (byte) left0;
            row[i + 1] = (byte) left1;
            row[i + 2] = (byte) left2;
            row[i + 3] = (byte) left3;
            upperLeft0 = above0;
            upperLeft1 = above1;
            upperLeft2 = above2;
            upperLeft3 = above3;
        }
    }

    private static void unfilterAverage3(@NotNull byte[] row, @NotNull byte[] previousRow, int rowBytes) {
        int left0 = 0, left1 = 0, left2 = 0;
        for (int i = 1; i <= rowBytes; i += 3) {
            left0 = (row[i] + ((left0 + (previousRow[i] & 0xFF)) >>> 1)) & 0xFF;
            left1 = (row[i + 1] + ((left1 + (previousRow[i + 1] & 0xFF)) >>> 1)) & 0xFF;
            left2 = (row[i + 2] + ((left2 + (previousRow[i + 2] & 0xFF)) >>> 1)) & 0xFF;
            row[i] = (byte) left0;
            row[i + 1] = (byte) left1;
            row[i + 2] = (byte) left2;
        }
    }

    private static void unfilterAverage4(@NotNull byte[] row, @NotNull byte[] previousRow, int rowBytes) {
        int left0 = 0, left1 = 0, left2 = 0, left3 = 0;
        for (int i = 1; i <= rowBytes; i += 4) {
            left0 = (row[i] + ((left0 + (previousRow[i] & 0xFF)) >>> 1)) & 0xFF;
            left1 = (row[i + 1] + ((left1 + (previousRow[i + 1] & 0xFF)) >>> 1)) & 0xFF;
            left2 = (row[i + 2] + ((left2 + (previousRow[i + 2] & 0xFF)) >>> 1)) & 0xFF;
            left3 = (row[i + 3] + ((left3 + (previousRow[i + 3] & 0xFF)) >>> 1)) & 0xFF;
            row[i] = (byte) left0;
            row[i + 1] = (byte) left1;
            row[i + 2] = (byte) left2;
            row[i + 3] = (byte) left3;
        }
    }

    /**
     * The Paeth predictor of the PNG specification, written so the JIT can compile it without branches.
     */
    private static int paeth(int left, int above, int upperLeft) {
        int towardsAbove = above - upperLeft;
        int towardsLeft = left - upperLeft;
        int distanceLeft = Math.abs(towardsAbove);
        int distanceAbove = Math.abs(towardsLeft);
        int distanceUpperLeft = Math.abs(towardsAbove + towardsLeft);
        int predictor = left;
        if (distanceAbove < distanceLeft) {
            distanceLeft = distanceAbove;
            predictor = above;
        }
        return (distanceUpperLeft < distanceLeft) ? upperLeft : predictor;
    }

    private void convertRow(@NotNull byte[] row, int count) {
        int[] out = this.rowPixels;
        switch (this.format) {
            case FORMAT_LOOKUP_PACKED -> {
                int[] table = this.lookup;
                int depth = this.bitDepth;
                int mask = (1 << depth) - 1;
                int pixel = 0;
                int source = 1;
                while (pixel < count) {
                    int packed = row[source++] & 0xFF;
                    for (int shift = 8 - depth; (shift >= 0) && (pixel < count); shift -= depth) {
                        out[pixel++] = table[(packed >>> shift) & mask];
                    }
                }
            }
            case FORMAT_LOOKUP_BYTE -> {
                int[] table = this.lookup;
                for (int i = 0; i < count; i++) {
                    out[i] = table[row[1 + i] & 0xFF];
                }
            }
            case FORMAT_GRAY_16 -> {
                for (int i = 0, source = 1; i < count; i++, source += 2) {
                    int sample = readSample16(row, source);
                    int gray = scale16(sample);
                    int alpha = (sample == this.transparentGray) ? 0 : 0xFF000000;
                    out[i] = alpha | (gray << 16) | (gray << 8) | gray;
                }
            }
            case FORMAT_RGB_8 -> {
                boolean transparency = this.transparentRed != ApngImage.NO_TRANSPARENT_SAMPLE;
                for (int i = 0, source = 1; i < count; i++, source += 3) {
                    int red = row[source] & 0xFF;
                    int green = row[source + 1] & 0xFF;
                    int blue = row[source + 2] & 0xFF;
                    int alpha = (transparency && (red == this.transparentRed) && (green == this.transparentGreen) && (blue == this.transparentBlue)) ? 0 : 0xFF000000;
                    out[i] = alpha | (blue << 16) | (green << 8) | red;
                }
            }
            case FORMAT_RGB_16 -> {
                for (int i = 0, source = 1; i < count; i++, source += 6) {
                    int red = readSample16(row, source);
                    int green = readSample16(row, source + 2);
                    int blue = readSample16(row, source + 4);
                    int alpha = ((red == this.transparentRed) && (green == this.transparentGreen) && (blue == this.transparentBlue)) ? 0 : 0xFF000000;
                    out[i] = alpha | (scale16(blue) << 16) | (scale16(green) << 8) | scale16(red);
                }
            }
            case FORMAT_GRAY_ALPHA_8 -> {
                for (int i = 0, source = 1; i < count; i++, source += 2) {
                    int gray = row[source] & 0xFF;
                    out[i] = ((row[source + 1] & 0xFF) << 24) | (gray << 16) | (gray << 8) | gray;
                }
            }
            case FORMAT_GRAY_ALPHA_16 -> {
                for (int i = 0, source = 1; i < count; i++, source += 4) {
                    int gray = scale16(readSample16(row, source));
                    out[i] = (scale16(readSample16(row, source + 2)) << 24) | (gray << 16) | (gray << 8) | gray;
                }
            }
            case FORMAT_RGBA_8 -> {
                for (int i = 0, source = 1; i < count; i++, source += 4) {
                    out[i] = (int) INT_LITTLE_ENDIAN.get(row, source);
                }
            }
            case FORMAT_RGBA_16 -> {
                for (int i = 0, source = 1; i < count; i++, source += 8) {
                    out[i] = (scale16(readSample16(row, source + 6)) << 24) | (scale16(readSample16(row, source + 4)) << 16) | (scale16(readSample16(row, source + 2)) << 8) | scale16(readSample16(row, source));
                }
            }
            default -> throw new IllegalStateException("Unknown APNG pixel format: " + this.format);
        }
    }

    private static int readSample16(@NotNull byte[] row, int offset) {
        return ((row[offset] & 0xFF) << 8) | (row[offset + 1] & 0xFF);
    }

    /**
     * Rounds a 16-bit sample to the nearest 8-bit value.
     */
    static int scale16(int sample) {
        return (sample * 255 + 32895) >>> 16;
    }

    private static int passSize(int size, int start, int step) {
        return (size > start) ? ((size - start + step - 1) / step) : 0;
    }

    private static int resolveFormat(int colorType, int bitDepth) {
        return switch (colorType) {
            case ApngImage.COLOR_TYPE_GRAYSCALE -> (bitDepth == 16) ? FORMAT_GRAY_16 : ((bitDepth == 8) ? FORMAT_LOOKUP_BYTE : FORMAT_LOOKUP_PACKED);
            case ApngImage.COLOR_TYPE_PALETTE -> (bitDepth == 8) ? FORMAT_LOOKUP_BYTE : FORMAT_LOOKUP_PACKED;
            case ApngImage.COLOR_TYPE_RGB -> (bitDepth == 16) ? FORMAT_RGB_16 : FORMAT_RGB_8;
            case ApngImage.COLOR_TYPE_GRAYSCALE_ALPHA -> (bitDepth == 16) ? FORMAT_GRAY_ALPHA_16 : FORMAT_GRAY_ALPHA_8;
            case ApngImage.COLOR_TYPE_RGBA -> (bitDepth == 16) ? FORMAT_RGBA_16 : FORMAT_RGBA_8;
            default -> throw new IllegalArgumentException("Unknown PNG color type: " + colorType);
        };
    }

    @Nullable
    private static int[] buildLookup(@NotNull ApngImage image) {
        if (image.getColorType() == ApngImage.COLOR_TYPE_PALETTE) return image.getPalette();
        if ((image.getColorType() != ApngImage.COLOR_TYPE_GRAYSCALE) || (image.getBitDepth() > 8)) return null;
        int maxSample = (1 << image.getBitDepth()) - 1;
        int[] table = new int[maxSample + 1];
        for (int sample = 0; sample <= maxSample; sample++) {
            int gray = (sample * 255) / maxSample;
            int alpha = (sample == image.getTransparentGray()) ? 0 : 0xFF000000;
            table[sample] = alpha | (gray << 16) | (gray << 8) | gray;
        }
        return table;
    }

    @Override
    public void close() {
        this.inflater.end();
    }

    /**
     * Receives the decoded rows of a frame.
     */
    interface RowConsumer {

        /**
         * @param y The frame row of the pixels.
         * @param x The frame column of the first pixel.
         * @param step The column distance of consecutive pixels. Rows of interlaced frames only have every n-th pixel of their row.
         * @param pixels The ABGR pixels of the row. The array gets reused for the next row, so it must not be kept.
         * @param count How many pixels of the array belong to the row.
         */
        void acceptRow(int y, int x, int step, @NotNull int[] pixels, int count);

    }

}
