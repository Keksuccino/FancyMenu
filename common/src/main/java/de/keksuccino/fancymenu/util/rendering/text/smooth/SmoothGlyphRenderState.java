package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.jetbrains.annotations.Nullable;

record SmoothGlyphRenderState(Matrix3x2f transform, SmoothFontAtlas atlas, float topLeftX, float bottomLeftX, float bottomRightX, float topRightX, float topY, float bottomY, float minU, float maxU, float minV, float maxV, int color, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

    SmoothGlyphRenderState(Matrix3x2f transform, SmoothFontAtlas atlas, float topLeftX, float bottomLeftX, float bottomRightX, float topRightX, float topY, float bottomY, float minU, float maxU, float minV, float maxV, int color, @Nullable ScreenRectangle scissorArea) {
        this(transform, atlas, topLeftX, bottomLeftX, bottomRightX, topRightX, topY, bottomY, minU, maxU, minV, maxV, color, scissorArea, getBounds(topLeftX, bottomLeftX, bottomRightX, topRightX, topY, bottomY, transform, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        consumer.addVertexWith2DPose(transform, topLeftX, topY).setUv(minU, minV).setColor(color);
        consumer.addVertexWith2DPose(transform, bottomLeftX, bottomY).setUv(minU, maxV).setColor(color);
        consumer.addVertexWith2DPose(transform, bottomRightX, bottomY).setUv(maxU, maxV).setColor(color);
        consumer.addVertexWith2DPose(transform, topRightX, topY).setUv(maxU, minV).setColor(color);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.singleTexture(atlas.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    @Nullable
    private static ScreenRectangle getBounds(float topLeftX, float bottomLeftX, float bottomRightX, float topRightX, float topY, float bottomY, Matrix3x2f transform, @Nullable ScreenRectangle scissorArea) {
        float minX = Math.min(Math.min(topLeftX, bottomLeftX), Math.min(bottomRightX, topRightX));
        float maxX = Math.max(Math.max(topLeftX, bottomLeftX), Math.max(bottomRightX, topRightX));
        float minY = Math.min(topY, bottomY);
        float maxY = Math.max(topY, bottomY);
        int x = (int)Math.floor(minX);
        int y = (int)Math.floor(minY);
        int width = (int)Math.ceil(maxX) - x;
        int height = (int)Math.ceil(maxY) - y;
        if (width <= 0 || height <= 0) {
            return null;
        }
        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(transform);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

}
