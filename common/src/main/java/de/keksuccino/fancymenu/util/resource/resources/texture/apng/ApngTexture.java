package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.TextureManagerReleaseDispatcher;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An APNG (or plain PNG) image that plays its animation on a single texture.<br>
 * The image gets parsed once, then {@link ApngFrameStream} decodes and composes its frames in the background, a few frames ahead of playback.
 * The render thread shows frames on time while the texture gets rendered and only uploads the pixels that changed since the frame before.<br>
 * Playback only advances while the texture gets rendered, like with AFMA textures. A texture that wasn't rendered for a while continues with the frame it showed.
 */
public class ApngTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Playback catches up when it falls behind, like when decoding was too slow for a moment, unless it fell behind by more than this. Then it continues from now. **/
    private static final long MAX_PLAYBACK_LAG_NANOS = 250_000_000L;
    /** Most frames that get shown per rendered frame, for animations faster than the frame rate of the game. **/
    private static final int MAX_FRAMES_PER_RENDER = 8;

    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile int width = 10;
    protected volatile int height = 10;
    @Nullable
    protected volatile Identifier sourceLocation;
    @Nullable
    protected volatile File sourceFile;
    @Nullable
    protected volatile String sourceURL;
    @Nullable
    protected volatile String sourceName;
    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();
    /** If the image got parsed or failed to load, so its size is known. **/
    protected final AtomicBoolean decoded = new AtomicBoolean(false);
    protected final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    protected final AtomicBoolean closed = new AtomicBoolean(false);
    /** How many full play cycles got completed since the last reset, loop restarts included. **/
    protected final AtomicInteger cycles = new AtomicInteger(0);
    /** How many times the APNG should play. Values <= 0 mean infinite loops. **/
    protected final AtomicInteger numPlays = new AtomicInteger(0);
    protected volatile boolean animated = false;
    protected volatile boolean maxLoopsReached = false;
    protected volatile boolean pendingStartEvent = true;
    protected volatile boolean paused = false;
    /** When the texture got paused, so the visible frame can continue its remaining time when it plays again. -1 while not paused. **/
    protected volatile long pausedAtNanos = -1L;
    protected volatile boolean restartRequested = false;
    /** The stream of the current image, replaced when the image gets loaded again and cleared once a static image is on its texture. **/
    protected final AtomicReference<ApngFrameStream> stream = new AtomicReference<>();

    // Only used on the render thread
    @Nullable
    protected ApngFrameStream playingStream;
    @Nullable
    protected ApngGpuTexture gpuTexture;
    @Nullable
    protected Identifier textureLocation;
    protected int textureCounter = 0;
    protected boolean frameShown = false;
    protected int shownPlay = 0;
    protected long nextFrameNanos = 0L;

    @NotNull
    public static ApngTexture location(@NotNull Identifier location) {
        return location(location, null);
    }

    @NotNull
    public static ApngTexture location(@NotNull Identifier location, @Nullable ApngTexture writeTo) {
        Objects.requireNonNull(location);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        texture.sourceLocation = location;
        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            texture.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read APNG image from Identifier: " + location, ex);
        }
        return texture;
    }

    @NotNull
    public static ApngTexture local(@NotNull File apngFile) {
        return local(apngFile, null);
    }

    @NotNull
    public static ApngTexture local(@NotNull File apngFile, @Nullable ApngTexture writeTo) {
        Objects.requireNonNull(apngFile);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        texture.sourceFile = apngFile;
        if (!apngFile.isFile()) {
            texture.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read APNG image from file! File not found: " + apngFile.getPath());
            return texture;
        }
        FancyMenuThreads.startDaemonThread(() -> {
            try {
                populateTexture(texture, new FileInputStream(apngFile), apngFile.getPath());
            } catch (Exception ex) {
                texture.publishFailure();
                LOGGER.error("[FANCYMENU] Failed to read APNG image from file: " + apngFile.getPath(), ex);
            }
        }, "ApngTexture-LocalLoader");
        return texture;
    }

    @NotNull
    public static ApngTexture web(@NotNull String apngUrl) {
        return web(apngUrl, null);
    }

    @NotNull
    public static ApngTexture web(@NotNull String apngUrl, @Nullable ApngTexture writeTo) {
        Objects.requireNonNull(apngUrl);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        texture.sourceURL = apngUrl;
        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(apngUrl)) {
            texture.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read APNG image from URL! Invalid URL: " + apngUrl);
            return texture;
        }
        FancyMenuThreads.startDaemonThread(() -> {
            try {
                InputStream in = texture.open();
                if (in == null) throw new IOException("Web resource input stream was NULL");
                populateTexture(texture, in, apngUrl);
            } catch (Exception ex) {
                texture.publishFailure();
                LOGGER.error("[FANCYMENU] Failed to read APNG image from URL: " + apngUrl, ex);
            }
        }, "ApngTexture-WebLoader");
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in, @Nullable String apngTextureName, @Nullable ApngTexture writeTo) {
        Objects.requireNonNull(in);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();
        FancyMenuThreads.startDaemonThread(() -> populateTexture(texture, in, (apngTextureName != null) ? apngTextureName : "[Generic InputStream Source]"), "ApngTexture-Decoder");
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    /**
     * Parses the image and starts decoding its frames. Closes the stream.
     */
    protected static void populateTexture(@NotNull ApngTexture texture, @NotNull InputStream in, @NotNull String apngTextureName) {
        texture.sourceName = apngTextureName;
        try (InputStream input = in) {
            if (texture.closed.get()) return;
            texture.applyImage(ApngReader.read(input, apngTextureName), apngTextureName);
        } catch (Exception ex) {
            texture.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read APNG image: " + apngTextureName, ex);
        }
    }

    protected ApngTexture() {
    }

    protected void applyImage(@NotNull ApngImage image, @NotNull String apngTextureName) {
        ApngFrameStream newStream = new ApngFrameStream(image, apngTextureName);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.aspectRatio = new AspectRatio(image.getWidth(), image.getHeight());
        this.numPlays.set(image.getNumPlays());
        this.animated = image.isAnimated();
        ApngFrameStream previous = this.stream.getAndSet(newStream);
        newStream.start();
        this.loadingFailed.set(false);
        this.decoded.set(true);
        // Streams belong to the render thread, which then also notices the new stream and switches to it
        if (previous != null) TextureManagerReleaseDispatcher.dispatch(previous::close);
        if (this.closed.get()) TextureManagerReleaseDispatcher.dispatch(this::releaseRenderResources);
    }

    protected void publishFailure() {
        this.loadingFailed.set(true);
        this.decoded.set(true);
    }

    @Nullable
    @Override
    public Identifier getResourceLocation() {
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        ApngFrameStream activeStream = this.stream.get();
        // Static images need nothing but their texture once it shows them
        if ((activeStream == null) && this.frameShown && !this.animated) return this.textureLocation;
        // Returning NULL while loading or after failing lets callers show their fallback, like they do for other images
        if ((activeStream == null) || this.loadingFailed.get()) return null;
        if (activeStream != this.playingStream) this.switchStream(activeStream);
        activeStream.markActive();
        try {
            this.updatePlayback(activeStream, System.nanoTime());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] APNG playback failed: " + this.resolveTextureName(), ex);
            this.publishFailure();
            return null;
        }
        if (activeStream.hasFailed()) {
            this.publishFailure();
            return null;
        }
        return this.frameShown ? this.textureLocation : null;
    }

    /**
     * Starts playing the stream of a newly loaded image from its beginning.
     */
    protected void switchStream(@NotNull ApngFrameStream newStream) {
        ApngFrameStream previous = this.playingStream;
        if (previous != null) previous.close();
        this.playingStream = newStream;
        this.frameShown = false;
        this.shownPlay = 0;
        this.restartRequested = false;
        this.maxLoopsReached = false;
        this.pendingStartEvent = true;
        this.cycles.set(0);
    }

    protected void updatePlayback(@NotNull ApngFrameStream activeStream, long now) {
        if (this.restartRequested) {
            this.restartRequested = false;
            if (this.frameShown) {
                ApngFrameUpdate first = activeStream.restart();
                this.shownPlay = 0;
                this.nextFrameNanos = now;
                if (first != null) this.showFrame(activeStream, first, now);
            }
        }
        if (!this.frameShown) {
            ApngFrameUpdate first = activeStream.poll();
            if (first == null) return;
            this.ensureGpuTexture(activeStream.getWidth(), activeStream.getHeight());
            this.showFrame(activeStream, first, now);
            if (!this.animated) {
                // The texture keeps the image, so the image data and the stream are not needed anymore
                activeStream.close();
                this.stream.compareAndSet(activeStream, null);
                this.playingStream = null;
            }
            return;
        }
        if (this.maxLoopsReached || this.paused) return;
        long pausedAt = this.pausedAtNanos;
        if (pausedAt >= 0L) {
            // The visible frame continues with the time it had left when the texture got paused
            this.nextFrameNanos += Math.max(0L, now - pausedAt);
            this.pausedAtNanos = -1L;
        }
        if ((now - this.nextFrameNanos) > MAX_PLAYBACK_LAG_NANOS) this.nextFrameNanos = now;
        for (int shownFrames = 0; (shownFrames < MAX_FRAMES_PER_RENDER) && (now >= this.nextFrameNanos); shownFrames++) {
            ApngFrameUpdate update = activeStream.poll();
            if (update == null) {
                // The last frame of the last play stayed visible for its full delay, so the animation is over
                if (activeStream.isFinished()) {
                    this.handleCycleBoundary();
                    this.maxLoopsReached = true;
                }
                return;
            }
            if ((update.getPlay() != this.shownPlay) && !this.handleCycleBoundary()) {
                this.maxLoopsReached = true;
                activeStream.recycle(update);
                return;
            }
            this.nextFrameNanos += update.getDelayNanos();
            this.uploadAndRecycle(activeStream, update);
        }
    }

    protected void showFrame(@NotNull ApngFrameStream activeStream, @NotNull ApngFrameUpdate update, long now) {
        this.nextFrameNanos = now + update.getDelayNanos();
        // A frame that appears while paused starts its time once playback continues. An older pause time would add the whole pause to its time.
        this.pausedAtNanos = this.paused ? now : -1L;
        this.uploadAndRecycle(activeStream, update);
    }

    protected void uploadAndRecycle(@NotNull ApngFrameStream activeStream, @NotNull ApngFrameUpdate update) {
        try {
            if (!update.isContentUnchanged()) this.upload(update);
            this.frameShown = true;
            this.shownPlay = update.getPlay();
            if (this.pendingStartEvent && (update.getFrameIndex() == 0)) {
                this.pendingStartEvent = false;
                this.notifyAnimatedTextureStarted(this.willRestartAfterCurrentCycle());
            }
        } finally {
            activeStream.recycle(update);
        }
    }

    protected void upload(@NotNull ApngFrameUpdate update) {
        ApngGpuTexture texture = Objects.requireNonNull(this.gpuTexture, "The APNG texture was not created yet");
        ByteBuffer pixels = Objects.requireNonNull(update.getPixels(), "The APNG frame update has no pixels");
        int offset = 0;
        for (int i = 0; i < update.getRectangleCount(); i++) {
            int rectangleWidth = update.getRectangleWidth(i);
            int rectangleHeight = update.getRectangleHeight(i);
            int bytes = rectangleWidth * rectangleHeight * 4;
            texture.writePixels(pixels.slice(offset, bytes), update.getRectangleX(i), update.getRectangleY(i), rectangleWidth, rectangleHeight);
            offset += bytes;
        }
    }

    protected void ensureGpuTexture(int textureWidth, int textureHeight) {
        ApngGpuTexture texture = this.gpuTexture;
        if ((texture != null) && (texture.getWidth() == textureWidth) && (texture.getHeight() == textureHeight)) return;
        this.releaseGpuTexture();
        this.textureCounter++;
        Identifier location = Identifier.fromNamespaceAndPath("fancymenu", "dynamic/apng_" + this.uniqueId + "_" + this.textureCounter);
        texture = new ApngGpuTexture(location.toString(), textureWidth, textureHeight);
        Minecraft.getInstance().getTextureManager().register(location, texture);
        this.gpuTexture = texture;
        this.textureLocation = location;
    }

    protected void releaseGpuTexture() {
        Identifier location = this.textureLocation;
        ApngGpuTexture texture = this.gpuTexture;
        this.textureLocation = null;
        this.gpuTexture = null;
        // Releasing the texture manager entry also closes the texture
        if (location != null) {
            Minecraft.getInstance().getTextureManager().release(location);
        } else if (texture != null) {
            texture.close();
        }
    }

    protected void releaseRenderResources() {
        ApngFrameStream active = this.stream.getAndSet(null);
        if (active != null) active.close();
        ApngFrameStream playing = this.playingStream;
        this.playingStream = null;
        if (playing != null) playing.close();
        this.frameShown = false;
        this.releaseGpuTexture();
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
    public @NotNull AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL, WebUtils.WebResourceType.BUFFERED_ANIMATED_TEXTURE);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    @Override
    public boolean isReady() {
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded.get();
    }

    @Override
    public boolean isLoadingCompleted() {
        return !this.closed.get() && !this.loadingFailed.get() && this.decoded.get();
    }

    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed.get();
    }

    /**
     * Restarts the animation at its first frame. The render thread shows the first frame on its next frame, without waiting for it to get decoded again.
     */
    @Override
    public void reset() {
        if (this.closed.get()) return;
        this.maxLoopsReached = false;
        this.pendingStartEvent = true;
        this.cycles.set(0);
        this.paused = false;
        this.pausedAtNanos = -1L;
        this.restartRequested = true;
    }

    /**
     * Counts the finished play cycle and returns if the sequence should restart from its first frame.
     */
    protected boolean handleCycleBoundary() {
        int plays = this.numPlays.get();
        // Cycles of infinitely looping textures (plays <= 0) get counted too, so their completed play cycles are known as well
        int newCycles = this.cycles.incrementAndGet();
        boolean willRestart = (plays <= 0) || (newCycles < plays);
        this.notifyAnimatedTextureFinished(willRestart);
        if (willRestart) this.pendingStartEvent = true;
        return willRestart;
    }

    protected boolean willRestartAfterCurrentCycle() {
        int plays = this.numPlays.get();
        if (plays <= 0) return true;
        return (this.cycles.get() + 1) < plays;
    }

    private void notifyAnimatedTextureStarted(boolean willRestart) {
        if (!this.animated) return;
        if (Listeners.ON_ANIMATED_TEXTURE_STARTED_PLAYING.hasInstancesListening()) {
            Listeners.ON_ANIMATED_TEXTURE_STARTED_PLAYING.onAnimatedTextureStartedPlaying(this.resolveTextureSource(), this.resolveTextureSourceType(), willRestart);
        }
    }

    private void notifyAnimatedTextureFinished(boolean willRestart) {
        if (Listeners.ON_ANIMATED_TEXTURE_FINISHED_PLAYING.hasInstancesListening()) {
            Listeners.ON_ANIMATED_TEXTURE_FINISHED_PLAYING.onAnimatedTextureFinishedPlaying(this.resolveTextureSource(), this.resolveTextureSourceType(), willRestart);
        }
    }

    private String resolveTextureSource() {
        if (this.sourceURL != null) return this.sourceURL;
        if (this.sourceFile != null) return this.sourceFile.getPath();
        if (this.sourceLocation != null) return this.sourceLocation.toString();
        return "ERROR";
    }

    @NotNull
    protected String resolveTextureName() {
        String name = this.sourceName;
        return (name != null) ? name : this.resolveTextureSource();
    }

    private String resolveTextureSourceType() {
        if (this.sourceURL != null) return "WEB";
        if (this.sourceFile != null) return "LOCAL";
        if (this.sourceLocation != null) return "RESOURCE_LOCATION";
        return "UNKNOWN";
    }

    @Override
    public void play() {
        if (this.paused) this.paused = false;
    }

    @Override
    public boolean isPlaying() {
        return !this.closed.get() && !this.paused && !this.maxLoopsReached;
    }

    @Override
    public int getCompletedPlayCycles() {
        return this.cycles.get();
    }

    @Override
    public void pause() {
        if (this.paused || this.closed.get()) return;
        // Remember the time before the flag, so the render thread never sees the flag cleared without this time
        if (this.pausedAtNanos < 0L) this.pausedAtNanos = System.nanoTime();
        this.paused = true;
    }

    @Override
    public boolean isPaused() {
        return this.paused;
    }

    @Override
    public void stop() {
        this.reset();
    }

    @Override
    public boolean isClosed() {
        return this.closed.get();
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) return;
        this.sourceLocation = null;
        // Streams and the texture belong to the render thread
        TextureManagerReleaseDispatcher.dispatch(this::releaseRenderResources);
    }

}
