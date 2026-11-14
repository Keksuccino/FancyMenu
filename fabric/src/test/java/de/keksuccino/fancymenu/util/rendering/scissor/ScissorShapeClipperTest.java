package de.keksuccino.fancymenu.util.rendering.scissor;

import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapeClipper.ComponentType;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapeClipper.PositionTransform;
import de.keksuccino.fancymenu.util.rendering.scissor.ScissorShapeClipper.VertexLayout;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScissorShapeClipperTest {

    // Position (3 floats), color (4 unsigned bytes), UV (2 floats), 2 bytes of padding, a generic vec4 like the smooth shape renderers write
    private static final int VERTEX_SIZE = 12 + 4 + 8 + 2 + 16;
    private static final VertexLayout LAYOUT = VertexLayout.builder(VERTEX_SIZE)
            .position(0)
            .add(12, ComponentType.UBYTE, 4)
            .add(16, ComponentType.FLOAT, 2)
            .add(26, ComponentType.FLOAT, 4)
            .build();
    /** A square rotated by 45 degrees around (50, 50), reaching 40 pixels from its center to its corners. */
    private static final ScissorShape DIAMOND = ScissorShape.ofConvexPolygon(new float[]{50, 90, 50, 10}, new float[]{10, 50, 90, 50});

    @Test
    void primitivesFullyInsideStayUntouched() {
        ByteBuffer source = quads(new float[][]{{40, 40, 60, 60}, {45, 30, 55, 70}});
        ScissorShapeClipper clipper = new ScissorShapeClipper();
        assertEquals(ScissorShapeClipper.UNCHANGED, clipper.clip(source, 0, 8, LAYOUT, 4, DIAMOND, PositionTransform.IDENTITY));
    }

    @Test
    void primitivesFullyOutsideGetDroppedAndInsideOnesKeepTheirBytes() {
        ByteBuffer source = quads(new float[][]{{40, 40, 60, 60}, {0, 0, 15, 15}, {85, 85, 100, 100}});
        ScissorShapeClipper clipper = new ScissorShapeClipper();
        int written = clipper.clip(source, 0, 12, LAYOUT, 4, DIAMOND, PositionTransform.IDENTITY);
        assertEquals(4, written);
        ByteBuffer output = clipper.getOutput();
        assertEquals(4 * VERTEX_SIZE, output.limit());
        for (int i = 0; i < 4 * VERTEX_SIZE; i++) {
            assertEquals(source.get(i), output.get(i), "byte " + i);
        }
    }

    @Test
    void crossingQuadsGetCutToTheShapeWithInterpolatedAttributes() {
        ByteBuffer source = quads(new float[][]{{20, 20, 80, 80}});
        ScissorShapeClipper clipper = new ScissorShapeClipper();
        int written = clipper.clip(source, 0, 4, LAYOUT, 4, DIAMOND, PositionTransform.IDENTITY);
        assertTrue(written > 4);
        assertEquals(0, written % 4);
        ByteBuffer output = clipper.getOutput();
        // The intersection of the 60x60 square with the diamond is an octagon: the square minus four corner triangles with legs of 20
        assertEquals(60 * 60 - 4 * (20 * 20 / 2.0), quadArea(output, written), 0.01);
        assertVerticesInsideWithLinearAttributes(output, written, DIAMOND);
    }

    @Test
    void crossingTrianglesGetCutIntoTriangles() {
        ByteBuffer source = ByteBuffer.allocate(3 * VERTEX_SIZE).order(ByteOrder.nativeOrder());
        writeVertex(source, 0, 20, 20);
        writeVertex(source, 1, 80, 20);
        writeVertex(source, 2, 20, 80);
        ScissorShapeClipper clipper = new ScissorShapeClipper();
        int written = clipper.clip(source, 0, 3, LAYOUT, 3, DIAMOND, PositionTransform.IDENTITY);
        assertEquals(0, written % 3);
        ByteBuffer output = clipper.getOutput();
        double area = 0.0;
        for (int i = 0; i < written; i += 3) {
            area += polygonArea(output, i, 3);
        }
        // The triangle is the half of the square above its diagonal, so it keeps half of the octagon
        assertEquals((60 * 60 - 4 * (20 * 20 / 2.0)) / 2.0, area, 0.01);
        assertVerticesInsideWithLinearAttributes(output, written, DIAMOND);
    }

    @Test
    void positionTransformMapsVerticesToTheSpaceOfTheShape() {
        // The vertices are in a space that is half the size of the shape's space and moved by (10, 10)
        PositionTransform transform = new PositionTransform(2, 0, 0, 10, 0, 2, 0, 10);
        ByteBuffer inside = quads(new float[][]{{15, 15, 25, 25}});
        ScissorShapeClipper clipper = new ScissorShapeClipper();
        assertEquals(ScissorShapeClipper.UNCHANGED, clipper.clip(inside, 0, 4, LAYOUT, 4, DIAMOND, transform));
        ByteBuffer outside = quads(new float[][]{{0, 0, 4, 4}});
        assertEquals(0, clipper.clip(outside, 0, 4, LAYOUT, 4, DIAMOND, transform));
    }

    @Test
    void sourceOffsetSkipsLeadingBytes() {
        ByteBuffer vertices = quads(new float[][]{{0, 0, 15, 15}});
        ByteBuffer source = ByteBuffer.allocate(7 + vertices.capacity()).order(ByteOrder.nativeOrder());
        source.put(7, vertices, 0, vertices.capacity());
        assertEquals(0, new ScissorShapeClipper().clip(source, 7, 4, LAYOUT, 4, DIAMOND, PositionTransform.IDENTITY));
    }

    @Test
    void emptyShapesDropEverythingAndIncompletePrimitivesStayUnchanged() {
        ScissorShapeClipper clipper = new ScissorShapeClipper();
        ScissorShape empty = ScissorShape.intersect(DIAMOND, ScissorShape.ofRectangle(200, 200, 300, 300));
        assertEquals(0, clipper.clip(quads(new float[][]{{40, 40, 60, 60}}), 0, 4, LAYOUT, 4, empty, PositionTransform.IDENTITY));
        assertEquals(ScissorShapeClipper.UNCHANGED, clipper.clip(quads(new float[][]{{0, 0, 15, 15}}), 0, 3, LAYOUT, 4, DIAMOND, PositionTransform.IDENTITY));
    }

    @Test
    void manyCrossingPrimitivesGrowTheOutput() {
        float[][] rects = new float[300][];
        for (int i = 0; i < rects.length; i++) {
            rects[i] = new float[]{i % 2 == 0 ? 5 : 20, 30 + (i % 40), 95, 31 + (i % 40)};
        }
        ByteBuffer source = quads(rects);
        ScissorShapeClipper clipper = new ScissorShapeClipper();
        int written = clipper.clip(source, 0, rects.length * 4, LAYOUT, 4, DIAMOND, PositionTransform.IDENTITY);
        assertTrue(written >= rects.length * 4);
        assertVerticesInsideWithLinearAttributes(clipper.getOutput(), written, DIAMOND);
    }

    /** UV, color and the generic vec4 are linear functions of the position, so cut vertices have to keep matching them. */
    private static void writeVertex(ByteBuffer buffer, int index, float x, float y) {
        int base = index * VERTEX_SIZE;
        buffer.putFloat(base, x);
        buffer.putFloat(base + 4, y);
        buffer.putFloat(base + 8, 0.5F);
        buffer.put(base + 12, (byte)Math.round(x * 2));
        buffer.put(base + 13, (byte)Math.round(y * 2));
        buffer.put(base + 14, (byte)255);
        buffer.put(base + 15, (byte)128);
        buffer.putFloat(base + 16, x / 100.0F);
        buffer.putFloat(base + 20, 1.0F - y / 100.0F);
        buffer.put(base + 24, (byte)0x5A);
        buffer.put(base + 25, (byte)0xA5);
        buffer.putFloat(base + 26, x + y);
        buffer.putFloat(base + 30, x - y);
        buffer.putFloat(base + 34, 7.0F);
        buffer.putFloat(base + 38, -3.0F);
    }

    /** Builds quads in the corner order the GUI uses: top left, bottom left, bottom right, top right. */
    private static ByteBuffer quads(float[][] rects) {
        ByteBuffer buffer = ByteBuffer.allocate(rects.length * 4 * VERTEX_SIZE).order(ByteOrder.nativeOrder());
        for (int i = 0; i < rects.length; i++) {
            float[] r = rects[i];
            writeVertex(buffer, i * 4, r[0], r[1]);
            writeVertex(buffer, i * 4 + 1, r[0], r[3]);
            writeVertex(buffer, i * 4 + 2, r[2], r[3]);
            writeVertex(buffer, i * 4 + 3, r[2], r[1]);
        }
        return buffer;
    }

    private static void assertVerticesInsideWithLinearAttributes(ByteBuffer output, int vertexCount, ScissorShape shape) {
        for (int i = 0; i < vertexCount; i++) {
            int base = i * VERTEX_SIZE;
            float x = output.getFloat(base);
            float y = output.getFloat(base + 4);
            assertTrue(shape.contains(x, y), "vertex " + i + " at " + x + ", " + y);
            assertEquals(0.5F, output.getFloat(base + 8), 1.0E-5);
            assertEquals(x * 2, output.get(base + 12) & 0xFF, 1.0);
            assertEquals(y * 2, output.get(base + 13) & 0xFF, 1.0);
            assertEquals(255, output.get(base + 14) & 0xFF);
            assertEquals(128, output.get(base + 15) & 0xFF);
            assertEquals(x / 100.0F, output.getFloat(base + 16), 1.0E-4);
            assertEquals(1.0F - y / 100.0F, output.getFloat(base + 20), 1.0E-4);
            assertEquals((byte)0x5A, output.get(base + 24));
            assertEquals((byte)0xA5, output.get(base + 25));
            assertEquals(x + y, output.getFloat(base + 26), 1.0E-3);
            assertEquals(x - y, output.getFloat(base + 30), 1.0E-3);
            assertEquals(7.0F, output.getFloat(base + 34), 1.0E-5);
            assertEquals(-3.0F, output.getFloat(base + 38), 1.0E-5);
        }
    }

    private static double quadArea(ByteBuffer output, int vertexCount) {
        double area = 0.0;
        for (int i = 0; i < vertexCount; i += 4) {
            area += polygonArea(output, i, 4);
        }
        return area;
    }

    private static double polygonArea(ByteBuffer output, int first, int count) {
        double twiceArea = 0.0;
        for (int i = 0; i < count; i++) {
            int current = (first + i) * VERTEX_SIZE;
            int next = (first + (i + 1) % count) * VERTEX_SIZE;
            twiceArea += (double)output.getFloat(current) * output.getFloat(next + 4) - (double)output.getFloat(next) * output.getFloat(current + 4);
        }
        return Math.abs(twiceArea) / 2.0;
    }

}
