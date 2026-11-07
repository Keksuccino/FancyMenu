package de.keksuccino.fancymenu.util.rendering;

/**
 * Resolves how FancyMenu's GUI blur reaches a framebuffer blur radius.
 *
 * <p>The blur runs on a chain of half-resolution levels: every level averages a 4x4 texel footprint of the level above it, the deepest
 * level gets a separable Gaussian, and the composite pass upsamples that level with a cubic B-spline. Each stage widens the blur by a
 * known variance (1.25 source texels² per downsample step, 1/3 deepest-level texels² for the B-spline). The Gaussian gets exactly the
 * variance that is left, so the blur strength changes continuously with the radius, even when the level count changes.</p>
 *
 * <p>The target variance per radius matches FancyMenu's earlier six-pass box blur (box radii R, R/2 and R/4 per axis), so existing
 * layouts keep their look.</p>
 */
public final class GuiBlurKernel {

    /** Deepest downsample level. Level 7 (1/128 resolution) already covers {@link GuiBlurRadius#MAX_FRAMEBUFFER_RADIUS}. */
    public static final int MAX_LEVELS = 8;
    /** Upper bound of Gaussian taps per side, which bounds the shader loop independently of the uniforms it gets. */
    public static final int MAX_TAPS = 24;
    private static final double DOWNSAMPLE_VARIANCE = 1.25D;
    private static final double UPSAMPLE_VARIANCE = 1.0D / 3.0D;
    private static final double MIN_LEVEL_SIGMA = 1.0D;
    private static final double MIN_GAUSSIAN_SIGMA = 0.05D;
    private static final double TAPS_PER_SIGMA = 3.0D;
    private static final Kernel NONE = new Kernel(1, 0.0F, 0, 0.0F);

    private GuiBlurKernel() {
    }

    /**
     * Resolves the blur stages for a framebuffer radius. Radii that are smaller than the blur of a single downsample step blend
     * that smallest blur with the sharp backdrop by {@link Kernel#strength()}, which keeps the blur variance continuous down to zero.
     */
    public static Kernel resolve(float framebufferRadius) {
        if (!Float.isFinite(framebufferRadius) || framebufferRadius <= 0.0F) {
            return NONE;
        }
        double variance = targetVariance(framebufferRadius);
        int level = 1;
        while (level < MAX_LEVELS && variance >= chainVariance(level + 1) + MIN_LEVEL_SIGMA * MIN_LEVEL_SIGMA * levelArea(level + 1)) {
            level++;
        }
        double residualVariance = (variance - chainVariance(level)) / levelArea(level);
        if (residualVariance <= 0.0D) {
            return new Kernel(level, 0.0F, 0, (float) Math.min(1.0D, variance / chainVariance(level)));
        }
        double sigma = Math.sqrt(residualVariance);
        if (sigma < MIN_GAUSSIAN_SIGMA) {
            return new Kernel(level, 0.0F, 0, 1.0F);
        }
        int taps = (int) Math.min(MAX_TAPS, Math.ceil(sigma * TAPS_PER_SIGMA));
        return new Kernel(level, (float) sigma, taps, 1.0F);
    }

    /**
     * Blur variance (framebuffer pixels² per axis) of the earlier box blur: boxes of radius R, R/2 and R/4, each with variance r(r+1)/3.
     */
    static double targetVariance(double framebufferRadius) {
        return (1.3125D * framebufferRadius * framebufferRadius + 1.75D * framebufferRadius) / 3.0D;
    }

    /**
     * Variance (framebuffer pixels² per axis) that the downsample steps down to {@code level} and the B-spline upsample from it add.
     */
    static double chainVariance(int level) {
        double area = levelArea(level);
        return DOWNSAMPLE_VARIANCE * (area - 1.0D) / 3.0D + UPSAMPLE_VARIANCE * area;
    }

    /**
     * Framebuffer pixels² covered by one texel of {@code level}.
     */
    static double levelArea(int level) {
        return (double) (1L << (2 * level));
    }

    /**
     * @param level Deepest downsample level, 1 is half resolution.
     * @param sigma Gaussian sigma in texels of {@code level}, 0 when the level needs no Gaussian.
     * @param taps Gaussian taps per side.
     * @param strength How much of the blur gets mixed over the sharp backdrop, 0 when there is no blur at all.
     */
    public record Kernel(int level, float sigma, int taps, float strength) {

        public boolean hasBlur() {
            return this.strength > 0.0F;
        }

        public int levelScale() {
            return 1 << this.level;
        }

    }

}
