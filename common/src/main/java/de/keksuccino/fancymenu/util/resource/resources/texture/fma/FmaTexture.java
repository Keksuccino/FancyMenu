package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FmaTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PREFETCH_QUEUE_SIZE = 4;
    private static final long MIN_FRAME_DELAY_MS = 10L;
    private static final long INACTIVITY_TIMEOUT_MS = 10000L;
    private static final long IDLE_SLEEP_MS = 10L;

    protected volatile int width = 10;
    protected volatile int height = 10;
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);

    protected volatile ResourceLocation sourceLocation;
    protected volatile File sourceFile;
    protected volatile String sourceURL;

    protected volatile long lastResourceLocationCall = -1L;
    protected final AtomicBoolean decoded = new AtomicBoolean(false);
    protected final AtomicBoolean loadingCompleted = new AtomicBoolean(false);
    protected final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    protected final AtomicBoolean closed = new AtomicBoolean(false);

    protected final AtomicInteger cycles = new AtomicInteger(0);
    /** How many times the FMA should loop. Value <= 0 means infinite loops. **/
    protected final AtomicInteger numPlays = new AtomicInteger(0);

    protected volatile boolean maxLoopsReached = false;
    protected volatile boolean pendingStartEvent = true;
    protected volatile boolean introFinishedPlaying = false;
    protected volatile boolean playRequested = true;
    protected volatile boolean pausedRequested = false;

    protected volatile int frameCount = 0;
    protected volatile int introFrameCount = 0;
    protected volatile long[] frameDelaysMs = new long[0];
    protected volatile long[] introFrameDelaysMs = new long[0];

    @Nullable
    protected volatile FmaDecoder decoder = null;

    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();

    // Single persistent texture that receives frame uploads.
    @Nullable
    protected volatile DynamicTexture streamingTexture = null;
    @Nullable
    protected volatile ResourceLocation streamingResourceLocation = null;

    // Streaming state.
    protected final Object streamStateLock = new Object();
    @NotNull
    protected final ArrayDeque<DecodedFrame> prefetchedFrames = new ArrayDeque<>();
    @NotNull
    protected final AtomicReference<DecodedFrame> pendingUploadFrame = new AtomicReference<>(null);
    protected final AtomicInteger streamGeneration = new AtomicInteger(0);

    @Nullable
    protected volatile Thread streamThread = null;
    protected volatile boolean playbackInitialized = false;
    protected volatile boolean playbackIntro = false;
    protected volatile int playbackIndex = -1;
    protected volatile long playbackFrameStartMs = 0L;
    protected volatile long playbackFrameDelayMs = MIN_FRAME_DELAY_MS;

    protected volatile boolean decodeIntro = false;
    protected volatile int decodeIndex = 0;

    @NotNull
    public static FmaTexture location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static FmaTexture location(@NotNull ResourceLocation location, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(location);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();
        texture.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read FMA image from ResourceLocation: " + location, ex);
        }

        return texture;
    }

    @NotNull
    public static FmaTexture local(@NotNull File fmaFile) {
        return local(fmaFile, null);
    }

    @NotNull
    public static FmaTexture local(@NotNull File fmaFile, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(fmaFile);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();
        texture.sourceFile = fmaFile;

        if (!fmaFile.isFile()) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read FMA image from file! File not found: " + fmaFile.getPath());
            return texture;
        }

        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(fmaFile);
                of(in, fmaFile.getPath(), texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read FMA image from file: " + fmaFile.getPath(), ex);
            }
        }).start();

        return texture;
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl) {
        return web(fmaUrl, null);
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(fmaUrl);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();
        texture.sourceURL = fmaUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(fmaUrl)) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read FMA image from URL! Invalid URL: " + fmaUrl);
            return texture;
        }

        // Stream directly into the decoder (decoder spools to a temp archive file internally).
        new Thread(() -> {
            InputStream in = null;
            try {
                in = WebUtils.openResourceStream(fmaUrl);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, fmaUrl, texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read FMA image from URL: " + fmaUrl, ex);
                CloseableUtils.closeQuietly(in);
            }
        }).start();

        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in, @Nullable String fmaTextureName, @Nullable FmaTexture writeTo) {
        Objects.requireNonNull(in);
        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();

        new Thread(() -> {
            populateTexture(texture, in, (fmaTextureName != null) ? fmaTextureName : "[Generic InputStream Source]");
            if (texture.closed.get()) {
                MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            }
        }).start();

        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected static void populateTexture(@NotNull FmaTexture texture, @NotNull InputStream in, @NotNull String fmaTextureName) {
        DecodedFmaImage decodedImage = null;
        if (!texture.closed.get()) {
            decodedImage = decodeFma(in, fmaTextureName);
            if (decodedImage == null) {
                texture.decoded.set(true);
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read FMA image, because DecodedFmaImage was NULL: {}", fmaTextureName);
                CloseableUtils.closeQuietly(in);
                return;
            }

            try {
                texture.configureStreamingState(decodedImage);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to initialize streaming state for FMA image: " + fmaTextureName, ex);
            }

            texture.decoded.set(true);
        }

        CloseableUtils.closeQuietly(in);
    }

    protected FmaTexture() {
    }

    protected void configureStreamingState(@NotNull DecodedFmaImage decodedImage) {
        FmaDecoder previousDecoder = this.decoder;
        if ((previousDecoder != null) && (previousDecoder != decodedImage.decoder())) {
            CloseableUtils.closeQuietly(previousDecoder);
        }
        this.decoder = decodedImage.decoder();
        this.frameCount = this.decoder != null ? this.decoder.getFrameCount() : 0;
        this.introFrameCount = (this.decoder != null && this.decoder.hasIntroFrames()) ? this.decoder.getIntroFrameCount() : 0;

        if (this.frameCount <= 0) {
            throw new IllegalStateException("FMA image has no usable frames");
        }

        this.width = decodedImage.imageWidth();
        this.height = decodedImage.imageHeight();
        this.aspectRatio = new AspectRatio(decodedImage.imageWidth(), decodedImage.imageHeight());
        this.numPlays.set(decodedImage.numPlays());

        FmaDecoder.FmaMetadata metadata = Objects.requireNonNull(this.decoder.getMetadata(), "FmaDecoder returned NULL for metadata!");

        long[] normalDelays = new long[this.frameCount];
        for (int i = 0; i < normalDelays.length; i++) {
            normalDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, false));
        }

        long[] introDelays = new long[this.introFrameCount];
        for (int i = 0; i < introDelays.length; i++) {
            introDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, true));
        }

        this.frameDelaysMs = normalDelays;
        this.introFrameDelaysMs = introDelays;

        this.loadingCompleted.set(true);
        this.loadingFailed.set(false);

        this.requestPlaybackReset();
    }

    protected static long sanitizeDelay(long delayMs) {
        return Math.max(MIN_FRAME_DELAY_MS, delayMs);
    }

    protected void startTickerIfNeeded() {
        if (this.closed.get() || this.loadingFailed.get() || !this.decoded.get()) return;

        synchronized (this.streamStateLock) {
            Thread running = this.streamThread;
            if ((running != null) && running.isAlive()) return;

            int generation = this.streamGeneration.get();
            Thread stream = new Thread(() -> this.streamLoop(generation), "FancyMenu-FmaStream-" + this.uniqueId);
            stream.setDaemon(true);
            this.streamThread = stream;
            stream.start();
        }
    }

    protected void streamLoop(int generation) {
        while (!this.closed.get() && (generation == this.streamGeneration.get())) {
            if (!this.playRequested || this.maxLoopsReached) {
                sleepQuietly(IDLE_SLEEP_MS);
                continue;
            }
            if (this.pausedRequested && this.playbackInitialized) {
                sleepQuietly(IDLE_SLEEP_MS);
                continue;
            }

            long now = System.currentTimeMillis();
            if ((this.lastResourceLocationCall > 0L) && ((this.lastResourceLocationCall + INACTIVITY_TIMEOUT_MS) < now)) {
                synchronized (this.streamStateLock) {
                    this.clearPrefetchedFramesLocked();
                }
                sleepQuietly(100L);
                continue;
            }

            try {
                if (!this.playbackInitialized) {
                    if (!this.initializeFirstFrame(generation)) {
                        sleepQuietly(IDLE_SLEEP_MS);
                    }
                    continue;
                }

                this.fillPrefetchQueue(generation);

                long elapsed = now - this.playbackFrameStartMs;
                long delay = Math.max(MIN_FRAME_DELAY_MS, this.playbackFrameDelayMs);
                if (elapsed < delay) {
                    sleepQuietly(Math.min(IDLE_SLEEP_MS, delay - elapsed));
                    continue;
                }

                if (this.isAtNormalCycleBoundary()) {
                    boolean willRestart = this.handleCycleBoundary();
                    if (!willRestart) {
                        this.maxLoopsReached = true;
                        this.playRequested = false;
                        continue;
                    }
                }

                DecodedFrame next = this.pollPrefetchedFrame();
                if (next == null) {
                    sleepQuietly(IDLE_SLEEP_MS);
                    continue;
                }
                if (generation != this.streamGeneration.get()) {
                    next.close();
                    break;
                }

                boolean switchedIntroToNormal = this.playbackIntro && !next.intro;
                if (switchedIntroToNormal) {
                    this.introFinishedPlaying = true;
                }

                this.playbackIntro = next.intro;
                this.playbackIndex = next.index;
                this.playbackFrameDelayMs = sanitizeDelay(next.delayMs);
                this.playbackFrameStartMs = now;

                this.publishDecodedFrame(next);
                this.maybeEmitStartEvent(next.intro, next.index);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] An error happened in the streaming thread of an FMA texture!", ex);
                sleepQuietly(50L);
            }
        }

        synchronized (this.streamStateLock) {
            if (generation == this.streamGeneration.get()) {
                this.streamThread = null;
            }
        }
    }

    protected boolean initializeFirstFrame(int generation) {
        this.fillPrefetchQueue(generation);

        DecodedFrame first = this.pollPrefetchedFrame();
        if (first == null) return false;
        if (generation != this.streamGeneration.get()) {
            first.close();
            return false;
        }

        long now = System.currentTimeMillis();
        this.playbackInitialized = true;
        this.playbackIntro = first.intro;
        this.playbackIndex = first.index;
        this.playbackFrameStartMs = now;
        this.playbackFrameDelayMs = sanitizeDelay(first.delayMs);

        this.publishDecodedFrame(first);
        this.maybeEmitStartEvent(first.intro, first.index);
        this.loadingCompleted.set(true);
        return true;
    }

    protected void fillPrefetchQueue(int generation) {
        synchronized (this.streamStateLock) {
            while (!this.closed.get() && (generation == this.streamGeneration.get()) && (this.prefetchedFrames.size() < PREFETCH_QUEUE_SIZE)) {
                DecodedFrame decodedFrame = this.decodeNextFrame();
                if (decodedFrame == null) {
                    break;
                }
                if (generation != this.streamGeneration.get()) {
                    decodedFrame.close();
                    break;
                }
                this.prefetchedFrames.addLast(decodedFrame);
            }
        }
    }

    @Nullable
    protected DecodedFrame decodeNextFrame() {
        boolean intro = this.decodeIntro;
        int index = this.decodeIndex;

        long delay = this.resolveFrameDelay(intro, index);
        NativeImage frameImage = this.decodeFrameImage(intro, index);
        if (frameImage == null) {
            if (this.loadingCompleted.get()) {
                this.loadingFailed.set(true);
            }
            return null;
        }

        this.advanceDecodeCursor();
        return new DecodedFrame(intro, index, delay, frameImage);
    }

    protected long resolveFrameDelay(boolean intro, int index) {
        if (intro) {
            if ((index >= 0) && (index < this.introFrameDelaysMs.length)) {
                return this.introFrameDelaysMs[index];
            }
            return MIN_FRAME_DELAY_MS;
        }

        if ((index >= 0) && (index < this.frameDelaysMs.length)) {
            return this.frameDelaysMs[index];
        }

        return MIN_FRAME_DELAY_MS;
    }

    @Nullable
    protected NativeImage decodeFrameImage(boolean intro, int index) {
        FmaDecoder activeDecoder = this.decoder;
        if (activeDecoder == null) return null;

        try {
            InputStream frameInput = intro ? activeDecoder.getIntroFrame(index) : activeDecoder.getFrame(index);
            if (frameInput == null) return null;

            try (InputStream closeableInput = frameInput) {
                return NativeImage.read(closeableInput);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode {} frame {} of FMA stream", intro ? "intro" : "normal", index, ex);
            return null;
        }
    }

    protected void advanceDecodeCursor() {
        if (this.decodeIntro) {
            if ((this.decodeIndex + 1) < this.introFrameCount) {
                this.decodeIndex++;
                return;
            }

            this.decodeIntro = false;
            this.decodeIndex = 0;
            return;
        }

        if (this.frameCount <= 0) {
            this.decodeIndex = 0;
            return;
        }

        if ((this.decodeIndex + 1) < this.frameCount) {
            this.decodeIndex++;
        } else {
            this.decodeIndex = 0;
        }
    }

    @Nullable
    protected DecodedFrame pollPrefetchedFrame() {
        synchronized (this.streamStateLock) {
            return this.prefetchedFrames.pollFirst();
        }
    }

    protected void clearPrefetchedFramesLocked() {
        DecodedFrame frame;
        while ((frame = this.prefetchedFrames.pollFirst()) != null) {
            frame.close();
        }
    }

    protected void publishDecodedFrame(@NotNull DecodedFrame nextFrame) {
        DecodedFrame oldPending = this.pendingUploadFrame.getAndSet(nextFrame);
        if (oldPending != null) {
            oldPending.close();
        }
    }

    protected void clearPendingUploadFrame() {
        DecodedFrame pending = this.pendingUploadFrame.getAndSet(null);
        if (pending != null) {
            pending.close();
        }
    }

    protected boolean isAtNormalCycleBoundary() {
        return this.playbackInitialized && !this.playbackIntro && (this.frameCount > 0) && (this.playbackIndex == (this.frameCount - 1));
    }

    protected boolean handleCycleBoundary() {
        int plays = this.numPlays.get();
        if (plays > 0) {
            int newCycles = this.cycles.incrementAndGet();
            boolean willRestart = newCycles < plays;
            this.notifyAnimatedTextureFinished(willRestart);
            if (willRestart) {
                this.pendingStartEvent = true;
            }
            return willRestart;
        }

        this.notifyAnimatedTextureFinished(true);
        this.pendingStartEvent = true;
        return true;
    }

    protected void maybeEmitStartEvent(boolean isIntroFrame, int frameIndex) {
        if (!this.pendingStartEvent) return;

        boolean isFirstFrame;
        if (isIntroFrame) {
            isFirstFrame = (frameIndex == 0) && !this.introFinishedPlaying;
        } else if (this.introFrameCount > 0) {
            isFirstFrame = this.introFinishedPlaying && (frameIndex == 0);
        } else {
            isFirstFrame = (frameIndex == 0);
        }

        if (!isFirstFrame) return;

        this.pendingStartEvent = false;
        this.notifyAnimatedTextureStarted(this.willRestartAfterCurrentCycle());
    }

    protected boolean willRestartAfterCurrentCycle() {
        int plays = this.numPlays.get();
        if (plays <= 0) return true;
        return (this.cycles.get() + 1) < plays;
    }

    protected void requestPlaybackReset() {
        this.streamGeneration.incrementAndGet();

        this.cycles.set(0);
        this.maxLoopsReached = false;
        this.pendingStartEvent = true;
        this.introFinishedPlaying = this.introFrameCount <= 0;
        this.playbackInitialized = false;
        this.playbackIntro = this.introFrameCount > 0;
        this.playbackIndex = -1;
        this.playbackFrameStartMs = 0L;
        this.playbackFrameDelayMs = MIN_FRAME_DELAY_MS;
        this.decodeIntro = this.introFrameCount > 0;
        this.decodeIndex = 0;

        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }

        this.clearPendingUploadFrame();

        Thread running = this.streamThread;
        if (running != null) {
            running.interrupt();
        }
        this.streamThread = null;
    }

    protected void uploadPendingFrameToTexture() {
        DecodedFrame frame = this.pendingUploadFrame.getAndSet(null);
        if (frame == null) return;
        if (frame.nativeImage == null) {
            frame.close();
            return;
        }

        try {
            DynamicTexture currentTexture = this.streamingTexture;

            if (currentTexture == null) {
                this.streamingTexture = new DynamicTexture(frame.nativeImage);
                frame.nativeImage = null;
                this.streamingResourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_fma_stream_" + this.uniqueId, this.streamingTexture);
                return;
            }

            NativeImage destinationPixels = currentTexture.getPixels();
            if ((destinationPixels == null)
                    || (destinationPixels.getWidth() != frame.nativeImage.getWidth())
                    || (destinationPixels.getHeight() != frame.nativeImage.getHeight())) {
                currentTexture.close();
                this.streamingTexture = new DynamicTexture(frame.nativeImage);
                frame.nativeImage = null;
                this.streamingResourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_fma_stream_" + this.uniqueId, this.streamingTexture);
                return;
            }

            destinationPixels.copyFrom(frame.nativeImage);
            currentTexture.upload();
        } catch (Exception ex) {
            this.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to upload streamed FMA frame into DynamicTexture", ex);
        } finally {
            frame.close();
        }
    }

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;

        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        this.uploadPendingFrameToTexture();

        if (this.loadingFailed.get()) return FULLY_TRANSPARENT_TEXTURE;
        return (this.streamingResourceLocation != null) ? this.streamingResourceLocation : FULLY_TRANSPARENT_TEXTURE;
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
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    @Override
    public boolean isReady() {
        return this.decoded.get();
    }

    @Override
    public boolean isLoadingCompleted() {
        return !this.closed.get() && !this.loadingFailed.get() && this.loadingCompleted.get();
    }

    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed.get();
    }

    public void reset() {
        if (this.closed.get()) return;

        this.playRequested = true;
        this.pausedRequested = false;
        this.requestPlaybackReset();
        this.startTickerIfNeeded();
    }

    private void notifyAnimatedTextureStarted(boolean willRestart) {
        Listeners.ON_ANIMATED_TEXTURE_STARTED_PLAYING.onAnimatedTextureStartedPlaying(
                this.resolveTextureSource(),
                this.resolveTextureSourceType(),
                willRestart
        );
    }

    private void notifyAnimatedTextureFinished(boolean willRestart) {
        Listeners.ON_ANIMATED_TEXTURE_FINISHED_PLAYING.onAnimatedTextureFinishedPlaying(
                this.resolveTextureSource(),
                this.resolveTextureSourceType(),
                willRestart
        );
    }

    private String resolveTextureSource() {
        if (this.sourceURL != null) return this.sourceURL;
        if (this.sourceFile != null) return this.sourceFile.getPath();
        if (this.sourceLocation != null) return this.sourceLocation.toString();
        return "ERROR";
    }

    private String resolveTextureSourceType() {
        if (this.sourceURL != null) return "WEB";
        if (this.sourceFile != null) return "LOCAL";
        if (this.sourceLocation != null) return "RESOURCE_LOCATION";
        return "UNKNOWN";
    }

    @Override
    public void play() {
        if (this.closed.get() || this.loadingFailed.get()) return;

        if (this.maxLoopsReached) {
            this.reset();
            return;
        }

        if (this.pausedRequested) {
            this.playbackFrameStartMs = System.currentTimeMillis();
        }
        this.playRequested = true;
        this.pausedRequested = false;
        this.startTickerIfNeeded();
    }

    @Override
    public boolean isPlaying() {
        return !this.closed.get() && this.playRequested && !this.pausedRequested && !this.maxLoopsReached;
    }

    @Override
    public void pause() {
        if (this.closed.get() || this.loadingFailed.get()) return;
        this.pausedRequested = true;
    }

    @Override
    public boolean isPaused() {
        return this.pausedRequested;
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
        this.closed.set(true);
        this.playRequested = false;
        this.pausedRequested = false;

        this.streamGeneration.incrementAndGet();

        Thread running = this.streamThread;
        if (running != null) {
            running.interrupt();
        }
        this.streamThread = null;

        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }
        this.clearPendingUploadFrame();

        if (this.streamingTexture != null) {
            try {
                this.streamingTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close streaming DynamicTexture of FMA", ex);
            }
            this.streamingTexture = null;
        }
        this.streamingResourceLocation = null;

        FmaDecoder activeDecoder = this.decoder;
        this.decoder = null;
        if (activeDecoder != null) {
            CloseableUtils.closeQuietly(activeDecoder);
        }

        this.sourceLocation = null;
        this.sourceFile = null;
        this.sourceURL = null;
    }

    @Nullable
    public static DecodedFmaImage decodeFma(@NotNull InputStream in, @NotNull String fmaName) {
        FmaDecoder decoder = null;
        try {
            decoder = new FmaDecoder();
            decoder.read(in);
            warnAboutExpensiveFmaFrames(decoder, fmaName);

            FmaDecoder.FmaMetadata metadata = Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!");
            InputStream firstFrameStream = decoder.hasIntroFrames() ? decoder.getIntroFrame(0) : decoder.getFirstFrame();
            if (firstFrameStream == null) {
                throw new NullPointerException("Failed to get first frame of FMA image!");
            }

            int imageWidth;
            int imageHeight;
            try (InputStream closeableFirstFrame = firstFrameStream; NativeImage firstFrameImage = NativeImage.read(closeableFirstFrame)) {
                imageWidth = firstFrameImage.getWidth();
                imageHeight = firstFrameImage.getHeight();
            }

            return new DecodedFmaImage(decoder, imageWidth, imageHeight, metadata.getLoopCount());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode FMA image: " + fmaName, ex);
            CloseableUtils.closeQuietly(decoder);
            return null;
        }
    }

    protected static void warnAboutExpensiveFmaFrames(@NotNull FmaDecoder decoder, @NotNull String fmaName) {
        try {
            FmaDecoder.ExpensiveFrameSample expensiveFrameSample = decoder.findExpensiveFrameSample();
            if (expensiveFrameSample == null) return;

            LOGGER.warn("[FANCYMENU] Detected expensive sampled frame while loading FMA {}. Frame: {}, bit depth: {}, color type: {}, interlace method: {}, resolution: {}x{}",
                    fmaName,
                    expensiveFrameSample.framePath(),
                    expensiveFrameSample.bitDepth(),
                    expensiveFrameSample.colorType(),
                    expensiveFrameSample.interlaceMethod(),
                    expensiveFrameSample.width(),
                    expensiveFrameSample.height());

            String displayName = resolveFmaDisplayName(fmaName);
            MainThreadTaskExecutor.executeInMainThread(() -> Dialogs.openExpensiveFmaWarning(displayName), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to scan FMA for expensive sampled frames before loading: {}", fmaName, ex);
        }
    }

    @NotNull
    protected static String resolveFmaDisplayName(@NotNull String fmaName) {
        String normalized = fmaName.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        if ((slashIndex >= 0) && (slashIndex < (normalized.length() - 1))) {
            return normalized.substring(slashIndex + 1);
        }
        return normalized;
    }

    protected static void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(1L, millis));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    protected static class DecodedFrame implements AutoCloseable {
        protected final boolean intro;
        protected final int index;
        protected final long delayMs;
        @Nullable
        protected NativeImage nativeImage;

        protected DecodedFrame(boolean intro, int index, long delayMs, @NotNull NativeImage nativeImage) {
            this.intro = intro;
            this.index = index;
            this.delayMs = delayMs;
            this.nativeImage = nativeImage;
        }

        @Override
        public void close() {
            if (this.nativeImage != null) {
                try {
                    this.nativeImage.close();
                } catch (Exception ignored) {
                }
                this.nativeImage = null;
            }
        }
    }

    public record DecodedFmaImage(@NotNull FmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}
