package de.keksuccino.fancymenu.rendering.texture;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Optional;

public class SimpleTexture {

    private final ResourceLocation textureLocation;
    private int width;
    private int height;
    private AspectRatio aspectRatio;

    /** Will return the new {@link SimpleTexture} instance or NULL if {@code textureLocation} was NULL. **/
    @Nullable
    public static SimpleTexture create(ResourceLocation textureLocation) {
        if (textureLocation != null) {
            return new SimpleTexture(textureLocation);
        }
        return null;
    }

    private SimpleTexture(ResourceLocation textureLocation) {
        this.textureLocation = textureLocation;
        this.calculateSize();
    }

    private void calculateSize() {
        InputStream in = null;
        NativeImage i = null;
        try {
            if (this.textureLocation != null) {
                Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(this.textureLocation);
                if (r.isPresent()) {
                    in = r.get().open();
                    i = NativeImage.read(in);
                    this.width = i.getWidth();
                    this.height = i.getHeight();
                } else {
                    ExternalTextureResourceLocation et = ExternalTextureHandler.INSTANCE.getLoadedTextureByResourceLocation(this.textureLocation);
                    if (et != null) {
                        this.width = et.getWidth();
                        this.height = et.getHeight();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (i != null) {
                i.close();
            }
        } catch (Exception ignored) {}
        IOUtils.closeQuietly(in);
        this.aspectRatio = new AspectRatio(this.width, this.height);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

}
