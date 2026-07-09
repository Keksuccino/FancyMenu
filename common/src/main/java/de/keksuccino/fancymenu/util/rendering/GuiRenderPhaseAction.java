package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

import javax.annotation.Nullable;

/**
 * Marker for work that must execute between ordered GUI draw ranges instead of during render-state extraction.
 * Implementations intentionally emit no vertices; {@code MixinGuiRenderer} consumes them as render-phase boundaries.
 */
public interface GuiRenderPhaseAction extends GuiElementRenderState {

    void executeRender_FancyMenu();

    @Override
    default void buildVertices(VertexConsumer vertexConsumer) {
    }

    @Override
    default RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    default TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    @Nullable
    default ScreenRectangle scissorArea() {
        return null;
    }

}
