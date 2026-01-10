package de.keksuccino.fancymenu.util.rendering;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class GuiBlurRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation GUI_BLUR_POST_CHAIN = ResourceLocation.withDefaultNamespace("shaders/post/fancymenu_gui_blur.json");
    private static final Map<Screen, List<BlurArea>> QUEUED_AREAS = new WeakHashMap<>();

    private static PostChain blurPostChain;
    private static boolean blurPostChainFailed;
    private static int cachedWidth = -1;
    private static int cachedHeight = -1;

    private GuiBlurRenderer() {
    }

    /**
     * Queues a blur area that gets rendered after the screen background.
     *
     * @param screen The screen this blur area belongs to. The area will render when that screen draws its background.
     * @param x The X position in GUI pixels (top-left origin). Recommended range: 0 to screen width.
     * @param y The Y position in GUI pixels (top-left origin). Recommended range: 0 to screen height.
     * @param width The width in GUI pixels. Recommended range: 1 to screen width.
     * @param height The height in GUI pixels. Recommended range: 1 to screen height.
     * @param blurRadius The blur intensity in GUI pixels. Recommended range: 0 to 16 (4 is a good default).
     * @param cornerRadius The rounded corner radius in GUI pixels. Recommended range: 0 to min(width, height) / 2 (6 is a good default).
     * @param tint The tint color that is mixed into the blurred area. Use alpha to control strength (0.15 is a good default).
     *
     * Example defaults: x=0, y=0, width=200, height=100, blurRadius=4, cornerRadius=6, tint=DrawableColor.of(0, 0, 0, 38)
     */
    public static void queueBlurArea(@Nonnull Screen screen, int x, int y, int width, int height, float blurRadius, float cornerRadius, @Nonnull DrawableColor tint) {
        Objects.requireNonNull(screen);
        Objects.requireNonNull(tint);
        if (width <= 0 || height <= 0) {
            return;
        }
        BlurArea area = new BlurArea(x, y, width, height, blurRadius, cornerRadius, tint);
        QUEUED_AREAS.computeIfAbsent(screen, key -> new ArrayList<>()).add(area);
    }

    /**
     * Renders all queued blur areas for the given screen immediately.
     *
     * @param screen The screen that owns the queued blur areas.
     * @param graphics The GuiGraphics for the current render pass.
     * @param partial Partial tick for the frame; pass the current render partial.
     */
    public static void renderQueuedBlurAreas(@Nonnull Screen screen, @Nonnull GuiGraphics graphics, float partial) {
        Objects.requireNonNull(screen);
        Objects.requireNonNull(graphics);
        List<BlurArea> areas = QUEUED_AREAS.remove(screen);
        if (areas == null || areas.isEmpty()) {
            return;
        }
        for (BlurArea area : areas) {
            renderBlurAreaInternal(graphics, partial, area);
        }
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
        renderBlurAreaInternal(graphics, partial, new BlurArea(x, y, width, height, blurRadius, cornerRadius, tint));
    }

    private static void renderBlurAreaInternal(GuiGraphics graphics, float partial, BlurArea area) {
        if (RenderingUtils.isMenuBlurringBlocked()) {
            return;
        }
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
        postChain.process(partial);
        minecraft.getMainRenderTarget().bindWrite(false);
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

    private record BlurArea(int x, int y, int width, int height, float blurRadius, float cornerRadius, DrawableColor tint) {
    }

}
