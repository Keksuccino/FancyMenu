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
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class SmoothRectangleRenderer {

    private static final float QUAD_AA_PADDING_PIXELS_FANCYMENU = 2.0F;
    private static final Matrix3x2f IDENTITY_POSE_FANCYMENU = new Matrix3x2f();
    private static final VertexFormatElement RECT_INFO_0_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormatElement RECT_INFO_1_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormatElement RECT_INFO_2_FANCYMENU = registerNextVertexFormatElement_FancyMenu();
    private static final VertexFormat SMOOTH_RECT_VERTEX_FORMAT_FANCYMENU = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("RectInfo0", RECT_INFO_0_FANCYMENU)
            .add("RectInfo1", RECT_INFO_1_FANCYMENU)
            .add("RectInfo2", RECT_INFO_2_FANCYMENU)
            .build();
    private static final RenderPipeline SMOOTH_RECT_PIPELINE_FANCYMENU = RenderPipeline.builder()
            .withLocation(Identifier.withDefaultNamespace("pipeline/fancymenu_gui_smooth_rect"))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/fancymenu_gui_smooth_rect")
            .withFragmentShader("core/fancymenu_gui_smooth_rect")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .withVertexFormat(SMOOTH_RECT_VERTEX_FORMAT_FANCYMENU, VertexFormat.Mode.QUADS)
            .build();

    private SmoothRectangleRenderer() {
    }

    private static VertexFormatElement registerNextVertexFormatElement_FancyMenu() {
        for (int i = 0; i < VertexFormatElement.MAX_COUNT; i++) {
            if (VertexFormatElement.byId(i) == null) {
                return VertexFormatElement.register(i, 0, VertexFormatElement.Type.FLOAT, false, 4);
            }
        }
        throw new IllegalStateException("VertexFormatElement count limit exceeded");
    }

    public static void renderSmoothRect(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.uniform(cornerRadius), color);
    }

    public static void renderSmoothRectScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothRect(graphics, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), cornerRadius * transform.scale(), color, partial);
    }

    public static void renderSmoothRectRoundTopCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.topOnly(cornerRadius), color);
    }

    public static void renderSmoothRectRoundTopCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothRectRoundTopCorners(graphics, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), cornerRadius * transform.scale(), color, partial);
    }

    public static void renderSmoothRectRoundBottomCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.bottomOnly(cornerRadius), color);
    }

    public static void renderSmoothRectRoundBottomCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothRectRoundBottomCorners(graphics, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), cornerRadius * transform.scale(), color, partial);
    }

    public static void renderSmoothRectRoundAllCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), color);
    }

    public static void renderSmoothRectRoundAllCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothRectRoundAllCorners(
                graphics,
                transform.transformX(x),
                transform.transformY(y),
                width * transform.scale(),
                height * transform.scale(),
                topLeftRadius * transform.scale(),
                topRightRadius * transform.scale(),
                bottomRightRadius * transform.scale(),
                bottomLeftRadius * transform.scale(),
                color,
                partial
        );
    }

    public static void renderSmoothBorder(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, borderThickness, CornerRadii.uniform(cornerRadius), color);
    }

    public static void renderSmoothBorderScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothBorder(
                graphics,
                transform.transformX(x),
                transform.transformY(y),
                width * transform.scale(),
                height * transform.scale(),
                borderThickness * transform.scale(),
                cornerRadius * transform.scale(),
                color,
                partial
        );
    }

    public static void renderSmoothBorderRoundAllCorners(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, borderThickness, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), color);
    }

    public static void renderSmoothBorderRoundAllCornersScaled(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothBorderRoundAllCorners(
                graphics,
                transform.transformX(x),
                transform.transformY(y),
                width * transform.scale(),
                height * transform.scale(),
                borderThickness * transform.scale(),
                topLeftRadius * transform.scale(),
                topRightRadius * transform.scale(),
                bottomRightRadius * transform.scale(),
                bottomLeftRadius * transform.scale(),
                color,
                partial
        );
    }

    private static void renderSmoothRectInternal(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float borderThickness, @Nonnull CornerRadii cornerRadii, int color) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(cornerRadii);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        _renderSmoothRect(graphics, new RectArea(x, y, width, height, Math.max(0.0F, borderThickness), cornerRadii, color));
    }

    private static void _renderSmoothRect(@Nonnull GuiGraphicsExtractor graphics, @Nonnull RectArea area) {
        float guiScale = resolveGuiScale_FancyMenu();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            renderFallbackRect_FancyMenu(graphics, area);
            return;
        }

        float scaledBorderThickness = area.borderThickness * guiScale;
        CornerRadii scaledRadii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F).flipVertical();
        RenderRotationUtil.Rotation2D rotation = GuiPoseTransformUtil.resolve(graphics).rotation();
        QuadBounds bounds = computeQuadBounds_FancyMenu(area, guiScale, scaledWidth, scaledHeight, rotation);
        submitSmoothRect_FancyMenu(graphics, area, bounds, guiScale, scaledWidth * 0.5F, scaledHeight * 0.5F, scaledBorderThickness, scaledRadii, rotation);
        RenderingUtils.resetShaderColor(graphics);
    }

    private static void renderFallbackRect_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, @Nonnull RectArea area) {
        float guiScale = resolveGuiScale_FancyMenu();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        float scaledX = area.x * guiScale;
        float scaledY = area.y * guiScale;
        CornerRadii radii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F);
        if (area.borderThickness > 0.0F) {
            float border = Math.min(area.borderThickness * guiScale, Math.min(scaledWidth * 0.5F, scaledHeight * 0.5F));
            if (border <= 0.0F) {
                return;
            }
            renderRoundedBorderFallback_FancyMenu(graphics, scaledX, scaledY, scaledWidth, scaledHeight, border, radii, area.color, guiScale);
            return;
        }
        renderRoundedRectFallback_FancyMenu(graphics, scaledX, scaledY, scaledWidth, scaledHeight, radii, area.color, guiScale);
    }

    private static void renderRoundedBorderFallback_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, float border, @Nonnull CornerRadii radii, int color, float guiScale) {
        float innerX = x + border;
        float innerY = y + border;
        float innerWidth = width - border * 2.0F;
        float innerHeight = height - border * 2.0F;
        if (innerWidth <= 0.0F || innerHeight <= 0.0F) {
            renderRoundedRectFallback_FancyMenu(graphics, x, y, width, height, radii, color, guiScale);
            return;
        }

        CornerRadii innerRadii = radii.shrunk(border).clamped(Math.min(innerWidth, innerHeight) * 0.5F);
        forEachRoundedSpan_FancyMenu(x, y, width, height, radii, outerSpan -> {
            float rowTop = outerSpan.y();
            float rowBottom = outerSpan.y() + outerSpan.height();
            if (rowBottom <= innerY || rowTop >= innerY + innerHeight) {
                fillPixelSpan_FancyMenu(graphics, outerSpan.left(), rowTop, outerSpan.right(), rowBottom, color, guiScale);
                return;
            }

            Span innerSpan = spanForY_FancyMenu(innerX, innerY, innerWidth, innerHeight, innerRadii, outerSpan.centerY());
            if (innerSpan == null) {
                fillPixelSpan_FancyMenu(graphics, outerSpan.left(), rowTop, outerSpan.right(), rowBottom, color, guiScale);
                return;
            }
            float leftEnd = Math.min(innerSpan.left(), outerSpan.right());
            if (leftEnd > outerSpan.left()) {
                fillPixelSpan_FancyMenu(graphics, outerSpan.left(), rowTop, leftEnd, rowBottom, color, guiScale);
            }
            float rightStart = Math.max(innerSpan.right(), outerSpan.left());
            if (outerSpan.right() > rightStart) {
                fillPixelSpan_FancyMenu(graphics, rightStart, rowTop, outerSpan.right(), rowBottom, color, guiScale);
            }
        });
    }

    private static void renderRoundedRectFallback_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, float x, float y, float width, float height, @Nonnull CornerRadii radii, int color, float guiScale) {
        if (!radii.hasRoundedCorners()) {
            fillPixelRect_FancyMenu(graphics, x, y, x + width, y + height, color, guiScale);
            return;
        }
        forEachRoundedSpan_FancyMenu(x, y, width, height, radii, span -> fillPixelSpan_FancyMenu(graphics, span.left(), span.y(), span.right(), span.y() + span.height(), color, guiScale));
    }

    private static void forEachRoundedSpan_FancyMenu(float x, float y, float width, float height, @Nonnull CornerRadii radii, @Nonnull SpanConsumer consumer) {
        int firstRow = (int)Math.floor(y);
        int lastRow = (int)Math.ceil(y + height);
        for (int row = firstRow; row < lastRow; row++) {
            float rowY = Math.max(y, row);
            float rowBottom = Math.min(y + height, row + 1.0F);
            float rowHeight = rowBottom - rowY;
            if (rowHeight <= 0.0F) {
                continue;
            }
            Span span = spanForY_FancyMenu(x, y, width, height, radii, rowY + rowHeight * 0.5F);
            if (span != null) {
                consumer.accept(new Span(rowY, rowHeight, span.left(), span.right()));
            }
        }
    }

    private static Span spanForY_FancyMenu(float x, float y, float width, float height, @Nonnull CornerRadii radii, float centerY) {
        if (centerY < y || centerY > y + height) {
            return null;
        }
        float topDistance = centerY - y;
        float bottomDistance = y + height - centerY;
        float leftInset = Math.max(cornerInset_FancyMenu(radii.topLeft, topDistance), cornerInset_FancyMenu(radii.bottomLeft, bottomDistance));
        float rightInset = Math.max(cornerInset_FancyMenu(radii.topRight, topDistance), cornerInset_FancyMenu(radii.bottomRight, bottomDistance));
        float left = x + leftInset;
        float right = x + width - rightInset;
        return right > left ? new Span(centerY, 0.0F, left, right) : null;
    }

    private static float resolveGuiScale_FancyMenu() {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D) {
            return 1.0F;
        }
        return (float)guiScale;
    }

    private static QuadBounds computeQuadBounds_FancyMenu(@Nonnull RectArea area, float guiScale, float scaledWidth, float scaledHeight, @Nonnull RenderRotationUtil.Rotation2D maskRotation) {
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

    private static void submitSmoothRect_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, @Nonnull RectArea area, @Nonnull QuadBounds bounds, float guiScale, float halfWidth, float halfHeight, float borderThickness, @Nonnull CornerRadii cornerRadii, @Nonnull RenderRotationUtil.Rotation2D rotation) {
        ((IMixinGuiGraphicsExtractor)graphics).get_guiRenderState_FancyMenu().addGuiElement(new SmoothRectRenderState(
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
                cornerRadii,
                rotation,
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

    private static void fillPixelSpan_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, float left, float top, float right, float bottom, int color, float guiScale) {
        if (right <= left || bottom <= top) {
            return;
        }

        float clampedLeft = Math.max(left, (float)Math.floor(left));
        float clampedRight = Math.min(right, (float)Math.ceil(right));
        if (clampedRight <= clampedLeft) {
            return;
        }

        int leftPixel = (int)Math.floor(clampedLeft);
        int rightPixel = (int)Math.ceil(clampedRight);
        if (rightPixel - leftPixel <= 1) {
            float coverage = Math.min(1.0F, Math.max(0.0F, clampedRight - clampedLeft));
            fillPixelRect_FancyMenu(graphics, leftPixel, top, leftPixel + 1.0F, bottom, withAlphaFactor_FancyMenu(color, coverage), guiScale);
            return;
        }

        int solidLeft = (int)Math.ceil(clampedLeft);
        int solidRight = (int)Math.floor(clampedRight);

        float leftCoverage = solidLeft - clampedLeft;
        if (leftCoverage > 0.0F) {
            fillPixelRect_FancyMenu(graphics, leftPixel, top, solidLeft, bottom, withAlphaFactor_FancyMenu(color, leftCoverage), guiScale);
        }

        if (solidRight > solidLeft) {
            fillPixelRect_FancyMenu(graphics, solidLeft, top, solidRight, bottom, color, guiScale);
        }

        float rightCoverage = clampedRight - solidRight;
        if (rightCoverage > 0.0F) {
            fillPixelRect_FancyMenu(graphics, solidRight, top, rightPixel, bottom, withAlphaFactor_FancyMenu(color, rightCoverage), guiScale);
        }
    }

    private static void fillPixelRect_FancyMenu(@Nonnull GuiGraphicsExtractor graphics, float left, float top, float right, float bottom, int color, float guiScale) {
        if (right <= left || bottom <= top || ARGB.alpha(color) <= 0) {
            return;
        }
        RenderingUtils.fillF(graphics, left / guiScale, top / guiScale, right / guiScale, bottom / guiScale, color);
    }

    private static int withAlphaFactor_FancyMenu(int color, float factor) {
        float clampedFactor = Math.max(0.0F, Math.min(1.0F, factor));
        int alpha = Math.round(ARGB.alpha(color) * clampedFactor);
        if (alpha <= 0) {
            return color & 0x00FFFFFF;
        }
        return ARGB.color(Math.min(255, alpha), ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }

    private static float cornerInset_FancyMenu(float radius, float distanceFromEdge) {
        if (radius <= 0.0F || distanceFromEdge >= radius) {
            return 0.0F;
        }
        float dy = radius - Math.max(0.0F, distanceFromEdge);
        return radius - (float)Math.sqrt(Math.max(0.0F, radius * radius - dy * dy));
    }

    private record RectArea(float x, float y, float width, float height, float borderThickness, CornerRadii cornerRadii, int color) {
    }

    private record QuadBounds(float minX, float minY, float maxX, float maxY) {
    }

    private record SmoothRectRenderState(
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
            CornerRadii cornerRadii,
            RenderRotationUtil.Rotation2D rotation,
            int color,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        private SmoothRectRenderState(
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
                CornerRadii cornerRadii,
                RenderRotationUtil.Rotation2D rotation,
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
                    cornerRadii,
                    rotation,
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
            writeVec4_FancyMenu(consumer, RECT_INFO_0_FANCYMENU, this.halfWidth, this.halfHeight, this.borderThickness, 0.0F);
            writeVec4_FancyMenu(consumer, RECT_INFO_1_FANCYMENU, this.cornerRadii.topLeft(), this.cornerRadii.topRight(), this.cornerRadii.bottomRight(), this.cornerRadii.bottomLeft());
            writeVec4_FancyMenu(consumer, RECT_INFO_2_FANCYMENU, this.rotation.m00(), this.rotation.m01(), this.rotation.m10(), this.rotation.m11());
        }

        @Override
        public RenderPipeline pipeline() {
            return SMOOTH_RECT_PIPELINE_FANCYMENU;
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

    private record Span(float y, float height, float left, float right) {

        private float centerY() {
            return y + height * 0.5F;
        }

    }

    @FunctionalInterface
    private interface SpanConsumer {

        void accept(@Nonnull Span span);

    }

    private record CornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {

        private static CornerRadii uniform(float radius) {
            return new CornerRadii(radius, radius, radius, radius);
        }

        private static CornerRadii topOnly(float radius) {
            return new CornerRadii(radius, radius, 0.0F, 0.0F);
        }

        private static CornerRadii bottomOnly(float radius) {
            return new CornerRadii(0.0F, 0.0F, radius, radius);
        }

        private static CornerRadii of(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            return new CornerRadii(topLeft, topRight, bottomRight, bottomLeft);
        }

        private CornerRadii scaled(float factor) {
            return new CornerRadii(topLeft * factor, topRight * factor, bottomRight * factor, bottomLeft * factor);
        }

        private CornerRadii clamped(float maxRadius) {
            float clampedMax = Math.max(0.0F, maxRadius);
            return new CornerRadii(clampCorner(topLeft, clampedMax), clampCorner(topRight, clampedMax), clampCorner(bottomRight, clampedMax), clampCorner(bottomLeft, clampedMax));
        }

        private CornerRadii shrunk(float amount) {
            float shrink = Math.max(0.0F, amount);
            return new CornerRadii(
                    Math.max(0.0F, topLeft - shrink),
                    Math.max(0.0F, topRight - shrink),
                    Math.max(0.0F, bottomRight - shrink),
                    Math.max(0.0F, bottomLeft - shrink)
            );
        }

        private boolean hasRoundedCorners() {
            return topLeft > 0.0F || topRight > 0.0F || bottomRight > 0.0F || bottomLeft > 0.0F;
        }

        private CornerRadii flipVertical() {
            return new CornerRadii(bottomLeft, bottomRight, topRight, topLeft);
        }

        private static float clampCorner(float value, float max) {
            if (value <= 0.0F) {
                return 0.0F;
            }
            return value > max ? max : value;
        }
    }

}
