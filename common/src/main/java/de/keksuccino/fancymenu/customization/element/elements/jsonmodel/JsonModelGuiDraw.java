package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import de.keksuccino.fancymenu.util.rendering.GuiRenderPhaseAction;
import de.keksuccino.fancymenu.util.rendering.GuiScissorUtil;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

/**
 * Draws JSON models as a part of the GUI: where they are in the order of what the GUI draws, cut like everything around them,
 * and with a projection of their own.<br>
 * A model can't be drawn while the GUI only collects what it is going to draw. Everything the GUI draws afterwards would end up
 * on top of it, and the projection that is set at that time is a leftover of whatever got drawn last.
 */
public final class JsonModelGuiDraw {

    /** How far a model reaches to the front and to the back of the GUI before it gets cut off. */
    private static final float DEPTH_RANGE = 10000.0F;
    private static final Projection PROJECTION = new Projection();
    /** X, Y, width and height of what a model gets cut to, in pixels of the render target with Y counted from the bottom. */
    private static final int[] TARGET_SCISSOR = new int[4];

    @Nullable
    private static ProjectionMatrixBuffer projectionBuffer;

    private JsonModelGuiDraw() {
    }

    /**
     * Makes the GUI draw a model once it gets to where it is right now, cut to what the GUI is cut to right now.
     *
     * @param draw Draws the model to the main render target, in the coordinates of the GUI.
     */
    static void submit(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height, @NotNull Runnable draw) {
        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformAxisAligned(graphics.pose());
        GuiRenderState renderState = ((IMixinGuiGraphicsExtractor) graphics).get_guiRenderState_FancyMenu();
        // A stratum of its own keeps the model between what the GUI draws before and after it
        renderState.nextStratum();
        renderState.addGuiElement(new RenderState(bounds, GuiScissorUtil.getActiveScissor(graphics), draw));
        renderState.nextStratum();
    }

    /**
     * Frees what drawing models needs of the graphics card. It gets made again when the next model gets drawn.
     */
    public static void close() {
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
    }

    private static void draw(@Nullable ScreenRectangle scissor, @NotNull Runnable draw) {
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        double guiScale = WindowHandler.getGuiScale();
        if ((scissor != null) && !updateTargetScissor(scissor, guiScale, target.width, target.height)) return;
        if (projectionBuffer == null) projectionBuffer = new ProjectionMatrixBuffer("FancyMenu JSON model");
        PROJECTION.setupOrtho(-DEPTH_RANGE, DEPTH_RANGE, (float) (target.width / guiScale), (float) (target.height / guiScale), true);
        GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType previousProjectionType = RenderSystem.getProjectionType();
        RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(PROJECTION), ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        // What is in the depth of the screen by now has nothing to do with the model, like the world behind a screen. The GUI itself draws without it.
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(target.getDepthTexture(), 0.0);
        // Render types take the scissor they draw with from here when they get prepared
        if (scissor != null) RenderSystem.enableScissorForRenderTypeDraws(TARGET_SCISSOR[0], TARGET_SCISSOR[1], TARGET_SCISSOR[2], TARGET_SCISSOR[3]);
        try {
            draw.run();
        } finally {
            if (scissor != null) RenderSystem.disableScissorForRenderTypeDraws();
            modelView.popMatrix();
            // The GUI goes on drawing with the projection it had
            if (previousProjection != null) RenderSystem.setProjectionMatrix(previousProjection, previousProjectionType);
        }
    }

    /**
     * Works out {@link #TARGET_SCISSOR} from what the GUI is cut to. The box gets cut to the render target, because a scissor
     * that reaches out of its target or that is empty gets refused with an exception.
     *
     * @return If anything of the box is in the render target.
     */
    private static boolean updateTargetScissor(@NotNull ScreenRectangle scissor, double guiScale, int targetWidth, int targetHeight) {
        int left = Math.max(0, (int) Math.round(scissor.left() * guiScale));
        int top = Math.max(0, (int) Math.round(scissor.top() * guiScale));
        int right = Math.min(targetWidth, (int) Math.round(scissor.right() * guiScale));
        int bottom = Math.min(targetHeight, (int) Math.round(scissor.bottom() * guiScale));
        if ((right <= left) || (bottom <= top)) return false;
        TARGET_SCISSOR[0] = left;
        TARGET_SCISSOR[1] = targetHeight - bottom;
        TARGET_SCISSOR[2] = right - left;
        TARGET_SCISSOR[3] = bottom - top;
        return true;
    }

    private record RenderState(@NotNull ScreenRectangle bounds, @Nullable ScreenRectangle scissor, @NotNull Runnable draw) implements GuiRenderPhaseAction {

        @Override
        public void executeRender_FancyMenu() {
            JsonModelGuiDraw.draw(this.scissor, this.draw);
        }

    }

}
