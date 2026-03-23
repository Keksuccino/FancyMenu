package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AfmaTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PREFETCH_QUEUE_SIZE = 1;
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
    protected volatile AfmaDecoder decoder = null;
    @Nullable
    protected volatile AfmaFrameIndex frameIndex = null;

    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();

    @Nullable
    protected volatile DynamicTexture streamingTexture = null;
    @Nullable
    protected volatile ResourceLocation streamingResourceLocation = null;

    protected final Object streamStateLock = new Object();
    @NotNull
    protected final ArrayDeque<PreparedFrame> prefetchedFrames = new ArrayDeque<>();
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
    public static AfmaTexture location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static AfmaTexture location(@NotNull ResourceLocation location, @Nullable AfmaTexture writeTo) {
        Objects.requireNonNull(location);
        AfmaTexture texture = (writeTo != null) ? writeTo : new AfmaTexture();
        texture.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read AFMA image from ResourceLocation: " + location, ex);
        }

        return texture;
    }

    @NotNull
    public static AfmaTexture local(@NotNull File afmaFile) {
        return local(afmaFile, null);
    }

    @NotNull
    public static AfmaTexture local(@NotNull File afmaFile, @Nullable AfmaTexture writeTo) {
        Objects.requireNonNull(afmaFile);
        AfmaTexture texture = (writeTo != null) ? writeTo : new AfmaTexture();
        texture.sourceFile = afmaFile;

        if (!afmaFile.isFile()) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read AFMA image from file! File not found: " + afmaFile.getPath());
            return texture;
        }

        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(afmaFile);
                of(in, afmaFile.getPath(), texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read AFMA image from file: " + afmaFile.getPath(), ex);
            }
        }).start();

        return texture;
    }

    @NotNull
    public static AfmaTexture web(@NotNull String afmaUrl) {
        return web(afmaUrl, null);
    }

    @NotNull
    public static AfmaTexture web(@NotNull String afmaUrl, @Nullable AfmaTexture writeTo) {
        Objects.requireNonNull(afmaUrl);
        AfmaTexture texture = (writeTo != null) ? writeTo : new AfmaTexture();
        texture.sourceURL = afmaUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(afmaUrl)) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read AFMA image from URL! Invalid URL: " + afmaUrl);
            return texture;
        }

        new Thread(() -> {
            InputStream in = null;
            try {
                in = WebUtils.openResourceStream(afmaUrl);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, afmaUrl, texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read AFMA image from URL: " + afmaUrl, ex);
                CloseableUtils.closeQuietly(in);
            }
        }).start();

        return texture;
    }

    @NotNull
    public static AfmaTexture of(@NotNull InputStream in, @Nullable String afmaTextureName, @Nullable AfmaTexture writeTo) {
        Objects.requireNonNull(in);
        AfmaTexture texture = (writeTo != null) ? writeTo : new AfmaTexture();

        new Thread(() -> populateTexture(texture, in, (afmaTextureName != null) ? afmaTextureName : "[Generic InputStream Source]")).start();
        return texture;
    }

    @NotNull
    public static AfmaTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected static void populateTexture(@NotNull AfmaTexture texture, @NotNull InputStream in, @NotNull String afmaTextureName) {
        DecodedAfmaImage decodedImage = null;
        if (!texture.closed.get()) {
            decodedImage = decodeAfma(in, afmaTextureName);
            if (decodedImage == null) {
                texture.decoded.set(true);
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read AFMA image, because DecodedAfmaImage was NULL: {}", afmaTextureName);
                CloseableUtils.closeQuietly(in);
                return;
            }

            try {
                texture.configureStreamingState(decodedImage);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to initialize streaming state for AFMA image: " + afmaTextureName, ex);
            }

            texture.decoded.set(true);
        }

        CloseableUtils.closeQuietly(in);
    }

    protected AfmaTexture() {
    }

    protected void configureStreamingState(@NotNull DecodedAfmaImage decodedImage) {
        AfmaDecoder previousDecoder = this.decoder;
        if ((previousDecoder != null) && (previousDecoder != decodedImage.decoder())) {
            CloseableUtils.closeQuietly(previousDecoder);
        }

        this.decoder = decodedImage.decoder();
        this.frameIndex = decodedImage.decoder().getFrameIndex();
        this.frameCount = this.decoder.getFrameCount();
        this.introFrameCount = this.decoder.getIntroFrameCount();

        if (this.frameCount <= 0) {
            throw new IllegalStateException("AFMA image has no usable frames");
        }

        this.width = decodedImage.imageWidth();
        this.height = decodedImage.imageHeight();
        this.aspectRatio = new AspectRatio(decodedImage.imageWidth(), decodedImage.imageHeight());
        this.numPlays.set(decodedImage.numPlays());

        AfmaMetadata metadata = Objects.requireNonNull(this.decoder.getMetadata(), "AfmaDecoder returned NULL for metadata");

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
            Thread stream = new Thread(() -> this.streamLoop(generation), "FancyMenu-AfmaStream-" + this.uniqueId);
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
                this.fillPrefetchQueue(generation);
                sleepQuietly(IDLE_SLEEP_MS);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] An error happened in the streaming thread of an AFMA texture!", ex);
                sleepQuietly(50L);
            }
        }

        synchronized (this.streamStateLock) {
            if (generation == this.streamGeneration.get()) {
                this.streamThread = null;
            }
        }
    }

    protected void fillPrefetchQueue(int generation) {
        synchronized (this.streamStateLock) {
            while (!this.closed.get() && (generation == this.streamGeneration.get()) && (this.prefetchedFrames.size() < PREFETCH_QUEUE_SIZE)) {
                PreparedFrame preparedFrame = this.prepareNextFrame();
                if (preparedFrame == null) {
                    break;
                }
                if (generation != this.streamGeneration.get()) {
                    preparedFrame.close();
                    break;
                }
                this.prefetchedFrames.addLast(preparedFrame);
            }
        }
    }

    @Nullable
    protected PreparedFrame prepareNextFrame() {
        AfmaDecoder activeDecoder = this.decoder;
        AfmaFrameIndex activeFrameIndex = this.frameIndex;
        if ((activeDecoder == null) || (activeFrameIndex == null)) return null;

        boolean intro = this.decodeIntro;
        int index = this.decodeIndex;
        AfmaFrameDescriptor descriptor = intro ? activeFrameIndex.getIntroFrames().get(index) : activeFrameIndex.getFrames().get(index);
        long delay = this.resolveFrameDelay(intro, index);

        NativeImage primaryPayload = null;
        NativeImage patchPayload = null;
        try {
            if (descriptor.requiresPrimaryPayload()) {
                primaryPayload = this.readPayload(activeDecoder, Objects.requireNonNull(descriptor.getPath()));
            }
            if (descriptor.requiresPatchPayload()) {
                patchPayload = this.readPayload(activeDecoder, Objects.requireNonNull(Objects.requireNonNull(descriptor.getPatch()).getPath()));
            }
        } catch (Exception ex) {
            CloseableUtils.closeQuietly(primaryPayload);
            CloseableUtils.closeQuietly(patchPayload);
            LOGGER.error("[FANCYMENU] Failed to decode AFMA payload for {} frame {}", intro ? "intro" : "normal", index, ex);
            return null;
        }

        this.advanceDecodeCursor();
        return new PreparedFrame(intro, index, delay, descriptor, primaryPayload, patchPayload);
    }

    @NotNull
    protected NativeImage readPayload(@NotNull AfmaDecoder activeDecoder, @NotNull String payloadPath) throws IOException {
        InputStream payloadInput = activeDecoder.openPayload(payloadPath);
        if (payloadInput == null) {
            throw new FileNotFoundException("AFMA payload input stream was NULL: " + payloadPath);
        }
        try (InputStream closeableInput = payloadInput) {
            return NativeImage.read(closeableInput);
        }
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
    protected PreparedFrame pollPrefetchedFrame() {
        synchronized (this.streamStateLock) {
            return this.prefetchedFrames.pollFirst();
        }
    }

    protected void clearPrefetchedFramesLocked() {
        PreparedFrame frame;
        while ((frame = this.prefetchedFrames.pollFirst()) != null) {
            frame.close();
        }
    }

    protected void advancePlaybackIfNeeded() {
        if (this.closed.get() || this.loadingFailed.get() || !this.playRequested || this.maxLoopsReached) return;
        if (this.pausedRequested && this.playbackInitialized) return;

        this.ensureStreamingTexture();
        if (this.streamingTexture == null) return;

        long now = System.currentTimeMillis();
        if (!this.playbackInitialized) {
            PreparedFrame first = this.pollPrefetchedFrame();
            if (first == null) return;

            try {
                this.applyPreparedFrame(first);
                this.playbackInitialized = true;
                this.playbackIntro = first.intro;
                this.playbackIndex = first.index;
                this.playbackFrameStartMs = now;
                this.playbackFrameDelayMs = sanitizeDelay(first.delayMs);
                this.maybeEmitStartEvent(first.intro, first.index);
            } finally {
                first.close();
            }
            return;
        }

        long elapsed = now - this.playbackFrameStartMs;
        long delay = Math.max(MIN_FRAME_DELAY_MS, this.playbackFrameDelayMs);
        if (elapsed < delay) return;

        if (this.isAtNormalCycleBoundary()) {
            boolean willRestart = this.handleCycleBoundary();
            if (!willRestart) {
                this.maxLoopsReached = true;
                this.playRequested = false;
                return;
            }
        }

        PreparedFrame next = this.pollPrefetchedFrame();
        if (next == null) return;

        try {
            this.applyPreparedFrame(next);
            boolean switchedIntroToNormal = this.playbackIntro && !next.intro;
            if (switchedIntroToNormal) {
                this.introFinishedPlaying = true;
            }

            this.playbackIntro = next.intro;
            this.playbackIndex = next.index;
            this.playbackFrameStartMs = now;
            this.playbackFrameDelayMs = sanitizeDelay(next.delayMs);
            this.maybeEmitStartEvent(next.intro, next.index);
        } finally {
            next.close();
        }
    }

    protected void ensureStreamingTexture() {
        DynamicTexture currentTexture = this.streamingTexture;
        if ((currentTexture != null) && (currentTexture.getPixels() != null)
                && (currentTexture.getPixels().getWidth() == this.width)
                && (currentTexture.getPixels().getHeight() == this.height)) {
            return;
        }

        if (currentTexture != null) {
            try {
                currentTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close old AFMA DynamicTexture", ex);
            }
        }

        this.streamingTexture = new DynamicTexture(new NativeImage(this.width, this.height, true));
        this.streamingResourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_afma_stream_" + this.uniqueId, this.streamingTexture);
    }

    protected void applyPreparedFrame(@NotNull PreparedFrame preparedFrame) {
        DynamicTexture currentTexture = this.streamingTexture;
        if (currentTexture == null) {
            throw new IllegalStateException("AFMA streaming texture was NULL");
        }

        NativeImage canvas = currentTexture.getPixels();
        if (canvas == null) {
            throw new IllegalStateException("AFMA streaming texture returned NULL pixels");
        }

        AfmaFrameDescriptor descriptor = preparedFrame.descriptor;
        AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame descriptor type was NULL");

        try {
            switch (type) {
                case SAME -> {
                }
                case FULL -> {
                    canvas.copyFrom(Objects.requireNonNull(preparedFrame.primaryPayload, "AFMA full frame payload was NULL"));
                    currentTexture.upload();
                }
                case DELTA_RECT -> {
                    AfmaNativeImageHelper.copyRect(Objects.requireNonNull(preparedFrame.primaryPayload, "AFMA delta payload was NULL"),
                            0, 0, canvas, descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight());
                    this.uploadDirtyRect(currentTexture, canvas, new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight()));
                }
                case COPY_RECT_PATCH -> {
                    AfmaCopyRect copyRect = Objects.requireNonNull(descriptor.getCopy(), "AFMA copy_rect_patch is missing its copy section");
                    AfmaNativeImageHelper.copyRectMemmove(canvas, copyRect);

                    AfmaRect dirtyRect = new AfmaRect(copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight());
                    AfmaPatchRegion patch = descriptor.getPatch();
                    if (patch != null) {
                        AfmaNativeImageHelper.copyRect(Objects.requireNonNull(preparedFrame.patchPayload, "AFMA copy_rect_patch patch payload was NULL"),
                                0, 0, canvas, patch.getX(), patch.getY(), patch.getWidth(), patch.getHeight());
                        dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(patch.getX(), patch.getY(), patch.getWidth(), patch.getHeight()));
                    }
                    this.uploadDirtyRect(currentTexture, canvas, dirtyRect);
                }
            }
        } catch (Exception ex) {
            this.loadingFailed.set(true);
            throw new IllegalStateException("Failed to apply AFMA frame " + preparedFrame.index + " (" + type + ")", ex);
        }
    }

    protected void uploadDirtyRect(@NotNull DynamicTexture texture, @NotNull NativeImage canvas, @NotNull AfmaRect dirtyRect) {
        texture.bind();
        canvas.upload(0, dirtyRect.x(), dirtyRect.y(), dirtyRect.x(), dirtyRect.y(), dirtyRect.width(), dirtyRect.height(), false, false);
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

        Thread running = this.streamThread;
        if (running != null) {
            running.interrupt();
        }
        this.streamThread = null;
    }

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;

        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        this.advancePlaybackIfNeeded();

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

        if (this.streamingTexture != null) {
            try {
                this.streamingTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close streaming DynamicTexture of AFMA", ex);
            }
            this.streamingTexture = null;
        }
        this.streamingResourceLocation = null;

        AfmaDecoder activeDecoder = this.decoder;
        this.decoder = null;
        this.frameIndex = null;
        if (activeDecoder != null) {
            CloseableUtils.closeQuietly(activeDecoder);
        }

        this.sourceLocation = null;
        this.sourceFile = null;
        this.sourceURL = null;
    }

    @Nullable
    public static DecodedAfmaImage decodeAfma(@NotNull InputStream in, @NotNull String afmaName) {
        AfmaDecoder decoder = null;
        try {
            decoder = new AfmaDecoder();
            decoder.read(in);
            AfmaMetadata metadata = Objects.requireNonNull(decoder.getMetadata(), "AfmaDecoder returned NULL for metadata");
            return new DecodedAfmaImage(decoder, metadata.getCanvasWidth(), metadata.getCanvasHeight(), metadata.getLoopCount());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode AFMA image: " + afmaName, ex);
            CloseableUtils.closeQuietly(decoder);
            return null;
        }
    }

    protected static void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(1L, millis));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    protected static class PreparedFrame implements AutoCloseable {
        protected final boolean intro;
        protected final int index;
        protected final long delayMs;
        @NotNull
        protected final AfmaFrameDescriptor descriptor;
        @Nullable
        protected NativeImage primaryPayload;
        @Nullable
        protected NativeImage patchPayload;

        protected PreparedFrame(boolean intro, int index, long delayMs, @NotNull AfmaFrameDescriptor descriptor,
                                @Nullable NativeImage primaryPayload, @Nullable NativeImage patchPayload) {
            this.intro = intro;
            this.index = index;
            this.delayMs = delayMs;
            this.descriptor = descriptor;
            this.primaryPayload = primaryPayload;
            this.patchPayload = patchPayload;
        }

        @Override
        public void close() {
            if (this.primaryPayload != null) {
                try {
                    this.primaryPayload.close();
                } catch (Exception ignored) {
                }
                this.primaryPayload = null;
            }
            if (this.patchPayload != null) {
                try {
                    this.patchPayload.close();
                } catch (Exception ignored) {
                }
                this.patchPayload = null;
            }
        }
    }

    public record DecodedAfmaImage(@NotNull AfmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}
