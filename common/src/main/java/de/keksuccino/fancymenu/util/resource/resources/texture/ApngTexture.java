package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.watermedia.WatermediaAnimatedTextureBackend;
import de.keksuccino.fancymenu.util.watermedia.WatermediaUtil;
import net.ellerton.japng.Png;
import net.ellerton.japng.argb8888.Argb8888Bitmap;
import net.ellerton.japng.argb8888.Argb8888BitmapSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ApngTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected volatile List<ApngFrame> frames = new ArrayList<>();
    @Nullable
    protected volatile ApngTexture.ApngFrame current = null;
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile int width = 10;
    protected volatile int height = 10;
    protected volatile long lastResourceLocationCall = -1;
    protected final AtomicBoolean tickerThreadRunning = new AtomicBoolean(false);
    protected final AtomicBoolean decoded = new AtomicBoolean(false);
    protected volatile boolean allFramesDecoded = false;
    protected final AtomicInteger cycles = new AtomicInteger(0);
    /** How many times the APNG should loop. Value <= 0 means infinite loops. **/
    protected final AtomicInteger numPlays = new AtomicInteger(0);
    protected Identifier sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected final AtomicBoolean loadingCompleted = new AtomicBoolean(false);
    protected final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();
    protected int frameRegistrationCounter = 0;
    protected volatile boolean maxLoopsReached = false;
    protected volatile boolean pendingStartEvent = true;
    protected final AtomicBoolean closed = new AtomicBoolean(false);
    @Nullable
    protected volatile WatermediaAnimatedTextureBackend watermediaBackend = null;
    @Nullable
    protected volatile byte[] watermediaFallbackData = null;
    @Nullable
    protected volatile String sourceName = null;
    protected final AtomicBoolean watermediaFallbackTriggered = new AtomicBoolean(false);

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
            texture.loadingFailed.set(true);
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
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read APNG image from file! File not found: " + apngFile.getPath());
            return texture;
        }

        //Decode APNG image
        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(apngFile);
                of(in, apngFile.getPath(), texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read APNG image from file: " + apngFile.getPath(), ex);
            }
        }).start();

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

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(apngUrl))) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read APNG image from URL! Invalid URL: " + apngUrl);
            return texture;
        }

        //Download and decode APNG image
        new Thread(() -> {
            try {
                populateTexture(texture, null, apngUrl);
                if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                texture.decoded.set(true);
                LOGGER.error("[FANCYMENU] Failed to read APNG image from URL: " + apngUrl, ex);
            }
        }).start();

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in, @Nullable String apngTextureName, @Nullable ApngTexture writeTo) {

        Objects.requireNonNull(in);

        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();

        //Decode APNG image
        new Thread(() -> {
            populateTexture(texture, in, (apngTextureName != null) ? apngTextureName : "[Generic InputStream Source]");
            if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }).start();

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected static void populateTexture(@NotNull ApngTexture texture, @Nullable InputStream in, @NotNull String apngTextureName) {
        InputStream readInput = in;
        texture.sourceName = apngTextureName;
        texture.watermediaFallbackTriggered.set(false);
        texture.watermediaFallbackData = null;
        if (!texture.closed.get()) {
            boolean decodedByWatermedia = false;
            if (WatermediaUtil.isWatermediaLoaded()) {
                LOGGER.info("[FANCYMENU] Starting APNG loading via Watermedia (direct source preferred): {}", apngTextureName);
                decodedByWatermedia = populateTextureWithWatermediaDirectSource(texture, apngTextureName);
            }

            byte[] apngData = null;
            if (!decodedByWatermedia) {
                if (readInput == null) {
                    try {
                        readInput = texture.open();
                    } catch (Exception ex) {
                        texture.loadingFailed.set(true);
                        texture.decoded.set(true);
                        LOGGER.error("[FANCYMENU] Failed to open APNG image data stream: " + apngTextureName, ex);
                        CloseableUtils.closeQuietly(readInput);
                        return;
                    }
                }
                if (readInput == null) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[FANCYMENU] Failed to open APNG image data stream: {}", apngTextureName);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                try {
                    apngData = readInput.readAllBytes();
                } catch (Exception ex) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[FANCYMENU] Failed to read APNG image data: " + apngTextureName, ex);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
            }

            if (!decodedByWatermedia && WatermediaUtil.isWatermediaLoaded()) {
                decodedByWatermedia = populateTextureWithWatermedia(texture, apngData, apngTextureName);
                if (decodedByWatermedia) {
                    WatermediaUtil.WATERMEDIA_initialized = true;
                } else {
                    LOGGER.warn("[FANCYMENU] Watermedia APNG decoding failed, falling back to primitive decoder: {}", apngTextureName);
                }
            }

            if (!decodedByWatermedia) {
                if (apngData == null) {
                    texture.loadingFailed.set(true);
                    LOGGER.error("[FANCYMENU] Failed to read APNG image data: {}", apngTextureName);
                    texture.decoded.set(true);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                populateTextureWithPrimitiveDecoder(texture, apngData, apngTextureName);
            }
        }
        texture.decoded.set(true);
        CloseableUtils.closeQuietly(readInput);
    }

    protected static boolean populateTextureWithWatermediaDirectSource(@NotNull ApngTexture texture, @NotNull String apngTextureName) {
        String directSource = null;
        if (texture.sourceURL != null) {
            directSource = texture.sourceURL;
        } else if ((texture.sourceFile != null) && texture.sourceFile.isFile()) {
            // Java's URLConnection usually reports ".apng" files as unknown media type.
            // If we pass that directly, Watermedia can resolve it as a generic source and use FF instead of Tx.
            // Fall back to the byte path for local APNG files so we can provide a ".png" source extension.
            return false;
        }
        if (directSource == null) return false;

        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "apng");
        backend.setLoopCount(-1);
        boolean initialized = backend.initializeFromSource(directSource, apngTextureName);
        if (!initialized) {
            backend.close();
            return false;
        }
        texture.watermediaBackend = backend;
        texture.watermediaFallbackData = null;
        texture.decoded.set(true);
        return true;
    }

    protected static void populateTextureWithPrimitiveDecoder(@NotNull ApngTexture texture, @NotNull byte[] apngData, @NotNull String apngTextureName) {
        texture.watermediaFallbackData = null;
        DecodedApngImage decodedImage = decodeApng(new ByteArrayInputStream(apngData), apngTextureName);
        if (decodedImage == null) {
            LOGGER.error("[FANCYMENU] Failed to read APNG image, because DecodedApngImage was NULL: " + apngTextureName);
            texture.loadingFailed.set(true);
            return;
        }
        texture.width = decodedImage.imageWidth;
        texture.height = decodedImage.imageHeight;
        texture.aspectRatio = new AspectRatio(decodedImage.imageWidth, decodedImage.imageHeight);
        texture.numPlays.set(decodedImage.numPlays);
        texture.decoded.set(true);
        try {
            deliverApngFrames(decodedImage.sequence(), apngTextureName, true, frame -> {
                if (frame != null) {
                    try {
                        if ((frame.nativeImage == null) && (frame.frameInputStream != null)) {
                            frame.nativeImage = NativeImage.read(frame.frameInputStream);
                        }
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to read frame of APNG image into NativeImage: " + apngTextureName, ex);
                    }
                    CloseableUtils.closeQuietly(frame.closeAfterLoading);
                    CloseableUtils.closeQuietly(frame.frameInputStream);
                    texture.frames.add(frame);
                }
            });
            texture.loadingCompleted.set(true);
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read frames of APNG image: " + apngTextureName, ex);
        }
        texture.allFramesDecoded = true;
    }

    protected static boolean populateTextureWithWatermedia(@NotNull ApngTexture texture, @NotNull byte[] apngData, @NotNull String apngTextureName) {
        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "apng");
        backend.setLoopCount(readApngLoopCount(apngData));
        // Use ".png" extension so Watermedia classifies the media as IMAGE and routes playback through TxMediaPlayer.
        boolean initialized = backend.initializeFromBytes(apngData, ".png", apngTextureName);
        if (!initialized) {
            backend.close();
            return false;
        }
        texture.watermediaBackend = backend;
        texture.watermediaFallbackData = apngData;
        texture.decoded.set(true);
        return true;
    }

    protected static int readApngLoopCount(@NotNull byte[] apngData) {
        // Default to infinite loops if the acTL chunk cannot be read.
        if (apngData.length < 8) return -1;
        int index = 8; // skip PNG signature
        while ((index + 12) <= apngData.length) {
            int chunkLength = ((apngData[index] & 255) << 24)
                    | ((apngData[index + 1] & 255) << 16)
                    | ((apngData[index + 2] & 255) << 8)
                    | (apngData[index + 3] & 255);
            int chunkType = ((apngData[index + 4] & 255) << 24)
                    | ((apngData[index + 5] & 255) << 16)
                    | ((apngData[index + 6] & 255) << 8)
                    | (apngData[index + 7] & 255);
            if (chunkType == 0x6163544C) { // acTL
                int dataStart = index + 8;
                if ((dataStart + 8) <= apngData.length) {
                    int plays = ((apngData[dataStart + 4] & 255) << 24)
                            | ((apngData[dataStart + 5] & 255) << 16)
                            | ((apngData[dataStart + 6] & 255) << 8)
                            | (apngData[dataStart + 7] & 255);
                    return (plays == 0) ? -1 : Math.max(1, plays);
                }
                return -1;
            }
            long next = index + 12L + chunkLength;
            if (next > apngData.length) break;
            index = (int) next;
        }
        return -1;
    }

    protected ApngTexture() {
    }

    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning.get() && !this.frames.isEmpty() && !this.maxLoopsReached && !this.closed.get()) {

            this.tickerThreadRunning.set(true);
            this.lastResourceLocationCall = System.currentTimeMillis();

            new Thread(() -> {

                //Automatically stop thread if APNG was inactive for >=10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
                    if (this.frames.isEmpty() || this.closed.get()) break;
                    //Don't tick frame if max loops reached
                    if (this.maxLoopsReached) break;
                    boolean sleep = false;
                    try {
                        boolean cachedAllDecoded = this.allFramesDecoded;
                        //Cache frames to avoid possible concurrent modification exceptions
                        List<ApngFrame> cachedFrames = new ArrayList<>(this.frames);
                        if (!cachedFrames.isEmpty()) {
                            //Set initial (first) frame if current is NULL
                            if (this.current == null) {
                                this.current = cachedFrames.get(0);
                                this.maybeEmitStartEvent(cachedFrames, this.current);
                                Thread.sleep(Math.max(20, cachedFrames.get(0).delayMs));
                            } else {
                                this.maybeEmitStartEvent(cachedFrames, this.current);
                            }
                            //Cache current frame to make sure it stays the same instance while working with it
                            ApngFrame cachedCurrent = this.current;
                            if (cachedCurrent != null) {
                                ApngFrame newCurrent = null;
                                int currentIndexIncrement = cachedCurrent.index + 1;
                                //Check if there's a frame after the current one and if so, go to the next frame
                                if (currentIndexIncrement < cachedFrames.size()) {
                                    newCurrent = cachedFrames.get(currentIndexIncrement);
                                } else if (cachedAllDecoded) {
                                    int cachedNumPlays = this.numPlays.get();
                                    //Count cycles up if APNG should not loop infinitely (numPlays > 0 = finite loops)
                                    if (cachedNumPlays > 0) {
                                        int newCycles = this.cycles.incrementAndGet();
                                        boolean willRestart = newCycles < cachedNumPlays;
                                        this.notifyAnimatedTextureFinished(willRestart);
                                        if (!willRestart) {
                                            this.maxLoopsReached = true;
                                            break; //end the while loop of the frame ticker
                                        }
                                        //If APNG has a finite number of loops but did not reach its max loops yet, reset to first frame, because end reached
                                        newCurrent = cachedFrames.get(0);
                                        this.pendingStartEvent = true;
                                    } else {
                                        //If APNG loops infinitely, reset to first frame, because end reached
                                        this.notifyAnimatedTextureFinished(true);
                                        newCurrent = cachedFrames.get(0);
                                        this.pendingStartEvent = true;
                                    }
                                }
                                if (newCurrent != null) {
                                    this.current = newCurrent;
                                    this.maybeEmitStartEvent(cachedFrames, this.current);
                                }
                                //Sleep for the new current frame's delay or sleep for 100ms if there's no new frame
                                Thread.sleep(Math.max(20, (newCurrent != null) ? newCurrent.delayMs : 100));
                            } else {
                                sleep = true;
                            }
                        } else {
                            sleep = true;
                        }
                    } catch (Exception ex) {
                        sleep = true;
                        LOGGER.error("[FANCYMENU] An error happened in the frame ticker thread on an APNG!", ex);
                    }
                    if (sleep) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] An error happened in the frame ticker thread on an APNG!", ex);
                        }
                    }
                }

                this.tickerThreadRunning.set(false);

            }).start();

        }
    }

    @Nullable
    @Override
    public Identifier getResourceLocation() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            this.width = backend.getWidth();
            this.height = backend.getHeight();
            this.aspectRatio = backend.getAspectRatio();
            Identifier resourceLocation = backend.getResourceLocation();
            return (resourceLocation != null) ? resourceLocation : FULLY_TRANSPARENT_TEXTURE;
        }
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        ApngFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    this.frameRegistrationCounter++;
                    frame.resourceLocation = Identifier.fromNamespaceAndPath("fancymenu", "dynamic/apng_frame_" + this.uniqueId + "_" + this.frameRegistrationCounter);
                    frame.dynamicTexture = new DynamicTexture(frame.resourceLocation::toString, frame.nativeImage);
                    Minecraft.getInstance().getTextureManager().register(frame.resourceLocation, frame.dynamicTexture);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to register APNG frame to Minecraft's TextureManager!", ex);
                }
                frame.loaded = true;
            }
            return (frame.resourceLocation != null) ? frame.resourceLocation : FULLY_TRANSPARENT_TEXTURE;
        }
        return null;
    }

    @Override
    public int getWidth() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.getWidth();
        return this.width;
    }

    @Override
    public int getHeight() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.getHeight();
        return this.height;
    }

    @Override
    public @NotNull AspectRatio getAspectRatio() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.getAspectRatio();
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
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isReady();
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded.get();
    }

    @Override
    public boolean isLoadingCompleted() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isLoadingCompleted();
        return !this.closed.get() && !this.loadingFailed.get() && this.loadingCompleted.get();
    }

    @Override
    public boolean isLoadingFailed() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isLoadingFailed();
        return this.loadingFailed.get();
    }

    public void reset() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.reset();
            return;
        }
        this.maxLoopsReached = false;
        this.current = null;
        this.pendingStartEvent = true;
        List<ApngFrame> frameList = new ArrayList<>(this.frames);
        if (!frameList.isEmpty()) {
            this.current = frameList.get(0);
        }
        this.cycles.set(0);
    }

    private void maybeEmitStartEvent(@NotNull List<ApngFrame> frames, @Nullable ApngFrame currentFrame) {
        if (!this.pendingStartEvent || currentFrame == null || frames.isEmpty()) return;
        if (currentFrame != frames.get(0)) return;
        this.pendingStartEvent = false;
        this.notifyAnimatedTextureStarted(this.willRestartAfterCurrentCycle());
    }

    private boolean willRestartAfterCurrentCycle() {
        int plays = this.numPlays.get();
        if (plays <= 0) return true;
        return (this.cycles.get() + 1) < plays;
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

    @NotNull
    protected String resolveTextureName() {
        return (this.sourceName != null) ? this.sourceName : this.resolveTextureSource();
    }

    private String resolveTextureSourceType() {
        if (this.sourceURL != null) return "WEB";
        if (this.sourceFile != null) return "LOCAL";
        if (this.sourceLocation != null) return "RESOURCE_LOCATION";
        return "UNKNOWN";
    }

    @Override
    public void play() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.play();
        }
    }

    @Override
    public boolean isPlaying() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isPlaying();
        return !this.maxLoopsReached;
    }

    @Override
    public void pause() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.pause();
        }
    }

    @Override
    public boolean isPaused() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) return backend.isPaused();
        return false;
    }

    @Override
    public void stop() {
        WatermediaAnimatedTextureBackend backend = this.resolveWatermediaBackend();
        if (backend != null) {
            backend.stop();
            return;
        }
        this.reset();
    }

    @Override
    public boolean isClosed() {
        return this.closed.get();
    }

    @Override
    public void close() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) {
            backend.close();
            this.watermediaBackend = null;
        }
        this.watermediaFallbackData = null;
        this.closed.set(true);
        this.sourceLocation = null;
        this.releasePrimitiveFrames();
    }

    @Nullable
    protected WatermediaAnimatedTextureBackend resolveWatermediaBackend() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if ((backend != null) && backend.isLoadingFailed()) {
            this.startPrimitiveFallbackAfterWatermediaFailure(backend);
            return null;
        }
        return backend;
    }

    protected void startPrimitiveFallbackAfterWatermediaFailure(@NotNull WatermediaAnimatedTextureBackend failedBackend) {
        if (!this.watermediaFallbackTriggered.compareAndSet(false, true)) return;
        if (this.watermediaBackend == failedBackend) {
            this.watermediaBackend = null;
        }
        failedBackend.close();
        String apngTextureName = this.resolveTextureName();
        LOGGER.warn("[FANCYMENU] Watermedia APNG playback failed, falling back to primitive decoder: {}", apngTextureName);
        this.preparePrimitiveFallbackState();
        new Thread(() -> this.loadPrimitiveFallback(apngTextureName)).start();
    }

    protected void preparePrimitiveFallbackState() {
        this.loadingCompleted.set(false);
        this.loadingFailed.set(false);
        this.decoded.set(false);
        this.allFramesDecoded = false;
        this.maxLoopsReached = false;
        this.pendingStartEvent = true;
        this.cycles.set(0);
        this.releasePrimitiveFrames();
    }

    protected void loadPrimitiveFallback(@NotNull String apngTextureName) {
        InputStream in = null;
        try {
            byte[] apngData = this.watermediaFallbackData;
            if (apngData == null) {
                in = this.open();
                if (in == null) {
                    this.loadingFailed.set(true);
                    LOGGER.error("[FANCYMENU] Failed to reopen APNG image data stream for Watermedia fallback: {}", apngTextureName);
                    return;
                }
                apngData = in.readAllBytes();
            }
            if (!this.closed.get()) {
                populateTextureWithPrimitiveDecoder(this, apngData, apngTextureName);
            }
        } catch (Exception ex) {
            this.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to decode APNG image after Watermedia fallback: " + apngTextureName, ex);
        } finally {
            this.decoded.set(true);
            this.watermediaFallbackData = null;
            CloseableUtils.closeQuietly(in);
            if (this.closed.get()) {
                MainThreadTaskExecutor.executeInMainThread(this::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            }
        }
    }

    protected void releasePrimitiveFrames() {
        for (ApngFrame frame : new ArrayList<>(this.frames)) {
            try {
                if (frame.dynamicTexture != null) frame.dynamicTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close DynamicTexture of APNG frame!", ex);
            }
            try {
                if (frame.nativeImage != null) frame.nativeImage.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close NativeImage of APNG frame!", ex);
            }
            frame.dynamicTexture = null;
            frame.nativeImage = null;
        }
        this.frames = new ArrayList<>();
        this.current = null;
    }

    @Nullable
    public static DecodedApngImage decodeApng(@NotNull InputStream in, @NotNull String apngName) {
        try {
            return decodeApng(Png.readArgb8888BitmapSequence(in));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode APNG image: " + apngName, ex);
        }
        return null;
    }

    @NotNull
    public static ApngTexture.DecodedApngImage decodeApng(@NotNull Argb8888BitmapSequence sequence) {
        int numPlays = -1;
        try {
            if (sequence.isAnimated()) {
                numPlays = sequence.getAnimationControl().loopForever() ? -1 : sequence.getAnimationControl().numPlays;
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to decode an APNG image!", ex);
        }
        return new DecodedApngImage(sequence, sequence.header.width, sequence.header.height, numPlays);
    }

    public static void deliverApngFrames(@NotNull Argb8888BitmapSequence sequence, @NotNull String apngName, boolean includeFirstFrame, @NotNull Consumer<ApngFrame> frameDelivery) {
        try {
            if (sequence.isAnimated()) {
                boolean defaultDelivered = false;
                if (sequence.hasDefaultImage() && includeFirstFrame) {
                    try {
                        BufferedImage frameImage = getBufferedImageFromBitmap(sequence.defaultImage, sequence.header.width, sequence.header.height, 0, 0);
                        ByteArrayOutputStream frameOut = new ByteArrayOutputStream();
                        ImageIO.write(frameImage, "PNG", frameOut);
                        ByteArrayInputStream frameIn = new ByteArrayInputStream(frameOut.toByteArray());
                        frameDelivery.accept(new ApngFrame(0, frameIn, 0, frameOut));
                        defaultDelivered = true;
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to decode default frame of APNG image: " + apngName, ex);
                    }
                }
                int index = defaultDelivered ? 1 : 0;
                int frameCount = 0;
                for (Argb8888BitmapSequence.Frame frame : sequence.getAnimationFrames()) {
                    try {
                        BufferedImage frameImage = getBufferedImageFromBitmap(frame.bitmap, sequence.header.width, sequence.header.height, frame.control.xOffset, frame.control.yOffset);
                        ByteArrayOutputStream frameOut = new ByteArrayOutputStream();
                        ImageIO.write(frameImage, "PNG", frameOut);
                        ByteArrayInputStream frameIn = new ByteArrayInputStream(frameOut.toByteArray());
                        boolean skip = !includeFirstFrame && (index == 0);
                        if (!skip) frameDelivery.accept(new ApngFrame(index, frameIn, frame.control.getDelayMilliseconds(), frameOut));
                        index++;
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to decode frame " + frameCount + " of APNG image: " + apngName, ex);
                    }
                    frameCount++;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode APNG image: " + apngName, ex);
        }
        frameDelivery.accept(null);
    }

    @NotNull
    protected static BufferedImage getBufferedImageFromBitmap(@NotNull Argb8888Bitmap bitmap, int imageWidth, int imageHeight, int frameXOffset, int frameYOffset) {
        int[] framePixels = bitmap.getPixelArray();
        BufferedImage frameImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        int frameWidth = bitmap.getWidth();
        int frameHeight = bitmap.getHeight();
        int frameXOff = Math.max(0, frameXOffset);
        int frameYOff = Math.max(0, frameYOffset);
        frameImage.setRGB(frameXOff, frameYOff, frameWidth, frameHeight, framePixels, 0, frameWidth);
        return frameImage;
    }

    public static class ApngFrame {

        protected final int index;
        protected final ByteArrayInputStream frameInputStream;
        protected final int delayMs;
        protected final ByteArrayOutputStream closeAfterLoading;
        protected DynamicTexture dynamicTexture;
        protected volatile NativeImage nativeImage;
        protected Identifier resourceLocation;
        protected boolean loaded = false;

        protected ApngFrame(int index, ByteArrayInputStream frameInputStream, int delayMs, ByteArrayOutputStream closeAfterLoading) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delayMs = delayMs;
            this.closeAfterLoading = closeAfterLoading;
        }

    }

    public record DecodedApngImage(@NotNull Argb8888BitmapSequence sequence, int imageWidth, int imageHeight, int numPlays) {
    }

}
