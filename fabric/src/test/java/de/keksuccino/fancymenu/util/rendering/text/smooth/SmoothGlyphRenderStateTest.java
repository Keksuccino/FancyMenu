package de.keksuccino.fancymenu.util.rendering.text.smooth;

import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SmoothGlyphRenderStateTest {

    @Test
    void usesVanillaCoveragePipeline() {
        SmoothGlyphRenderState state = new SmoothGlyphRenderState(new Matrix3x2f(), null, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0xFFFFFFFF, null);

        assertSame(RenderPipelines.GUI_TEXTURED, state.pipeline());
    }

    @Test
    void obsoleteSmoothTextShadersAreNotPackaged() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertNull(classLoader.getResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_text.vsh"));
        assertNull(classLoader.getResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_text.fsh"));
        assertNull(classLoader.getResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_text.json"));
    }
}
