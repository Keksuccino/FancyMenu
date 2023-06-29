package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AnchorPointOverlay extends GuiComponent implements Renderable {

    protected final LayoutEditorScreen editor;
    protected List<AbstractEditorElement> selectedElements = new ArrayList<>();

    protected Supplier<DrawableColor> areaColorSupplier = () -> UIBase.getUIColorScheme().layout_editor_anchor_point_area_color;

    //TODO remove hover size (not used anymore)

    protected AnchorPointArea topLeftArea = new AnchorPointArea(ElementAnchorPoints.TOP_LEFT, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea midLeftArea = new AnchorPointArea(ElementAnchorPoints.MID_LEFT, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea bottomLeftArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_LEFT, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea topCenteredArea = new AnchorPointArea(ElementAnchorPoints.TOP_CENTERED, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea midCenteredArea = new AnchorPointArea(ElementAnchorPoints.MID_CENTERED, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea bottomCenteredArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_CENTERED, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea topRightArea = new AnchorPointArea(ElementAnchorPoints.TOP_RIGHT, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea midRightArea = new AnchorPointArea(ElementAnchorPoints.MID_RIGHT, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea bottomRightArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_RIGHT, 30, 30, 30, 30, areaColorSupplier);
    protected AnchorPointArea[] anchorPointAreas = new AnchorPointArea[] { topLeftArea, midLeftArea, bottomLeftArea, topCenteredArea, midCenteredArea, bottomCenteredArea, topRightArea, midRightArea, bottomRightArea };
    protected ElementAnchorPointArea elementArea = new ElementAnchorPointArea(areaColorSupplier);

    public AnchorPointOverlay(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        this.selectedElements = this.editor.getSelectedElements();

        if (this.selectedElements.isEmpty() || !this.selectedElements.get(0).isPressed()) return;

        this.topLeftArea.x = -1;
        this.topLeftArea.y = -1;
        this.topLeftArea.render(pose, mouseX, mouseY, partial);

        this.midLeftArea.x = -1;
        this.midLeftArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midLeftArea.getHeight() / 2);
        this.midLeftArea.render(pose, mouseX, mouseY, partial);

        this.bottomLeftArea.x = -1;
        this.bottomLeftArea.y = ScreenUtils.getScreenHeight() - this.bottomLeftArea.getHeight() + 1;
        this.bottomLeftArea.render(pose, mouseX, mouseY, partial);

        this.topCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.topCenteredArea.getWidth() / 2);
        this.topCenteredArea.y = -1;
        this.topCenteredArea.render(pose, mouseX, mouseY, partial);

        this.midCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.midCenteredArea.getWidth() / 2);
        this.midCenteredArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midCenteredArea.getHeight() / 2);
        this.midCenteredArea.render(pose, mouseX, mouseY, partial);

        this.bottomCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.bottomCenteredArea.getWidth() / 2);
        this.bottomCenteredArea.y = ScreenUtils.getScreenHeight() - this.bottomCenteredArea.getHeight() + 1;
        this.bottomCenteredArea.render(pose, mouseX, mouseY, partial);

        this.topRightArea.x = ScreenUtils.getScreenWidth() - this.topRightArea.getWidth() + 1;
        this.topRightArea.y = -1;
        this.topRightArea.render(pose, mouseX, mouseY, partial);

        this.midRightArea.x = ScreenUtils.getScreenWidth() - this.midRightArea.getWidth() + 1;
        this.midRightArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midRightArea.getHeight() / 2);
        this.midRightArea.render(pose, mouseX, mouseY, partial);

        this.bottomRightArea.x = ScreenUtils.getScreenWidth() - this.bottomRightArea.getWidth() + 1;
        this.bottomRightArea.y = ScreenUtils.getScreenHeight() - this.bottomRightArea.getHeight() + 1;
        this.bottomRightArea.render(pose, mouseX, mouseY, partial);

        this.renderConnectionLines(pose);

    }

    protected void renderConnectionLines(PoseStack pose) {
        for (AbstractEditorElement e : this.selectedElements) {
            if (e.isPressed()) {
                AnchorPointArea a = this.getAreaForElement(e);
                if (a != null) {
                    int xElement = e.getX() + (e.getWidth() / 2);
                    int yElement = e.getY() + (e.getHeight() / 2);
                    int xArea = a.x + (a.getWidth() / 2);
                    int yArea = a.y + (a.getHeight() / 2);
                    this.renderSquareLine(pose, xElement, yElement, xArea, yArea, 2, a.color.get().getColorInt());
                }
            } else {
                break;
            }
        }
    }

    @SuppressWarnings("all")
    protected void renderSquareLine(PoseStack pose, int xElement, int yElement, int xArea, int yArea, int lineThickness, int color) {

        int horizontalWidth = Math.max(xElement, xArea) - Math.min(xElement, xArea);
        int verticalHeight = Math.max(yElement, yArea) - Math.min(yElement, yArea);
        int horizontalX = Math.min(xElement, xArea);
        int horizontalY = yArea;
        int verticalX = xElement;
        int verticalY = Math.min(yElement, yArea);
        if (xArea < xElement) {
            horizontalX += lineThickness;
        }

        RenderSystem.enableBlend();
        UIBase.resetShaderColor();
        //Horizontal Line
        fill(pose, horizontalX, horizontalY, horizontalX + horizontalWidth, horizontalY + lineThickness, color);
        //Vertical Line
        fill(pose, verticalX, verticalY, verticalX + lineThickness, verticalY + verticalHeight, color);
        UIBase.resetShaderColor();

    }

    @Nullable
    protected AnchorPointArea getAreaForElement(@NotNull AbstractEditorElement element) {
        if (element.element.anchorPoint == ElementAnchorPoints.ELEMENT) {
            if (element.element.getElementAnchorPointElement() != null) {
                this.elementArea.applyElement(element.element.getElementAnchorPointElement());
                return this.elementArea;
            } else {
                return null;
            }
        }
        for (AnchorPointArea a : this.anchorPointAreas) {
            if (a.anchorPoint == element.element.anchorPoint) return a;
        }
        return null;
    }

    protected class ElementAnchorPointArea extends AnchorPointArea {

        protected ElementAnchorPointArea(@NotNull Supplier<DrawableColor> color) {
            super(ElementAnchorPoints.ELEMENT, 0, 0, 0, 0, color);
        }

        protected ElementAnchorPointArea applyElement(AbstractElement e) {
            this.x = e.getX();
            this.y = e.getY();
            this.width = e.getWidth();
            this.height = e.getHeight();
            this.hoverWidth = e.getWidth();
            this.hoverHeight = e.getHeight();
            return this;
        }

    }

    protected class AnchorPointArea extends GuiComponent implements Renderable {

        protected ElementAnchorPoint anchorPoint;
        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected int hoverWidth;
        protected int hoverHeight;
        protected boolean hovered = false;
        protected Supplier<DrawableColor> color;

        protected AnchorPointArea(@NotNull ElementAnchorPoint anchorPoint, int width, int height, int hoverWidth, int hoverHeight, @NotNull Supplier<DrawableColor> color) {
            this.width = width;
            this.height = height;
            this.hoverWidth = hoverWidth;
            this.hoverHeight = hoverHeight;
            this.color = color;
            this.anchorPoint = anchorPoint;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.hovered = UIBase.isXYInArea(mouseX, mouseY, this.x, this.y, this.getWidth(), this.getWidth());
            this.handleHover(mouseX, mouseY);
            int endX = this.x + this.getWidth();
            int endY = this.y + this.getHeight();
            fill(pose, this.x, this.y, endX, endY, RenderUtils.replaceAlphaInColor(this.color.get().getColorInt(), 70));
            UIBase.renderBorder(pose, this.x, this.y, endX, endY, 1, this.color.get(), true, true, true, true);
            UIBase.resetShaderColor();
        }

        protected void handleHover(int mouseX, int mouseY) {
            if (this.hovered) {
                for (AbstractEditorElement e : AnchorPointOverlay.this.selectedElements) {
                    if (e.element.anchorPoint != this.anchorPoint) {
                        e.setAnchorPointViaOverlay(this.anchorPoint, mouseX, mouseY);
                    }
                }
            }
        }

        protected int getWidth() {
            return this.hovered ? this.hoverWidth : this.width;
        }

        protected int getHeight() {
            return this.hovered ? this.hoverHeight : this.height;
        }

    }

}
