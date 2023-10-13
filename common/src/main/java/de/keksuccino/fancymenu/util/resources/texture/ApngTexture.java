package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import net.ellerton.japng.Png;
import net.ellerton.japng.argb8888.Argb8888Bitmap;
import net.ellerton.japng.argb8888.Argb8888BitmapSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApngTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final ApngTexture EMPTY = new ApngTexture();

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

    @NotNull
    public static ApngTexture web(@NotNull String apngUrl) {

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(apngUrl))) {
            LOGGER.error("[FANCYMENU] Unable to load Web APNG image! Invalid URL: " + apngUrl);
            return EMPTY;
        }

        ApngTexture apngTexture = new ApngTexture();

        //Download and decode APNG image
        new Thread(() -> {
            InputStream in = null;
            ByteArrayInputStream byteIn = null;
            try {
                URL url = new URL(apngUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("User-Agent", "Mozilla/4.0");
                in = connection.getInputStream();
                //The extract method seems to struggle with a direct web input stream, so read all bytes of it and wrap them into a ByteArrayInputStream
                byteIn = new ByteArrayInputStream(in.readAllBytes());
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to download Web APNG image: " + apngUrl, ex);
            }
            if (byteIn != null) {
                try {
                    populateTexture(apngTexture, byteIn, apngUrl);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to load Web APNG image: " + apngUrl, ex);
                }
            }
            CloseableUtils.closeQuietly(in);
            CloseableUtils.closeQuietly(byteIn);
        }).start();

        return apngTexture;

    }

    @NotNull
    public static ApngTexture local(@NotNull File apng) {

        if (!FileTypes.APNG_IMAGE.isFileTypeLocal(apng)) {
            LOGGER.error("[FANCYMENU] APNG image not found or not a valid APNG: " + apng.getPath());
            return EMPTY;
        }

        ApngTexture apngTexture = new ApngTexture();

        //Decode APNG image
        new Thread(() -> {
            InputStream in = null;
            try {
                in = new FileInputStream(apng);
                populateTexture(apngTexture, in, apng.getPath());
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load APNG image: " + apng.getPath(), ex);
            }
            CloseableUtils.closeQuietly(in);
        }).start();

        return apngTexture;

    }

    protected static void populateTexture(@NotNull ApngTexture apngTexture, @NotNull InputStream in, @NotNull String apngTextureName) throws IOException {
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
            first.nativeImage = NativeImage.read(first.frameInputStream);
            CloseableUtils.closeQuietly(first.closeAfterLoading);
            CloseableUtils.closeQuietly(first.frameInputStream);
            apngTexture.frames = imageAndFirstFrame.frames();
            apngTexture.decoded = true;
        }
        //Decode the full APNG and set its frames to the ApngTexture
        List<ApngFrame> allFrames = readApng(imageAndFirstFrame.sequence(), apngTextureName, false).frames();
        for (ApngFrame frame : allFrames) {
            frame.nativeImage = NativeImage.read(frame.frameInputStream);
            CloseableUtils.closeQuietly(frame.closeAfterLoading);
            CloseableUtils.closeQuietly(frame.frameInputStream);
        }
        apngTexture.frames = allFrames;
        apngTexture.numPlays = imageAndFirstFrame.numPlays;
        apngTexture.decoded = true;
    }

    protected ApngTexture() {
    }

    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning && !this.frames.isEmpty()) {

            this.tickerThreadRunning = true;
            this.lastResourceLocationCall = System.currentTimeMillis();

            new Thread(() -> {

                //Automatically stop thread if APNG was inactive for 10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
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
                    frame.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_apng_texture", new SelfcleaningDynamicTexture(frame.nativeImage));
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
