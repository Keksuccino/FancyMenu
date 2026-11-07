package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The GPU texture of one glyph atlas page (RGBA, linear filtering) plus the render type that draws text with it. This is the
 * only part of the atlas that talks to the Minecraft render system, so it is the part that differs between Minecraft versions.
 */
final class SmoothGlyphTexture implements AutoCloseable {

    // Transparent white: filtered samples at glyph edges blend towards white instead of black, so edges don't get darker.
    private static final int CLEAR_COLOR_RGBA = 0x00FFFFFF;

    private final int textureId;
    private final int size;
    private final RenderType renderType;

    SmoothGlyphTexture(@NotNull String label, int size) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.size = size;
        this.textureId = TextureUtil.generateTextureId();
        TextureUtil.prepareImage(this.textureId, size, size);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        this.clear();
        int textureId = this.textureId;
        this.renderType = new RenderType(
                label,
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                () -> {
                    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                    RenderSystem.setShaderTexture(0, textureId);
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                },
                () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                }
        ) {};
    }

    static int maxSize() {
        return RenderSystem.maxSupportedTextureSize();
    }

    void clear() {
        ByteBuffer pixels = MemoryUtil.memAlloc(this.size * this.size * 4).order(ByteOrder.LITTLE_ENDIAN);
        try {
            for (int i = 0; i < this.size * this.size; i++) {
                pixels.putInt(i * 4, CLEAR_COLOR_RGBA);
            }
            this.upload(pixels, 0, 0, this.size, this.size);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    /**
     * Writes tightly packed RGBA pixels into the texture. The data gets copied before this method returns.
     */
    void upload(@NotNull ByteBuffer rgba, int x, int y, int width, int height) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._bindTexture(this.textureId);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(rgba));
    }

    @NotNull
    RenderType renderType() {
        return this.renderType;
    }

    @Override
    public void close() {
        TextureUtil.releaseTextureId(this.textureId);
    }

}
