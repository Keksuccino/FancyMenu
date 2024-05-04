package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.ThreadUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.MinecraftResourceUtils;
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class FmaTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected volatile List<FmaFrame> frames = new ArrayList<>();
    @NotNull
    protected volatile List<FmaFrame> introFrames = new ArrayList<>();
    @Nullable
    protected volatile FmaTexture.FmaFrame current = null;
    protected volatile boolean introFinishedPlaying = false;
    protected volatile boolean skipToFirstNormalAfterIntro = false;
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile int width = 10;
    protected volatile int height = 10;
    protected volatile long lastResourceLocationCall = -1;
    protected final AtomicBoolean tickerThreadRunning = new AtomicBoolean(false);
    protected final AtomicBoolean decoded = new AtomicBoolean(false);
    protected volatile boolean allFramesDecoded = false;
    protected volatile boolean allIntroFramesDecoded = false;
    protected final AtomicInteger cycles = new AtomicInteger(0);
    /** How many times the FMA should loop. Value <= 0 means infinite loops. **/
    protected final AtomicInteger numPlays = new AtomicInteger(0);
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected final AtomicBoolean loadingCompleted = new AtomicBoolean(false);
    protected final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();
    protected int frameRegistrationCounter = 0;
    protected volatile boolean maxLoopsReached = false;
    protected final AtomicBoolean closed = new AtomicBoolean(false);

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
            of(MinecraftResourceUtils.open(location), location.toString(), texture);
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

        //Decode FMA image
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

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(fmaUrl))) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read FMA image from URL! Invalid URL: " + fmaUrl);
            return texture;
        }

        //Download and decode FMA image
        new Thread(() -> {
            InputStream in = null;
            ByteArrayInputStream byteIn = null;
            try {
                in = WebUtils.openResourceStream(fmaUrl);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                //The extract method seems to struggle with a direct web input stream, so read all bytes of it and wrap them into a ByteArrayInputStream
                byteIn = new ByteArrayInputStream(in.readAllBytes());
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read FMA image from URL: " + fmaUrl, ex);
            }
            if (byteIn != null) {
                of(byteIn, fmaUrl, texture);
            }
            //"byteIn" gets closed in of(), so only close "in" here
            CloseableUtils.closeQuietly(in);
        }).start();

        return texture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in, @Nullable String gifTextureName, @Nullable FmaTexture writeTo) {

        Objects.requireNonNull(in);

        FmaTexture texture = (writeTo != null) ? writeTo : new FmaTexture();

        //Decode FMA image
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
    public static FmaTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected static void populateTexture(@NotNull FmaTexture texture, @NotNull InputStream in, @NotNull String fmaTextureName) {
        DecodedFmaImage decodedImage = null;
        if (!texture.closed.get()) {
            decodedImage = decodeFma(in, fmaTextureName);
            if (decodedImage == null) {
                LOGGER.error("[FANCYMENU] Failed to read FMA image, because DecodedFmaImage was NULL: " + fmaTextureName);
                texture.decoded.set(true);
                texture.loadingFailed.set(true);
                return;
            }
            texture.width = decodedImage.imageWidth;
            texture.height = decodedImage.imageHeight;
            texture.aspectRatio = new AspectRatio(decodedImage.imageWidth, decodedImage.imageHeight);
            texture.numPlays.set(decodedImage.numPlays);
            texture.decoded.set(true);
            try {
                if (decodedImage.decoder().hasIntroFrames()) {
                    deliverFmaIntroFrames(decodedImage.decoder(), fmaTextureName, frame -> {
                        if (frame != null) {
                            try {
                                frame.nativeImage = NativeImage.read(frame.frameInputStream);
                            } catch (Exception ex) {
                                LOGGER.error("[FANCYMENU] Failed to read intro frame of FMA image into NativeImage: " + fmaTextureName, ex);
                            }
                            CloseableUtils.closeQuietly(frame.frameInputStream);
                            texture.introFrames.add(frame);
                        }
                    });
                }
                texture.allIntroFramesDecoded = true;
                deliverFmaFrames(decodedImage.decoder(), fmaTextureName, frame -> {
                    if (frame != null) {
                        try {
                            frame.nativeImage = NativeImage.read(frame.frameInputStream);
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] Failed to read frame of FMA image into NativeImage: " + fmaTextureName, ex);
                        }
                        CloseableUtils.closeQuietly(frame.frameInputStream);
                        texture.frames.add(frame);
                    }
                });
                texture.loadingCompleted.set(true);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read frames of FMA image: " + fmaTextureName, ex);
            }
            texture.allFramesDecoded = true;
            texture.allIntroFramesDecoded = true;
        }
        texture.decoded.set(true);
        CloseableUtils.closeQuietly(in);
        if (decodedImage != null) CloseableUtils.closeQuietly(decodedImage.decoder());
    }

    protected FmaTexture() {
    }

    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning.get() && (!this.frames.isEmpty() || !this.introFrames.isEmpty()) && !this.maxLoopsReached && !this.closed.get()) {

            this.tickerThreadRunning.set(true);
            this.lastResourceLocationCall = System.currentTimeMillis();

            new Thread(() -> {

                //Automatically stop thread if FMA was inactive for >=10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
                    if ((this.frames.isEmpty() && this.introFrames.isEmpty()) || this.closed.get()) break;
                    //Don't tick frame if max loops reached
                    if (this.maxLoopsReached) break;
                    boolean sleep = false;
                    try {
                        boolean cachedAllDecoded = this.allFramesDecoded;
                        boolean cachedAllIntroDecoded = this.allIntroFramesDecoded;
                        boolean cachedIntroFinished = this.introFinishedPlaying;
                        boolean cachedSkipToFirstAfterIntro = this.skipToFirstNormalAfterIntro;
                        //Cache frames to avoid possible concurrent modification exceptions
                        List<FmaFrame> cachedFrames = new ArrayList<>(this.frames);
                        List<FmaFrame> cachedIntroFrames = new ArrayList<>(this.introFrames);
                        if (!cachedFrames.isEmpty() || !cachedIntroFrames.isEmpty()) {
                            //Set initial (first) frame if current is NULL
                            if (this.current == null) {
                                FmaFrame first = !cachedIntroFrames.isEmpty() ? cachedIntroFrames.get(0) : cachedFrames.get(0);
                                this.current = first;
                                Thread.sleep(Math.max(10, first.delayMs));
                            } else if (cachedSkipToFirstAfterIntro) {
                                if (cachedFrames.isEmpty()) { //wait for first normal frame to be ready
                                    ThreadUtils.sleep(100);
                                    continue;
                                }
                                this.skipToFirstNormalAfterIntro = false;
                                FmaFrame firstNormal = cachedFrames.get(0);
                                this.current = firstNormal;
                                Thread.sleep(Math.max(10, firstNormal.delayMs));
                            }
                            //Cache current frame to make sure it stays the same instance while working with it
                            FmaFrame cachedCurrent = this.current;
                            if (cachedCurrent != null) {
                                FmaFrame newCurrent = null;
                                int currentIndexIncrement = cachedCurrent.index + 1;
                                //Check if there's a frame after the current one and if so, go to the next frame
                                boolean pickNextIntroFrame = (currentIndexIncrement < cachedIntroFrames.size()) && !cachedIntroFinished;
                                if (!pickNextIntroFrame && !cachedIntroFinished && !cachedSkipToFirstAfterIntro && cachedAllIntroDecoded) {
                                    this.introFinishedPlaying = true; //intro finished playing now, so set this to true
                                    this.skipToFirstNormalAfterIntro = true;
                                    continue;
                                }
                                if (pickNextIntroFrame || (currentIndexIncrement < cachedFrames.size())) {
                                    newCurrent = pickNextIntroFrame ? cachedIntroFrames.get(currentIndexIncrement) : cachedFrames.get(currentIndexIncrement);
                                } else if (cachedAllDecoded) {
                                    int cachedNumPlays = this.numPlays.get();
                                    //Count cycles up if FMA should not loop infinitely (numPlays > 0 = finite loops)
                                    if (cachedNumPlays > 0) {
                                        int newCycles = this.cycles.incrementAndGet();
                                        if (newCycles >= cachedNumPlays) {
                                            this.maxLoopsReached = true;
                                            break; //end the while loop of the frame ticker
                                        } else {
                                            //If FMA has a finite number of loops but did not reach its max loops yet, reset to first frame, because end reached
                                            newCurrent = cachedFrames.get(0);
                                        }
                                    } else {
                                        //If FMA loops infinitely, reset to first frame, because end reached
                                        newCurrent = cachedFrames.get(0);
                                    }
                                }
                                if (newCurrent != null) this.current = newCurrent;
                                //Sleep for the new current frame's delay or sleep for 100ms if there's no new frame
                                Thread.sleep(Math.max(10, (newCurrent != null) ? newCurrent.delayMs : 100));
                            } else {
                                sleep = true;
                            }
                        } else {
                            sleep = true;
                        }
                    } catch (Exception ex) {
                        sleep = true;
                        LOGGER.error("[FANCYMENU] An error happened in the frame ticker thread of an FMA texture!", ex);
                    }
                    if (sleep) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] An error happened in the frame ticker thread of an FMA texture!", ex);
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
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        FmaFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    this.frameRegistrationCounter++;
                    frame.dynamicTexture = new DynamicTexture(frame.nativeImage);
                    frame.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_fma_frame_" + this.uniqueId + "_" + this.frameRegistrationCounter, frame.dynamicTexture);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to register FMA frame to Minecraft's TextureManager!", ex);
                }
                frame.loaded = true;
            }
            return (frame.resourceLocation != null) ? frame.resourceLocation : FULLY_TRANSPARENT_TEXTURE;
        }
        return null;
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
        if (this.sourceLocation != null) return MinecraftResourceUtils.open(this.sourceLocation);
        return null;
    }

    @Override
    public boolean isReady() {
        //Everything important (like size) is set at this point, so it is considered ready
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
        this.introFinishedPlaying = false;
        this.skipToFirstNormalAfterIntro = false;
        this.current = null;
        List<FmaFrame> normalFrames = new ArrayList<>(this.frames);
        List<FmaFrame> introFrames = new ArrayList<>(this.introFrames);
        if (!introFrames.isEmpty()) {
            this.current = introFrames.get(0);
        } else if (!normalFrames.isEmpty()) {
            this.current = normalFrames.get(0);
        }
        this.cycles.set(0);
    }

    @Override
    public void play() {
    }

    @Override
    public boolean isPlaying() {
        return !this.maxLoopsReached;
    }

    @Override
    public void pause() {
    }

    @Override
    public boolean isPaused() {
        return false;
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
        this.sourceLocation = null;
        for (FmaFrame frame : new ArrayList<>(this.frames)) {
            try {
                if (frame.dynamicTexture != null) frame.dynamicTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close DynamicTexture of FMA frame!", ex);
            }
            try {
                if (frame.nativeImage != null) frame.nativeImage.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close NativeImage of FMA frame!", ex);
            }
            frame.dynamicTexture = null;
            frame.nativeImage = null;
        }
        for (FmaFrame frame : new ArrayList<>(this.introFrames)) {
            try {
                if (frame.dynamicTexture != null) frame.dynamicTexture.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close DynamicTexture of FMA intro frame!", ex);
            }
            try {
                if (frame.nativeImage != null) frame.nativeImage.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close NativeImage of FMA intro frame!", ex);
            }
            frame.dynamicTexture = null;
            frame.nativeImage = null;
        }
        this.frames = new ArrayList<>();
        this.introFrames = new ArrayList<>();
        this.current = null;
    }

    @Nullable
    public static FmaTexture.DecodedFmaImage decodeFma(@NotNull InputStream in, @NotNull String fmaName) {
        try {
            FmaDecoder decoder = new FmaDecoder();
            decoder.read(in);
            BufferedImage firstFrame = Objects.requireNonNull(decoder.getFirstFrameAsBufferedImage(), "Failed to get first frame of FMA image!");
            return new DecodedFmaImage(decoder, firstFrame.getWidth(), firstFrame.getHeight(), Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!").getLoopCount()); //loopCount == 0 == infinite loops | loopCount > 0 == number of loops
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode FMA image: " + fmaName, ex);
        }
        return null;
    }

    public static void deliverFmaFrames(@NotNull FmaDecoder decoder, @NotNull String fmaName, @NotNull Consumer<FmaFrame> frameDelivery) {
        int gifFrameCount = decoder.getFrameCount();
        int i = 0;
        int index = 0;
        while (i < gifFrameCount) {
            try {
                long delay = Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!").getFrameTimeForFrame(i, false);
                InputStream image = Objects.requireNonNull(decoder.getFrame(i), "FmaDecoder returned NULL for frame!");
                frameDelivery.accept(new FmaFrame(index, image, delay));
                index++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get frame '" + i + "' of FMA image '" + fmaName + "!", ex);
            }
            i++;
        }
    }

    public static void deliverFmaIntroFrames(@NotNull FmaDecoder decoder, @NotNull String fmaName, @NotNull Consumer<FmaFrame> frameDelivery) {
        if (!decoder.hasIntroFrames()) return;
        int gifFrameCount = decoder.getIntroFrameCount();
        int i = 0;
        int index = 0;
        while (i < gifFrameCount) {
            try {
                long delay = Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!").getFrameTimeForFrame(i, true);
                InputStream image = Objects.requireNonNull(decoder.getIntroFrame(i), "FmaDecoder returned NULL for intro frame!");
                frameDelivery.accept(new FmaFrame(index, image, delay));
                index++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get intro frame '" + i + "' of FMA image '" + fmaName + "!", ex);
            }
            i++;
        }
    }

    public static class FmaFrame {

        protected final int index;
        protected final InputStream frameInputStream;
        protected final long delayMs;
        protected DynamicTexture dynamicTexture;
        protected volatile NativeImage nativeImage;
        protected ResourceLocation resourceLocation;
        protected boolean loaded = false;

        protected FmaFrame(int index, InputStream frameInputStream, long delayMs) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delayMs = delayMs;
        }

    }

    public record DecodedFmaImage(@NotNull FmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}
