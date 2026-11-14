package de.keksuccino.fancymenu.util.rendering.scissor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBufferBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinByteBufferBuilder;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Connects {@link ScissorShape}s to the GUI rendering of Minecraft:<br>
 * - {@code MixinGuiGraphicsExtractor} turns a scissor enabled under a rotated pose into the bounds of the rotated rectangle for the GPU scissor,
 *   carrying the rotated rectangle as its shape.<br>
 * - {@code MixinGuiGraphicsExtractor_ScissorStack} hands the shape on to the scissors enabled inside of it.<br>
 * - {@code MixinGuiRenderer} cuts the vertices of every render state to the shape of its scissor, right after the render state wrote them.
 */
public final class ScissorShapes {

    // The GUI renderer builds its meshes on the render thread only, so one clipper and one layout cache are enough
    private static final ScissorShapeClipper CLIPPER = new ScissorShapeClipper();
    private static final Map<VertexFormat, ScissorShapeClipper.VertexLayout> LAYOUTS = new IdentityHashMap<>();

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
     * Returns the shape the given scissor rectangle covers under the given pose, or NULL if the pose keeps it axis-aligned.
     */
    @Nullable
    public static ScissorShape getTransformedShape(@NotNull ScreenRectangle rectangle, @NotNull Matrix3x2fc pose) {
        return ScissorShape.ofTransformedRectangle(pose.m00(), pose.m01(), pose.m10(), pose.m11(), pose.m20(), pose.m21(), rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
    }

    /**
     * Cuts the vertices the builder got since it had the given number of vertices to the given shape.<br>
     * The vertices of a GUI render state are the last ones in the builder, so they get replaced in place: the builder forgets them and gets the cut ones instead.
     * Only quads and triangles get cut. Other primitives keep their vertices and only get cut to the bounds of the shape by the GPU scissor.
     */
    public static void clipAddedVertices(@NotNull BufferBuilder builder, int firstVertex, @NotNull ScissorShape shape) {
        IMixinBufferBuilder access = (IMixinBufferBuilder)builder;
        int vertexCount = access.get_vertices_FancyMenu() - firstVertex;
        if (vertexCount <= 0) return;
        int verticesPerPrimitive = switch (access.get_mode_FancyMenu()) {
            case QUADS -> 4;
            case TRIANGLES -> 3;
            default -> 0;
        };
        if (verticesPerPrimitive == 0) return;

        int vertexSize = access.get_vertexSize_FancyMenu();
        ByteBufferBuilder byteBuffer = access.get_buffer_FancyMenu();
        IMixinByteBufferBuilder byteBufferAccess = (IMixinByteBufferBuilder)byteBuffer;
        long addedBytes = (long)vertexCount * vertexSize;
        long addedOffset = byteBufferAccess.get_writeOffset_FancyMenu() - addedBytes;
        ByteBuffer added = MemoryUtil.memByteBuffer(byteBufferAccess.get_pointer_FancyMenu() + addedOffset, (int)addedBytes);
        int written = CLIPPER.clip(added, 0, vertexCount, getLayout(access.get_format_FancyMenu()), verticesPerPrimitive, shape, ScissorShapeClipper.PositionTransform.IDENTITY);
        if (written == ScissorShapeClipper.UNCHANGED) return;

        // Rewinding the write offset frees the bytes of the added vertices, so the cut ones take their place
        byteBufferAccess.set_writeOffset_FancyMenu(addedOffset);
        if (written > 0) {
            long target = byteBuffer.reserve(written * vertexSize);
            MemoryUtil.memCopy(MemoryUtil.memAddress(CLIPPER.getOutput()), target, (long)written * vertexSize);
        }
        int vertices = firstVertex + written;
        access.set_vertices_FancyMenu(vertices);
        // The builder fills its last vertex until the next one starts. The last vertex is complete now, so nothing is left to fill.
        long lastVertex = byteBufferAccess.get_pointer_FancyMenu() + byteBufferAccess.get_writeOffset_FancyMenu() - vertexSize;
        access.set_vertexPointer_FancyMenu((vertices > 0) ? lastVertex : -1L);
        access.set_elementsToFill_FancyMenu(0);
    }

    @NotNull
    private static ScissorShapeClipper.VertexLayout getLayout(@NotNull VertexFormat format) {
        return LAYOUTS.computeIfAbsent(format, ScissorShapes::createLayout);
    }

    @NotNull
    private static ScissorShapeClipper.VertexLayout createLayout(@NotNull VertexFormat format) {
        ScissorShapeClipper.VertexLayout.Builder builder = ScissorShapeClipper.VertexLayout.builder(format.getVertexSize());
        for (VertexFormatElement element : format.getElements()) {
            int offset = format.getOffset(element);
            if (element == VertexFormatElement.POSITION) {
                builder.position(offset);
            } else {
                builder.add(offset, ScissorShapeClipper.ComponentType.valueOf(element.type().name()), element.count());
            }
        }
        return builder.build();
    }

}
