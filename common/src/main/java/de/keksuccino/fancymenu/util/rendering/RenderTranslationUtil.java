package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

public final class RenderTranslationUtil {

    private static final float DEFAULT_TRANSLATION_FANCYMENU = 0.0F;
    private static final ThreadLocal<TranslationState> ACTIVE_RENDER_TRANSLATION_FANCYMENU = ThreadLocal.withInitial(TranslationState::new);

    private RenderTranslationUtil() {
    }

    /**
     * Returns the current X translation in screen space at the exact moment this method is called.
     * <p>
     * This is the effective translation applied by the active {@link com.mojang.blaze3d.vertex.PoseStack}
     * after combining:
     * <ul>
     *     <li>The current Minecraft window GUI scale, and</li>
     *     <li>The live translation offsets accumulated via {@link com.mojang.blaze3d.vertex.PoseStack#translate(float, float, float)}.</li>
     * </ul>
     * The returned value is expressed in window pixels (screen space), ready to be used for
     * manual reversal or alignment during rendering.
     *
     * @return current absolute X translation in screen space
     */
    public static float getCurrentRenderTranslationX() {
        float baseScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        return baseScale * ACTIVE_RENDER_TRANSLATION_FANCYMENU.get().x;
    }

    /**
     * Returns the current Y translation in screen space at the exact moment this method is called.
     * <p>
     * This is the effective translation applied by the active {@link com.mojang.blaze3d.vertex.PoseStack}
     * after combining:
     * <ul>
     *     <li>The current Minecraft window GUI scale, and</li>
     *     <li>The live translation offsets accumulated via {@link com.mojang.blaze3d.vertex.PoseStack#translate(float, float, float)}.</li>
     * </ul>
     * The returned value is expressed in window pixels (screen space), ready to be used for
     * manual reversal or alignment during rendering.
     *
     * @return current absolute Y translation in screen space
     */
    public static float getCurrentRenderTranslationY() {
        float baseScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        return baseScale * ACTIVE_RENDER_TRANSLATION_FANCYMENU.get().y;
    }

    /**
     * Returns the current Z translation in screen space at the exact moment this method is called.
     * <p>
     * This uses the same conversion logic as the X/Y components, even though GUI rendering
     * typically operates in 2D. The value is still provided for completeness when 3D-style
     * translations are used in UI rendering.
     *
     * @return current absolute Z translation in screen space
     */
    public static float getCurrentRenderTranslationZ() {
        float baseScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        return baseScale * ACTIVE_RENDER_TRANSLATION_FANCYMENU.get().z;
    }

    /**
     * Returns the current X translation contributed only by {@code PoseStack#translate(...)} calls.
     * <p>
     * This does <strong>not</strong> include the Minecraft window GUI scale. Use this when you want
     * the raw pose-stack translation in GUI units (before conversion to window pixels).
     *
     * @return current pose-only X translation
     */
    public static float getCurrentAdditionalRenderTranslationX() {
        return ACTIVE_RENDER_TRANSLATION_FANCYMENU.get().x;
    }

    /**
     * Returns the current Y translation contributed only by {@code PoseStack#translate(...)} calls.
     * <p>
     * This does <strong>not</strong> include the Minecraft window GUI scale. Use this when you want
     * the raw pose-stack translation in GUI units (before conversion to window pixels).
     *
     * @return current pose-only Y translation
     */
    public static float getCurrentAdditionalRenderTranslationY() {
        return ACTIVE_RENDER_TRANSLATION_FANCYMENU.get().y;
    }

    /**
     * Returns the current Z translation contributed only by {@code PoseStack#translate(...)} calls.
     * <p>
     * This does <strong>not</strong> include the Minecraft window GUI scale. It represents the
     * pose-stack translation in GUI units on the Z axis.
     *
     * @return current pose-only Z translation
     */
    public static float getCurrentAdditionalRenderTranslationZ() {
        return ACTIVE_RENDER_TRANSLATION_FANCYMENU.get().z;
    }

    /**
     * Resets the tracked pose-only translation back to {@code 0,0,0}.
     * <p>
     * This is intended for internal use when a new {@link net.minecraft.client.gui.GuiGraphics}
     * instance is created or when the render context is reset.
     */
    @ApiStatus.Internal
    public static void resetActiveRenderTranslation_FancyMenu() {
        TranslationState state = ACTIVE_RENDER_TRANSLATION_FANCYMENU.get();
        state.x = DEFAULT_TRANSLATION_FANCYMENU;
        state.y = DEFAULT_TRANSLATION_FANCYMENU;
        state.z = DEFAULT_TRANSLATION_FANCYMENU;
    }

    /**
     * Sets the currently tracked pose-only translation.
     * <p>
     * This is updated internally by {@link com.mojang.blaze3d.vertex.PoseStack#translate(float, float, float)}
     * hooks and should not be called by external code.
     *
     * @param x current pose-only X translation
     * @param y current pose-only Y translation
     * @param z current pose-only Z translation
     */
    @ApiStatus.Internal
    public static void setActiveRenderTranslation_FancyMenu(float x, float y, float z) {
        TranslationState state = ACTIVE_RENDER_TRANSLATION_FANCYMENU.get();
        state.x = x;
        state.y = y;
        state.z = z;
    }

    @ApiStatus.Internal
    public static final class TranslationState {
        public float x = DEFAULT_TRANSLATION_FANCYMENU;
        public float y = DEFAULT_TRANSLATION_FANCYMENU;
        public float z = DEFAULT_TRANSLATION_FANCYMENU;
    }

}
