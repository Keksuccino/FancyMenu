package de.keksuccino.fancymenu.util.rendering.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

public class TestUIComponent extends UIComponent {

    @Override
    public void renderComponent(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {

        RenderSystem.enableBlend();
        resetShaderColor();
        fillF(pose, x, y, x + width, y + height, this.isHovered() ? getUIColorScheme().error_text_color.getColorInt() : getUIColorScheme().success_text_color.getColorInt());
        resetShaderColor();

    }

    @Override
    protected float getComponentX(@NotNull UIComponentPositioner positioner) {
        return positioner.getScreenWidth() - positioner.getComponentWidth() - 20;
    }

    @Override
    protected float getComponentY(@NotNull UIComponentPositioner positioner) {
        return positioner.getScreenHeight() - positioner.getComponentHeight() - 20;
    }

    @Override
    public float getComponentWidth() {
        return 100;
    }

    @Override
    public float getComponentHeight() {
        return 100;
    }

    @Override
    protected boolean mouseClickedComponent(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected boolean mouseReleasedComponent(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected boolean mouseDraggedComponent(double mouseX, double mouseY, int button, double d1, double d2) {
        return false;
    }

    @Override
    protected boolean mouseScrolledComponent(double mouseX, double mouseY, double scrollDelta) {
        return false;
    }

    @Override
    protected void mouseMovedComponent(double mouseX, double mouseY) {
    }

}
