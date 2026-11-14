package de.keksuccino.fancymenu.util.rendering.scissor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;

/**
 * The convex area of the screen that a scissor shows when it does not line up with the screen axes, like the scissor of a rotated element.<br>
 * The GPU scissor can only cut to axis-aligned rectangles, so it cuts to the bounds of the shape, and {@link ScissorShapeClipper}
 * cuts the geometry rendered in the scissor to the shape itself.<br><br>
 *
 * Shapes are immutable. Every edge stores the line equation {@code a * x + b * y + c}, normalized so it is the distance of a point to the edge,
 * positive on the inner side.
 */
public final class ScissorShape {

    /** Points this close to an edge still count as inside, so vertices lying on an edge don't get clipped because of float rounding. */
    static final float EDGE_TOLERANCE = 1.0E-3F;
    /** A pose whose rotation part has no larger off-diagonal entries keeps rectangles axis-aligned, so the GPU scissor alone cuts them exactly. */
    private static final float AXIS_ALIGNED_EPSILON = 1.0E-6F;
    private static final ScissorShape EMPTY = new ScissorShape(new float[0], new float[0]);

    private final float[] cornersX;
    private final float[] cornersY;
    final float[] edgeA;
    final float[] edgeB;
    final float[] edgeC;

    private ScissorShape(float[] cornersX, float[] cornersY) {
        this.cornersX = cornersX;
        this.cornersY = cornersY;
        int size = cornersX.length;
        this.edgeA = new float[size];
        this.edgeB = new float[size];
        this.edgeC = new float[size];
        if (size < 3) return;
        // The sign of the area tells the winding, which decides on which side of the edges the inside is
        double area = 0.0;
        for (int i = 0; i < size; i++) {
            int next = (i + 1) % size;
            area += (double)cornersX[i] * cornersY[next] - (double)cornersX[next] * cornersY[i];
        }
        double winding = (area >= 0.0) ? 1.0 : -1.0;
        for (int i = 0; i < size; i++) {
            int next = (i + 1) % size;
            double edgeX = cornersX[next] - cornersX[i];
            double edgeY = cornersY[next] - cornersY[i];
            double length = Math.sqrt(edgeX * edgeX + edgeY * edgeY);
            if (length <= 0.0) length = 1.0;
            double a = -edgeY * winding / length;
            double b = edgeX * winding / length;
            this.edgeA[i] = (float)a;
            this.edgeB[i] = (float)b;
            this.edgeC[i] = (float)(-(a * cornersX[i] + b * cornersY[i]));
        }
    }

    /**
     * Returns the shape the rectangle from (left, top) to (right, bottom) covers after the given affine transform, or NULL if the transform keeps
     * it axis-aligned, so the bounds of the transformed rectangle already are the exact scissor.<br>
     * The transform maps a point to ({@code m00 * x + m10 * y + m20}, {@code m01 * x + m11 * y + m21}).
     */
    @Nullable
    public static ScissorShape ofTransformedRectangle(float m00, float m01, float m10, float m11, float m20, float m21, float left, float top, float right, float bottom) {
        if ((Math.abs(m01) <= AXIS_ALIGNED_EPSILON) && (Math.abs(m10) <= AXIS_ALIGNED_EPSILON)) return null;
        float[] localX = {left, right, right, left};
        float[] localY = {top, top, bottom, bottom};
        float[] cornersX = new float[4];
        float[] cornersY = new float[4];
        for (int i = 0; i < 4; i++) {
            cornersX[i] = m00 * localX[i] + m10 * localY[i] + m20;
            cornersY[i] = m01 * localX[i] + m11 * localY[i] + m21;
        }
        return ofConvexPolygon(cornersX, cornersY);
    }

    /**
     * Returns the shape with the given corners, which have to form a convex polygon, in any winding. Fewer than three corners make an empty shape.
     */
    @NotNull
    public static ScissorShape ofConvexPolygon(float @NotNull [] cornersX, float @NotNull [] cornersY) {
        if (cornersX.length != cornersY.length) throw new IllegalArgumentException("Corner coordinate arrays need the same length");
        if (cornersX.length < 3) return EMPTY;
        return new ScissorShape(Arrays.copyOf(cornersX, cornersX.length), Arrays.copyOf(cornersY, cornersY.length));
    }

    /** Returns the shape of the given axis-aligned rectangle. */
    @NotNull
    public static ScissorShape ofRectangle(float left, float top, float right, float bottom) {
        if ((right <= left) || (bottom <= top)) return EMPTY;
        return new ScissorShape(new float[]{left, right, right, left}, new float[]{top, top, bottom, bottom});
    }

    /**
     * Returns the part of the screen both shapes show. NULL counts as a shape that shows everything.
     */
    @Nullable
    public static ScissorShape intersect(@Nullable ScissorShape first, @Nullable ScissorShape second) {
        if (first == null) return second;
        if (second == null) return first;
        if (first.isEmpty() || second.isEmpty()) return EMPTY;
        // Sutherland-Hodgman: cut the corners of the first shape at every edge of the second one
        float[] xs = first.cornersX;
        float[] ys = first.cornersY;
        int count = xs.length;
        for (int edge = 0; (edge < second.edgeA.length) && (count > 0); edge++) {
            float a = second.edgeA[edge];
            float b = second.edgeB[edge];
            float c = second.edgeC[edge];
            float[] nextXs = new float[count + 1];
            float[] nextYs = new float[count + 1];
            int nextCount = 0;
            for (int i = 0; i < count; i++) {
                int following = (i + 1) % count;
                float distance = a * xs[i] + b * ys[i] + c;
                float followingDistance = a * xs[following] + b * ys[following] + c;
                if (distance >= 0.0F) {
                    nextXs[nextCount] = xs[i];
                    nextYs[nextCount] = ys[i];
                    nextCount++;
                }
                if ((distance >= 0.0F) != (followingDistance >= 0.0F)) {
                    float t = distance / (distance - followingDistance);
                    nextXs[nextCount] = xs[i] + (xs[following] - xs[i]) * t;
                    nextYs[nextCount] = ys[i] + (ys[following] - ys[i]) * t;
                    nextCount++;
                }
            }
            xs = nextXs;
            ys = nextYs;
            count = nextCount;
        }
        if (count < 3) return EMPTY;
        return new ScissorShape(Arrays.copyOf(xs, count), Arrays.copyOf(ys, count));
    }

    /** Returns if the shape shows nothing, so everything rendered in it gets cut away. */
    public boolean isEmpty() {
        return this.cornersX.length < 3;
    }

    public boolean contains(double x, double y) {
        if (this.isEmpty()) return false;
        for (int i = 0; i < this.edgeA.length; i++) {
            if ((this.edgeA[i] * x + this.edgeB[i] * y + this.edgeC[i]) < -EDGE_TOLERANCE) return false;
        }
        return true;
    }

    public int getCornerCount() {
        return this.cornersX.length;
    }

    public float getCornerX(int index) {
        return this.cornersX[index];
    }

    public float getCornerY(int index) {
        return this.cornersY[index];
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ScissorShape[");
        for (int i = 0; i < this.cornersX.length; i++) {
            if (i > 0) builder.append(", ");
            builder.append('(').append(this.cornersX[i]).append(", ").append(this.cornersY[i]).append(')');
        }
        return builder.append(']').toString();
    }

}
