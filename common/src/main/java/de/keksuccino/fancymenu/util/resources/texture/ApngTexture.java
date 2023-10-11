package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.konkrete.rendering.GifDecoder;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
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

    //TODO APNG support adden (PNGJ lib testen)

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
        ExtractedApngImage imageAndFirstFrame = extractFrames(in, apngTextureName, 1);
        boolean sizeSet = false;
        if (!imageAndFirstFrame.frames().isEmpty()) {
            ApngFrame first = imageAndFirstFrame.frames().get(0);
            first.nativeImage = NativeImage.read(first.frameInputStream);
            CloseableUtils.closeQuietly(first.closeAfterLoading);
            CloseableUtils.closeQuietly(first.frameInputStream);
            if (first.nativeImage != null) {
                apngTexture.width = first.nativeImage.getWidth();
                apngTexture.height = first.nativeImage.getHeight();
                apngTexture.aspectRatio = new AspectRatio(first.nativeImage.getWidth(), first.nativeImage.getHeight());
                sizeSet = true;
            }
            apngTexture.frames = imageAndFirstFrame.frames();
            apngTexture.decoded = true;
        }
        //Decode the full APNG and set its frames to the ApngTexture
        List<ApngFrame> allFrames = extractFrames(imageAndFirstFrame.image(), apngTextureName, -1).frames();
        for (ApngFrame frame : allFrames) {
            frame.nativeImage = NativeImage.read(frame.frameInputStream);
            CloseableUtils.closeQuietly(frame.closeAfterLoading);
            CloseableUtils.closeQuietly(frame.frameInputStream);
        }
        if (!sizeSet && !allFrames.isEmpty()) {
            ApngFrame first = allFrames.get(0);
            first.nativeImage = NativeImage.read(first.frameInputStream);
            CloseableUtils.closeQuietly(first.closeAfterLoading);
            CloseableUtils.closeQuietly(first.frameInputStream);
            if (first.nativeImage != null) {
                apngTexture.width = first.nativeImage.getWidth();
                apngTexture.height = first.nativeImage.getHeight();
                apngTexture.aspectRatio = new AspectRatio(first.nativeImage.getWidth(), first.nativeImage.getHeight());
            }
        }
        apngTexture.frames = allFrames;
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
                    boolean sleep = false;
                    try {
                        //Cache frames to avoid possible concurrent modification exceptions
                        List<ApngFrame> cachedFrames = new ArrayList<>(this.frames);
                        if (!cachedFrames.isEmpty()) {
                            //Set initial (first) frame if current is NULL
                            if (this.current == null) {
                                this.current = cachedFrames.get(0);
                                Thread.sleep(Math.max(20, cachedFrames.get(0).delay * 10L));
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
                                }
                                this.current = newCurrent;
                                Thread.sleep(Math.max(20, newCurrent.delay * 10L));
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
     * Set max frames to -1 to read all frames.
     */
    @NotNull
    protected static ApngTexture.ExtractedApngImage extractFrames(@NotNull InputStream in, @NotNull String gifName, int maxFrames) throws IOException {
        GifDecoder.GifImage gif = GifDecoder.read(in);
        return extractFrames(gif, gifName, maxFrames);
    }

    /**
     * Set max frames to -1 to read all frames.
     */
    @NotNull
    protected static ApngTexture.ExtractedApngImage extractFrames(@NotNull GifDecoder.GifImage gif, @NotNull String gifName, int maxFrames) {
        List<ApngFrame> l = new ArrayList<>();
        int gifFrameCount = gif.getFrameCount();
        int i = 0;
        int index = 0;
        while (i < gifFrameCount) {
            try {
                int delay = gif.getDelay(i);
                BufferedImage image = gif.getFrame(i);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", os);
                ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
                l.add(new ApngFrame(index, bis, delay, os));
                index++;
                if ((maxFrames != -1) && (maxFrames <= index)) break;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get frame '" + i + "' of APNG image '" + gifName + "! This can happen if the APNG is corrupted in some way.", ex);
            }
            i++;
        }
        return new ExtractedApngImage(gif, l);
    }

    protected static class ApngFrame {

        protected final int index;
        protected final ByteArrayInputStream frameInputStream;
        protected  final int delay;
        protected final ByteArrayOutputStream closeAfterLoading;
        protected NativeImage nativeImage;
        protected ResourceLocation resourceLocation;
        protected boolean loaded = false;

        protected ApngFrame(int index, ByteArrayInputStream frameInputStream, int delay, ByteArrayOutputStream closeAfterLoading) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delay = delay;
            this.closeAfterLoading = closeAfterLoading;
        }

    }

    protected record ExtractedApngImage(@NotNull GifDecoder.GifImage image, @NotNull List<ApngFrame> frames) {
    }

}
