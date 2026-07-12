package de.keksuccino.fancymenu.util.rendering;

import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

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
        ResourceLocation location = resource.getResourceLocation();
        if (location == null) {
            return false;
        }
        return render(graphics, location, x, y, width, height, resource.getWidth(), resource.getHeight());
    }

    /**
     * @return {@code true} when the dimensions were valid and a draw was submitted.
     */
    public static boolean render(@Nonnull GuiGraphics graphics, @Nonnull ResourceLocation location, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        CoverBounds bounds = calculateBounds(x, y, width, height, textureWidth, textureHeight);
        if (bounds == null) {
            return false;
        }

        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        // Destination and native texture dimensions are intentionally separate so the complete image maps to UV
        // 0-1 instead of repeating when a GUI scale makes the destination larger than the native texture.
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            // Full-image UVs reach the outer texture boundary, so clamp sampling prevents opposite-edge bleed.
            // GuiTextureSamplerUtil restores the texture's wrapping and the previous OpenGL binding after the draw.
            GuiTextureSamplerUtil.runWithClampToEdge(texture.getId(), () -> graphics.blit(location, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight));
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

        // Negative offsets are expected for the overflowing axis; the render method clips them to the requested area.
        int renderX = x + (width - renderWidth) / 2;
        int renderY = y + (height - renderHeight) / 2;
        return new CoverBounds(renderX, renderY, renderWidth, renderHeight);
    }

    record CoverBounds(int x, int y, int width, int height) {
    }
}
