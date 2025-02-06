package de.keksuccino.fancymenu.util.rendering.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import de.keksuccino.fancymenu.util.ObjectUtils;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;

@SuppressWarnings("unused")
public class GuiGraphics {

    public static final Screen DUMMY_SCREEN = ObjectUtils.build(() -> {
        Screen s = new TitleScreen();
        s.init(Minecraft.getInstance(), 1000, 1000);
        return s;
    });
    public static final GuiComponent GUI_COMPONENT = new GuiComponent() {
        @Override
        protected void hLine(PoseStack $$0, int $$1, int $$2, int $$3, int $$4) {
            super.hLine($$0, $$1, $$2, $$3, $$4);
        }
    };

    public static final float MAX_GUI_Z = 10000.0F;
    public static final float MIN_GUI_Z = -10000.0F;
    private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
    private final Minecraft minecraft;
    private final PoseStack pose;
    private final MultiBufferSource.BufferSource bufferSource;
    private final GuiGraphics.ScissorStack scissorStack = new GuiGraphics.ScissorStack();
    private boolean managed;

    protected static GuiGraphics currentGraphics = null;

    @NotNull
    public static GuiGraphics updateGraphicsAndGet(@NotNull PoseStack pose, @NotNull MultiBufferSource.BufferSource bufferSource) {
        currentGraphics = new GuiGraphics(Minecraft.getInstance(), pose, bufferSource);
        return currentGraphics;
    }

    public static GuiGraphics currentGraphics() {
        return currentGraphics;
    }

    private GuiGraphics(Minecraft minecraft, PoseStack pose, MultiBufferSource.BufferSource bufferSource) {
        this.minecraft = minecraft;
        this.pose = pose;
        this.bufferSource = bufferSource;
    }

    public GuiGraphics(Minecraft minecraft, MultiBufferSource.BufferSource bufferSource) {
        this(minecraft, new PoseStack(), bufferSource);
    }

    public MultiBufferSource.BufferSource getBufferSource() {
        return this.bufferSource;
    }

    /**
     * Executes a runnable while managing the render state. The render state is flushed before and after executing the runnable.
     *
     * @param runnable the runnable to execute.
     */
    public void drawManaged(Runnable runnable) {
        this.flush();
        this.managed = true;
        runnable.run();
        this.managed = false;
        this.flush();
    }

    /**
     * Flushes the render state if it is not managed.
     */
    private void flushIfUnmanaged() {
        if (!this.managed) {
            this.flush();
        }
    }

    /**
     * Flushes the render state if it is managed.
     */
    private void flushIfManaged() {
        if (this.managed) {
            this.flush();
        }
    }

    /**
     * {@return returns the width of the GUI screen in pixels}
     */
    public int guiWidth() {
        return this.minecraft.getWindow().getGuiScaledWidth();
    }

    /**
     * {@return returns the height of the GUI screen in pixels}
     */
    public int guiHeight() {
        return this.minecraft.getWindow().getGuiScaledHeight();
    }

    /**
     * {@return returns the PoseStack used for transformations and rendering.}
     */
    public PoseStack pose() {
        return this.pose;
    }

    /**
     * {@return returns the buffer source for rendering.}
     */
    public MultiBufferSource.BufferSource bufferSource() {
        return this.bufferSource;
    }

    /**
     * Flushes the render state, ending the current batch and enabling depth testing.
     */
    public void flush() {
        RenderSystem.disableDepthTest();
        this.bufferSource.endBatch();
        RenderSystem.enableDepthTest();
    }

    /**
     * Draws a horizontal line from minX to maxX at the specified y-coordinate with the given color.
     *
     * @param minX the x-coordinate of the start point.
     * @param maxX the x-coordinate of the end point.
     * @param y the y-coordinate of the line.
     * @param color the color of the line.
     */
    public void hLine(int minX, int maxX, int y, int color) {
        this.hLine(GuiRenderTypes.gui(), minX, maxX, y, color);
    }

    /**
     * Draws a horizontal line from minX to maxX at the specified y-coordinate with the given color using the specified render type.
     *
     * @param renderType the render type to use.
     * @param minX the x-coordinate of the start point.
     * @param maxX the x-coordinate of the end point.
     * @param y the y-coordinate of the line.
     * @param color the color of the line.
     */
    public void hLine(RenderType renderType, int minX, int maxX, int y, int color) {
        if (maxX < minX) {
            int i = minX;
            minX = maxX;
            maxX = i;
        }

        this.fill(renderType, minX, y, maxX + 1, y + 1, color);
    }

    /**
     * Draws a vertical line from minY to maxY at the specified x-coordinate with the given color.
     *
     * @param x the x-coordinate of the line.
     * @param minY the y-coordinate of the start point.
     * @param maxY the y-coordinate of the end point.
     * @param color the color of the line.
     */
    public void vLine(int x, int minY, int maxY, int color) {
        this.vLine(GuiRenderTypes.gui(), x, minY, maxY, color);
    }

    /**
     * Draws a vertical line from minY to maxY at the specified x-coordinate with the given color using the specified render type.
     *
     * @param renderType the render type to use.
     * @param x the x-coordinate of the line.
     * @param minY the y-coordinate of the start point.
     * @param maxY the y-coordinate of the end point.
     * @param color the color of the line.
     */
    public void vLine(RenderType renderType, int x, int minY, int maxY, int color) {
        if (maxY < minY) {
            int i = minY;
            minY = maxY;
            maxY = i;
        }

        this.fill(renderType, x, minY + 1, x + 1, maxY, color);
    }

    /**
     * Enables scissoring with the specified screen coordinates.
     *
     * @param minX the minimum x-coordinate of the scissor region.
     * @param minY the minimum y-coordinate of the scissor region.
     * @param maxX the maximum x-coordinate of the scissor region.
     * @param maxY the maximum y-coordinate of the scissor region.
     */
    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        this.applyScissor(this.scissorStack.push(new ScreenRectangle(minX, minY, maxX - minX, maxY - minY)));
    }

    /**
     * Disables scissoring.
     */
    public void disableScissor() {
        this.applyScissor(this.scissorStack.pop());
    }

    /**
     * Applies scissoring based on the provided screen rectangle.
     *
     * @param rectangle the screen rectangle to apply scissoring with. Can be null to disable scissoring.
     */
    private void applyScissor(@Nullable ScreenRectangle rectangle) {
        this.flushIfManaged();
        if (rectangle != null) {
            Window window = Minecraft.getInstance().getWindow();
            int i = window.getHeight();
            double d = window.getGuiScale();
            double e = (double)rectangle.left() * d;
            double f = (double)i - (double)rectangle.bottom() * d;
            double g = (double)rectangle.width() * d;
            double h = (double)rectangle.height() * d;
            RenderSystem.enableScissor((int)e, (int)f, Math.max(0, (int)g), Math.max(0, (int)h));
        } else {
            RenderSystem.disableScissor();
        }
    }

    /**
     * Sets the current rendering color.
     *
     * @param red the red component of the color.
     * @param green the green component of the color.
     * @param blue the blue component of the color.
     * @param alpha the alpha component of the color.
     */
    public void setColor(float red, float green, float blue, float alpha) {
        this.flushIfManaged();
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    /**
     * Fills a rectangle with the specified color using the given coordinates as the boundaries.
     *
     * @param minX the minimum x-coordinate of the rectangle.
     * @param minY the minimum y-coordinate of the rectangle.
     * @param maxX the maximum x-coordinate of the rectangle.
     * @param maxY the maximum y-coordinate of the rectangle.
     * @param color the color to fill the rectangle with.
     */
    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        this.fill(minX, minY, maxX, maxY, 0, color);
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given coordinates as the boundaries.
     *
     * @param minX the minimum x-coordinate of the rectangle.
     * @param minY the minimum y-coordinate of the rectangle.
     * @param maxX the maximum x-coordinate of the rectangle.
     * @param maxY the maximum y-coordinate of the rectangle.
     * @param z the z-level of the rectangle.
     * @param color the color to fill the rectangle with.
     */
    public void fill(int minX, int minY, int maxX, int maxY, int z, int color) {
        this.fill(GuiRenderTypes.gui(), minX, minY, maxX, maxY, z, color);
    }

    /**
     * Fills a rectangle with the specified color using the given render type and coordinates as the boundaries.
     *
     * @param renderType the render type to use.
     * @param minX the minimum x-coordinate of the rectangle.
     * @param minY the minimum y-coordinate of the rectangle.
     * @param maxX the maximum x-coordinate of the rectangle.
     * @param maxY the maximum y-coordinate of the rectangle.
     * @param color the color to fill the rectangle with.
     */
    public void fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int color) {
        this.fill(renderType, minX, minY, maxX, maxY, 0, color);
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given render type and coordinates as the boundaries.
     *
     * @param renderType the render type to use.
     * @param minX the minimum x-coordinate of the rectangle.
     * @param minY the minimum y-coordinate of the rectangle.
     * @param maxX the maximum x-coordinate of the rectangle.
     * @param maxY the maximum y-coordinate of the rectangle.
     * @param z the z-level of the rectangle.
     * @param color the color to fill the rectangle with.
     */
    public void fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int z, int color) {
        Matrix4f matrix4F = this.pose.last().pose();
        if (minX < maxX) {
            int i = minX;
            minX = maxX;
            maxX = i;
        }

        if (minY < maxY) {
            int i = minY;
            minY = maxY;
            maxY = i;
        }

        float f = (float)FastColor.ARGB32.alpha(color) / 255.0F;
        float g = (float)FastColor.ARGB32.red(color) / 255.0F;
        float h = (float)FastColor.ARGB32.green(color) / 255.0F;
        float j = (float)FastColor.ARGB32.blue(color) / 255.0F;
        VertexConsumer vertexConsumer = this.bufferSource.getBuffer(renderType);
        vertexConsumer.vertex(matrix4F, (float)minX, (float)minY, (float)z).color(g, h, j, f).endVertex();
        vertexConsumer.vertex(matrix4F, (float)minX, (float)maxY, (float)z).color(g, h, j, f).endVertex();
        vertexConsumer.vertex(matrix4F, (float)maxX, (float)maxY, (float)z).color(g, h, j, f).endVertex();
        vertexConsumer.vertex(matrix4F, (float)maxX, (float)minY, (float)z).color(g, h, j, f).endVertex();
        this.flushIfUnmanaged();
    }

    /**
     * Fills a rectangle with a gradient color from colorFrom to colorTo using the given coordinates as the boundaries.
     *
     * @param x1 the x-coordinate of the first corner of the rectangle.
     * @param y1 the y-coordinate of the first corner of the rectangle.
     * @param x2 the x-coordinate of the second corner of the rectangle.
     * @param y2 the y-coordinate of the second corner of the rectangle.
     * @param colorFrom the starting color of the gradient.
     * @param colorTo the ending color of the gradient.
     */
    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        this.fillGradient(x1, y1, x2, y2, 0, colorFrom, colorTo);
    }

    /**
     * Fills a rectangle with a gradient color from colorFrom to colorTo at the specified z-level using the given coordinates as the boundaries.
     *
     * @param x1 the x-coordinate of the first corner of the rectangle.
     * @param y1 the y-coordinate of the first corner of the rectangle.
     * @param x2 the x-coordinate of the second corner of the rectangle.
     * @param y2 the y-coordinate of the second corner of the rectangle.
     * @param z the z-level of the rectangle.
     * @param colorFrom the starting color of the gradient.
     * @param colorTo the ending color of the gradient.
     */
    public void fillGradient(int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo) {
        this.fillGradient(GuiRenderTypes.gui(), x1, y1, x2, y2, colorFrom, colorTo, z);
    }

    /**
     * Fills a rectangle with a gradient color from colorFrom to colorTo at the specified z-level using the given render type and coordinates as the boundaries.
     *
     * @param renderType the render type to use.
     * @param x1 the x-coordinate of the first corner of the rectangle.
     * @param y1 the y-coordinate of the first corner of the rectangle.
     * @param x2 the x-coordinate of the second corner of the rectangle.
     * @param y2 the y-coordinate of the second corner of the rectangle.
     * @param colorFrom the starting color of the gradient.
     * @param colorTo the ending color of the gradient.
     * @param z the z-level of the rectangle.
     */
    public void fillGradient(RenderType renderType, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, int z) {
        VertexConsumer vertexConsumer = this.bufferSource.getBuffer(renderType);
        this.fillGradient(vertexConsumer, x1, y1, x2, y2, z, colorFrom, colorTo);
        this.flushIfUnmanaged();
    }

    /**
     * The core `fillGradient` method.
     * <p>
     * Fills a rectangle with a gradient color from colorFrom to colorTo at the specified z-level using the given render type and coordinates as the boundaries.
     *
     * @param consumer the {@linkplain VertexConsumer} object for drawing the vertices on screen.
     * @param x1 the x-coordinate of the first corner of the rectangle.
     * @param y1 the y-coordinate of the first corner of the rectangle.
     * @param x2 the x-coordinate of the second corner of the rectangle.
     * @param y2 the y-coordinate of the second corner of the rectangle.
     * @param z the z-level of the rectangle.
     * @param colorFrom the starting color of the gradient.
     * @param colorTo the ending color of the gradient.
     */
    private void fillGradient(VertexConsumer consumer, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo) {
        float f = (float)FastColor.ARGB32.alpha(colorFrom) / 255.0F;
        float g = (float)FastColor.ARGB32.red(colorFrom) / 255.0F;
        float h = (float)FastColor.ARGB32.green(colorFrom) / 255.0F;
        float i = (float)FastColor.ARGB32.blue(colorFrom) / 255.0F;
        float j = (float)FastColor.ARGB32.alpha(colorTo) / 255.0F;
        float k = (float)FastColor.ARGB32.red(colorTo) / 255.0F;
        float l = (float)FastColor.ARGB32.green(colorTo) / 255.0F;
        float m = (float)FastColor.ARGB32.blue(colorTo) / 255.0F;
        Matrix4f matrix4F = this.pose.last().pose();
        consumer.vertex(matrix4F, (float)x1, (float)y1, (float)z).color(g, h, i, f).endVertex();
        consumer.vertex(matrix4F, (float)x1, (float)y2, (float)z).color(k, l, m, j).endVertex();
        consumer.vertex(matrix4F, (float)x2, (float)y2, (float)z).color(k, l, m, j).endVertex();
        consumer.vertex(matrix4F, (float)x2, (float)y1, (float)z).color(g, h, i, f).endVertex();
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, text, and color.
     *
     * @param font the font to use for rendering.
     * @param text the text to draw.
     * @param x the x-coordinate of the center of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     */
    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        this.drawString(font, text, x - font.width(text) / 2, y, color);
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, text component, and color.
     *
     * @param font the font to use for rendering.
     * @param text the text component to draw.
     * @param x the x-coordinate of the center of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     */
    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        FormattedCharSequence formattedCharSequence = text.getVisualOrderText();
        this.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color);
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, formatted character sequence, and color.
     *
     * @param font the font to use for rendering.
     * @param text the formatted character sequence to draw.
     * @param x the x-coordinate of the center of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     */
    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.drawString(font, text, x - font.width(text) / 2, y, color);
    }

    /**
     * Draws a string at the specified coordinates using the given font, text, and color. Returns the width of the drawn string.
     * <p>
     * @return the width of the drawn string.
     *
     * @param font the font to use for rendering.
     * @param text the text to draw.
     * @param x the x-coordinate of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     */
    public int drawString(Font font, @Nullable String text, int x, int y, int color) {
        return this.drawString(font, text, x, y, color, true);
    }

    /**
     * Draws a string at the specified coordinates using the given font, text, color, and drop shadow. Returns the width of the drawn string.
     * <p>
     * @return the width of the drawn string.
     *
     * @param font the font to use for rendering.
     * @param text the text to draw.
     * @param x the x-coordinate of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     * @param dropShadow whether to apply a drop shadow to the string.
     */
    public int drawString(Font font, @Nullable String text, int x, int y, int color, boolean dropShadow) {
        if (text == null) {
            return 0;
        } else {
            boolean seeThrough = false;
            int i = font.drawInBatch(text, (float)x, (float)y, color, dropShadow, this.pose.last().pose(), this.bufferSource, seeThrough, 0, 15728880, font.isBidirectional());
            this.flushIfUnmanaged();
            return i;
        }
    }

    /**
     * Draws a formatted character sequence at the specified coordinates using the given font, text, and color. Returns the width of the drawn string.
     * <p>
     * @return the width of the drawn string.
     *
     * @param font the font to use for rendering.
     * @param text the formatted character sequence to draw.
     * @param x the x-coordinate of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string
     */
    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        return this.drawString(font, text, x, y, color, true);
    }

    /**
     * Draws a formatted character sequence at the specified coordinates using the given font, text, color, and drop shadow. Returns the width of the drawn string.
     * <p>
     * @return returns the width of the drawn string.
     *
     * @param font the font to use for rendering.
     * @param text the formatted character sequence to draw.
     * @param x the x-coordinate of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     * @param dropShadow whether to apply a drop shadow to the string.
     */
    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow) {
        boolean seeThrough = false;
        int i = font.drawInBatch(text, (float)x, (float)y, color, dropShadow, this.pose.last().pose(), this.bufferSource, seeThrough, 0, 15728880);
        this.flushIfUnmanaged();
        return i;
    }

    /**
     * Draws a component's visual order text at the specified coordinates using the given font, text component, and color.
     * <p>
     * @return the width of the drawn string.
     *
     * @param font the font to use for rendering.
     * @param text the text component to draw.
     * @param x the x-coordinate of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     */
    public int drawString(Font font, Component text, int x, int y, int color) {
        return this.drawString(font, text, x, y, color, true);
    }

    /**
     * Draws a component's visual order text at the specified coordinates using the given font, text component, color, and drop shadow.
     * <p>
     * @return the width of the drawn string.
     *
     * @param font the font to use for rendering.
     * @param text the text component to draw.
     * @param x the x-coordinate of the string.
     * @param y the y-coordinate of the string.
     * @param color the color of the string.
     * @param dropShadow whether to apply a drop shadow to the string.
     */
    public int drawString(Font font, Component text, int x, int y, int color, boolean dropShadow) {
        return this.drawString(font, text.getVisualOrderText(), x, y, color, dropShadow);
    }

    /**
     * Draws a formatted text with word wrapping at the specified coordinates using the given font, text, line width, and color.
     *
     * @param font the font to use for rendering.
     * @param text the formatted text to draw.
     * @param x the x-coordinate of the starting position.
     * @param y the y-coordinate of the starting position.
     * @param lineWidth the maximum width of each line before wrapping.
     * @param color the color of the text.
     */
    public void drawWordWrap(Font font, FormattedText text, int x, int y, int lineWidth, int color) {
        for (FormattedCharSequence formattedCharSequence : font.split(text, lineWidth)) {
            this.drawString(font, formattedCharSequence, x, y, color, false);
            y += 9;
        }
    }

    /**
     * Blits a portion of the specified texture atlas sprite onto the screen at the given coordinates.
     *
     * @param x the x-coordinate of the blit position.
     * @param y the y-coordinate of the blit position.
     * @param blitOffset the z-level offset for rendering order.
     * @param width the width of the blitted portion.
     * @param height the height of the blitted portion.
     * @param sprite the texture atlas sprite to blit.
     */
    public void blit(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite) {
        this.innerBlit(sprite.getName(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }

    /**
     * Blits a portion of the specified texture atlas sprite onto the screen at the given coordinates with a color tint.
     *
     * @param x the x-coordinate of the blit position.
     * @param y the y-coordinate of the blit position.
     * @param blitOffset the z-level offset for rendering order.
     * @param width the width of the blitted portion.
     * @param height the height of the blitted portion.
     * @param sprite the texture atlas sprite to blit.
     * @param red the red component of the color tint.
     * @param green the green component of the color tint.
     * @param blue the blue component of the color tint.
     * @param alpha the alpha component of the color tint.
     */
    public void blit(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, float red, float green, float blue, float alpha) {
        this.innerBlit(
                sprite.getName(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), red, green, blue, alpha
        );
    }

    /**
     * Renders an outline rectangle on the screen with the specified color.
     *
     * @param x the x-coordinate of the top-left corner of the rectangle.
     * @param y the y-coordinate of the top-left corner of the rectangle.
     * @param width the width of the blitted portion.
     * @param height the height of the rectangle.
     * @param color the color of the outline.
     */
    public void renderOutline(int x, int y, int width, int height, int color) {
        this.fill(x, y, x + width, y + 1, color);
        this.fill(x, y + height - 1, x + width, y + height, color);
        this.fill(x, y + 1, x + 1, y + height - 1, color);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x the x-coordinate of the blit position.
     * @param y the y-coordinate of the blit position.
     * @param uOffset the horizontal texture coordinate offset.
     * @param vOffset the vertical texture coordinate offset.
     * @param uWidth the width of the blitted portion in texture coordinates.
     * @param vHeight the height of the blitted portion in texture coordinates.
     */
    public void blit(ResourceLocation atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        this.blit(atlasLocation, x, y, 0, (float)uOffset, (float)vOffset, uWidth, vHeight, 256, 256);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given coordinates with a blit offset and texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x the x-coordinate of the blit position.
     * @param y the y-coordinate of the blit position.
     * @param blitOffset the z-level offset for rendering order.
     * @param uOffset the horizontal texture coordinate offset.
     * @param vOffset the vertical texture coordinate offset.
     * @param uWidth the width of the blitted portion in texture coordinates.
     * @param vHeight the height of the blitted portion in texture coordinates.
     * @param textureWidth the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(
            ResourceLocation atlasLocation, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight
    ) {
        this.blit(atlasLocation, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and dimensions with texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x the x-coordinate of the top-left corner of the blit position.
     * @param y the y-coordinate of the top-left corner of the blit position.
     * @param width the width of the blitted portion.
     * @param height the height of the blitted portion.
     * @param uOffset the horizontal texture coordinate offset.
     * @param vOffset the vertical texture coordinate offset.
     * @param uWidth the width of the blitted portion in texture coordinates.
     * @param vHeight the height of the blitted portion in texture coordinates.
     * @param textureWidth the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(
            ResourceLocation atlasLocation,
            int x,
            int y,
            int width,
            int height,
            float uOffset,
            float vOffset,
            int uWidth,
            int vHeight,
            int textureWidth,
            int textureHeight
    ) {
        this.blit(atlasLocation, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and dimensions with texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x the x-coordinate of the top-left corner of the blit position.
     * @param y the y-coordinate of the top-left corner of the blit position.
     * @param uOffset the horizontal texture coordinate offset.
     * @param vOffset the vertical texture coordinate offset.
     * @param width the width of the blitted portion.
     * @param height the height of the blitted portion.
     * @param textureWidth the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(ResourceLocation atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.blit(atlasLocation, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1 the x-coordinate of the first corner of the blit position.
     * @param x2 the x-coordinate of the second corner of the blit position.
     * @param y1 the y-coordinate of the first corner of the blit position.
     * @param y2 the y-coordinate of the second corner of the blit position.
     * @param blitOffset the z-level offset for rendering order.
     * @param uWidth the width of the blitted portion in texture coordinates.
     * @param vHeight the height of the blitted portion in texture coordinates.
     * @param uOffset the horizontal texture coordinate offset.
     * @param vOffset the vertical texture coordinate offset.
     * @param textureWidth the width of the texture.
     * @param textureHeight the height of the texture.
     */
    void blit(
            ResourceLocation atlasLocation,
            int x1,
            int x2,
            int y1,
            int y2,
            int blitOffset,
            int uWidth,
            int vHeight,
            float uOffset,
            float vOffset,
            int textureWidth,
            int textureHeight
    ) {
        this.innerBlit(
                atlasLocation,
                x1,
                x2,
                y1,
                y2,
                blitOffset,
                (uOffset + 0.0F) / (float)textureWidth,
                (uOffset + (float)uWidth) / (float)textureWidth,
                (vOffset + 0.0F) / (float)textureHeight,
                (vOffset + (float)vHeight) / (float)textureHeight
        );
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates without color tinting.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1 the x-coordinate of the first corner of the blit position.
     * @param x2 the x-coordinate of the second corner of the blit position.
     * @param y1 the y-coordinate of the first corner of the blit position.
     * @param y2 the y-coordinate of the second corner of the blit position.
     * @param blitOffset the z-level offset for rendering order.
     * @param minU the minimum horizontal texture coordinate.
     * @param maxU the maximum horizontal texture coordinate.
     * @param minV the minimum vertical texture coordinate.
     * @param maxV the maximum vertical texture coordinate.
     */
    void innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4F = this.pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix4F, (float)x1, (float)y1, (float)blitOffset).uv(minU, minV).endVertex();
        builder.vertex(matrix4F, (float)x1, (float)y2, (float)blitOffset).uv(minU, maxV).endVertex();
        builder.vertex(matrix4F, (float)x2, (float)y2, (float)blitOffset).uv(maxU, maxV).endVertex();
        builder.vertex(matrix4F, (float)x2, (float)y1, (float)blitOffset).uv(maxU, minV).endVertex();
        builder.end();
        BufferUploader.end(builder);
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates, texture coordinates, and color tint.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1 the x-coordinate of the first corner of the blit position.
     * @param x2 the x-coordinate of the second corner of the blit position.
     * @param y1 the y-coordinate of the first corner of the blit position.
     * @param y2 the y-coordinate of the second corner of the blit position.
     * @param blitOffset the z-level offset for rendering order.
     * @param minU the minimum horizontal texture coordinate.
     * @param maxU the maximum horizontal texture coordinate.
     * @param minV the minimum vertical texture coordinate.
     * @param maxV the maximum vertical texture coordinate.
     * @param red the red component of the color tint.
     * @param green the green component of the color tint.
     * @param blue the blue component of the color tint.
     * @param alpha the alpha component of the color tint.
     */
    void innerBlit(
            ResourceLocation atlasLocation,
            int x1,
            int x2,
            int y1,
            int y2,
            int blitOffset,
            float minU,
            float maxU,
            float minV,
            float maxV,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4F = this.pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        builder.vertex(matrix4F, (float)x1, (float)y1, (float)blitOffset).color(red, green, blue, alpha).uv(minU, minV).endVertex();
        builder.vertex(matrix4F, (float)x1, (float)y2, (float)blitOffset).color(red, green, blue, alpha).uv(minU, maxV).endVertex();
        builder.vertex(matrix4F, (float)x2, (float)y2, (float)blitOffset).color(red, green, blue, alpha).uv(maxU, maxV).endVertex();
        builder.vertex(matrix4F, (float)x2, (float)y1, (float)blitOffset).color(red, green, blue, alpha).uv(maxU, minV).endVertex();
        builder.end();
        BufferUploader.end(builder);
        RenderSystem.disableBlend();
    }

    public void blitNineSliced(ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p, int q) {
        this.blitNineSliced(resourceLocation, i, j, k, l, m, m, m, m, n, o, p, q);
    }

    public void blitNineSliced(ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r) {
        this.blitNineSliced(resourceLocation, i, j, k, l, m, n, m, n, o, p, q, r);
    }

    public void blitNineSliced(ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t) {
        m = Math.min(m, k / 2);
        o = Math.min(o, k / 2);
        n = Math.min(n, l / 2);
        p = Math.min(p, l / 2);
        if (k == q && l == r) {
            this.blit(resourceLocation, i, j, s, t, k, l);
        } else if (l == r) {
            this.blit(resourceLocation, i, j, s, t, m, l);
            this.blitRepeating(resourceLocation, i + m, j, k - o - m, l, s + m, t, q - o - m, r);
            this.blit(resourceLocation, i + k - o, j, s + q - o, t, o, l);
        } else if (k == q) {
            this.blit(resourceLocation, i, j, s, t, k, n);
            this.blitRepeating(resourceLocation, i, j + n, k, l - p - n, s, t + n, q, r - p - n);
            this.blit(resourceLocation, i, j + l - p, s, t + r - p, k, p);
        } else {
            this.blit(resourceLocation, i, j, s, t, m, n);
            this.blitRepeating(resourceLocation, i + m, j, k - o - m, n, s + m, t, q - o - m, n);
            this.blit(resourceLocation, i + k - o, j, s + q - o, t, o, n);
            this.blit(resourceLocation, i, j + l - p, s, t + r - p, m, p);
            this.blitRepeating(resourceLocation, i + m, j + l - p, k - o - m, p, s + m, t + r - p, q - o - m, p);
            this.blit(resourceLocation, i + k - o, j + l - p, s + q - o, t + r - p, o, p);
            this.blitRepeating(resourceLocation, i, j + n, m, l - p - n, s, t + n, m, r - p - n);
            this.blitRepeating(resourceLocation, i + m, j + n, k - o - m, l - p - n, s + m, t + n, q - o - m, r - p - n);
            this.blitRepeating(resourceLocation, i + k - o, j + n, m, l - p - n, s + q - o, t + n, o, r - p - n);
        }
    }

    public void blitRepeating(ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p) {
        int q = i;
        IntIterator intIterator = slices(k, o);

        while (intIterator.hasNext()) {
            int r = intIterator.nextInt();
            int s = (o - r) / 2;
            int t = j;
            IntIterator intIterator2 = slices(l, p);

            while (intIterator2.hasNext()) {
                int u = intIterator2.nextInt();
                int v = (p - u) / 2;
                this.blit(resourceLocation, q, t, m + s, n + v, r, u);
                t += u;
            }

            q += r;
        }
    }

    private static IntIterator slices(int i, int j) {
        int k = Mth.positiveCeilDiv(i, j);
        return new Divisor(i, k);
    }

    /**
     * Renders an item stack at the specified coordinates.
     *
     * @param stack the item stack to render.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     */
    public void renderItem(ItemStack stack, int x, int y) {
        this.renderItem(this.minecraft.player, this.minecraft.level, stack, x, y, 0);
    }

    /**
     * Renders an item stack at the specified coordinates with a random seed.
     *
     * @param stack the item stack to render.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     * @param seed the random seed.
     */
    public void renderItem(ItemStack stack, int x, int y, int seed) {
        this.renderItem(this.minecraft.player, this.minecraft.level, stack, x, y, seed);
    }

    /**
     * Renders an item stack at the specified coordinates with a random seed and a custom value.
     *
     * @param stack the item stack to render.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     * @param seed the random seed.
     * @param guiOffset the GUI offset.
     */
    public void renderItem(ItemStack stack, int x, int y, int seed, int guiOffset) {
        this.renderItem(this.minecraft.player, this.minecraft.level, stack, x, y, seed, guiOffset);
    }

    /**
     * Renders a fake item stack at the specified coordinates.
     *
     * @param stack the fake item stack to render.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     */
    public void renderFakeItem(ItemStack stack, int x, int y) {
        this.renderItem(null, this.minecraft.level, stack, x, y, 0);
    }

    /**
     * Renders an item stack for a living entity at the specified coordinates with a random seed.
     *
     * @param entity the living entity.
     * @param stack the item stack to render.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     * @param seed the random seed.
     */
    public void renderItem(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        this.renderItem(entity, entity.level, stack, x, y, seed);
    }

    /**
     * Renders an item stack for a living entity in a specific level at the specified coordinates with a random seed.
     *
     * @param entity the living entity. Can be null.
     * @param level the level in which the rendering occurs. Can be null.
     * @param stack the item stack to render.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     * @param seed the random seed.
     */
    private void renderItem(@Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y, int seed) {
        this.renderItem(entity, level, stack, x, y, seed, 0);
    }

    /**
     * Renders an item stack for a living entity in a specific level at the specified coordinates with a random seed and a custom GUI offset.
     *
     * @param entity the living entity. Can be null.
     * @param level the level in which the rendering occurs. Can be null.
     * @param stack the item stack to render.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     * @param seed the random seed.
     * @param guiOffset the GUI offset value.
     */
    private void renderItem(@Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y, int seed, int guiOffset) {
        if (!stack.isEmpty()) {
            BakedModel bakedModel = this.minecraft.getItemRenderer().getModel(stack, level, entity, seed);
            this.pose.pushPose();
            this.pose.translate((float)(x + 8), (float)(y + 8), (float)(150 + (bakedModel.isGui3d() ? guiOffset : 0)));

            try {
                this.pose.mulPoseMatrix(new GuiMatrix4f().scaling(1.0F, -1.0F, 1.0F));
                this.pose.scale(16.0F, 16.0F, 16.0F);
                boolean bl = !bakedModel.usesBlockLight();
                if (bl) {
                    Lighting.setupForFlatItems();
                }
                this.minecraft
                        .getItemRenderer()
                        .render(stack, ItemTransforms.TransformType.GUI, false, this.pose, this.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
                this.flush();
                if (bl) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable var12) {
                CrashReport crashReport = CrashReport.forThrowable(var12, "Rendering item");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Item being rendered");
                crashReportCategory.setDetail("Item Type", (CrashReportDetail<String>)(() -> String.valueOf(stack.getItem())));
                crashReportCategory.setDetail("Item Damage", (CrashReportDetail<String>)(() -> String.valueOf(stack.getDamageValue())));
                crashReportCategory.setDetail("Item NBT", (CrashReportDetail<String>)(() -> String.valueOf(stack.getTag())));
                crashReportCategory.setDetail("Item Foil", (CrashReportDetail<String>)(() -> String.valueOf(stack.hasFoil())));
                throw new ReportedException(crashReport);
            }

            this.pose.popPose();
        }
    }

    /**
     * Renders additional decorations for an item stack at the specified coordinates.
     *
     * @param font the font used for rendering text.
     * @param stack the item stack to decorate.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        this.renderItemDecorations(font, stack, x, y, null);
    }

    /**
     * Renders additional decorations for an item stack at the specified coordinates with optional custom text.
     *
     * @param font the font used for rendering text.
     * @param stack the item stack to decorate.
     * @param x the x-coordinate of the rendering position.
     * @param y the y-coordinate of the rendering position.
     * @param text the custom text to display. Can be null.
     */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String text) {
        if (!stack.isEmpty()) {
            this.pose.pushPose();
            if (stack.getCount() != 1 || text != null) {
                String string = text == null ? String.valueOf(stack.getCount()) : text;
                this.pose.translate(0.0F, 0.0F, 200.0F);
                this.drawString(font, string, x + 19 - 2 - font.width(string), y + 6 + 3, 16777215, true);
            }

            if (stack.isBarVisible()) {
                int i = stack.getBarWidth();
                int j = stack.getBarColor();
                int k = x + 2;
                int l = y + 13;
                this.fill(GuiRenderTypes.guiOverlay(), k, l, k + 13, l + 2, -16777216);
                this.fill(GuiRenderTypes.guiOverlay(), k, l, k + i, l + 1, j | 0xFF000000);
            }

            LocalPlayer localPlayer = this.minecraft.player;
            float f = localPlayer == null ? 0.0F : localPlayer.getCooldowns().getCooldownPercent(stack.getItem(), this.minecraft.getFrameTime());
            if (f > 0.0F) {
                int k = y + Mth.floor(16.0F * (1.0F - f));
                int l = k + Mth.ceil(16.0F * f);
                this.fill(GuiRenderTypes.guiOverlay(), x, k, x + 16, l, Integer.MAX_VALUE);
            }

            this.pose.popPose();
        }
    }

    /**
     * Renders a tooltip for an item stack at the specified mouse coordinates.
     *
     * @param font the font used for rendering text.
     * @param stack the item stack to display the tooltip for.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font font, ItemStack stack, int mouseX, int mouseY) {
        this.renderTooltip(font, DUMMY_SCREEN.getTooltipFromItem(stack), stack.getTooltipImage(), mouseX, mouseY);
    }

    /**
     * Renders a tooltip with customizable components at the specified mouse coordinates.
     *
     * @param font the font used for rendering text.
     * @param tooltipLines the lines of the tooltip.
     * @param visualTooltipComponent the visual tooltip component. Can be empty.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY) {
        List<ClientTooltipComponent> list = (List<ClientTooltipComponent>)tooltipLines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .collect(Collectors.toList());
        visualTooltipComponent.ifPresent(tooltipComponent -> list.add(1, ClientTooltipComponent.create(tooltipComponent)));
        this.renderTooltipInternal(font, list, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
    }

    /**
     * Renders a tooltip with a single line of text at the specified mouse coordinates.
     *
     * @param font the font used for rendering text.
     * @param text the text to display in the tooltip.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font font, Component text, int mouseX, int mouseY) {
        this.renderTooltip(font, List.of(text.getVisualOrderText()), mouseX, mouseY);
    }

    /**
     * Renders a tooltip with multiple lines of component-based text at the specified mouse coordinates.
     *
     * @param font the font used for rendering text.
     * @param tooltipLines the lines of the tooltip as components.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderComponentTooltip(Font font, List<Component> tooltipLines, int mouseX, int mouseY) {
        this.renderTooltip(font, Lists.transform(tooltipLines, Component::getVisualOrderText), mouseX, mouseY);
    }

    /**
     * Renders a tooltip with multiple lines of formatted text at the specified mouse coordinates.
     *
     * @param font the font used for rendering text.
     * @param tooltipLines the lines of the tooltip as formatted character sequences.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font font, List<? extends FormattedCharSequence> tooltipLines, int mouseX, int mouseY) {
        this.renderTooltipInternal(
                font,
                (List<ClientTooltipComponent>)tooltipLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE
        );
    }

    /**
     * Renders a tooltip with multiple lines of formatted text using a custom tooltip positioner at the specified mouse coordinates.
     *
     * @param font the font used for rendering text.
     * @param tooltipLines the lines of the tooltip as formatted character sequences.
     * @param tooltipPositioner the positioner to determine the tooltip's position.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font font, List<FormattedCharSequence> tooltipLines, ClientTooltipPositioner tooltipPositioner, int mouseX, int mouseY) {
        this.renderTooltipInternal(
                font,
                (List<ClientTooltipComponent>)tooltipLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()),
                mouseX,
                mouseY,
                tooltipPositioner
        );
    }

    /**
     * Renders an internal tooltip with customizable tooltip components at the specified mouse coordinates using a tooltip positioner.
     *
     * @param font the font used for rendering text.
     * @param components the tooltip components to render.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     * @param tooltipPositioner the positioner to determine the tooltip's position.
     */
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner tooltipPositioner) {
        if (!components.isEmpty()) {
            int i = 0;
            int j = components.size() == 1 ? -2 : 0;

            for (ClientTooltipComponent clientTooltipComponent : components) {
                int k = clientTooltipComponent.getWidth(font);
                if (k > i) {
                    i = k;
                }

                j += clientTooltipComponent.getHeight();
            }

            int l = i;
            int m = j;
            Vector2ic vector2ic = tooltipPositioner.positionTooltip(this.guiWidth(), this.guiHeight(), mouseX, mouseY, l, m);
            int n = vector2ic.x();
            int o = vector2ic.y();
            this.pose.pushPose();
            int p = 400;
            this.drawManaged(() -> TooltipRenderUtil.renderTooltipBackground(this, n, o, l, m, 400));
            this.pose.translate(0.0F, 0.0F, 400.0F);
            int q = o;

            for (int r = 0; r < components.size(); r++) {
                ClientTooltipComponent clientTooltipComponent2 = (ClientTooltipComponent)components.get(r);
                clientTooltipComponent2.renderText(font, n, q, this.pose.last().pose(), this.bufferSource);
                q += clientTooltipComponent2.getHeight() + (r == 0 ? 2 : 0);
            }

            q = o;

            for (int r = 0; r < components.size(); r++) {
                ClientTooltipComponent clientTooltipComponent2 = (ClientTooltipComponent)components.get(r);
                clientTooltipComponent2.renderImage(font, n, q, this.pose, Minecraft.getInstance().getItemRenderer(), 400);
                q += clientTooltipComponent2.getHeight() + (r == 0 ? 2 : 0);
            }

            this.pose.popPose();
        }
    }

    /**
     * Renders a hover effect for a text component at the specified mouse coordinates.
     *
     * @param font the font used for rendering text.
     * @param style the style of the text component. Can be null.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderComponentHoverEffect(Font font, @Nullable Style style, int mouseX, int mouseY) {
        if (style != null && style.getHoverEvent() != null) {
            HoverEvent hoverEvent = style.getHoverEvent();
            HoverEvent.ItemStackInfo itemStackInfo = hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
            if (itemStackInfo != null) {
                this.renderTooltip(font, itemStackInfo.getItemStack(), mouseX, mouseY);
            } else {
                HoverEvent.EntityTooltipInfo entityTooltipInfo = hoverEvent.getValue(HoverEvent.Action.SHOW_ENTITY);
                if (entityTooltipInfo != null) {
                    if (this.minecraft.options.advancedItemTooltips) {
                        this.renderComponentTooltip(font, entityTooltipInfo.getTooltipLines(), mouseX, mouseY);
                    }
                } else {
                    Component component = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
                    if (component != null) {
                        this.renderTooltip(font, font.split(component, Math.max(this.guiWidth() / 2, 200)), mouseX, mouseY);
                    }
                }
            }
        }
    }

    /**
     * A utility class for managing a stack of screen rectangles for scissoring.
     */
    public static class ScissorStack {

        private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

        /**
         * Pushes a screen rectangle onto the scissor stack.
         * <p>
         * @return The resulting intersection of the pushed rectangle with the previous top rectangle on the stack, or the pushed rectangle if the stack is empty.
         *
         * @param scissor the screen rectangle to push.
         */
        public ScreenRectangle push(ScreenRectangle scissor) {
            ScreenRectangle screenRectangle = (ScreenRectangle)this.stack.peekLast();
            if (screenRectangle != null) {
                ScreenRectangle screenRectangle2 = (ScreenRectangle)Objects.requireNonNullElse(scissor.intersection(screenRectangle), ScreenRectangle.empty());
                this.stack.addLast(screenRectangle2);
                return screenRectangle2;
            } else {
                this.stack.addLast(scissor);
                return scissor;
            }
        }

        /**
         * Pops the top screen rectangle from the scissor stack.
         * <p>
         * @return The new top screen rectangle after the pop operation, or null if the stack is empty.
         * @throws IllegalStateException if the stack is empty.
         */
        @Nullable
        public ScreenRectangle pop() {
            if (this.stack.isEmpty()) {
                throw new IllegalStateException("Scissor stack underflow");
            } else {
                this.stack.removeLast();
                return (ScreenRectangle)this.stack.peekLast();
            }
        }

    }

}
