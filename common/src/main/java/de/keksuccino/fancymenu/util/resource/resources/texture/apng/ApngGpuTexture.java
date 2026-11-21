package de.keksuccino.fancymenu.util.resource.resources.texture.apng;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

/**
 * The GPU texture an APNG animation plays on. Unlike {@link net.minecraft.client.renderer.texture.DynamicTexture}, it keeps no copy of its pixels in memory,
 * because playback only uploads the regions that change between frames.<br>
 * It samples like DynamicTexture, so APNG images look exactly like PNG images. Must only be used on the render thread.
 */
public class ApngGpuTexture extends AbstractTexture {

    private final int width;
    private final int height;

    /**
     * @param label Debug name of the texture. OpenGL textures of this Minecraft version have no labels, it only exists so all versions create the texture the same way.
     */
    public ApngGpuTexture(@NotNull String label, int width, int height) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.width = width;
        this.height = height;
        TextureUtil.prepareImage(this.getId(), width, height);
        // DynamicTexture gets nearest filtering from NativeImage on every upload, this texture sets it once
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
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
        RenderSystem.assertOnRenderThreadOrInit();
        this.bind();
        // Other uploads leave their unpack state behind, so it has to get set for tightly packed rows every time
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(pixels));
    }

    @Override
    public void load(@NotNull ResourceManager resourceManager) {
    }

    @Override
    public void close() {
        this.releaseId();
    }

}
