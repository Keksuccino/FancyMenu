package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public final class GuiBlurRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    // It's important to have shader files in the default 'minecraft' namespace
    private static final ResourceLocation GUI_BLUR_POST_CHAIN = ResourceLocation.withDefaultNamespace("shaders/post/fancymenu_gui_blur.json");

    private static PostChain blurPostChain;
    private static boolean blurPostChainFailed;
    private static int cachedWidth = -1;
    private static int cachedHeight = -1;

    private GuiBlurRenderer() {
    }

    /**
     * Renders a blur area immediately using the current framebuffer contents.
     *
     * @param graphics The GuiGraphics for the current render pass.
     * @param x The X position in GUI pixels (top-left origin). Recommended range: 0 to screen width.
     * @param y The Y position in GUI pixels (top-left origin). Recommended range: 0 to screen height.
     * @param width The width in GUI pixels. Recommended range: 1 to screen width.
     * @param height The height in GUI pixels. Recommended range: 1 to screen height.
     * @param blurRadius The blur intensity in GUI pixels. Recommended range: 0 to 16 (4 is a good default).
     * @param cornerRadius The rounded corner radius in GUI pixels. Recommended range: 0 to min(width, height) / 2 (6 is a good default).
     * @param tint The tint color that is mixed into the blurred area. Use alpha to control strength (0.15 is a good default).
     * @param partial Partial tick for the frame; pass the current render partial.
     *
     * Example defaults: x=0, y=0, width=200, height=100, blurRadius=4, cornerRadius=6, tint=DrawableColor.of(0, 0, 0, 38)
     */
    public static void renderBlurArea(@Nonnull GuiGraphics graphics, int x, int y, int width, int height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint, float partial) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(tint);
        if (width <= 0 || height <= 0) {
            return;
        }
        _renderBlurArea(graphics, partial, new BlurArea(x, y, width, height, blurRadius, cornerRadius, tint));
    }

    private static void _renderBlurArea(GuiGraphics graphics, float partial, BlurArea area) {
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
        float blurRadius = Math.max(0.0F, area.blurRadius * guiScale);
        float cornerRadius = Math.max(0.0F, area.cornerRadius * guiScale);
        float maxCorner = Math.min(scaledWidth, scaledHeight) * 0.5F;
        if (cornerRadius > maxCorner) {
            cornerRadius = maxCorner;
        }

        DrawableColor.FloatColor tint = area.tint.getAsFloats();
        applyUniforms(postChain, scaledX, scaledY, scaledWidth, scaledHeight, blurRadius, cornerRadius, tint);

        graphics.flush();
        // Post effects must render with blending disabled or they'll repeatedly alpha‑multiply
        // the framebuffer content (causing translucent draws to darken each time we blur).
        RenderSystem.disableBlend();
        runPostChainWithScissor(graphics, minecraft, area, postChain, partial);
        minecraft.getMainRenderTarget().bindWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderingUtils.resetShaderColor(graphics);
    }

    private static PostChain getOrCreatePostChain(Minecraft minecraft) {
        if (blurPostChainFailed) {
            return null;
        }
        if (blurPostChain == null) {
            try {
                blurPostChain = new PostChain(
                        minecraft.getTextureManager(),
                        minecraft.getResourceManager(),
                        minecraft.getMainRenderTarget(),
                        GUI_BLUR_POST_CHAIN
                );
                cachedWidth = -1;
                cachedHeight = -1;
                ensurePostChainSize(blurPostChain, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            } catch (Exception ex) {
                blurPostChainFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI blur shader!", ex);
                return null;
            }
        }
        return blurPostChain;
    }

    private static void ensurePostChainSize(PostChain postChain, int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            postChain.resize(width, height);
        }
    }

    private static void applyUniforms(PostChain postChain, float x, float y, float width, float height, float blurRadius, float cornerRadius, DrawableColor.FloatColor tint) {
        List<PostPass> passes = ((IMixinPostChain) postChain).getPasses_FancyMenu();
        float[] blurMultipliers = new float[]{1.0F, 1.0F, 0.5F, 0.5F, 0.25F, 0.25F};
        int blurIndex = 0;
        for (PostPass pass : passes) {
            if ("box_blur".equals(pass.getName()) && blurIndex < blurMultipliers.length) {
                pass.getEffect().safeGetUniform("Radius").set(blurRadius * blurMultipliers[blurIndex]);
                blurIndex++;
                continue;
            }
            if (!"fancymenu_gui_blur".equals(pass.getName())) {
                continue;
            }
            pass.getEffect().safeGetUniform("Rect").set(x, y, width, height);
            pass.getEffect().safeGetUniform("CornerRadius").set(cornerRadius);
            pass.getEffect().safeGetUniform("Tint").set(tint.red(), tint.green(), tint.blue(), tint.alpha());
        }
    }

    private static void runPostChainWithScissor(GuiGraphics graphics, Minecraft minecraft, BlurArea area, PostChain postChain, float partial) {
        // Limit writes to the blur rectangle (plus blur fringe) so unrelated pixels stay untouched.
        int margin = Mth.ceil(area.blurRadius * 4.0F); // generous padding for multi-pass blur spread
        int minX = Math.max(0, area.x - margin);
        int minY = Math.max(0, area.y - margin);
        int maxX = Math.min(minecraft.getWindow().getGuiScaledWidth(), area.x + area.width + margin);
        int maxY = Math.min(minecraft.getWindow().getGuiScaledHeight(), area.y + area.height + margin);

        boolean hasArea = maxX > minX && maxY > minY;
        if (hasArea) {
            graphics.enableScissor(minX, minY, maxX, maxY);
        }
        postChain.process(partial);
        if (hasArea) {
            graphics.disableScissor();
        }
    }

    private record BlurArea(int x, int y, int width, int height, float blurRadius, float cornerRadius, DrawableColor tint) {
    }

}
