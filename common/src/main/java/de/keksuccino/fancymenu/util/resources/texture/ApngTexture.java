package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import net.ellerton.japng.Png;
import net.ellerton.japng.argb8888.Argb8888Bitmap;
import net.ellerton.japng.argb8888.Argb8888BitmapSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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
import java.util.Optional;

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
    protected volatile int cycles = 0;
    protected volatile int numPlays = -1;
    protected volatile ResourceLocation sourceLocation;
    protected volatile boolean closed = false;

    @NotNull
    public static ApngTexture location(@NotNull ResourceLocation location) {

        Objects.requireNonNull(location);
        ApngTexture texture = new ApngTexture();

        texture.sourceLocation = location;
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                InputStream in = resource.get().open();
                of(in, location.toString(), texture);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load APNG image by ResourceLocation: " + location, ex);
        }

        return texture;

    }

    @NotNull
    public static ApngTexture web(@NotNull String apngUrl) {

        ApngTexture apngTexture = new ApngTexture();

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(apngUrl))) {
            LOGGER.error("[FANCYMENU] Unable to load Web APNG image! Invalid URL: " + apngUrl);
            return apngTexture;
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
                LOGGER.error("[FANCYMENU] Failed to download Web APNG image: " + apngUrl, ex);
            }
            if (byteIn != null) {
                of(byteIn, apngUrl, apngTexture);
            }
            //"byteIn" gets closed in of(), so only close "in" here
            CloseableUtils.closeQuietly(in);
        }).start();

        return apngTexture;

    }

    @NotNull
    public static ApngTexture local(@NotNull File apng) {

        ApngTexture apngTexture = new ApngTexture();

        if (!apng.isFile()) {
            LOGGER.error("[FANCYMENU] APNG image not found: " + apng.getPath());
            return apngTexture;
        }

        //Decode APNG image
        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(apng);
                of(in, apng.getPath(), apngTexture);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load APNG image: " + apng.getPath(), ex);
            }
        }).start();

        return apngTexture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in, @Nullable String apngTextureName, @Nullable ApngTexture writeTo) {

        Objects.requireNonNull(in);

        ApngTexture apngTexture = (writeTo != null) ? writeTo : new ApngTexture();

        //Decode APNG image
        new Thread(() -> {
            populateTexture(apngTexture, in, (apngTextureName != null) ? apngTextureName : "[Generic InputStream Source]");
        }).start();

        return apngTexture;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static ApngTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected static void populateTexture(@NotNull ApngTexture apngTexture, @NotNull InputStream in, @NotNull String apngTextureName) {
        //Decode first frame and set it as temporary frame list to show APNG quicker
        DecodedApngImage imageAndFirstFrame = readApng(in, apngTextureName, true);
        if (imageAndFirstFrame == null) {
            LOGGER.error("[FANCYMENU] Failed to read APNG image, because DecodedApngImage was NULL: " + apngTextureName);
            apngTexture.decoded = true;
            return;
        }
        apngTexture.width = imageAndFirstFrame.imageWidth;
        apngTexture.height = imageAndFirstFrame.imageHeight;
        apngTexture.aspectRatio = new AspectRatio(imageAndFirstFrame.imageWidth, imageAndFirstFrame.imageHeight);
        if (!imageAndFirstFrame.frames().isEmpty()) {
            ApngFrame first = imageAndFirstFrame.frames().get(0);
            try {
                first.nativeImage = NativeImage.read(first.frameInputStream);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read preview frame of APNG image into NativeImage: " + apngTextureName, ex);
            }
            CloseableUtils.closeQuietly(first.closeAfterLoading);
            CloseableUtils.closeQuietly(first.frameInputStream);
            apngTexture.frames = imageAndFirstFrame.frames();
            apngTexture.decoded = true;
        }
        //Decode the full APNG and set its frames to the ApngTexture
        DecodedApngImage allFrames = null;
        try {
            allFrames = readApng(imageAndFirstFrame.sequence(), apngTextureName, false);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read APNG image: " + apngTextureName, ex);
        }
        if (allFrames != null) {
            for (ApngFrame frame : allFrames.frames()) {
                try {
                    frame.nativeImage = NativeImage.read(frame.frameInputStream);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to read frame of APNG image into NativeImage: " + apngTextureName, ex);
                }
                CloseableUtils.closeQuietly(frame.closeAfterLoading);
                CloseableUtils.closeQuietly(frame.frameInputStream);
            }
            apngTexture.frames = allFrames.frames();
        }
        apngTexture.numPlays = imageAndFirstFrame.numPlays;
        apngTexture.decoded = true;
        CloseableUtils.closeQuietly(in);
    }

    protected ApngTexture() {
    }

    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning && !this.frames.isEmpty() && !this.closed) {

            this.tickerThreadRunning = true;
            this.lastResourceLocationCall = System.currentTimeMillis();

            new Thread(() -> {

                //Automatically stop thread if APNG was inactive for 10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
                    if (this.frames.isEmpty() || this.closed) break;
                    //Don't tick frame if max loops reached
                    if ((this.numPlays >= 0) && (this.cycles >= this.numPlays)) return;
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
                                ApngFrame newCurrent;
                                if ((cachedCurrent.index + 1) < cachedFrames.size()) {
                                    newCurrent = cachedFrames.get(cachedCurrent.index + 1);
                                } else {
                                    newCurrent = cachedFrames.get(0);
                                    //Count cycles up if APNG should not loop infinitely (numPlays == -1 == infinite loops)
                                    if (this.numPlays >= 0) this.cycles++;
                                }
                                this.current = newCurrent;
                                Thread.sleep(Math.max(20, newCurrent.delayMs));
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
            }
            frame.loaded = true;
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
    public boolean isReady() {
        return this.decoded;
    }

    public void reset() {
        this.current = null;
        if (!this.frames.isEmpty()) {
            this.current = this.frames.get(0);
            this.cycles = 0;
        }
    }

    @Override
    public void play() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void stop() {
        this.reset();
    }

    @Override
    public boolean isPlaying() {
        return true;
    }

    /**
     * Only reloads textures loaded via ResourceLocation.
     */
    @Override
    public void reload() {
        if (this.sourceLocation != null) {
            for (ApngFrame frame : this.frames) {
                //Closes NativeImage and DynamicTexture
                CloseableUtils.closeQuietly(frame.dynamicTexture);
                frame.dynamicTexture = null;
                frame.nativeImage = null;
            }
            this.decoded = false;
            this.frames.clear();
            this.current = null;
            this.cycles = 0;
            this.lastResourceLocationCall = -1;
            try {
                Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(this.sourceLocation);
                if (resource.isPresent()) {
                    InputStream in = resource.get().open();
                    of(in, this.sourceLocation.toString(), this);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to reload ResourceLocation APNG image: " + this.sourceLocation, ex);
            }
        }
    }

    @Override
    public void close() {
        this.closed = true;
        this.sourceLocation = null;
        for (ApngFrame frame : this.frames) {
            //Closes NativeImage and DynamicTexture
            CloseableUtils.closeQuietly(frame.dynamicTexture);
            frame.dynamicTexture = null;
            frame.nativeImage = null;
        }
        this.frames.clear();
        this.current = null;
    }

    @Nullable
    public static DecodedApngImage readApng(@NotNull InputStream in, @NotNull String apngName, boolean onlyFirst) {
        try {
            return readApng(Png.readArgb8888BitmapSequence(in), apngName, onlyFirst);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @NotNull
    public static ApngTexture.DecodedApngImage readApng(@NotNull Argb8888BitmapSequence sequence, @NotNull String apngName, boolean onlyFirst) {
        List<ApngFrame> frames = new ArrayList<>();
        int numPlays = -1;
        try {
            numPlays = sequence.getAnimationControl().loopForever() ? -1 : sequence.getAnimationControl().numPlays;
            if (sequence.isAnimated()) {
                if (sequence.hasDefaultImage() && onlyFirst) {
                    try {
                        BufferedImage frameImage = getBufferedImageFromBitmap(sequence.defaultImage, sequence.header.width, sequence.header.height, 0, 0);
                        ByteArrayOutputStream frameOut = new ByteArrayOutputStream();
                        ImageIO.write(frameImage, "PNG", frameOut);
                        ByteArrayInputStream frameIn = new ByteArrayInputStream(frameOut.toByteArray());
                        frames.add(new ApngFrame(0, frameIn, 0, frameOut));
                        return new DecodedApngImage(sequence, frames, sequence.header.width, sequence.header.height, numPlays);
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to decode default frame of APNG image: " + apngName, ex);
                    }
                }
                int index = frames.isEmpty() ? 0 : 1;
                int frameCount = 0;
                for (Argb8888BitmapSequence.Frame frame : sequence.getAnimationFrames()) {
                    try {
                        BufferedImage frameImage = getBufferedImageFromBitmap(frame.bitmap, sequence.header.width, sequence.header.height, frame.control.xOffset, frame.control.yOffset);
                        ByteArrayOutputStream frameOut = new ByteArrayOutputStream();
                        ImageIO.write(frameImage, "PNG", frameOut);
                        ByteArrayInputStream frameIn = new ByteArrayInputStream(frameOut.toByteArray());
                        frames.add(new ApngFrame(index, frameIn, frame.control.getDelayMilliseconds(), frameOut));
                        index++;
                        if (onlyFirst) return new DecodedApngImage(sequence, frames, sequence.header.width, sequence.header.height, numPlays);
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to decode frame " + frameCount + " of APNG image: " + apngName, ex);
                    }
                    frameCount++;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new DecodedApngImage(sequence, frames, sequence.header.width, sequence.header.height, numPlays);
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

    protected static class ApngFrame {

        protected final int index;
        protected final ByteArrayInputStream frameInputStream;
        protected  final int delayMs;
        protected final ByteArrayOutputStream closeAfterLoading;
        protected DynamicTexture dynamicTexture;
        protected NativeImage nativeImage;
        protected ResourceLocation resourceLocation;
        protected boolean loaded = false;

        protected ApngFrame(int index, ByteArrayInputStream frameInputStream, int delayMs, ByteArrayOutputStream closeAfterLoading) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delayMs = delayMs;
            this.closeAfterLoading = closeAfterLoading;
        }

    }

    protected record DecodedApngImage(@NotNull Argb8888BitmapSequence sequence, @NotNull List<ApngFrame> frames, int imageWidth, int imageHeight, int numPlays) {
    }

}
