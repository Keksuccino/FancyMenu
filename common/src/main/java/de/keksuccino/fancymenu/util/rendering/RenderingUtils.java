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
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import java.awt.*;

public class RenderingUtils {

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
