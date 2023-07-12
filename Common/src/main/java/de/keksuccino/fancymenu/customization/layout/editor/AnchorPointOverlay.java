package de.keksuccino.fancymenu.customization.layout.editor;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnchorPointOverlay extends GuiComponent implements Renderable, GuiEventListener {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final int MOUSE_OVER_DELAY_MS = 1000;
    protected static final int AREA_ALPHA_NORMAL = 40;
    protected static final int AREA_ALPHA_BORDER = 70;
    protected static final float AREA_ALPHA_MULTIPLIER = 2.0F;

    protected final LayoutEditorScreen editor;
    protected List<AbstractEditorElement> elements = new ArrayList<>();
    protected boolean leftMouseDown = false;
    protected AnchorPointArea initialHoverArea = null;
    protected AnchorPointArea lastTickMouseOverArea = null;
    protected AnchorPointArea currentMouseOverArea = null;
    protected long areaMouseOverStartTime = -1;
    protected boolean mouseDragged = false;

    protected AnchorPointArea topLeftArea = new AnchorPointArea(ElementAnchorPoints.TOP_LEFT, 30, 30, AnchorPointArea.ProgressDirection.TO_RIGHT);
    protected AnchorPointArea midLeftArea = new AnchorPointArea(ElementAnchorPoints.MID_LEFT, 30, 30, AnchorPointArea.ProgressDirection.TO_RIGHT);
    protected AnchorPointArea bottomLeftArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_LEFT, 30, 30, AnchorPointArea.ProgressDirection.TO_RIGHT);
    protected AnchorPointArea topCenteredArea = new AnchorPointArea(ElementAnchorPoints.TOP_CENTERED, 30, 30, AnchorPointArea.ProgressDirection.TO_DOWN);
    protected AnchorPointArea midCenteredArea = new AnchorPointArea(ElementAnchorPoints.MID_CENTERED, 30, 30, AnchorPointArea.ProgressDirection.TO_TOP);
    protected AnchorPointArea bottomCenteredArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_CENTERED, 30, 30, AnchorPointArea.ProgressDirection.TO_TOP);
    protected AnchorPointArea topRightArea = new AnchorPointArea(ElementAnchorPoints.TOP_RIGHT, 30, 30, AnchorPointArea.ProgressDirection.TO_LEFT);
    protected AnchorPointArea midRightArea = new AnchorPointArea(ElementAnchorPoints.MID_RIGHT, 30, 30, AnchorPointArea.ProgressDirection.TO_LEFT);
    protected AnchorPointArea bottomRightArea = new AnchorPointArea(ElementAnchorPoints.BOTTOM_RIGHT, 30, 30, AnchorPointArea.ProgressDirection.TO_LEFT);
    protected AnchorPointArea[] anchorPointAreas = new AnchorPointArea[] { topLeftArea, midLeftArea, bottomLeftArea, topCenteredArea, midCenteredArea, bottomCenteredArea, topRightArea, midRightArea, bottomRightArea };

    public AnchorPointOverlay(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!FancyMenu.getOptions().showAnchorOverlay.getValue()) {
            this.leftMouseDown = false;
            this.initialHoverArea = null;
            this.currentMouseOverArea = null;
            this.lastTickMouseOverArea = null;
            this.areaMouseOverStartTime = -1;
            this.mouseDragged = false;
            return;
        }

        this.updateCachedElements();

        if (!FancyMenu.getOptions().alwaysShowAnchorOverlay.getValue() && !this.isElementPressed()) return;

        if ((this.initialHoverArea != null) && (!this.initialHoverArea.isMouseOver(mouseX, mouseY))) {
            this.initialHoverArea = null;
        }
        if (this.initialHoverArea instanceof ElementAnchorPointArea ea) {
            AbstractEditorElement element = ea.getElement();
            if ((element == null) || element.isSelected() || element.isMultiSelected()) this.initialHoverArea = null;
        }
        if (!this.leftMouseDown) {
            this.initialHoverArea = null;
        }

        this.handleAreaMouseOver(mouseX, mouseY);

        int menuBarHeight = ((this.editor.menuBar != null) ? (int)((float)this.editor.menuBar.getHeight() * UIBase.calculateFixedScale(this.editor.menuBar.getScale())) : 0);

        this.topLeftArea.x = -1;
        this.topLeftArea.y = -1 + menuBarHeight;
        this.topLeftArea.render(pose, mouseX, mouseY, partial);

        this.midLeftArea.x = -1;
        this.midLeftArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midLeftArea.getHeight() / 2);
        this.midLeftArea.render(pose, mouseX, mouseY, partial);

        this.bottomLeftArea.x = -1;
        this.bottomLeftArea.y = ScreenUtils.getScreenHeight() - this.bottomLeftArea.getHeight() + 1;
        this.bottomLeftArea.render(pose, mouseX, mouseY, partial);

        this.topCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.topCenteredArea.getWidth() / 2);
        this.topCenteredArea.y = -1 + menuBarHeight;
        this.topCenteredArea.render(pose, mouseX, mouseY, partial);

        this.midCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.midCenteredArea.getWidth() / 2);
        this.midCenteredArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midCenteredArea.getHeight() / 2);
        this.midCenteredArea.render(pose, mouseX, mouseY, partial);

        this.bottomCenteredArea.x = (ScreenUtils.getScreenWidth() / 2) - (this.bottomCenteredArea.getWidth() / 2);
        this.bottomCenteredArea.y = ScreenUtils.getScreenHeight() - this.bottomCenteredArea.getHeight() + 1;
        this.bottomCenteredArea.render(pose, mouseX, mouseY, partial);

        this.topRightArea.x = ScreenUtils.getScreenWidth() - this.topRightArea.getWidth() + 1;
        this.topRightArea.y = -1 + menuBarHeight;
        this.topRightArea.render(pose, mouseX, mouseY, partial);

        this.midRightArea.x = ScreenUtils.getScreenWidth() - this.midRightArea.getWidth() + 1;
        this.midRightArea.y = (ScreenUtils.getScreenHeight() / 2) - (this.midRightArea.getHeight() / 2);
        this.midRightArea.render(pose, mouseX, mouseY, partial);

        this.bottomRightArea.x = ScreenUtils.getScreenWidth() - this.bottomRightArea.getWidth() + 1;
        this.bottomRightArea.y = ScreenUtils.getScreenHeight() - this.bottomRightArea.getHeight() + 1;
        this.bottomRightArea.render(pose, mouseX, mouseY, partial);

        if ((this.currentMouseOverArea != null) && FancyMenu.getOptions().changeAnchorOnHover.getValue()) {
            this.currentMouseOverArea.renderMouseOverProgress(pose, this.calculateMouseOverProgress());
        }

        this.renderConnectionLines(pose);

    }

    protected void renderConnectionLines(PoseStack pose) {
        if (FancyMenu.getOptions().showAllAnchorConnections.getValue()) {
            for (AbstractEditorElement e : this.elements) {
                this.renderConnectionLineFor(pose, e);
            }
        } else if (this.leftMouseDown) {
            for (AbstractEditorElement e : this.elements) {
                if (e.isSelected() || e.isMultiSelected() || this.isParentSelected(e)) {
                    this.renderConnectionLineFor(pose, e);
                }
            }
        }
    }

    protected void renderConnectionLineFor(PoseStack pose, AbstractEditorElement e) {
        AnchorPointArea a = this.getAreaForElement(e);
        if (a != null) {
            int xElement = e.getX() + (e.getWidth() / 2);
            int yElement = e.getY() + (e.getHeight() / 2);
            int xArea = a.getX() + (a.getWidth() / 2);
            int yArea = a.getY() + (a.getHeight() / 2);
            this.renderSquareLine(pose, xElement, yElement, xArea, yArea, 2, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_BORDER * a.getAlphaMultiplier())));
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

    protected void updateCachedElements() {
        this.elements = this.editor.getAllElements();
    }

    protected void handleAreaMouseOver(int mouseX, int mouseY) {
        if (this.leftMouseDown && (this.initialHoverArea == null) && this.mouseDragged) {
            this.currentMouseOverArea = this.getMouseOverArea(mouseX, mouseY);
            if (!FancyMenu.getOptions().changeAnchorOnHover.getValue()) this.currentMouseOverArea = null;
            if (this.isElementGettingResized()) this.currentMouseOverArea = null;
            if ((this.lastTickMouseOverArea instanceof ElementAnchorPointArea a1) && (this.currentMouseOverArea instanceof ElementAnchorPointArea a2)) {
                if (a1.elementIdentifier.equals(a2.elementIdentifier)) this.currentMouseOverArea = this.lastTickMouseOverArea;
            }
            if ((this.currentMouseOverArea != null) && (this.currentMouseOverArea != this.lastTickMouseOverArea)) {
                this.areaMouseOverStartTime = System.currentTimeMillis();
            }
            this.lastTickMouseOverArea = this.currentMouseOverArea;
            if (this.currentMouseOverArea != null) {
                if ((this.areaMouseOverStartTime + MOUSE_OVER_DELAY_MS) <= System.currentTimeMillis()) {
                    for (AbstractEditorElement e : this.elements) {
                        if (e.isDragged() && !this.isAlreadyAttachedToAnchor(e, this.currentMouseOverArea)) {
                            e.setAnchorPointViaOverlay(this.currentMouseOverArea, mouseX, mouseY);
                        }
                    }
                }
            }
        } else {
            this.currentMouseOverArea = null;
            this.lastTickMouseOverArea = null;
            this.areaMouseOverStartTime = -1;
        }
    }

    protected boolean isAlreadyAttachedToAnchor(@NotNull AbstractEditorElement element, @NotNull AnchorPointArea area) {
        if ((element.element.anchorPoint == ElementAnchorPoints.ELEMENT) && (area instanceof ElementAnchorPointArea ae)) {
            AbstractEditorElement element1 = ae.getElement();
            if (element1 != null) return Objects.equals(element.element.anchorPointElementIdentifier, element1.element.getInstanceIdentifier());
        }
        return element.element.anchorPoint == area.anchorPoint;
    }

    protected float calculateMouseOverProgress() {
        if (this.currentMouseOverArea != null) {
            long now = System.currentTimeMillis();
            if ((this.areaMouseOverStartTime + MOUSE_OVER_DELAY_MS) > now) {
                long diff = (this.areaMouseOverStartTime + MOUSE_OVER_DELAY_MS) - now;
                float f = Math.max(0.0F, Math.min(1.0F, Math.max(1F, (float)diff) / (float)MOUSE_OVER_DELAY_MS));
                return 1.0F - f;
            }
            return 1.0F;
        }
        return 0.0F;
    }

    protected boolean isElementPressed() {
        for (AbstractEditorElement e : this.elements) {
            if (e.isPressed() && !e.isGettingResized()) return true;
        }
        return false;
    }

    protected boolean isElementGettingResized() {
        for (AbstractEditorElement e : this.elements) {
            if (e.isGettingResized()) return true;
        }
        return false;
    }

    protected boolean isSelectedElementParentOf(@NotNull AbstractEditorElement element) {
        if (element.element.anchorPoint != ElementAnchorPoints.ELEMENT) return false;
        if (element.element.anchorPointElementIdentifier != null) {
            AbstractEditorElement parent = this.editor.getElementByInstanceIdentifier(element.element.anchorPointElementIdentifier);
            if (parent != null) {
                for (AbstractEditorElement e : this.elements) {
                    if ((e.isSelected() || e.isMultiSelected()) && (e != element)) {
                        if (e == parent) return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean isParentSelected(AbstractEditorElement e) {
        if (e.element.anchorPoint != ElementAnchorPoints.ELEMENT) return false;
        if (e.element.anchorPointElementIdentifier == null) return false;
        AbstractEditorElement parent = this.editor.getElementByInstanceIdentifier(e.element.anchorPointElementIdentifier);
        if (parent != null) {
            if (parent.isSelected() || parent.isMultiSelected()) return true;
            return this.isParentSelected(parent);
        }
        return false;
    }

    @Nullable
    protected AbstractEditorElement getTopMouseOverAnchorElement(int mouseX, int mouseY) {
        for (AbstractEditorElement e : Lists.reverse(new ArrayList<>(this.elements))) {
            if (e.isMouseOver(mouseX, mouseY) && !e.isSelected() && !e.isMultiSelected() && !this.isSelectedElementParentOf(e)) return e;
        }
        return null;
    }

    @Nullable
    protected AbstractEditorElement getTopMouseOverElement(int mouseX, int mouseY) {
        for (AbstractEditorElement e : Lists.reverse(new ArrayList<>(this.elements))) {
            if (e.isMouseOver(mouseX, mouseY)) return e;
        }
        return null;
    }

    @Nullable
    protected AnchorPointArea getAreaForElement(@NotNull AbstractEditorElement element) {
        if (element.element.anchorPoint == ElementAnchorPoints.ELEMENT) {
            if (element.element.anchorPointElementIdentifier != null) {
                AbstractEditorElement e = this.editor.getElementByInstanceIdentifier(element.element.anchorPointElementIdentifier);
                if (e != null) return new ElementAnchorPointArea(e.element.getInstanceIdentifier());
            }
            return null;
        }
        for (AnchorPointArea a : this.anchorPointAreas) {
            if (a.anchorPoint == element.element.anchorPoint) return a;
        }
        return null;
    }

    @Nullable
    protected AnchorPointArea getMouseOverArea(int mouseX, int mouseY) {
        for (AnchorPointArea a : this.anchorPointAreas) {
            if (a.isMouseOver(mouseX, mouseY)) return a;
        }
        AbstractEditorElement e = this.getTopMouseOverAnchorElement(mouseX, mouseY);
        if (e != null) return new ElementAnchorPointArea(e.element.getInstanceIdentifier());
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (!FancyMenu.getOptions().showAnchorOverlay.getValue()) return false;

        this.updateCachedElements();

        this.leftMouseDown = (this.getTopMouseOverElement((int) mouseX, (int) mouseY) != null);
        if (this.leftMouseDown) this.initialHoverArea = this.getMouseOverArea((int) mouseX, (int) mouseY);

        return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);

    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        if (!FancyMenu.getOptions().showAnchorOverlay.getValue()) return false;

        this.updateCachedElements();

        this.leftMouseDown = false;
        this.initialHoverArea = null;
        this.currentMouseOverArea = null;
        this.lastTickMouseOverArea = null;
        this.areaMouseOverStartTime = -1;
        this.mouseDragged = false;

        return GuiEventListener.super.mouseReleased(mouseX, mouseY, button);

    }

    @Override
    public boolean mouseDragged(double $$0, double $$1, int $$2, double $$3, double $$4) {

        if (!FancyMenu.getOptions().showAnchorOverlay.getValue()) return false;

        this.mouseDragged = true;

        return GuiEventListener.super.mouseDragged($$0, $$1, $$2, $$3, $$4);

    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    public class ElementAnchorPointArea extends AnchorPointArea {

        @NotNull
        public final String elementIdentifier;

        private ElementAnchorPointArea(@NotNull String elementIdentifier) {
            super(ElementAnchorPoints.ELEMENT, 0, 0, ProgressDirection.TO_TOP);
            this.elementIdentifier = elementIdentifier;
        }

        @Override
        public String toString() {
            return "[element_anchor_point_area;id=" + this.elementIdentifier + ";x=" + this.getX() + ";y=" + this.getY() + ";w=" + this.getWidth() + ";h=" + this.getHeight() + "]";
        }

        @Nullable
        public AbstractEditorElement getElement() {
            return AnchorPointOverlay.this.editor.getElementByInstanceIdentifier(this.elementIdentifier);
        }

        @Override
        protected int getX() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getX() : 20;
        }

        @Override
        protected int getY() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getY() : 20;
        }

        @Override
        protected int getWidth() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getWidth() : 20;
        }

        @Override
        protected int getHeight() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getHeight() : 20;
        }

        @SuppressWarnings("all")
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.isMouseOver(mouseX, mouseY) : false;
        }

    }

    public class AnchorPointArea extends GuiComponent implements Renderable, GuiEventListener {

        public final ElementAnchorPoint anchorPoint;
        private int x;
        private int y;
        private final int width;
        private final int height;
        private final ProgressDirection direction;

        private AnchorPointArea(@NotNull ElementAnchorPoint anchorPoint, int width, int height, @NotNull ProgressDirection direction) {
            this.width = width;
            this.height = height;
            this.direction = direction;
            this.anchorPoint = anchorPoint;
        }

        @Override
        public String toString() {
            return "[anchor_point_area;x=" + this.getX() + ";y=" + this.getY() + ";w=" + this.getWidth() + ";h=" + this.getHeight() + "]";
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            int endX = this.getX() + this.getWidth();
            int endY = this.getY() + this.getHeight();
            fill(pose, this.getX(), this.getY(), endX, endY, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_NORMAL * this.getAlphaMultiplier())));
            UIBase.renderBorder(pose, this.getX(), this.getY(), endX, endY, 1, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_BORDER * this.getAlphaMultiplier())), true, true, true, true);
            UIBase.resetShaderColor();
        }

        protected void renderMouseOverProgress(@NotNull PoseStack pose, float progress) {
            int progressWidth = (int) ((float)this.getWidth() * progress);
            int progressHeight = (int) ((float)this.getHeight() * progress);
            int startX = this.getX();
            int startY = this.getY();
            int endX = this.getX() + progressWidth;
            int endY = this.getY() + this.getHeight();
            if (this.direction == ProgressDirection.TO_LEFT) {
                endX = this.getX() + this.getWidth();
                startX = endX - progressWidth;
            } else if (this.direction == ProgressDirection.TO_DOWN) {
                endX = this.getX() + this.getWidth();
                endY = this.getY() + progressHeight;
            } else if (this.direction == ProgressDirection.TO_TOP) {
                endX = this.getX() + this.getWidth();
                startY = endY - progressHeight;
            }
            fill(pose, startX, startY, endX, endY, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_NORMAL * this.getAlphaMultiplier())));
            UIBase.resetShaderColor();
        }

        protected int getWidth() {
            return this.width;
        }

        protected int getHeight() {
            return this.height;
        }

        protected int getX() {
            return this.x;
        }

        protected int getY() {
            return this.y;
        }

        protected float getAlphaMultiplier() {
            if (AnchorPointOverlay.this.leftMouseDown && AnchorPointOverlay.this.isElementPressed()) {
                return AREA_ALPHA_MULTIPLIER;
            }
            return 1.0F;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return UIBase.isXYInArea((int) mouseX, (int) mouseY, this.getX(), this.getY(), this.getWidth(), this.getWidth());
        }

        @Override
        public void setFocused(boolean var1) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        protected enum ProgressDirection {
            TO_LEFT,
            TO_RIGHT,
            TO_TOP,
            TO_DOWN
        }

    }

}
