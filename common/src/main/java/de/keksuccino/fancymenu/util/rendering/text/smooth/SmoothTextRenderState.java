package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import java.util.Arrays;

/**
 * All quads of one text or icon draw call that use the same atlas page, submitted to the GUI render state as a single element.
 * One element per text (instead of one per glyph) keeps the GUI renderer's layer placement and sorting cheap, and the
 * glyphs of a text never end up on different layers.
 */
final class SmoothTextRenderState implements GuiElementRenderState {

    private final Matrix3x2f pose;
    private final TextureSetup textureSetup;
    private final float[] quads;
    private final int[] colors;
    private final int quadCount;
    @Nullable
    private final ScreenRectangle scissorArea;
    @Nullable
    private final ScreenRectangle bounds;

    SmoothTextRenderState(@NotNull Matrix3x2f pose, @NotNull SmoothTextMesh.Batch batch, @NotNull TextureSetup textureSetup, @Nullable ScreenRectangle scissorArea) {
        this.pose = pose;
        this.textureSetup = textureSetup;
        this.quadCount = batch.quadCount;
        this.quads = Arrays.copyOf(batch.quads, batch.quadCount * SmoothTextMesh.FLOATS_PER_QUAD);
        this.colors = Arrays.copyOf(batch.colors, batch.quadCount);
        this.scissorArea = scissorArea;
        this.bounds = computeBounds(batch, pose, scissorArea);
    }

    @Override
    public void buildVertices(@NotNull VertexConsumer consumer) {
        for (int i = 0; i < this.quadCount; i++) {
            int offset = i * SmoothTextMesh.FLOATS_PER_QUAD;
            float x0 = this.quads[offset];
            float y0 = this.quads[offset + 1];
            float x1 = this.quads[offset + 2];
            float y1 = this.quads[offset + 3];
            float u0 = this.quads[offset + 4];
            float v0 = this.quads[offset + 5];
            float u1 = this.quads[offset + 6];
            float v1 = this.quads[offset + 7];
            int color = this.colors[i];
            consumer.addVertexWith2DPose(this.pose, x0, y0).setUv(u0, v0).setColor(color);
            consumer.addVertexWith2DPose(this.pose, x0, y1).setUv(u0, v1).setColor(color);
            consumer.addVertexWith2DPose(this.pose, x1, y1).setUv(u1, v1).setColor(color);
            consumer.addVertexWith2DPose(this.pose, x1, y0).setUv(u1, v0).setColor(color);
        }
    }

    @Override
    public @NotNull RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return this.textureSetup;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return this.scissorArea;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return this.bounds;
    }

    @Nullable
    private static ScreenRectangle computeBounds(@NotNull SmoothTextMesh.Batch batch, @NotNull Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        int x = (int) Math.floor(batch.minX);
        int y = (int) Math.floor(batch.minY);
        int width = (int) Math.ceil(batch.maxX) - x;
        int height = (int) Math.ceil(batch.maxY) - y;
        if (width <= 0 || height <= 0) {
            return null;
        }
        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        return (scissorArea != null) ? scissorArea.intersection(bounds) : bounds;
    }

}
