package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostChain;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SmoothRectangleRenderer {

    private static final Identifier SMOOTH_RECT_POST_CHAIN_FANCYMENU = Identifier.withDefaultNamespace("fancymenu_gui_smooth_rect");
    private static final int SMOOTH_RECT_CONFIG_SIZE_FANCYMENU = new Std140SizeCalculator()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .putFloat()
            .get();
    private static boolean smoothRectPostChainFailed_FancyMenu;

    private SmoothRectangleRenderer() {
    }

    public static void renderSmoothRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.uniform(cornerRadius), color);
    }

    public static void renderSmoothRectScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRect(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, cornerRadius * additionalScale, color, partial);
    }

    public static void renderSmoothRectRoundTopCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.topOnly(cornerRadius), color);
    }

    public static void renderSmoothRectRoundTopCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRectRoundTopCorners(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, cornerRadius * additionalScale, color, partial);
    }

    public static void renderSmoothRectRoundBottomCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.bottomOnly(cornerRadius), color);
    }

    public static void renderSmoothRectRoundBottomCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRectRoundBottomCorners(graphics, x * additionalScale + translationX, y * additionalScale + translationY, width * additionalScale, height * additionalScale, cornerRadius * additionalScale, color, partial);
    }

    public static void renderSmoothRectRoundAllCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, 0.0F, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), color);
    }

    public static void renderSmoothRectRoundAllCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothRectRoundAllCorners(
                graphics,
                x * additionalScale + translationX,
                y * additionalScale + translationY,
                width * additionalScale,
                height * additionalScale,
                topLeftRadius * additionalScale,
                topRightRadius * additionalScale,
                bottomRightRadius * additionalScale,
                bottomLeftRadius * additionalScale,
                color,
                partial
        );
    }

    public static void renderSmoothBorder(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float cornerRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, borderThickness, CornerRadii.uniform(cornerRadius), color);
    }

    public static void renderSmoothBorderScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float cornerRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothBorder(
                graphics,
                x * additionalScale + translationX,
                y * additionalScale + translationY,
                width * additionalScale,
                height * additionalScale,
                borderThickness * additionalScale,
                cornerRadius * additionalScale,
                color,
                partial
        );
    }

    public static void renderSmoothBorderRoundAllCorners(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        renderSmoothRectInternal(graphics, x, y, width, height, borderThickness, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), color);
    }

    public static void renderSmoothBorderRoundAllCornersScaled(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        float additionalScale = resolveAdditionalRenderScale();
        float translationX = resolveAdditionalRenderTranslationX();
        float translationY = resolveAdditionalRenderTranslationY();
        renderSmoothBorderRoundAllCorners(
                graphics,
                x * additionalScale + translationX,
                y * additionalScale + translationY,
                width * additionalScale,
                height * additionalScale,
                borderThickness * additionalScale,
                topLeftRadius * additionalScale,
                topRightRadius * additionalScale,
                bottomRightRadius * additionalScale,
                bottomLeftRadius * additionalScale,
                color,
                partial
        );
    }

    private static void renderSmoothRectInternal(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float borderThickness, @Nonnull CornerRadii cornerRadii, int color) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(cornerRadii);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        _renderSmoothRect(graphics, new RectArea(x, y, width, height, Math.max(0.0F, borderThickness), cornerRadii, color));
    }

    private static float resolveAdditionalRenderScale() {
        float scale = RenderScaleUtil.getCurrentAdditionalRenderScale();
        return Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
    }

    private static float resolveAdditionalRenderTranslationX() {
        float translation = RenderTranslationUtil.getCurrentAdditionalRenderTranslationX();
        return Float.isFinite(translation) ? translation : 0.0F;
    }

    private static float resolveAdditionalRenderTranslationY() {
        float translation = RenderTranslationUtil.getCurrentAdditionalRenderTranslationY();
        return Float.isFinite(translation) ? translation : 0.0F;
    }

    private static void _renderSmoothRect(@Nonnull GuiGraphics graphics, @Nonnull RectArea area) {
        Minecraft minecraft = Minecraft.getInstance();
        int targetWidth = minecraft.getWindow().getWidth();
        int targetHeight = minecraft.getWindow().getHeight();
        float guiScale = resolveGuiScale_FancyMenu();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (targetWidth <= 0 || targetHeight <= 0 || scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        PostChain postChain = getPostChain_FancyMenu(minecraft);
        if (postChain == null) {
            renderFallbackRect_FancyMenu(graphics, area);
            return;
        }

        float scaledX = area.x * guiScale;
        float scaledY = targetHeight - (area.y * guiScale) - scaledHeight;
        float scaledBorderThickness = area.borderThickness * guiScale;
        CornerRadii scaledRadii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F).flipVertical();
        RenderRotationUtil.Rotation2D rotation = RenderRotationUtil.getCurrentAdditionalRenderMaskRotation2D();

        if (!applyUniforms_FancyMenu(postChain, scaledX, scaledY, scaledWidth, scaledHeight, scaledBorderThickness, scaledRadii, rotation, area.color)) {
            renderFallbackRect_FancyMenu(graphics, area);
            return;
        }

        postChain.process(minecraft.getMainRenderTarget(), GraphicsResourceAllocator.UNPOOLED);
        RenderingUtils.resetShaderColor(graphics);
    }

    private static PostChain getPostChain_FancyMenu(@Nonnull Minecraft minecraft) {
        if (smoothRectPostChainFailed_FancyMenu) {
            return null;
        }
        PostChain postChain = minecraft.getShaderManager().getPostChain(SMOOTH_RECT_POST_CHAIN_FANCYMENU, LevelTargetBundle.MAIN_TARGETS);
        if (postChain == null) {
            smoothRectPostChainFailed_FancyMenu = true;
        }
        return postChain;
    }

    private static boolean applyUniforms_FancyMenu(@Nonnull PostChain postChain, float x, float y, float width, float height, float borderThickness, @Nonnull CornerRadii cornerRadii, @Nonnull RenderRotationUtil.Rotation2D rotation, int color) {
        List<PostPass> passes = ((IMixinPostChain)postChain).getPasses_FancyMenu();
        if (passes.isEmpty()) {
            return false;
        }

        Map<String, GpuBuffer> customUniforms = ((IMixinPostPass)passes.get(0)).get_customUniforms_FancyMenu();
        GpuBuffer buffer = customUniforms.get("SmoothRectConfig");
        if (buffer == null) {
            return false;
        }

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer config = createConfigBuffer_FancyMenu(memoryStack, x, y, width, height, borderThickness, cornerRadii, rotation, color);
            if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0 || buffer.size() < SMOOTH_RECT_CONFIG_SIZE_FANCYMENU) {
                GpuBuffer writableBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "FancyMenu SmoothRectConfig",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                        config
                );
                customUniforms.put("SmoothRectConfig", writableBuffer);
                buffer.close();
                return true;
            }
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), config);
        }
        return true;
    }

    private static ByteBuffer createConfigBuffer_FancyMenu(@Nonnull MemoryStack memoryStack, float x, float y, float width, float height, float borderThickness, @Nonnull CornerRadii cornerRadii, @Nonnull RenderRotationUtil.Rotation2D rotation, int color) {
        return Std140Builder.onStack(memoryStack, SMOOTH_RECT_CONFIG_SIZE_FANCYMENU)
                .putVec4(x, y, width, height)
                .putVec4(rotation.m00(), rotation.m01(), rotation.m10(), rotation.m11())
                .putVec4(cornerRadii.topLeft(), cornerRadii.topRight(), cornerRadii.bottomRight(), cornerRadii.bottomLeft())
                .putVec4((float)ARGB.red(color) / 255.0F, (float)ARGB.green(color) / 255.0F, (float)ARGB.blue(color) / 255.0F, (float)ARGB.alpha(color) / 255.0F)
                .putFloat(borderThickness)
                .get();
    }

    private static void renderFallbackRect_FancyMenu(@Nonnull GuiGraphics graphics, @Nonnull RectArea area) {
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

    private static void renderRoundedBorderFallback_FancyMenu(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float border, @Nonnull CornerRadii radii, int color, float guiScale) {
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

    private static void renderRoundedRectFallback_FancyMenu(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, @Nonnull CornerRadii radii, int color, float guiScale) {
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

    private static void fillPixelSpan_FancyMenu(@Nonnull GuiGraphics graphics, float left, float top, float right, float bottom, int color, float guiScale) {
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

    private static void fillPixelRect_FancyMenu(@Nonnull GuiGraphics graphics, float left, float top, float right, float bottom, int color, float guiScale) {
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
