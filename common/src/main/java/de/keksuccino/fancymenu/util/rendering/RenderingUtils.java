package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import java.awt.*;
import java.util.function.Function;

public class RenderingUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/fully_transparent.png");

    /**
     * Renders a "missing" texture.
     * This is a 2x2 pattern of magenta and black squares.
     *
     * @param graphics The graphics instance to render to.
     * @param x Top-left X coordinate of the area to render to.
     * @param y Top-left Y coordinate of the area to render to.
     * @param width Width of the rendered area.
     * @param height Height of the rendered area.
     */
    public static void renderMissing(@NotNull GuiGraphics graphics, int x, int y, int width, int height) {
        int partW = width / 2;
        int partH = height / 2;
        //Top-left
        graphics.fill(RenderType.guiOverlay(), x, y, x + partW, y + partH, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
        //Top-right
        graphics.fill(RenderType.guiOverlay(), x + partW, y, x + width, y + partH, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-left
        graphics.fill(RenderType.guiOverlay(), x, y + partH, x + partW, y + height, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-right
        graphics.fill(RenderType.guiOverlay(), x + partW, y + partH, x + width, y + height, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
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
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight, int color) {
        blitRepeat(graphics, RenderType::guiTextured, location, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, color);
    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphics} instance.
     * @param renderType The render type.
     * @param location The {@link ResourceLocation} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull Function<ResourceLocation, RenderType> renderType, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight, int color) {
        graphics.blit(renderType, location, x, y, 0.0F, 0.0F, areaRenderWidth, areaRenderHeight, texWidth, texHeight, color);
    }

    /**
     * Renders a texture using nine-slice scaling with tiled edges and center.
     *
     * @param graphics The GuiGraphics instance to use for rendering
     * @param texture The texture ResourceLocation to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The desired width to render
     * @param height The desired height to render
     * @param textureWidth The actual width of the texture
     * @param textureHeight The actual height of the texture
     * @param borderTop The size of the top border
     * @param borderRight The size of the right border
     * @param borderBottom The size of the bottom border
     * @param borderLeft The size of the left border
     * @param color The color to tint the texture with
     */
    public static void blitNineSlicedTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                             int textureWidth, int textureHeight,
                                             int borderTop, int borderRight, int borderBottom, int borderLeft, int color) {

        blitNineSlicedTexture(graphics, RenderType::guiTextured, texture, x, y, width, height, textureWidth, textureHeight, borderTop, borderRight, borderBottom, borderLeft, color);

    }

    /**
     * Renders a texture using nine-slice scaling with tiled edges and center.
     *
     * @param graphics The GuiGraphics instance to use for rendering
     * @param renderType The render type.
     * @param texture The texture ResourceLocation to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The desired width to render
     * @param height The desired height to render
     * @param textureWidth The actual width of the texture
     * @param textureHeight The actual height of the texture
     * @param borderTop The size of the top border
     * @param borderRight The size of the right border
     * @param borderBottom The size of the bottom border
     * @param borderLeft The size of the left border
     * @param color The color to tint the texture with
     */
    public static void blitNineSlicedTexture(GuiGraphics graphics, @NotNull Function<ResourceLocation, RenderType> renderType, ResourceLocation texture, int x, int y, int width, int height,
                               int textureWidth, int textureHeight,
                               int borderTop, int borderRight, int borderBottom, int borderLeft, int color) {

        // Corner pieces
        // Top left
        graphics.blit(renderType, texture, x, y, 0, 0, borderLeft, borderTop, textureWidth, textureHeight, color);
        // Top right
        graphics.blit(renderType, texture, x + width - borderRight, y, textureWidth - borderRight, 0, borderRight, borderTop, textureWidth, textureHeight, color);
        // Bottom left
        graphics.blit(renderType, texture, x, y + height - borderBottom, 0, textureHeight - borderBottom, borderLeft, borderBottom, textureWidth, textureHeight, color);
        // Bottom right
        graphics.blit(renderType, texture, x + width - borderRight, y + height - borderBottom, textureWidth - borderRight, textureHeight - borderBottom, borderRight, borderBottom, textureWidth, textureHeight, color);

        // Edges - Tiled
        int centerWidth = textureWidth - borderLeft - borderRight;
        int centerHeight = textureHeight - borderTop - borderBottom;

        // Top edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(renderType, texture, x + i, y, borderLeft, 0, pieceWidth, borderTop, textureWidth, textureHeight, color);
        }

        // Bottom edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(renderType, texture, x + i, y + height - borderBottom, borderLeft, textureHeight - borderBottom, pieceWidth, borderBottom, textureWidth, textureHeight, color);
        }

        // Left edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(renderType, texture, x, y + j, 0, borderTop, borderLeft, pieceHeight, textureWidth, textureHeight, color);
        }

        // Right edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(renderType, texture, x + width - borderRight, y + j, textureWidth - borderRight, borderTop, borderRight, pieceHeight, textureWidth, textureHeight, color);
        }

        // Center - Tiled
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
                int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
                graphics.blit(renderType, texture, x + i, y + j, borderLeft, borderTop, pieceWidth, pieceHeight, textureWidth, textureHeight, color);
            }
        }

    }


    public static float getPartialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
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
        fillF(graphics, RenderType.gui(), minX, minY, maxX, maxY, z, color);
    }

    public static void fillF(@NotNull GuiGraphics graphics, @NotNull RenderType renderType, float minX, float minY, float maxX, float maxY, int color) {
        fillF(graphics, renderType, minX, minY, maxX, maxY, 0, color);
    }

    public static void fillF(@NotNull GuiGraphics graphics, @NotNull RenderType renderType, float minX, float minY, float maxX, float maxY, float z, int color) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        if (minX < maxX) {
            float f = minX;
            minX = maxX;
            maxX = f;
        }
        if (minY < maxY) {
            float f = minY;
            minY = maxY;
            maxY = f;
        }
        VertexConsumer $$10 = ((IMixinGuiGraphics)graphics).getBufferSource_FancyMenu().getBuffer(renderType);
        $$10.addVertex(matrix4f, (float)minX, (float)minY, (float)z).setColor(color);
        $$10.addVertex(matrix4f, (float)minX, (float)maxY, (float)z).setColor(color);
        $$10.addVertex(matrix4f, (float)maxX, (float)maxY, (float)z).setColor(color);
        $$10.addVertex(matrix4f, (float)maxX, (float)minY, (float)z).setColor(color);
    }

    public static void blitF(@NotNull GuiGraphics graphics, Function<ResourceLocation, RenderType> renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, int color) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$6, $$7, $$8, $$9, color);
    }

    public static void blitF(@NotNull GuiGraphics graphics, Function<ResourceLocation, RenderType> renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$6, $$7, $$8, $$9);
    }

    public static void blitF(@NotNull GuiGraphics graphics, Function<ResourceLocation, RenderType> renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$8, $$9, $$10, $$11, -1);
    }

    public static void blitF(@NotNull GuiGraphics graphics, Function<ResourceLocation, RenderType> renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11, int color) {
        innerBlit(
                graphics,
                renderTypeFunc,
                location,
                $$2,
                $$2 + $$6,
                $$3,
                $$3 + $$7,
                ($$4 + 0.0F) / (float)$$10,
                ($$4 + (float)$$8) / (float)$$10,
                ($$5 + 0.0F) / (float)$$11,
                ($$5 + (float)$$9) / (float)$$11,
                color
        );
    }

    private static void innerBlit(@NotNull GuiGraphics graphics, Function<ResourceLocation, RenderType> renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, int color) {
        RenderType renderType = renderTypeFunc.apply(location);
        Matrix4f matrix4f = graphics.pose().last().pose();
        VertexConsumer consumer = ((IMixinGuiGraphics)graphics).getBufferSource_FancyMenu().getBuffer(renderType);
        consumer.addVertex(matrix4f, (float)$$2, (float)$$4, 0.0F).setUv($$6, $$8).setColor(color);
        consumer.addVertex(matrix4f, (float)$$2, (float)$$5, 0.0F).setUv($$6, $$9).setColor(color);
        consumer.addVertex(matrix4f, (float)$$3, (float)$$5, 0.0F).setUv($$7, $$9).setColor(color);
        consumer.addVertex(matrix4f, (float)$$3, (float)$$4, 0.0F).setUv($$7, $$8).setColor(color);
    }

}
