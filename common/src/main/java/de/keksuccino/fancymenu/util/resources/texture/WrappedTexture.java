package de.keksuccino.fancymenu.util.resources.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

public class WrappedTexture implements ITexture {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final WrappedTexture FULLY_TRANSPARENT_WRAPPED_TEXTURE = WrappedTexture.of(FULLY_TRANSPARENT_TEXTURE);

    @Nullable
    protected ResourceLocation textureLocation;
    @Nullable
    protected InputStream inputStream;
    protected int width = 10;
    protected int height = 10;
    protected AspectRatio aspectRatio = new AspectRatio(10, 10);

    /**
     * Will wrap an existing texture into an {@link ITexture} to get its size and potentially other meta-level stuff.<br>
     * Keep in mind that the texture needs to be registered already!
     **/
    @NotNull
    public static WrappedTexture of(@NotNull ResourceLocation textureLocation) {
        Objects.requireNonNull(textureLocation);
        WrappedTexture t = new WrappedTexture(textureLocation);
        t.loadTexture();
        return t;
    }

    protected WrappedTexture(@NotNull ResourceLocation textureLocation) {
        this.textureLocation = textureLocation;
    }

    protected void loadTexture() {
        NativeImage i = null;
        try {
            if (this.textureLocation != null) {
                Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(this.textureLocation);
                if (r.isPresent()) {
                    this.inputStream = r.get().open();
                    i = NativeImage.read(this.inputStream);
                    this.width = i.getWidth();
                    this.height = i.getHeight();
                } else {
                    ITexture t = TextureHandler.INSTANCE.getLoadedTextureByResourceLocation(this.textureLocation);
                    if ((t != null) && t.isReady()) {
                        this.width = t.getWidth();
                        this.height = t.getHeight();
                    } else {
                        LOGGER.error("[FANCYMENU] Failed to wrap texture! Texture not found or not ready: " + this.textureLocation);
                        this.textureLocation = null;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to wrap texture: " + this.textureLocation, ex);
            this.textureLocation = null;
        }
        CloseableUtils.closeQuietly(i);
        CloseableUtils.closeQuietly(this.inputStream);
        this.aspectRatio = new AspectRatio(this.width, this.height);
    }

    @Nullable
    public ResourceLocation getResourceLocation() {
        return textureLocation;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @NotNull
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    public boolean isReady() {
        return this.textureLocation != null;
    }

    @Override
    public void reset() {
    }

}
