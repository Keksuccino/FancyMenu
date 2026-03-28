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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

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
            AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame descriptor type was NULL");
            switch (type) {
                case FULL, DELTA_RECT -> primaryPayload = this.readPngPayload(activeDecoder, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
                case RESIDUAL_DELTA_RECT -> primaryPayload = this.readRawPayload(activeDecoder, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
                case SPARSE_DELTA_RECT -> {
                    primaryPayload = this.readRawPayload(activeDecoder, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
                    patchPayload = this.readRawPayload(activeDecoder, Objects.requireNonNull(descriptor.getSecondaryPayloadPath()));
                }
                case SAME -> {
                }
                case COPY_RECT_PATCH -> {
                    if (descriptor.requiresPatchPayload()) {
                        patchPayload = this.readPngPayload(activeDecoder, Objects.requireNonNull(descriptor.getSecondaryPayloadPath()));
                    }
                }
                case COPY_RECT_RESIDUAL_PATCH -> primaryPayload = this.readRawPayload(activeDecoder, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
                case COPY_RECT_SPARSE_PATCH -> {
                    primaryPayload = this.readRawPayload(activeDecoder, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
                    patchPayload = this.readRawPayload(activeDecoder, Objects.requireNonNull(descriptor.getSecondaryPayloadPath()));
                }
                case BLOCK_INTER -> primaryPayload = this.readRawPayload(activeDecoder, Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
            }
        } catch (Exception ex) {
            this.failStreaming("Failed to decode AFMA payload for " + (intro ? "intro" : "normal") + " frame " + index, ex);
            return null;
        }

        if (!this.advanceDecodeCursor(generation)) {
            return null;
        }
        return new PreparedFrame(intro, index, delay, descriptor, primaryPayload, patchPayload);
    }

    @NotNull
    protected PixelPayload readPngPayload(@NotNull AfmaDecoder activeDecoder, @NotNull String payloadPath) throws IOException {
        InputStream payloadInput = activeDecoder.openPayload(payloadPath);
        if (payloadInput == null) {
            throw new FileNotFoundException("AFMA payload input stream was NULL: " + payloadPath);
        }
        try (InputStream closeableInput = payloadInput) {
            MemoryCacheImageInputStream imageInput = new MemoryCacheImageInputStream(closeableInput);
            try {
                BufferedImage image = ImageIO.read(imageInput);
                if (image == null) {
                    throw new IOException("Failed to decode AFMA PNG payload: " + payloadPath);
                }
                PixelPayload directPayload = createDirectPixelPayload(image);
                if (directPayload != null) {
                    return directPayload;
                }

                BufferedImage normalizedImage = normalizePayloadImage(image);
                directPayload = createDirectPixelPayload(normalizedImage);
                if (directPayload != null) {
                    return directPayload;
                }

                int width = normalizedImage.getWidth();
                int height = normalizedImage.getHeight();
                int[] pixels = new int[width * height];
                normalizedImage.getRGB(0, 0, width, height, pixels, 0, width);
                return new PixelPayload(width, height, pixels, 0, width, false);
            } finally {
                try {
                    imageInput.close();
                } catch (IOException ex) {
                    if (!"closed".equalsIgnoreCase(String.valueOf(ex.getMessage()))) {
                        throw ex;
                    }
                }
            }
        }
    }

    @NotNull
    protected RawPayload readRawPayload(@NotNull AfmaDecoder activeDecoder, @NotNull String payloadPath) throws IOException {
        InputStream payloadInput = activeDecoder.openPayload(payloadPath);
        if (payloadInput == null) {
            throw new FileNotFoundException("AFMA payload input stream was NULL: " + payloadPath);
        }
        try (InputStream closeableInput = payloadInput) {
            return new RawPayload(closeableInput.readAllBytes());
        }
    }

    @Nullable
    protected static PixelPayload createDirectPixelPayload(@NotNull BufferedImage image) {
        int imageType = image.getType();
        if ((imageType != BufferedImage.TYPE_INT_ARGB)
                && (imageType != BufferedImage.TYPE_INT_RGB)) {
            return null;
        }

        Raster raster = image.getRaster();
        DataBuffer dataBuffer = raster.getDataBuffer();
        if (!(dataBuffer instanceof DataBufferInt intBuffer)) {
            return null;
        }
        if (intBuffer.getNumBanks() != 1) {
            return null;
        }
        if (!(raster.getSampleModel() instanceof SinglePixelPackedSampleModel sampleModel)) {
            return null;
        }

        int[] pixels = intBuffer.getData();
        if (pixels.length <= 0) {
            return null;
        }

        int stride = sampleModel.getScanlineStride();
        int offset = intBuffer.getOffset();
        offset += ((raster.getMinY() - raster.getSampleModelTranslateY()) * stride);
        offset += (raster.getMinX() - raster.getSampleModelTranslateX());
        return new PixelPayload(image.getWidth(), image.getHeight(), pixels, offset, stride, imageType == BufferedImage.TYPE_INT_RGB);
    }

    @NotNull
    protected static BufferedImage normalizePayloadImage(@NotNull BufferedImage image) {
        int targetType = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage normalized = new BufferedImage(image.getWidth(), image.getHeight(), targetType);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return normalized;
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
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA sparse delta mask payload was NULL"),
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
                            this.requireRawPayload(preparedFrame.primaryPayload, "AFMA copy_rect_sparse_patch mask payload was NULL"),
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
                                        int dstX, int dstY, int width, int height, @NotNull AfmaResidualPayload residualMetadata) {
        AfmaNativeImageHelper.applyResidualBytes(canvas, dstX, dstY, width, height, residualPayload.bytes, residualMetadata.getChannels());
    }

    protected void applySparseResidualPayload(@NotNull RawPayload maskPayload, @NotNull RawPayload residualPayload,
                                              @NotNull NativeImage canvas, int dstX, int dstY, int width, int height,
                                              @NotNull AfmaSparsePayload sparsePayload) {
        AfmaNativeImageHelper.applySparseResidualBytes(
                canvas,
                dstX,
                dstY,
                width,
                height,
                maskPayload.bytes,
                residualPayload.bytes,
                sparsePayload.getChangedPixelCount(),
                sparsePayload.getChannels()
        );
    }

    protected void applyBlockInterPayload(@NotNull RawPayload blockPayload, @NotNull NativeImage canvas,
                                          int regionX, int regionY, int regionWidth, int regionHeight,
                                          @NotNull AfmaBlockInter blockInter) throws IOException {
        NativeImage referenceCanvas = this.ensureBlockInterReferenceCanvas(canvas);
        AfmaBlockInterPayloadHelper.walkPayload(blockPayload.bytes, blockInter.getTileSize(), regionWidth, regionHeight,
                (localX, localY, tileWidth, tileHeight, mode, dx, dy, channels, changedPixelCount, primaryBytes, secondaryBytes) -> {
                    int dstX = regionX + localX;
                    int dstY = regionY + localY;
                    switch (mode) {
                        case SKIP -> {
                        }
                        case COPY -> AfmaNativeImageHelper.copyRect(referenceCanvas, dstX + dx, dstY + dy, canvas, dstX, dstY, tileWidth, tileHeight);
                        case COPY_DENSE -> {
                            AfmaNativeImageHelper.copyRect(referenceCanvas, dstX + dx, dstY + dy, canvas, dstX, dstY, tileWidth, tileHeight);
                            this.applyResidualPayload(new RawPayload(Objects.requireNonNull(primaryBytes)), canvas, dstX, dstY, tileWidth, tileHeight, new AfmaResidualPayload(channels));
                        }
                        case COPY_SPARSE -> {
                            AfmaNativeImageHelper.copyRect(referenceCanvas, dstX + dx, dstY + dy, canvas, dstX, dstY, tileWidth, tileHeight);
                            this.applySparseResidualPayload(new RawPayload(Objects.requireNonNull(primaryBytes)), new RawPayload(Objects.requireNonNull(secondaryBytes)),
                                    canvas, dstX, dstY, tileWidth, tileHeight, new AfmaSparsePayload(null, changedPixelCount, channels));
                        }
                        case RAW -> this.applyRawTilePayload(Objects.requireNonNull(primaryBytes), channels, canvas, dstX, dstY, tileWidth, tileHeight);
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

    protected void applyRawTilePayload(@NotNull byte[] rawBytes, int channels, @NotNull NativeImage canvas,
                                       int dstX, int dstY, int width, int height) {
        int expectedBytes = AfmaBlockInterPayloadHelper.expectedRawTileBytes(width, height, channels);
        if ((expectedBytes <= 0) || (rawBytes.length != expectedBytes)) {
            throw new IllegalStateException("AFMA block_inter raw tile payload size does not match the descriptor");
        }

        int[] unpackedPixels = new int[width * height];
        int rawIndex = 0;
        int pixelIndex = 0;
        for (int localY = 0; localY < height; localY++) {
            for (int localX = 0; localX < width; localX++) {
                int red = rawBytes[rawIndex++] & 0xFF;
                int green = rawBytes[rawIndex++] & 0xFF;
                int blue = rawBytes[rawIndex++] & 0xFF;
                int alpha = (channels == AfmaResidualPayloadHelper.RGBA_CHANNELS) ? (rawBytes[rawIndex++] & 0xFF) : 0xFF;
                unpackedPixels[pixelIndex++] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
        AfmaNativeImageHelper.blitPixels(canvas, dstX, dstY, width, height, unpackedPixels, 0, width, false);
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
        this.frameIndex = null;
        this.sequenceUsesBlockInter = false;
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

        protected RawPayload(@NotNull byte[] bytes) {
            this.bytes = bytes;
        }
    }

    public record DecodedAfmaImage(@NotNull AfmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}
