package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.resources.texture.AnimatedTextureResetFrame;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

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

    /** How many full play cycles got completed since the last reset, loop restarts included. **/
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
    /** The decoder that reads the archive while the texture loads, so closing the texture can release its file right away, also while the loading thread still reads it. **/
    @Nullable
    protected volatile AfmaDecoder loadingDecoder = null;
    @Nullable
    protected volatile AfmaFrameIndex frameIndex = null;

    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();

    @Nullable
    protected volatile DynamicTexture streamingTexture = null;
    @Nullable
    protected volatile ResourceLocation streamingResourceLocation = null;
    @NotNull
    protected final AnimatedTextureResetFrame resetFrame = new AnimatedTextureResetFrame();

    protected final Object streamStateLock = new Object();
    @NotNull
    protected final ArrayDeque<PreparedFrame> prefetchedFrames = new ArrayDeque<>();
    @NotNull
    protected final AfmaDecodeScratch payloadDecodeScratch = new AfmaDecodeScratch();
    protected final AtomicInteger streamGeneration = new AtomicInteger(0);
    protected final AtomicBoolean deferredDecoderRelease = new AtomicBoolean(false);

    @Nullable
    protected volatile Thread streamThread = null;
    protected volatile boolean sequenceUsesBlockInter = false;
    @Nullable
    protected volatile NativeImage blockInterReferenceCanvas = null;
    protected volatile boolean playbackInitialized = false;
    protected volatile boolean playbackIntro = false;
    protected volatile int playbackIndex = -1;
    protected volatile long playbackFrameStartMs = 0L;
    protected volatile long playbackFrameDelayMs = MIN_FRAME_DELAY_MS;
    /**
     * When the paused texture got rendered first, so the visible frame can continue its remaining time when it plays again. -1 while it didn't get rendered paused.
     * Playback on the render thread and resets write it, the stream thread reads it to stop decoding ahead while the texture holds its paused frame.
     */
    protected volatile long pausedAtMs = -1L;
    protected volatile boolean playbackResetFrameRestorePending = false;
    protected volatile boolean playbackResetFrameRestored = false;

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
                in = WebUtils.openResourceStream(afmaUrl, WebUtils.WebResourceType.STREAMED_ANIMATED_ARCHIVE);
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

    /**
     * Closes the passed {@link InputStream}.
     */
    protected static void populateTexture(@NotNull AfmaTexture texture, @NotNull InputStream in, @NotNull String afmaTextureName) {
        texture.populate(decoder -> decoder.read(in), afmaTextureName);
        CloseableUtils.closeQuietly(in);
    }

    protected static void scheduleCloseIfNeeded(@NotNull AfmaTexture texture) {
        if (texture.closed.get()) {
            MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
    }

    protected static void populateTexture(@NotNull AfmaTexture texture, @NotNull File afmaFile, @NotNull String afmaTextureName) {
        texture.populate(decoder -> decoder.read(afmaFile), afmaTextureName);
    }

    protected AfmaTexture() {
    }

    /**
     * Reads the archive with a new decoder and prepares streaming it. Closed textures don't read it.<br>
     * The decoder is the {@link #loadingDecoder} while it reads, so closing the texture releases its file right away. Its reads fail then, which is no error.
     */
    protected void populate(@NotNull ArchiveReader reader, @NotNull String afmaTextureName) {
        if (this.closed.get()) return;
        AfmaDecoder decoder = this.createDecoder();
        this.loadingDecoder = decoder;
        try {
            // close() first marks the texture as closed and then releases the loading decoder, so checking again after tracking the decoder never misses a close
            if (this.closed.get()) return;
            DecodedAfmaImage decodedImage;
            try {
                reader.read(decoder);
                AfmaMetadata metadata = Objects.requireNonNull(decoder.getMetadata(), "AfmaDecoder returned NULL for metadata");
                decodedImage = new DecodedAfmaImage(decoder, metadata.getCanvasWidth(), metadata.getCanvasHeight(), metadata.getLoopCount());
            } catch (Exception ex) {
                this.decoded.set(true);
                this.loadingFailed.set(true);
                if (!this.closed.get()) LOGGER.error("[FANCYMENU] Failed to decode AFMA image: " + afmaTextureName, ex);
                return;
            }

            try {
                this.configureStreamingState(decodedImage);
                // The texture owns the decoder now
                decoder = null;
            } catch (Exception ex) {
                this.loadingFailed.set(true);
                this.releaseDecoder();
                LOGGER.error("[FANCYMENU] Failed to initialize streaming state for AFMA image: " + afmaTextureName, ex);
            }

            this.decoded.set(true);
        } finally {
            this.loadingDecoder = null;
            CloseableUtils.closeQuietly(decoder);
        }
    }

    @NotNull
    protected AfmaDecoder createDecoder() {
        return new AfmaDecoder();
    }

    protected void configureStreamingState(@NotNull DecodedAfmaImage decodedImage) {
        this.resetFrame.clear();
        AfmaDecoder previousDecoder = this.decoder;
        if ((previousDecoder != null) && (previousDecoder != decodedImage.decoder())) {
            CloseableUtils.closeQuietly(previousDecoder);
        }

        this.decoder = decodedImage.decoder();
        AfmaFrameIndex activeFrameIndex = Objects.requireNonNull(decodedImage.decoder().getFrameIndex(), "AfmaDecoder returned NULL for frame index");
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

        if ((mainFrameCount <= 0) && (introCount > 0)) {
            this.frameIndex = new AfmaFrameIndex(activeFrameIndex.getIntroFrames(), java.util.List.of());
            this.frameCount = introCount;
            this.introFrameCount = 0;
        } else {
            this.frameIndex = activeFrameIndex;
            this.frameCount = mainFrameCount;
            this.introFrameCount = introCount;
        }

        long[] normalDelays = new long[this.frameCount];
        for (int i = 0; i < normalDelays.length; i++) {
            normalDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, mainFrameCount <= 0));
        }

        long[] introDelays = new long[this.introFrameCount];
        for (int i = 0; i < introDelays.length; i++) {
            introDelays[i] = sanitizeDelay(metadata.getFrameTimeForFrame(i, true));
        }

        this.frameDelaysMs = normalDelays;
        this.introFrameDelaysMs = introDelays;
        this.sequenceUsesBlockInter = this.sequenceUsesBlockInter(activeFrameIndex);
        if (!this.sequenceUsesBlockInter) {
            CloseableUtils.closeQuietly(this.blockInterReferenceCanvas);
            this.blockInterReferenceCanvas = null;
        } else if ((this.blockInterReferenceCanvas != null)
                && ((this.blockInterReferenceCanvas.getWidth() != this.width) || (this.blockInterReferenceCanvas.getHeight() != this.height))) {
            CloseableUtils.closeQuietly(this.blockInterReferenceCanvas);
            this.blockInterReferenceCanvas = null;
        }
        // Match the older animated-texture contract used by pre-loading:
        // once decode/setup finished successfully, async loading is complete
        // even if the first streamed frame has not been uploaded yet.
        this.loadingCompleted.set(true);
        this.loadingFailed.set(false);

        this.requestPlaybackReset();
    }

    protected boolean sequenceUsesBlockInter(@NotNull AfmaFrameIndex activeFrameIndex) {
        for (AfmaFrameDescriptor descriptor : activeFrameIndex.getIntroFrames()) {
            if ((descriptor != null) && (descriptor.getType() == AfmaFrameOperationType.BLOCK_INTER)) {
                return true;
            }
        }
        for (AfmaFrameDescriptor descriptor : activeFrameIndex.getFrames()) {
            if ((descriptor != null) && (descriptor.getType() == AfmaFrameOperationType.BLOCK_INTER)) {
                return true;
            }
        }
        return false;
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
                sleepQuietly(this.streamIteration(generation, this.currentTimeMillis()));
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

    /** Runs one prefetch iteration and returns the delay before the worker should try again. */
    protected long streamIteration(int generation, long now) {
        if (!this.playRequested || this.maxLoopsReached) return IDLE_SLEEP_MS;
        if (this.isHoldingPausedFrame()) return IDLE_SLEEP_MS;
        if (this.shouldIdleOnTerminalSingleMainFrame()) {
            synchronized (this.streamStateLock) {
                this.clearPrefetchedFramesLocked();
            }
            return 100L;
        }

        if ((this.lastResourceLocationCall > 0L) && ((this.lastResourceLocationCall + INACTIVITY_TIMEOUT_MS) < now)) {
            // These bounded pending frames bridge the displayed canvas to the already advanced decode cursor.
            // Dropping them would apply later residual/motion frames to the wrong reference image on resume.
            return 100L;
        }

        try {
            this.fillPrefetchQueue(generation);
            return IDLE_SLEEP_MS;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened in the streaming thread of an AFMA texture!", ex);
            return 50L;
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
        AfmaFrameIndex activeFrameIndex = this.frameIndex;
        if ((activeDecoder == null) || (activeFrameIndex == null)) return null;

        boolean intro;
        int index;
        synchronized (this.streamStateLock) {
            if (generation != this.streamGeneration.get()) {
                return null;
            }
            intro = this.decodeIntro;
            index = this.decodeIndex;
        }
        java.util.List<AfmaFrameDescriptor> sequence = intro ? activeFrameIndex.getIntroFrames() : activeFrameIndex.getFrames();
        if ((index < 0) || (index >= sequence.size())) {
            this.failStreaming("AFMA frame index is out of bounds for the active playback sequence", null);
            return null;
        }
        AfmaFrameDescriptor descriptor = sequence.get(index);
        long delay = this.resolveFrameDelay(intro, index);

        FramePayload primaryPayload = null;
        FramePayload patchPayload = null;
        try {
            primaryPayload = this.readPayloadByCodec(activeDecoder, this.resolvePrimaryPayloadCodec(descriptor), descriptor.getPrimaryPayloadPath());
            patchPayload = this.readPayloadByCodec(activeDecoder, this.resolveSecondaryPayloadCodec(descriptor), descriptor.getSecondaryPayloadPath());
        } catch (Exception ex) {
            // Closing the texture releases the archive file right away, so reads that come after it fail, which is no error
            if (this.closed.get()) return null;
            this.failStreaming("Failed to decode AFMA payload for " + (intro ? "intro" : "normal") + " frame " + index, ex);
            return null;
        }

        if (!this.advanceDecodeCursor(generation)) {
            return null;
        }
        return new PreparedFrame(intro, index, delay, descriptor, primaryPayload, patchPayload);
    }

    @NotNull
    protected AfmaPayloadCodec resolvePrimaryPayloadCodec(@NotNull AfmaFrameDescriptor descriptor) {
        AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame descriptor type was NULL");
        return switch (type) {
            case FULL, DELTA_RECT -> AfmaPayloadCodec.BIN_INTRA;
            case RESIDUAL_DELTA_RECT, COPY_RECT_RESIDUAL_PATCH, MULTI_COPY_RESIDUAL_PATCH -> AfmaPayloadCodec.RAW_RESIDUAL;
            case SPARSE_DELTA_RECT, COPY_RECT_SPARSE_PATCH, MULTI_COPY_SPARSE_PATCH -> AfmaPayloadCodec.RAW_SPARSE_LAYOUT;
            case BLOCK_INTER -> AfmaPayloadCodec.INTER_FRAME;
            case SAME, COPY_RECT_PATCH, MULTI_COPY_PATCH -> AfmaPayloadCodec.NONE;
        };
    }

    @NotNull
    protected AfmaPayloadCodec resolveSecondaryPayloadCodec(@NotNull AfmaFrameDescriptor descriptor) {
        AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame descriptor type was NULL");
        return switch (type) {
            case SPARSE_DELTA_RECT, COPY_RECT_SPARSE_PATCH, MULTI_COPY_SPARSE_PATCH -> AfmaPayloadCodec.RAW_SPARSE_RESIDUAL;
            case COPY_RECT_PATCH, MULTI_COPY_PATCH -> descriptor.requiresPatchPayload() ? AfmaPayloadCodec.BIN_INTRA : AfmaPayloadCodec.NONE;
            case FULL, DELTA_RECT, RESIDUAL_DELTA_RECT, SAME, COPY_RECT_RESIDUAL_PATCH, MULTI_COPY_RESIDUAL_PATCH, BLOCK_INTER -> AfmaPayloadCodec.NONE;
        };
    }

    @Nullable
    protected FramePayload readPayloadByCodec(@NotNull AfmaDecoder activeDecoder, @NotNull AfmaPayloadCodec codec, @Nullable String payloadPath) throws IOException {
        return switch (codec) {
            case NONE -> null;
            case BIN_INTRA -> this.readBinIntraPayload(activeDecoder, Objects.requireNonNull(payloadPath));
            case RAW_RESIDUAL, RAW_SPARSE_LAYOUT, RAW_SPARSE_RESIDUAL, INTER_FRAME, TILE_INTRA ->
                    this.readRawPayload(activeDecoder, Objects.requireNonNull(payloadPath));
        };
    }

    @NotNull
    protected PixelPayload readBinIntraPayload(@NotNull AfmaDecoder activeDecoder, @NotNull String payloadPath) throws IOException {
        AfmaDecoder.PayloadBytes payloadBytes = activeDecoder.readPayloadBytes(payloadPath);
        AfmaBinIntraPayloadHelper.DecodedFrame decodedFrame = AfmaBinIntraPayloadHelper.decodePayload(
                payloadBytes.bytes(),
                payloadBytes.offset(),
                payloadBytes.length(),
                this.payloadDecodeScratch
        );
        return new PixelPayload(decodedFrame.width(), decodedFrame.height(), decodedFrame.pixels(), 0, decodedFrame.width(), false);
    }

    @NotNull
    protected RawPayload readRawPayload(@NotNull AfmaDecoder activeDecoder, @NotNull String payloadPath) throws IOException {
        AfmaDecoder.PayloadBytes payloadBytes = activeDecoder.readPayloadBytes(payloadPath);
        return new RawPayload(payloadBytes.bytes(), payloadBytes.offset(), payloadBytes.length());
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

        long now = this.currentTimeMillis();
        if (!this.playbackInitialized) {
            PreparedFrame first = this.pollPrefetchedFrame();
            if (first == null) return;

            try {
                boolean useRestoredFrame = this.isRestoredResetFrame(first);
                this.playbackResetFrameRestorePending = false;
                this.playbackResetFrameRestored = false;
                if (!useRestoredFrame) {
                    this.resetFrame.cancelRestore();
                    this.applyPreparedFrame(first);
                    NativeImage canvas = Objects.requireNonNull(this.streamingTexture, "AFMA streaming texture was NULL after applying the first frame").getPixels();
                    this.resetFrame.captureIfAbsent(Objects.requireNonNull(canvas, "AFMA streaming texture returned NULL pixels after applying the first frame"));
                }
                this.playbackInitialized = true;
                this.playbackIntro = first.intro;
                this.playbackIndex = first.index;
                this.playbackFrameStartMs = now;
                // A frame that appears while paused starts its time once playback continues
                this.pausedAtMs = this.pausedRequested ? now : -1L;
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

        if (this.pausedRequested) {
            /*
             * Only the time the texture gets rendered while paused counts as pause, which the visible frame gets once playback continues.
             * Several users can share one texture, and one of them can pause it while another one keeps showing it, like widgets that pause the background of
             * their hidden states, which can be the shown background of other widgets. Counting the time between their pause and play calls would stall it then.
             */
            if (this.pausedAtMs < 0L) this.pausedAtMs = now;
            return;
        }
        long pausedAt = this.pausedAtMs;
        if (pausedAt >= 0L) {
            // The visible frame continues with the time it had left when it got rendered paused
            this.playbackFrameStartMs += Math.max(0L, now - pausedAt);
            this.pausedAtMs = -1L;
        }

        if (this.shouldIdleOnTerminalSingleMainFrame()) {
            this.completeHeldMainFrameCycle(now);
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

        AfmaFrameDescriptor descriptor = preparedFrame.descriptor;
        AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame descriptor type was NULL");

        try {
            if (type == AfmaFrameOperationType.BLOCK_INTER) {
                NativeImage referenceCanvas = this.ensureBlockInterReferenceCanvas(canvas);
                AfmaNativeImageHelper.copyRect(canvas, 0, 0, referenceCanvas, 0, 0, canvas.getWidth(), canvas.getHeight());
            }
            switch (type) {
                case SAME -> {
                }
                case FULL -> {
                    this.copyPayloadIntoCanvas(this.requirePixelPayload(preparedFrame.primaryPayload, "AFMA full frame payload was NULL"), canvas, 0, 0);
                    currentTexture.upload();
                }
                case DELTA_RECT -> {
                    this.copyPayloadIntoCanvas(this.requirePixelPayload(preparedFrame.primaryPayload, "AFMA delta payload was NULL"),
                            canvas, descriptor.getX(), descriptor.getY());
                    this.uploadDirtyRect(currentTexture, canvas, new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight()));
                }
                case RESIDUAL_DELTA_RECT -> {
                    this.applyResidualPayload(
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA residual delta payload was NULL"),
                            canvas,
                            descriptor.getX(),
                            descriptor.getY(),
                            descriptor.getWidth(),
                            descriptor.getHeight(),
                            Objects.requireNonNull(descriptor.getResidual(), "AFMA residual delta metadata was NULL")
                    );
                    this.uploadDirtyRect(currentTexture, canvas, new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight()));
                }
                case SPARSE_DELTA_RECT -> {
                    this.applySparseResidualPayload(
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA sparse delta layout payload was NULL"),
                            this.requireRawPayload(preparedFrame.patchPayload, "AFMA sparse delta residual payload was NULL"),
                            canvas,
                            descriptor.getX(),
                            descriptor.getY(),
                            descriptor.getWidth(),
                            descriptor.getHeight(),
                            Objects.requireNonNull(descriptor.getSparse(), "AFMA sparse delta metadata was NULL")
                    );
                    this.uploadDirtyRect(currentTexture, canvas, new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight()));
                }
                case COPY_RECT_SPARSE_PATCH -> {
                    AfmaCopyRect copyRect = Objects.requireNonNull(descriptor.getCopy(), "AFMA copy_rect_sparse_patch is missing its copy section");
                    AfmaNativeImageHelper.copyRectMemmove(canvas, copyRect);
                    this.applySparseResidualPayload(
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA copy_rect_sparse_patch layout payload was NULL"),
                            this.requireRawPayload(preparedFrame.patchPayload, "AFMA copy_rect_sparse_patch residual payload was NULL"),
                            canvas,
                            descriptor.getX(),
                            descriptor.getY(),
                            descriptor.getWidth(),
                            descriptor.getHeight(),
                            Objects.requireNonNull(descriptor.getSparse(), "AFMA copy_rect_sparse_patch metadata was NULL")
                    );
                    AfmaRect dirtyRect = AfmaRect.union(
                            new AfmaRect(copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight()),
                            new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight())
                    );
                    this.uploadDirtyRect(currentTexture, canvas, dirtyRect);
                }
                case COPY_RECT_PATCH -> {
                    AfmaCopyRect copyRect = Objects.requireNonNull(descriptor.getCopy(), "AFMA copy_rect_patch is missing its copy section");
                    AfmaNativeImageHelper.copyRectMemmove(canvas, copyRect);

                    AfmaRect dirtyRect = new AfmaRect(copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight());
                    AfmaPatchRegion patch = descriptor.getPatch();
                    if (patch != null) {
                        this.copyPayloadIntoCanvas(this.requirePixelPayload(preparedFrame.patchPayload, "AFMA copy_rect_patch patch payload was NULL"),
                                canvas, patch.getX(), patch.getY());
                        dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(patch.getX(), patch.getY(), patch.getWidth(), patch.getHeight()));
                    }
                    this.uploadDirtyRect(currentTexture, canvas, dirtyRect);
                }
                case MULTI_COPY_PATCH -> {
                    AfmaMultiCopy multiCopy = Objects.requireNonNull(descriptor.getMultiCopy(), "AFMA multi_copy_patch is missing its multi-copy section");
                    AfmaNativeImageHelper.copyRectsMemmove(canvas, multiCopy.getCopyRects());

                    AfmaRect dirtyRect = null;
                    for (AfmaCopyRect copyRect : multiCopy.getCopyRects()) {
                        dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight()));
                    }

                    AfmaPatchRegion patch = descriptor.getPatch();
                    if (patch != null) {
                        this.copyPayloadIntoCanvas(this.requirePixelPayload(preparedFrame.patchPayload, "AFMA multi_copy_patch patch payload was NULL"),
                                canvas, patch.getX(), patch.getY());
                        dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(patch.getX(), patch.getY(), patch.getWidth(), patch.getHeight()));
                    }
                    this.uploadDirtyRect(currentTexture, canvas, Objects.requireNonNull(dirtyRect, "AFMA multi_copy_patch did not mark any dirty pixels"));
                }
                case COPY_RECT_RESIDUAL_PATCH -> {
                    AfmaCopyRect copyRect = Objects.requireNonNull(descriptor.getCopy(), "AFMA copy_rect_residual_patch is missing its copy section");
                    AfmaNativeImageHelper.copyRectMemmove(canvas, copyRect);
                    this.applyResidualPayload(
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA copy_rect_residual_patch payload was NULL"),
                            canvas,
                            descriptor.getX(),
                            descriptor.getY(),
                            descriptor.getWidth(),
                            descriptor.getHeight(),
                            Objects.requireNonNull(descriptor.getResidual(), "AFMA copy_rect_residual_patch metadata was NULL")
                    );
                    AfmaRect dirtyRect = AfmaRect.union(
                            new AfmaRect(copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight()),
                            new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight())
                    );
                    this.uploadDirtyRect(currentTexture, canvas, dirtyRect);
                }
                case MULTI_COPY_RESIDUAL_PATCH -> {
                    AfmaMultiCopy multiCopy = Objects.requireNonNull(descriptor.getMultiCopy(), "AFMA multi_copy_residual_patch is missing its multi-copy section");
                    AfmaNativeImageHelper.copyRectsMemmove(canvas, multiCopy.getCopyRects());
                    this.applyResidualPayload(
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA multi_copy_residual_patch payload was NULL"),
                            canvas,
                            descriptor.getX(),
                            descriptor.getY(),
                            descriptor.getWidth(),
                            descriptor.getHeight(),
                            Objects.requireNonNull(descriptor.getResidual(), "AFMA multi_copy_residual_patch metadata was NULL")
                    );

                    AfmaRect dirtyRect = null;
                    for (AfmaCopyRect copyRect : multiCopy.getCopyRects()) {
                        dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight()));
                    }
                    dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight()));
                    this.uploadDirtyRect(currentTexture, canvas, Objects.requireNonNull(dirtyRect, "AFMA multi_copy_residual_patch did not mark any dirty pixels"));
                }
                case MULTI_COPY_SPARSE_PATCH -> {
                    AfmaMultiCopy multiCopy = Objects.requireNonNull(descriptor.getMultiCopy(), "AFMA multi_copy_sparse_patch is missing its multi-copy section");
                    AfmaNativeImageHelper.copyRectsMemmove(canvas, multiCopy.getCopyRects());
                    this.applySparseResidualPayload(
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA multi_copy_sparse_patch layout payload was NULL"),
                            this.requireRawPayload(preparedFrame.patchPayload, "AFMA multi_copy_sparse_patch residual payload was NULL"),
                            canvas,
                            descriptor.getX(),
                            descriptor.getY(),
                            descriptor.getWidth(),
                            descriptor.getHeight(),
                            Objects.requireNonNull(descriptor.getSparse(), "AFMA multi_copy_sparse_patch metadata was NULL")
                    );

                    AfmaRect dirtyRect = null;
                    for (AfmaCopyRect copyRect : multiCopy.getCopyRects()) {
                        dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(copyRect.getDstX(), copyRect.getDstY(), copyRect.getWidth(), copyRect.getHeight()));
                    }
                    dirtyRect = AfmaRect.union(dirtyRect, new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight()));
                    this.uploadDirtyRect(currentTexture, canvas, Objects.requireNonNull(dirtyRect, "AFMA multi_copy_sparse_patch did not mark any dirty pixels"));
                }
                case BLOCK_INTER -> {
                    this.applyBlockInterPayload(
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA block_inter payload was NULL"),
                            canvas,
                            descriptor.getX(),
                            descriptor.getY(),
                            descriptor.getWidth(),
                            descriptor.getHeight(),
                            Objects.requireNonNull(descriptor.getBlockInter(), "AFMA block_inter metadata was NULL")
                    );
                    this.uploadDirtyRect(currentTexture, canvas, new AfmaRect(descriptor.getX(), descriptor.getY(), descriptor.getWidth(), descriptor.getHeight()));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to apply AFMA frame " + preparedFrame.index + " (" + type + ")", ex);
        }
    }

    protected void uploadDirtyRect(@NotNull DynamicTexture texture, @NotNull NativeImage canvas, @NotNull AfmaRect dirtyRect) {
        if ((dirtyRect.width() <= 0) || (dirtyRect.height() <= 0)) {
            return;
        }
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

    protected void applyResidualPayload(@NotNull RawPayload residualPayload, @NotNull NativeImage canvas,
                                        int dstX, int dstY, int width, int height, @NotNull AfmaResidualPayload residualMetadata) throws IOException {
        AfmaNativeImageHelper.applyResidualPayload(canvas, dstX, dstY, width, height,
                residualPayload.bytes, residualPayload.offset, residualPayload.length, residualMetadata);
    }

    protected void applySparseResidualPayload(@NotNull RawPayload layoutPayload, @NotNull RawPayload residualPayload,
                                              @NotNull NativeImage canvas, int dstX, int dstY, int width, int height,
                                              @NotNull AfmaSparsePayload sparsePayload) throws IOException {
        AfmaNativeImageHelper.applySparseResidualPayload(
                canvas,
                dstX,
                dstY,
                width,
                height,
                layoutPayload.bytes,
                layoutPayload.offset,
                layoutPayload.length,
                residualPayload.bytes,
                residualPayload.offset,
                residualPayload.length,
                sparsePayload
        );
    }

    protected void applyBlockInterPayload(@NotNull RawPayload blockPayload, @NotNull NativeImage canvas,
                                          int regionX, int regionY, int regionWidth, int regionHeight,
                                          @NotNull AfmaBlockInter blockInter) throws IOException {
        NativeImage referenceCanvas = this.ensureBlockInterReferenceCanvas(canvas);
        AfmaBlockInterPayloadHelper.walkPayload(blockPayload.bytes, blockPayload.offset, blockPayload.length,
                blockInter.getTileSize(), regionWidth, regionHeight,
                (localX, localY, tileWidth, tileHeight, mode, dx, dy, channels, changedPixelCount, sparsePayload, payloadBytes, primaryOffset, primaryLength, secondaryOffset, secondaryLength) -> {
                    int dstX = regionX + localX;
                    int dstY = regionY + localY;
                    switch (mode) {
                        case SKIP -> {
                        }
                        case COPY -> AfmaNativeImageHelper.copyRect(referenceCanvas, dstX + dx, dstY + dy, canvas, dstX, dstY, tileWidth, tileHeight);
                        case COPY_DENSE -> {
                            AfmaNativeImageHelper.copyRect(referenceCanvas, dstX + dx, dstY + dy, canvas, dstX, dstY, tileWidth, tileHeight);
                            AfmaNativeImageHelper.applyResidualBytes(canvas, dstX, dstY, tileWidth, tileHeight,
                                    payloadBytes, primaryOffset, primaryLength, channels);
                        }
                        case COPY_SPARSE -> {
                            AfmaNativeImageHelper.copyRect(referenceCanvas, dstX + dx, dstY + dy, canvas, dstX, dstY, tileWidth, tileHeight);
                            AfmaNativeImageHelper.applySparseResidualPayloadUnchecked(
                                    canvas,
                                    dstX,
                                    dstY,
                                    tileWidth,
                                    tileHeight,
                                    payloadBytes,
                                    primaryOffset,
                                    primaryLength,
                                    payloadBytes,
                                    secondaryOffset,
                                    secondaryLength,
                                    Objects.requireNonNull(sparsePayload, "AFMA block inter sparse tile metadata was NULL")
                            );
                        }
                        case RAW -> this.applyRawTilePayload(payloadBytes, primaryOffset, primaryLength, channels, canvas, dstX, dstY, tileWidth, tileHeight);
                    }
                });
    }

    @NotNull
    protected NativeImage ensureBlockInterReferenceCanvas(@NotNull NativeImage sourceCanvas) {
        NativeImage referenceCanvas = this.blockInterReferenceCanvas;
        if ((referenceCanvas != null)
                && (referenceCanvas.getWidth() == sourceCanvas.getWidth())
                && (referenceCanvas.getHeight() == sourceCanvas.getHeight())) {
            return referenceCanvas;
        }

        CloseableUtils.closeQuietly(referenceCanvas);
        this.blockInterReferenceCanvas = new NativeImage(sourceCanvas.getWidth(), sourceCanvas.getHeight(), true);
        return Objects.requireNonNull(this.blockInterReferenceCanvas);
    }

    protected void applyRawTilePayload(@NotNull byte[] rawBytes, int rawOffset, int rawLength, int channels, @NotNull NativeImage canvas,
                                       int dstX, int dstY, int width, int height) {
        AfmaNativeImageHelper.blitInterleavedBytes(canvas, dstX, dstY, width, height, rawBytes, rawOffset, rawLength, channels);
    }

    @NotNull
    protected PixelPayload requirePixelPayload(@Nullable FramePayload payload, @NotNull String message) {
        if (payload instanceof PixelPayload pixelPayload) {
            return pixelPayload;
        }
        throw new IllegalStateException(message);
    }

    @NotNull
    protected RawPayload requireRawPayload(@Nullable FramePayload payload, @NotNull String message) {
        if (payload instanceof RawPayload rawPayload) {
            return rawPayload;
        }
        throw new IllegalStateException(message);
    }

    /**
     * If the frame is the first frame after a reset that already shows, because the texture got the reset frame snapshot back. The frame must not get applied again then:
     * the snapshot is a fully composed frame, and applying delta, copy, residual or block-inter operations onto it again would break the picture.
     */
    protected boolean isRestoredResetFrame(@NotNull PreparedFrame frame) {
        return this.playbackResetFrameRestored && (frame.index == 0) && (frame.intro || (this.introFrameCount <= 0));
    }

    protected boolean isAtNormalCycleBoundary() {
        return this.playbackInitialized && !this.playbackIntro && (this.frameCount > 0) && (this.playbackIndex == (this.frameCount - 1));
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

    /**
     * A single infinitely looping main frame gets held instead of replayed, so it never reaches the normal cycle boundary.
     * Its first full display still counts as a completed play cycle.
     */
    protected void completeHeldMainFrameCycle(long now) {
        if ((now - this.playbackFrameStartMs) < Math.max(MIN_FRAME_DELAY_MS, this.playbackFrameDelayMs)) return;
        this.cycles.compareAndSet(0, 1);
    }

    /**
     * If the texture holds its visible frame, because it got rendered while paused and didn't get rendered playing since. The stream thread doesn't decode ahead then.
     * Pauses while nobody renders the texture paused don't hold it, so pauses of other users of a shared texture don't stall it.
     */
    protected boolean isHoldingPausedFrame() {
        return this.playbackInitialized && (this.pausedAtMs >= 0L);
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
        this.pausedAtMs = -1L;
        this.playbackResetFrameRestorePending = false;
        this.playbackResetFrameRestored = false;
        this.decodeIntro = this.introFrameCount > 0;
        this.decodeIndex = 0;

        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }

        // Ends the replaced stream right away if it sleeps, a read it runs right now finishes first and gets dropped
        LockSupport.unpark(this.streamThread);

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

        this.lastResourceLocationCall = this.currentTimeMillis();
        if (this.loadingFailed.get()) return FULLY_TRANSPARENT_TEXTURE;
        if (!this.decoded.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.startTickerIfNeeded();
        try {
            if (this.playbackResetFrameRestorePending && this.resetFrame.restoreTo(this.streamingTexture)) {
                this.playbackResetFrameRestorePending = false;
                this.playbackResetFrameRestored = true;
            }
            this.advancePlaybackIfNeeded();
        } catch (Exception ex) {
            this.failStreaming("AFMA playback failed on the client thread", ex);
            return FULLY_TRANSPARENT_TEXTURE;
        }

        if (this.loadingFailed.get()) return FULLY_TRANSPARENT_TEXTURE;
        return (this.loadingCompleted.get() && (this.streamingResourceLocation != null)) ? this.streamingResourceLocation : FULLY_TRANSPARENT_TEXTURE;
    }

    /**
     * The clock playback runs on.
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
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
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL, WebUtils.WebResourceType.STREAMED_ANIMATED_ARCHIVE);
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
        this.playbackResetFrameRestorePending = this.resetFrame.requestRestore();
        this.startTickerIfNeeded();
    }

    private void notifyAnimatedTextureStarted(boolean willRestart) {
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

    private String resolveTextureSourceType() {
        if (this.sourceURL != null) return "WEB";
        if (this.sourceFile != null) return "LOCAL";
        if (this.sourceLocation != null) return "RESOURCE_LOCATION";
        return "UNKNOWN";
    }

    @Override
    public void play() {
        if (this.closed.get() || this.loadingFailed.get()) return;

        if (this.maxLoopsReached) return;

        this.playRequested = true;
        this.pausedRequested = false;
        this.startTickerIfNeeded();
    }

    @Override
    public boolean isPlaying() {
        return !this.closed.get() && this.playRequested && !this.pausedRequested && !this.maxLoopsReached;
    }

    @Override
    public int getCompletedPlayCycles() {
        return this.cycles.get();
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

        // Ends the stream right away if it sleeps, a read it runs right now finishes first
        LockSupport.unpark(this.streamThread);

        synchronized (this.streamStateLock) {
            this.clearPrefetchedFramesLocked();
        }

        this.releaseStreamingTextureNow();
        this.playbackResetFrameRestorePending = false;
        this.playbackResetFrameRestored = false;
        this.resetFrame.close();
        // Released before the streaming decoder, because a loading thread that finishes right now first makes its decoder the streaming decoder and then stops tracking it
        AfmaDecoder activeLoadingDecoder = this.loadingDecoder;
        if (activeLoadingDecoder != null) {
            activeLoadingDecoder.closeArchiveFile();
        }
        this.requestDecoderRelease();

        this.sourceLocation = null;
        this.sourceFile = null;
        this.sourceURL = null;
    }

    /**
     * Lets the stream thread wait until the time passed or until a reset, close or failure wakes it up with {@link LockSupport#unpark(Thread)}.<br>
     * <b>Never interrupt the stream thread instead:</b> legacy ZIP archives get read through a {@link java.nio.channels.FileChannel}, which an interrupt during a read closes for good,
     * so every later frame would fail and the texture would stop playing.
     */
    protected static void sleepQuietly(long millis) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Math.max(1L, millis)));
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
        if (running != Thread.currentThread()) {
            // Ends the stream right away if it sleeps
            LockSupport.unpark(running);
        }
        this.requestDecoderRelease();
        this.playbackResetFrameRestorePending = false;
        this.playbackResetFrameRestored = false;
        this.resetFrame.clear();
        this.scheduleStreamingTextureRelease();
        if (throwable != null) {
            LOGGER.error("[FANCYMENU] {}", message, throwable);
        } else {
            LOGGER.error("[FANCYMENU] {}", message);
        }
    }

    /**
     * Releases the archive file of the decoder right away, so it is never open anymore once the texture got closed or failed. A read of the stream thread that runs right now finishes first.
     * The rest of the decoder gets released by the stream thread once it stops, if it still runs, because it can still be using it.
     */
    protected void requestDecoderRelease() {
        AfmaDecoder activeDecoder = this.decoder;
        if (activeDecoder != null) {
            activeDecoder.closeArchiveFile();
        }
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
        this.frameIndex = null;
        this.sequenceUsesBlockInter = false;
        this.payloadDecodeScratch.clear();
        CloseableUtils.closeQuietly(this.blockInterReferenceCanvas);
        this.blockInterReferenceCanvas = null;
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
        protected final AfmaFrameDescriptor descriptor;
        @Nullable
        protected FramePayload primaryPayload;
        @Nullable
        protected FramePayload patchPayload;

        protected PreparedFrame(boolean intro, int index, long delayMs, @NotNull AfmaFrameDescriptor descriptor,
                                @Nullable FramePayload primaryPayload, @Nullable FramePayload patchPayload) {
            this.intro = intro;
            this.index = index;
            this.delayMs = delayMs;
            this.descriptor = descriptor;
            this.primaryPayload = primaryPayload;
            this.patchPayload = patchPayload;
        }

        @Override
        public void close() {
            this.primaryPayload = null;
            this.patchPayload = null;
        }
    }

    protected interface FramePayload {
    }

    protected static class PixelPayload implements FramePayload {
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

    protected static class RawPayload implements FramePayload {
        @NotNull
        protected final byte[] bytes;
        protected final int offset;
        protected final int length;

        protected RawPayload(@NotNull byte[] bytes) {
            this(bytes, 0, bytes.length);
        }

        protected RawPayload(@NotNull byte[] bytes, int offset, int length) {
            this.bytes = bytes;
            this.offset = Math.max(0, offset);
            this.length = Math.max(0, Math.min(length, bytes.length - this.offset));
        }
    }

    public record DecodedAfmaImage(@NotNull AfmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

    /**
     * Reads an archive with the decoder, like {@link AfmaDecoder#read(File)}.
     */
    @FunctionalInterface
    protected interface ArchiveReader {

        void read(@NotNull AfmaDecoder decoder) throws IOException;

    }

}
