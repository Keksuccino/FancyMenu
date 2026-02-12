package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.madgag.gif.fmsware.GifDecoder;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
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

public class GifTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected volatile List<GifFrame> frames = new ArrayList<>();
    @Nullable
    protected volatile GifTexture.GifFrame current = null;
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile int width = 10;
    protected volatile int height = 10;
    protected volatile long lastResourceLocationCall = -1;
    protected final AtomicBoolean tickerThreadRunning = new AtomicBoolean(false);
    protected final AtomicBoolean decoded = new AtomicBoolean(false);
    protected volatile boolean allFramesDecoded = false;
    protected final AtomicInteger cycles = new AtomicInteger(0);
    /** How many times the GIF should loop. Value <= 0 means infinite loops. **/
    protected final AtomicInteger numPlays = new AtomicInteger(0);
    protected ResourceLocation sourceLocation;
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

    @NotNull
    public static GifTexture location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static GifTexture location(@NotNull ResourceLocation location, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(location);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read GIF image from ResourceLocation: " + location, ex);
        }

        return texture;

    }

    @NotNull
    public static GifTexture local(@NotNull File apngFile) {
        return local(apngFile, null);
    }

    @NotNull
    public static GifTexture local(@NotNull File gifFile, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(gifFile);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceFile = gifFile;

        if (!gifFile.isFile()) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read GIF image from file! File not found: " + gifFile.getPath());
            return texture;
        }

        //Decode GIF image
        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(gifFile);
                of(in, gifFile.getPath(), texture);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read GIF image from file: " + gifFile.getPath(), ex);
            }
        }).start();

        return texture;

    }

    @NotNull
    public static GifTexture web(@NotNull String apngUrl) {
        return web(apngUrl, null);
    }

    @NotNull
    public static GifTexture web(@NotNull String gifUrl, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(gifUrl);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceURL = gifUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(gifUrl))) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read GIF image from URL! Invalid URL: " + gifUrl);
            return texture;
        }

        //Download and decode GIF image
        new Thread(() -> {
            try {
                populateTexture(texture, null, gifUrl);
                if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                texture.decoded.set(true);
                LOGGER.error("[FANCYMENU] Failed to read GIF image from URL: " + gifUrl, ex);
            }
        }).start();

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static GifTexture of(@NotNull InputStream in, @Nullable String gifTextureName, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(in);

        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        //Decode GIF image
        new Thread(() -> {
            populateTexture(texture, in, (gifTextureName != null) ? gifTextureName : "[Generic InputStream Source]");
            if (texture.closed.get()) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }).start();

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static GifTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected static void populateTexture(@NotNull GifTexture texture, @Nullable InputStream in, @NotNull String gifTextureName) {
        InputStream readInput = in;
        if (!texture.closed.get()) {
            boolean decodedByWatermedia = false;
            if (WatermediaUtil.isWatermediaLoaded()) {
                LOGGER.info("[FANCYMENU] Starting GIF loading via Watermedia (direct source preferred): {}", gifTextureName);
                decodedByWatermedia = populateTextureWithWatermediaDirectSource(texture, gifTextureName);
            }

            byte[] gifData = null;
            if (!decodedByWatermedia) {
                if (readInput == null) {
                    try {
                        readInput = texture.open();
                    } catch (Exception ex) {
                        texture.loadingFailed.set(true);
                        texture.decoded.set(true);
                        LOGGER.error("[FANCYMENU] Failed to open GIF image data stream: " + gifTextureName, ex);
                        CloseableUtils.closeQuietly(readInput);
                        return;
                    }
                }
                if (readInput == null) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[FANCYMENU] Failed to open GIF image data stream: {}", gifTextureName);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                try {
                    gifData = readInput.readAllBytes();
                } catch (Exception ex) {
                    texture.loadingFailed.set(true);
                    texture.decoded.set(true);
                    LOGGER.error("[FANCYMENU] Failed to read GIF image data: " + gifTextureName, ex);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
            }

            if (!decodedByWatermedia && WatermediaUtil.isWatermediaLoaded()) {
                decodedByWatermedia = populateTextureWithWatermedia(texture, gifData, gifTextureName);
                if (decodedByWatermedia) {
                    WatermediaUtil.WATERMEDIA_initialized = true;
                } else {
                    LOGGER.warn("[FANCYMENU] Watermedia GIF decoding failed, falling back to primitive decoder: {}", gifTextureName);
                }
            }

            if (!decodedByWatermedia) {
                if (gifData == null) {
                    texture.loadingFailed.set(true);
                    LOGGER.error("[FANCYMENU] Failed to read GIF image data: {}", gifTextureName);
                    texture.decoded.set(true);
                    CloseableUtils.closeQuietly(readInput);
                    return;
                }
                populateTextureWithPrimitiveDecoder(texture, gifData, gifTextureName);
            }
        }
        texture.decoded.set(true);
        CloseableUtils.closeQuietly(readInput);
    }

    protected static boolean populateTextureWithWatermediaDirectSource(@NotNull GifTexture texture, @NotNull String gifTextureName) {
        String directSource = null;
        if (texture.sourceURL != null) {
            directSource = texture.sourceURL;
        } else if ((texture.sourceFile != null) && texture.sourceFile.isFile()) {
            directSource = texture.sourceFile.getAbsolutePath();
        }
        if (directSource == null) return false;

        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "gif");
        backend.setLoopCount(-1);
        boolean initialized = backend.initializeFromSource(directSource, gifTextureName);
        if (!initialized) {
            backend.close();
            return false;
        }
        texture.watermediaBackend = backend;
        texture.decoded.set(true);
        return true;
    }

    protected static void populateTextureWithPrimitiveDecoder(@NotNull GifTexture texture, @NotNull byte[] gifData, @NotNull String gifTextureName) {
        DecodedGifImage decodedImage = decodeGif(new ByteArrayInputStream(gifData), gifTextureName);
        if (decodedImage == null) {
            LOGGER.error("[FANCYMENU] Failed to read GIF image, because DecodedGifImage was NULL: " + gifTextureName);
            texture.loadingFailed.set(true);
            return;
        }
        texture.width = decodedImage.imageWidth;
        texture.height = decodedImage.imageHeight;
        texture.aspectRatio = new AspectRatio(decodedImage.imageWidth, decodedImage.imageHeight);
        texture.numPlays.set(decodedImage.numPlays);
        texture.decoded.set(true);
        try {
            deliverGifFrames(decodedImage.decoder(), gifTextureName, frame -> {
                if (frame != null) {
                    try {
                        if ((frame.nativeImage == null) && (frame.frameInputStream != null)) {
                            frame.nativeImage = NativeImage.read(frame.frameInputStream);
                        }
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to read frame of GIF image into NativeImage: " + gifTextureName, ex);
                    }
                    CloseableUtils.closeQuietly(frame.closeAfterLoading);
                    CloseableUtils.closeQuietly(frame.frameInputStream);
                    texture.frames.add(frame);
                }
            });
            texture.loadingCompleted.set(true);
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read frames of GIF image: " + gifTextureName, ex);
        }
        texture.allFramesDecoded = true;
    }

    protected static boolean populateTextureWithWatermedia(@NotNull GifTexture texture, @NotNull byte[] gifData, @NotNull String gifTextureName) {
        WatermediaAnimatedTextureBackend backend = new WatermediaAnimatedTextureBackend(texture.uniqueId, "gif");
        backend.setLoopCount(readGifLoopCount(gifData));
        boolean initialized = backend.initializeFromBytes(gifData, ".gif", gifTextureName);
        if (!initialized) {
            backend.close();
            return false;
        }
        texture.watermediaBackend = backend;
        texture.decoded.set(true);
        return true;
    }

    protected static int readGifLoopCount(@NotNull byte[] gifData) {
        // Defaults to one playback if no loop extension is present.
        if (gifData.length < 13) return 1;
        int index = 6;
        int packed = gifData[index + 4] & 255;
        index += 7;
        if ((packed & 0x80) != 0) {
            int gctSize = 3 * (1 << ((packed & 0x07) + 1));
            index += gctSize;
        }
        while (index < gifData.length) {
            int block = gifData[index++] & 255;
            if (block == 0x3B) break; // trailer
            if (block == 0x21) { // extension
                if (index >= gifData.length) break;
                int label = gifData[index++] & 255;
                String appIdentifier = null;
                if ((label == 0xFF) && (index < gifData.length)) {
                    int appBlockSize = gifData[index++] & 255;
                    if ((appBlockSize > 0) && ((index + appBlockSize) <= gifData.length)) {
                        appIdentifier = new String(gifData, index, appBlockSize);
                        index += appBlockSize;
                    } else {
                        return 1;
                    }
                }
                while (index < gifData.length) {
                    int size = gifData[index++] & 255;
                    if (size == 0) break;
                    if ((index + size) > gifData.length) return 1;
                    if ((appIdentifier != null)
                            && (size >= 3)
                            && (("NETSCAPE2.0".equals(appIdentifier)) || ("ANIMEXTS1.0".equals(appIdentifier)))
                            && ((gifData[index] & 255) == 1)) {
                        int loops = (gifData[index + 1] & 255) | ((gifData[index + 2] & 255) << 8);
                        return (loops == 0) ? -1 : Math.max(1, loops);
                    }
                    index += size;
                    if (index > gifData.length) return 1;
                }
            } else if (block == 0x2C) { // image descriptor
                if ((index + 9) > gifData.length) break;
                int localPacked = gifData[index + 8] & 255;
                index += 9;
                if ((localPacked & 0x80) != 0) {
                    int lctSize = 3 * (1 << ((localPacked & 0x07) + 1));
                    index += lctSize;
                }
                if (index >= gifData.length) break;
                index++; // LZW min code size
                while (index < gifData.length) {
                    int size = gifData[index++] & 255;
                    if (size == 0) break;
                    index += size;
                    if (index > gifData.length) return 1;
                }
            } else {
                break;
            }
        }
        return 1;
    }

    protected GifTexture() {
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
                        List<GifFrame> cachedFrames = new ArrayList<>(this.frames);
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
                            GifFrame cachedCurrent = this.current;
                            if (cachedCurrent != null) {
                                GifFrame newCurrent = null;
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
                        LOGGER.error("[FANCYMENU] An error happened in the frame ticker thread on an GIF!", ex);
                    }
                    if (sleep) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] An error happened in the frame ticker thread on an GIF!", ex);
                        }
                    }
                }

                this.tickerThreadRunning.set(false);

            }).start();

        }
    }

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) {
            this.width = backend.getWidth();
            this.height = backend.getHeight();
            this.aspectRatio = backend.getAspectRatio();
            ResourceLocation resourceLocation = backend.getResourceLocation();
            return (resourceLocation != null) ? resourceLocation : FULLY_TRANSPARENT_TEXTURE;
        }
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        GifFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    this.frameRegistrationCounter++;
                    frame.dynamicTexture = new DynamicTexture(frame.nativeImage);
                    frame.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_gif_frame_" + this.uniqueId + "_" + this.frameRegistrationCounter, frame.dynamicTexture);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to register GIF frame to Minecraft's TextureManager!", ex);
                }
                frame.loaded = true;
            }
            return (frame.resourceLocation != null) ? frame.resourceLocation : FULLY_TRANSPARENT_TEXTURE;
        }
        return null;
    }

    @Override
    public int getWidth() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) return backend.getWidth();
        return this.width;
    }

    @Override
    public int getHeight() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) return backend.getHeight();
        return this.height;
    }

    @Override
    public @NotNull AspectRatio getAspectRatio() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
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
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) return backend.isReady();
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded.get();
    }

    @Override
    public boolean isLoadingCompleted() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) return backend.isLoadingCompleted();
        return !this.closed.get() && !this.loadingFailed.get() && this.loadingCompleted.get();
    }

    @Override
    public boolean isLoadingFailed() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) return backend.isLoadingFailed();
        return this.loadingFailed.get();
    }

    public void reset() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) {
            backend.reset();
            return;
        }
        this.current = null;
        this.pendingStartEvent = true;
        List<GifFrame> l = new ArrayList<>(this.frames);
        if (!l.isEmpty()) {
            this.current = l.get(0);
            this.cycles.set(0);
        }
    }

    private void maybeEmitStartEvent(@NotNull List<GifFrame> frames, @Nullable GifFrame currentFrame) {
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

    private String resolveTextureSourceType() {
        if (this.sourceURL != null) return "WEB";
        if (this.sourceFile != null) return "LOCAL";
        if (this.sourceLocation != null) return "RESOURCE_LOCATION";
        return "UNKNOWN";
    }

    @Override
    public void play() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) {
            backend.play();
        }
    }

    @Override
    public boolean isPlaying() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) return backend.isPlaying();
        return !this.maxLoopsReached;
    }

    @Override
    public void pause() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) {
            backend.pause();
        }
    }

    @Override
    public boolean isPaused() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
        if (backend != null) return backend.isPaused();
        return false;
    }

    @Override
    public void stop() {
        WatermediaAnimatedTextureBackend backend = this.watermediaBackend;
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
        this.closed.set(true);
        this.sourceLocation = null;
        for (GifFrame frame : new ArrayList<>(this.frames)) {
            try {
                if (frame.dynamicTexture != null) frame.dynamicTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close DynamicTexture of GIF frame!", ex);
            }
            try {
                if (frame.nativeImage != null) frame.nativeImage.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close NativeImage of GIF frame!", ex);
            }
            frame.dynamicTexture = null;
            frame.nativeImage = null;
        }
        this.frames = new ArrayList<>();
        this.current = null;
    }

    @Nullable
    public static DecodedGifImage decodeGif(@NotNull InputStream in, @NotNull String gifName) {
        try {
            GifDecoder decoder = new GifDecoder();
            decoder.read(in);
            BufferedImage firstFrame = decoder.getImage();
            return new DecodedGifImage(decoder, firstFrame.getWidth(), firstFrame.getHeight(), decoder.getLoopCount()); //loopCount == 0 == infinite loops | loopCount > 0 == number of loops
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode GIF image: " + gifName, ex);
        }
        return null;
    }

    public static void deliverGifFrames(@NotNull GifDecoder decoder, @NotNull String gifName, @NotNull Consumer<GifFrame> frameDelivery) {
        int gifFrameCount = decoder.getFrameCount();
        int i = 0;
        int index = 0;
        while (i < gifFrameCount) {
            try {
                double delay = decoder.getDelay(i);
                BufferedImage image = decoder.getFrame(i);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", os);
                ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
                frameDelivery.accept(new GifFrame(index, bis, (long)delay, os));
                index++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get frame '" + i + "' of GIF image '" + gifName + "!", ex);
            }
            i++;
        }
    }

    public static class GifFrame {

        protected final int index;
        protected final ByteArrayInputStream frameInputStream;
        protected final long delayMs;
        protected final ByteArrayOutputStream closeAfterLoading;
        protected DynamicTexture dynamicTexture;
        protected volatile NativeImage nativeImage;
        protected ResourceLocation resourceLocation;
        protected boolean loaded = false;

        protected GifFrame(int index, ByteArrayInputStream frameInputStream, long delayMs, ByteArrayOutputStream closeAfterLoading) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delayMs = delayMs;
            this.closeAfterLoading = closeAfterLoading;
        }

    }

    public record DecodedGifImage(@NotNull GifDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}
