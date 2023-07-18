package de.keksuccino.fancymenu.util.rendering.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class UIComponent extends UIBase implements GuiEventListener, Renderable, NarratableEntry {

    /** The scale of the {@link UIComponent}. Not fixed. */
    @NotNull
    protected Supplier<Float> componentScaleSupplier = UIBase::getUIScale;
    protected Minecraft mc = Minecraft.getInstance();
    public float z = 0f;
    protected boolean hovered = false;
    protected boolean visible = true;
    protected UIComponentPositioner positioner = new UIComponentPositioner();

    public abstract void renderComponent(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height);

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.isVisible()) return;

        this.hovered = this.isMouseOver(0,0);

        pose.pushPose();
        pose.scale(this.getFixedComponentScale(), this.getFixedComponentScale(), this.getFixedComponentScale());
        pose.translate(this.getComponentX(this.positioner), this.getComponentY(this.positioner), this.z);
        this.renderComponent(pose, this.getComponentScaledMouseX(), this.getComponentScaledMouseY(), partial, 0f, 0f, this.getComponentWidth(), this.getComponentHeight());
        pose.popPose();

    }

//    /** This is the final/real X-position of the component. Should be used when getting the component's position outside the component class. **/
//    public float getX() {
//        return this.getComponentX(this.positioner) * (float)this.getWindowScale();
////        return this.getComponentX(this.positioner) / (float)this.getWindowScale();
//    }
//
//    /** This is the final/real Y-position of the component. Should be used when getting the component's position outside the component class. **/
//    public float getY() {
//        return this.getComponentY(this.positioner) * (float)this.getWindowScale();
//    }
//
//    /** This is the final/real width of the component. Should be used when getting the component's size outside the component class. **/
//    public float getWidth() {
//        return this.getComponentScaledWidth();
//    }
//
//    /** This is the final/real height of the component. Should be used when getting the component's size outside the component class. **/
//    public float getHeight() {
//        return this.getComponentScaledHeight();
//    }

    public boolean isHovered() {
        if (!this.isVisible()) return false;
        return this.hovered;
    }

    public boolean isVisible() {
        return this.visible;
    }

    /**
     * This is the render X-position of the component.<br>
     * Here you "build" the X-position of the component.<br><br>
     *
     * The goal is to prepare everything in advance, so you don't need to take care of scaling here, as long as you only use the given {@link UIComponentPositioner}.<br>
     * <br>
     */
    protected abstract float getComponentX(@NotNull UIComponentPositioner positioner);

    /**
     * This is the render Y-position of the component.<br>
     * Here you "build" the X-position of the component.<br><br>
     *
     * The goal is to prepare everything in advance, so you don't need to take care of scaling here, as long as you only use the given {@link UIComponentPositioner}.<br>
     * <br>
     */
    protected abstract float getComponentY(@NotNull UIComponentPositioner positioner);

    /** This is the unscaled width of the component. Not the real width. **/
    public abstract float getComponentWidth();

    /** This is the unscaled height of the component. Not the real height. **/
    public abstract float getComponentHeight();

    public float getComponentScaledWidth() {
        return this.calcComponentScaledDimension(this.getComponentWidth());
    }

    public float getComponentScaledHeight() {
        return this.calcComponentScaledDimension(this.getComponentHeight());
    }

    public double getComponentScaledMouseX() {
        return this.mc.mouseHandler.xpos() / this.getComponentScale();
    }

    public double getComponentScaledMouseY() {
        return this.mc.mouseHandler.ypos() / this.getComponentScale();
    }

    public double getWindowScaledMouseX() {
        return this.mc.mouseHandler.xpos() / this.getWindowScale();
    }

    public double getWindowScaledMouseY() {
        return this.mc.mouseHandler.ypos() / this.getWindowScale();
    }

    public float getComponentScaledScreenWith() {
        int width = ScreenUtils.getScreenWidth();
        if (width == 0) return 0;
        return this.calcComponentScaledDimension(width);
    }

    public float getComponentScaledScreenHeight() {
        int height = ScreenUtils.getScreenHeight();
        if (height == 0) return 0;
        return this.calcComponentScaledDimension(height);
    }

    protected float calcComponentScaledDimension(float unscaledWidthHeightXY) {
        return unscaledWidthHeightXY / this.getFixedComponentScale();
    }

    protected boolean isComponentScaledAreaHovered(float unscaledX, float unscaledY, float unscaledWidth, float unscaledHeight) {
        return isXYInArea(this.getComponentScaledMouseX(), this.getComponentScaledMouseY(), unscaledX, unscaledY, unscaledWidth+1, unscaledHeight+1);
    }

    public double getWindowScale() {
        return this.mc.getWindow().getGuiScale();
    }

    public float getComponentScale() {
        return this.componentScaleSupplier.get();
    }

    public float getFixedComponentScale() {
        return calculateFixedScale(this.getComponentScale());
    }

    protected abstract boolean mouseClickedComponent(double mouseX, double mouseY, int button);

    @Override
    public boolean mouseClicked(double ignoredMouseX, double ignoredMouseY, int button) {
        return this.mouseClickedComponent(this.getComponentScaledMouseX(), this.getComponentScaledMouseY(), button);
    }

    protected abstract boolean mouseReleasedComponent(double mouseX, double mouseY, int button);

    @Override
    public boolean mouseReleased(double ignoredMouseX, double ignoredMouseY, int button) {
        return this.mouseReleasedComponent(this.getComponentScaledMouseX(), this.getComponentScaledMouseY(), button);
    }

    protected abstract boolean mouseDraggedComponent(double mouseX, double mouseY, int button, double d1, double d2);

    @Override
    public boolean mouseDragged(double ignoredMouseX, double ignoredMouseY, int button, double d1, double d2) {
        return this.mouseDraggedComponent(this.getComponentScaledMouseX(), this.getComponentScaledMouseY(), button, d1, d2);
    }

    protected abstract boolean mouseScrolledComponent(double mouseX, double mouseY, double scrollDelta);

    @Override
    public boolean mouseScrolled(double ignoredMouseX, double ignoredMouseY, double scrollDelta) {
        return this.mouseScrolledComponent(this.getComponentScaledMouseX(), this.getComponentScaledMouseY(), scrollDelta);
    }

    protected abstract void mouseMovedComponent(double mouseX, double mouseY);

    @Override
    public void mouseMoved(double ignoredMouseX, double ignoredMouseY) {
        this.mouseMovedComponent(this.getComponentScaledMouseX(), this.getComponentScaledMouseY());
    }

    @Override
    public boolean isMouseOver(double ignoredMouseX, double ignoredMouseY) {
        return this.isComponentScaledAreaHovered(this.getComponentX(this.positioner), this.getComponentY(this.positioner), this.getComponentWidth(), this.getComponentHeight());
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

    public class UIComponentPositioner {

        private final UIComponent comp = UIComponent.this;

        protected float getScreenWidth() {
            return comp.getComponentScaledScreenWith();
        }

        protected float getScreenHeight() {
            return comp.getComponentScaledScreenHeight();
        }

        protected float getComponentWidth() {
            return comp.getComponentWidth();
        }

        protected float getComponentHeight() {
            return comp.getComponentHeight();
        }

    }

}
