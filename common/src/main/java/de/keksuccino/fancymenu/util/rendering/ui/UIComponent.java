package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public abstract class UIComponent extends UIBase implements FocuslessContainerEventHandler, Renderable, NarratableEntry {

    public float posZ = 0f;
    protected boolean hovered = false;
    protected boolean visible = true;
    protected Minecraft mc = Minecraft.getInstance();
    protected boolean dragging = false;
    protected final List<GuiEventListener> children = new ArrayList<>();

    /**
     * Make sure to render everything here at X=0 and Y=0!<br>
     * The {@link UIComponent} gets translated to the correct position!
     */
    public abstract void renderComponent(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial);

    @Override
    public void render(@NotNull GuiGraphics graphics, int ignoredMouseX, int ignoredMouseY, float partial) {

        if (!this.isVisible()) return;

        this.hovered = this.isMouseOver();

        graphics.pose().pushMatrix();
        graphics.pose().scale(this.getFixedComponentScale(), this.getFixedComponentScale());
        graphics.pose().translate(this.getTranslatedX(), this.getTranslatedY());

        this.renderComponent(graphics, this.getRealMouseX(), this.getRealMouseY(), partial);

        graphics.pose().popMatrix();

    }

    /**
     * This is always 0, since {@link UIComponent}s get translated to their "final" positions on render.
     */
    protected float getRealX() {
        return 0;
    }

    /**
     * This is always 0, since {@link UIComponent}s get translated to their "final" positions on render.
     */
    protected float getRealY() {
        return 0;
    }

    /**
     * This is the X-position the {@link UIComponent} gets translated to on render.<br>
     * Keep in mind that this is NOT the REAL position.
     */
    public abstract float getTranslatedX();

    /**
     * This is the Y-position the {@link UIComponent} gets translated to on render.<br>
     * Keep in mind that this is NOT the REAL position.
     */
    public abstract float getTranslatedY();

    public abstract float getWidth();

    public abstract float getHeight();

    /**
     * Since {@link UIComponent} positions get translated and scaled on render, normal mouse positions wouldn't work, so you need to use these whenever
     * you do anything mouse-pos-related. They corrected to work in this custom-scaled and -translated render environment.
     */
    public double getRealMouseX() {
        return (this.mc.mouseHandler.xpos() - (this.getTranslatedX() * this.getComponentScale())) / this.getComponentScale();
    }

    /**
     * Since {@link UIComponent} positions get translated and scaled on render, normal mouse positions wouldn't work, so you need to use these whenever
     * you do anything mouse-pos-related. They corrected to work in this custom-scaled and -translated render environment.
     */
    public double getRealMouseY() {
        return (this.mc.mouseHandler.ypos() - (this.getTranslatedY() * this.getComponentScale())) / this.getComponentScale();
    }

    /**
     * The mouse position after the {@link UIComponent} got translated on render.
     */
    public double getTranslatedMouseX() {
        return this.mc.mouseHandler.xpos() / this.getComponentScale();
    }

    /**
     * The mouse position after the {@link UIComponent} got translated on render.
     */
    public double getTranslatedMouseY() {
        return this.mc.mouseHandler.ypos() / this.getComponentScale();
    }

    protected float getScreenWidth() {
        return this.mc.getWindow().getWidth() / this.getComponentScale();
    }

    protected float getScreenHeight() {
        return this.mc.getWindow().getHeight() / this.getComponentScale();
    }

    /**
     * Checks if a component area is hovered.
     */
    protected boolean isComponentAreaHovered(float x, float y, float width, float height, boolean isRealPosition) {
        double mX = this.getRealMouseX();
        double mY = this.getRealMouseY();
        if (!isRealPosition) {
            x -= this.getTranslatedX();
            y -= this.getTranslatedY();
        }
        return isXYInArea(mX, mY, x, y, width+1, height+1);
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
     * Scissor with automatic scale handling.
     */
    protected void enableComponentScissor(GuiGraphics graphics, int x, int y, int width, int height, boolean isRealPosition) {
        if (isRealPosition) {
            x += this.getTranslatedX();
            y += this.getTranslatedY();
        }
        int scissorX = (int) (x * this.getFixedComponentScale());
        int scissorY = (int) (y * this.getFixedComponentScale());
        int scissorWidth = (int) (width * this.getFixedComponentScale());
        int scissorHeight = (int) (height * this.getFixedComponentScale());
        graphics.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
    }

    protected void disableComponentScissor(GuiGraphics graphics) {
        graphics.disableScissor();
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

    @Override
    public @NotNull List<GuiEventListener> children() {
        return this.children;
    }

    protected boolean mouseClickedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {
        for(GuiEventListener child : this.children()) {
            if (child.mouseClicked(realMouseX, realMouseY, button)) {
                this.setFocused(child);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    @Deprecated
    @Override
    public boolean mouseClicked(double ignoredMouseX, double ignoredMouseY, int button) {
        return this.mouseClickedComponent(this.getRealMouseX(), this.getRealMouseY(), this.getTranslatedMouseX(), this.getTranslatedMouseY(), button);
    }

    protected boolean mouseReleasedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {
        this.setDragging(false);
        for(GuiEventListener child : this.children()) {
            if (child.mouseReleased(realMouseX, realMouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    @Override
    public boolean mouseReleased(double ignoredMouseX, double ignoredMouseY, int button) {
        return this.mouseReleasedComponent(this.getRealMouseX(), this.getRealMouseY(), this.getTranslatedMouseX(), this.getTranslatedMouseY(), button);
    }

    /**
     * Real mouse coordinates don't really support drag offset calculation, so you should use translated coordinates here.
     */
    protected boolean mouseDraggedComponent(double translatedMouseX, double translatedMouseY, int button, double d1, double d2) {
        if (this.isDragging() && (button == 0)) {
            for (GuiEventListener child : this.children()) {
                if (child.mouseDragged(this.getRealMouseX(), this.getRealMouseY(), button, d1, d2)) return true;
            }
        }
        return false;
    }

    @Deprecated
    @Override
    public boolean mouseDragged(double ignoredMouseX, double ignoredMouseY, int button, double d1, double d2) {
        return this.mouseDraggedComponent(this.getTranslatedMouseX(), this.getTranslatedMouseY(), button, d1, d2);
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    protected boolean mouseScrolledComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, double scrollDeltaX, double scrollDeltaY) {
        for(GuiEventListener child : this.children()) {
            if (child.mouseScrolled(realMouseX, realMouseY, scrollDeltaX, scrollDeltaY)) return true;
        }
        return false;
    }

    @Deprecated
    @Override
    public boolean mouseScrolled(double ignoredMouseX, double ignoredMouseY, double scrollDeltaX, double scrollDeltaY) {
        return this.mouseScrolledComponent(this.getRealMouseX(), this.getRealMouseY(), this.getTranslatedMouseX(), this.getTranslatedMouseY(), scrollDeltaX, scrollDeltaY);
    }

    protected void mouseMovedComponent(double realMouseX, double realMouseY) {
    }

    @Deprecated
    @Override
    public void mouseMoved(double ignoredMouseX, double ignoredMouseY) {
        this.mouseMovedComponent(this.getRealMouseX(), this.getRealMouseY());
    }

    public boolean isMouseOver() {
        if (!this.isVisible()) return false;
        return this.isComponentAreaHovered(this.getTranslatedX(), this.getTranslatedY(), this.getWidth(), this.getHeight(), false);
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