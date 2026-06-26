package de.keksuccino.fancymenu.util.mcef;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;

public class BrowserFrameTexture extends AbstractTexture {

    protected BrowserGlTexture browserGlTexture;
    protected final String label;
    protected final int textureUsage;

    public BrowserFrameTexture(int id, @NotNull String label) {
        this(id, label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING);
    }

    protected BrowserFrameTexture(int id, @NotNull String label, int textureUsage) {
        this.label = label;
        this.textureUsage = textureUsage;
        this.browserGlTexture = new BrowserGlTexture(this.textureUsage, label, GpuFormat.RGBA8_UNORM, 100, 100, 1, 1, id);
        this.texture = this.browserGlTexture;
        GpuDevice device = RenderSystem.getDevice();
        this.textureView = device.createTextureView(this.texture);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST, false);
    }

    public void setId(int id) {
        if (this.browserGlTexture.glId() == id) return;
        int width = Math.max(1, this.browserGlTexture.getWidth(0));
        int height = Math.max(1, this.browserGlTexture.getHeight(0));
        this.browserGlTexture = new BrowserGlTexture(this.textureUsage, this.label, GpuFormat.RGBA8_UNORM, width, height, 1, 1, id);
        this.texture = this.browserGlTexture;
        if (this.textureView != null) {
            this.textureView.close();
        }
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
    }

    public void setWidth(int width) {
        this.browserGlTexture.setWidth(width);
    }

    public void setHeight(int height) {
        this.browserGlTexture.setHeight(height);
    }

}
