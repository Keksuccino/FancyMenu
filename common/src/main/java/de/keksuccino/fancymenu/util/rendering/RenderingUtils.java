package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphics;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScissorStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RenderingUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/fully_transparent.png");

    private static final List<DeferredScreenRenderingTask> DEFERRED_SCREEN_RENDERING_TASKS = new ArrayList<>();
    private static boolean lockDepthTest = false;
    private static boolean blurBlocked = false;
    private static boolean tooltipRenderingBlocked = false;
    private static int overrideBackgroundBlurRadius = -1000;

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

    public static void setOverrideBackgroundBlurRadius(int radius) {
        overrideBackgroundBlurRadius = radius;
    }

    public static void resetOverrideBackgroundBlurRadius() {
        overrideBackgroundBlurRadius = -1000;
    }

    public static boolean shouldOverrideBackgroundBlurRadius() {
        return overrideBackgroundBlurRadius != -1000;
    }

    public static int getOverrideBackgroundBlurRadius() {
        return overrideBackgroundBlurRadius;
    }

    public static void setDepthTestLocked(boolean locked) {
        lockDepthTest = locked;
    }

    public static boolean isDepthTestLocked() {
        return lockDepthTest;
    }

    public static void setMenuBlurringBlocked(boolean blocked) {
        blurBlocked = blocked;
    }

    public static boolean isMenuBlurringBlocked() {
        return blurBlocked;
    }

    public static void setTooltipRenderingBlocked(boolean blocked) {
        tooltipRenderingBlocked = blocked;
    }

    public static boolean isTooltipRenderingBlocked() {
        return tooltipRenderingBlocked;
    }

    public static void addDeferredScreenRenderingTask(@NotNull DeferredScreenRenderingTask task) {
        DEFERRED_SCREEN_RENDERING_TASKS.add(task);
    }

    @NotNull
    public static List<DeferredScreenRenderingTask> getDeferredScreenRenderingTasks() {
        return new ArrayList<>(DEFERRED_SCREEN_RENDERING_TASKS);
    }

    public static void executeAndClearDeferredScreenRenderingTasks(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        List<DeferredScreenRenderingTask> tasks = getDeferredScreenRenderingTasks();
        DEFERRED_SCREEN_RENDERING_TASKS.clear();
        tasks.forEach(task -> task.render(graphics, mouseX, mouseY, partial));
    }

    /**
     * Draws a textured quad, mirrored horizontally.
     * <p>
     * This is achieved by applying a negative horizontal scale to the transformation matrix before drawing.
     *
     * @param graphics      The GuiGraphics context, which manages transformations and rendering.
     * @param atlasLocation The texture resource location.
     * @param x             The x coordinate on screen.
     * @param y             The y coordinate on screen.
     * @param u             The u coordinate in the texture (top-left of the sprite).
     * @param v             The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth   The width of the sprite quad on screen and in the texture.
     * @param spriteHeight  The height of the sprite quad on screen and in the texture.
     * @param textureWidth  The total width of the texture atlas.
     * @param textureHeight The total height of the texture atlas.
     */
    public static void blitMirrored(@NotNull GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight) {
        // Delegate to the scaled version with a default white tint (-1)
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, -1);
    }

    /**
     * Draws a textured quad with a color tint, mirrored horizontally.
     * <p>
     * This is achieved by applying a negative horizontal scale to the transformation matrix before drawing.
     *
     * @param graphics      The GuiGraphics context, which manages transformations and rendering.
     * @param atlasLocation The texture resource location.
     * @param x             The x coordinate on screen.
     * @param y             The y coordinate on screen.
     * @param u             The u coordinate in the texture (top-left of the sprite).
     * @param v             The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth   The width of the sprite quad on screen and in the texture.
     * @param spriteHeight  The height of the sprite quad on screen and in the texture.
     * @param textureWidth  The total width of the texture atlas.
     * @param textureHeight The total height of the texture atlas.
     * @param colorTint     The color tint to apply (ARGB format).
     */
    public static void blitMirrored(@NotNull GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight, int colorTint) {
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, colorTint);
    }

    /**
     * Draws a textured quad scaled to a specific render size, mirrored horizontally.
     * <p>
     * The mirroring is performed by manipulating the transformation stack within {@link GuiGraphics}.
     * The context is translated to the right edge of the target location, scaled by -1 on the X-axis,
     * and then the texture is drawn normally at the new, mirrored origin. This correctly uses the
     * 2D transformation methods from JOML's {@link Matrix3x2f}.
     *
     * @param graphics      The GuiGraphics context.
     * @param atlasLocation The texture resource location.
     * @param x             The x coordinate on screen (top-left of the rendered quad).
     * @param y             The y coordinate on screen (top-left of the rendered quad).
     * @param u             The u coordinate in the texture (top-left of the source sprite region).
     * @param v             The v coordinate in the texture (top-left of the source sprite region).
     * @param spriteWidth   The width of the source sprite region in the texture atlas.
     * @param spriteHeight  The height of the source sprite region in the texture atlas.
     * @param renderWidth   The desired width of the quad to render on screen.
     * @param renderHeight  The desired height of the quad to render on screen.
     * @param textureWidth  The total width of the texture atlas.
     * @param textureHeight The total height of the texture atlas.
     * @param color         The color tint to apply (ARGB format, -1 for white/no tint).
     */
    public static void blitMirroredScaled(@NotNull GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int renderWidth, int renderHeight, int textureWidth, int textureHeight, int color) {

        // Push the current transformation matrix onto the stack to isolate our changes.
        graphics.pose().pushMatrix();

        // 1. Translate the coordinate system's origin to the TOP-RIGHT corner of our target render area.
        //    All subsequent drawing operations will be relative to this new origin.
        graphics.pose().translate((float)x + (float)renderWidth, (float)y);

        // 2. Scale the coordinate system. A negative x-scale flips the x-axis.
        //    Now, drawing with a positive width will extend to the LEFT from the origin.
        graphics.pose().scale(-1.0f, 1.0f);

        // 3. Blit the texture.
        //    We draw at the new, transformed origin (0, 0).
        //    Because the coordinate system is flipped, a quad of `renderWidth` drawn at 0
        //    will occupy the screen space from [x + renderWidth] to [x + renderWidth - renderWidth],
        //    which is [x + renderWidth] to [x]. This achieves the horizontal mirror.
        //    We use a blit overload that handles separate source and render dimensions.
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                atlasLocation,
                0, 0, // Draw at the new (0,0) of our transformed matrix
                (float) u, (float) v, // Top-left corner of the texture region to draw (in pixels)
                renderWidth, renderHeight, // The size to draw on screen
                spriteWidth, spriteHeight, // The size of the source texture region
                textureWidth, textureHeight,
                color
        );

        // Pop the matrix from the stack to restore the original transformation state.
        graphics.pose().popMatrix();

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
        blitRepeat(graphics, RenderPipelines.GUI_TEXTURED, location, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, color);
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
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull RenderPipeline renderType, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight, int color) {
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

        blitNineSlicedTexture(graphics, RenderPipelines.GUI_TEXTURED, texture, x, y, width, height, textureWidth, textureHeight, borderTop, borderRight, borderBottom, borderLeft, color);

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
    public static void blitNineSlicedTexture(GuiGraphics graphics, @NotNull RenderPipeline renderType, ResourceLocation texture, int x, int y, int width, int height,
                                             int textureWidth, int textureHeight,
                                             int borderTop, int borderRight, int borderBottom, int borderLeft, int color) {

        // Correct border sizes if they're too large
        if (borderLeft + borderRight >= textureWidth) {
            float scale = (float)(textureWidth - 2) / (borderLeft + borderRight);
            borderLeft = (int)(borderLeft * scale);
            borderRight = (int)(borderRight * scale);
        }
        if (borderTop + borderBottom >= textureHeight) {
            float scale = (float)(textureHeight - 2) / (borderTop + borderBottom);
            borderTop = (int)(borderTop * scale);
            borderBottom = (int)(borderBottom * scale);
        }

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
        submitColoredRectangle(graphics, RenderPipelines.GUI, TextureSetup.noTexture(), minX, minY, maxX, maxY, color, null);
    }

    public static void fillF(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, float z, int color) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        if (minX < maxX) {
            float i = minX;
            minX = maxX;
            maxX = i;
        }
        if (minY < maxY) {
            float i = minY;
            minY = maxY;
            maxY = i;
        }
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());
        vertexConsumer.addVertex(matrix4f, (float)minX, (float)minY, (float)z).setColor(color);
        vertexConsumer.addVertex(matrix4f, (float)minX, (float)maxY, (float)z).setColor(color);
        vertexConsumer.addVertex(matrix4f, (float)maxX, (float)maxY, (float)z).setColor(color);
        vertexConsumer.addVertex(matrix4f, (float)maxX, (float)minY, (float)z).setColor(color);
        graphics.flush();
    }

    public static void blitF(@NotNull GuiGraphics graphics, RenderPipeline renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, int color) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$6, $$7, $$8, $$9, color);
    }

    public static void blitF(@NotNull GuiGraphics graphics, RenderPipeline renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$6, $$7, $$8, $$9);
    }

    public static void blitF(@NotNull GuiGraphics graphics, RenderPipeline renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11) {
        blitF(graphics, renderTypeFunc, location, $$2, $$3, $$4, $$5, $$6, $$7, $$8, $$9, $$10, $$11, -1);
    }

    public static void blitF(@NotNull GuiGraphics graphics, RenderPipeline renderTypeFunc, ResourceLocation location, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11, int color) {
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

    private static void innerBlit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f $$10 = graphics.pose().last().pose();
        BufferBuilder $$11 = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        $$11.addVertex($$10, $$1, $$3, $$5).setUv($$6, $$8);
        $$11.addVertex($$10, $$1, $$4, $$5).setUv($$6, $$9);
        $$11.addVertex($$10, $$2, $$4, $$5).setUv($$7, $$9);
        $$11.addVertex($$10, $$2, $$3, $$5).setUv($$7, $$8);
        BufferUploader.drawWithShader(Objects.requireNonNull($$11.build()));
        graphics.flush();
    }

    private static void submitBlit(@NotNull GuiGraphics graphics, RenderPipeline pipeline, GpuTextureView textureView, float minX, float minY, float maxX, float maxY, float minU, float maxU, float minV, float maxV, int color) {
        ScreenRectangle scissorStackPeek = ((IMixinScissorStack)((IMixinGuiGraphics)graphics).get_scissorStack_FancyMenu()).invoke_peek_FancyMenu();
        ((IMixinGuiGraphics)graphics).get_guiRenderState_FancyMenu().submitGuiElement(
                        new FloatBlitRenderState(
                                pipeline, TextureSetup.singleTexture(textureView), new Matrix3x2f(graphics.pose()), minX, minY, maxX, maxY, minU, maxU, minV, maxV, color, scissorStackPeek
                        )
                );
    }

    public static void blendFuncSeparate(SourceFactor sourceFactor, DestFactor destFactor, SourceFactor sourceFactor2, DestFactor destFactor2) {
        RenderSystem.assertOnRenderThread();
        GlStateManager._blendFuncSeparate(sourceFactor.value, destFactor.value, sourceFactor2.value, destFactor2.value);
    }

    public static void defaultBlendFunc() {
        blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
    }

    public static enum SourceFactor {
        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_ALPHA_SATURATE(776),
        SRC_COLOR(768),
        ZERO(0);

        public final int value;

        private SourceFactor(final int value) {
            this.value = value;
        }
    }

    public static enum DestFactor {
        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_COLOR(768),
        ZERO(0);

        public final int value;

        private DestFactor(final int value) {
            this.value = value;
        }
    }

    @FunctionalInterface
    public interface DeferredScreenRenderingTask {
        void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);
    }

}