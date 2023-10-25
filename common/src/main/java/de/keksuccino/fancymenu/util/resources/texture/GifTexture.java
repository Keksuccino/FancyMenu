package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.rendering.GifDecoder;
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

public class GifTexture implements ITexture, PlayableResource {

    private static final Logger LOGGER = LogManager.getLogger();

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
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean closed = false;

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
            LOGGER.error("[FANCYMENU] Failed to read GIF image from ResourceLocation: " + location, ex);
        }

        return texture;

    }

    @NotNull
    public static GifTexture local(@NotNull File gif) {
        return local(gif, null);
    }

    @NotNull
    public static GifTexture local(@NotNull File gif, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(gif);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceFile = gif;

        if (!gif.isFile()) {
            LOGGER.error("[FANCYMENU] Failed to read GIF image from file! File not found: " + gif.getPath());
            return texture;
        }

        //Decode APNG image
        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(gif);
                of(in, gif.getPath(), texture);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to read GIF image from file: " + gif.getPath(), ex);
            }
        }).start();

        return texture;

    }

    @NotNull
    public static GifTexture web(@NotNull String gifUrl) {
        return web(gifUrl, null);
    }

    @NotNull
    public static GifTexture web(@NotNull String gifUrl, @Nullable GifTexture writeTo) {

        Objects.requireNonNull(gifUrl);
        GifTexture texture = (writeTo != null) ? writeTo : new GifTexture();

        texture.sourceURL = gifUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(Objects.requireNonNull(gifUrl))) {
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
    public static GifTexture of(@NotNull InputStream in) {
        return of(in, null, null);
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
            String name = (gifTextureName != null) ? gifTextureName : "[Generic InputStream Source]";
            populateTexture(texture, in, name);
            if (texture.closed) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }).start();

        return texture;

    }

    protected static void populateTexture(@NotNull GifTexture gifTexture, @NotNull InputStream in, @NotNull String gifTextureName) {
        if (!gifTexture.closed) {
            //Decode first frame and set it as temporary frame list to show GIF quicker
            DecodedGifImage imageAndFirstFrame;
            try {
                imageAndFirstFrame = extractFrames(in, gifTextureName, 1);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load GIF image, because DecodedGifImage was NULL: " + gifTextureName, ex);
                gifTexture.decoded = true;
                return;
            }
            boolean sizeSet = false;
            if (!imageAndFirstFrame.frames().isEmpty()) {
                GifFrame first = imageAndFirstFrame.frames().get(0);
                try {
                    first.nativeImage = NativeImage.read(first.frameInputStream);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to read preview frame of GIF image into NativeImage: " + gifTextureName, ex);
                }
                CloseableUtils.closeQuietly(first.closeAfterLoading);
                CloseableUtils.closeQuietly(first.frameInputStream);
                if (first.nativeImage != null) {
                    gifTexture.width = first.nativeImage.getWidth();
                    gifTexture.height = first.nativeImage.getHeight();
                    gifTexture.aspectRatio = new AspectRatio(first.nativeImage.getWidth(), first.nativeImage.getHeight());
                    sizeSet = true;
                }
                gifTexture.frames = imageAndFirstFrame.frames();
                gifTexture.decoded = true;
            }
            //Decode the full GIF and set its frames to the GifTexture
            DecodedGifImage allFrames = null;
            try {
                allFrames = extractFrames(imageAndFirstFrame.image(), gifTextureName, -1);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load GIF image: " + gifTextureName, ex);
            }
            if (allFrames != null) {
                for (GifFrame frame : allFrames.frames()) {
                    try {
                        frame.nativeImage = NativeImage.read(frame.frameInputStream);
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to read frame of GIF image into NativeImage: " + gifTextureName, ex);
                    }
                    CloseableUtils.closeQuietly(frame.closeAfterLoading);
                    CloseableUtils.closeQuietly(frame.frameInputStream);
                }
                if (!sizeSet && !allFrames.frames().isEmpty()) {
                    GifFrame first = allFrames.frames().get(0);
                    if (first.nativeImage != null) {
                        gifTexture.width = first.nativeImage.getWidth();
                        gifTexture.height = first.nativeImage.getHeight();
                        gifTexture.aspectRatio = new AspectRatio(first.nativeImage.getWidth(), first.nativeImage.getHeight());
                    }
                }
                gifTexture.frames = allFrames.frames();
            }
        }
        gifTexture.decoded = true;
        CloseableUtils.closeQuietly(in);
    }

    protected GifTexture() {
    }

    @SuppressWarnings("all")
    protected void startTickerIfNeeded() {
        if (!this.tickerThreadRunning && !this.frames.isEmpty() && !this.closed) {

            this.tickerThreadRunning = true;
            this.lastResourceLocationCall = System.currentTimeMillis();

            new Thread(() -> {

                //Automatically stop thread if GIF was inactive for 10 seconds
                while ((this.lastResourceLocationCall + 10000) > System.currentTimeMillis()) {
                    if (this.frames.isEmpty() || this.closed) break;
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

    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (this.closed) return FULLY_TRANSPARENT_TEXTURE;
        this.lastResourceLocationCall = System.currentTimeMillis();
        this.startTickerIfNeeded();
        GifFrame frame = this.current;
        if (frame != null) {
            if ((frame.resourceLocation == null) && !frame.loaded && (frame.nativeImage != null)) {
                try {
                    frame.dynamicTexture = new DynamicTexture(frame.nativeImage);
                    frame.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_gif_texture_frame", frame.dynamicTexture);
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
    public boolean isReady() {
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded;
    }

    public void reset() {
        this.current = null;
        List<GifFrame> l = new ArrayList<>(this.frames);
        if (!l.isEmpty()) {
            this.current = l.get(0);
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

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        this.sourceLocation = null;
        for (GifFrame frame : new ArrayList<>(this.frames)) {
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

    /**
     * Set max frames to -1 to read all frames.
     */
    @NotNull
    protected static GifTexture.DecodedGifImage extractFrames(@NotNull InputStream in, @NotNull String gifName, int maxFrames) throws IOException {
        GifDecoder.GifImage gif = GifDecoder.read(in);
        return extractFrames(gif, gifName, maxFrames);
    }

    /**
     * Set max frames to -1 to read all frames.
     */
    @NotNull
    protected static GifTexture.DecodedGifImage extractFrames(@NotNull GifDecoder.GifImage gif, @NotNull String gifName, int maxFrames) {
        List<GifFrame> l = new ArrayList<>();
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
                if ((maxFrames != -1) && (maxFrames <= index)) break;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get frame '" + i + "' of GIF image '" + gifName + "!", ex);
            }
            i++;
        }
        return new DecodedGifImage(gif, l);
    }

    protected static class GifFrame {

        protected final int index;
        protected final ByteArrayInputStream frameInputStream;
        protected final int delay;
        protected final ByteArrayOutputStream closeAfterLoading;
        protected volatile NativeImage nativeImage;
        protected DynamicTexture dynamicTexture;
        protected ResourceLocation resourceLocation;
        protected boolean loaded = false;

        protected GifFrame(int index, ByteArrayInputStream frameInputStream, int delay, ByteArrayOutputStream closeAfterLoading) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delay = delay;
            this.closeAfterLoading = closeAfterLoading;
        }

    }

    protected record DecodedGifImage(@NotNull GifDecoder.GifImage image, @NotNull List<GifFrame> frames) {
    }

}
