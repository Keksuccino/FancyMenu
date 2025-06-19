package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

/**
 * Represents the state required to render a textured quadrilateral (a "blit" operation)
 * with floating-point coordinates.
 * This immutable record is used by the GUI rendering system to batch draw calls.
 *
 * @param pipeline The rendering pipeline to use for this element.
 * @param textureSetup The texture setup, defining which textures are bound.
 * @param transform The transformation matrix to apply to the vertices.
 * @param minX The minimum X coordinate of the quad.
 * @param minY The minimum Y coordinate of the quad.
 * @param maxX The maximum X coordinate of the quad.
 * @param maxY The maximum Y coordinate of the quad.
 * @param minU The minimum U texture coordinate (horizontal).
 * @param maxU The maximum U texture coordinate (horizontal).
 * @param minV The minimum V texture coordinate (vertical).
 * @param maxV The maximum V texture coordinate (vertical).
 * @param color The color tint to apply to the texture, in ARGB format.
 * @param scissorArea The optional scissor rectangle for clipping.
 * @param bounds The pre-calculated bounding box of this element after transformation and clipping, used for culling.
 */
public record FloatBlitRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f transform, float minX, float minY, float maxX, float maxY, float minU, float maxU, float minV, float maxV, int color, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

    /**
     * Creates a new blit render state, automatically calculating the bounding box.
     *
     * @param pipeline The rendering pipeline to use for this element.
     * @param textureSetup The texture setup, defining which textures are bound.
     * @param transform The transformation matrix to apply to the vertices.
     * @param minX The minimum X coordinate of the quad.
     * @param minY The minimum Y coordinate of the quad.
     * @param maxX The maximum X coordinate of the quad.
     * @param maxY The maximum Y coordinate of the quad.
     * @param minU The minimum U texture coordinate (horizontal).
     * @param maxU The maximum U texture coordinate (horizontal).
     * @param minV The minimum V texture coordinate (vertical).
     * @param maxV The maximum V texture coordinate (vertical).
     * @param color The color tint to apply to the texture, in ARGB format.
     * @param scissorRectangle The optional scissor rectangle for clipping.
     */
    public FloatBlitRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f transform, float minX, float minY, float maxX, float maxY, float minU, float maxU, float minV, float maxV, int color, @Nullable ScreenRectangle scissorRectangle) {
        this(pipeline, textureSetup, transform, minX, minY, maxX, maxY, minU, maxU, minV, maxV, color, scissorRectangle, getBounds(minX, minY, maxX, maxY, transform, scissorRectangle));
    }

    /**
     * Adds the four vertices of the textured quad to the given vertex consumer.
     *
     * @param vertexConsumer The consumer to which the vertices will be added.
     * @param depth The Z-coordinate (depth) for the vertices.
     */
    @Override
    public void buildVertices(VertexConsumer vertexConsumer, float depth) {
        vertexConsumer.addVertexWith2DPose(this.transform(), this.minX(), this.minY(), depth).setUv(this.minU(), this.minV()).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.transform(), this.minX(), this.maxY(), depth).setUv(this.minU(), this.maxV()).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.transform(), this.maxX(), this.maxY(), depth).setUv(this.maxU(), this.maxV()).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.transform(), this.maxX(), this.minY(), depth).setUv(this.maxU(), this.minV()).setColor(this.color());
    }

    /**
     * Calculates the screen-space bounding box for the quad after transformation and clipping.
     *
     * @param minX The minimum X coordinate of the quad.
     * @param minY The minimum Y coordinate of the quad.
     * @param maxX The maximum X coordinate of the quad.
     * @param maxY The maximum Y coordinate of the quad.
     * @param transform The transformation matrix applied to the quad.
     * @param scissorRectangle The scissor rectangle used for clipping.
     * @return The intersected bounding box, or {@code null} if the intersection is empty.
     */
    @Nullable
    private static ScreenRectangle getBounds(float minX, float minY, float maxX, float maxY, Matrix3x2f transform, @Nullable ScreenRectangle scissorRectangle) {
        // Since ScreenRectangle uses integer coordinates, we must cast the floats.
        // This captures the intended area for culling, even though rendering uses floats.
        int x = (int)minX;
        int y = (int)minY;
        int width = (int)(maxX - minX);
        int height = (int)(maxY - minY);

        ScreenRectangle elementBounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(transform);
        return scissorRectangle != null ? scissorRectangle.intersection(elementBounds) : elementBounds;
    }

}