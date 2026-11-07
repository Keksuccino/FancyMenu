package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.ByteBuffer;

/**
 * Reads color glyph data that AWT can't render: embedded color bitmaps ({@code CBDT}/{@code CBLC}, as used by Noto Color Emoji,
 * and {@code sbix}, as used by Apple Color Emoji) and layered color glyphs ({@code COLR} version 0 records with a {@code CPAL} palette,
 * as used by Twemoji). {@code COLR} version 1 paint graphs are not supported; glyphs that only exist as version 1 paints fall back
 * to their plain outlines.
 */
final class ColorGlyphReader {

    private static final int IMAGE_TYPE_PNG = OpenTypeTables.tag("png ");
    private static final int IMAGE_TYPE_JPG = OpenTypeTables.tag("jpg ");
    private static final int IMAGE_TYPE_DUPE = OpenTypeTables.tag("dupe");
    private static final int FOREGROUND_PALETTE_INDEX = 0xFFFF;
    private static final byte[] NO_IMAGE_DATA = new byte[0];

    private final ByteBuffer data;
    private final int cblc;
    private final int cbdt;
    private final int sbix;
    private final int numGlyphs;
    private final int colr;
    private final int cpal;

    private ColorGlyphReader(@NotNull OpenTypeTables tables) {
        this.data = tables.data();
        int cblcOffset = tables.offsetOf("CBLC");
        int cbdtOffset = tables.offsetOf("CBDT");
        boolean hasCbdt = (cblcOffset >= 0) && (cbdtOffset >= 0);
        this.cblc = hasCbdt ? cblcOffset : -1;
        this.cbdt = hasCbdt ? cbdtOffset : -1;
        this.sbix = tables.offsetOf("sbix");
        this.numGlyphs = tables.numGlyphs();
        int colrOffset = tables.offsetOf("COLR");
        int cpalOffset = tables.offsetOf("CPAL");
        boolean hasColr = (colrOffset >= 0) && (cpalOffset >= 0) && ((this.data.getShort(colrOffset + 2) & 0xFFFF) > 0);
        this.colr = hasColr ? colrOffset : -1;
        this.cpal = hasColr ? cpalOffset : -1;
    }

    /**
     * Returns a reader for the color glyph tables of the font, or {@code null} when the font has no supported color glyphs.
     */
    @Nullable
    static ColorGlyphReader create(@NotNull OpenTypeTables tables) {
        try {
            ColorGlyphReader reader = new ColorGlyphReader(tables);
            return (reader.cbdt >= 0 || reader.sbix >= 0 || reader.colr >= 0) ? reader : null;
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    boolean hasColorGlyph(int glyphId) {
        try {
            if (this.colr >= 0 && this.findBaseGlyphRecord(glyphId) >= 0) {
                return true;
            }
            if (this.cbdt >= 0 && this.findCbdtImage(this.findCbdtStrike(Float.MAX_VALUE), glyphId, false) != null) {
                return true;
            }
            return this.sbix >= 0 && this.findSbixImage(this.findSbixStrike(Float.MAX_VALUE), glyphId, true, false) != null;
        } catch (IndexOutOfBoundsException ex) {
            return false;
        }
    }

    /**
     * Finds the embedded color bitmap of a glyph in the strike that fits the requested pixels-per-em best:
     * the smallest strike that is at least as large, so it only ever gets scaled down, or the largest one otherwise.
     */
    @Nullable
    EmbeddedBitmap findBitmap(int glyphId, float targetPpem) {
        try {
            if (this.cbdt >= 0) {
                EmbeddedBitmap bitmap = this.findCbdtImage(this.findCbdtStrike(targetPpem), glyphId, true);
                if (bitmap != null) {
                    return bitmap;
                }
            }
            if (this.sbix >= 0) {
                return this.findSbixImage(this.findSbixStrike(targetPpem), glyphId, true, true);
            }
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
        return null;
    }

    /**
     * Returns the {@code COLR} version 0 layers of a glyph as pairs of {@code [layerGlyphId, argbColor, ...]}, bottom layer first,
     * or {@code null} when the glyph has no layers. Layers in the foreground color are white, so they take the text color.
     */
    @Nullable
    int[] findLayers(int glyphId) {
        if (this.colr < 0) {
            return null;
        }
        try {
            int record = this.findBaseGlyphRecord(glyphId);
            if (record < 0) {
                return null;
            }
            int firstLayer = this.data.getShort(record + 2) & 0xFFFF;
            int numLayers = this.data.getShort(record + 4) & 0xFFFF;
            int layerRecords = this.colr + this.data.getInt(this.colr + 8);
            int colorRecords = this.cpal + this.data.getInt(this.cpal + 8);
            int numColorRecords = this.data.getShort(this.cpal + 6) & 0xFFFF;
            int paletteStart = this.data.getShort(this.cpal + 12) & 0xFFFF;
            int[] layers = new int[numLayers * 2];
            for (int i = 0; i < numLayers; i++) {
                int layer = layerRecords + ((firstLayer + i) * 4);
                int paletteIndex = this.data.getShort(layer + 2) & 0xFFFF;
                int argb = 0xFFFFFFFF;
                int colorIndex = paletteStart + paletteIndex;
                if (paletteIndex != FOREGROUND_PALETTE_INDEX && colorIndex < numColorRecords) {
                    int color = colorRecords + (colorIndex * 4);
                    int blue = this.data.get(color) & 0xFF;
                    int green = this.data.get(color + 1) & 0xFF;
                    int red = this.data.get(color + 2) & 0xFF;
                    int alpha = this.data.get(color + 3) & 0xFF;
                    argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
                }
                layers[i * 2] = this.data.getShort(layer) & 0xFFFF;
                layers[(i * 2) + 1] = argb;
            }
            return layers;
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    private int findBaseGlyphRecord(int glyphId) {
        int numBaseGlyphs = this.data.getShort(this.colr + 2) & 0xFFFF;
        int baseRecords = this.colr + this.data.getInt(this.colr + 4);
        int low = 0;
        int high = numBaseGlyphs - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int record = baseRecords + (mid * 6);
            int recordGlyph = this.data.getShort(record) & 0xFFFF;
            if (recordGlyph < glyphId) {
                low = mid + 1;
            } else if (recordGlyph > glyphId) {
                high = mid - 1;
            } else {
                return ((this.data.getShort(record + 4) & 0xFFFF) > 0) ? record : -1;
            }
        }
        return -1;
    }

    private int findCbdtStrike(float targetPpem) {
        int numSizes = this.data.getInt(this.cblc + 4);
        int best = -1;
        int bestPpem = 0;
        int largest = -1;
        int largestPpem = -1;
        for (int i = 0; i < numSizes; i++) {
            int size = this.cblc + 8 + (i * 48);
            int ppem = this.data.get(size + 45) & 0xFF;
            int bitDepth = this.data.get(size + 46) & 0xFF;
            if (bitDepth != 32) {
                continue;
            }
            if (ppem >= targetPpem && (best < 0 || ppem < bestPpem)) {
                best = size;
                bestPpem = ppem;
            }
            if (ppem > largestPpem) {
                largest = size;
                largestPpem = ppem;
            }
        }
        return (best >= 0) ? best : largest;
    }

    @Nullable
    private EmbeddedBitmap findCbdtImage(int strike, int glyphId, boolean readImageData) {
        if (strike < 0) {
            return null;
        }
        int ppem = this.data.get(strike + 45) & 0xFF;
        int subTableArray = this.cblc + this.data.getInt(strike);
        int numSubTables = this.data.getInt(strike + 8);
        for (int i = 0; i < numSubTables; i++) {
            int entry = subTableArray + (i * 8);
            int firstGlyph = this.data.getShort(entry) & 0xFFFF;
            int lastGlyph = this.data.getShort(entry + 2) & 0xFFFF;
            if (glyphId < firstGlyph || glyphId > lastGlyph) {
                continue;
            }
            int header = subTableArray + this.data.getInt(entry + 4);
            int indexFormat = this.data.getShort(header) & 0xFFFF;
            int imageFormat = this.data.getShort(header + 2) & 0xFFFF;
            int imageDataOffset = this.cbdt + this.data.getInt(header + 4);
            int imageStart;
            int imageLength;
            int sharedMetrics = -1;
            switch (indexFormat) {
                case 1 -> {
                    int offsets = header + 8 + ((glyphId - firstGlyph) * 4);
                    imageStart = this.data.getInt(offsets);
                    imageLength = this.data.getInt(offsets + 4) - imageStart;
                }
                case 2 -> {
                    imageLength = this.data.getInt(header + 8);
                    imageStart = (glyphId - firstGlyph) * imageLength;
                    sharedMetrics = header + 12;
                }
                case 3 -> {
                    int offsets = header + 8 + ((glyphId - firstGlyph) * 2);
                    imageStart = this.data.getShort(offsets) & 0xFFFF;
                    imageLength = (this.data.getShort(offsets + 2) & 0xFFFF) - imageStart;
                }
                case 4 -> {
                    int numGlyphPairs = this.data.getInt(header + 8);
                    int pairs = header + 12;
                    int index = this.binarySearchGlyph(pairs, 4, numGlyphPairs, glyphId);
                    if (index < 0) {
                        return null;
                    }
                    imageStart = this.data.getShort(pairs + (index * 4) + 2) & 0xFFFF;
                    imageLength = (this.data.getShort(pairs + ((index + 1) * 4) + 2) & 0xFFFF) - imageStart;
                }
                case 5 -> {
                    imageLength = this.data.getInt(header + 8);
                    sharedMetrics = header + 12;
                    int numIds = this.data.getInt(header + 20);
                    int index = this.binarySearchGlyph(header + 24, 2, numIds, glyphId);
                    if (index < 0) {
                        return null;
                    }
                    imageStart = index * imageLength;
                }
                default -> {
                    return null;
                }
            }
            if (imageLength <= 0) {
                return null;
            }
            return this.readCbdtImage(imageDataOffset + imageStart, imageFormat, sharedMetrics, ppem, readImageData);
        }
        return null;
    }

    @Nullable
    private EmbeddedBitmap readCbdtImage(int glyphData, int imageFormat, int sharedMetrics, int ppem, boolean readImageData) {
        int metrics;
        int lengthField;
        switch (imageFormat) {
            case 17 -> {
                metrics = glyphData;
                lengthField = glyphData + 5;
            }
            case 18 -> {
                metrics = glyphData;
                lengthField = glyphData + 8;
            }
            case 19 -> {
                if (sharedMetrics < 0) {
                    return null;
                }
                metrics = sharedMetrics;
                lengthField = glyphData;
            }
            default -> {
                return null;
            }
        }
        // Small and big glyph metrics share their first four fields: height, width, (hori)BearingX, (hori)BearingY.
        int bearingX = this.data.get(metrics + 2);
        int bearingY = this.data.get(metrics + 3);
        int length = this.data.getInt(lengthField);
        if (length <= 0) {
            return null;
        }
        return new EmbeddedBitmap(ppem, readImageData ? this.copy(lengthField + 4, length) : NO_IMAGE_DATA, bearingX, bearingY, false);
    }

    private int findSbixStrike(float targetPpem) {
        int numStrikes = this.data.getInt(this.sbix + 4);
        int best = -1;
        int bestPpem = 0;
        int largest = -1;
        int largestPpem = -1;
        for (int i = 0; i < numStrikes; i++) {
            int strike = this.sbix + this.data.getInt(this.sbix + 8 + (i * 4));
            int ppem = this.data.getShort(strike) & 0xFFFF;
            if (ppem >= targetPpem && (best < 0 || ppem < bestPpem)) {
                best = strike;
                bestPpem = ppem;
            }
            if (ppem > largestPpem) {
                largest = strike;
                largestPpem = ppem;
            }
        }
        return (best >= 0) ? best : largest;
    }

    @Nullable
    private EmbeddedBitmap findSbixImage(int strike, int glyphId, boolean followDuplicates, boolean readImageData) {
        if (strike < 0 || glyphId < 0 || glyphId >= this.numGlyphs) {
            return null;
        }
        int ppem = this.data.getShort(strike) & 0xFFFF;
        int start = this.data.getInt(strike + 4 + (glyphId * 4));
        int end = this.data.getInt(strike + 4 + ((glyphId + 1) * 4));
        int length = end - start;
        if (length <= 8) {
            return null;
        }
        int glyphData = strike + start;
        int originX = this.data.getShort(glyphData);
        int originY = this.data.getShort(glyphData + 2);
        int graphicType = this.data.getInt(glyphData + 4);
        if (graphicType == IMAGE_TYPE_DUPE && followDuplicates) {
            return this.findSbixImage(strike, this.data.getShort(glyphData + 8) & 0xFFFF, false, readImageData);
        }
        if (graphicType != IMAGE_TYPE_PNG && graphicType != IMAGE_TYPE_JPG) {
            return null;
        }
        return new EmbeddedBitmap(ppem, readImageData ? this.copy(glyphData + 8, length - 8) : NO_IMAGE_DATA, originX, originY, true);
    }

    private int binarySearchGlyph(int array, int stride, int count, int glyphId) {
        int low = 0;
        int high = count - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int value = this.data.getShort(array + (mid * stride)) & 0xFFFF;
            if (value < glyphId) {
                low = mid + 1;
            } else if (value > glyphId) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    @NotNull
    private byte[] copy(int offset, int length) {
        byte[] bytes = new byte[length];
        this.data.get(offset, bytes, 0, length);
        return bytes;
    }

    /**
     * An encoded (PNG or JPEG) color bitmap with its placement at the strike's pixels-per-em.
     *
     * @param offsetX left edge of the bitmap relative to the glyph origin, in strike pixels
     * @param offsetY vertical bitmap edge relative to the baseline, in strike pixels, positive up; the top edge for {@code CBDT},
     *                the bottom edge for {@code sbix} ({@code offsetFromBottom})
     */
    record EmbeddedBitmap(int ppem, @NotNull byte[] imageData, int offsetX, int offsetY, boolean offsetFromBottom) {
    }

}
