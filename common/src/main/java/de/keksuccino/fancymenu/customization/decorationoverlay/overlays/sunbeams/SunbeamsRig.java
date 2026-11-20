package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * Everything about the light that stays fixed while the sun and the elements don't move.<br><br>
 *
 * Every light value on the screen only depends on the direction and the distance from the sun, so every pixel stores its direction
 * (as one of {@link #BINS} bins across the fan of directions that cover the screen) and its distance once. A frame then only refreshes
 * small tables over the direction ({@link SunbeamsRays}). The rig also stores how much light the elements leave in their shade at every
 * pixel, and the edge pixels of every element that face the sun and are not hidden behind another element.<br>
 * Building a rig takes a few milliseconds, so overlays build it off the render thread: everything but the sun glow is done once
 * {@link #buildShade} returned, and the direction and distance tables never change after the constructor, so new rigs can share them.
 */
final class SunbeamsRig {

    /** Directions across the fan. */
    static final int BINS = 2048;
    /** Light left in the shade behind an element: in its core and in the softer rim around it. The face of an element gets none, so labels stay clean. */
    static final float UMBRA = 0.12F;
    static final float PENUMBRA = 0.5F;
    /** How fast a shade narrows with the distance behind its element, so it ends in a point like the rays do. */
    static final float TAPER = 0.5F;
    /** Pixels from the edge of a shade to where its core begins. */
    private static final float UMBRA_DEPTH = 3.0F;
    static final byte SHADE_FACE = 0;
    static final byte SHADE_UMBRA = 1;
    static final byte SHADE_PENUMBRA = 2;
    static final byte SHADE_LIT = 3;
    /** Light left at every shade code. */
    static final float[] SHADE_LIGHT = {0.0F, UMBRA, PENUMBRA, 1.0F};
    private static final float PI = (float)Math.PI;

    final int width;
    final int height;
    final float sunX;
    final float sunY;
    /** Direction from the sun to the middle of the screen, all other directions are stored relative to it */
    private final float centerAngle;
    /** Relative direction of bin 0 and the width of the fan, in radians */
    final float fanStart;
    final float fanWidth;
    /** Bins per radian */
    final float binScale;
    /** Direction bin of every pixel, row by row */
    final char[] bins;
    /** Distance of every pixel from the sun */
    final float[] distances;
    /** Shade code of every pixel, see {@link #SHADE_LIGHT} */
    final byte[] shade;
    /** Light of the sun glow at every pixel, see {@link #setGlow} */
    final float[] glow;
    private float glowStrength = 0.0F;
    private float glowRadius = 0.0F;
    /** The lit element edges: pixel position, its index in the pixel tables, and its bevel pixel one step into the element. */
    int rimCount = 0;
    int[] rimX = new int[0];
    int[] rimY = new int[0];
    int[] rimIndex = new int[0];
    int[] rimBevelX = new int[0];
    int[] rimBevelY = new int[0];
    /** Area the lit edges and their bevels cover: left, top, right and bottom (exclusive). Empty when right <= left. */
    int rimLeft = 0;
    int rimTop = 0;
    int rimRight = 0;
    int rimBottom = 0;
    /** The element boxes the shade got built for (x, y, width and height of each), NULL while it has no shade */
    @Nullable
    int[] shadeBoxes = null;

    /**
     * Builds the direction and distance tables for a sun outside of the screen. All pixels are lit until {@link #buildShade} runs.
     */
    SunbeamsRig(float sunX, float sunY, int width, int height) {
        this.width = width;
        this.height = height;
        this.sunX = sunX;
        this.sunY = sunY;
        this.centerAngle = (float)Math.atan2(height * 0.5F - sunY, width * 0.5F - sunX);
        float first = Float.MAX_VALUE;
        float last = -Float.MAX_VALUE;
        float[][] corners = {{0, 0}, {width, 0}, {0, height}, {width, height}};
        for (float[] corner : corners) {
            float angle = this.relativeAngle(corner[0] - sunX, corner[1] - sunY);
            first = Math.min(first, angle);
            last = Math.max(last, angle);
        }
        this.fanStart = first;
        this.fanWidth = Math.max(1.0E-4F, last - first);
        this.binScale = (BINS - 1) / this.fanWidth;
        int size = width * height;
        this.bins = new char[size];
        this.distances = new float[size];
        this.shade = new byte[size];
        this.glow = new float[size];
        Arrays.fill(this.shade, SHADE_LIT);
        for (int y = 0, j = 0; y < height; y++) {
            float dy = y + 0.5F - sunY;
            for (int x = 0; x < width; x++, j++) {
                float dx = x + 0.5F - sunX;
                this.distances[j] = (float)Math.sqrt(dx * dx + dy * dy);
                this.bins[j] = (char)this.binOf(this.relativeAngle(dx, dy));
            }
        }
    }

    /**
     * Starts a rig for the same sun and screen as another one, with the same direction and distance tables, so only the shade
     * needs to be built. All pixels are lit until {@link #buildShade} runs.
     */
    SunbeamsRig(@NotNull SunbeamsRig geometry) {
        this.width = geometry.width;
        this.height = geometry.height;
        this.sunX = geometry.sunX;
        this.sunY = geometry.sunY;
        this.centerAngle = geometry.centerAngle;
        this.fanStart = geometry.fanStart;
        this.fanWidth = geometry.fanWidth;
        this.binScale = geometry.binScale;
        this.bins = geometry.bins;
        this.distances = geometry.distances;
        this.shade = new byte[geometry.shade.length];
        this.glow = new float[geometry.glow.length];
        Arrays.fill(this.shade, SHADE_LIT);
    }

    /**
     * Places the sun behind a side of a screen, turned clockwise around the middle of the screen by an angle.
     * It sits {@code margin} pixels outside of the screen, measured along the line from the middle of the screen.
     *
     * @param result gets x and y of the sun
     */
    static void placeSun(@NotNull SunbeamsSide side, float angleDegrees, int width, int height, float margin, @NotNull float[] result) {
        double angle = Math.toRadians(angleDegrees);
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        // Screen y points down, so this turns the outward direction clockwise on the screen
        float ux = side.outwardX * cos - side.outwardY * sin;
        float uy = side.outwardX * sin + side.outwardY * cos;
        float toSideX = (Math.abs(ux) > 1.0E-6F) ? (width * 0.5F / Math.abs(ux)) : Float.MAX_VALUE;
        float toSideY = (Math.abs(uy) > 1.0E-6F) ? (height * 0.5F / Math.abs(uy)) : Float.MAX_VALUE;
        float distance = Math.min(toSideX, toSideY) + margin;
        result[0] = width * 0.5F + ux * distance;
        result[1] = height * 0.5F + uy * distance;
    }

    /**
     * Stores the light of the sun glow at every pixel. It fades from the sun out to {@code radius} pixels.
     *
     * @param strength 0 for no glow
     */
    void setGlow(float strength, float radius) {
        if ((strength == this.glowStrength) && (radius == this.glowRadius)) return;
        this.glowStrength = strength;
        this.glowRadius = radius;
        float inverseRadius = 1.0F / radius;
        for (int j = 0; j < this.glow.length; j++) {
            float q = 1.0F - this.distances[j] * inverseRadius;
            this.glow[j] = (q > 0.0F) ? (strength * q * (float)Math.sqrt(q)) : 0.0F;
        }
    }

    /**
     * Direction of a vector from the sun, relative to the direction to the middle of the screen, from -PI to PI.
     */
    private float relativeAngle(float dx, float dy) {
        float angle = (float)Math.atan2(dy, dx) - this.centerAngle;
        if (angle > PI) angle -= 2.0F * PI;
        else if (angle < -PI) angle += 2.0F * PI;
        return angle;
    }

    private int binOf(float relativeAngle) {
        return PixelMath.clamp(Math.round((relativeAngle - this.fanStart) * this.binScale), 0, BINS - 1);
    }

    /**
     * Stores the shade of every pixel and the lit edges of the elements, for element boxes in GUI pixels.
     */
    void buildShade(@NotNull List<CollisionAreaBounds> boxes) {
        Arrays.fill(this.shade, SHADE_LIT);
        this.shadeBoxes = snapshot(boxes);
        int count = 0;
        for (int i = 0; i < boxes.size(); i++) {
            CollisionAreaBounds box = boxes.get(i);
            if ((box.width() > 0) && (box.height() > 0)) count++;
        }
        int[] rects = new int[count * 4];
        count = 0;
        for (int i = 0; i < boxes.size(); i++) {
            CollisionAreaBounds box = boxes.get(i);
            if ((box.width() <= 0) || (box.height() <= 0)) continue;
            rects[count * 4] = box.x();
            rects[count * 4 + 1] = box.y();
            rects[count * 4 + 2] = box.width();
            rects[count * 4 + 3] = box.height();
            count++;
        }
        for (int i = 0; i < count; i++) this.shadeBehind(rects, i);
        this.buildRim(rects, count);
    }

    /**
     * Casts the shade of one element. Only pixels whose direction lies inside the element's silhouette can be in its shade,
     * so the exact test only runs for those.
     */
    private void shadeBehind(@NotNull int[] rects, int index) {
        int rx = rects[index * 4];
        int ry = rects[index * 4 + 1];
        int rw = rects[index * 4 + 2];
        int rh = rects[index * 4 + 3];
        // The corners where the silhouette starts and ends, seen from the sun
        float startAngle = Float.MAX_VALUE;
        float endAngle = -Float.MAX_VALUE;
        float startX = 0.0F;
        float startY = 0.0F;
        float endX = 0.0F;
        float endY = 0.0F;
        int[][] corners = {{rx, ry}, {rx + rw, ry}, {rx, ry + rh}, {rx + rw, ry + rh}};
        for (int[] corner : corners) {
            float cx = corner[0] - this.sunX;
            float cy = corner[1] - this.sunY;
            float angle = this.relativeAngle(cx, cy);
            float length = (float)Math.sqrt(cx * cx + cy * cy);
            if (angle < startAngle) {
                startAngle = angle;
                startX = cx / length;
                startY = cy / length;
            }
            if (angle > endAngle) {
                endAngle = angle;
                endX = cx / length;
                endY = cy / length;
            }
        }
        int binFrom = this.binOf(startAngle) - 1;
        int binTo = this.binOf(endAngle) + 1;
        float[] hit = new float[2];
        for (int y = 0, j = 0; y < this.height; y++) {
            boolean rowInside = (y >= ry) && (y < ry + rh);
            float dy = y + 0.5F - this.sunY;
            for (int x = 0; x < this.width; x++, j++) {
                if (rowInside && (x >= rx) && (x < rx + rw)) {
                    this.shade[j] = SHADE_FACE;
                    continue;
                }
                int bin = this.bins[j];
                if ((bin < binFrom) || (bin > binTo) || (this.shade[j] == SHADE_FACE)) continue;
                float dx = x + 0.5F - this.sunX;
                float d = this.distances[j];
                if (!hitBox(rx, ry, rw, rh, this.sunX, this.sunY, dx / d, dy / d, hit) || (hit[1] > d)) continue;
                // Distance of the pixel from the nearer edge of the shade (the rays through the silhouette corners), which narrows
                // behind the element so the shade ends in a point like the rays do. The cross products are the distances to those rays.
                float fromStart = startX * dy - startY * dx;
                float fromEnd = dx * endY - dy * endX;
                float depth = Math.min(fromStart, fromEnd) - (d - hit[1]) * TAPER;
                if (depth <= 0.0F) continue;
                byte code = (depth > UMBRA_DEPTH) ? SHADE_UMBRA : SHADE_PENUMBRA;
                if (code < this.shade[j]) this.shade[j] = code;
            }
        }
    }

    /**
     * Collects the edge pixels of every element that face the sun (top or bottom row, left or right column) and that no other
     * element hides from it. Each gets a bevel pixel one step into the element.
     */
    private void buildRim(@NotNull int[] rects, int count) {
        int capacity = 0;
        for (int i = 0; i < count; i++) capacity += rects[i * 4 + 2] + rects[i * 4 + 3];
        this.rimX = new int[capacity];
        this.rimY = new int[capacity];
        this.rimIndex = new int[capacity];
        this.rimBevelX = new int[capacity];
        this.rimBevelY = new int[capacity];
        this.rimCount = 0;
        this.rimLeft = Integer.MAX_VALUE;
        this.rimTop = Integer.MAX_VALUE;
        this.rimRight = Integer.MIN_VALUE;
        this.rimBottom = Integer.MIN_VALUE;
        float[] hit = new float[2];
        for (int i = 0; i < count; i++) {
            int rx = rects[i * 4];
            int ry = rects[i * 4 + 1];
            int rw = rects[i * 4 + 2];
            int rh = rects[i * 4 + 3];
            int rowY = -1;
            int bevelDy = 0;
            if (this.sunY < ry) {
                rowY = ry;
                bevelDy = 1;
            } else if (this.sunY > ry + rh) {
                rowY = ry + rh - 1;
                bevelDy = -1;
            }
            int columnX = -1;
            int bevelDx = 0;
            if (this.sunX < rx) {
                columnX = rx;
                bevelDx = 1;
            } else if (this.sunX > rx + rw) {
                columnX = rx + rw - 1;
                bevelDx = -1;
            }
            if (rowY >= 0) {
                for (int x = rx; x < rx + rw; x++) this.addRim(rects, count, i, x, rowY, x, rowY + bevelDy, hit);
            }
            if (columnX >= 0) {
                // The corner pixel already belongs to the lit row
                int from = (bevelDy > 0) ? (ry + 1) : ry;
                int to = (bevelDy < 0) ? (ry + rh - 1) : (ry + rh);
                for (int y = from; y < to; y++) this.addRim(rects, count, i, columnX, y, columnX + bevelDx, y, hit);
            }
        }
        if (this.rimCount == 0) {
            this.rimLeft = 0;
            this.rimTop = 0;
            this.rimRight = 0;
            this.rimBottom = 0;
        }
    }

    private void addRim(@NotNull int[] rects, int count, int owner, int x, int y, int bevelX, int bevelY, @NotNull float[] hit) {
        if ((x < 0) || (y < 0) || (x >= this.width) || (y >= this.height)) return;
        float dx = x + 0.5F - this.sunX;
        float dy = y + 0.5F - this.sunY;
        float d = (float)Math.sqrt(dx * dx + dy * dy);
        for (int n = 0; n < count; n++) {
            if (n == owner) continue;
            if (hitBox(rects[n * 4], rects[n * 4 + 1], rects[n * 4 + 2], rects[n * 4 + 3], this.sunX, this.sunY, dx / d, dy / d, hit) && (hit[1] < d)) return;
        }
        int k = this.rimCount++;
        this.rimX[k] = x;
        this.rimY[k] = y;
        this.rimIndex[k] = y * this.width + x;
        this.rimBevelX[k] = bevelX;
        this.rimBevelY[k] = bevelY;
        this.rimLeft = Math.min(this.rimLeft, Math.min(x, bevelX));
        this.rimTop = Math.min(this.rimTop, Math.min(y, bevelY));
        this.rimRight = Math.max(this.rimRight, Math.max(x, bevelX) + 1);
        this.rimBottom = Math.max(this.rimBottom, Math.max(y, bevelY) + 1);
    }

    /**
     * Entry and exit distance of a ray from the sun along the unit direction through a box. Returns false when it misses.
     */
    static boolean hitBox(int x, int y, int width, int height, float fromX, float fromY, float dirX, float dirY, @NotNull float[] result) {
        float ax = (x - fromX) / dirX;
        float bx = (x + width - fromX) / dirX;
        float ay = (y - fromY) / dirY;
        float by = (y + height - fromY) / dirY;
        float enter = Math.max(Math.min(ax, bx), Math.min(ay, by));
        float exit = Math.min(Math.max(ax, bx), Math.max(ay, by));
        if (!(exit > enter) || !(exit > 0.0F)) return false;
        result[0] = enter;
        result[1] = exit;
        return true;
    }

    /**
     * Returns x, y, width and height of every box.
     */
    @NotNull
    static int[] snapshot(@NotNull List<CollisionAreaBounds> boxes) {
        int[] snapshot = new int[boxes.size() * 4];
        for (int i = 0; i < boxes.size(); i++) {
            CollisionAreaBounds box = boxes.get(i);
            snapshot[i * 4] = box.x();
            snapshot[i * 4 + 1] = box.y();
            snapshot[i * 4 + 2] = box.width();
            snapshot[i * 4 + 3] = box.height();
        }
        return snapshot;
    }

    /**
     * Whether the shade got built for these boxes. Doesn't allocate, so it can run every frame.
     */
    boolean hasShadeFor(@NotNull List<CollisionAreaBounds> boxes) {
        int[] built = this.shadeBoxes;
        if ((built == null) || (built.length != boxes.size() * 4)) return false;
        for (int i = 0; i < boxes.size(); i++) {
            CollisionAreaBounds box = boxes.get(i);
            if ((built[i * 4] != box.x()) || (built[i * 4 + 1] != box.y()) || (built[i * 4 + 2] != box.width()) || (built[i * 4 + 3] != box.height())) return false;
        }
        return true;
    }

    /**
     * Whether the rig was built for this sun and screen.
     */
    boolean matches(float sunX, float sunY, int width, int height) {
        return (this.sunX == sunX) && (this.sunY == sunY) && (this.width == width) && (this.height == height);
    }

}
