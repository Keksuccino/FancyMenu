package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.ReleasableFileInputStream;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
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
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class JpegTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final TextureManagerEntry<NativeImage, DynamicTexture> textureEntry = TextureManagerEntry.dynamicTexture();
    protected volatile int width = 10;
    protected volatile int height = 10;
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile boolean decoded = false;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean loadingCompleted = false;
    protected volatile boolean loadingFailed = false;
    protected volatile boolean closed = false;
    /** The stream of the local file while it loads, so closing the texture can release the file right away. **/
    @Nullable
    protected volatile ReleasableFileInputStream loadingFileStream;

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture location(@NotNull ResourceLocation location, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(location);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        texture.sourceLocation = location;

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                of(Objects.requireNonNull(resource.get().open()), location.toString(), texture);
            }
        } catch (Exception ex) {
            texture.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read texture from ResourceLocation: " + location, ex);
        }
        return texture;

    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture local(@NotNull File textureFile) {
        return local(textureFile, null);
    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture local(@NotNull File textureFile, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(textureFile);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        texture.sourceFile = textureFile;

        if (!textureFile.isFile()) {
            texture.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read texture from file! File not found: " + textureFile.getPath());
            return texture;
        }

        try {
            ReleasableFileInputStream in = new ReleasableFileInputStream(textureFile);
            texture.loadingFileStream = in;
            of(in, textureFile.getPath(), texture);
        } catch (Exception ex) {
            texture.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read texture from file: " + textureFile.getPath(), ex);
        }

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture web(@NotNull String textureURL) {
        return web(textureURL, null);
    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static JpegTexture web(@NotNull String textureURL, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(textureURL);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        texture.sourceURL = textureURL;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(textureURL)) {
            texture.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read texture from URL! Invalid URL: " + textureURL);
            return texture;
        }

        FancyMenuThreads.startDaemonThread(() -> {
            try {
                of(WebUtils.openResourceStreamOrThrow(textureURL, WebUtils.WebResourceType.IMAGE), textureURL, texture);
            } catch (Exception ex) {
                texture.loadingFailed = true;
                // Shutting down the client cancels web loads, which is no error
                if (!ClientShutdownHandler.isShuttingDown()) LOGGER.error("[FANCYMENU] Failed to read texture from URL: " + textureURL, ex);
            }
        }, "JpegTexture-WebLoader");

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.<br>
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static JpegTexture of(@NotNull InputStream in, @Nullable String textureName, @Nullable JpegTexture writeTo) {

        Objects.requireNonNull(in);
        JpegTexture texture = (writeTo != null) ? writeTo : new JpegTexture();

        FancyMenuThreads.startDaemonThread(() -> {
            populateTexture(texture, in, (textureName != null) ? textureName : "[Generic InputStream Source]");
            if (texture.closed) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }, "JpegTexture-Decoder");

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.<br>
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static JpegTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    @NotNull
    public static JpegTexture of(@NotNull NativeImage nativeImage) {

        Objects.requireNonNull(nativeImage);

        JpegTexture texture = new JpegTexture();

        texture.textureEntry.adopt(nativeImage);
        texture.width = nativeImage.getWidth();
        texture.height = nativeImage.getHeight();
        texture.aspectRatio = new AspectRatio(nativeImage.getWidth(), nativeImage.getHeight());
        texture.decoded = true;
        texture.loadingCompleted = true;

        return texture;

    }

    protected JpegTexture() {
    }

    protected static void populateTexture(@NotNull JpegTexture texture, @NotNull InputStream in, @NotNull String textureName) {
        if (!texture.closed) {
            try {
                SizedNativeImage image = convertJpegToPng(in);
                texture.width = image.width;
                texture.height = image.height;
                texture.aspectRatio = new AspectRatio(texture.width, texture.height);
                if (texture.textureEntry.adopt(image.image)) texture.loadingCompleted = true;
            } catch (Exception ex) {
                texture.loadingFailed = true;
                // Closing the texture closes the stream of its file right away and shutting down the client cancels web loads, so reading fails then, which is no error
                if (!texture.closed && !ClientShutdownHandler.isShuttingDown()) LOGGER.error("[FANCYMENU] Failed to load texture: " + textureName, ex);
            }
        }
        texture.decoded = true;
        CloseableUtils.closeQuietly(in);
        texture.loadingFileStream = null;
    }

    /**
     * Converts JPEG images to PNG, because Minecraft dropped support for JPEGs. Closes the passed {@link InputStream}.
     *
     * @throws IOException If the stream holds no readable image or reading it failed.
     */
    @NotNull
    protected static SizedNativeImage convertJpegToPng(@NotNull InputStream in) throws IOException {
        BufferedImage bufferedImage;
        // Closes the stream right after reading the JPEG, so its file isn't open while the image gets converted
        try (InputStream input = in) {
            bufferedImage = ImageIO.read(input);
        }
        if (bufferedImage == null) throw new IOException("No image reader can read the JPEG image");
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOut);
        //ByteArrayInputStream is important, because using NativeImage#read(byte[]) causes OutOfMemoryExceptions
        NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(byteArrayOut.toByteArray()));
        return new SizedNativeImage(nativeImage, bufferedImage.getWidth(), bufferedImage.getHeight());
    }

    @Nullable
    public ResourceLocation getResourceLocation() {
        if (this.closed) return FULLY_TRANSPARENT_TEXTURE;
        if (this.textureEntry.canRegister()) {
            try {
                this.textureEntry.register(ResourceLocation.fromNamespaceAndPath("fancymenu", "dynamic/simple_jpeg_texture_" + System.nanoTime()));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get ResourceLocation of JpegTexture!", ex);
            }
        }
        return this.textureEntry.getLocation();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    @NotNull
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    @Override
    public @Nullable InputStream open() throws IOException {
        NativeImage image = this.textureEntry.getImage();
        if (image != null) return new ByteArrayInputStream(image.asByteArray());
        return null;
    }

    @Override
    public boolean isReady() {
        //Everything important (like size) is set at this point, so it is considered ready
        return this.decoded;
    }

    @Override
    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Releases the owned dynamic entry through Minecraft's texture manager, which disposes both the DynamicTexture and
     * its NativeImage. JPEG resources loaded from a ResourceLocation are decoded into owned dynamic entries too.
     */
    @Override
    public void close() {
        this.closed = true;
        // Releases the file of a texture that still loads right away, a read of the loading thread that runs right now finishes first
        CloseableUtils.closeQuietly(this.loadingFileStream);
        this.textureEntry.close();
        this.decoded = false;
    }

    protected record SizedNativeImage(@NotNull NativeImage image, int width, int height) {
    }

}
