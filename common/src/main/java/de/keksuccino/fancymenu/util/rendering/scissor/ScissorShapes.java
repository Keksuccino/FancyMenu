package de.keksuccino.fancymenu.util.rendering.scissor;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBufferBuilder;
import de.keksuccino.fancymenu.util.rendering.RenderRotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Connects {@link ScissorShape}s to the GUI rendering of Minecraft:<br>
 * - {@code MixinGuiGraphics} turns a scissor enabled under a rotated or tilted pose into the bounds of the transformed rectangle for the GPU scissor,
 *   carrying the transformed rectangle as its shape. Vanilla ignores the pose for scissors, which only works for poses that don't rotate or tilt.<br>
 * - {@code MixinGuiGraphics_ScissorStack} hands the shape on to the scissors enabled inside of it.<br>
 * - When {@code GuiGraphics} applies a scissor, its shape becomes the active one, and {@code MixinBufferBuilder} cuts every mesh built on the render thread
 *   to it before the mesh gets stored for drawing. GUI rendering flushes its batched meshes before it changes the scissor, so every mesh gets cut to the shape it was rendered in.
 */
public final class ScissorShapes {

    // Meshes that get cut are only built on the render thread, so one clipper and one layout cache are enough
    private static final ScissorShapeClipper CLIPPER = new ScissorShapeClipper();
    private static final Map<VertexFormat, ScissorShapeClipper.VertexLayout> LAYOUTS = new IdentityHashMap<>();
    /** The shape of the scissor applied last, in framebuffer pixels, or NULL if it has none. Only the render thread reads and writes it. */
    @Nullable
    private static ScissorShape activeShape;

    private ScissorShapes() {
    }

    @Nullable
    public static ScissorShape getShape(@Nullable ScreenRectangle rectangle) {
        return (rectangle != null) ? ((ScissorShapeHolder)(Object)rectangle).getScissorShape_FancyMenu() : null;
    }

    public static void setShape(@NotNull ScreenRectangle rectangle, @Nullable ScissorShape shape) {
        ((ScissorShapeHolder)(Object)rectangle).setScissorShape_FancyMenu(shape);
    }

    /**
     * Returns if the current pose rotates or tilts what gets rendered, which is when a scissor has to follow the pose.
     */
    public static boolean isPoseRotated() {
        RenderRotationUtil.RotationState rotation = RenderRotationUtil.getCurrentAdditionalRenderRotation_FancyMenu();
        return (rotation.x != 0.0F) || (rotation.y != 0.0F) || (rotation.z != 0.0F);
    }

    /**
     * Returns the shape the scissor rectangle from (minX, minY) to (maxX, maxY) covers under the given pose, or NULL if the pose keeps it axis-aligned.
     */
    @Nullable
    public static ScissorShape getTransformedShape(int minX, int minY, int maxX, int maxY, @NotNull Matrix4fc pose) {
        // Rendering projects the GUI orthographically, so the pose maps a point of the GUI plane (Z = 0) by its X and Y rows only
        return ScissorShape.ofTransformedRectangle(pose.m00(), pose.m01(), pose.m10(), pose.m11(), pose.m30(), pose.m31(), minX, minY, maxX, maxY);
    }

    /**
     * Returns the axis-aligned bounds of the scissor rectangle from (minX, minY) to (maxX, maxY) under the given pose, as {x, y, width, height}.
     */
    public static int[] getTransformedBounds(int minX, int minY, int maxX, int maxY, @NotNull Matrix4fc pose) {
        float left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY, right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            float x = ((corner == 1) || (corner == 2)) ? maxX : minX;
            float y = (corner >= 2) ? maxY : minY;
            float transformedX = pose.m00() * x + pose.m10() * y + pose.m30();
            float transformedY = pose.m01() * x + pose.m11() * y + pose.m31();
            left = Math.min(left, transformedX);
            top = Math.min(top, transformedY);
            right = Math.max(right, transformedX);
            bottom = Math.max(bottom, transformedY);
        }
        int x = (int)Math.floor(left);
        int y = (int)Math.floor(top);
        return new int[]{x, y, Math.max(0, (int)Math.ceil(right) - x), Math.max(0, (int)Math.ceil(bottom) - y)};
    }

    /**
     * Makes the shape of the given scissor rectangle the active one, or clears it for NULL or a rectangle without a shape.
     * Gets called when {@code GuiGraphics} applies a scissor to the GPU, after it flushed everything rendered in the previous scissor.
     */
    public static void activate(@Nullable ScreenRectangle rectangle) {
        ScissorShape shape = getShape(rectangle);
        ScissorShapeClipper.PositionTransform transform = (shape != null) ? getFramebufferTransform() : null;
        if ((shape == null) || (transform == null) || shape.isEmpty()) {
            activeShape = shape;
            return;
        }
        // GUI vertices are in the space of the pose, like the shape, so the shape gets mapped to framebuffer pixels like they get mapped when they get drawn
        float[] cornersX = new float[shape.getCornerCount()];
        float[] cornersY = new float[shape.getCornerCount()];
        for (int i = 0; i < cornersX.length; i++) {
            cornersX[i] = transform.x(shape.getCornerX(i), shape.getCornerY(i), 0.0F);
            cornersY[i] = transform.y(shape.getCornerX(i), shape.getCornerY(i), 0.0F);
        }
        activeShape = ScissorShape.ofConvexPolygon(cornersX, cornersY);
    }

    /**
     * Cuts all vertices of the builder to the active shape, right before it stores its mesh. Does nothing without an active shape, off the render thread
     * (where meshes of the world get built) and for projections that are not orthographic.<br>
     * Only quads and triangles get cut. Other primitives keep their vertices and only get cut to the bounds of the shape by the GPU scissor.
     *
     * @return TRUE if the vertices changed, so data derived from them, like quad sorting points, has to get computed again.
     */
    public static boolean clipToActiveShape(@NotNull BufferBuilder builder) {
        ScissorShape shape = activeShape;
        if ((shape == null) || !RenderSystem.isOnRenderThread()) return false;
        IMixinBufferBuilder access = (IMixinBufferBuilder)builder;
        // Index-only buffers re-sort vertices that already are on the GPU
        if (access.get_indexOnly_FancyMenu()) return false;
        int vertexCount = access.get_vertices_FancyMenu();
        if (vertexCount <= 0) return false;
        int verticesPerPrimitive = switch (access.get_mode_FancyMenu()) {
            case QUADS -> 4;
            case TRIANGLES -> 3;
            default -> 0;
        };
        if (verticesPerPrimitive == 0) return false;
        ScissorShapeClipper.PositionTransform transform = getFramebufferTransform();
        if (transform == null) return false;

        VertexFormat format = access.get_format_FancyMenu();
        int vertexSize = format.getVertexSize();
        int start = access.get_renderedBufferPointer_FancyMenu();
        int written = CLIPPER.clip(access.get_buffer_FancyMenu(), start, vertexCount, getLayout(format), verticesPerPrimitive, shape, transform);
        if (written == ScissorShapeClipper.UNCHANGED) return false;

        // The vertices of the current mesh are the last ones in the buffer, so the cut ones replace them from the start of the mesh
        access.set_nextElementByte_FancyMenu(start);
        access.invoke_ensureCapacity_FancyMenu(written * vertexSize);
        access.get_buffer_FancyMenu().put(start, CLIPPER.getOutput(), 0, written * vertexSize);
        access.set_nextElementByte_FancyMenu(start + written * vertexSize);
        access.set_vertices_FancyMenu(written);
        return true;
    }

    /**
     * Returns the transform from the space vertices get built in to framebuffer pixels, from the current projection and model view matrices,
     * or NULL if the projection is not orthographic, so it is no affine transform.
     */
    @Nullable
    private static ScissorShapeClipper.PositionTransform getFramebufferTransform() {
        Matrix4f matrix = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        if ((Math.abs(matrix.m03()) > 1.0E-6F) || (Math.abs(matrix.m13()) > 1.0E-6F) || (Math.abs(matrix.m23()) > 1.0E-6F) || (Math.abs(matrix.m33() - 1.0F) > 1.0E-6F)) return null;
        Window window = Minecraft.getInstance().getWindow();
        float halfWidth = window.getWidth() / 2.0F;
        float halfHeight = window.getHeight() / 2.0F;
        // Normalized device coordinates to pixels with the origin at the top left, like GUI coordinates
        return new ScissorShapeClipper.PositionTransform(
                matrix.m00() * halfWidth, matrix.m10() * halfWidth, matrix.m20() * halfWidth, (matrix.m30() + 1.0F) * halfWidth,
                -matrix.m01() * halfHeight, -matrix.m11() * halfHeight, -matrix.m21() * halfHeight, (1.0F - matrix.m31()) * halfHeight);
    }

    @NotNull
    private static ScissorShapeClipper.VertexLayout getLayout(@NotNull VertexFormat format) {
        return LAYOUTS.computeIfAbsent(format, ScissorShapes::createLayout);
    }

    @NotNull
    private static ScissorShapeClipper.VertexLayout createLayout(@NotNull VertexFormat format) {
        ScissorShapeClipper.VertexLayout.Builder builder = ScissorShapeClipper.VertexLayout.builder(format.getVertexSize());
        // Elements follow each other without gaps, like the offsets the format computes for itself
        int offset = 0;
        for (VertexFormatElement element : format.getElements()) {
            if (element.getUsage() == VertexFormatElement.Usage.POSITION) {
                builder.position(offset);
            } else if (element.getUsage() != VertexFormatElement.Usage.PADDING) {
                builder.add(offset, ScissorShapeClipper.ComponentType.valueOf(element.getType().name()), element.getCount());
            }
            offset += element.getByteSize();
        }
        return builder.build();
    }

}
