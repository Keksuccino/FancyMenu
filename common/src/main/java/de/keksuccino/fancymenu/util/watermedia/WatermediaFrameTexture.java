package de.keksuccino.fancymenu.util.watermedia;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class WatermediaFrameTexture extends AbstractTexture {

    private static final String LABEL_FANCYMENU = "FancyMenu WaterMedia frame";
    private static final int TEXTURE_USAGE_FANCYMENU = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING;

    protected WatermediaGlTexture watermediaTexture;

    public WatermediaFrameTexture(int id) {
        this.watermediaTexture = new WatermediaGlTexture(TEXTURE_USAGE_FANCYMENU, LABEL_FANCYMENU, TextureFormat.RGBA8, 100, 100, 1, 1, id);
        this.texture = this.watermediaTexture;
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST, false);
    }

    public void setId(int id) {
        if (this.watermediaTexture.glId() == id) return;
        int width = Math.max(1, this.watermediaTexture.getWidth(0));
        int height = Math.max(1, this.watermediaTexture.getHeight(0));
        this.watermediaTexture = new WatermediaGlTexture(TEXTURE_USAGE_FANCYMENU, LABEL_FANCYMENU, TextureFormat.RGBA8, width, height, 1, 1, id);
        this.texture = this.watermediaTexture;
        if (this.textureView != null) {
            this.textureView.close();
        }
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
    }

    public void setWidth(int width) {
        this.watermediaTexture.setWidth(width);
    }

    public void setHeight(int height) {
        this.watermediaTexture.setHeight(height);
    }

    protected static class WatermediaGlTexture extends GlTexture {

        protected int width;
        protected int height;

        protected WatermediaGlTexture(int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int glId) {
            super(usage, label, format, width, height, depthOrLayers, mipLevels, glId);
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth(int mipLevel) {
            return this.width >> mipLevel;
        }

        protected void setWidth(int width) {
            this.width = width;
        }

        @Override
        public int getHeight(int mipLevel) {
            return this.height >> mipLevel;
        }

        protected void setHeight(int height) {
            this.height = height;
        }

        @Override
        public void close() {
            // Watermedia owns the OpenGL texture object.
        }

    }

}
