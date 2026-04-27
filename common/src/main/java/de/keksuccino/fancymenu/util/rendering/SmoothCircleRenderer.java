package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Optional;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBufferBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class SmoothCircleRenderer {

    private static final float QUAD_AA_PADDING_PIXELS_FANCYMENU = 2.0F;
    private static final Matrix3x2f IDENTITY_POSE_FANCYMENU = new Matrix3x2f();
    private static final VertexFormatElement CIRCLE_INFO_0_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormatElement CIRCLE_INFO_1_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormatElement CIRCLE_INFO_2_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormat SMOOTH_CIRCLE_VERTEX_FORMAT_FANCYMENU = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("CircleInfo0", CIRCLE_INFO_0_FANCYMENU)
            .add("CircleInfo1", CIRCLE_INFO_1_FANCYMENU)
            .add("CircleInfo2", CIRCLE_INFO_2_FANCYMENU)
            .build();
    private static final RenderPipeline SMOOTH_CIRCLE_PIPELINE_FANCYMENU = RenderPipeline.builder()
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_gui_smooth_circle"))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/fancymenu_gui_smooth_circle")
            .withFragmentShader("core/fancymenu_gui_smooth_circle")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .withVertexFormat(SMOOTH_CIRCLE_VERTEX_FORMAT_FANCYMENU, VertexFormat.Mode.QUADS)
            .build();
    private static final float SHAPE_MODE_CIRCLE_FANCYMENU = 0.0F;
    private static final float SHAPE_MODE_ARC_FANCYMENU = 2.0F;

    private SmoothCircleRenderer() {
    }

    private static VertexFormatElement registerNextVertexFormatElement_FancyMenu() {
        for (int i = 0; i < VertexFormatElement.MAX_COUNT; i++) {
            if (VertexFormatElement.byId(i) == null) {
                return VertexFormatElement.register(i, 0, VertexFormatElement.Type.FLOAT, false, 4);
            }
        }
        throw new IllegalStateException("VertexFormatElement count limit exceeded");
    }

    /**
     * Renders a smooth superellipse using the provided bounding rectangle.
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
    public static void renderSmoothCircle(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float roundness, int color, float partial) {
        renderSmoothCircleInternal(graphics, x, y, width, height, 0.0F, roundness, color, partial);
    }

    public static void renderSmoothCircleScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float roundness, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothCircle(graphics, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), roundness, color, partial);
    }

    /**
     * Renders a smooth superellipse border using the provided bounding rectangle.
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
    public static void renderSmoothCircleBorder(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float roundness, int color, float partial) {
        renderSmoothCircleInternal(graphics, x, y, width, height, borderThickness, roundness, color, partial);
    }

    public static void renderSmoothCircleBorderScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float roundness, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothCircleBorder(graphics, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), borderThickness * transform.scale(), roundness, color, partial);
    }

    /**
     * Renders a smooth superellipse border arc using the provided bounding rectangle.
     *
     * <p>The arc is centered around the rectangle's center. The line is the border itself:
     * the thickness expands inward from the superellipse outline (matching {@link #renderSmoothCircleBorder}).
     * Start/end angles are in radians where 0 points to the right, and angles increase clockwise
     * (because GUI Y increases downward).
     *
     * <p>If {@code roundInside} is false, the provided rectangle is treated as the inner edge,
     * and the border is expanded outward by {@code borderThickness}.
     *
     * <p>If {@code borderThickness <= 0}, the method renders a filled arc wedge instead of just the border.
     */
    public static void renderSmoothCircleBorderArc(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float roundness, float startAngleRadians, float endAngleRadians, boolean roundInside, int color, float partial) {
        renderSmoothCircleArcInternal(graphics, x, y, width, height, borderThickness, roundness, startAngleRadians, endAngleRadians, roundInside, color, partial);
    }

    /**
     * Renders a smooth superellipse border arc using the provided bounding rectangle with additional render scaling.
     *
     * <p>The arc is centered around the rectangle's center. The line is the border itself:
     * the thickness expands inward from the superellipse outline (matching {@link #renderSmoothCircleBorder}).
     * Start/end angles are in radians where 0 points to the right, and angles increase clockwise
     * (because GUI Y increases downward).
     *
     * <p>If {@code roundInside} is false, the provided rectangle is treated as the inner edge,
     * and the border is expanded outward by {@code borderThickness}.
     */
    public static void renderSmoothCircleBorderArcScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float roundness, float startAngleRadians, float endAngleRadians, boolean roundInside, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothCircleBorderArc(
                graphics,
                transform.transformX(x),
                transform.transformY(y),
                width * transform.scale(),
                height * transform.scale(),
                borderThickness * transform.scale(),
                roundness,
                startAngleRadians,
                endAngleRadians,
                roundInside,
                color,
                partial
        );
    }

    private static void renderSmoothCircleInternal(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float roundness, int color, float partial) {
        Objects.requireNonNull(graphics);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float clampedRoundness = Math.max(0.1F, roundness);
        _renderSmoothCircle(graphics, partial, new CircleArea(x, y, width, height, Math.max(0.0F, borderThickness), clampedRoundness, color));
    }

    private static void renderSmoothCircleArcInternal(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float roundness, float startAngleRadians, float endAngleRadians, boolean roundInside, int color, float partial) {
        Objects.requireNonNull(graphics);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float clampedRoundness = Math.max(0.1F, roundness);
        float clampedBorder = Math.max(0.0F, borderThickness);
        float adjustedX = x;
        float adjustedY = y;
        float adjustedWidth = width;
        float adjustedHeight = height;
        if (!roundInside && clampedBorder > 0.0F) {
            adjustedX -= clampedBorder;
            adjustedY -= clampedBorder;
            adjustedWidth += clampedBorder * 2.0F;
            adjustedHeight += clampedBorder * 2.0F;
        }
        _renderSmoothCircleArc(graphics, partial, new ArcArea(adjustedX, adjustedY, adjustedWidth, adjustedHeight, clampedBorder, clampedRoundness, startAngleRadians, endAngleRadians, color));
    }

    private static void _renderSmoothCircle(GuiGraphicsExtractor graphics, float partial, CircleArea area) {
        float guiScale = resolveGuiScale_FancyMenu();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        float scaledBorderThickness = area.borderThickness * guiScale;
        float scaledRoundness = area.roundness;

        RenderRotationUtil.Rotation2D rotation = GuiPoseTransformUtil.resolve(graphics).rotation();
        QuadBounds bounds = computeQuadBounds_FancyMenu(area, guiScale, scaledWidth, scaledHeight, rotation);
        submitSmoothCircle_FancyMenu(
                graphics,
                area,
                bounds,
                guiScale,
                scaledWidth * 0.5F,
                scaledHeight * 0.5F,
                scaledBorderThickness,
                scaledRoundness,
                rotation,
                SHAPE_MODE_CIRCLE_FANCYMENU,
                0.0F,
                0.0F
        );
        RenderingUtils.resetShaderColor(graphics);
    }

    private static void _renderSmoothCircleArc(GuiGraphicsExtractor graphics, float partial, ArcArea area) {
        float guiScale = resolveGuiScale_FancyMenu();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        float scaledBorderThickness = area.borderThickness * guiScale;
        float scaledRoundness = area.roundness;

        RenderRotationUtil.Rotation2D rotation = GuiPoseTransformUtil.resolve(graphics).rotation();
        QuadBounds bounds = computeQuadBounds_FancyMenu(area, guiScale, scaledWidth, scaledHeight, rotation);
        submitSmoothCircle_FancyMenu(
                graphics,
                area,
                bounds,
                guiScale,
                scaledWidth * 0.5F,
                scaledHeight * 0.5F,
                scaledBorderThickness,
                scaledRoundness,
                rotation,
                SHAPE_MODE_ARC_FANCYMENU,
                area.startAngleRadians,
                area.endAngleRadians
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

    private static QuadBounds computeQuadBounds_FancyMenu(@Nonnull ArcArea area, float guiScale, float scaledWidth, float scaledHeight, @Nonnull RenderRotationUtil.Rotation2D maskRotation) {
        return computeQuadBounds_FancyMenu(new CircleArea(area.x, area.y, area.width, area.height, area.borderThickness, area.roundness, area.color), guiScale, scaledWidth, scaledHeight, maskRotation);
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

    private static void submitSmoothCircle_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, @Nonnull CircleArea area, @Nonnull QuadBounds bounds, float guiScale, float halfWidth, float halfHeight, float borderThickness, float roundness, @Nonnull RenderRotationUtil.Rotation2D rotation, float shapeMode, float arcStartRadians, float arcEndRadians) {
        ((IMixinGuiGraphicsExtractor)graphics).get_guiRenderState_FancyMenu().addGuiElement(new SmoothCircleRenderState(
                new Matrix3x2f(IDENTITY_POSE_FANCYMENU),
                bounds.minX(),
                bounds.minY(),
                bounds.maxX(),
                bounds.maxY(),
                area.x + area.width * 0.5F,
                area.y + area.height * 0.5F,
                guiScale,
                halfWidth,
                halfHeight,
                borderThickness,
                roundness,
                rotation,
                shapeMode,
                arcStartRadians,
                arcEndRadians,
                area.color,
                GuiScissorUtil.getActiveScissor(graphics)
        ));
    }

    private static void submitSmoothCircle_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, @Nonnull ArcArea area, @Nonnull QuadBounds bounds, float guiScale, float halfWidth, float halfHeight, float borderThickness, float roundness, @Nonnull RenderRotationUtil.Rotation2D rotation, float shapeMode, float arcStartRadians, float arcEndRadians) {
        submitSmoothCircle_FancyMenu(graphics, new CircleArea(area.x, area.y, area.width, area.height, area.borderThickness, area.roundness, area.color), bounds, guiScale, halfWidth, halfHeight, borderThickness, roundness, rotation, shapeMode, arcStartRadians, arcEndRadians);
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

    private record CircleArea(float x, float y, float width, float height, float borderThickness, float roundness, int color) {
    }

    private record ArcArea(float x, float y, float width, float height, float borderThickness, float roundness, float startAngleRadians, float endAngleRadians, int color) {
    }

    private record QuadBounds(float minX, float minY, float maxX, float maxY) {
    }

    private record SmoothCircleRenderState(
            Matrix3x2f transform,
            float minX,
            float minY,
            float maxX,
            float maxY,
            float centerX,
            float centerY,
            float guiScale,
            float halfWidth,
            float halfHeight,
            float borderThickness,
            float roundness,
            RenderRotationUtil.Rotation2D rotation,
            float shapeMode,
            float arcStartRadians,
            float arcEndRadians,
            int color,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        private SmoothCircleRenderState(
                Matrix3x2f transform,
                float minX,
                float minY,
                float maxX,
                float maxY,
                float centerX,
                float centerY,
                float guiScale,
                float halfWidth,
                float halfHeight,
                float borderThickness,
                float roundness,
                RenderRotationUtil.Rotation2D rotation,
                float shapeMode,
                float arcStartRadians,
                float arcEndRadians,
                int color,
                @Nullable ScreenRectangle scissorArea
        ) {
            this(
                    transform,
                    minX,
                    minY,
                    maxX,
                    maxY,
                    centerX,
                    centerY,
                    guiScale,
                    halfWidth,
                    halfHeight,
                    borderThickness,
                    roundness,
                    rotation,
                    shapeMode,
                    arcStartRadians,
                    arcEndRadians,
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
            writeVec4_FancyMenu(consumer, CIRCLE_INFO_0_FANCYMENU, this.halfWidth, this.halfHeight, this.borderThickness, this.roundness);
            writeVec4_FancyMenu(consumer, CIRCLE_INFO_1_FANCYMENU, this.rotation.m00(), this.rotation.m01(), this.rotation.m10(), this.rotation.m11());
            writeVec4_FancyMenu(consumer, CIRCLE_INFO_2_FANCYMENU, this.shapeMode, this.arcStartRadians, this.arcEndRadians, 0.0F);
        }

        @Override
        public RenderPipeline pipeline() {
            return SMOOTH_CIRCLE_PIPELINE_FANCYMENU;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
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

}
