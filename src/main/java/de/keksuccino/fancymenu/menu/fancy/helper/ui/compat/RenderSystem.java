package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat;

import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;

public class RenderSystem extends GlStateManager {

    public static void color4f(float r, float g, float b, float a) {
        color(r, g, b, a);
    }

    public static void enableScissor(int x, int y, int width, int height) {
        RenderUtils.enableScissor(x, y, width, height);
    }

    public static void disableScissor() {
        RenderUtils.disableScissor();
    }

}
