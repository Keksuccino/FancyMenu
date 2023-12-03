package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.ellerton.japng.Png;
import net.ellerton.japng.argb8888.Argb8888Bitmap;
import net.ellerton.japng.argb8888.Argb8888BitmapSequence;
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
    protected volatile boolean tickerThreadRunning = false;
    protected volatile boolean decoded = false;
    protected volatile boolean allFramesDecoded = false;
    protected volatile int cycles = 0;
    /** How many times the APNG should loop. Value <= 0 means infinite loops. **/
    protected volatile int numPlays = 0;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean closed = false;

    @NotNull
    public static ApngTexture location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static ApngTexture location(@NotNull ResourceLocation location, @Nullable ApngTexture writeTo) {

        Objects.requireNonNull(location);
        ApngTexture texture = (writeTo != null) ? writeTo : new ApngTexture();

        texture.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), texture);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read APNG image from ResourceLocation: " + location, ex);
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
            LOGGER.error("[FANCYMENU] Failed to read APNG image from file! File not found: " + apngFile.getPath());
            return texture;
        }

        //Decode APNG image
        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(apngFile);
                of(in, apngFile.getPath(), texture);
            } catch (Exception ex) {
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
            if (texture.closed) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
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

    protected static void populateTexture(@NotNull ApngTexture apngTexture, @NotNull InputStream in, @NotNull String apngTextureName) {
        if (!apngTexture.closed) {
            DecodedApngImage decodedImage = decodeApng(in, apngTextureName);
            if (decodedImage == null) {
                LOGGER.error("[FANCYMENU] Failed to read APNG image, because DecodedApngImage was NULL: " + apngTextureName);
                apngTexture.decoded = true;
                return;
            }
            apngTexture.width = decodedImage.imageWidth;
            apngTexture.height = decodedImage.imageHeight;
            apngTexture.aspectRatio = new AspectRatio(decodedImage.imageWidth, decodedImage.imageHeight);
            apngTexture.numPlays = decodedImage.numPlays;
            apngTexture.decoded = true;
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
                        apngTexture.frames.add(frame);
                    }
                });
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read frames of APNG image: " + apngTextureName, ex);
            }
            apngTexture.allFramesDecoded = true;
        }
        apngTexture.decoded = true;
        CloseableUtils.closeQuietly(in);
    }

    protected ApngTexture() {
    }

    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning && !this.frames.isEmpty() && !this.maxLoopsReached() && !this.closed) {

            this.tickerThreadRunning = true;
            this.lastResourceLocationCall = System.currentTimeMillis();

            new Thread(() -> {

                //Automatically stop thread if APNG was inactive for 10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
                    if (this.frames.isEmpty() || this.closed) break;
                    //Don't tick frame if max loops reached
                    if (this.maxLoopsReached()) break;
                    boolean sleep = false;
                    try {
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
                                //Go to the next frame if current frame display time is over
                                ApngFrame newCurrent = null;
                                if ((cachedCurrent.index + 1) < cachedFrames.size()) {
                                    newCurrent = cachedFrames.get(cachedCurrent.index + 1);
                                } else {
                                    //Count cycles up if APNG should not loop infinitely
                                    if ((this.numPlays > 0) && this.allFramesDecoded) this.cycles++;
                                    if (!this.maxLoopsReached() && this.allFramesDecoded) newCurrent = cachedFrames.get(0);
                                }
                                if (newCurrent != null) this.current = newCurrent;
                                Thread.sleep(Math.max(20, (newCurrent != null) ? newCurrent.delayMs : 100));
                            } else {
                                sleep = true;
                            }
                        } else {
                            sleep = true;
                        }
                    } catch (Exception ex) {
                        sleep = true;
                        ex.printStackTrace();
                    }
                    if (sleep) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex2) {
                            ex2.printStackTrace();
                        }
                    }
                }

                this.tickerThreadRunning = false;

            }).start();

        }
    }

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (this.closed) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        ApngFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    frame.dynamicTexture = new DynamicTexture(frame.nativeImage);
                    frame.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_apng_texture_frame", frame.dynamicTexture);
                } catch (Exception ex) {
                    ex.printStackTrace();
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

    public boolean maxLoopsReached() {
        return (this.numPlays > 0) && (this.cycles >= this.numPlays);
    }

    @Override
    public boolean isReady() {
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded;
    }

    public void reset() {
        this.current = null;
        List<ApngFrame> l = new ArrayList<>(this.frames);
        if (!l.isEmpty()) {
            this.current = l.get(0);
            this.cycles = 0;
        }
    }

    @Override
    public void play() {
    }

    @Override
    public boolean isPlaying() {
        return !this.maxLoopsReached();
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
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        this.sourceLocation = null;
        for (ApngFrame frame : new ArrayList<>(this.frames)) {
            try {
                if (frame.dynamicTexture != null) frame.dynamicTexture.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                if (frame.nativeImage != null) frame.nativeImage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
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
            ex.printStackTrace();
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
                int index = !defaultDelivered ? 0 : 1;
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
        protected ResourceLocation resourceLocation;
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
