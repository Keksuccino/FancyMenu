package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class SimpleTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected ResourceLocation resourceLocation;
    protected int width = 10;
    protected int height = 10;
    protected AspectRatio aspectRatio = new AspectRatio(10, 10);
    protected volatile boolean decoded = false;
    protected volatile boolean loaded = false;
    protected volatile NativeImage nativeImage;

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
                //TODO better close NativeImage after this???
                this.resourceLocation = Minecraft.getInstance().getTextureManager().register("fancymenu_simple_texture", new SelfcleaningDynamicTexture(this.nativeImage));
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

}

