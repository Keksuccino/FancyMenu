package de.keksuccino.fancymenu.util.mcef;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;

public class BrowserFrameTexture extends AbstractTexture {

    protected final BrowserGlTexture browserGlTexture;

    public BrowserFrameTexture(int id, @NotNull String label) {
        this.browserGlTexture = new BrowserGlTexture(5, label, TextureFormat.RGBA8, 100, 100, 1, 1, id);
        this.browserGlTexture.setTextureFilter(FilterMode.NEAREST, false);
        this.texture = this.browserGlTexture;
        GpuDevice device = RenderSystem.getDevice();
        this.textureView = device.createTextureView(this.texture);
    }

    @Override
    public void setFilter(boolean $$0, boolean $$1) {
        // do nothing
    }

    @Override
    public void setClamp(boolean $$0) {
        // do nothing
    }

    public void setId(int id) {
        this.browserGlTexture.setGlId(id);
    }

    public void setWidth(int width) {
        this.browserGlTexture.setWidth(width);
    }

    public void setHeight(int height) {
        this.browserGlTexture.setHeight(height);
    }

}
