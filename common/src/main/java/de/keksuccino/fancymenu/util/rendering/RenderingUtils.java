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

    //TODO übernehmen
    /**
     * Repeatedly renders a texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link Graphics} instance.
     * @param location The texture {@link ResourceLocation}.
     * @param x The render start X coordinate.
     * @param y The render start Y coordinate.
     * @param fullRenderWidth The full width of the repeating texture area. This is the full size and will get filled with the texture that gets repeatedly rendered inside.
     * @param fullRenderHeight The full height of the repeating texture area. This is the full size and will get filled with the texture that gets repeatedly rendered inside.
     * @param texRenderWidth The texture's width. This is the render size of the SINGLE texture. The texture will get repeatedly rendered inside the full render size.
     * @param texRenderHeight The texture's height. This is the render size of the SINGLE texture. The texture will get repeatedly rendered inside the full render size.
     * @param texOffsetX The X offset of what the first X pixel of the texture should be. (Default 0)
     * @param texOffsetY The Y offset of what the first Y pixel of the texture should be. (Default 0)
     * @param texPartWidth The part of the texture that should get rendered. Starts at the first X pixel and ends at first X pixel + part width.
     * @param texPartHeight The part of the texture that should get rendered. Starts at the first Y pixel and ends at first Y pixel + part height.
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int fullRenderWidth, int fullRenderHeight, int texRenderWidth, int texRenderHeight, int texOffsetX, int texOffsetY, int texPartWidth, int texPartHeight) {

        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        if ((fullRenderWidth <= 0) || (fullRenderHeight <= 0) || (texRenderWidth <= 0) || (texRenderHeight <= 0) || (texPartWidth <= 0) || (texPartHeight <= 0)) return;

        int repeatsHorizontal = Math.max(1, (fullRenderWidth / texPartWidth));
        if ((texPartWidth * repeatsHorizontal) < fullRenderWidth) repeatsHorizontal++;
        int repeatsVertical = Math.max(1, (fullRenderHeight / texPartHeight));
        if ((texPartHeight * repeatsVertical) < fullRenderHeight) repeatsVertical++;

        graphics.enableScissor(x, y, x + fullRenderWidth, y + fullRenderHeight);

        for (int horizontal = 0; horizontal < repeatsHorizontal; horizontal++) {
            for (int vertical = 0; vertical < repeatsVertical; vertical++) {
                int renderX = x + (texPartWidth * horizontal);
                int renderY = y + (texPartHeight * vertical);
                graphics.blit(location, renderX, renderY, texOffsetX, texOffsetY, texPartWidth, texPartHeight, texRenderWidth, texRenderHeight);
            }
        }

        graphics.disableScissor();

    }

    //TODO übernehmen
    /**
     * Repeatedly renders a texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link Graphics} instance.
     * @param location The texture {@link ResourceLocation}.
     * @param x The render start X coordinate.
     * @param y The render start Y coordinate.
     * @param fullRenderWidth The full width of the repeating texture area. This is the full size and will get filled with the texture that gets repeatedly rendered inside.
     * @param fullRenderHeight The full height of the repeating texture area. This is the full size and will get filled with the texture that gets repeatedly rendered inside.
     * @param texRenderWidth The texture's width. This is the render size of the SINGLE texture. The texture will get repeatedly rendered inside the full render size.
     * @param texRenderHeight The texture's height. This is the render size of the SINGLE texture. The texture will get repeatedly rendered inside the full render size.
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int fullRenderWidth, int fullRenderHeight, int texRenderWidth, int texRenderHeight) {
        blitRepeat(graphics, location, x, y, fullRenderWidth, fullRenderHeight, texRenderWidth, texRenderHeight, 0, 0, texRenderWidth, texRenderHeight);
    }

    //TODO übernehmen
    public static void blitNineSlicedCustom(@NotNull GuiGraphics graphics, ResourceLocation location, int x, int y, int renderWidth, int renderHeight, int borderLeft, int borderTop, int borderRight, int borderBottom, int texPartWidth, int texPartHeight, int texOffsetX, int texOffsetY, int texWidth, int texHeight) {

        //TODO alle hier und in blitRepeat auf längere (richtige) blit methode umstellen (wie in top-left corner)
        //TODO alle hier und in blitRepeat auf längere (richtige) blit methode umstellen (wie in top-left corner)
        //TODO alle hier und in blitRepeat auf längere (richtige) blit methode umstellen (wie in top-left corner)
        //TODO alle hier und in blitRepeat auf längere (richtige) blit methode umstellen (wie in top-left corner)

        //Top-left corner
//        graphics.blit(location, x, y, texOffsetX, texOffsetY, borderLeft, borderTop, borderLeft, borderTop);
        graphics.blit(location, x, y, borderLeft, borderTop, (float)texOffsetX, (float)texOffsetY, borderLeft, borderTop, texWidth, texHeight);
        //Top-right corner
        graphics.blit(location, x + renderWidth - borderRight, y, texOffsetX + texPartWidth - borderRight, texOffsetY, borderRight, borderTop, borderRight, borderTop);
        //Bottom-left corner
        graphics.blit(location, x, y + renderHeight - borderBottom, texOffsetX, texOffsetY + texPartHeight - borderBottom, borderLeft, borderBottom, borderLeft, borderBottom);
        //Bottom-right corner
        graphics.blit(location, x + renderWidth - borderRight, y + renderHeight - borderBottom, texOffsetX + texPartWidth - borderRight, texOffsetY + texPartHeight - borderBottom, borderRight, borderBottom, borderRight, borderBottom);

        //Top edge
//        blitRepeat(graphics, location, (x + borderLeft), y, (renderWidth - borderLeft - borderRight), borderTop, (texPartWidth - borderLeft - borderRight), borderTop, (texOffsetX + borderLeft), texOffsetY, (texPartWidth - borderLeft - borderRight), borderTop);
//        //Bottom edge
//        blitRepeat(graphics, location, (x + borderLeft), (y + renderHeight - borderBottom), (renderWidth - borderLeft - borderRight), borderBottom, (texPartWidth - borderLeft - borderRight), borderBottom, (texOffsetX + borderLeft), (texOffsetY + texPartHeight - borderBottom), (texPartWidth - borderLeft - borderRight), borderBottom);
//        //Left edge
//        blitRepeat(graphics, location, x, (y + borderTop), borderLeft, (renderHeight - borderTop - borderBottom), borderLeft, (texPartHeight - borderTop - borderBottom), texOffsetX, (texOffsetY + borderTop), borderLeft, (texPartHeight - borderTop - borderBottom));
//        //Right edge
//        blitRepeat(graphics, location, (x + renderWidth - borderRight), (y + borderTop), borderRight, (renderHeight - borderTop - borderBottom), borderRight, (texPartHeight - borderTop - borderBottom), (texOffsetX + texPartWidth - borderRight), (texOffsetY + borderTop), borderRight, (texPartHeight - borderTop - borderBottom));

        //Middle part
        blitRepeat(graphics, location, (x + borderLeft), (y + borderTop), (renderWidth - borderLeft - borderRight), (renderHeight - borderTop - borderBottom), (texPartWidth - borderLeft - borderRight), (texPartHeight - borderTop - borderBottom), (texOffsetX + borderLeft), (texOffsetY + borderTop), (texPartWidth - borderLeft - borderRight), (texPartHeight - borderTop - borderBottom));

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
