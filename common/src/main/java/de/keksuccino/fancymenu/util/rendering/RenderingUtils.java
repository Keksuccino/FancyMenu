package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMinecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.MatrixUtils;
import de.keksuccino.fancymenu.util.rendering.gui.ScreenRectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RenderingUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = new ResourceLocation("fancymenu", "textures/fully_transparent.png");

    private static final List<DeferredScreenRenderingTask> DEFERRED_SCREEN_RENDERING_TASKS = new ArrayList<>();
    private static boolean lockDepthTest = false;
    private static boolean tooltipRenderingBlocked = false;

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

    public static void setDepthTestLocked(boolean locked) {
        lockDepthTest = locked;
    }

    public static boolean isDepthTestLocked() {
        return lockDepthTest;
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
     * Draws a textured quad with the texture mirrored horizontally by explicitly flipping the texture coordinates.
     * Uses the standard sprite width and height for both texture coordinates and rendering dimensions.
     *
     * @param graphics       The GuiGraphics context.
     * @param atlasLocation  The texture resource location.
     * @param x              The x coordinate on screen.
     * @param y              The y coordinate on screen.
     * @param u              The u coordinate in the texture (top-left of the sprite).
     * @param v              The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth    The width of the sprite quad on screen and in the texture.
     * @param spriteHeight   The height of the sprite quad on screen and in the texture.
     * @param textureWidth   The total width of the texture atlas.
     * @param textureHeight  The total height of the texture atlas.
     */
    public static void blitMirrored(@NotNull GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight) {
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, -1);
    }

    /**
     * Draws a textured quad with the texture mirrored horizontally by explicitly flipping the texture coordinates.
     * Uses the standard sprite width and height for both texture coordinates and rendering dimensions.
     *
     * @param graphics       The GuiGraphics context.
     * @param atlasLocation  The texture resource location.
     * @param x              The x coordinate on screen.
     * @param y              The y coordinate on screen.
     * @param u              The u coordinate in the texture (top-left of the sprite).
     * @param v              The v coordinate in the texture (top-left of the sprite).
     * @param spriteWidth    The width of the sprite quad on screen and in the texture.
     * @param spriteHeight   The height of the sprite quad on screen and in the texture.
     * @param textureWidth   The total width of the texture atlas.
     * @param textureHeight  The total height of the texture atlas.
     */
    public static void blitMirrored(@NotNull GuiGraphics graphics, ResourceLocation atlasLocation, int x, int y, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight, int colorTint) {
        blitMirroredScaled(graphics, atlasLocation, x, y, u, v, spriteWidth, spriteHeight, spriteWidth, spriteHeight, textureWidth, textureHeight, colorTint);
    }

    /**
     * Draws a textured quad scaled to a specific render size, with the texture mirrored horizontally
     * by explicitly flipping the texture coordinates.
     *
     * @param graphics       The GuiGraphics context.
     * @param atlasLocation  The texture resource location.
     * @param x              The x coordinate on screen (top-left of the rendered quad).
     * @param y              The y coordinate on screen (top-left of the rendered quad).
     * @param u              The u coordinate in the texture (top-left of the source sprite region).
     * @param v              The v coordinate in the texture (top-left of the source sprite region).
     * @param spriteWidth    The width of the source sprite region in the texture atlas.
     * @param spriteHeight   The height of the source sprite region in the texture atlas.
     * @param renderWidth    The desired width of the quad to render on screen.
     * @param renderHeight   The desired height of the quad to render on screen.
     * @param textureWidth   The total width of the texture atlas.
     * @param textureHeight  The total height of the texture atlas.
     * @param color          The color tint to apply (ARGB format, -1 for white/no tint).
     */
    public static void blitMirroredScaled(
            @NotNull GuiGraphics graphics,
            ResourceLocation atlasLocation,
            int x, int y,          // Screen position
            int u, int v,          // Texture region origin in atlas
            int spriteWidth, int spriteHeight, // Original sprite size in atlas for UV calculation
            int renderWidth, int renderHeight, // Target size on screen
            int textureWidth, int textureHeight, // Total atlas size
            int color             // Tint color
    ) {
        // Calculate texture coordinates based on the original sprite dimensions
        float minU = (float)u / (float)textureWidth;
        float maxU = (float)(u + spriteWidth) / (float)textureWidth; // Use spriteWidth for UVs
        float minV = (float)v / (float)textureHeight;
        float maxV = (float)(v + spriteHeight) / (float)textureHeight; // Use spriteHeight for UVs

        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();

        // Access rendering internals
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder consumer = Tesselator.getInstance().getBuilder();
        consumer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // Add vertices with screen dimensions using renderWidth/renderHeight,
        // but texture coordinates (UVs) swapped horizontally (minU/maxU flipped)
        consumer.vertex(matrix4f, (float)x,                 (float)y,                  0.0F).uv(maxU, minV).color(color); // Top-left screen -> Top-right texture UV (maxU, minV)
        consumer.vertex(matrix4f, (float)x,                 (float)(y + renderHeight), 0.0F).uv(maxU, maxV).color(color); // Bottom-left screen -> Bottom-right texture UV (maxU, maxV)
        consumer.vertex(matrix4f, (float)(x + renderWidth), (float)(y + renderHeight), 0.0F).uv(minU, maxV).color(color); // Bottom-right screen -> Bottom-left texture UV (minU, maxV)
        consumer.vertex(matrix4f, (float)(x + renderWidth), (float)y,                  0.0F).uv(minU, minV).color(color); // Top-right screen -> Top-left texture UV (minU, minV)

        BufferUploader.drawWithShader(consumer.end());
        RenderSystem.disableBlend();

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
        graphics.blit(location, x, y, 0.0F, 0.0F, areaRenderWidth, areaRenderHeight, texWidth, texHeight);
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
     */
    public static void blitNineSlicedTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                             int textureWidth, int textureHeight,
                                             int borderTop, int borderRight, int borderBottom, int borderLeft) {

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
        graphics.blit(texture, x, y, 0, 0, borderLeft, borderTop, textureWidth, textureHeight);
        // Top right
        graphics.blit(texture, x + width - borderRight, y, textureWidth - borderRight, 0, borderRight, borderTop, textureWidth, textureHeight);
        // Bottom left
        graphics.blit(texture, x, y + height - borderBottom, 0, textureHeight - borderBottom, borderLeft, borderBottom, textureWidth, textureHeight);
        // Bottom right
        graphics.blit(texture, x + width - borderRight, y + height - borderBottom, textureWidth - borderRight, textureHeight - borderBottom, borderRight, borderBottom, textureWidth, textureHeight);

        // Edges - Tiled
        int centerWidth = textureWidth - borderLeft - borderRight;
        int centerHeight = textureHeight - borderTop - borderBottom;

        // Top edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(texture, x + i, y, borderLeft, 0, pieceWidth, borderTop, textureWidth, textureHeight);
        }

        // Bottom edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(texture, x + i, y + height - borderBottom, borderLeft, textureHeight - borderBottom, pieceWidth, borderBottom, textureWidth, textureHeight);
        }

        // Left edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(texture, x, y + j, 0, borderTop, borderLeft, pieceHeight, textureWidth, textureHeight);
        }

        // Right edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(texture, x + width - borderRight, y + j, textureWidth - borderRight, borderTop, borderRight, pieceHeight, textureWidth, textureHeight);
        }

        // Center - Tiled
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
                int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
                graphics.blit(texture, x + i, y + j, borderLeft, borderTop, pieceWidth, pieceHeight, textureWidth, textureHeight);
            }
        }

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

    public static void setShaderColor(GuiGraphics graphics, int color, float alpha) {
        float red = (float) FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
    }

    public static void setShaderColor(GuiGraphics graphics, int color) {
        float red = (float) FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
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

    public static void blitF(@NotNull GuiGraphics graphics, ResourceLocation location, float x, float y, float f3, float f4, float width, float height, float width2, float height2, int color) {
        float red = (float)FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
        blit(graphics, location, x, y, width, height, f3, f4, width, height, width2, height2);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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

    public static void enableScissor(@NotNull GuiGraphics graphics, int minX, int minY, int maxX, int maxY) {
        ScreenRectangle r = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);
        r = transformAxisAligned(r, graphics.pose().last().pose());
        graphics.enableScissor(r.left(), r.top(), r.right(), r.bottom());
    }

    public static void disableScissor(@NotNull GuiGraphics graphics) {
        graphics.disableScissor();
    }

    @NotNull
    public static ScreenRectangle transformAxisAligned(@NotNull ScreenRectangle toTransform, @NotNull Matrix4f pose) {
        if (isMatrixIdentity(pose)) {
            return toTransform;
        } else {
            org.joml.Matrix4f jomlPose = MatrixUtils.convertToJoml(pose);
            Vector3f vector3f = jomlPose.transformPosition((float)toTransform.left(), (float)toTransform.top(), 0.0F, new Vector3f());
            Vector3f vector3f2 = jomlPose.transformPosition((float)toTransform.right(), (float)toTransform.bottom(), 0.0F, new Vector3f());
            return new ScreenRectangle(Mth.floor(vector3f.x), Mth.floor(vector3f.y), Mth.floor(vector3f2.x - vector3f.x), Mth.floor(vector3f2.y - vector3f.y));
        }
    }

    public static boolean isMatrixIdentity(Matrix4f matrix) {
        return MatrixUtils.isMatrixIdentityMojang(matrix);
    }

    @FunctionalInterface
    public interface DeferredScreenRenderingTask {
        void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);
    }

}