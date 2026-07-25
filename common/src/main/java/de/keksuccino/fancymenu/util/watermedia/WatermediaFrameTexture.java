package de.keksuccino.fancymenu.util.watermedia;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.watermedia.vulkan.WatermediaVulkanInterop;
import de.keksuccino.fancymenu.util.watermedia.vulkan.WatermediaVulkanTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

public class WatermediaFrameTexture extends AbstractTexture {

    private static final String LABEL_FANCYMENU = "FancyMenu WaterMedia frame";
    private static final int TEXTURE_USAGE_FANCYMENU = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING;
    private static final FrameBufferCache FRAME_BUFFER_CACHE_FANCYMENU = new FrameBufferCache();

    @Nullable protected WatermediaGlTexture watermediaGlTexture;
    @Nullable protected WatermediaVulkanTexture watermediaVulkanTexture;
    @Nullable protected WatermediaVulkanTextureView watermediaVulkanTextureView;
    protected final boolean vulkan;

    public WatermediaFrameTexture(long handle) {
        this.vulkan = RenderingUtils.isVulkanActive();
        if (this.vulkan) {
            VulkanDevice device = WatermediaVulkanInterop.device();
            if (device == null) throw new IllegalStateException("Minecraft's Vulkan device is unavailable for the Watermedia frame texture");
            this.watermediaVulkanTexture = new WatermediaVulkanTexture(device, TEXTURE_USAGE_FANCYMENU, LABEL_FANCYMENU, GpuFormat.RGBA8_UNORM, 100, 100, 1, 1);
            this.watermediaVulkanTextureView = new WatermediaVulkanTextureView(device, this.watermediaVulkanTexture);
            this.texture = this.watermediaVulkanTexture;
            this.textureView = this.watermediaVulkanTextureView;
        } else {
            this.watermediaGlTexture = new WatermediaGlTexture(TEXTURE_USAGE_FANCYMENU, LABEL_FANCYMENU, GpuFormat.RGBA8_UNORM, 100, 100, 1, 1, WatermediaReflectionBridge.openGlTextureId(handle));
            this.texture = this.watermediaGlTexture;
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        }
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST, false);
        this.setHandle(handle);
    }

    public void setHandle(long handle) {
        if (this.vulkan) {
            if (this.watermediaVulkanTextureView != null) this.watermediaVulkanTextureView.setExternalImageView(handle);
            return;
        }
        int id = WatermediaReflectionBridge.openGlTextureId(handle);
        if (this.watermediaGlTexture == null || this.watermediaGlTexture.glId() == id) return;
        int width = Math.max(1, this.watermediaGlTexture.getWidth(0));
        int height = Math.max(1, this.watermediaGlTexture.getHeight(0));
        this.watermediaGlTexture = new WatermediaGlTexture(TEXTURE_USAGE_FANCYMENU, LABEL_FANCYMENU, GpuFormat.RGBA8_UNORM, width, height, 1, 1, id);
        this.texture = this.watermediaGlTexture;
        if (this.textureView != null) {
            this.textureView.close();
        }
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
    }

    public void setWidth(int width) {
        if (this.watermediaGlTexture != null) this.watermediaGlTexture.setWidth(width);
        if (this.watermediaVulkanTexture != null) this.watermediaVulkanTexture.setWidth(width);
    }

    public void setHeight(int height) {
        if (this.watermediaGlTexture != null) this.watermediaGlTexture.setHeight(height);
        if (this.watermediaVulkanTexture != null) this.watermediaVulkanTexture.setHeight(height);
    }

    protected static class WatermediaGlTexture extends GlTexture {

        protected int width;
        protected int height;

        protected WatermediaGlTexture(int usage, String label, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels, int glId) {
            super(usage, label, format, width, height, depthOrLayers, mipLevels, glId, FRAME_BUFFER_CACHE_FANCYMENU);
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

    protected static class WatermediaVulkanTexture extends VulkanGpuTexture {

        protected int width;
        protected int height;

        protected WatermediaVulkanTexture(VulkanDevice device, int usage, String label, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
            // Only a 1x1 owned placeholder is allocated. Minecraft reads the overridden logical size while rendering WaterMedia's external image view.
            super(device, usage, label, format, 1, 1, depthOrLayers, mipLevels);
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

    }

}
