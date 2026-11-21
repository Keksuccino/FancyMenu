package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedFrameDecoder;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * A parsed APNG or PNG image: its canvas size, pixel format, transparency info, loop count and frames, and the source its frames read their data from.<br>
 * Plain PNG images, and APNG images whose animation breaks before its first intact frame, become a single non-animated frame.<br>
 * The image owns its source, so closing the image closes the source.
 */
public final class ApngImage implements AnimatedImage {

    /** Name of the format, for log messages. **/
    public static final String FORMAT_NAME = "APNG";
    public static final int COLOR_TYPE_GRAYSCALE = 0;
    public static final int COLOR_TYPE_RGB = 2;
    public static final int COLOR_TYPE_PALETTE = 3;
    public static final int COLOR_TYPE_GRAYSCALE_ALPHA = 4;
    public static final int COLOR_TYPE_RGBA = 6;
    /** Transparency sample value of images without a transparent color. **/
    public static final int NO_TRANSPARENT_SAMPLE = -1;

    private final int width;
    private final int height;
    private final int bitDepth;
    private final int colorType;
    private final boolean interlaced;
    @Nullable
    private final int[] palette;
    private final int transparentGray;
    private final int transparentRed;
    private final int transparentGreen;
    private final int transparentBlue;
    private final boolean animated;
    private final int numPlays;
    @NotNull
    private final ApngFrame[] frames;
    @NotNull
    private final AnimatedImageSource source;

    ApngImage(int width, int height, int bitDepth, int colorType, boolean interlaced, @Nullable int[] palette, int transparentGray, int transparentRed, int transparentGreen, int transparentBlue, boolean animated, int numPlays, @NotNull ApngFrame[] frames, @NotNull AnimatedImageSource source) {
        if (frames.length == 0) throw new IllegalArgumentException("An image needs at least one frame");
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.colorType = colorType;
        this.interlaced = interlaced;
        this.palette = palette;
        this.transparentGray = transparentGray;
        this.transparentRed = transparentRed;
        this.transparentGreen = transparentGreen;
        this.transparentBlue = transparentBlue;
        this.animated = animated;
        this.numPlays = numPlays;
        this.frames = Objects.requireNonNull(frames);
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public @NotNull String getFormatName() {
        return FORMAT_NAME;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    public int getBitDepth() {
        return this.bitDepth;
    }

    public int getColorType() {
        return this.colorType;
    }

    public boolean isInterlaced() {
        return this.interlaced;
    }

    /**
     * The 256 palette entries of palette images as ABGR pixels with their tRNS alpha applied, or NULL for other color types.
     */
    @Nullable
    int[] getPalette() {
        return this.palette;
    }

    /**
     * The raw gray sample that is fully transparent, or {@link #NO_TRANSPARENT_SAMPLE}.
     */
    int getTransparentGray() {
        return this.transparentGray;
    }

    /**
     * The raw red sample of the fully transparent RGB color, or {@link #NO_TRANSPARENT_SAMPLE}.
     */
    int getTransparentRed() {
        return this.transparentRed;
    }

    int getTransparentGreen() {
        return this.transparentGreen;
    }

    int getTransparentBlue() {
        return this.transparentBlue;
    }

    /**
     * If the image is an APNG animation. Plain PNG images and animations that break before their first intact frame only have their default image as single frame.
     */
    @Override
    public boolean isAnimated() {
        return this.animated;
    }

    /**
     * How often the animation plays, or {@link #INFINITE_PLAYS} if it loops forever.
     */
    @Override
    public int getNumPlays() {
        return this.numPlays;
    }

    @Override
    public int getFrameCount() {
        return this.frames.length;
    }

    @Override
    public @NotNull ApngFrame getFrame(int index) {
        return this.frames[index];
    }

    /**
     * How many bits one pixel of the image data has.
     */
    public int getBitsPerPixel() {
        return switch (this.colorType) {
            case COLOR_TYPE_RGB -> this.bitDepth * 3;
            case COLOR_TYPE_GRAYSCALE_ALPHA -> this.bitDepth * 2;
            case COLOR_TYPE_RGBA -> this.bitDepth * 4;
            default -> this.bitDepth;
        };
    }

    @Override
    public @NotNull AnimatedFrameDecoder createFrameDecoder() {
        return new ApngFrameDecoder(this);
    }

    /**
     * Where the frames read their compressed data from.
     */
    @NotNull
    AnimatedImageSource getSource() {
        return this.source;
    }

    /**
     * How many bytes of compressed frame data the frames read from the source together.
     */
    public long getCompressedDataSize() {
        long size = 0L;
        for (ApngFrame frame : this.frames) {
            size += frame.dataChunks().dataSize();
        }
        return size;
    }

    @Override
    public void close() {
        this.source.close();
    }

}
