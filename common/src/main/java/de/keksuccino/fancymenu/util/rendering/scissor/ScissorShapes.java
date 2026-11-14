package de.keksuccino.fancymenu.util.rendering.scissor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinBufferBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinByteBufferBuilder;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;
import org.lwjgl.system.MemoryUtil;

/**
 * Connects {@link ScissorShape}s to the GUI rendering of Minecraft:<br>
 * - {@code MixinGuiGraphicsExtractor} turns a scissor enabled under a rotated pose into the bounds of the rotated rectangle for the GPU scissor,
 *   carrying the rotated rectangle as its shape.<br>
 * - {@code MixinGuiGraphicsExtractor_ScissorStack} hands the shape on to the scissors enabled inside of it.<br>
 * - {@code MixinGuiRenderer} cuts the vertices of every render state to the shape of its scissor, right after the render state wrote them.
 */
public final class ScissorShapes {

    private static final String POSITION_ELEMENT = "Position";
    // The GUI renderer builds its meshes on the render thread only, so one clipper and one layout cache are enough
    private static final ScissorShapeClipper CLIPPER = new ScissorShapeClipper();
    /** Formats with components that can't get interpolated map to NULL, so their vertices only get cut to the bounds of the shape by the GPU scissor. */
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
        int verticesPerPrimitive = switch (access.get_primitiveTopology_FancyMenu()) {
            case QUADS -> 4;
            case TRIANGLES -> 3;
            default -> 0;
        };
        if (verticesPerPrimitive == 0) return;
        ScissorShapeClipper.VertexLayout layout = getLayout(access.get_format_FancyMenu());
        if (layout == null) return;

        int vertexSize = access.get_vertexSize_FancyMenu();
        ByteBufferBuilder byteBuffer = access.get_buffer_FancyMenu();
        IMixinByteBufferBuilder byteBufferAccess = (IMixinByteBufferBuilder)byteBuffer;
        long addedBytes = (long)vertexCount * vertexSize;
        long addedOffset = byteBufferAccess.get_writeOffset_FancyMenu() - addedBytes;
        ByteBuffer added = MemoryUtil.memByteBuffer(byteBufferAccess.get_pointer_FancyMenu() + addedOffset, (int)addedBytes);
        int written = CLIPPER.clip(added, 0, vertexCount, layout, verticesPerPrimitive, shape, ScissorShapeClipper.PositionTransform.IDENTITY);
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

    @Nullable
    private static ScissorShapeClipper.VertexLayout getLayout(@NotNull VertexFormat format) {
        if (LAYOUTS.containsKey(format)) return LAYOUTS.get(format);
        ScissorShapeClipper.VertexLayout layout = createLayout(format);
        LAYOUTS.put(format, layout);
        return layout;
    }

    @Nullable
    private static ScissorShapeClipper.VertexLayout createLayout(@NotNull VertexFormat format) {
        ScissorShapeClipper.VertexLayout.Builder builder = ScissorShapeClipper.VertexLayout.builder(format.getVertexSize());
        boolean hasPosition = false;
        for (VertexFormatElement element : format.getElements()) {
            GpuFormat elementFormat = element.format();
            if (POSITION_ELEMENT.equals(element.name())) {
                if (elementFormat != GpuFormat.RGB32_FLOAT) return null;
                builder.position(element.offset());
                hasPosition = true;
                continue;
            }
            ScissorShapeClipper.ComponentType type = switch (elementFormat.componentType()) {
                case UNORM_8, UINT_8 -> ScissorShapeClipper.ComponentType.UBYTE;
                case SNORM_8, SINT_8 -> ScissorShapeClipper.ComponentType.BYTE;
                case UNORM_16, UINT_16 -> ScissorShapeClipper.ComponentType.USHORT;
                case SNORM_16, SINT_16 -> ScissorShapeClipper.ComponentType.SHORT;
                case UINT_32 -> ScissorShapeClipper.ComponentType.UINT;
                case SINT_32 -> ScissorShapeClipper.ComponentType.INT;
                case FLOAT_32 -> ScissorShapeClipper.ComponentType.FLOAT;
                // Half floats and packed formats can't get interpolated component by component
                default -> null;
            };
            if (type == null) return null;
            builder.add(element.offset(), type, elementFormat.componentCount());
        }
        return hasPosition ? builder.build() : null;
    }

}
