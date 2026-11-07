package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.TextureSetup;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import java.nio.ByteBuffer;

/**
 * The GPU texture of one glyph atlas page (RGBA, linear filtering). This is the only part of the atlas that talks to the
 * Minecraft render system, so it is the part that differs between Minecraft versions.
 */
final class SmoothGlyphTexture implements AutoCloseable {

    // Transparent white: filtered samples at glyph edges blend towards white instead of black, so edges don't get darker.
    private static final Vector4f CLEAR_COLOR = new Vector4f(1.0F, 1.0F, 1.0F, 0.0F);

    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private final TextureSetup textureSetup;

    SmoothGlyphTexture(@NotNull String label, int size) {
        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture(() -> label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, GpuFormat.RGBA8_UNORM, size, size, 1, 1);
        this.textureView = device.createTextureView(this.texture);
        this.textureSetup = TextureSetup.singleTexture(this.textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        this.clear();
    }

    static int maxSize() {
        return RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSize();
    }

    void clear() {
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, CLEAR_COLOR);
    }

    /**
     * Writes tightly packed RGBA pixels into the texture. The data gets copied before this method returns.
     */
    void upload(@NotNull ByteBuffer rgba, int x, int y, int width, int height) {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, rgba, 0, 0, x, y, width, height);
    }

    @NotNull
    TextureSetup textureSetup() {
        return this.textureSetup;
    }

    @Override
    public void close() {
        this.textureView.close();
        this.texture.close();
    }

}
