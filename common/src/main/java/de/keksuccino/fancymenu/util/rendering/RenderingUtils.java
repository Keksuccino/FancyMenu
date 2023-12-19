package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMinecraft;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.NoSuchElementException;

public class RenderingUtils extends GuiComponent {

    public static final RenderingUtils INSTANCE = new RenderingUtils();

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

    public static void resetShaderColor() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void setShaderColor(DrawableColor color) {
        Color c = color.getColor();
        float a = Math.min(1F, Math.max(0F, (float)c.getAlpha() / 255.0F));
        setShaderColor(color, a);
    }

    public static void setShaderColor(DrawableColor color, float alpha) {
        Color c = color.getColor();
        float r = Math.min(1F, Math.max(0F, (float)c.getRed() / 255.0F));
        float g = Math.min(1F, Math.max(0F, (float)c.getGreen() / 255.0F));
        float b = Math.min(1F, Math.max(0F, (float)c.getBlue() / 255.0F));
        RenderSystem.setShaderColor(r, g, b, alpha);
    }

    public static void bindTexture(@NotNull ResourceLocation texture, boolean depthTest) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        if (depthTest) RenderSystem.enableDepthTest();
    }

    public static void bindTexture(@NotNull ResourceLocation texture) {
        bindTexture(texture, false);
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

    public static void fillF(@NotNull PoseStack pose, float minX, float minY, float maxX, float maxY, int color) {
        fillF(pose, minX, minY, maxX, maxY, 0F, color);
    }

    public static void fillF(@NotNull PoseStack pose, float minX, float minY, float maxX, float maxY, float z, int color) {
        Matrix4f matrix4f = pose.last().pose();
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
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix4f, minX, minY, z).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, minX, maxY, z).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, maxX, maxY, z).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix4f, maxX, minY, z).color(red, green, blue, alpha).endVertex();
        BufferUploader.draw(builder.end());
        RenderSystem.disableBlend();
    }

    public static void blitF(@NotNull PoseStack pose, float x, float y, float f3, float f4, float width, float height, float width2, float height2) {
        blit(pose, x, y, width, height, f3, f4, width, height, width2, height2);
    }

    private static void blit(PoseStack $$0, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10) {
        blit($$0, $$1, $$1 + $$3, $$2, $$2 + $$4, 0, $$7, $$8, $$5, $$6, $$9, $$10);
    }

    private static void blit(PoseStack $$0, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11) {
        innerBlit($$0.last().pose(), $$1, $$2, $$3, $$4, $$5, ($$8 + 0.0F) / (float)$$10, ($$8 + (float)$$6) / (float)$$10, ($$9 + 0.0F) / (float)$$11, ($$9 + (float)$$7) / (float)$$11);
    }

    private static void innerBlit(Matrix4f $$0, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex($$0, $$1, $$3, $$5).uv($$6, $$8).endVertex();
        builder.vertex($$0, $$1, $$4, $$5).uv($$6, $$9).endVertex();
        builder.vertex($$0, $$2, $$4, $$5).uv($$7, $$9).endVertex();
        builder.vertex($$0, $$2, $$3, $$5).uv($$7, $$8).endVertex();
        BufferUploader.draw(builder.end());
    }

    public static void blitNineSliced(PoseStack $$0, int $$1, int $$2, int $$3, int $$4, int $$5, int $$6, int $$7, int $$8, int $$9) {
        blitNineSliced($$0, $$1, $$2, $$3, $$4, $$5, $$5, $$5, $$5, $$6, $$7, $$8, $$9);
    }

    public static void blitNineSliced(PoseStack $$0, int $$1, int $$2, int $$3, int $$4, int $$5, int $$6, int $$7, int $$8, int $$9, int $$10) {
        blitNineSliced($$0, $$1, $$2, $$3, $$4, $$5, $$6, $$5, $$6, $$7, $$8, $$9, $$10);
    }

    public static void blitNineSliced(PoseStack $$0, int $$1, int $$2, int $$3, int $$4, int $$5, int $$6, int $$7, int $$8, int $$9, int $$10, int $$11, int $$12) {
        $$5 = Math.min($$5, $$3 / 2);
        $$7 = Math.min($$7, $$3 / 2);
        $$6 = Math.min($$6, $$4 / 2);
        $$8 = Math.min($$8, $$4 / 2);
        if ($$3 == $$9 && $$4 == $$10) {
            INSTANCE.blit($$0, $$1, $$2, $$11, $$12, $$3, $$4);
        } else if ($$4 == $$10) {
            INSTANCE.blit($$0, $$1, $$2, $$11, $$12, $$5, $$4);
            blitRepeating($$0, $$1 + $$5, $$2, $$3 - $$7 - $$5, $$4, $$11 + $$5, $$12, $$9 - $$7 - $$5, $$10);
            INSTANCE.blit($$0, $$1 + $$3 - $$7, $$2, $$11 + $$9 - $$7, $$12, $$7, $$4);
        } else if ($$3 == $$9) {
            INSTANCE.blit($$0, $$1, $$2, $$11, $$12, $$3, $$6);
            blitRepeating($$0, $$1, $$2 + $$6, $$3, $$4 - $$8 - $$6, $$11, $$12 + $$6, $$9, $$10 - $$8 - $$6);
            INSTANCE.blit($$0, $$1, $$2 + $$4 - $$8, $$11, $$12 + $$10 - $$8, $$3, $$8);
        } else {
            INSTANCE.blit($$0, $$1, $$2, $$11, $$12, $$5, $$6);
            blitRepeating($$0, $$1 + $$5, $$2, $$3 - $$7 - $$5, $$6, $$11 + $$5, $$12, $$9 - $$7 - $$5, $$6);
            INSTANCE.blit($$0, $$1 + $$3 - $$7, $$2, $$11 + $$9 - $$7, $$12, $$7, $$6);
            INSTANCE.blit($$0, $$1, $$2 + $$4 - $$8, $$11, $$12 + $$10 - $$8, $$5, $$8);
            blitRepeating($$0, $$1 + $$5, $$2 + $$4 - $$8, $$3 - $$7 - $$5, $$8, $$11 + $$5, $$12 + $$10 - $$8, $$9 - $$7 - $$5, $$8);
            INSTANCE.blit($$0, $$1 + $$3 - $$7, $$2 + $$4 - $$8, $$11 + $$9 - $$7, $$12 + $$10 - $$8, $$7, $$8);
            blitRepeating($$0, $$1, $$2 + $$6, $$5, $$4 - $$8 - $$6, $$11, $$12 + $$6, $$5, $$10 - $$8 - $$6);
            blitRepeating($$0, $$1 + $$5, $$2 + $$6, $$3 - $$7 - $$5, $$4 - $$8 - $$6, $$11 + $$5, $$12 + $$6, $$9 - $$7 - $$5, $$10 - $$8 - $$6);
            blitRepeating($$0, $$1 + $$3 - $$7, $$2 + $$6, $$5, $$4 - $$8 - $$6, $$11 + $$9 - $$7, $$12 + $$6, $$7, $$10 - $$8 - $$6);
        }
    }

    public static void blitRepeating(PoseStack $$0, int $$1, int $$2, int $$3, int $$4, int $$5, int $$6, int $$7, int $$8) {
        int $$9 = $$1;

        int $$11;
        for(IntIterator $$10 = slices($$3, $$7); $$10.hasNext(); $$9 += $$11) {
            $$11 = $$10.nextInt();
            int $$12 = ($$7 - $$11) / 2;
            int $$13 = $$2;

            int $$15;
            for(IntIterator $$14 = slices($$4, $$8); $$14.hasNext(); $$13 += $$15) {
                $$15 = $$14.nextInt();
                int $$16 = ($$8 - $$15) / 2;
                INSTANCE.blit($$0, $$9, $$13, $$5 + $$12, $$6 + $$16, $$11, $$15);
            }
        }
    }

    private static IntIterator slices(int $$0, int $$1) {
        int $$2 = Mth.positiveCeilDiv($$0, $$1);
        return new Divisor($$0, $$2);
    }

    public static void enableScissor(int xStart, int yStart, int xEnd, int yEnd) {
        applyScissor(new ScreenRectangle(xStart, yStart, xEnd - xStart, yEnd - yStart));
    }

    public static void disableScissor() {
        applyScissor(null);
    }

    private static void applyScissor(@Nullable ScreenRectangle rectangle) {
        if (rectangle != null) {
            Window window = Minecraft.getInstance().getWindow();
            int windowHeight = window.getHeight();
            double windowScale = window.getGuiScale();
            double $$4 = (double)rectangle.left() * windowScale;
            double $$5 = (double)windowHeight - (double)rectangle.bottom() * windowScale;
            double $$6 = (double)rectangle.width() * windowScale;
            double $$7 = (double)rectangle.height() * windowScale;
            RenderSystem.enableScissor((int)$$4, (int)$$5, Math.max(0, (int)$$6), Math.max(0, (int)$$7));
        } else {
            RenderSystem.disableScissor();
        }
    }

    public record ScreenRectangle(ScreenPosition position, int width, int height) {

        private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

        public ScreenRectangle(int x, int y, int width, int height) {
            this(new ScreenPosition(x, y), width, height);
        }

        public static ScreenRectangle empty() {
            return EMPTY;
        }

        public int top() {
            return this.position.y();
        }

        public int bottom() {
            return this.position.y() + this.height;
        }

        public int left() {
            return this.position.x();
        }

        public int right() {
            return this.position.x() + this.width;
        }

    }

    public record ScreenPosition(int x, int y) {
    }

    public static class Divisor implements IntIterator {

        private final int denominator;
        private final int quotient;
        private final int mod;
        private int returnedParts;
        private int remainder;

        public Divisor(int $$0, int $$1) {
            this.denominator = $$1;
            if ($$1 > 0) {
                this.quotient = $$0 / $$1;
                this.mod = $$0 % $$1;
            } else {
                this.quotient = 0;
                this.mod = 0;
            }
        }

        @Override
        public boolean hasNext() {
            return this.returnedParts < this.denominator;
        }

        @Override
        public int nextInt() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                int $$0 = this.quotient;
                this.remainder += this.mod;
                if (this.remainder >= this.denominator) {
                    this.remainder -= this.denominator;
                    ++$$0;
                }

                ++this.returnedParts;
                return $$0;
            }
        }

    }

}
