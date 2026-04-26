package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public final class SmoothImageRectangleRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    // Keep shader files in the default 'minecraft' namespace so the vanilla resource manager finds them for every loader.
    private static final Identifier GUI_SMOOTH_IMAGE_RECT_POST_CHAIN = Identifier.withDefaultNamespace("shaders/post/fancymenu_gui_smooth_image_rect.json");

    private static PostChain smoothImageRectPostChain;
    private static boolean smoothImageRectPostChainFailed;
    private static int cachedWidth = -1;
    private static int cachedHeight = -1;

    private SmoothImageRectangleRenderer() {
    }

    public static void renderSmoothImageRect(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothImageRectInternal(graphics, texture, x, y, width, height, CornerRadii.uniform(cornerRadius), TextureRegion.full(), color, partial);
    }

    public static void renderSmoothImageRectScaled(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothImageRect(graphics, texture, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), cornerRadius * transform.scale(), color, partial);
    }

    public static void renderSmoothImageRect(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight, float cornerRadius, int color, float partial) {
        renderSmoothImageRectInternal(graphics, texture, x, y, width, height, CornerRadii.uniform(cornerRadius), TextureRegion.of(uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight), color, partial);
    }

    public static void renderSmoothImageRectScaled(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothImageRect(graphics, texture, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight, cornerRadius * transform.scale(), color, partial);
    }

    public static void renderSmoothImageRectRoundTopCorners(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothImageRectInternal(graphics, texture, x, y, width, height, CornerRadii.topOnly(cornerRadius), TextureRegion.full(), color, partial);
    }

    public static void renderSmoothImageRectRoundTopCornersScaled(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothImageRectRoundTopCorners(graphics, texture, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), cornerRadius * transform.scale(), color, partial);
    }

    public static void renderSmoothImageRectRoundBottomCorners(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        renderSmoothImageRectInternal(graphics, texture, x, y, width, height, CornerRadii.bottomOnly(cornerRadius), TextureRegion.full(), color, partial);
    }

    public static void renderSmoothImageRectRoundBottomCornersScaled(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float cornerRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothImageRectRoundBottomCorners(graphics, texture, transform.transformX(x), transform.transformY(y), width * transform.scale(), height * transform.scale(), cornerRadius * transform.scale(), color, partial);
    }

    public static void renderSmoothImageRectRoundAllCorners(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        renderSmoothImageRectInternal(graphics, texture, x, y, width, height, CornerRadii.of(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), TextureRegion.full(), color, partial);
    }

    public static void renderSmoothImageRectRoundAllCornersScaled(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius, int color, float partial) {
        GuiPoseTransformUtil.PoseTransform transform = GuiPoseTransformUtil.resolve(graphics);
        renderSmoothImageRectRoundAllCorners(
                graphics,
                texture,
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

    private static void renderSmoothImageRectInternal(@Nonnull GuiGraphics graphics, @Nonnull Identifier texture, float x, float y, float width, float height, @Nonnull CornerRadii cornerRadii, @Nonnull TextureRegion textureRegion, int color, float partial) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(texture);
        Objects.requireNonNull(cornerRadii);
        Objects.requireNonNull(textureRegion);
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        _renderSmoothImageRect(graphics, partial, new RectArea(x, y, width, height, cornerRadii, textureRegion, texture, color));
    }

    private static void _renderSmoothImageRect(GuiGraphics graphics, float partial, RectArea area) {
        Minecraft minecraft = Minecraft.getInstance();
        PostChain postChain = getOrCreatePostChain(minecraft);
        if (postChain == null) {
            return;
        }
        int targetWidth = minecraft.getWindow().getWidth();
        int targetHeight = minecraft.getWindow().getHeight();
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        ensurePostChainSize(postChain, targetWidth, targetHeight);

        float guiScale = (float) minecraft.getWindow().getGuiScale();
        float scaledWidth = area.width * guiScale;
        float scaledHeight = area.height * guiScale;
        if (scaledWidth <= 0.0F || scaledHeight <= 0.0F) {
            return;
        }

        float scaledX = area.x * guiScale;
        float scaledY = targetHeight - (area.y * guiScale) - scaledHeight;
        CornerRadii scaledRadii = area.cornerRadii.scaled(guiScale).clamped(Math.min(scaledWidth, scaledHeight) * 0.5F).flipVertical();

        float red = (float) ARGB.red(area.color) / 255.0F;
        float green = (float) ARGB.green(area.color) / 255.0F;
        float blue = (float) ARGB.blue(area.color) / 255.0F;
        float alpha = (float) ARGB.alpha(area.color) / 255.0F;

        RenderRotationUtil.Rotation2D rotation = GuiPoseTransformUtil.resolve(graphics).rotation();
        int textureId = 0;
        applyUniforms(postChain, area.textureRegion, scaledX, scaledY, scaledWidth, scaledHeight, scaledRadii, rotation, red, green, blue, alpha, textureId);

        
        com.mojang.blaze3d.opengl.GlStateManager._disableBlend();
        postChain.process(minecraft.getMainRenderTarget(), com.mojang.blaze3d.resource.GraphicsResourceAllocator.UNPOOLED);
        RenderTarget finalTarget = getFinalTarget(postChain);
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        de.keksuccino.fancymenu.util.rendering.RenderingUtils.defaultBlendFunc();
        if (finalTarget != null) {
            finalTarget.blitToScreen();
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    private static PostChain getOrCreatePostChain(Minecraft minecraft) {
        return null;
    }

    private static void ensurePostChainSize(PostChain postChain, int width, int height) {
    }

    private static void applyUniforms(PostChain postChain, TextureRegion textureRegion, float x, float y, float width, float height, CornerRadii cornerRadii, RenderRotationUtil.Rotation2D rotation, float red, float green, float blue, float alpha, int textureId) {
    }

    private static RenderTarget getFinalTarget(PostChain postChain) {
        return null;
    }

    private record RectArea(float x, float y, float width, float height, CornerRadii cornerRadii, TextureRegion textureRegion, Identifier texture, int color) {
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
