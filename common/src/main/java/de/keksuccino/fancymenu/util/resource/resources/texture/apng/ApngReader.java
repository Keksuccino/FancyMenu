package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Reads APNG and plain PNG images chunk by chunk from a stream, as specified by the PNG and APNG specifications.<br>
 * Only the compressed image data of the frames gets kept, so reading never needs much more memory than the file itself, and the stream gets read only once.<br><br>
 *
 * Chunk CRCs of all chunks that get interpreted are verified. Problems in the animation chunks don't fail the whole image: the animation ends with the last
 * intact frame before the problem, or the image shows its default image if there is no intact frame. Truncated files keep all complete frames.
 * Ancillary chunks that don't affect the pixels (text, color profiles, etc.) get skipped.
 */
public final class ApngReader {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Largest supported canvas width and height. Most GPUs can't use bigger textures. **/
    public static final int MAX_DIMENSION = 16384;
    /** Largest supported canvas pixel count, which keeps the 32-bit canvas of an image at 256 MB or less. **/
    public static final long MAX_PIXEL_COUNT = 1L << 26;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int CHUNK_IHDR = 0x49484452;
    private static final int CHUNK_PLTE = 0x504C5445;
    private static final int CHUNK_IDAT = 0x49444154;
    private static final int CHUNK_IEND = 0x49454E44;
    private static final int CHUNK_TRNS = 0x74524E53;
    private static final int CHUNK_ACTL = 0x6163544C;
    private static final int CHUNK_FCTL = 0x6663544C;
    private static final int CHUNK_FDAT = 0x66644154;
    private static final int FCTL_LENGTH = 26;
    private static final int READ_BUFFER_SIZE = 64 * 1024;
    /** Image data gets read in steps of this size, so a corrupted chunk length can't make the reader allocate more than the stream really holds. **/
    private static final int DATA_READ_STEP = 1024 * 1024;

    @NotNull
    private final InputStream in;
    @NotNull
    private final String sourceName;
    private final CRC32 crc = new CRC32();
    /** Holds the length and type of the current chunk, the type gets read from here to compute the CRC. **/
    private final byte[] scratch = new byte[8];
    private final byte[] crcBytes = new byte[4];
    @NotNull
    private final GrowableByteArray frameData = new GrowableByteArray();
    @NotNull
    private final List<ApngFrame> frames = new ArrayList<>();

    private boolean headerRead;
    private int width;
    private int height;
    private int bitDepth;
    private int colorType;
    private boolean interlaced;
    @Nullable
    private byte[] paletteRgb;
    @Nullable
    private byte[] transparencyData;
    private boolean animationControlRead;
    private int numPlays;
    /** Set when an animation chunk was broken, so all animation chunks after it get ignored. **/
    private boolean animationStopped;
    private boolean sequenceWarningLogged;
    private int expectedSequenceNumber;
    /** The frame control of the frame whose data chunks get read right now, or of the frame that waits for the default image. **/
    @Nullable
    private FrameControl pendingFrame;
    private boolean pendingFrameUsesDefaultImage;
    private boolean defaultImageStarted;
    private boolean defaultImageFinished;
    @Nullable
    private byte[] defaultImageData;
    private int previousChunkType;

    private ApngReader(@NotNull InputStream in, @NotNull String sourceName) {
        this.in = (in instanceof BufferedInputStream) ? in : new BufferedInputStream(in, READ_BUFFER_SIZE);
        this.sourceName = sourceName;
    }

    /**
     * Reads a complete APNG or PNG image. Does not close the stream.
     *
     * @param sourceName Name of the image source, only used for log messages.
     * @throws IOException If the stream is no valid PNG image or reading it failed.
     */
    @NotNull
    public static ApngImage read(@NotNull InputStream in, @NotNull String sourceName) throws IOException {
        return new ApngReader(in, sourceName).readImage();
    }

    @NotNull
    private ApngImage readImage() throws IOException {
        this.readFully(this.scratch, 0, 8);
        if (!Arrays.equals(this.scratch, PNG_SIGNATURE)) throw new IOException("Not a PNG image, its signature is invalid");
        boolean lastFrameComplete;
        try {
            lastFrameComplete = this.readChunks();
        } catch (EOFException ex) {
            // Truncated images keep everything that got read completely, like browsers show partially downloaded images
            if (!this.defaultImageStarted) throw new IOException("The PNG image ends before its image data", ex);
            LOGGER.warn("[FANCYMENU] APNG image ends in the middle of a chunk, so its last frame gets dropped: {}", this.sourceName);
            lastFrameComplete = false;
        }
        return this.buildImage(lastFrameComplete);
    }

    /**
     * Reads all chunks until the IEND chunk or until the stream ends between two chunks, so all data that got read is complete.
     *
     * @throws EOFException If the stream ends in the middle of a chunk.
     */
    private boolean readChunks() throws IOException {
        while (true) {
            int headerBytes = this.in.readNBytes(this.scratch, 0, 8);
            if ((headerBytes == 0) && this.defaultImageStarted) {
                LOGGER.warn("[FANCYMENU] APNG image has no IEND chunk: {}", this.sourceName);
                return true;
            }
            if (headerBytes < 8) throw new EOFException();
            int length = readInt(this.scratch, 0);
            int type = readInt(this.scratch, 4);
            if (length < 0) throw new IOException("Invalid PNG chunk length: " + Integer.toUnsignedString(length));
            if (!this.headerRead && (type != CHUNK_IHDR)) throw new IOException("The first chunk of the PNG image is not IHDR");
            if (this.defaultImageStarted && !this.defaultImageFinished && (type != CHUNK_IDAT)) this.finishDefaultImage();
            switch (type) {
                case CHUNK_IHDR -> this.readHeader(length);
                case CHUNK_PLTE -> this.readPalette(length);
                case CHUNK_TRNS -> this.readTransparency(length);
                case CHUNK_ACTL -> this.readAnimationControl(length);
                case CHUNK_FCTL -> this.readFrameControl(length);
                case CHUNK_IDAT -> this.readDefaultImageData(length);
                case CHUNK_FDAT -> this.readFrameData(length);
                case CHUNK_IEND -> {
                    return true;
                }
                default -> {
                    if (isCritical(type)) throw new IOException("Unknown critical PNG chunk: " + chunkName(type));
                    // Ancillary chunks like text, time and color profile chunks don't change the decoded pixels
                    this.skipFully(length + 4L);
                }
            }
            this.previousChunkType = type;
        }
    }

    private void readHeader(int length) throws IOException {
        if (this.headerRead) throw new IOException("The PNG image has more than one IHDR chunk");
        if (length != 13) throw new IOException("Invalid IHDR chunk length: " + length);
        byte[] data = Objects.requireNonNull(this.readChunkData(CHUNK_IHDR, length));
        this.width = readInt(data, 0);
        this.height = readInt(data, 4);
        this.bitDepth = data[8] & 0xFF;
        this.colorType = data[9] & 0xFF;
        int compressionMethod = data[10] & 0xFF;
        int filterMethod = data[11] & 0xFF;
        int interlaceMethod = data[12] & 0xFF;
        if ((this.width <= 0) || (this.height <= 0)) throw new IOException("Invalid PNG image size: " + Integer.toUnsignedString(this.width) + "x" + Integer.toUnsignedString(this.height));
        if ((this.width > MAX_DIMENSION) || (this.height > MAX_DIMENSION) || (((long) this.width * this.height) > MAX_PIXEL_COUNT)) {
            throw new IOException("PNG image is too big to get used as texture: " + this.width + "x" + this.height);
        }
        if (!isValidBitDepth(this.colorType, this.bitDepth)) throw new IOException("Invalid PNG color type and bit depth combination: " + this.colorType + "/" + this.bitDepth);
        if (compressionMethod != 0) throw new IOException("Unknown PNG compression method: " + compressionMethod);
        if (filterMethod != 0) throw new IOException("Unknown PNG filter method: " + filterMethod);
        if (interlaceMethod > 1) throw new IOException("Unknown PNG interlace method: " + interlaceMethod);
        this.interlaced = interlaceMethod == 1;
        this.headerRead = true;
    }

    private void readPalette(int length) throws IOException {
        if (this.defaultImageStarted) throw new IOException("The PLTE chunk comes after the image data");
        if (this.paletteRgb != null) throw new IOException("The PNG image has more than one PLTE chunk");
        if ((length < 3) || (length > 768) || ((length % 3) != 0)) throw new IOException("Invalid PLTE chunk length: " + length);
        byte[] data = Objects.requireNonNull(this.readChunkData(CHUNK_PLTE, length));
        // Grayscale images must not have a palette, and RGB images only have it as suggestion for displays with few colors
        if (this.colorType == ApngImage.COLOR_TYPE_PALETTE) this.paletteRgb = data;
    }

    private void readTransparency(int length) throws IOException {
        boolean valid = switch (this.colorType) {
            case ApngImage.COLOR_TYPE_GRAYSCALE -> length == 2;
            case ApngImage.COLOR_TYPE_RGB -> length == 6;
            case ApngImage.COLOR_TYPE_PALETTE -> (this.paletteRgb != null) && (length <= 256);
            default -> false; // Color types with alpha channel must not have a tRNS chunk
        };
        if (!valid || this.defaultImageStarted || (this.transparencyData != null)) {
            this.skipFully(length + 4L);
            return;
        }
        // A corrupted tRNS chunk gets ignored, like libpng does with all corrupted ancillary chunks
        this.transparencyData = this.readChunkData(CHUNK_TRNS, length);
    }

    private void readAnimationControl(int length) throws IOException {
        // The animation control only counts before the image data, otherwise the image is a plain PNG
        if (this.defaultImageStarted || this.animationControlRead || (length != 8)) {
            this.skipFully(length + 4L);
            return;
        }
        byte[] data = this.readChunkData(CHUNK_ACTL, length);
        if (data == null) {
            this.stopAnimation("its acTL chunk is corrupted", false);
            return;
        }
        int frameCount = readInt(data, 0);
        this.numPlays = readInt(data, 4);
        if ((frameCount <= 0) || (this.numPlays < 0)) {
            this.stopAnimation("its acTL chunk is invalid", false);
            return;
        }
        this.animationControlRead = true;
    }

    private void readFrameControl(int length) throws IOException {
        if (!this.animationControlRead || this.animationStopped) {
            this.skipFully(length + 4L);
            return;
        }
        // A frame control ends the data of the frame before it, so that frame is complete even if this frame control turns out broken
        if (length != FCTL_LENGTH) {
            this.skipFully(length + 4L);
            this.stopAnimation("one of its fcTL chunks has an invalid length", true);
            return;
        }
        byte[] data = this.readChunkData(CHUNK_FCTL, length);
        if (data == null) {
            this.stopAnimation("one of its fcTL chunks is corrupted", true);
            return;
        }
        this.checkSequenceNumber(readInt(data, 0));
        FrameControl control = new FrameControl(readInt(data, 4), readInt(data, 8), readInt(data, 12), readInt(data, 16), readShort(data, 20), readShort(data, 22), data[24] & 0xFF, data[25] & 0xFF);
        if (!control.isValid(this.width, this.height)) {
            this.stopAnimation("one of its frames is outside the canvas or has invalid dispose/blend operations", true);
            return;
        }
        if (!this.defaultImageStarted) {
            // A frame control before the image data makes the default image the first frame, which then has to cover the whole canvas
            if ((this.pendingFrame != null) || (control.x != 0) || (control.y != 0) || (control.width != this.width) || (control.height != this.height)) {
                this.stopAnimation("its first frame does not match the default image", false);
                return;
            }
            this.pendingFrame = control;
            this.pendingFrameUsesDefaultImage = true;
            return;
        }
        this.finishPendingFrame();
        this.pendingFrame = control;
        this.pendingFrameUsesDefaultImage = false;
        this.frameData.clear();
    }

    private void readDefaultImageData(int length) throws IOException {
        if (this.defaultImageFinished || (this.defaultImageStarted && (this.previousChunkType != CHUNK_IDAT))) {
            throw new IOException("The IDAT chunks of the PNG image are not consecutive");
        }
        if (!this.defaultImageStarted) {
            if ((this.colorType == ApngImage.COLOR_TYPE_PALETTE) && (this.paletteRgb == null)) throw new IOException("The palette PNG image has no PLTE chunk");
            this.defaultImageStarted = true;
            this.frameData.clear();
        }
        this.crc.reset();
        this.crc.update(this.scratch, 4, 4);
        this.readFrameBytes(length);
        if (!this.readAndCheckCrc()) throw new IOException("Corrupted PNG chunk, its CRC does not match: IDAT");
    }

    private void readFrameData(int length) throws IOException {
        boolean expected = this.animationControlRead && !this.animationStopped && this.defaultImageFinished && (this.pendingFrame != null) && !this.pendingFrameUsesDefaultImage;
        if (!expected || (length < 4)) {
            this.skipFully(length + 4L);
            if (expected) this.stopAnimation("one of its fdAT chunks is too short", false);
            return;
        }
        this.crc.reset();
        this.crc.update(this.scratch, 4, 4);
        this.readFully(this.scratch, 0, 4);
        this.crc.update(this.scratch, 0, 4);
        int sequenceNumber = readInt(this.scratch, 0);
        this.readFrameBytes(length - 4);
        if (!this.readAndCheckCrc()) {
            this.stopAnimation("one of its fdAT chunks is corrupted", false);
            return;
        }
        this.checkSequenceNumber(sequenceNumber);
    }

    private void readFrameBytes(int length) throws IOException {
        int remaining = length;
        while (remaining > 0) {
            int step = Math.min(remaining, DATA_READ_STEP);
            int offset = this.frameData.size();
            this.frameData.ensureAdditionalCapacity(step);
            this.readFully(this.frameData.array(), offset, step);
            this.frameData.setSize(offset + step);
            this.crc.update(this.frameData.array(), offset, step);
            remaining -= step;
        }
    }

    private void finishDefaultImage() {
        this.defaultImageFinished = true;
        this.defaultImageData = this.frameData.toArray();
        this.frameData.clear();
        if ((this.pendingFrame != null) && this.pendingFrameUsesDefaultImage) {
            this.frames.add(this.pendingFrame.toFrame(this.defaultImageData));
            this.pendingFrame = null;
            this.pendingFrameUsesDefaultImage = false;
        }
    }

    private void finishPendingFrame() {
        FrameControl control = this.pendingFrame;
        if ((control == null) || this.pendingFrameUsesDefaultImage) return;
        this.pendingFrame = null;
        if (this.frameData.size() == 0) {
            this.stopAnimation("one of its frames has no image data", false);
            return;
        }
        this.frames.add(control.toFrame(this.frameData.toArray()));
        this.frameData.clear();
    }

    @NotNull
    private ApngImage buildImage(boolean lastFrameComplete) throws IOException {
        if (!this.defaultImageStarted) throw new IOException("The PNG image has no image data");
        if (!this.defaultImageFinished) this.finishDefaultImage();
        // The data of a frame that got cut off by the end of a truncated stream is incomplete, so that frame gets dropped
        if (lastFrameComplete && !this.animationStopped) this.finishPendingFrame();
        this.pendingFrame = null;
        byte[] defaultImage = this.defaultImageData;
        if ((defaultImage == null) || (defaultImage.length == 0)) throw new IOException("The PNG image has no image data");
        boolean animated = this.animationControlRead && !this.frames.isEmpty();
        ApngFrame[] resultFrames = animated ? this.frames.toArray(new ApngFrame[0]) : new ApngFrame[] {new ApngFrame(0, 0, this.width, this.height, 0L, ApngFrame.DISPOSE_OP_NONE, ApngFrame.BLEND_OP_SOURCE, defaultImage)};
        int transparentGray = ApngImage.NO_TRANSPARENT_SAMPLE;
        int transparentRed = ApngImage.NO_TRANSPARENT_SAMPLE;
        int transparentGreen = ApngImage.NO_TRANSPARENT_SAMPLE;
        int transparentBlue = ApngImage.NO_TRANSPARENT_SAMPLE;
        byte[] transparency = this.transparencyData;
        // Samples with more bits than the image break the specification, libpng only uses their low bits, so these images look like in browsers
        int sampleMask = (1 << this.bitDepth) - 1;
        if ((transparency != null) && (this.colorType == ApngImage.COLOR_TYPE_GRAYSCALE)) {
            transparentGray = readShort(transparency, 0) & sampleMask;
        } else if ((transparency != null) && (this.colorType == ApngImage.COLOR_TYPE_RGB)) {
            transparentRed = readShort(transparency, 0) & sampleMask;
            transparentGreen = readShort(transparency, 2) & sampleMask;
            transparentBlue = readShort(transparency, 4) & sampleMask;
        }
        return new ApngImage(this.width, this.height, this.bitDepth, this.colorType, this.interlaced, this.buildPalette(), transparentGray, transparentRed, transparentGreen, transparentBlue, animated, animated ? this.numPlays : ApngImage.INFINITE_PLAYS, resultFrames);
    }

    /**
     * Builds the 256 ABGR entries of palette images. Pixels with indices outside the palette are invalid, they show opaque black like in libpng.
     */
    @Nullable
    private int[] buildPalette() {
        byte[] rgb = this.paletteRgb;
        if ((this.colorType != ApngImage.COLOR_TYPE_PALETTE) || (rgb == null)) return null;
        byte[] alpha = this.transparencyData;
        int[] palette = new int[256];
        Arrays.fill(palette, 0xFF000000);
        int entries = rgb.length / 3;
        for (int i = 0; i < entries; i++) {
            int a = ((alpha != null) && (i < alpha.length)) ? (alpha[i] & 0xFF) : 0xFF;
            palette[i] = (a << 24) | ((rgb[i * 3 + 2] & 0xFF) << 16) | ((rgb[i * 3 + 1] & 0xFF) << 8) | (rgb[i * 3] & 0xFF);
        }
        return palette;
    }

    /**
     * Ends the animation at a broken animation chunk. All animation chunks after it get ignored.
     *
     * @param pendingFrameComplete If all data of the frame that got read last is intact, so it can still play.
     */
    private void stopAnimation(@NotNull String reason, boolean pendingFrameComplete) {
        if (this.animationStopped) return;
        FrameControl pending = this.pendingFrame;
        if (pendingFrameComplete && (pending != null) && !this.pendingFrameUsesDefaultImage && (this.frameData.size() > 0)) {
            this.frames.add(pending.toFrame(this.frameData.toArray()));
        }
        this.animationStopped = true;
        this.pendingFrame = null;
        this.pendingFrameUsesDefaultImage = false;
        if (this.defaultImageFinished) this.frameData.clear();
        if (this.frames.isEmpty()) {
            LOGGER.warn("[FANCYMENU] The animation of an APNG image is broken, because {}, so only its default image gets shown: {}", reason, this.sourceName);
        } else {
            LOGGER.warn("[FANCYMENU] The animation of an APNG image is broken, because {}, so it only plays the {} frames before that: {}", reason, this.frames.size(), this.sourceName);
        }
    }

    private void checkSequenceNumber(int sequenceNumber) {
        // Chunk order is what really defines the frame order, so wrong numbers only get reported
        if ((sequenceNumber != this.expectedSequenceNumber) && !this.sequenceWarningLogged) {
            this.sequenceWarningLogged = true;
            LOGGER.warn("[FANCYMENU] APNG image has out-of-order animation chunk sequence numbers (expected {}, got {}), using the chunk order: {}", this.expectedSequenceNumber, sequenceNumber, this.sourceName);
        }
        this.expectedSequenceNumber = sequenceNumber + 1;
    }

    /**
     * Reads the data of a chunk whose length got checked before. Corrupted critical chunks fail the image, corrupted ancillary chunks return NULL.
     */
    @Nullable
    private byte[] readChunkData(int type, int length) throws IOException {
        byte[] data = new byte[length];
        this.readFully(data, 0, length);
        this.crc.reset();
        this.crc.update(this.scratch, 4, 4);
        this.crc.update(data, 0, length);
        if (this.readAndCheckCrc()) return data;
        if (isCritical(type)) throw new IOException("Corrupted PNG chunk, its CRC does not match: " + chunkName(type));
        LOGGER.warn("[FANCYMENU] Ignoring corrupted {} chunk of APNG image: {}", chunkName(type), this.sourceName);
        return null;
    }

    private boolean readAndCheckCrc() throws IOException {
        this.readFully(this.crcBytes, 0, 4);
        return readInt(this.crcBytes, 0) == (int) this.crc.getValue();
    }

    private void readFully(@NotNull byte[] target, int offset, int length) throws IOException {
        if (this.in.readNBytes(target, offset, length) < length) throw new EOFException();
    }

    private void skipFully(long count) throws IOException {
        this.in.skipNBytes(count);
    }

    private static boolean isValidBitDepth(int colorType, int bitDepth) {
        return switch (colorType) {
            case ApngImage.COLOR_TYPE_GRAYSCALE -> (bitDepth == 1) || (bitDepth == 2) || (bitDepth == 4) || (bitDepth == 8) || (bitDepth == 16);
            case ApngImage.COLOR_TYPE_PALETTE -> (bitDepth == 1) || (bitDepth == 2) || (bitDepth == 4) || (bitDepth == 8);
            case ApngImage.COLOR_TYPE_RGB, ApngImage.COLOR_TYPE_GRAYSCALE_ALPHA, ApngImage.COLOR_TYPE_RGBA -> (bitDepth == 8) || (bitDepth == 16);
            default -> false;
        };
    }

    private static boolean isCritical(int type) {
        return (type & 0x20000000) == 0;
    }

    @NotNull
    private static String chunkName(int type) {
        char[] name = {(char) ((type >>> 24) & 0xFF), (char) ((type >>> 16) & 0xFF), (char) ((type >>> 8) & 0xFF), (char) (type & 0xFF)};
        return new String(name);
    }

    private static int readInt(@NotNull byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    private static int readShort(@NotNull byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    /**
     * The values of one fcTL chunk.
     */
    private record FrameControl(int width, int height, int x, int y, int delayNumerator, int delayDenominator, int disposeOp, int blendOp) {

        private boolean isValid(int canvasWidth, int canvasHeight) {
            return (this.width > 0) && (this.height > 0) && (this.x >= 0) && (this.y >= 0)
                    && (((long) this.x + this.width) <= canvasWidth) && (((long) this.y + this.height) <= canvasHeight)
                    && (this.disposeOp <= ApngFrame.DISPOSE_OP_PREVIOUS) && (this.blendOp <= ApngFrame.BLEND_OP_OVER);
        }

        @NotNull
        private ApngFrame toFrame(@NotNull byte[] data) {
            // A denominator of 0 means 1/100 seconds, as the APNG specification says
            int denominator = (this.delayDenominator == 0) ? 100 : this.delayDenominator;
            long delayNanos = (this.delayNumerator * 1_000_000_000L) / denominator;
            return new ApngFrame(this.x, this.y, this.width, this.height, delayNanos, this.disposeOp, this.blendOp, data);
        }

    }

    /**
     * Collects the compressed data of one frame. The buffer gets reused for all frames, and every frame gets an exactly sized copy.
     */
    private static final class GrowableByteArray {

        private static final int INITIAL_CAPACITY = 64 * 1024;

        @NotNull
        private byte[] array = new byte[0];
        private int size;

        private void ensureAdditionalCapacity(int additional) throws IOException {
            long required = (long) this.size + additional;
            if (required > (Integer.MAX_VALUE - 8)) throw new IOException("APNG frame data is too big");
            if (required <= this.array.length) return;
            long grown = Math.max(required, Math.max(INITIAL_CAPACITY, this.array.length * 2L));
            this.array = Arrays.copyOf(this.array, (int) Math.min(grown, Integer.MAX_VALUE - 8));
        }

        @NotNull
        private byte[] array() {
            return this.array;
        }

        private int size() {
            return this.size;
        }

        private void setSize(int size) {
            this.size = size;
        }

        private void clear() {
            this.size = 0;
        }

        @NotNull
        private byte[] toArray() {
            return Arrays.copyOf(this.array, this.size);
        }

    }

}
