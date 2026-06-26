package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBufferBuilder;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Optional;

record SmoothGlyphRenderState(
        Matrix3x2f transform,
        SmoothFontAtlas atlas,
        float topLeftX,
        float bottomLeftX,
        float bottomRightX,
        float topRightX,
        float topY,
        float bottomY,
        float minU,
        float maxU,
        float minV,
        float maxV,
        int color,
        float sdfRange,
        float sdfEdge,
        float sdfSharpness,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    private static final String SDF_INFO_NAME_FANCYMENU = "SdfInfo";
    private static final VertexFormat SMOOTH_TEXT_VERTEX_FORMAT_FANCYMENU = VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM)
            .addAttribute(SDF_INFO_NAME_FANCYMENU, GpuFormat.RGBA32_FLOAT)
            .build();
    private static final VertexFormatElement SDF_INFO_FANCYMENU = getVertexFormatElement_FancyMenu(SMOOTH_TEXT_VERTEX_FORMAT_FANCYMENU, SDF_INFO_NAME_FANCYMENU);
    private static final RenderPipeline SMOOTH_TEXT_PIPELINE_FANCYMENU = RenderPipeline.builder().withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_gui_smooth_text"))
            .withVertexShader("core/fancymenu_gui_smooth_text")
            .withFragmentShader("core/fancymenu_gui_smooth_text")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, SMOOTH_TEXT_VERTEX_FORMAT_FANCYMENU)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    SmoothGlyphRenderState(Matrix3x2f transform, SmoothFontAtlas atlas, float topLeftX, float bottomLeftX, float bottomRightX, float topRightX, float topY, float bottomY, float minU, float maxU, float minV, float maxV, int color, @Nullable ScreenRectangle scissorArea) {
        this(
                transform,
                atlas,
                topLeftX,
                bottomLeftX,
                bottomRightX,
                topRightX,
                topY,
                bottomY,
                minU,
                maxU,
                minV,
                maxV,
                color,
                atlas.getEffectiveSdfRange(),
                SmoothTextShader.getResolvedEdge(),
                SmoothTextShader.getResolvedSharpness(),
                scissorArea,
                getBounds(topLeftX, bottomLeftX, bottomRightX, topRightX, topY, bottomY, transform, scissorArea)
        );
    }

    private static VertexFormatElement getVertexFormatElement_FancyMenu(VertexFormat format, String attributeName) {
        VertexFormatElement element = format.getElement(attributeName);
        if (element == null) {
            throw new IllegalStateException("Missing vertex format element: " + attributeName);
        }
        return element;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        addVertex_FancyMenu(consumer, topLeftX, topY, minU, minV);
        addVertex_FancyMenu(consumer, bottomLeftX, bottomY, minU, maxV);
        addVertex_FancyMenu(consumer, bottomRightX, bottomY, maxU, maxV);
        addVertex_FancyMenu(consumer, topRightX, topY, maxU, minV);
    }

    private void addVertex_FancyMenu(VertexConsumer consumer, float x, float y, float u, float v) {
        consumer.addVertexWith2DPose(transform, x, y).setUv(u, v).setColor(color);
        writeVec4_FancyMenu(consumer, SDF_INFO_FANCYMENU, sdfRange, sdfEdge, sdfSharpness, 0.0F);
    }

    private static void writeVec4_FancyMenu(VertexConsumer consumer, VertexFormatElement element, float x, float y, float z, float w) {
        long pointer = ((IMixinBufferBuilder)consumer).get_vertexPointer_FancyMenu();
        if (pointer == -1L) {
            return;
        }
        long elementPointer = pointer + element.offset();
        MemoryUtil.memPutFloat(elementPointer, x);
        MemoryUtil.memPutFloat(elementPointer + 4L, y);
        MemoryUtil.memPutFloat(elementPointer + 8L, z);
        MemoryUtil.memPutFloat(elementPointer + 12L, w);
    }

    @Override
    public RenderPipeline pipeline() {
        return SMOOTH_TEXT_PIPELINE_FANCYMENU;
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
