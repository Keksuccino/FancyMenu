package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.rendering.RenderUtils;
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
import java.util.UUID;
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
    protected final AtomicBoolean closed = new AtomicBoolean(false);

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
            InputStream in = null;
            ByteArrayInputStream byteIn = null;
            try {
                in = WebUtils.openResourceStream(apngUrl);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                //The extract method seems to struggle with a direct web input stream, so read all bytes of it and wrap them into a ByteArrayInputStream
                byteIn = new ByteArrayInputStream(in.readAllBytes());
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read APNG image from URL: " + apngUrl, ex);
            }
            if (byteIn != null) {
                of(byteIn, apngUrl, texture);
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

    protected static void populateTexture(@NotNull ApngTexture texture, @NotNull InputStream in, @NotNull String apngTextureName) {
        if (!texture.closed.get()) {
            DecodedApngImage decodedImage = decodeApng(in, apngTextureName);
            if (decodedImage == null) {
                LOGGER.error("[FANCYMENU] Failed to read APNG image, because DecodedApngImage was NULL: " + apngTextureName);
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
                deliverApngFrames(decodedImage.sequence(), apngTextureName, true, frame -> {
                    if (frame != null) {
                        try {
                            frame.nativeImage = NativeImage.read(frame.frameInputStream);
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
        texture.decoded.set(true);
        CloseableUtils.closeQuietly(in);
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
                                Thread.sleep(Math.max(20, cachedFrames.get(0).delayMs));
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
        if (this.closed.get()) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        ApngFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    this.frameRegistrationCounter++;
                    frame.dynamicTexture = new DynamicTexture(() -> UUID.randomUUID().toString(), frame.nativeImage);
                    frame.resourceLocation = RenderUtils.register("fancymenu_apng_frame_" + this.uniqueId + "_" + this.frameRegistrationCounter, frame.dynamicTexture);
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
        this.maxLoopsReached = false;
        this.current = null;
        List<ApngFrame> frameList = new ArrayList<>(this.frames);
        if (!frameList.isEmpty()) {
            this.current = frameList.get(0);
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
