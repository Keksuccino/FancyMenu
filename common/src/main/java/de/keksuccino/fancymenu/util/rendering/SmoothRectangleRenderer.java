package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.FastColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class SmoothRectangleRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SMOOTH_RECT_LOCAL_SHADER_NAME_FANCYMENU = "fancymenu_gui_smooth_rect_local";
    private static final float QUAD_AA_PADDING_PIXELS_FANCYMENU = 2.0F;
    private static final Matrix4f IDENTITY_POSE_FANCYMENU = new Matrix4f().identity();

    @Nullable
    private static ShaderInstance smoothRectLocalShader_FancyMenu;
    private static boolean smoothRectLocalShaderFailed_FancyMenu;
    private static boolean reloadListenerRegistered_FancyMenu;

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
        ShaderInstance shader = getOrCreateShader_FancyMenu();
        if (shader == null) {
            renderFallbackRect_FancyMenu(graphics, area);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int targetWidth = minecraft.getWindow().getWidth();
        int targetHeight = minecraft.getWindow().getHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }

        float guiScale = (float) minecraft.getWindow().getGuiScale();
        if (!Float.isFinite(guiScale) || guiScale <= 0.0F) {
            renderFallbackRect_FancyMenu(graphics, area);
            return;
        }

        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        float scaledX = area.x * guiScale;
        float scaledY = targetHeight - (area.y * guiScale) - scaledHeight;
        float scaledBorderThickness = area.borderThickness * guiScale;
        CornerRadii scaledRadii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F).flipVertical();

        float red = (float) FastColor.ARGB32.red(area.color) / 255.0F;
        float green = (float) FastColor.ARGB32.green(area.color) / 255.0F;
        float blue = (float) FastColor.ARGB32.blue(area.color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(area.color) / 255.0F;

        RenderRotationUtil.Rotation2D rotation = RenderRotationUtil.getCurrentAdditionalRenderMaskRotation2D();
        applyUniforms_FancyMenu(shader, scaledX, scaledY, scaledWidth, scaledHeight, scaledBorderThickness, scaledRadii, rotation, red, green, blue, alpha);

        QuadBounds bounds = computeQuadBounds_FancyMenu(area, targetHeight, guiScale, scaledX, scaledY, scaledWidth, scaledHeight, rotation);

        graphics.flush();
        RenderingUtils.RenderStateSnapshot renderState = RenderingUtils.captureRenderState();
        try {
            RenderingUtils.setupAlphaBlend();
            RenderSystem.setShader(() -> shader);
            drawQuad_FancyMenu(bounds);
        } finally {
            renderState.restore();
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    private static void renderFallbackRect_FancyMenu(@Nonnull GuiGraphics graphics, @Nonnull RectArea area) {
        if (area.borderThickness > 0.0F) {
            float border = Math.min(area.borderThickness, Math.min(area.width * 0.5F, area.height * 0.5F));
            if (border <= 0.0F) {
                return;
            }
            RenderingUtils.fillF(graphics, area.x, area.y, area.x + area.width, area.y + border, area.color);
            RenderingUtils.fillF(graphics, area.x, area.y + area.height - border, area.x + area.width, area.y + area.height, area.color);
            RenderingUtils.fillF(graphics, area.x, area.y + border, area.x + border, area.y + area.height - border, area.color);
            RenderingUtils.fillF(graphics, area.x + area.width - border, area.y + border, area.x + area.width, area.y + area.height - border, area.color);
            return;
        }
        RenderingUtils.fillF(graphics, area.x, area.y, area.x + area.width, area.y + area.height, area.color);
    }

    @Nullable
    private static ShaderInstance getOrCreateShader_FancyMenu() {
        ensureReloadListener_FancyMenu();
        if (smoothRectLocalShaderFailed_FancyMenu) {
            return null;
        }
        if (smoothRectLocalShader_FancyMenu == null) {
            try {
                smoothRectLocalShader_FancyMenu = new ShaderInstance(Minecraft.getInstance().getResourceManager(), SMOOTH_RECT_LOCAL_SHADER_NAME_FANCYMENU, DefaultVertexFormat.POSITION);
            } catch (Exception ex) {
                smoothRectLocalShaderFailed_FancyMenu = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI smooth rectangle local shader!", ex);
                return null;
            }
        }
        return smoothRectLocalShader_FancyMenu;
    }

    private static void ensureReloadListener_FancyMenu() {
        if (reloadListenerRegistered_FancyMenu) {
            return;
        }
        reloadListenerRegistered_FancyMenu = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderSystem.recordRenderCall(SmoothRectangleRenderer::clearShader_FancyMenu);
            }
        });
    }

    private static void clearShader_FancyMenu() {
        if (smoothRectLocalShader_FancyMenu != null) {
            smoothRectLocalShader_FancyMenu.close();
            smoothRectLocalShader_FancyMenu = null;
        }
        smoothRectLocalShaderFailed_FancyMenu = false;
    }

    private static QuadBounds computeQuadBounds_FancyMenu(@Nonnull RectArea fallbackArea, int targetHeight, float guiScale, float scaledX, float scaledY, float scaledWidth, float scaledHeight, @Nonnull RenderRotationUtil.Rotation2D maskRotation) {
        float halfWidth = scaledWidth * 0.5F;
        float halfHeight = scaledHeight * 0.5F;
        float centerX = scaledX + halfWidth;
        float centerY = scaledY + halfHeight;

        RenderRotationUtil.Rotation2D forwardRotation = invertRotation_FancyMenu(maskRotation);
        float extentX = Math.abs(forwardRotation.m00()) * halfWidth + Math.abs(forwardRotation.m01()) * halfHeight;
        float extentY = Math.abs(forwardRotation.m10()) * halfWidth + Math.abs(forwardRotation.m11()) * halfHeight;

        if (!Float.isFinite(extentX) || !Float.isFinite(extentY)) {
            return new QuadBounds(fallbackArea.x, fallbackArea.y, fallbackArea.x + fallbackArea.width, fallbackArea.y + fallbackArea.height);
        }

        float minXInPixels = centerX - extentX - QUAD_AA_PADDING_PIXELS_FANCYMENU;
        float maxXInPixels = centerX + extentX + QUAD_AA_PADDING_PIXELS_FANCYMENU;
        float minYInPixels = centerY - extentY - QUAD_AA_PADDING_PIXELS_FANCYMENU;
        float maxYInPixels = centerY + extentY + QUAD_AA_PADDING_PIXELS_FANCYMENU;

        float minX = minXInPixels / guiScale;
        float maxX = maxXInPixels / guiScale;
        float minY = (targetHeight - maxYInPixels) / guiScale;
        float maxY = (targetHeight - minYInPixels) / guiScale;
        return new QuadBounds(minX, minY, maxX, maxY);
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

    private static void applyUniforms_FancyMenu(@Nonnull ShaderInstance shader, float x, float y, float width, float height, float borderThickness, @Nonnull CornerRadii cornerRadii, @Nonnull RenderRotationUtil.Rotation2D rotation, float red, float green, float blue, float alpha) {
        shader.safeGetUniform("Rect").set(x, y, width, height);
        shader.safeGetUniform("CornerRadii").set(cornerRadii.topLeft(), cornerRadii.topRight(), cornerRadii.bottomRight(), cornerRadii.bottomLeft());
        shader.safeGetUniform("Rotation").set(rotation.m00(), rotation.m01(), rotation.m10(), rotation.m11());
        shader.safeGetUniform("BorderThickness").set(borderThickness);
        shader.safeGetUniform("Color").set(red, green, blue, alpha);
    }

    private static void drawQuad_FancyMenu(@Nonnull QuadBounds bounds) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.vertex(IDENTITY_POSE_FANCYMENU, bounds.minX(), bounds.minY(), 0.0F).endVertex();
        builder.vertex(IDENTITY_POSE_FANCYMENU, bounds.minX(), bounds.maxY(), 0.0F).endVertex();
        builder.vertex(IDENTITY_POSE_FANCYMENU, bounds.maxX(), bounds.maxY(), 0.0F).endVertex();
        builder.vertex(IDENTITY_POSE_FANCYMENU, bounds.maxX(), bounds.minY(), 0.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    private record RectArea(float x, float y, float width, float height, float borderThickness, CornerRadii cornerRadii, int color) {
    }

    private record QuadBounds(float minX, float minY, float maxX, float maxY) {
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
