package de.keksuccino.fancymenu.util.rendering;

import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Renders a complete texture centered over a GUI area while preserving its aspect ratio.
 */
public final class GuiTextureCoverRenderer {

    private GuiTextureCoverRenderer() {
    }

    /**
     * @return {@code true} when the resource was valid and a draw was submitted.
     */
    public static boolean render(@Nonnull GuiGraphics graphics, @Nonnull RenderableResource resource, int x, int y, int width, int height) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(resource);
        Identifier location = resource.getResourceLocation();
        if (location == null) {
            return false;
        }
        return render(graphics, location, x, y, width, height, resource.getWidth(), resource.getHeight());
    }

    /**
     * @return {@code true} when the dimensions were valid and a draw was submitted.
     */
    public static boolean render(@Nonnull GuiGraphics graphics, @Nonnull Identifier location, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        CoverBounds bounds = calculateBounds(x, y, width, height, textureWidth, textureHeight);
        if (bounds == null) {
            return false;
        }

        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        // Full-image UVs and GUI-space bounds are intentional here. Using native texture dimensions as the
        // blit UV denominator repeats the image whenever the GUI scale makes the destination comparatively larger.
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            // Cover UVs reach the outer texture boundary, so clamp sampling prevents opposite-edge bleed while
            // GuiTextureSamplerUtil keeps the source sampler's filtering and mipmap behavior unchanged.
            RenderingUtils.submitBlit(graphics, RenderPipelines.GUI_TEXTURED, texture.getTextureView(), GuiTextureSamplerUtil.clampToEdge(texture.getSampler()), bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0.0F, 1.0F, 0.0F, 1.0F, RenderingUtils.getShaderColor());
        } finally {
            graphics.disableScissor();
        }
        return true;
    }

    @Nullable
    static CoverBounds calculateBounds(int x, int y, int width, int height, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return null;
        }

        int[] renderSize = new AspectRatio(textureWidth, textureHeight).getAspectRatioSizeByMinimumSize(width, height);
        int renderWidth = renderSize[0];
        int renderHeight = renderSize[1];
        if (renderWidth <= 0 || renderHeight <= 0) {
            return null;
        }

        // Negative offsets are expected for the overflowing axis. The render method adds a scissor for the
        // requested GUI area, which GuiGraphics intersects with the GUI viewport and any outer scissor.
        int renderX = x + (width - renderWidth) / 2;
        int renderY = y + (height - renderHeight) / 2;
        return new CoverBounds(renderX, renderY, renderWidth, renderHeight);
    }

    record CoverBounds(int x, int y, int width, int height) {

        int right() {
            return this.x + this.width;
        }

        int bottom() {
            return this.y + this.height;
        }
    }
}
