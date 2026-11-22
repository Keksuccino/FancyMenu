package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.TextureManagerReleaseDispatcher;
import de.keksuccino.fancymenu.util.resource.resources.texture.TextureManagerReleaser;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An animated image (or a still image) of a format like APNG or GIF, which plays its animation on a single texture. Subclasses only parse their format.<br>
 * The image gets parsed once, then {@link AnimatedFrameStream} decodes and composes its frames in the background, a few frames ahead of playback.
 * The render thread shows frames on time while the texture gets rendered and only uploads the pixels that changed since the frame before.<br>
 * Playback only advances while the texture gets rendered, like with AFMA textures. A texture that wasn't rendered for a while continues with the frame it showed.<br><br>
 *
 * Frames read their compressed data from a file whenever they get decoded: local images read their own file, images that only come as stream
 * (resources, web images and other streams) get copied to a temporary file first, which gets deleted once the image is not needed anymore.
 * If the file changes while the animation plays, the animation stops at the frame it shows. Closing the texture closes the file right away,
 * so files can get moved right after closing textures, like when switching profiles.
 */
public abstract class AnimatedImageTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();
    /** Playback catches up when it falls behind, like when decoding was too slow for a moment, unless it fell behind by more than this. Then it continues from now. **/
    private static final long MAX_PLAYBACK_LAG_NANOS = 250_000_000L;
    /** Most frames that get shown per rendered frame, for animations faster than the frame rate of the game. **/
    private static final int MAX_FRAMES_PER_RENDER = 8;

    /** Name of the image format, like "APNG" or "GIF", for log messages. **/
    @NotNull
    protected final String formatName;
    /** Lowercase format name, used in texture locations and temporary file names. **/
    @NotNull
    private final String formatId;
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile int width = 10;
    protected volatile int height = 10;
    @Nullable
    protected volatile ResourceLocation sourceLocation;
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
    /** How many times the animation should play. Values <= 0 mean infinite loops. **/
    protected final AtomicInteger numPlays = new AtomicInteger(0);
    protected volatile boolean animated = false;
    protected volatile boolean maxLoopsReached = false;
    protected volatile boolean pendingStartEvent = true;
    protected volatile boolean paused = false;
    protected volatile boolean restartRequested = false;
    /** The stream of the current image, replaced when the image gets loaded again and cleared once a static image is on its texture. **/
    protected final AtomicReference<AnimatedFrameStream> stream = new AtomicReference<>();
    /** Guards {@link #loadingSource}. Local files get opened while holding it, so closing the texture never misses a file that is just getting opened. **/
    protected final Object loadingSourceLock = new Object();
    /** The source a loading thread parses right now, which closing the texture closes right away. Guarded by {@link #loadingSourceLock}. **/
    @Nullable
    protected AnimatedImageSource loadingSource;

    // Only used on the render thread
    @Nullable
    protected AnimatedFrameStream playingStream;
    @Nullable
    protected AnimatedGpuTexture gpuTexture;
    @Nullable
    protected ResourceLocation textureLocation;
    protected int textureCounter = 0;
    protected boolean frameShown = false;
    protected int shownPlay = 0;
    protected long nextFrameNanos = 0L;
    /** When the paused texture got rendered first, so the visible frame can continue its remaining time when it plays again. -1 while it didn't get rendered paused. **/
    protected long pausedAtNanos = -1L;

    /**
     * @param formatName Name of the image format, like "APNG" or "GIF".
     */
    protected AnimatedImageTexture(@NotNull String formatName) {
        this.formatName = Objects.requireNonNull(formatName);
        this.formatId = formatName.toLowerCase(Locale.ROOT);
    }

    /**
     * Parses the image. Takes ownership of the source: the returned image closes it when it gets closed, and if parsing fails, the source has to get closed right away.
     *
     * @param sourceName Name of the image source, only used for log messages.
     * @throws IOException If the source is no valid image of the format or reading it failed.
     */
    @NotNull
    protected abstract AnimatedImage readImage(@NotNull AnimatedImageSource source, @NotNull String sourceName) throws IOException;

    /**
     * Loads the image from a resource. Opens the resource right away and parses it in the background.
     */
    protected void loadFromLocation(@NotNull ResourceLocation location) {
        Objects.requireNonNull(location);
        this.sourceLocation = location;
        try {
            this.loadFromStream(Minecraft.getInstance().getResourceManager().open(location), location.toString());
        } catch (Exception ex) {
            this.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read {} image from ResourceLocation: {}", this.formatName, location, ex);
        }
    }

    /**
     * Loads the image from a local file in the background, which frames then read their data from while the image plays.
     */
    protected void loadFromFile(@NotNull File file) {
        Objects.requireNonNull(file);
        this.sourceFile = file;
        if (!file.isFile()) {
            this.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read {} image from file! File not found: {}", this.formatName, file.getPath());
            return;
        }
        FancyMenuThreads.startDaemonThread(() -> this.populate(() -> AnimatedImageFileSource.open(file.toPath()), file.getPath()), this.getClass().getSimpleName() + "-LocalLoader");
    }

    /**
     * Downloads the image in the background.
     */
    protected void loadFromWeb(@NotNull String url) {
        Objects.requireNonNull(url);
        this.sourceURL = url;
        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(url)) {
            this.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read {} image from URL! Invalid URL: {}", this.formatName, url);
            return;
        }
        FancyMenuThreads.startDaemonThread(() -> {
            try {
                this.populate(WebUtils.openResourceStreamOrThrow(url, WebUtils.WebResourceType.BUFFERED_ANIMATED_TEXTURE), url);
            } catch (Exception ex) {
                this.publishFailure();
                // Shutting down the client cancels web loads, which is no error
                if (!ClientShutdownHandler.isShuttingDown()) LOGGER.error("[FANCYMENU] Failed to read {} image from URL: {}", this.formatName, url, ex);
            }
        }, this.getClass().getSimpleName() + "-WebLoader");
    }

    /**
     * Loads the image from a stream in the background. Closes the stream.
     */
    protected void loadFromStream(@NotNull InputStream in, @Nullable String name) {
        Objects.requireNonNull(in);
        FancyMenuThreads.startDaemonThread(() -> this.populate(in, (name != null) ? name : "[Generic InputStream Source]"), this.getClass().getSimpleName() + "-StreamLoader");
    }

    /**
     * Copies the stream to a temporary file, parses the image from there and starts decoding its frames. Closes the stream. Closed textures don't copy the stream.<br>
     * The copy gets made before the copy gets tracked as loading source, so closing the texture never waits for a slow stream, like a download.
     */
    protected void populate(@NotNull InputStream in, @NotNull String name) {
        AnimatedImageSource copy;
        try {
            if (this.closed.get()) return;
            copy = AnimatedImageFileSource.copyToTempFile(in, resolveTempDirectory(), this.formatId);
        } catch (Exception ex) {
            this.sourceName = name;
            this.publishFailure();
            // Shutting down the client cancels web loads, so copying their stream fails then, which is no error
            if (!ClientShutdownHandler.isShuttingDown()) LOGGER.error("[FANCYMENU] Failed to read {} image: {}", this.formatName, name, ex);
            return;
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                LOGGER.warn("[FANCYMENU] Failed to close the stream of {} image: {}", this.formatName, name, ex);
            }
        }
        // A texture that got closed while the stream got copied doesn't take the copy, which deletes it
        if (!this.populate(() -> copy, name)) copy.close();
    }

    /**
     * Where streams get copied to. Resolved for every copy instead of once, so the directory comes back if it got deleted, and loading this class doesn't initialize FancyMenu.
     */
    @NotNull
    private static Path resolveTempDirectory() {
        return FileUtils.createDirectory(new File(FancyMenu.TEMP_DATA_DIR, "/animated_textures")).toPath();
    }

    /**
     * Opens the source, parses the image from it and starts decoding its frames. Closed textures don't open the source.<br>
     * The source is the {@link #loadingSource} while it gets parsed, so closing the texture closes it right away, which makes parsing fail without an error.
     *
     * @return If the source got opened. The texture owns it then.
     */
    protected boolean populate(@NotNull SourceOpener sourceOpener, @NotNull String name) {
        this.sourceName = name;
        AnimatedImageSource source;
        try {
            synchronized (this.loadingSourceLock) {
                if (this.closed.get()) return false;
                source = sourceOpener.open();
                this.loadingSource = source;
            }
        } catch (Exception ex) {
            this.publishFailure();
            LOGGER.error("[FANCYMENU] Failed to read {} image: {}", this.formatName, name, ex);
            return false;
        }
        try {
            this.applyImage(this.readImage(source, name), name);
        } catch (Exception ex) {
            this.publishFailure();
            if (!this.closed.get()) LOGGER.error("[FANCYMENU] Failed to read {} image: {}", this.formatName, name, ex);
        } finally {
            synchronized (this.loadingSourceLock) {
                this.loadingSource = null;
            }
        }
        return true;
    }

    /**
     * Starts playing the image. The texture takes ownership of the image.
     */
    protected void applyImage(@NotNull AnimatedImage image, @NotNull String name) {
        this.applyStream(image, new AnimatedFrameStream(image, name));
    }

    /**
     * Starts playing the stream of a newly parsed image. The texture takes ownership of the stream.
     */
    protected void applyStream(@NotNull AnimatedImage image, @NotNull AnimatedFrameStream newStream) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.aspectRatio = new AspectRatio(image.getWidth(), image.getHeight());
        this.numPlays.set(image.getNumPlays());
        this.animated = image.isAnimated();
        AnimatedFrameStream previous = this.stream.getAndSet(newStream);
        // A close() that ran before this stream became current didn't see it, so its source gets closed here, before the stream decodes anything
        if (this.closed.get()) newStream.closeSource();
        newStream.start();
        this.loadingFailed.set(false);
        this.decoded.set(true);
        if (previous != null) {
            // The file of the replaced image gets closed right away, its stream belongs to the render thread, which then also notices the new stream and switches to it
            previous.closeSource();
            TextureManagerReleaseDispatcher.dispatch(previous::close);
        }
        if (this.closed.get()) this.releaseRenderResourcesOnRenderThread();
    }

    protected void publishFailure() {
        this.loadingFailed.set(true);
        this.decoded.set(true);
    }

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        AnimatedFrameStream activeStream = this.stream.get();
        // Static images once their texture shows them, and animations that can't play any further, need nothing but their texture
        if ((activeStream == null) && this.frameShown) {
            if (this.animated) {
                // The stream of an animation only goes away when it can't play any further
                this.maxLoopsReached = true;
            } else {
                this.markStillImagePlayed();
            }
            return this.textureLocation;
        }
        // Returning NULL while loading or after failing lets callers show their fallback, like they do for other images
        if ((activeStream == null) || this.loadingFailed.get()) return null;
        if (activeStream != this.playingStream) this.switchStream(activeStream);
        activeStream.markActive();
        try {
            this.updatePlayback(activeStream, this.nanoTime());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] {} playback failed: {}", this.formatName, this.resolveTextureName(), ex);
            this.publishFailure();
            return null;
        }
        if (activeStream.hasFailed()) {
            if (this.frameShown) {
                // The stream can fail after it showed frames when the file of the image changes, then the frame the texture shows stays, but the animation is over
                this.releaseStream(activeStream);
                this.maxLoopsReached = true;
                return this.textureLocation;
            }
            this.publishFailure();
            return null;
        }
        return this.frameShown ? this.textureLocation : null;
    }

    /**
     * The clock playback runs on.
     */
    protected long nanoTime() {
        return System.nanoTime();
    }

    /**
     * Starts playing the stream of a newly loaded image from its beginning.
     */
    protected void switchStream(@NotNull AnimatedFrameStream newStream) {
        AnimatedFrameStream previous = this.playingStream;
        if (previous != null) previous.close();
        this.playingStream = newStream;
        this.frameShown = false;
        this.shownPlay = 0;
        this.restartRequested = false;
        this.maxLoopsReached = false;
        this.pendingStartEvent = true;
        this.cycles.set(0);
    }

    protected void updatePlayback(@NotNull AnimatedFrameStream activeStream, long now) {
        if (this.restartRequested) {
            this.restartRequested = false;
            if (this.frameShown) {
                AnimatedFrameUpdate first = activeStream.restart();
                this.shownPlay = 0;
                this.nextFrameNanos = now;
                if (first != null) this.showFrame(activeStream, first, now);
            }
        }
        if (!this.frameShown) {
            AnimatedFrameUpdate first = activeStream.poll();
            if (first == null) return;
            this.ensureGpuTexture(activeStream.getWidth(), activeStream.getHeight());
            this.showFrame(activeStream, first, now);
            if (!this.animated) {
                // The texture keeps the image, so the stream and the file are not needed anymore
                this.releaseStream(activeStream);
                this.markStillImagePlayed();
            }
            return;
        }
        if (this.maxLoopsReached) return;
        if (this.paused) {
            /*
             * Only the time the texture gets rendered while paused counts as pause, which the visible frame gets once playback continues.
             * Several users can share one texture, and one of them can pause it while another one keeps showing it, like widgets that pause the background of
             * their hidden states, which can be the shown background of other widgets. Counting the time between their pause and play calls would stall it then.
             */
            if (this.pausedAtNanos < 0L) this.pausedAtNanos = now;
            return;
        }
        long pausedAt = this.pausedAtNanos;
        if (pausedAt >= 0L) {
            // The visible frame continues with the time it had left when it got shown paused
            this.nextFrameNanos += Math.max(0L, now - pausedAt);
            this.pausedAtNanos = -1L;
        }
        if ((now - this.nextFrameNanos) > MAX_PLAYBACK_LAG_NANOS) this.nextFrameNanos = now;
        // Of all frames that are due in one render only the last one gets seen, so a frame needs no upload if the frame after it replaces the whole canvas
        AnimatedFrameUpdate due = null;
        for (int shownFrames = 0; (shownFrames < MAX_FRAMES_PER_RENDER) && (now >= this.nextFrameNanos); shownFrames++) {
            AnimatedFrameUpdate update = activeStream.poll();
            if (update == null) {
                // The last frame of the last play stayed visible for its full delay, so the animation is over
                if (activeStream.isFinished()) this.finishLastPlay();
                break;
            }
            // Frames that got dropped while nobody rendered the texture can skip whole plays, which count as finished too
            int finishedPlays = update.getPlay() - this.shownPlay;
            if ((finishedPlays > 0) && !this.handleCycleBoundary(finishedPlays)) {
                activeStream.recycle(update);
                break;
            }
            this.shownPlay = update.getPlay();
            this.nextFrameNanos += update.getDelayNanos();
            if (due != null) this.showUpdate(activeStream, due, !update.replacesCanvas(activeStream.getWidth(), activeStream.getHeight()));
            due = update;
        }
        if (due != null) this.showUpdate(activeStream, due, true);
    }

    /**
     * Closes the stream while the texture keeps showing the frame it shows, which also closes the file of the image.
     */
    protected void releaseStream(@NotNull AnimatedFrameStream activeStream) {
        activeStream.close();
        this.stream.compareAndSet(activeStream, null);
        this.playingStream = null;
    }

    /**
     * Still images count as played once as soon as their texture shows them, like an animation with a single frame that plays once,
     * so everything that waits for a texture to finish playing (like game intros) can continue. Restarting them counts the play again.
     * They fire no animation events, because they have no animation.
     */
    protected void markStillImagePlayed() {
        if (this.maxLoopsReached) return;
        this.cycles.set(1);
        this.maxLoopsReached = true;
    }

    protected void showFrame(@NotNull AnimatedFrameStream activeStream, @NotNull AnimatedFrameUpdate update, long now) {
        this.nextFrameNanos = now + update.getDelayNanos();
        // A frame that appears while paused starts its time once playback continues. An older pause time would add the whole pause to its time.
        this.pausedAtNanos = this.paused ? now : -1L;
        this.shownPlay = update.getPlay();
        this.showUpdate(activeStream, update, true);
    }

    /**
     * @param upload If the pixels of the update have to get uploaded, which they don't when a later update of the same render replaces the whole canvas anyway.
     */
    protected void showUpdate(@NotNull AnimatedFrameStream activeStream, @NotNull AnimatedFrameUpdate update, boolean upload) {
        try {
            if (upload && !update.isContentUnchanged()) this.upload(update);
            this.frameShown = true;
            if (this.pendingStartEvent && (update.getFrameIndex() == 0)) {
                this.pendingStartEvent = false;
                this.notifyAnimatedTextureStarted(this.willRestartAfterCurrentCycle());
            }
        } finally {
            activeStream.recycle(update);
        }
    }

    protected void upload(@NotNull AnimatedFrameUpdate update) {
        AnimatedGpuTexture texture = Objects.requireNonNull(this.gpuTexture, "The texture of the animated image was not created yet");
        ByteBuffer pixels = Objects.requireNonNull(update.getPixels(), "The frame update has no pixels");
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
        AnimatedGpuTexture texture = this.gpuTexture;
        if ((texture != null) && (texture.getWidth() == textureWidth) && (texture.getHeight() == textureHeight)) return;
        this.releaseGpuTexture();
        this.textureCounter++;
        ResourceLocation location = new ResourceLocation("fancymenu", "dynamic/" + this.formatId + "_" + this.uniqueId + "_" + this.textureCounter);
        texture = new AnimatedGpuTexture(location.toString(), textureWidth, textureHeight);
        Minecraft.getInstance().getTextureManager().register(location, texture);
        this.gpuTexture = texture;
        this.textureLocation = location;
    }

    protected void releaseGpuTexture() {
        ResourceLocation location = this.textureLocation;
        AnimatedGpuTexture texture = this.gpuTexture;
        this.textureLocation = null;
        this.gpuTexture = null;
        // Releasing the texture manager entry also frees the GPU texture
        if (location != null) {
            TextureManagerReleaser.release(location);
        } else if (texture != null) {
            texture.close();
        }
    }

    /**
     * Releases the streams and the texture on the render thread, which they belong to: right away when called on the render thread, otherwise before its next tick.
     */
    protected void releaseRenderResourcesOnRenderThread() {
        TextureManagerReleaseDispatcher.dispatch(this::releaseRenderResources);
    }

    protected void releaseRenderResources() {
        AnimatedFrameStream active = this.stream.getAndSet(null);
        if (active != null) active.close();
        AnimatedFrameStream playing = this.playingStream;
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
        if (this.sourceURL != null) return WebUtils.openResourceStreamOrThrow(this.sourceURL, WebUtils.WebResourceType.BUFFERED_ANIMATED_TEXTURE);
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
        this.restartRequested = true;
    }

    /**
     * Counts finished play cycles and returns if the sequence should restart from its first frame.
     * Playback ends before the listeners get notified when it doesn't restart, so listeners can restart it.
     */
    protected boolean handleCycleBoundary(int finishedPlays) {
        int plays = this.numPlays.get();
        // Cycles of infinitely looping textures (plays <= 0) get counted too, so their completed play cycles are known as well
        int newCycles = this.cycles.addAndGet(finishedPlays);
        boolean willRestart = (plays <= 0) || (newCycles < plays);
        if (willRestart) {
            this.pendingStartEvent = true;
        } else {
            this.maxLoopsReached = true;
        }
        this.notifyAnimatedTextureFinished(willRestart);
        return willRestart;
    }

    /**
     * Ends playback after the last frame of the last play. Plays that got skipped while nobody rendered the texture count as finished too.
     * Playback ends before the listeners get notified, so listeners can restart it.
     */
    protected void finishLastPlay() {
        this.cycles.set(Math.max(this.cycles.get() + 1, this.numPlays.get()));
        this.maxLoopsReached = true;
        this.notifyAnimatedTextureFinished(false);
    }

    protected boolean willRestartAfterCurrentCycle() {
        int plays = this.numPlays.get();
        if (plays <= 0) return true;
        return (this.cycles.get() + 1) < plays;
    }

    protected void notifyAnimatedTextureStarted(boolean willRestart) {
        if (!this.animated) return;
        if (Listeners.ON_ANIMATED_TEXTURE_STARTED_PLAYING.hasInstancesListening()) {
            Listeners.ON_ANIMATED_TEXTURE_STARTED_PLAYING.onAnimatedTextureStartedPlaying(this.resolveTextureSource(), this.resolveTextureSourceType(), willRestart);
        }
    }

    protected void notifyAnimatedTextureFinished(boolean willRestart) {
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
        return !this.closed.get() && !this.loadingFailed.get() && !this.paused && !this.maxLoopsReached;
    }

    @Override
    public int getCompletedPlayCycles() {
        return this.cycles.get();
    }

    @Override
    public void pause() {
        if (this.closed.get()) return;
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

    /**
     * Closes the file of the image right away on any thread, so it is never open anymore once this returns, also while the loading thread still parses it
     * or a decoding task reads a frame from it. Streams and the texture get released on the render thread.
     */
    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) return;
        this.sourceLocation = null;
        // The source of the current stream gets closed before the loading source, because a loading thread first makes its stream current and then stops tracking its source
        AnimatedFrameStream activeStream = this.stream.get();
        if (activeStream != null) activeStream.closeSource();
        AnimatedImageSource parsingSource;
        synchronized (this.loadingSourceLock) {
            parsingSource = this.loadingSource;
        }
        if (parsingSource != null) parsingSource.close();
        this.releaseRenderResourcesOnRenderThread();
    }

    /**
     * Opens the source an image gets parsed from.
     */
    @FunctionalInterface
    protected interface SourceOpener {

        @NotNull
        AnimatedImageSource open() throws IOException;

    }

}
