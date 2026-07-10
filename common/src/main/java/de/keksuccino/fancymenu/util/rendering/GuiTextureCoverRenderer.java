package de.keksuccino.fancymenu.util.rendering;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
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
        if (location == null) return false;
        return render(graphics, location, x, y, width, height, resource.getWidth(), resource.getHeight());
    }

    /**
     * @return {@code true} when the dimensions were valid and a draw was submitted.
     */
    public static boolean render(@Nonnull GuiGraphics graphics, @Nonnull ResourceLocation location, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        CoverBounds bounds = calculateBounds(x, y, width, height, textureWidth, textureHeight);
        if (bounds == null) return false;

        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        // Full-image UVs prevent GUI dimensions from turning into repeating texture coordinates. The temporary
        // clamp keeps filtering at UV 0/1 from blending the opposite texture edge and is restored after this draw.
        RenderingUtils.enableScissor(graphics, x, y, x + width, y + height);
        try {
            GuiTextureSamplerUtil.runWithClampToEdge(texture.getId(), () -> RenderingUtils.submitAlphaTextureBlit(graphics, location, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0.0F, 1.0F, 0.0F, 1.0F));
        } finally {
            RenderingUtils.disableScissor(graphics);
        }
        return true;
    }

    @Nullable
    static CoverBounds calculateBounds(int x, int y, int width, int height, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) return null;

        int[] renderSize = new AspectRatio(textureWidth, textureHeight).getAspectRatioSizeByMinimumSize(width, height);
        int renderWidth = renderSize[0];
        int renderHeight = renderSize[1];
        if (renderWidth <= 0 || renderHeight <= 0) return null;

        // Negative offsets are expected for the overflowing axis; the render method clips them to the requested area.
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
