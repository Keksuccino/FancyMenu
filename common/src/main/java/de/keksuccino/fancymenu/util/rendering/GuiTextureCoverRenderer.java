package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Renders a texture as a centered aspect-ratio cover image bounded to a GUI area.
 */
public final class GuiTextureCoverRenderer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TEXTURE_COVER_SHADER_NAME = "fancymenu_gui_texture_cover";

    @Nullable private static ShaderInstance textureCoverShader;
    private static boolean textureCoverShaderFailed;
    private static boolean textureCoverShaderReloadListenerRegistered;

    private GuiTextureCoverRenderer() {
    }

    /**
     * @return {@code true} when the resource was valid and its draw was submitted.
     */
    public static boolean render(@NotNull GuiGraphics graphics, @NotNull RenderableResource resource, int x, int y, int width, int height) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(resource);
        ResourceLocation location = resource.getResourceLocation();
        if (location == null) {
            return false;
        }
        return render(graphics, location, x, y, width, height, resource.getWidth(), resource.getHeight());
    }

    /**
     * @return {@code true} when the dimensions were valid and the draw was submitted.
     */
    public static boolean render(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        CoverRegion region = calculateRegion(x, y, width, height, textureWidth, textureHeight);
        if (region == null) {
            return false;
        }

        // Cropped normalized UVs are equivalent to clipping an oversized cover quad, but keep the quad bounded
        // under transformed poses without introducing screen-space scissor rounding at fractional GUI scales.
        RenderingUtils.submitTexturedQuad(graphics, location, GuiTextureCoverRenderer::getTextureCoverShader, region.minX(), region.maxX(), region.minY(), region.maxY(), 0.0F, region.minU(), region.maxU(), region.minV(), region.maxV(), -1);
        return true;
    }

    @Nullable
    static CoverRegion calculateRegion(int x, int y, int width, int height, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return null;
        }

        float minU = 0.0F;
        float maxU = 1.0F;
        float minV = 0.0F;
        float maxV = 1.0F;
        long textureScaledWidth = (long)textureWidth * height;
        long areaScaledWidth = (long)width * textureHeight;
        if (textureScaledWidth > areaScaledWidth) {
            double visibleWidth = (double)areaScaledWidth / (double)textureScaledWidth;
            minU = (float)((1.0D - visibleWidth) * 0.5D);
            maxU = 1.0F - minU;
        } else if (textureScaledWidth < areaScaledWidth) {
            double visibleHeight = (double)textureScaledWidth / (double)areaScaledWidth;
            minV = (float)((1.0D - visibleHeight) * 0.5D);
            maxV = 1.0F - minV;
        }

        // Convert after addition so extreme valid inputs cannot overflow integer coordinates.
        float minX = x;
        float maxX = (float)((double)x + width);
        float minY = y;
        float maxY = (float)((double)y + height);
        return new CoverRegion(minX, maxX, minY, maxY, minU, maxU, minV, maxV);
    }

    private static ShaderInstance getTextureCoverShader() {
        ensureTextureCoverShaderReloadListener();
        if (textureCoverShaderFailed) {
            return GameRenderer.getPositionTexColorShader();
        }
        if (textureCoverShader == null) {
            try {
                textureCoverShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(), TEXTURE_COVER_SHADER_NAME, DefaultVertexFormat.POSITION_TEX_COLOR);
            } catch (Exception ex) {
                textureCoverShaderFailed = true;
                LOGGER.error("[FANCYMENU] Failed to load GUI texture cover shader!", ex);
                return GameRenderer.getPositionTexColorShader();
            }
        }
        return textureCoverShader;
    }

    private static void ensureTextureCoverShaderReloadListener() {
        if (textureCoverShaderReloadListenerRegistered) {
            return;
        }
        textureCoverShaderReloadListenerRegistered = true;
        MinecraftResourceReloadObserver.addReloadListener(action -> {
            if (action == MinecraftResourceReloadObserver.ReloadAction.STARTING) {
                RenderSystem.recordRenderCall(GuiTextureCoverRenderer::clearTextureCoverShader);
            }
        });
    }

    private static void clearTextureCoverShader() {
        if (textureCoverShader != null) {
            textureCoverShader.close();
            textureCoverShader = null;
        }
        textureCoverShaderFailed = false;
    }

    record CoverRegion(float minX, float maxX, float minY, float maxY, float minU, float maxU, float minV, float maxV) {
    }
}
