package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

public final class RenderScaleUtil {

    private static final float DEFAULT_RENDER_SCALE_FANCYMENU = 1.0F;
    private static final ThreadLocal<Float> ACTIVE_RENDER_SCALE_FANCYMENU = ThreadLocal.withInitial(() -> DEFAULT_RENDER_SCALE_FANCYMENU);

    private RenderScaleUtil() {
    }

    /**
     * Returns the full render scale that is active at the exact moment this method is called.
     * <p>
     * This is a snapshot of the effective GUI scale that FancyMenu should render with, combining:
     * <ul>
     *     <li>The current Minecraft window GUI scale ({@link net.minecraft.client.Minecraft#getInstance()} →
     *     {@link com.mojang.blaze3d.platform.Window#getGuiScale()}).</li>
     *     <li>Any additional scaling applied through {@link com.mojang.blaze3d.vertex.PoseStack#scale(float, float, float)}
     *     (tracked in real time via mixins).</li>
     * </ul>
     * The returned value represents the final/absolute scale in window pixel space.
     *
     * @return current absolute render scale (window GUI scale × active pose scale)
     */
    public static float getCurrentRenderScale() {
        float baseScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        return baseScale * ACTIVE_RENDER_SCALE_FANCYMENU.get();
    }

    /**
     * Returns only the additional scale introduced via {@code PoseStack#scale(...)} calls.
     * <p>
     * This does <strong>not</strong> include the Minecraft window GUI scale. It reflects the
     * extra multiplier added on top of the base GUI scale by runtime rendering code.
     *
     * @return current pose-only render scale (1.0 means no extra scaling)
     */
    public static float getCurrentAdditionalRenderScale() {
        return ACTIVE_RENDER_SCALE_FANCYMENU.get();
    }

    /**
     * Resets the tracked pose-only scale back to {@code 1.0}.
     * <p>
     * This is intended for internal use when a new {@link net.minecraft.client.gui.GuiGraphics}
     * instance is created or when the render context is reset.
     */
    @ApiStatus.Internal
    public static void resetActiveRenderScale_FancyMenu() {
        ACTIVE_RENDER_SCALE_FANCYMENU.set(DEFAULT_RENDER_SCALE_FANCYMENU);
    }

    /**
     * Sets the currently tracked pose-only scale.
     * <p>
     * This is updated internally by {@link com.mojang.blaze3d.vertex.PoseStack#scale(float, float, float)}
     * hooks and should not be called by external code.
     *
     * @param renderScale current pose-only scale multiplier
     */
    @ApiStatus.Internal
    public static void setActiveRenderScale_FancyMenu(float renderScale) {
        ACTIVE_RENDER_SCALE_FANCYMENU.set(renderScale);
    }

    /**
     * Computes a single scalar scale factor from a {@code PoseStack#scale(x, y, z)} call.
     * <p>
     * If the scale is uniform in X/Y (or very close), the absolute X value is used.
     * Otherwise, the absolute X/Y values are averaged to get a stable 2D GUI scale.
     * The Z value is only used as a fallback if both X and Y are zero.
     *
     * @param x scale on the X axis
     * @param y scale on the Y axis
     * @param z scale on the Z axis
     * @return absolute scalar scale factor derived from the provided axes
     */
    @ApiStatus.Internal
    public static float getAbsoluteScaleFactor_FancyMenu(float x, float y, float z) {
        float absX = Math.abs(x);
        float absY = Math.abs(y);
        if (absX == 0.0F && absY == 0.0F) {
            float absZ = Math.abs(z);
            return absZ == 0.0F ? DEFAULT_RENDER_SCALE_FANCYMENU : absZ;
        }
        if (Math.abs(absX - absY) < 1.0E-4F) {
            return absX;
        }
        return (absX + absY) * 0.5F;
    }

}
