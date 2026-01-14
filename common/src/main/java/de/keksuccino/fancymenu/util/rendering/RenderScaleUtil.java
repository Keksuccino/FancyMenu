package de.keksuccino.fancymenu.util.rendering;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

public final class RenderScaleUtil {

    private static final float DEFAULT_RENDER_SCALE_FANCYMENU = 1.0F;
    private static final ThreadLocal<Float> ACTIVE_RENDER_SCALE_FANCYMENU = ThreadLocal.withInitial(() -> DEFAULT_RENDER_SCALE_FANCYMENU);

    private RenderScaleUtil() {
    }

    public static float getCurrentRenderScale() {
        float baseScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        return baseScale * ACTIVE_RENDER_SCALE_FANCYMENU.get();
    }

    @ApiStatus.Internal
    public static void resetActiveRenderScale_FancyMenu() {
        ACTIVE_RENDER_SCALE_FANCYMENU.set(DEFAULT_RENDER_SCALE_FANCYMENU);
    }

    @ApiStatus.Internal
    public static void setActiveRenderScale_FancyMenu(float renderScale) {
        ACTIVE_RENDER_SCALE_FANCYMENU.set(renderScale);
    }

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
