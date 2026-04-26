package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBufferBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class SmoothImageCircleRenderer {

    private static final float QUAD_AA_PADDING_PIXELS_FANCYMENU = 2.0F;
    private static final Matrix3x2f IDENTITY_POSE_FANCYMENU = new Matrix3x2f();
    private static final VertexFormatElement IMAGE_CIRCLE_INFO_0_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormatElement IMAGE_CIRCLE_INFO_1_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormatElement IMAGE_CIRCLE_INFO_2_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormat SMOOTH_IMAGE_CIRCLE_VERTEX_FORMAT_FANCYMENU = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("ImageCircleInfo0", IMAGE_CIRCLE_INFO_0_FANCYMENU)
            .add("ImageCircleInfo1", IMAGE_CIRCLE_INFO_1_FANCYMENU)
            .add("ImageCircleInfo2", IMAGE_CIRCLE_INFO_2_FANCYMENU)
            .build();
    private static final RenderPipeline SMOOTH_IMAGE_CIRCLE_PIPELINE_FANCYMENU = RenderPipeline.builder()
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_gui_smooth_image_circle"))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withVertexShader("core/fancymenu_gui_smooth_image_circle")
            .withFragmentShader("core/fancymenu_gui_smooth_image_circle")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(SMOOTH_IMAGE_CIRCLE_VERTEX_FORMAT_FANCYMENU, VertexFormat.Mode.QUADS)
            .build();

    private SmoothImageCircleRenderer() {
    }

    private static VertexFormatElement registerNextVertexFormatElement_FancyMenu() {
        for (int i = 0; i < VertexFormatElement.MAX_COUNT; i++) {
            if (VertexFormatElement.byId(i) == null) {
                return VertexFormatElement.register(i, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
            }
        }
        throw new IllegalStateException("VertexFormatElement count limit exceeded");
    }

    /**
     * Renders a smooth superellipse image using the provided bounding rectangle.
     *
     * <p>Width/height control the aspect ratio (circle vs oval), while {@code roundness} controls the shape exponent
     * (how boxy or pointy the outline is). If you only need true circles/ovals, keep {@code roundness = 2.0}.
     * Values higher than {@code 2.0} make the shape boxier (approaching a rounded rectangle), while values lower
     * than {@code 2.0} make it pointier (approaching a diamond).
     *
     * <p>Roundness examples:
     * <br>- Perfect circle/ellipse: {@code roundness = 2.0}
     * <br>- Squircle-like (boxier): {@code roundness = 4.0}
     * <br>- Diamond-like (pointier): {@code roundness = 1.0}
     *
     * <p>Shape examples:
     * <br>- Perfect circle: width == height and {@code roundness = 2.0}
     * <br>- Oval: width != height and {@code roundness = 2.0}
     */
    public static void renderSmoothImageCircle(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float roundness, int color, float partial) {
        renderSmoothImageCircleInternal(graphics, texture, x, y, width, height, roundness, TextureRegion.full(), color);
    }

    public static void renderSmoothImageCircleScaled(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float roundness, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothImageCircle(graphics, texture, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), roundness, color, partial);
    }

    public static void renderSmoothImageCircle(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight, float roundness, int color, float partial) {
        renderSmoothImageCircleInternal(graphics, texture, x, y, width, height, roundness, TextureRegion.of(uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight), color);
    }

    public static void renderSmoothImageCircleScaled(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight, float roundness, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothImageCircle(graphics, texture, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight, roundness, color, partial);
    }

    private static void renderSmoothImageCircleInternal(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float roundness, @Nonnull TextureRegion textureRegion, int color) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(texture);
        Objects.requireNonNull(textureRegion);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float clampedRoundness = Math.max(0.1F, roundness);
        _renderSmoothImageCircle(graphics, new CircleArea(x, y, width, height, clampedRoundness, textureRegion, texture, color));
    }

    private static void _renderSmoothImageCircle(@Nonnull GuiGraphics graphics, @Nonnull CircleArea area) {
        float guiScale = resolveGuiScale_FancyMenu();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        RenderRotationUtil.Rotation2D rotation = GuiPoseTransformUtil.resolve(graphics).rotation();
        QuadBounds bounds = computeQuadBounds_FancyMenu(area, guiScale, scaledWidth, scaledHeight, rotation);
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(area.texture);
        submitSmoothImageCircle_FancyMenu(
                graphics,
                area,
                texture,
                bounds,
                guiScale,
                scaledWidth * 0.5F,
                scaledHeight * 0.5F,
                area.roundness,
                rotation
        );
        RenderingUtils.resetShaderColor(graphics);
    }

    private static float resolveGuiScale_FancyMenu() {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D) {
            return 1.0F;
        }
        return (float)guiScale;
    }

    private static QuadBounds computeQuadBounds_FancyMenu(@Nonnull CircleArea area, float guiScale, float scaledWidth, float scaledHeight, @Nonnull RenderRotationUtil.Rotation2D maskRotation) {
        float halfWidth = scaledWidth * 0.5F;
        float halfHeight = scaledHeight * 0.5F;
        RenderRotationUtil.Rotation2D forwardRotation = invertRotation_FancyMenu(maskRotation);
        float extentX = Math.abs(forwardRotation.m00()) * halfWidth + Math.abs(forwardRotation.m01()) * halfHeight;
        float extentY = Math.abs(forwardRotation.m10()) * halfWidth + Math.abs(forwardRotation.m11()) * halfHeight;

        if (!Float.isFinite(extentX) || !Float.isFinite(extentY)) {
            return new QuadBounds(area.x, area.y, area.x + area.width, area.y + area.height);
        }

        float centerX = area.x + area.width * 0.5F;
        float centerY = area.y + area.height * 0.5F;
        float extentXGui = (extentX + QUAD_AA_PADDING_PIXELS_FANCYMENU) / guiScale;
        float extentYGui = (extentY + QUAD_AA_PADDING_PIXELS_FANCYMENU) / guiScale;
        return new QuadBounds(centerX - extentXGui, centerY - extentYGui, centerX + extentXGui, centerY + extentYGui);
    }

    private static RenderRotationUtil.Rotation2D invertRotation_FancyMenu(@Nonnull RenderRotationUtil.Rotation2D rotation) {
        float det = rotation.m00() * rotation.m11() - rotation.m01() * rotation.m10();
        if (!Float.isFinite(det) || Math.abs(det) < 1.0E-6F) {
            return RenderRotationUtil.Rotation2D.identity();
        }
        float invDet = 1.0F / det;
        return new RenderRotationUtil.Rotation2D(
                rotation.m11() * invDet,
                -rotation.m01() * invDet,
                -rotation.m10() * invDet,
                rotation.m00() * invDet
        );
    }

    private static void submitSmoothImageCircle_FancyMenu(@Nonnull GuiGraphics graphics, @Nonnull CircleArea area, @Nonnull AbstractTexture texture, @Nonnull QuadBounds bounds, float guiScale, float halfWidth, float halfHeight, float roundness, @Nonnull RenderRotationUtil.Rotation2D rotation) {
        ((IMixinGuiGraphics)graphics).get_guiRenderState_FancyMenu().submitGuiElement(new SmoothImageCircleRenderState(
                new Matrix3x2f(IDENTITY_POSE_FANCYMENU),
                texture,
                bounds.minX(),
                bounds.minY(),
                bounds.maxX(),
                bounds.maxY(),
                area.x + area.width * 0.5F,
                area.y + area.height * 0.5F,
                guiScale,
                halfWidth,
                halfHeight,
                roundness,
                rotation,
                area.textureRegion,
                area.color,
                GuiScissorUtil.getActiveScissor(graphics)
        ));
    }

    private static void writeVec4_FancyMenu(@Nonnull VertexConsumer consumer, @Nonnull VertexFormatElement element, float x, float y, float z, float w) {
        long pointer = ((IMixinBufferBuilder)consumer).invoke_beginElement_FancyMenu(element);
        if (pointer == -1L) {
            return;
        }
        MemoryUtil.memPutFloat(pointer, x);
        MemoryUtil.memPutFloat(pointer + 4L, y);
        MemoryUtil.memPutFloat(pointer + 8L, z);
        MemoryUtil.memPutFloat(pointer + 12L, w);
    }

    private record CircleArea(float x, float y, float width, float height, float roundness, TextureRegion textureRegion, Identifier texture, int color) {
    }

    private record QuadBounds(float minX, float minY, float maxX, float maxY) {
    }

    private record SmoothImageCircleRenderState(
            Matrix3x2f transform,
            AbstractTexture texture,
            float minX,
            float minY,
            float maxX,
            float maxY,
            float centerX,
            float centerY,
            float guiScale,
            float halfWidth,
            float halfHeight,
            float roundness,
            RenderRotationUtil.Rotation2D rotation,
            TextureRegion textureRegion,
            int color,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        private SmoothImageCircleRenderState(
                Matrix3x2f transform,
                AbstractTexture texture,
                float minX,
                float minY,
                float maxX,
                float maxY,
                float centerX,
                float centerY,
                float guiScale,
                float halfWidth,
                float halfHeight,
                float roundness,
                RenderRotationUtil.Rotation2D rotation,
                TextureRegion textureRegion,
                int color,
                @Nullable ScreenRectangle scissorArea
        ) {
            this(
                    transform,
                    texture,
                    minX,
                    minY,
                    maxX,
                    maxY,
                    centerX,
                    centerY,
                    guiScale,
                    halfWidth,
                    halfHeight,
                    roundness,
                    rotation,
                    textureRegion,
                    color,
                    scissorArea,
                    getBounds_FancyMenu(minX, minY, maxX, maxY, transform, scissorArea)
            );
        }

        @Override
        public void buildVertices(@Nonnull VertexConsumer consumer) {
            this.addVertex_FancyMenu(consumer, this.minX, this.minY);
            this.addVertex_FancyMenu(consumer, this.minX, this.maxY);
            this.addVertex_FancyMenu(consumer, this.maxX, this.maxY);
            this.addVertex_FancyMenu(consumer, this.maxX, this.minY);
        }

        private void addVertex_FancyMenu(@Nonnull VertexConsumer consumer, float x, float y) {
            consumer.addVertexWith2DPose(this.transform, x, y)
                    .setColor(this.color)
                    .setUv((x - this.centerX) * this.guiScale, (this.centerY - y) * this.guiScale);
            writeVec4_FancyMenu(consumer, IMAGE_CIRCLE_INFO_0_FANCYMENU, this.halfWidth, this.halfHeight, this.roundness, 0.0F);
            writeVec4_FancyMenu(consumer, IMAGE_CIRCLE_INFO_1_FANCYMENU, this.rotation.m00(), this.rotation.m01(), this.rotation.m10(), this.rotation.m11());
            writeVec4_FancyMenu(consumer, IMAGE_CIRCLE_INFO_2_FANCYMENU, this.textureRegion.minU(), this.textureRegion.minV(), this.textureRegion.maxU(), this.textureRegion.maxV());
        }

        @Override
        public RenderPipeline pipeline() {
            return SMOOTH_IMAGE_CIRCLE_PIPELINE_FANCYMENU;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.singleTexture(this.texture.getTextureView(), this.texture.getSampler());
        }

        @Nullable
        private static ScreenRectangle getBounds_FancyMenu(float minX, float minY, float maxX, float maxY, Matrix3x2f transform, @Nullable ScreenRectangle scissorArea) {
            int x = (int)Math.floor(Math.min(minX, maxX));
            int y = (int)Math.floor(Math.min(minY, maxY));
            int right = (int)Math.ceil(Math.max(minX, maxX));
            int bottom = (int)Math.ceil(Math.max(minY, maxY));
            int width = Math.max(1, right - x);
            int height = Math.max(1, bottom - y);
            ScreenRectangle rectangle = new ScreenRectangle(x, y, width, height).transformMaxBounds(transform);
            return scissorArea != null ? scissorArea.intersection(rectangle) : rectangle;
        }

    }

    private record TextureRegion(float minU, float minV, float maxU, float maxV) {

        private static TextureRegion full() {
            return new TextureRegion(0.0F, 0.0F, 1.0F, 1.0F);
        }

        private static TextureRegion of(float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
            float safeTexWidth = textureWidth == 0.0F ? 1.0F : textureWidth;
            float safeTexHeight = textureHeight == 0.0F ? 1.0F : textureHeight;
            float minU = uOffset / safeTexWidth;
            float minV = vOffset / safeTexHeight;
            float maxU = (uOffset + uWidth) / safeTexWidth;
            float maxV = (vOffset + vHeight) / safeTexHeight;
            return new TextureRegion(minU, minV, maxU, maxV);
        }
    }
}
