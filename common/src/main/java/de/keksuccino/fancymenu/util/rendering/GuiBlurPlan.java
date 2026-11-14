package de.keksuccino.fancymenu.util.rendering;

import org.jetbrains.annotations.NotNull;

/**
 * Texel regions that FancyMenu's GUI blur reads and writes for one blur area.
 *
 * <p>All regions are half-open integer rectangles with a bottom-left origin, in texels of the target they belong to. That's the space
 * render pass scissors use. The level textures are shared by all blur areas and frames, so everything outside the regions written for
 * the current area is stale. Each region is grown by exactly the texels the next stage samples, which keeps stale texels out of the
 * composite while touching as few texels as possible.</p>
 *
 * <p>Instances get reused to keep blur areas allocation-free, so only use them on the render thread.</p>
 */
public final class GuiBlurPlan {

    /** Texels the B-spline upsample reaches beyond a composited pixel's own level texel. */
    static final int UPSAMPLE_REACH = 2;

    private final Region composite = new Region();
    private final Region horizontal = new Region();
    private final Region vertical = new Region();
    private final Region[] downsample = new Region[GuiBlurKernel.MAX_LEVELS + 1];
    private final int[] levelWidths = new int[GuiBlurKernel.MAX_LEVELS + 1];
    private final int[] levelHeights = new int[GuiBlurKernel.MAX_LEVELS + 1];
    private GuiBlurKernel.Kernel kernel = GuiBlurKernel.resolve(0.0F);

    public GuiBlurPlan() {
        for (int i = 0; i < this.downsample.length; i++) {
            this.downsample[i] = new Region();
        }
    }

    /**
     * Plans all stages for a composite region that is already clipped to the target.
     *
     * @return false if there is nothing to composite.
     */
    public boolean prepare(@NotNull Region compositeRegion, @NotNull GuiBlurKernel.Kernel kernel, int targetWidth, int targetHeight) {
        this.kernel = kernel;
        this.composite.set(compositeRegion).clamp(targetWidth, targetHeight);
        if (this.composite.isEmpty() || !kernel.hasBlur()) {
            return !this.composite.isEmpty();
        }

        int level = kernel.level();
        this.levelWidths[0] = targetWidth;
        this.levelHeights[0] = targetHeight;
        for (int i = 1; i <= level; i++) {
            this.levelWidths[i] = levelSize(targetWidth, i);
            this.levelHeights[i] = levelSize(targetHeight, i);
        }

        int scale = kernel.levelScale();
        this.vertical.set(Math.floorDiv(this.composite.x0, scale) - UPSAMPLE_REACH, Math.floorDiv(this.composite.y0, scale) - UPSAMPLE_REACH, ceilDiv(this.composite.x1, scale) + UPSAMPLE_REACH, ceilDiv(this.composite.y1, scale) + UPSAMPLE_REACH).clamp(this.levelWidths[level], this.levelHeights[level]);
        // A bilinear pair tap reaches one texel past the tap count.
        int reach = (kernel.taps() > 0) ? kernel.taps() + 1 : 0;
        this.horizontal.set(this.vertical).expand(0, reach).clamp(this.levelWidths[level], this.levelHeights[level]);
        this.downsample[level].set(this.vertical).expand(reach, reach).clamp(this.levelWidths[level], this.levelHeights[level]);
        for (int i = level - 1; i >= 1; i--) {
            // A downsampled texel averages the 4x4 source texels around its center.
            Region next = this.downsample[i + 1];
            this.downsample[i].set(2 * next.x0 - 1, 2 * next.y0 - 1, 2 * next.x1 + 1, 2 * next.y1 + 1).clamp(this.levelWidths[i], this.levelHeights[i]);
        }
        return true;
    }

    @NotNull
    public GuiBlurKernel.Kernel kernel() {
        return this.kernel;
    }

    @NotNull
    public Region composite() {
        return this.composite;
    }

    /**
     * Region of the Gaussian's horizontal pass on the deepest level.
     */
    @NotNull
    public Region horizontal() {
        return this.horizontal;
    }

    /**
     * Region of the Gaussian's vertical pass on the deepest level, which is also what the composite samples.
     */
    @NotNull
    public Region vertical() {
        return this.vertical;
    }

    /**
     * Region of downsample {@code level} (1 is half resolution).
     */
    @NotNull
    public Region downsample(int level) {
        return this.downsample[level];
    }

    public int levelWidth(int level) {
        return this.levelWidths[level];
    }

    public int levelHeight(int level) {
        return this.levelHeights[level];
    }

    /**
     * Texel count of {@code level} along an axis of {@code size} target texels. A level texel covers the target texels
     * [i * 2^level, (i + 1) * 2^level), so the last texel may reach past the target's edge.
     */
    public static int levelSize(int size, int level) {
        return Math.max(1, ceilDiv(size, 1 << level));
    }

    /**
     * Resolves the framebuffer region a GUI-space blur area covers, including a pixel of antialiasing margin.
     *
     * @param rotation The mask matrix as row-major 2x2 matrix (m00, m01, m10, m11), which maps a screen offset from the area's center into the mask; the mask rotates and tilts around the area's center.
     * @return false if the area misses the target.
     */
    public static boolean resolveCompositeRegion(@NotNull Region out, float x, float y, float width, float height, float m00, float m01, float m10, float m11, double guiScale, int targetWidth, int targetHeight) {
        float margin = (float) (1.0D / guiScale);
        float minX = x - margin;
        float minY = y - margin;
        float maxX = x + width + margin;
        float maxY = y + height + margin;
        boolean identity = Math.abs(m00 - 1.0F) <= 1.0E-4F && Math.abs(m11 - 1.0F) <= 1.0E-4F && Math.abs(m01) <= 1.0E-4F && Math.abs(m10) <= 1.0E-4F;
        float determinant = (m00 * m11) - (m01 * m10);
        if (!identity && Float.isFinite(determinant) && (Math.abs(determinant) > 1.0E-6F)) {
            float halfWidth = width * 0.5F;
            float halfHeight = height * 0.5F;
            // The shader maps screen pixels into the mask's local space, so the mask covers the screen through the inverse of the matrix.
            // For a tilted mask, this is not its transpose, since tilting compresses the mask on the screen.
            float boundHalfWidth = (Math.abs(m11) * halfWidth + Math.abs(m01) * halfHeight) / Math.abs(determinant);
            float boundHalfHeight = (Math.abs(m10) * halfWidth + Math.abs(m00) * halfHeight) / Math.abs(determinant);
            float centerX = x + halfWidth;
            float centerY = y + halfHeight;
            minX = Math.min(minX, centerX - boundHalfWidth - margin);
            minY = Math.min(minY, centerY - boundHalfHeight - margin);
            maxX = Math.max(maxX, centerX + boundHalfWidth + margin);
            maxY = Math.max(maxY, centerY + boundHalfHeight + margin);
        }
        double x0 = Math.floor(minX * guiScale);
        double x1 = Math.ceil(maxX * guiScale);
        double y0 = Math.floor(targetHeight - maxY * guiScale);
        double y1 = Math.ceil(targetHeight - minY * guiScale);
        if (!Double.isFinite(x0) || !Double.isFinite(x1) || !Double.isFinite(y0) || !Double.isFinite(y1)) {
            out.set(0, 0, 0, 0);
            return false;
        }
        out.set(clampToTarget(x0, targetWidth), clampToTarget(y0, targetHeight), clampToTarget(x1, targetWidth), clampToTarget(y1, targetHeight));
        return !out.isEmpty();
    }

    /**
     * Intersects {@code region} with a GUI scissor rectangle (top-left origin GUI pixels), using the same pixel mapping as
     * FancyMenu's main GUI scissor.
     *
     * @return false if nothing is left.
     */
    public static boolean clipToGuiScissor(@NotNull Region region, int left, int top, int right, int bottom, double guiScale, int targetWidth, int targetHeight) {
        int clipX0 = (int) Math.max(0.0D, left * guiScale);
        int clipX1 = (int) Math.min(targetWidth, right * guiScale);
        int clipTop = (int) Math.max(0.0D, top * guiScale);
        int clipBottom = (int) Math.min(targetHeight, bottom * guiScale);
        region.intersect(clipX0, targetHeight - clipBottom, clipX1, targetHeight - clipTop);
        return !region.isEmpty();
    }

    private static int clampToTarget(double value, int max) {
        if (value <= 0.0D) {
            return 0;
        }
        return (value >= max) ? max : (int) value;
    }

    private static int ceilDiv(int value, int divisor) {
        return -Math.floorDiv(-value, divisor);
    }

    /**
     * Mutable half-open texel rectangle with a bottom-left origin.
     */
    public static final class Region {

        int x0;
        int y0;
        int x1;
        int y1;

        @NotNull
        public Region set(int x0, int y0, int x1, int y1) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            return this;
        }

        @NotNull
        public Region set(@NotNull Region other) {
            return this.set(other.x0, other.y0, other.x1, other.y1);
        }

        @NotNull
        Region expand(int horizontal, int vertical) {
            return this.set(this.x0 - horizontal, this.y0 - vertical, this.x1 + horizontal, this.y1 + vertical);
        }

        @NotNull
        Region clamp(int width, int height) {
            return this.set(Math.max(0, this.x0), Math.max(0, this.y0), Math.min(width, this.x1), Math.min(height, this.y1));
        }

        @NotNull
        Region intersect(int otherX0, int otherY0, int otherX1, int otherY1) {
            return this.set(Math.max(this.x0, otherX0), Math.max(this.y0, otherY0), Math.min(this.x1, otherX1), Math.min(this.y1, otherY1));
        }

        public boolean isEmpty() {
            return this.x1 <= this.x0 || this.y1 <= this.y0;
        }

        public int x() {
            return this.x0;
        }

        public int y() {
            return this.y0;
        }

        public int width() {
            return Math.max(0, this.x1 - this.x0);
        }

        public int height() {
            return Math.max(0, this.y1 - this.y0);
        }

        public boolean contains(int x, int y) {
            return x >= this.x0 && x < this.x1 && y >= this.y0 && y < this.y1;
        }

    }

}
