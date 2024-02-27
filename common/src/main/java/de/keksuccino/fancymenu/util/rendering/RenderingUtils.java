package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import java.awt.*;
import java.util.Objects;

public class RenderingUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;

    public static void renderMissing(@NotNull GuiGraphics graphics, int x, int y, int width, int height) {
        int partW = width / 2;
        int partH = height / 2;
        //Top-left
        graphics.fill(x, y, x + partW, y + partH, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
        //Top-right
        graphics.fill(x + partW, y, x + width, y + partH, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-left
        graphics.fill(x, y + partH, x + partW, y + height, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-right
        graphics.fill(x + partW, y + partH, x + width, y + height, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphics} instance.
     * @param location The {@link ResourceLocation} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight) {
        blitRepeat(graphics, location, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, 0, 0, texWidth, texHeight, texWidth, texHeight);
    }

    /**
     * Repeatedly renders a tileable (seamless) portion of a texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphics} instance.
     * @param location The {@link ResourceLocation} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width (in pixels) of the area.
     * @param areaRenderHeight The height (in pixels) of the area.
     * @param texRenderWidth The width (in pixels) each repeated texture should render rendered with.
     * @param texRenderHeight The height (in pixels) each repeated texture should render rendered with.
     * @param texOffsetX The top-left X start coordinate (in pixels) of the part of the full texture that should get rendered.
     * @param texOffsetY The top-left Y start coordinate (in pixels) of the part of the full texture that should get rendered.
     * @param texPartWidth The width (in pixels) of the part of the texture that should get rendered.
     * @param texPartHeight The height (in pixels) of the part of the texture that should get rendered.
     * @param texWidth The FULL width (in pixels) of the texture. NOT the width of the part that should get rendered, but the FULL width!
     * @param texHeight The FULL height (in pixels) of the texture. NOT the height of the part that should get rendered, but the FULL height!
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texRenderWidth, int texRenderHeight, int texOffsetX, int texOffsetY, int texPartWidth, int texPartHeight, int texWidth, int texHeight) {

        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        if ((areaRenderWidth <= 0) || (areaRenderHeight <= 0) || (texRenderWidth <= 0) || (texRenderHeight <= 0) || (texPartWidth <= 0) || (texPartHeight <= 0)) return;

        int repeatsHorizontal = Math.max(1, (areaRenderWidth / texPartWidth));
        if ((texPartWidth * repeatsHorizontal) < areaRenderWidth) repeatsHorizontal++;
        int repeatsVertical = Math.max(1, (areaRenderHeight / texPartHeight));
        if ((texPartHeight * repeatsVertical) < areaRenderHeight) repeatsVertical++;

        graphics.enableScissor(x, y, x + areaRenderWidth, y + areaRenderHeight);

        for (int horizontal = 0; horizontal < repeatsHorizontal; horizontal++) {
            for (int vertical = 0; vertical < repeatsVertical; vertical++) {
                int renderX = x + (texPartWidth * horizontal);
                int renderY = y + (texPartHeight * vertical);
                graphics.blit(location, renderX, renderY, texRenderWidth, texRenderHeight, (float)texOffsetX, (float)texOffsetY, texPartWidth, texPartHeight, texWidth, texHeight);
            }
        }

        graphics.disableScissor();

    }

    /**
     * Renders a nine-sliced portion of a texture.<br><br>
     *
     * Nine-slicing cuts a texture into 9 slices (4 corners, 4 edges and a middle part).<br>
     * This is useful when a texture should keep its proportions no matter what size it gets rendered with.<br><br>
     *
     * Only works with textures that have a tileable (seamless) middle part and tileable edges that can get tiled horizontally and/or vertically without looking bad.
     *
     * @param graphics The {@link GuiGraphics} instance.
     * @param location The {@link ResourceLocation} of the texture.
     * @param x The X position the texture should get rendered at.
     * @param y The Y position the texture should get rendered at.
     * @param renderWidth The width (in pixels) the texture should get rendered with.
     * @param renderHeight The height (in pixels) the texture should get rendered with.
     * @param borderLeft The size (in pixels) of the left border of the texture.
     * @param borderTop The size (in pixels) of the top border of the texture.
     * @param borderRight The size (in pixels) of the right border of the texture.
     * @param borderBottom The size (in pixels) of the bottom border of the texture.
     * @param texPartWidth The width (in pixels) of the part of the texture that should get rendered.
     * @param texPartHeight The height (in pixels) of the part of the texture that should get rendered.
     * @param texOffsetX The top-left X start coordinate (in pixels) of the part of the full texture that should get rendered.
     * @param texOffsetY The top-left Y start coordinate (in pixels) of the part of the full texture that should get rendered.
     * @param texWidth The FULL width (in pixels) of the texture. NOT the width of the part that should get rendered, but the FULL width!
     * @param texHeight The FULL height (in pixels) of the texture. NOT the height of the part that should get rendered, but the FULL height!
     */
    public static void blitNineSliced(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int renderWidth, int renderHeight, int borderLeft, int borderTop, int borderRight, int borderBottom, int texPartWidth, int texPartHeight, int texOffsetX, int texOffsetY, int texWidth, int texHeight) {

        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        if ((renderWidth <= 0) || (renderHeight <= 0) || (texPartWidth <= 0) || (texPartHeight <= 0) || (texWidth <= 0) || (texHeight <= 0)) return;

        if ((renderWidth == texWidth) && (renderHeight == texHeight) && (texOffsetX == 0) && (texOffsetY == 0)) {
            graphics.blit(location, x, y, 0.0F, 0.0F, renderWidth, renderHeight, renderWidth, renderHeight);
            return;
        }

        graphics.enableScissor(x, y, x + renderWidth, y + renderHeight);

        //Top-left corner
        if ((borderLeft > 0) && (borderTop > 0)) {
            graphics.blit(location, x, y, borderLeft, borderTop, (float)texOffsetX, (float)texOffsetY, borderLeft, borderTop, texWidth, texHeight);
        }
        //Top-right corner
        if ((borderRight > 0) && (borderTop > 0)) {
            graphics.blit(location, (x + renderWidth - borderRight), y, borderRight, borderTop, (float)(texOffsetX + texPartWidth - borderRight), (float)texOffsetY, borderRight, borderTop, texWidth, texHeight);
        }
        //Bottom-left corner
        if ((borderLeft > 0) && (borderBottom > 0)) {
            graphics.blit(location, x, (y + renderHeight - borderBottom), borderLeft, borderBottom, (float)texOffsetX, (float)(texOffsetY + texPartHeight - borderBottom), borderLeft, borderBottom, texWidth, texHeight);
        }
        //Bottom-right corner
        if ((borderRight > 0) && (borderBottom > 0)) {
            graphics.blit(location, (x + renderWidth - borderRight), (y + renderHeight - borderBottom), borderRight, borderBottom, (float)(texOffsetX + texPartWidth - borderRight), (float)(texOffsetY + texPartHeight - borderBottom), borderRight, borderBottom, texWidth, texHeight);
        }

        graphics.disableScissor();

        //Top edge
        if (borderTop > 0) blitRepeat(graphics, location, (x + borderLeft), y, (renderWidth - borderLeft - borderRight), borderTop, (texPartWidth - borderLeft - borderRight), borderTop, (texOffsetX + borderLeft), texOffsetY, (texPartWidth - borderLeft - borderRight), borderTop, texWidth, texHeight);
        //Bottom edge
        if (borderBottom > 0) blitRepeat(graphics, location, (x + borderLeft), (y + renderHeight - borderBottom), (renderWidth - borderLeft - borderRight), borderBottom, (texPartWidth - borderLeft - borderRight), borderBottom, (texOffsetX + borderLeft), (texOffsetY + texPartHeight - borderBottom), (texPartWidth - borderLeft - borderRight), borderBottom, texWidth, texHeight);
        //Left edge
        if (borderLeft > 0) blitRepeat(graphics, location, x, (y + borderTop), borderLeft, (renderHeight - borderTop - borderBottom), borderLeft, (texPartHeight - borderTop - borderBottom), texOffsetX, (texOffsetY + borderTop), borderLeft, (texPartHeight - borderTop - borderBottom), texWidth, texHeight);
        //Right edge
        if (borderRight > 0) blitRepeat(graphics, location, (x + renderWidth - borderRight), (y + borderTop), borderRight, (renderHeight - borderTop - borderBottom), borderRight, (texPartHeight - borderTop - borderBottom), (texOffsetX + texPartWidth - borderRight), (texOffsetY + borderTop), borderRight, (texPartHeight - borderTop - borderBottom), texWidth, texHeight);;

        //Middle part
        blitRepeat(graphics, location, (x + borderLeft), (y + borderTop), (renderWidth - borderLeft - borderRight), (renderHeight - borderTop - borderBottom), (texPartWidth - borderLeft - borderRight), (texPartHeight - borderTop - borderBottom), (texOffsetX + borderLeft), (texOffsetY + borderTop), (texPartWidth - borderLeft - borderRight), (texPartHeight - borderTop - borderBottom), texWidth, texHeight);

    }

    public static float getPartialTick() {
        return Minecraft.getInstance().isPaused() ? ((IMixinMinecraft)Minecraft.getInstance()).getPausePartialTickFancyMenu() : Minecraft.getInstance().getFrameTime();
    }

    public static boolean isXYInArea(int targetX, int targetY, int x, int y, int width, int height) {
        return isXYInArea((double)targetX, targetY, x, y, width, height);
    }

    public static boolean isXYInArea(double targetX, double targetY, double x, double y, double width, double height) {
        return (targetX >= x) && (targetX < (x + width)) && (targetY >= y) && (targetY < (y + height));
    }

    public static void resetGuiScale() {
        Window m = Minecraft.getInstance().getWindow();
        m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
    }

    public static void resetShaderColor(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void setShaderColor(GuiGraphics graphics, DrawableColor color) {
        Color c = color.getColor();
        float a = Math.min(1F, Math.max(0F, (float)c.getAlpha() / 255.0F));
        setShaderColor(graphics, color, a);
    }

    public static void setShaderColor(GuiGraphics graphics, DrawableColor color, float alpha) {
        Color c = color.getColor();
        float r = Math.min(1F, Math.max(0F, (float)c.getRed() / 255.0F));
        float g = Math.min(1F, Math.max(0F, (float)c.getGreen() / 255.0F));
        float b = Math.min(1F, Math.max(0F, (float)c.getBlue() / 255.0F));
        graphics.setColor(r, g, b, alpha);
    }

    /**
     * @param color The color.
     * @param newAlpha Value between 0 and 255.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, int newAlpha) {
        newAlpha = Math.min(newAlpha, 255);
        return color & 16777215 | newAlpha << 24;
    }

    /**
     * @param color The color.
     * @param newAlpha Value between 0.0F and 1.0F.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, float newAlpha) {
        return replaceAlphaInColor(color, (int)(newAlpha * 255.0F));
    }

    public static void fillF(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        fillF(graphics, minX, minY, maxX, maxY, 0F, color);
    }

    public static void fillF(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, float z, int color) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        if (minX < maxX) {
            float $$8 = minX;
            minX = maxX;
            maxX = $$8;
        }
        if (minY < maxY) {
            float $$9 = minY;
            minY = maxY;
            maxY = $$9;
        }
        float red = (float)FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, minX, minY, z).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix4f, minX, maxY, z).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix4f, maxX, maxY, z).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix4f, maxX, minY, z).color(red, green, blue, alpha).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void blitF(@NotNull GuiGraphics graphics, ResourceLocation location, float x, float y, float f3, float f4, float width, float height, float width2, float height2) {
        blit(graphics, location, x, y, width, height, f3, f4, width, height, width2, height2);
    }

    private static void blit(GuiGraphics $$0, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10) {
        blit($$0, location, $$1, $$1 + $$3, $$2, $$2 + $$4, 0, $$7, $$8, $$5, $$6, $$9, $$10);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11) {
        innerBlit(
                graphics,
                location,
                $$1,
                $$2,
                $$3,
                $$4,
                $$5,
                ($$8 + 0.0F) / (float)$$10,
                ($$8 + (float)$$6) / (float)$$10,
                ($$9 + 0.0F) / (float)$$11,
                ($$9 + (float)$$7) / (float)$$11
        );
    }

    private static void innerBlit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f $$10 = graphics.pose().last().pose();
        BufferBuilder $$11 = Tesselator.getInstance().getBuilder();
        $$11.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        $$11.vertex($$10, $$1, $$3, $$5).uv($$6, $$8).endVertex();
        $$11.vertex($$10, $$1, $$4, $$5).uv($$6, $$9).endVertex();
        $$11.vertex($$10, $$2, $$4, $$5).uv($$7, $$9).endVertex();
        $$11.vertex($$10, $$2, $$3, $$5).uv($$7, $$8).endVertex();
        BufferUploader.drawWithShader($$11.end());
    }

}
