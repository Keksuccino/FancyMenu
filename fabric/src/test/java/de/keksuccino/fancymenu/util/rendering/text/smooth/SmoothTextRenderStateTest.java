package de.keksuccino.fancymenu.util.rendering.text.smooth;

import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SmoothTextRenderStateTest {

    @Test
    void usesVanillaCoveragePipeline() {
        SmoothTextRenderState state = new SmoothTextRenderState(new Matrix3x2f(), new SmoothTextMesh.Batch(), TextureSetup.noTexture(), null);

        assertSame(RenderPipelines.GUI_TEXTURED, state.pipeline());
        // A batch without quads covers no area, so the GUI renderer skips it.
        assertNull(state.bounds());
    }

    @Test
    void obsoleteSmoothTextShadersAreNotPackaged() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertNull(classLoader.getResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_text.vsh"));
        assertNull(classLoader.getResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_text.fsh"));
        assertNull(classLoader.getResource("assets/minecraft/shaders/core/fancymenu_gui_smooth_text.json"));
    }

}
