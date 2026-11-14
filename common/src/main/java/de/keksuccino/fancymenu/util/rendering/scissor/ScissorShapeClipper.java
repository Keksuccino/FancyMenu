package de.keksuccino.fancymenu.util.rendering.scissor;

import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Cuts raw vertex data to a {@link ScissorShape}, for everything that gets rendered in a scissor that does not line up with the screen axes.<br>
 * It works on the bytes of the vertices, so it supports every vertex format and shader: primitives that are fully inside stay untouched,
 * primitives that are fully outside get dropped, and the others get cut at the edges of the shape, with all attributes interpolated linearly.
 * Shaders that compute their shape from interpolated attributes, like the smooth rectangles of FancyMenu, get cut exactly like plain geometry.<br><br>
 *
 * Instances reuse their buffers, so one instance must only be used by one thread.
 */
public final class ScissorShapeClipper {

    /** Returned by {@link #clip} when every primitive is inside the shape, so the vertices can stay as they are. */
    public static final int UNCHANGED = -1;

    private ByteBuffer output = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder());
    private byte[] primitiveStates = new byte[64];
    // Corners of the polygon a primitive gets cut to: screen position and every component of the vertex
    private double[][] polygon = new double[16][];
    private double[][] nextPolygon = new double[16][];
    private double[] polygonScreenX = new double[16];
    private double[] polygonScreenY = new double[16];
    private double[] nextScreenX = new double[16];
    private double[] nextScreenY = new double[16];
    private final float[] screenX = new float[4];
    private final float[] screenY = new float[4];

    private static final byte PRIMITIVE_INSIDE = 0;
    private static final byte PRIMITIVE_OUTSIDE = 1;
    private static final byte PRIMITIVE_CROSSING = 2;

    /**
     * Cuts the given vertices to the shape.
     *
     * @param source The vertices. Gets read with absolute positions, in the byte order of the buffer.
     * @param sourceOffset The byte position of the first vertex in the source.
     * @param vertexCount The number of vertices. A count that is no multiple of the vertices per primitive leaves everything unchanged.
     * @param verticesPerPrimitive 4 for quads, 3 for triangles. Quads get cut into quads again, triangles into triangles.
     * @param transform Maps vertex positions to the space of the shape.
     * @return The number of vertices in {@link #getOutput()}, or {@link #UNCHANGED} if all primitives are inside the shape.
     */
    public int clip(@NotNull ByteBuffer source, int sourceOffset, int vertexCount, @NotNull VertexLayout layout, int verticesPerPrimitive, @NotNull ScissorShape shape, @NotNull PositionTransform transform) {
        if ((verticesPerPrimitive != 3) && (verticesPerPrimitive != 4)) throw new IllegalArgumentException("Only quads and triangles can get clipped");
        if ((vertexCount <= 0) || ((vertexCount % verticesPerPrimitive) != 0)) return UNCHANGED;
        int primitiveCount = vertexCount / verticesPerPrimitive;
        if (this.primitiveStates.length < primitiveCount) this.primitiveStates = new byte[Math.max(primitiveCount, this.primitiveStates.length * 2)];

        boolean anyOutsideOrCrossing = false;
        for (int primitive = 0; primitive < primitiveCount; primitive++) {
            int first = sourceOffset + primitive * verticesPerPrimitive * layout.vertexSize;
            byte state = this.classify(source, first, layout, verticesPerPrimitive, shape, transform);
            this.primitiveStates[primitive] = state;
            if (state != PRIMITIVE_INSIDE) anyOutsideOrCrossing = true;
        }
        if (!anyOutsideOrCrossing) return UNCHANGED;

        this.output.clear();
        int written = 0;
        for (int primitive = 0; primitive < primitiveCount; primitive++) {
            int first = sourceOffset + primitive * verticesPerPrimitive * layout.vertexSize;
            byte state = this.primitiveStates[primitive];
            if (state == PRIMITIVE_INSIDE) {
                this.copyToOutput(source, first, verticesPerPrimitive * layout.vertexSize);
                written += verticesPerPrimitive;
            } else if (state == PRIMITIVE_CROSSING) {
                written += this.clipPrimitive(source, first, layout, verticesPerPrimitive, shape, transform);
            }
        }
        this.output.flip();
        return written;
    }

    /**
     * Returns the vertices of the last {@link #clip} call that changed something, from position 0 to the limit, in native byte order.
     * The buffer gets reused by the next call.
     */
    @NotNull
    public ByteBuffer getOutput() {
        return this.output;
    }

    private byte classify(@NotNull ByteBuffer source, int first, @NotNull VertexLayout layout, int verticesPerPrimitive, @NotNull ScissorShape shape, @NotNull PositionTransform transform) {
        if (shape.isEmpty()) return PRIMITIVE_OUTSIDE;
        for (int vertex = 0; vertex < verticesPerPrimitive; vertex++) {
            int position = first + vertex * layout.vertexSize + layout.positionOffset;
            float x = source.getFloat(position);
            float y = source.getFloat(position + 4);
            float z = source.getFloat(position + 8);
            this.screenX[vertex] = transform.x(x, y, z);
            this.screenY[vertex] = transform.y(x, y, z);
        }
        boolean allInside = true;
        for (int edge = 0; edge < shape.edgeA.length; edge++) {
            int outside = 0;
            for (int vertex = 0; vertex < verticesPerPrimitive; vertex++) {
                if ((shape.edgeA[edge] * this.screenX[vertex] + shape.edgeB[edge] * this.screenY[vertex] + shape.edgeC[edge]) < -ScissorShape.EDGE_TOLERANCE) outside++;
            }
            if (outside == verticesPerPrimitive) return PRIMITIVE_OUTSIDE;
            if (outside > 0) allInside = false;
        }
        return allInside ? PRIMITIVE_INSIDE : PRIMITIVE_CROSSING;
    }

    /** Cuts one primitive to the shape and writes the resulting polygon as quads or triangles. Returns the number of written vertices. */
    private int clipPrimitive(@NotNull ByteBuffer source, int first, @NotNull VertexLayout layout, int verticesPerPrimitive, @NotNull ScissorShape shape, @NotNull PositionTransform transform) {
        int components = layout.componentOffsets.length;
        int count = verticesPerPrimitive;
        this.ensurePolygonCapacity(count + shape.edgeA.length + 1, components);
        for (int vertex = 0; vertex < verticesPerPrimitive; vertex++) {
            int base = first + vertex * layout.vertexSize;
            double[] values = this.polygon[vertex];
            for (int component = 0; component < components; component++) {
                values[component] = layout.read(source, base, component);
            }
            int position = base + layout.positionOffset;
            float x = source.getFloat(position);
            float y = source.getFloat(position + 4);
            float z = source.getFloat(position + 8);
            this.polygonScreenX[vertex] = transform.x(x, y, z);
            this.polygonScreenY[vertex] = transform.y(x, y, z);
        }

        for (int edge = 0; (edge < shape.edgeA.length) && (count > 0); edge++) {
            double a = shape.edgeA[edge];
            double b = shape.edgeB[edge];
            double c = shape.edgeC[edge];
            int nextCount = 0;
            for (int i = 0; i < count; i++) {
                int following = (i + 1) % count;
                double distance = a * this.polygonScreenX[i] + b * this.polygonScreenY[i] + c;
                double followingDistance = a * this.polygonScreenX[following] + b * this.polygonScreenY[following] + c;
                boolean inside = distance >= 0.0;
                if (inside) {
                    this.copyCorner(i, nextCount, components);
                    nextCount++;
                }
                if (inside != (followingDistance >= 0.0)) {
                    // Screen space is an affine map of the vertex space, so the same factor interpolates every attribute
                    double t = distance / (distance - followingDistance);
                    double[] from = this.polygon[i];
                    double[] to = this.polygon[following];
                    double[] target = this.nextPolygon[nextCount];
                    for (int component = 0; component < components; component++) {
                        target[component] = from[component] + (to[component] - from[component]) * t;
                    }
                    this.nextScreenX[nextCount] = this.polygonScreenX[i] + (this.polygonScreenX[following] - this.polygonScreenX[i]) * t;
                    this.nextScreenY[nextCount] = this.polygonScreenY[i] + (this.polygonScreenY[following] - this.polygonScreenY[i]) * t;
                    nextCount++;
                }
            }
            this.swapPolygons();
            count = nextCount;
        }
        if (count < 3) return 0;

        // A convex polygon is a fan around its first corner. Quads take two fan triangles each, an odd one out gets a repeated last corner.
        int written = 0;
        if (verticesPerPrimitive == 4) {
            for (int i = 1; i < count - 1; i += 2) {
                int third = Math.min(i + 2, count - 1);
                this.writeCorner(source, first, layout, 0);
                this.writeCorner(source, first, layout, i);
                this.writeCorner(source, first, layout, i + 1);
                this.writeCorner(source, first, layout, third);
                written += 4;
            }
        } else {
            for (int i = 1; i < count - 1; i++) {
                this.writeCorner(source, first, layout, 0);
                this.writeCorner(source, first, layout, i);
                this.writeCorner(source, first, layout, i + 1);
                written += 3;
            }
        }
        return written;
    }

    private void copyCorner(int from, int to, int components) {
        System.arraycopy(this.polygon[from], 0, this.nextPolygon[to], 0, components);
        this.nextScreenX[to] = this.polygonScreenX[from];
        this.nextScreenY[to] = this.polygonScreenY[from];
    }

    private void swapPolygons() {
        double[][] corners = this.polygon;
        this.polygon = this.nextPolygon;
        this.nextPolygon = corners;
        double[] xs = this.polygonScreenX;
        this.polygonScreenX = this.nextScreenX;
        this.nextScreenX = xs;
        double[] ys = this.polygonScreenY;
        this.polygonScreenY = this.nextScreenY;
        this.nextScreenY = ys;
    }

    private void writeCorner(@NotNull ByteBuffer source, int primitiveStart, @NotNull VertexLayout layout, int corner) {
        int base = this.output.position();
        // Bytes no component covers, like padding, keep the values of the first vertex of the primitive
        this.copyToOutput(source, primitiveStart, layout.vertexSize);
        double[] values = this.polygon[corner];
        for (int component = 0; component < layout.componentOffsets.length; component++) {
            layout.write(this.output, base, component, values[component]);
        }
    }

    private void ensurePolygonCapacity(int corners, int components) {
        if ((this.polygon.length < corners) || (this.polygonScreenX.length < corners)) {
            int size = Math.max(corners, this.polygon.length * 2);
            this.polygon = new double[size][];
            this.nextPolygon = new double[size][];
            this.polygonScreenX = new double[size];
            this.polygonScreenY = new double[size];
            this.nextScreenX = new double[size];
            this.nextScreenY = new double[size];
        }
        for (int i = 0; i < this.polygon.length; i++) {
            if ((this.polygon[i] == null) || (this.polygon[i].length < components)) this.polygon[i] = new double[components];
            if ((this.nextPolygon[i] == null) || (this.nextPolygon[i].length < components)) this.nextPolygon[i] = new double[components];
        }
    }

    private void copyToOutput(@NotNull ByteBuffer source, int sourcePosition, int length) {
        this.ensureOutputCapacity(length);
        int position = this.output.position();
        this.output.put(position, source, sourcePosition, length);
        this.output.position(position + length);
    }

    private void ensureOutputCapacity(int additionalBytes) {
        if (this.output.remaining() >= additionalBytes) return;
        int required = this.output.position() + additionalBytes;
        ByteBuffer grown = ByteBuffer.allocateDirect(Math.max(required, this.output.capacity() * 2)).order(ByteOrder.nativeOrder());
        this.output.flip();
        grown.put(this.output);
        this.output = grown;
    }

    /** The type of one vertex component, which decides how it gets read, interpolated and written. */
    public enum ComponentType {

        FLOAT(4),
        UBYTE(1),
        BYTE(1),
        USHORT(2),
        SHORT(2),
        UINT(4),
        INT(4);

        public final int size;

        ComponentType(int size) {
            this.size = size;
        }

    }

    /**
     * Describes where the components of a vertex are, built once per vertex format. Position has to be three floats.
     */
    public static final class VertexLayout {

        final int vertexSize;
        final int positionOffset;
        final int[] componentOffsets;
        final ComponentType[] componentTypes;

        private VertexLayout(int vertexSize, int positionOffset, int[] componentOffsets, ComponentType[] componentTypes) {
            this.vertexSize = vertexSize;
            this.positionOffset = positionOffset;
            this.componentOffsets = componentOffsets;
            this.componentTypes = componentTypes;
        }

        @NotNull
        public static Builder builder(int vertexSize) {
            return new Builder(vertexSize);
        }

        public int getVertexSize() {
            return this.vertexSize;
        }

        double read(@NotNull ByteBuffer buffer, int vertexStart, int component) {
            int position = vertexStart + this.componentOffsets[component];
            return switch (this.componentTypes[component]) {
                case FLOAT -> buffer.getFloat(position);
                case UBYTE -> buffer.get(position) & 0xFF;
                case BYTE -> buffer.get(position);
                case USHORT -> buffer.getShort(position) & 0xFFFF;
                case SHORT -> buffer.getShort(position);
                case UINT -> buffer.getInt(position) & 0xFFFFFFFFL;
                case INT -> buffer.getInt(position);
            };
        }

        void write(@NotNull ByteBuffer buffer, int vertexStart, int component, double value) {
            int position = vertexStart + this.componentOffsets[component];
            switch (this.componentTypes[component]) {
                case FLOAT -> buffer.putFloat(position, (float)value);
                case UBYTE -> buffer.put(position, (byte)clampRound(value, 0, 255));
                case BYTE -> buffer.put(position, (byte)clampRound(value, Byte.MIN_VALUE, Byte.MAX_VALUE));
                case USHORT -> buffer.putShort(position, (short)clampRound(value, 0, 65535));
                case SHORT -> buffer.putShort(position, (short)clampRound(value, Short.MIN_VALUE, Short.MAX_VALUE));
                case UINT -> buffer.putInt(position, (int)clampRound(value, 0, 4294967295L));
                case INT -> buffer.putInt(position, (int)clampRound(value, Integer.MIN_VALUE, Integer.MAX_VALUE));
            }
        }

        private static long clampRound(double value, long min, long max) {
            return Math.max(min, Math.min(max, Math.round(value)));
        }

        @Override
        public String toString() {
            return "VertexLayout[size=" + this.vertexSize + ", position=" + this.positionOffset + ", offsets=" + Arrays.toString(this.componentOffsets) + ", types=" + Arrays.toString(this.componentTypes) + "]";
        }

        public static final class Builder {

            private final int vertexSize;
            private int positionOffset = -1;
            private int[] offsets = new int[16];
            private ComponentType[] types = new ComponentType[16];
            private int count = 0;

            private Builder(int vertexSize) {
                this.vertexSize = vertexSize;
            }

            /** Adds the three float components of the position. */
            @NotNull
            public Builder position(int offset) {
                this.positionOffset = offset;
                return this.add(offset, ComponentType.FLOAT, 3);
            }

            /** Adds an element of the given number of components of the given type, which start at the given byte offset in the vertex. */
            @NotNull
            public Builder add(int offset, @NotNull ComponentType type, int components) {
                for (int i = 0; i < components; i++) {
                    if (this.count == this.offsets.length) {
                        this.offsets = Arrays.copyOf(this.offsets, this.count * 2);
                        this.types = Arrays.copyOf(this.types, this.count * 2);
                    }
                    this.offsets[this.count] = offset + i * type.size;
                    this.types[this.count] = type;
                    this.count++;
                }
                return this;
            }

            @NotNull
            public VertexLayout build() {
                if (this.positionOffset < 0) throw new IllegalStateException("Vertex layouts need a position");
                return new VertexLayout(this.vertexSize, this.positionOffset, Arrays.copyOf(this.offsets, this.count), Arrays.copyOf(this.types, this.count));
            }

        }

    }

    /**
     * Maps a vertex position to the space of the shape: {@code x' = xx * x + xy * y + xz * z + xw} and {@code y' = yx * x + yy * y + yz * z + yw}.
     */
    public record PositionTransform(float xx, float xy, float xz, float xw, float yx, float yy, float yz, float yw) {

        public static final PositionTransform IDENTITY = new PositionTransform(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);

        public float x(float x, float y, float z) {
            return this.xx * x + this.xy * y + this.xz * z + this.xw;
        }

        public float y(float x, float y, float z) {
            return this.yx * x + this.yy * y + this.yz * z + this.yw;
        }

    }

}
