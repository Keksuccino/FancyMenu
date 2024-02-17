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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class RenderingUtils extends GuiComponent {

    public static final RenderingUtils INSTANCE = new RenderingUtils();
    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;
    //TODO übernehmen 1.18.2
    private static final List<ScissorLayer> SCISSOR_STACK = new ArrayList<>();

    public static void renderMissing(@NotNull PoseStack pose, int x, int y, int width, int height) {
        int partW = width / 2;
        int partH = height / 2;
        //Top-left
        fill(pose, x, y, x + partW, y + partH, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
        //Top-right
        fill(pose, x + partW, y, x + width, y + partH, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-left
        fill(pose, x, y + partH, x + partW, y + height, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-right
        fill(pose, x + partW, y + partH, x + width, y + height, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param pose The {@link PoseStack} instance.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull PoseStack pose, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight) {
        blitRepeat(pose, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, 0, 0, texWidth, texHeight, texWidth, texHeight);
    }

    /**
     * Repeatedly renders a tileable (seamless) portion of a texture inside an area. Fills the area with the texture.
     *
     * @param pose The {@link PoseStack} instance.
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
    public static void blitRepeat(@NotNull PoseStack pose, int x, int y, int areaRenderWidth, int areaRenderHeight, int texRenderWidth, int texRenderHeight, int texOffsetX, int texOffsetY, int texPartWidth, int texPartHeight, int texWidth, int texHeight) {

        Objects.requireNonNull(pose);
        if ((areaRenderWidth <= 0) || (areaRenderHeight <= 0) || (texRenderWidth <= 0) || (texRenderHeight <= 0) || (texPartWidth <= 0) || (texPartHeight <= 0)) return;

        int repeatsHorizontal = Math.max(1, (areaRenderWidth / texPartWidth));
        if ((texPartWidth * repeatsHorizontal) < areaRenderWidth) repeatsHorizontal++;
        int repeatsVertical = Math.max(1, (areaRenderHeight / texPartHeight));
        if ((texPartHeight * repeatsVertical) < areaRenderHeight) repeatsVertical++;

        enableScissor(x, y, x + areaRenderWidth, y + areaRenderHeight);

        for (int horizontal = 0; horizontal < repeatsHorizontal; horizontal++) {
            for (int vertical = 0; vertical < repeatsVertical; vertical++) {
                int renderX = x + (texPartWidth * horizontal);
                int renderY = y + (texPartHeight * vertical);
                blit(pose, renderX, renderY, texRenderWidth, texRenderHeight, (float)texOffsetX, (float)texOffsetY, texPartWidth, texPartHeight, texWidth, texHeight);
            }
        }

        disableScissor();

    }

    /**
     * Renders a nine-sliced portion of a texture.<br><br>
     *
     * Nine-slicing cuts a texture into 9 slices (4 corners, 4 edges and a middle part).<br>
     * This is useful when a texture should keep its proportions no matter what size it gets rendered with.<br><br>
     *
     * Only works with textures that have a tileable (seamless) middle part and tileable edges that can get tiled horizontally and/or vertically without looking bad.
     *
     * @param pose The {@link PoseStack} instance.
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
    public static void blitNineSliced(@NotNull PoseStack pose, int x, int y, int renderWidth, int renderHeight, int borderLeft, int borderTop, int borderRight, int borderBottom, int texPartWidth, int texPartHeight, int texOffsetX, int texOffsetY, int texWidth, int texHeight) {

        Objects.requireNonNull(pose);
        if ((renderWidth <= 0) || (renderHeight <= 0) || (texPartWidth <= 0) || (texPartHeight <= 0) || (texWidth <= 0) || (texHeight <= 0)) return;

        if ((renderWidth == texWidth) && (renderHeight == texHeight) && (texOffsetX == 0) && (texOffsetY == 0)) {
            blit(pose, x, y, 0.0F, 0.0F, renderWidth, renderHeight, renderWidth, renderHeight);
            return;
        }

        enableScissor(x, y, x + renderWidth, y + renderHeight);

        //Top-left corner
        if ((borderLeft > 0) && (borderTop > 0)) {
            blit(pose, x, y, borderLeft, borderTop, (float)texOffsetX, (float)texOffsetY, borderLeft, borderTop, texWidth, texHeight);
        }
        //Top-right corner
        if ((borderRight > 0) && (borderTop > 0)) {
            blit(pose, (x + renderWidth - borderRight), y, borderRight, borderTop, (float)(texOffsetX + texPartWidth - borderRight), (float)texOffsetY, borderRight, borderTop, texWidth, texHeight);
        }
        //Bottom-left corner
        if ((borderLeft > 0) && (borderBottom > 0)) {
            blit(pose, x, (y + renderHeight - borderBottom), borderLeft, borderBottom, (float)texOffsetX, (float)(texOffsetY + texPartHeight - borderBottom), borderLeft, borderBottom, texWidth, texHeight);
        }
        //Bottom-right corner
        if ((borderRight > 0) && (borderBottom > 0)) {
            blit(pose, (x + renderWidth - borderRight), (y + renderHeight - borderBottom), borderRight, borderBottom, (float)(texOffsetX + texPartWidth - borderRight), (float)(texOffsetY + texPartHeight - borderBottom), borderRight, borderBottom, texWidth, texHeight);
        }

        disableScissor();

        //Top edge
        if (borderTop > 0) blitRepeat(pose, (x + borderLeft), y, (renderWidth - borderLeft - borderRight), borderTop, (texPartWidth - borderLeft - borderRight), borderTop, (texOffsetX + borderLeft), texOffsetY, (texPartWidth - borderLeft - borderRight), borderTop, texWidth, texHeight);
        //Bottom edge
        if (borderBottom > 0) blitRepeat(pose, (x + borderLeft), (y + renderHeight - borderBottom), (renderWidth - borderLeft - borderRight), borderBottom, (texPartWidth - borderLeft - borderRight), borderBottom, (texOffsetX + borderLeft), (texOffsetY + texPartHeight - borderBottom), (texPartWidth - borderLeft - borderRight), borderBottom, texWidth, texHeight);
        //Left edge
        if (borderLeft > 0) blitRepeat(pose, x, (y + borderTop), borderLeft, (renderHeight - borderTop - borderBottom), borderLeft, (texPartHeight - borderTop - borderBottom), texOffsetX, (texOffsetY + borderTop), borderLeft, (texPartHeight - borderTop - borderBottom), texWidth, texHeight);
        //Right edge
        if (borderRight > 0) blitRepeat(pose, (x + renderWidth - borderRight), (y + borderTop), borderRight, (renderHeight - borderTop - borderBottom), borderRight, (texPartHeight - borderTop - borderBottom), (texOffsetX + texPartWidth - borderRight), (texOffsetY + borderTop), borderRight, (texPartHeight - borderTop - borderBottom), texWidth, texHeight);;

        //Middle part
        blitRepeat(pose, (x + borderLeft), (y + borderTop), (renderWidth - borderLeft - borderRight), (renderHeight - borderTop - borderBottom), (texPartWidth - borderLeft - borderRight), (texPartHeight - borderTop - borderBottom), (texOffsetX + borderLeft), (texOffsetY + borderTop), (texPartWidth - borderLeft - borderRight), (texPartHeight - borderTop - borderBottom), texWidth, texHeight);

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
        BufferBuilder $$10 = Tesselator.getInstance().getBuilder();
        $$10.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        $$10.vertex($$0, $$1, $$3, $$5).uv($$6, $$8).endVertex();
        $$10.vertex($$0, $$1, $$4, $$5).uv($$6, $$9).endVertex();
        $$10.vertex($$0, $$2, $$4, $$5).uv($$7, $$9).endVertex();
        $$10.vertex($$0, $$2, $$3, $$5).uv($$7, $$8).endVertex();
        BufferUploader.drawWithShader($$10.end());
    }

    public static void blitNineSliced_Vanilla(PoseStack pose, int x, int y, int width, int height, int sliceSize, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        blitNineSliced_Vanilla(pose, x, y, width, height, sliceSize, sliceSize, sliceSize, sliceSize, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blitNineSliced_Vanilla(PoseStack pose, int x, int y, int width, int height, int sliceWidth, int sliceHeight, int uWidth, int vHeight, int textureX, int textureY) {
        blitNineSliced_Vanilla(pose, x, y, width, height, sliceWidth, sliceHeight, sliceWidth, sliceHeight, uWidth, vHeight, textureX, textureY);
    }

    public static void blitNineSliced_Vanilla(PoseStack pose, int x, int y, int width, int height, int leftBorder, int topBorder, int rightBorder, int bottomBorder, int uWidth, int vHeight, int textureX, int textureY) {
        //TODO übernehmen 1.18.2
        if ((width <= 0) || (height <= 0) || (uWidth <= 0) || (vHeight <= 0)) return;
        leftBorder = Math.min(leftBorder, width / 2);
        rightBorder = Math.min(rightBorder, width / 2);
        topBorder = Math.min(topBorder, height / 2);
        bottomBorder = Math.min(bottomBorder, height / 2);
        if (width == uWidth && height == vHeight) {
            INSTANCE.blit(pose, x, y, textureX, textureY, width, height);
        } else if (height == vHeight) {
            INSTANCE.blit(pose, x, y, textureX, textureY, leftBorder, height);
            blitRepeating_Vanilla(pose, x + leftBorder, y, width - rightBorder - leftBorder, height, textureX + leftBorder, textureY, uWidth - rightBorder - leftBorder, vHeight);
            INSTANCE.blit(pose, x + width - rightBorder, y, textureX + uWidth - rightBorder, textureY, rightBorder, height);
        } else if (width == uWidth) {
            INSTANCE.blit(pose, x, y, textureX, textureY, width, topBorder);
            blitRepeating_Vanilla(pose, x, y + topBorder, width, height - bottomBorder - topBorder, textureX, textureY + topBorder, uWidth, vHeight - bottomBorder - topBorder);
            INSTANCE.blit(pose, x, y + height - bottomBorder, textureX, textureY + vHeight - bottomBorder, width, bottomBorder);
        } else {
            INSTANCE.blit(pose, x, y, textureX, textureY, leftBorder, topBorder);
            blitRepeating_Vanilla(pose, x + leftBorder, y, width - rightBorder - leftBorder, topBorder, textureX + leftBorder, textureY, uWidth - rightBorder - leftBorder, topBorder);
            INSTANCE.blit(pose, x + width - rightBorder, y, textureX + uWidth - rightBorder, textureY, rightBorder, topBorder);
            INSTANCE.blit(pose, x, y + height - bottomBorder, textureX, textureY + vHeight - bottomBorder, leftBorder, bottomBorder);
            blitRepeating_Vanilla(pose, x + leftBorder, y + height - bottomBorder, width - rightBorder - leftBorder, bottomBorder, textureX + leftBorder, textureY + vHeight - bottomBorder, uWidth - rightBorder - leftBorder, bottomBorder);
            INSTANCE.blit(pose, x + width - rightBorder, y + height - bottomBorder, textureX + uWidth - rightBorder, textureY + vHeight - bottomBorder, rightBorder, bottomBorder);
            blitRepeating_Vanilla(pose, x, y + topBorder, leftBorder, height - bottomBorder - topBorder, textureX, textureY + topBorder, leftBorder, vHeight - bottomBorder - topBorder);
            blitRepeating_Vanilla(pose, x + leftBorder, y + topBorder, width - rightBorder - leftBorder, height - bottomBorder - topBorder, textureX + leftBorder, textureY + topBorder, uWidth - rightBorder - leftBorder, vHeight - bottomBorder - topBorder);
            blitRepeating_Vanilla(pose, x + width - rightBorder, y + topBorder, leftBorder, height - bottomBorder - topBorder, textureX + uWidth - rightBorder, textureY + topBorder, rightBorder, vHeight - bottomBorder - topBorder);
        }
    }

    public static void blitRepeating_Vanilla(PoseStack pose, int x, int y, int width, int height, int uOffset, int vOffset, int sourceWidth, int sourceHeight) {
        //TODO übernehmen 1.18.2
        if ((width <= 0) || (height <= 0) || (sourceWidth <= 0) || (sourceHeight <= 0)) return;
        int i1 = x;
        int i2;
        for(IntIterator iterator = slices_Vanilla(width, sourceWidth); iterator.hasNext(); i1 += i2) {
            i2 = iterator.nextInt();
            int $$12 = (sourceWidth - i2) / 2;
            int $$13 = y;
            int $$15;
            for(IntIterator $$14 = slices_Vanilla(height, sourceHeight); $$14.hasNext(); $$13 += $$15) {
                $$15 = $$14.nextInt();
                int $$16 = (sourceHeight - $$15) / 2;
                INSTANCE.blit(pose, i1, $$13, uOffset + $$12, vOffset + $$16, i2, $$15);
            }
        }
    }

    private static IntIterator slices_Vanilla(int target, int total) {
        int i = Mth.positiveCeilDiv(target, total);
        return new Divisor(target, i);
    }

    public static void enableScissor(int xStart, int yStart, int xEnd, int yEnd) {
        applyScissor(new ScreenRectangle(xStart, yStart, xEnd - xStart, yEnd - yStart), true);
    }

    public static void disableScissor() {
        applyScissor(null, true);
    }

    //TODO übernehmen 1.18.2
    private static void applyScissor(@Nullable ScreenRectangle rectangle, boolean pushStack) {
        if (rectangle != null) {
            //push layer
            if (pushStack) {
                if (!SCISSOR_STACK.isEmpty()) {
                    rectangle = Objects.requireNonNullElse(rectangle.intersection(SCISSOR_STACK.get(SCISSOR_STACK.size()-1).rectangle()), ScreenRectangle.empty());
                }
                SCISSOR_STACK.add(new ScissorLayer(rectangle));
            }
            Window window = Minecraft.getInstance().getWindow();
            int windowHeight = window.getHeight();
            double windowScale = window.getGuiScale();
            double $$4 = (double)rectangle.left() * windowScale;
            double $$5 = (double)windowHeight - (double)rectangle.bottom() * windowScale;
            double $$6 = (double)rectangle.width() * windowScale;
            double $$7 = (double)rectangle.height() * windowScale;
            RenderSystem.enableScissor((int)$$4, (int)$$5, Math.max(0, (int)$$6), Math.max(0, (int)$$7));
        } else {
            //pop layer
            if (!SCISSOR_STACK.isEmpty()) {
                SCISSOR_STACK.remove(SCISSOR_STACK.size()-1);
            }
            //apply new top scissor layer OR disable scissor if stack is empty
            if (!SCISSOR_STACK.isEmpty()) {
                applyScissor(SCISSOR_STACK.get(SCISSOR_STACK.size()-1).rectangle(), false);
            } else {
                RenderSystem.disableScissor();
            }
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

        //TODO übernehmen 1.18.2
        @Nullable
        public ScreenRectangle intersection(ScreenRectangle rectangle) {
            int $$1 = Math.max(this.left(), rectangle.left());
            int $$2 = Math.max(this.top(), rectangle.top());
            int $$3 = Math.min(this.right(), rectangle.right());
            int $$4 = Math.min(this.bottom(), rectangle.bottom());
            return $$1 < $$3 && $$2 < $$4 ? new ScreenRectangle($$1, $$2, $$3 - $$1, $$4 - $$2) : null;
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

    //TODO übernehmen 1.18.2
    private record ScissorLayer(@NotNull ScreenRectangle rectangle) {
    }

}
