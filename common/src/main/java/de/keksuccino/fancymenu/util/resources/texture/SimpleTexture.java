package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class SimpleTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final SimpleTexture FULLY_TRANSPARENT_SIMPLE_TEXTURE = SimpleTexture.location(FULLY_TRANSPARENT_TEXTURE);

    @Nullable
    protected ResourceLocation resourceLocation;
    protected int width = 10;
    protected int height = 10;
    protected AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile boolean decoded = false;
    protected volatile boolean loaded = false;
    protected volatile NativeImage nativeImage;
    protected DynamicTexture dynamicTexture;
    protected boolean createdByLocation = false;
    protected boolean closed = false;

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static SimpleTexture location(@NotNull ResourceLocation location) {

        Objects.requireNonNull(location);
        SimpleTexture texture = new SimpleTexture();

        texture.createdByLocation = true;
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                NativeImage image = NativeImage.read(resource.get().open());
                texture.width = image.getWidth();
                texture.height = image.getHeight();
                texture.aspectRatio = new AspectRatio(texture.width, texture.height);
                CloseableUtils.closeQuietly(image);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load texture by ResourceLocation: " + location, ex);
        }
        texture.loaded = true;
        texture.decoded = true;
        texture.resourceLocation = location;

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static SimpleTexture local(@NotNull File textureFile) {

        Objects.requireNonNull(textureFile);
        SimpleTexture texture = new SimpleTexture();

        if (!textureFile.isFile()) {
            LOGGER.error("[FANCYMENU] Failed to load local texture! File not found: " + textureFile.getPath());
            return texture;
        }

        try {
            InputStream in = new FileInputStream(textureFile);
            of(in, textureFile.getPath(), texture);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load local texture: " + textureFile.getPath(), ex);
        }

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.
     */
    @NotNull
    public static SimpleTexture web(@NotNull String textureURL) {

        Objects.requireNonNull(textureURL);
        SimpleTexture texture = new SimpleTexture();

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(textureURL)) {
            LOGGER.error("[FANCYMENU] Failed to load web texture! Invalid URL: " + textureURL);
            return texture;
        }

        new Thread(() -> {
            try {
                InputStream in = WebUtils.openResourceStream(textureURL);
                if (in == null) throw new NullPointerException("Web resource input stream was NULL!");
                of(in, textureURL, texture);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to load web texture: " + textureURL, ex);
            }
        }).start();

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.<br>
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static SimpleTexture of(@NotNull InputStream in, @Nullable String textureName, @Nullable SimpleTexture writeTo) {

        Objects.requireNonNull(in);
        SimpleTexture texture = (writeTo != null) ? writeTo : new SimpleTexture();

        new Thread(() -> {
            populateTexture(texture, in, (textureName != null) ? textureName : "[Generic InputStream Source]");
        }).start();

        return texture;

    }

    /**
     * Supports JPEG and PNG textures.<br>
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static SimpleTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    protected SimpleTexture() {
    }

    protected static void populateTexture(@NotNull SimpleTexture texture, @NotNull InputStream in, @NotNull String textureName) {
        try {
            texture.nativeImage = NativeImage.read(in);
            if (texture.nativeImage != null) {
                texture.width = texture.nativeImage.getWidth();
                texture.height = texture.nativeImage.getHeight();
                texture.aspectRatio = new AspectRatio(texture.width, texture.height);
            } else {
                LOGGER.error("[FANCYMENU] Failed to read texture, NativeImage was NULL: " + textureName);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to load texture: " + textureName, ex);
        }
        texture.decoded = true;
        CloseableUtils.closeQuietly(in);
    }

    @Nullable
    public ResourceLocation getResourceLocation() {
        if ((this.resourceLocation == null) && !this.loaded && (this.nativeImage != null)) {
            try {
                this.dynamicTexture = new DynamicTexture(this.nativeImage);
                this.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_simple_texture", this.dynamicTexture);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        this.loaded = true;
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

    public boolean isReady() {
        return this.decoded;
    }

    @Override
    public void reset() {
    }

    /**
     * Only reloads textures loaded via ResourceLocation.
     */
    @Override
    public void reload() {
        if (this.createdByLocation && (this.resourceLocation != null)) {
            try {
                Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(this.resourceLocation);
                if (resource.isPresent()) {
                    NativeImage image = NativeImage.read(resource.get().open());
                    this.width = image.getWidth();
                    this.height = image.getHeight();
                    this.aspectRatio = new AspectRatio(this.width, this.height);
                    CloseableUtils.closeQuietly(image);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to reload ResourceLocation texture: " + this.resourceLocation, ex);
            }
        }
    }

    /**
     * Only really closes textures that are NOT loaded via ResourceLocation.<br>
     * Does basically nothing for ResourceLocation textures, because these are handled by Minecraft.
     */
    @Override
    public void close() {
        this.closed = true;
        this.createdByLocation = false;
        //Closes NativeImage and DynamicTexture
        if (this.dynamicTexture != null) CloseableUtils.closeQuietly(this.dynamicTexture);
        this.nativeImage = null;
        this.resourceLocation = null;
        this.decoded = false;
        this.loaded = true;
    }

}

