package de.keksuccino.fancymenu.customization.background.backgrounds.worldscene;

public final class WorldSceneRenderContext {

    private static final ThreadLocal<Boolean> RENDERING = ThreadLocal.withInitial(() -> false);

    private WorldSceneRenderContext() {
    }

    public static void begin() {
        RENDERING.set(true);
    }

    public static void end() {
        RENDERING.set(false);
    }

    public static boolean isRendering() {
        return RENDERING.get();
    }
}
