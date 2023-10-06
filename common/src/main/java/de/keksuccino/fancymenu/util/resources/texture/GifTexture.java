package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
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

public class GifTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final GifTexture EMPTY = new GifTexture();

    @NotNull
    protected volatile List<GifFrame> frames = new ArrayList<>();
    @Nullable
    protected volatile GifFrame current = null;
    @NotNull
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile int width = 10;
    protected volatile int height = 10;
    protected volatile long lastResourceLocationCall = -1;
    protected volatile boolean tickerThreadRunning = false;
    protected volatile boolean decoded = false;

    @NotNull
    public static GifTexture web(@NotNull String gifUrl) {

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(gifUrl))) {
            LOGGER.error("[FANCYMENU] Unable to load Web GIF image! Invalid URL: " + gifUrl);
            return EMPTY;
        }

        GifTexture gifTexture = new GifTexture();

        //Decode GIF image
        new Thread(() -> {
            InputStream in = null;
            ByteArrayInputStream byteIn = null;
            try {
                URL url = new URL(gifUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("User-Agent", "Mozilla/4.0");
                in = connection.getInputStream();
                //The extract method seems to struggle with a direct web input stream, so read all bytes of it and wrap them into a ByteArrayInputStream
                byteIn = new ByteArrayInputStream(in.readAllBytes());
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to download Web GIF image: " + gifUrl, ex);
            }
            if (byteIn != null) {
                try {
                    List<GifFrame> f = extractFrames(byteIn, gifUrl);
                    for (GifFrame frame : f) {
                        frame.nativeImage = NativeImage.read(frame.frameInputStream);
                        CloseableUtils.closeQuietly(frame.closeAfterLoading);
                        CloseableUtils.closeQuietly(frame.frameInputStream);
                    }
                    if (!f.isEmpty()) {
                        GifFrame first = f.get(0);
                        if (first.nativeImage != null) {
                            gifTexture.width = first.nativeImage.getWidth();
                            gifTexture.height = first.nativeImage.getHeight();
                            gifTexture.aspectRatio = new AspectRatio(first.nativeImage.getWidth(), first.nativeImage.getHeight());
                        }
                    }
                    gifTexture.frames = f;
                    gifTexture.decoded = true;
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to load Web GIF image: " + gifUrl, ex);
                }
            }
            CloseableUtils.closeQuietly(in);
            CloseableUtils.closeQuietly(byteIn);
        }).start();

        return gifTexture;

    }

    @NotNull
    public static GifTexture local(@NotNull File gif) {

        if (!gif.isFile() || !gif.getName().toLowerCase().endsWith(".gif")) {
            LOGGER.error("[FANCYMENU] GIF image not found: " + gif.getPath());
            return EMPTY;
        }

        GifTexture gifTexture = new GifTexture();

        //Decode GIF image
        new Thread(() -> {
            InputStream in = null;
            try {
                in = new FileInputStream(gif);
                List<GifFrame> f = extractFrames(in, gif.getPath());
                for (GifFrame frame : f) {
                    frame.nativeImage = NativeImage.read(frame.frameInputStream);
                    CloseableUtils.closeQuietly(frame.closeAfterLoading);
                    CloseableUtils.closeQuietly(frame.frameInputStream);
                }
                if (!f.isEmpty()) {
                    GifFrame first = f.get(0);
                    if (first.nativeImage != null) {
                        gifTexture.width = first.nativeImage.getWidth();
                        gifTexture.height = first.nativeImage.getHeight();
                        gifTexture.aspectRatio = new AspectRatio(first.nativeImage.getWidth(), first.nativeImage.getHeight());
                    }
                }
                gifTexture.frames = f;
                gifTexture.decoded = true;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load GIF image: " + gif.getPath(), ex);
            }
            CloseableUtils.closeQuietly(in);
        }).start();

        return gifTexture;

    }

    protected GifTexture() {
    }

    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning && !this.frames.isEmpty()) {

            this.tickerThreadRunning = true;
            this.lastResourceLocationCall = System.currentTimeMillis();

            new Thread(() -> {

                //Automatically stop thread if GIF was inactive for 10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
                    boolean sleep = false;
                    try {
                        //Cache frames to avoid possible concurrent modification exceptions
                        List<GifFrame> cachedFrames = new ArrayList<>(this.frames);
                        if (!cachedFrames.isEmpty()) {
                            //Set initial (first) frame if current is NULL
                            if (this.current == null) {
                                this.current = cachedFrames.get(0);
                                Thread.sleep(Math.max(20, cachedFrames.get(0).delay * 10L));
                            }
                            //Cache current frame to make sure it stays the same instance while working with it
                            GifFrame cachedCurrent = this.current;
                            if (cachedCurrent != null) {
                                //Go to the next frame if current frame display time is over
                                GifFrame newCurrent;
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

    @NotNull
    @Override
    public ResourceLocation getResourceLocation() {
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        GifFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    frame.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_gif_texture", new SelfcleaningDynamicTexture(frame.nativeImage));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            frame.loaded = true;
            if (frame.resourceLocation != null) return frame.resourceLocation;
        }
        return RenderableResource.FULLY_TRANSPARENT_TEXTURE;
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

    @NotNull
    protected static List<GifFrame> extractFrames(@NotNull InputStream in, @NotNull String gifName) throws IOException {
        List<GifFrame> l = new ArrayList<>();
        GifDecoder.GifImage gif = GifDecoder.read(in);
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
                l.add(new GifFrame(index, bis, delay, os));
                index++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get frame '" + i + "' of GIF image '" + gifName + "! This can happen if the GIF is corrupted in some way.", ex);
            }
            i++;
        }
        return l;
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

    protected static class GifFrame {

        protected final int index;
        protected final ByteArrayInputStream frameInputStream;
        protected  final int delay;
        protected final ByteArrayOutputStream closeAfterLoading;
        protected NativeImage nativeImage;
        protected ResourceLocation resourceLocation;
        protected boolean loaded = false;

        protected GifFrame(int index, ByteArrayInputStream frameInputStream, int delay, ByteArrayOutputStream closeAfterLoading) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delay = delay;
            this.closeAfterLoading = closeAfterLoading;
        }

    }

}
