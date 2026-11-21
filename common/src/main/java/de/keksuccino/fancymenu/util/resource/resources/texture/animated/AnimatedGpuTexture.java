package de.keksuccino.fancymenu.util.resource.resources.texture.animated;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

/**
 * The GPU texture an animated image plays on. Unlike {@link net.minecraft.client.renderer.texture.DynamicTexture}, it keeps no copy of its pixels in memory,
 * because playback only uploads the regions that change between frames.<br>
 * It samples like DynamicTexture, so animated images look exactly like PNG images. Must only be used on the render thread.
 */
public class AnimatedGpuTexture extends AbstractTexture {

    private final int width;
    private final int height;

    public AnimatedGpuTexture(@NotNull String label, int width, int height) {
        this.width = width;
        this.height = height;
        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        this.textureView = device.createTextureView(this.texture);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    /**
     * Uploads tightly packed RGBA pixels to a region of the texture. The pixels get copied right away, so the buffer can get reused after this returns.
     */
    public void writePixels(@NotNull ByteBuffer pixels, int x, int y, int width, int height) {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.getTexture(), pixels, 0, 0, x, y, width, height);
    }

}
