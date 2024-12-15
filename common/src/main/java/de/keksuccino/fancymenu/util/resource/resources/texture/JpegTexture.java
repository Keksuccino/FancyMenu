package de.keksuccino.fancymenu.util.resource.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.NativeImageUtil;
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

    @Nullable
    protected ResourceLocation resourceLocation;
    protected volatile int width = 10;
    protected volatile int height = 10;
    protected volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile boolean decoded = false;
    protected volatile boolean loadedIntoMinecraft = false;
    protected volatile NativeImage nativeImage;
    protected DynamicTexture dynamicTexture;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean loadingCompleted = false;
    protected volatile boolean loadingFailed = false;
    protected volatile boolean closed = false;

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
            InputStream in = new FileInputStream(textureFile);
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

        new Thread(() -> {
            try {
                InputStream in = WebUtils.openResourceStream(textureURL);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, textureURL, texture);
            } catch (Exception ex) {
                texture.loadingFailed = true;
                LOGGER.error("[FANCYMENU] Failed to read texture from URL: " + textureURL, ex);
            }
        }).start();

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

        new Thread(() -> {
            populateTexture(texture, in, (textureName != null) ? textureName : "[Generic InputStream Source]");
            if (texture.closed) MainThreadTaskExecutor.executeInMainThread(texture::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }).start();

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

        texture.nativeImage = nativeImage;
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
                if (image != null) {
                    texture.nativeImage = image.image;
                    texture.width = image.width;
                    texture.height = image.height;
                    texture.aspectRatio = new AspectRatio(texture.width, texture.height);
                    texture.loadingCompleted = true;
                } else {
                    texture.loadingFailed = true;
                    LOGGER.error("[FANCYMENU] Failed to read texture, NativeImage was NULL: " + textureName);
                }
            } catch (Exception ex) {
                texture.loadingFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load texture: " + textureName, ex);
            }
        }
        texture.decoded = true;
        CloseableUtils.closeQuietly(in);
    }

    /**
     * Converts JPEG images to PNG, because Minecraft dropped support for JPEGs.
     */
    @Nullable
    protected static SizedNativeImage convertJpegToPng(@NotNull InputStream in) {
        int w = 1;
        int h = 1;
        NativeImage nativeImage = null;
        ByteArrayOutputStream byteArrayOut = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(in);
            w = bufferedImage.getWidth();
            h = bufferedImage.getHeight();
            byteArrayOut = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOut);
            //ByteArrayInputStream is important, because using NativeImage#read(byte[]) causes OutOfMemoryExceptions
            nativeImage = NativeImage.read(new ByteArrayInputStream(byteArrayOut.toByteArray()));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to convert JPEG image to PNG!", ex);
        }
        CloseableUtils.closeQuietly(in);
        CloseableUtils.closeQuietly(byteArrayOut);
        return (nativeImage != null) ? new SizedNativeImage(nativeImage, w, h) : null;
    }

    @Nullable
    public ResourceLocation getResourceLocation() {
        if (this.closed) return FULLY_TRANSPARENT_TEXTURE;
        if ((this.resourceLocation == null) && !this.loadedIntoMinecraft && (this.nativeImage != null)) {
            try {
                this.dynamicTexture = new DynamicTexture(this.nativeImage);
                this.resourceLocation = this.registerAbstractTexture(this.dynamicTexture);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get ResourceLocation of JpegTexture!", ex);
            }
            this.loadedIntoMinecraft = true;
        }
        return this.resourceLocation;
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
        if (this.nativeImage != null) return new ByteArrayInputStream(NativeImageUtil.asByteArray(this.nativeImage));
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
     * Only really closes textures that are NOT loaded via ResourceLocation.<br>
     * Does basically nothing for ResourceLocation textures, because these are handled by Minecraft.
     */
    @Override
    public void close() {
        this.closed = true;
        try {
            if (this.dynamicTexture != null) this.dynamicTexture.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to close the DynamicTexture!", ex);
        }
        try {
            if (this.nativeImage != null) this.nativeImage.close();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to close the NativeImage!", ex);
        }
        this.dynamicTexture = null;
        this.nativeImage = null;
        this.resourceLocation = null;
        this.decoded = false;
        this.loadedIntoMinecraft = true;
    }

    protected record SizedNativeImage(@NotNull NativeImage image, int width, int height) {
    }

}

