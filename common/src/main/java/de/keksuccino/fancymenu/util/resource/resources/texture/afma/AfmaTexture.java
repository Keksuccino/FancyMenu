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
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaDecodedFrame;
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

public class AfmaTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PREFETCH_QUEUE_SIZE = 2;
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
    protected volatile boolean mainFramesUseIntroSequence = false;

    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();

    @Nullable
    protected volatile DynamicTexture streamingTexture = null;
    @Nullable
    protected volatile ResourceLocation streamingResourceLocation = null;

    protected final Object streamStateLock = new Object();
    @NotNull
    protected final ArrayDeque<PreparedFrame> prefetchedFrames = new ArrayDeque<>();
    protected final AtomicInteger streamGeneration = new AtomicInteger(0);
    protected final AtomicBoolean deferredDecoderRelease = new AtomicBoolean(false);

    @Nullable
    protected volatile Thread streamThread = null;
    protected volatile boolean playbackInitialized = false;
    protected volatile boolean playbackIntro = false;
    protected volatile int playbackIndex = -1;
    protected volatile long playbackFrameStartMs = 0L;
    protected volatile long playbackFrameDelayMs = MIN_FRAME_DELAY_MS;

    protected volatile boolean decodeIntro = false;
    protected volatile int decodeIndex = 0;
    @Nullable
    protected volatile int[] decodePreviousPixels = null;

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

        Thread loaderThread = new Thread(() -> {
            try {
                populateTexture(texture, afmaFile, afmaFile.getPath());
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read AFMA image from file: " + afmaFile.getPath(), ex);
            } finally {
                scheduleCloseIfNeeded(texture);
            }
        }, "FancyMenu-AfmaLoader-Local-" + texture.uniqueId);
        loaderThread.setDaemon(true);
        loaderThread.start();

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

        Thread loaderThread = new Thread(() -> {
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
        }, "FancyMenu-AfmaLoader-Web-" + texture.uniqueId);
        loaderThread.setDaemon(true);
        loaderThread.start();

        return texture;
    }

    @NotNull
    public static AfmaTexture of(@NotNull InputStream in, @Nullable String afmaTextureName, @Nullable AfmaTexture writeTo) {
        Objects.requireNonNull(in);
        AfmaTexture texture = (writeTo != null) ? writeTo : new AfmaTexture();

        Thread loaderThread = new Thread(() -> {
            populateTexture(texture, in, (afmaTextureName != null) ? afmaTextureName : "[Generic InputStream Source]");
            scheduleCloseIfNeeded(texture);
        }, "FancyMenu-AfmaLoader-Stream-" + texture.uniqueId);
        loaderThread.setDaemon(true);
        loaderThread.start();
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
                texture.releaseDecoder();
                LOGGER.error("[FANCYMENU] Failed to initialize streaming state for AFMA image: " + afmaTextureName, ex);
            }

            texture.decoded.set(true);
        }

        CloseableUtils.closeQuietly(in);
    }

    protected static void scheduleCloseIfNeeded(@NotNull AfmaTexture texture) {
        if (texture.closed.get()) {
            MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
    }

    protected static void populateTexture(@NotNull AfmaTexture texture, @NotNull File afmaFile, @NotNull String afmaTextureName) {
        DecodedAfmaImage decodedImage = null;
        if (!texture.closed.get()) {
            decodedImage = decodeAfma(afmaFile, afmaTextureName);
            if (decodedImage == null) {
                texture.decoded.set(true);
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read AFMA image, because DecodedAfmaImage was NULL: {}", afmaTextureName);
                return;
            }

            try {
                texture.configureStreamingState(decodedImage);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                texture.releaseDecoder();
                LOGGER.error("[FANCYMENU] Failed to initialize streaming state for AFMA image: " + afmaTextureName, ex);
            }

            texture.decoded.set(true);
        }
    }

    protected AfmaTexture() {
    }

    protected void configureStreamingState(@NotNull DecodedAfmaImage decodedImage) {
        AfmaDecoder previousDecoder = this.decoder;
        if ((previousDecoder != null) && (previousDecoder != decodedImage.decoder())) {
            CloseableUtils.closeQuietly(previousDecoder);
        }

        this.decoder = decodedImage.decoder();
        int mainFrameCount = this.decoder.getFrameCount();
        int introCount = this.decoder.getIntroFrameCount();

        if ((mainFrameCount <= 0) && (introCount <= 0)) {
            throw new IllegalStateException("AFMA image has no usable frames");
        }

        this.width = decodedImage.imageWidth();
        this.height = decodedImage.imageHeight();
        this.aspectRatio = new AspectRatio(decodedImage.imageWidth(), decodedImage.imageHeight());
        this.numPlays.set(decodedImage.numPlays());

        AfmaMetadata metadata = Objects.requireNonNull(this.decoder.getMetadata(), "AfmaDecoder returned NULL for metadata");

        this.mainFramesUseIntroSequence = (mainFrameCount <= 0) && (introCount > 0);
        this.frameCount = this.mainFramesUseIntroSequence ? introCount : mainFrameCount;
        this.introFrameCount = this.mainFramesUseIntroSequence ? 0 : introCount;

        long[] normalDelays = new long[this.frameCount];
        for (int i = 0; i < normalDelays.length; i++) {
            normalDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, this.mainFramesUseIntroSequence));
        }

        long[] introDelays = new long[this.introFrameCount];
        for (int i = 0; i < introDelays.length; i++) {
            introDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, true));
        }

        this.frameDelaysMs = normalDelays;
        this.introFrameDelaysMs = introDelays;
        // Match the older animated-texture contract used by pre-loading:
        // once decode/setup finished successfully, async loading is complete
        // even if the first streamed frame has not been uploaded yet.
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
        try {
            while (!this.closed.get() && !this.loadingFailed.get() && (generation == this.streamGeneration.get())) {
                if (!this.playRequested || this.maxLoopsReached) {
                    sleepQuietly(IDLE_SLEEP_MS);
                    continue;
                }
                if (this.pausedRequested && this.playbackInitialized) {
                    sleepQuietly(IDLE_SLEEP_MS);
                    continue;
                }
                if (this.shouldIdleOnTerminalSingleMainFrame()) {
                    synchronized (this.streamStateLock) {
                        this.clearPrefetchedFramesLocked();
                    }
                    sleepQuietly(100L);
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
        } finally {
            synchronized (this.streamStateLock) {
                if (generation == this.streamGeneration.get()) {
                    this.streamThread = null;
                } else if (this.streamThread == Thread.currentThread()) {
                    this.streamThread = null;
                }
            }
            this.releaseDeferredDecoderIfNeeded();
        }
    }

    protected void fillPrefetchQueue(int generation) {
        if (this.shouldIdleOnTerminalSingleMainFrame()) {
            synchronized (this.streamStateLock) {
                this.clearPrefetchedFramesLocked();
            }
            return;
        }

        while (!this.closed.get() && !this.loadingFailed.get() && (generation == this.streamGeneration.get())) {
            synchronized (this.streamStateLock) {
                if (this.prefetchedFrames.size() >= PREFETCH_QUEUE_SIZE) {
                    return;
                }
            }

            PreparedFrame preparedFrame = this.prepareNextFrame(generation);
            if (preparedFrame == null) {
                return;
            }

            synchronized (this.streamStateLock) {
                if (generation != this.streamGeneration.get()) {
                    preparedFrame.close();
                    return;
                }
                this.prefetchedFrames.addLast(preparedFrame);
            }
        }
    }

    @Nullable
    protected PreparedFrame prepareNextFrame(int generation) {
        AfmaDecoder activeDecoder = this.decoder;
        if (activeDecoder == null) return null;

        boolean intro;
        int index;
        synchronized (this.streamStateLock) {
            if (generation != this.streamGeneration.get()) {
                return null;
            }
            intro = this.decodeIntro;
            index = this.decodeIndex;
        }
        long delay = this.resolveFrameDelay(intro, index);
        try {
            AfmaDecodedFrame decodedFrame = this.resolveDecodedFrame(activeDecoder, intro, index);
            int[] renderedPixels = decodedFrame.copyPixels();
            PreparedFrame preparedFrame = this.buildPreparedFrame(intro, index, delay, renderedPixels);
            if (!this.advanceDecodeCursor(generation)) {
                preparedFrame.close();
                return null;
            }
            return preparedFrame;
        } catch (Exception ex) {
            this.failStreaming("Failed to decode AFMA frame " + (intro ? "intro" : "normal") + " " + index, ex);
            return null;
        }
    }

    protected @NotNull AfmaDecodedFrame resolveDecodedFrame(@NotNull AfmaDecoder activeDecoder, boolean intro, int index) {
        AfmaDecodedFrame frame = (intro || this.mainFramesUseIntroSequence) ? activeDecoder.getIntroFrame(index) : activeDecoder.getFrame(index);
        if (frame == null) {
            throw new IllegalStateException("AFMA frame index is out of bounds for the active playback sequence");
        }
        return frame;
    }

    protected @NotNull PreparedFrame buildPreparedFrame(boolean intro, int index, long delayMs, @NotNull int[] framePixels) {
        int[] previousPixels = this.decodePreviousPixels;
        this.decodePreviousPixels = framePixels;

        if ((previousPixels == null) || (previousPixels.length != framePixels.length)) {
            return PreparedFrame.fullFrame(intro, index, delayMs, this.width, this.height, framePixels);
        }

        AfmaRect dirtyRect = this.findDirtyRect(previousPixels, framePixels);
        if (dirtyRect == null) {
            return PreparedFrame.unchanged(intro, index, delayMs);
        }
        return PreparedFrame.dirtyRect(intro, index, delayMs, dirtyRect, this.extractRectPixels(framePixels, dirtyRect));
    }

    protected @Nullable AfmaRect findDirtyRect(@NotNull int[] previousPixels, @NotNull int[] currentPixels) {
        int minX = this.width;
        int minY = this.height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < this.height; y++) {
            int rowOffset = y * this.width;
            for (int x = 0; x < this.width; x++) {
                int pixelIndex = rowOffset + x;
                if (previousPixels[pixelIndex] == currentPixels[pixelIndex]) {
                    continue;
                }
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        if (maxX < minX || maxY < minY) {
            return null;
        }
        return new AfmaRect(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    protected @NotNull int[] extractRectPixels(@NotNull int[] sourcePixels, @NotNull AfmaRect dirtyRect) {
        int[] rectPixels = new int[dirtyRect.width() * dirtyRect.height()];
        for (int localY = 0; localY < dirtyRect.height(); localY++) {
            int srcOffset = (dirtyRect.y() + localY) * this.width + dirtyRect.x();
            int dstOffset = localY * dirtyRect.width();
            System.arraycopy(sourcePixels, srcOffset, rectPixels, dstOffset, dirtyRect.width());
        }
        return rectPixels;
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

    protected boolean advanceDecodeCursor(int generation) {
        synchronized (this.streamStateLock) {
            if (generation != this.streamGeneration.get()) {
                return false;
            }

            if (this.decodeIntro) {
                if ((this.decodeIndex + 1) < this.introFrameCount) {
                    this.decodeIndex++;
                    return true;
                }

                this.decodeIntro = false;
                this.decodeIndex = 0;
                return true;
            }

            if (this.frameCount <= 0) {
                this.decodeIndex = 0;
                return true;
            }

            if ((this.decodeIndex + 1) < this.frameCount) {
                this.decodeIndex++;
            } else {
                this.decodeIndex = 0;
            }
            return true;
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
        if (this.closed.get() || this.loadingFailed.get() || !this.decoded.get() || !this.playRequested || this.maxLoopsReached) return;
        if (this.pausedRequested && this.playbackInitialized) return;

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
                this.loadingCompleted.set(true);
                this.maybeEmitStartEvent(first.intro, first.index);
            } catch (Exception ex) {
                this.failStreaming("Failed to initialize AFMA playback", ex);
            } finally {
                first.close();
            }
            return;
        }

        long elapsed = now - this.playbackFrameStartMs;
        long delay = Math.max(MIN_FRAME_DELAY_MS, this.playbackFrameDelayMs);
        if (elapsed < delay) return;

        PreparedFrame next = this.pollPrefetchedFrame();
        if (next == null) return;

        try {
            if (this.isAtNormalCycleBoundary() && !this.playbackIntro && (next.index == 0) && !next.intro) {
                boolean willRestart = this.handleCycleBoundary();
                if (!willRestart) {
                    this.maxLoopsReached = true;
                    this.playRequested = false;
                    return;
                }
            }
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
            if (this.shouldIdleOnTerminalSingleMainFrame()) {
                synchronized (this.streamStateLock) {
                    this.clearPrefetchedFramesLocked();
                }
            }
        } catch (Exception ex) {
            this.failStreaming("Failed to advance AFMA playback", ex);
        } finally {
            next.close();
        }
    }

    @Nullable
    protected DynamicTexture ensureStreamingTexture() {
        if ((this.width <= 0) || (this.height <= 0)) {
            return null;
        }
        DynamicTexture currentTexture = this.streamingTexture;
        if ((currentTexture != null) && (currentTexture.getPixels() != null)
                && (currentTexture.getPixels().getWidth() == this.width)
                && (currentTexture.getPixels().getHeight() == this.height)) {
            return currentTexture;
        }

        if (currentTexture != null) {
            try {
                currentTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close old AFMA DynamicTexture", ex);
            }
        }
        if (this.streamingResourceLocation != null) {
            Minecraft.getInstance().getTextureManager().release(this.streamingResourceLocation);
            this.streamingResourceLocation = null;
        }

        this.streamingTexture = new DynamicTexture(new NativeImage(this.width, this.height, true));
        this.streamingResourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_afma_stream_" + this.uniqueId, this.streamingTexture);
        return this.streamingTexture;
    }

    protected void applyPreparedFrame(@NotNull PreparedFrame preparedFrame) {
        DynamicTexture currentTexture = this.ensureStreamingTexture();
        if (currentTexture == null) {
            throw new IllegalStateException("AFMA streaming texture was NULL");
        }

        NativeImage canvas = currentTexture.getPixels();
        if (canvas == null) {
            throw new IllegalStateException("AFMA streaming texture returned NULL pixels");
        }

        try {
            if ((preparedFrame.dirtyRect != null) && (preparedFrame.pixelPayload != null)) {
                this.copyPayloadIntoCanvas(preparedFrame.pixelPayload, canvas, preparedFrame.dirtyRect.x(), preparedFrame.dirtyRect.y());
                this.uploadDirtyRect(currentTexture, canvas, preparedFrame.dirtyRect);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to apply AFMA frame " + preparedFrame.index, ex);
        }
    }

    protected void uploadDirtyRect(@NotNull DynamicTexture texture, @NotNull NativeImage canvas, @NotNull AfmaRect dirtyRect) {
        if ((dirtyRect.width() >= canvas.getWidth()) && (dirtyRect.height() >= canvas.getHeight())) {
            texture.upload();
            return;
        }
        texture.bind();
        canvas.upload(0, dirtyRect.x(), dirtyRect.y(), dirtyRect.x(), dirtyRect.y(), dirtyRect.width(), dirtyRect.height(), false, false);
    }

    protected void copyPayloadIntoCanvas(@NotNull PixelPayload payload, @NotNull NativeImage canvas, int dstX, int dstY) {
        AfmaNativeImageHelper.blitPixels(canvas, dstX, dstY, payload.width, payload.height, payload.pixels, payload.offset, payload.scanlineStride, payload.forceOpaqueAlpha);
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

    protected boolean shouldIdleOnTerminalSingleMainFrame() {
        return this.playbackInitialized
                && !this.playbackIntro
                && (this.playbackIndex == 0)
                && (this.introFrameCount > 0)
                && (this.frameCount == 1)
                && (this.numPlays.get() <= 0);
    }

    protected void requestPlaybackReset() {
        int nextGeneration = this.streamGeneration.incrementAndGet();

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
        this.decodePreviousPixels = null;

        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }

        Thread running = this.streamThread;
        if (running != null) {
            running.interrupt();
        }

        synchronized (this.streamStateLock) {
            if (nextGeneration == this.streamGeneration.get()) {
                this.decodeIntro = this.introFrameCount > 0;
                this.decodeIndex = 0;
            }
        }
    }

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;

        this.lastResourceLocationCall = System.currentTimeMillis();
        if (this.loadingFailed.get()) return FULLY_TRANSPARENT_TEXTURE;
        if (!this.decoded.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.startTickerIfNeeded();
        try {
            this.advancePlaybackIfNeeded();
        } catch (Exception ex) {
            this.failStreaming("AFMA playback failed on the client thread", ex);
            return FULLY_TRANSPARENT_TEXTURE;
        }

        if (this.loadingFailed.get()) return FULLY_TRANSPARENT_TEXTURE;
        return (this.loadingCompleted.get() && (this.streamingResourceLocation != null)) ? this.streamingResourceLocation : FULLY_TRANSPARENT_TEXTURE;
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

        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }

        this.releaseStreamingTextureNow();
        this.requestDecoderRelease();

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

    @Nullable
    public static DecodedAfmaImage decodeAfma(@NotNull File file, @NotNull String afmaName) {
        AfmaDecoder decoder = null;
        try {
            decoder = new AfmaDecoder();
            decoder.read(file);
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

    protected void failStreaming(@NotNull String message, @Nullable Throwable throwable) {
        this.loadingFailed.set(true);
        this.loadingCompleted.set(false);
        this.playRequested = false;
        this.pausedRequested = false;
        this.playbackInitialized = false;
        this.streamGeneration.incrementAndGet();
        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }
        Thread running = this.streamThread;
        if ((running != null) && (running != Thread.currentThread())) {
            running.interrupt();
        }
        this.requestDecoderRelease();
        this.scheduleStreamingTextureRelease();
        if (throwable != null) {
            LOGGER.error("[FANCYMENU] {}", message, throwable);
        } else {
            LOGGER.error("[FANCYMENU] {}", message);
        }
    }

    protected void requestDecoderRelease() {
        Thread running = this.streamThread;
        if ((running != null) && running.isAlive() && (running != Thread.currentThread())) {
            this.deferredDecoderRelease.set(true);
            return;
        }
        this.deferredDecoderRelease.set(false);
        this.releaseDecoder();
    }

    protected void releaseDeferredDecoderIfNeeded() {
        if (this.deferredDecoderRelease.compareAndSet(true, false)) {
            this.releaseDecoder();
        }
    }

    protected void releaseDecoder() {
        AfmaDecoder activeDecoder = this.decoder;
        this.decoder = null;
        this.mainFramesUseIntroSequence = false;
        this.decodePreviousPixels = null;
        if (activeDecoder != null) {
            CloseableUtils.closeQuietly(activeDecoder);
        }
    }

    protected void scheduleStreamingTextureRelease() {
        DynamicTexture activeTexture = this.streamingTexture;
        ResourceLocation activeLocation = this.streamingResourceLocation;
        this.streamingTexture = null;
        this.streamingResourceLocation = null;
        if ((activeTexture == null) && (activeLocation == null)) {
            return;
        }

        MainThreadTaskExecutor.executeInMainThread(() -> this.releaseStreamingTexture(activeLocation, activeTexture),
                MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    protected void releaseStreamingTextureNow() {
        DynamicTexture activeTexture = this.streamingTexture;
        ResourceLocation activeLocation = this.streamingResourceLocation;
        this.streamingTexture = null;
        this.streamingResourceLocation = null;
        this.releaseStreamingTexture(activeLocation, activeTexture);
    }

    protected void releaseStreamingTexture(@Nullable ResourceLocation resourceLocation, @Nullable DynamicTexture texture) {
        if (resourceLocation != null) {
            Minecraft.getInstance().getTextureManager().release(resourceLocation);
        }
        if (texture != null) {
            try {
                texture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close streaming DynamicTexture of AFMA", ex);
            }
        }
    }

    protected static class PreparedFrame implements AutoCloseable {
        protected final boolean intro;
        protected final int index;
        protected final long delayMs;
        @NotNull
        protected final AfmaRect dirtyRect;
        @Nullable
        protected PixelPayload pixelPayload;

        protected PreparedFrame(boolean intro, int index, long delayMs, @NotNull AfmaRect dirtyRect, @Nullable PixelPayload pixelPayload) {
            this.intro = intro;
            this.index = index;
            this.delayMs = delayMs;
            this.dirtyRect = dirtyRect;
            this.pixelPayload = pixelPayload;
        }

        protected static @NotNull PreparedFrame fullFrame(boolean intro, int index, long delayMs, int width, int height, @NotNull int[] pixels) {
            return new PreparedFrame(intro, index, delayMs, new AfmaRect(0, 0, width, height), new PixelPayload(width, height, pixels, 0, width, false));
        }

        protected static @NotNull PreparedFrame dirtyRect(boolean intro, int index, long delayMs, @NotNull AfmaRect dirtyRect, @NotNull int[] pixels) {
            return new PreparedFrame(intro, index, delayMs, dirtyRect, new PixelPayload(dirtyRect.width(), dirtyRect.height(), pixels, 0, dirtyRect.width(), false));
        }

        protected static @NotNull PreparedFrame unchanged(boolean intro, int index, long delayMs) {
            return new PreparedFrame(intro, index, delayMs, new AfmaRect(0, 0, 0, 0), null);
        }

        @Override
        public void close() {
            this.pixelPayload = null;
        }
    }

    protected static class PixelPayload {
        protected final int width;
        protected final int height;
        @NotNull
        protected final int[] pixels;
        protected final int offset;
        protected final int scanlineStride;
        protected final boolean forceOpaqueAlpha;

        protected PixelPayload(int width, int height, @NotNull int[] pixels, int offset, int scanlineStride, boolean forceOpaqueAlpha) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
            this.offset = Math.max(0, offset);
            this.scanlineStride = Math.max(width, scanlineStride);
            this.forceOpaqueAlpha = forceOpaqueAlpha;
        }
    }

    public record DecodedAfmaImage(@NotNull AfmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}
