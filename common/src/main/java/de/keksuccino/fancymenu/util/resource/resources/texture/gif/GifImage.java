package de.keksuccino.fancymenu.util.resource.resources.texture.gif;

import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedFrameDecoder;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.animated.AnimatedImageSource;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A parsed GIF image: its canvas size, loop count, global color table and frames, and the source its frames read their data from.<br>
 * Only the global color table stays in memory. Local color tables and the LZW data of all frames get read from the source again whenever a frame gets decoded.<br>
 * The image owns its source, so closing the image closes the source.
 */
public final class GifImage implements AnimatedImage {

    /** Name of the format, for log messages. **/
    public static final String FORMAT_NAME = "GIF";

    private final int width;
    private final int height;
    private final int numPlays;
    @NotNull
    private final int[] globalPalette;
    @NotNull
    private final GifFrame[] frames;
    @NotNull
    private final AnimatedImageSource source;
    @NotNull
    private final String sourceName;
    private final AtomicBoolean brokenFrameDataReported = new AtomicBoolean();

    GifImage(int width, int height, int numPlays, @NotNull int[] globalPalette, @NotNull GifFrame[] frames, @NotNull AnimatedImageSource source, @NotNull String sourceName) {
        if (frames.length == 0) throw new IllegalArgumentException("An image needs at least one frame");
        if (globalPalette.length != 256) throw new IllegalArgumentException("The global palette needs 256 entries");
        this.width = width;
        this.height = height;
        this.numPlays = numPlays;
        this.globalPalette = globalPalette;
        this.frames = Objects.requireNonNull(frames);
        this.source = Objects.requireNonNull(source);
        this.sourceName = Objects.requireNonNull(sourceName);
    }

    @Override
    @NotNull
    public String getFormatName() {
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

    @Override
    public boolean isAnimated() {
        return this.frames.length > 1;
    }

    /**
     * How often the animation plays, or {@link #INFINITE_PLAYS} if it loops forever. Images without a loop extension play once, like in browsers.
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
    @NotNull
    public GifFrame getFrame(int index) {
        return this.frames[index];
    }

    @Override
    @NotNull
    public AnimatedFrameDecoder createFrameDecoder() {
        return new GifFrameDecoder(this);
    }

    /**
     * The 256 entries of the global color table as opaque ABGR pixels. Entries the table doesn't have, and all entries of images without a global color table, are opaque black.
     * Must not get changed.
     */
    @NotNull
    int[] getGlobalPalette() {
        return this.globalPalette;
    }

    /**
     * Where the frames read their data from.
     */
    @NotNull
    AnimatedImageSource getSource() {
        return this.source;
    }

    /**
     * Name of the image source, only for log messages.
     */
    @NotNull
    String getSourceName() {
        return this.sourceName;
    }

    /**
     * Marks that broken frame data of this image got reported, so the warning only gets logged once per image, not again for every decoded frame and loop.
     *
     * @return True only for the first call.
     */
    boolean markBrokenFrameDataReported() {
        return !this.brokenFrameDataReported.getAndSet(true);
    }

    @Override
    public void close() {
        this.source.close();
    }

}
