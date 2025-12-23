package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.madgag.gif.fmsware.GifDecoder;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.rendering.RenderUtils;
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
import java.util.UUID;
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
    protected Identifier sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected final AtomicBoolean loadingCompleted = new AtomicBoolean(false);
    protected final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    protected final String uniqueId = ScreenCustomization.generateUniqueIdentifier();
    protected int frameRegistrationCounter = 0;
    protected volatile boolean maxLoopsReached = false;
    protected final AtomicBoolean closed = new AtomicBoolean(false);

    @NotNull
    public static GifTexture location(@NotNull Identifier location) {
        return location(location, null);
    }

    @NotNull
    public static GifTexture location(@NotNull Identifier location, @Nullable GifTexture writeTo) {

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
            InputStream in = null;
            ByteArrayInputStream byteIn = null;
            try {
                in = WebUtils.openResourceStream(gifUrl);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                //The extract method seems to struggle with a direct web input stream, so read all bytes of it and wrap them into a ByteArrayInputStream
                byteIn = new ByteArrayInputStream(in.readAllBytes());
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read GIF image from URL: " + gifUrl, ex);
            }
            if (byteIn != null) {
                of(byteIn, gifUrl, texture);
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

    protected static void populateTexture(@NotNull GifTexture texture, @NotNull InputStream in, @NotNull String gifTextureName) {
        if (!texture.closed.get()) {
            DecodedGifImage decodedImage = decodeGif(in, gifTextureName);
            if (decodedImage == null) {
                LOGGER.error("[FANCYMENU] Failed to read GIF image, because DecodedGifImage was NULL: " + gifTextureName);
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
                deliverGifFrames(decodedImage.decoder(), gifTextureName, frame -> {
                    if (frame != null) {
                        try {
                            frame.nativeImage = NativeImage.read(frame.frameInputStream);
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
        texture.decoded.set(true);
        CloseableUtils.closeQuietly(in);
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
                                Thread.sleep(Math.max(20, cachedFrames.get(0).delayMs));
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
                                        if (newCycles >= cachedNumPlays) {
                                            this.maxLoopsReached = true;
                                            break; //end the while loop of the frame ticker
                                        } else {
                                            //If APNG has a finite number of loops but did not reach its max loops yet, reset to first frame, because end reached
                                            newCurrent = cachedFrames.get(0);
                                        }
                                    } else {
                                        //If APNG loops infinitely, reset to first frame, because end reached
                                        newCurrent = cachedFrames.get(0);
                                    }
                                }
                                if (newCurrent != null) this.current = newCurrent;
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
    public Identifier getResourceLocation() {
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        GifFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    this.frameRegistrationCounter++;
                    frame.dynamicTexture = new DynamicTexture(() -> UUID.randomUUID().toString(), frame.nativeImage);
                    frame.resourceLocation = RenderUtils.register("fancymenu_gif_frame_" + this.uniqueId + "_" + this.frameRegistrationCounter, frame.dynamicTexture);
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
        this.current = null;
        List<GifFrame> l = new ArrayList<>(this.frames);
        if (!l.isEmpty()) {
            this.current = l.get(0);
            this.cycles.set(0);
        }
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
        protected Identifier resourceLocation;
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
