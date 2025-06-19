package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

/**
 * Represents the state required to render a colored rectangle, which can have a solid color
 * or a vertical gradient. This uses floating-point coordinates for precision.
 * This immutable record is used by the GUI rendering system to batch draw calls.
 *
 * @param pipeline The rendering pipeline to use for this element.
 * @param textureSetup The texture setup, which is typically {@link TextureSetup#noTexture()} for solid colors.
 * @param transform The transformation matrix to apply to the vertices.
 * @param minX The minimum X coordinate of the rectangle.
 * @param minY The minimum Y coordinate of the rectangle.
 * @param maxX The maximum X coordinate of the rectangle.
 * @param maxY The maximum Y coordinate of the rectangle.
 * @param startColor The color of the top edge of the rectangle (or the entire rectangle if endColor is the same), in ARGB format.
 * @param endColor The color of the bottom edge of the rectangle, for creating gradients, in ARGB format.
 * @param scissorArea The optional scissor rectangle for clipping.
 * @param bounds The pre-calculated bounding box of this element after transformation and clipping, used for culling.
 */
public record FloatColoredRectangleRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f transform, float minX, float minY, float maxX, float maxY, int startColor, int endColor, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

    /**
     * Creates a new colored rectangle render state, automatically calculating the bounding box.
     *
     * @param pipeline The rendering pipeline to use for this element.
     * @param textureSetup The texture setup.
     * @param transform The transformation matrix to apply to the vertices.
     * @param minX The minimum X coordinate of the rectangle.
     * @param minY The minimum Y coordinate of the rectangle.
     * @param maxX The maximum X coordinate of the rectangle.
     * @param maxY The maximum Y coordinate of the rectangle.
     * @param startColor The color for the top edge.
     * @param endColor The color for the bottom edge.
     * @param scissorRectangle The optional scissor rectangle for clipping.
     */
    public FloatColoredRectangleRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f transform, float minX, float minY, float maxX, float maxY, int startColor, int endColor, @Nullable ScreenRectangle scissorRectangle) {
        this(pipeline, textureSetup, transform, minX, minY, maxX, maxY, startColor, endColor, scissorRectangle, getBounds(minX, minY, maxX, maxY, transform, scissorRectangle));
    }

    /**
     * Adds the four vertices of the colored rectangle to the given vertex consumer.
     *
     * @param vertexConsumer The consumer to which the vertices will be added.
     * @param depth The Z-coordinate (depth) for the vertices.
     */
    @Override
    public void buildVertices(VertexConsumer vertexConsumer, float depth) {
        // Top-left vertex with startColor
        vertexConsumer.addVertexWith2DPose(this.transform(), this.minX(), this.minY(), depth).setColor(this.startColor());
        // Bottom-left vertex with endColor
        vertexConsumer.addVertexWith2DPose(this.transform(), this.minX(), this.maxY(), depth).setColor(this.endColor());
        // Bottom-right vertex with endColor
        vertexConsumer.addVertexWith2DPose(this.transform(), this.maxX(), this.maxY(), depth).setColor(this.endColor());
        // Top-right vertex with startColor
        vertexConsumer.addVertexWith2DPose(this.transform(), this.maxX(), this.minY(), depth).setColor(this.startColor());
    }

    /**
     * Calculates the screen-space bounding box for the rectangle after transformation and clipping.
     *
     * @param minX The minimum X coordinate of the rectangle.
     * @param minY The minimum Y coordinate of the rectangle.
     * @param maxX The maximum X coordinate of the rectangle.
     * @param maxY The maximum Y coordinate of the rectangle.
     * @param transform The transformation matrix applied to the rectangle.
     * @param scissorRectangle The scissor rectangle used for clipping.
     * @return The intersected bounding box, or {@code null} if the intersection is empty.
     */
    @Nullable
    private static ScreenRectangle getBounds(float minX, float minY, float maxX, float maxY, Matrix3x2f transform, @Nullable ScreenRectangle scissorRectangle) {
        // Cast to int for ScreenRectangle, which is used for integer-based culling.
        int x = (int)minX;
        int y = (int)minY;
        int width = (int)(maxX - minX);
        int height = (int)(maxY - minY);

        ScreenRectangle elementBounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(transform);
        return scissorRectangle != null ? scissorRectangle.intersection(elementBounds) : elementBounds;
    }

}