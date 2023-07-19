package de.keksuccino.fancymenu.util.rendering.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;

public abstract class UIComponent extends UIBase implements GuiEventListener, Renderable, NarratableEntry {

    public float posZ = 0f;
    protected boolean hovered = false;
    protected boolean visible = true;
    protected Minecraft mc = Minecraft.getInstance();

    /**
     * Make sure to render everything here at X=0 and Y=0!<br>
     * The {@link UIComponent} gets translated to the correct position!
     */
    public abstract void renderComponent(@NotNull PoseStack pose, double mouseX, double mouseY, float partial);

    @Override
    public void render(@NotNull PoseStack pose, int ignoredMouseX, int ignoredMouseY, float partial) {

        if (!this.isVisible()) return;

        this.hovered = this.isMouseOver();

        pose.pushPose();
        pose.scale(this.getFixedComponentScale(), this.getFixedComponentScale(), this.getFixedComponentScale());
        pose.translate(this.getComponentX(), this.getComponentY(), this.posZ);

        this.renderComponent(pose, this.getComponentMouseX(), this.getComponentMouseY(), partial);

        pose.popPose();

    }

    /**
     * This is always 0 and is just a dummy coordinate to not get too confused with rendering everything at X=0 Y=0 in {@link UIComponent#renderComponent(PoseStack, double, double, float)}.
     */
    protected float getRenderX() {
        return 0;
    }

    /**
     * This is always 0 and is just a dummy coordinate to not get too confused with rendering everything at X=0 Y=0 in {@link UIComponent#renderComponent(PoseStack, double, double, float)}.
     */
    protected float getRenderY() {
        return 0;
    }

    public abstract float getComponentX();

    public abstract float getComponentY();

    public abstract float getComponentWidth();

    public abstract float getComponentHeight();

    /**
     * Since {@link UIComponent}s get rendered in a different scale than the normal GUI, it's important to scale the mouse position to the component's scale.<br>
     * The position returned by this method is correctly scaled.
     */
    public double getComponentMouseX() {
        return this.mc.mouseHandler.xpos() / this.getComponentScale();
    }

    /**
     * Since {@link UIComponent}s get rendered in a different scale than the normal GUI, it's important to scale the mouse position to the component's scale.<br>
     * The position returned by this method is correctly scaled.
     */
    public double getComponentMouseY() {
        return this.mc.mouseHandler.ypos() / this.getComponentScale();
    }

    protected float getScreenWidth() {
        return this.mc.getWindow().getWidth() / this.getComponentScale();
    }

    protected float getScreenHeight() {
        return this.mc.getWindow().getHeight() / this.getComponentScale();
    }

    /**
     * Checks if a component area is hovered.<br>
     * Set {@code isRenderPosition} to {@code true}, if the XY-position is a component render position (rendered at X=0 and Y=0).
     */
    protected boolean isComponentAreaHovered(float x, float y, float width, float height, boolean isRenderPosition) {
        if (isRenderPosition) {
            x += this.getComponentX();
            y += this.getComponentY();
        }
        return isXYInArea(this.getComponentMouseX(), this.getComponentMouseY(), x, y, width+1, height+1);
    }

    /**
     * The scale of the {@link UIComponent}.
     */
    public float getComponentScale() {
        return getUIScale();
    }

    /**
     * This scale works against the actual GUI scale to make it possible to render the {@link UIComponent} in a different scale than the GUI scale.
     */
    public float getFixedComponentScale() {
        return calculateFixedScale(this.getComponentScale());
    }

    /**
     * Scissor with automatic scale handling.<br>
     * Set {@code isRenderPosition} to {@code true}, if the XY-position is a component render position (rendered at X=0 and Y=0).
     */
    protected void enableComponentScissor(int x, int y, int width, int height, boolean isRenderPosition) {
        if (isRenderPosition) {
            x += this.getComponentX();
            y += this.getComponentY();
        }
        int scissorX = (int) (x * this.getFixedComponentScale());
        int scissorY = (int) (y * this.getFixedComponentScale());
        int scissorWidth = (int) (width * this.getFixedComponentScale());
        int scissorHeight = (int) (height * this.getFixedComponentScale());
        enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
    }

    protected void disableComponentScissor() {
        disableScissor();
    }

    public boolean isHovered() {
        if (!this.isVisible()) return false;
        return this.hovered;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    protected abstract boolean mouseClickedComponent(double mouseX, double mouseY, int button);

    @Deprecated
    @Override
    public boolean mouseClicked(double ignoredMouseX, double ignoredMouseY, int button) {
        return this.mouseClickedComponent(this.getComponentMouseX(), this.getComponentMouseY(), button);
    }

    protected abstract boolean mouseReleasedComponent(double mouseX, double mouseY, int button);

    @Deprecated
    @Override
    public boolean mouseReleased(double ignoredMouseX, double ignoredMouseY, int button) {
        return this.mouseReleasedComponent(this.getComponentMouseX(), this.getComponentMouseY(), button);
    }

    protected abstract boolean mouseDraggedComponent(double mouseX, double mouseY, int button, double d1, double d2);

    @Deprecated
    @Override
    public boolean mouseDragged(double ignoredMouseX, double ignoredMouseY, int button, double d1, double d2) {
        return this.mouseDraggedComponent(this.getComponentMouseX(), this.getComponentMouseY(), button, d1, d2);
    }

    protected abstract boolean mouseScrolledComponent(double mouseX, double mouseY, double scrollDelta);

    @Deprecated
    @Override
    public boolean mouseScrolled(double ignoredMouseX, double ignoredMouseY, double scrollDelta) {
        return this.mouseScrolledComponent(this.getComponentMouseX(), this.getComponentMouseY(), scrollDelta);
    }

    protected abstract void mouseMovedComponent(double mouseX, double mouseY);

    @Deprecated
    @Override
    public void mouseMoved(double ignoredMouseX, double ignoredMouseY) {
        this.mouseMovedComponent(this.getComponentMouseX(), this.getComponentMouseY());
    }

    public boolean isMouseOver() {
        if (!this.isVisible()) return false;
        return this.isComponentAreaHovered(this.getComponentX(), this.getComponentY(), this.getComponentWidth(), this.getComponentHeight(), false);
    }

    @Deprecated
    @Override
    public boolean isMouseOver(double ignoredMouseX, double ignoredMouseY) {
        return this.isMouseOver();
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

}