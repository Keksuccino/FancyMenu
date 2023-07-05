package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnchorPointOverlay extends GuiComponent implements Renderable, GuiEventListener {

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

        this.elements = this.editor.getAllElements();

        if ((this.initialHoverArea != null) && (!this.initialHoverArea.isMouseOver(mouseX, mouseY))) {
            this.initialHoverArea = null;
        }
        if (!this.leftMouseDown) {
            this.initialHoverArea = null;
        }

        this.handleAreaMouseOver(mouseX, mouseY);

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

        if (this.currentMouseOverArea != null) {
            this.currentMouseOverArea.renderMouseOverProgress(pose, this.calculateMouseOverProgress());
        }

        this.renderConnectionLines(pose);

    }

    protected void renderConnectionLines(PoseStack pose) {
        for (AbstractEditorElement e : this.elements) {
            AnchorPointArea a = this.getAreaForElement(e);
            if (a != null) {
                int xElement = e.getX() + (e.getWidth() / 2);
                int yElement = e.getY() + (e.getHeight() / 2);
                int xArea = a.x + (a.getWidth() / 2);
                int yArea = a.y + (a.getHeight() / 2);
                this.renderSquareLine(pose, xElement, yElement, xArea, yArea, 2, RenderUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_BORDER * a.getAlphaMultiplier())));
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

    protected void handleAreaMouseOver(int mouseX, int mouseY) {
        if (this.leftMouseDown && (this.initialHoverArea == null) && this.isElementDragged()) {
            this.currentMouseOverArea = this.getMouseOverArea(mouseX, mouseY);
            if ((this.lastTickMouseOverArea instanceof ElementAnchorPointArea a1) && (this.currentMouseOverArea instanceof ElementAnchorPointArea a2)) {
                if (a1.element == a2.element) this.currentMouseOverArea = this.lastTickMouseOverArea;
            }
            if ((this.currentMouseOverArea != null) && (this.currentMouseOverArea != this.lastTickMouseOverArea)) {
                this.areaMouseOverStartTime = System.currentTimeMillis();
            }
            this.lastTickMouseOverArea = this.currentMouseOverArea;
            if (this.currentMouseOverArea != null) {
                if ((this.areaMouseOverStartTime + MOUSE_OVER_DELAY_MS) <= System.currentTimeMillis()) {
                    for (AbstractEditorElement e : AnchorPointOverlay.this.elements) {
                        if (e.isDragged() && !this.isAlreadyAttachedToAnchor(e, this.currentMouseOverArea)) {
                            e.setAnchorPointViaOverlay(this.currentMouseOverArea, mouseX, mouseY);
                        }
                    }
                }
            }
        } else {
            this.currentMouseOverArea = null;
            this.areaMouseOverStartTime = -1;
        }
    }

    protected boolean isAlreadyAttachedToAnchor(@NotNull AbstractEditorElement element, @NotNull AnchorPointArea area) {
        if ((element.element.anchorPoint == ElementAnchorPoints.ELEMENT) && (area instanceof ElementAnchorPointArea ae)) {
            return Objects.equals(element.element.anchorPointElementIdentifier, ae.element.element.getInstanceIdentifier());
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

    protected boolean isElementDragged() {
        for (AbstractEditorElement e : this.elements) {
            if (e.isDragged()) return true;
        }
        return false;
    }

    protected boolean isElementPressed() {
        for (AbstractEditorElement e : this.elements) {
            if (e.isPressed()) return true;
        }
        return false;
    }

    @Nullable
    protected AbstractEditorElement getHoveredAnchorElement() {
        for (AbstractEditorElement e : this.elements) {
            if (e.isHovered() && !e.isSelected()) return e;
        }
        return null;
    }

    @Nullable
    protected AnchorPointArea getAreaForElement(@NotNull AbstractEditorElement element) {
        if (element.element.anchorPoint == ElementAnchorPoints.ELEMENT) {
            if (element.element.anchorPointElementIdentifier != null) {
                AbstractEditorElement e = this.editor.getElementByInstanceIdentifier(element.element.anchorPointElementIdentifier);
                if (e != null) return new ElementAnchorPointArea(e);
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
        AbstractEditorElement e = this.getHoveredAnchorElement();
        if (e != null) return new ElementAnchorPointArea(e);
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        this.leftMouseDown = true;
        this.initialHoverArea = this.getMouseOverArea((int) mouseX, (int) mouseY);
        if (this.initialHoverArea == null) {
            AbstractEditorElement e = this.getHoveredAnchorElement();
            if (e != null) this.initialHoverArea = new ElementAnchorPointArea(e);
        }

        return GuiEventListener.super.mouseClicked(mouseX, mouseY, button);

    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        this.leftMouseDown = false;

        return GuiEventListener.super.mouseReleased(mouseX, mouseY, button);

    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    public class ElementAnchorPointArea extends AnchorPointArea {

        public AbstractEditorElement element = null;

        protected ElementAnchorPointArea(AbstractEditorElement element) {
            super(ElementAnchorPoints.ELEMENT, 0, 0, ProgressDirection.TO_TOP);
            this.applyElement(element);
        }

        protected ElementAnchorPointArea applyElement(AbstractEditorElement e) {
            this.element = e;
            this.x = e.element.getAbsoluteX();
            this.y = e.element.getAbsoluteY();
            this.width = e.element.getAbsoluteWidth();
            this.height = e.element.getAbsoluteHeight();
            return this;
        }

    }

    public class AnchorPointArea extends GuiComponent implements Renderable, GuiEventListener {

        public ElementAnchorPoint anchorPoint;
        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected ProgressDirection direction;

        protected AnchorPointArea(@NotNull ElementAnchorPoint anchorPoint, int width, int height, @NotNull ProgressDirection direction) {
            this.width = width;
            this.height = height;
            this.direction = direction;
            this.anchorPoint = anchorPoint;
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            int endX = this.x + this.getWidth();
            int endY = this.y + this.getHeight();
            fill(pose, this.x, this.y, endX, endY, RenderUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_NORMAL * this.getAlphaMultiplier())));
            UIBase.renderBorder(pose, this.x, this.y, endX, endY, 1, RenderUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_BORDER * this.getAlphaMultiplier())), true, true, true, true);
            UIBase.resetShaderColor();
        }

        protected void renderMouseOverProgress(@NotNull PoseStack pose, float progress) {
            int progressWidth = (int) ((float)this.getWidth() * progress);
            int progressHeight = (int) ((float)this.getHeight() * progress);
            int startX = this.x;
            int startY = this.y;
            int endX = this.x + progressWidth;
            int endY = this.y + this.getHeight();
            if (this.direction == ProgressDirection.TO_LEFT) {
                endX = this.x + this.getWidth();
                startX = endX - progressWidth;
            } else if (this.direction == ProgressDirection.TO_DOWN) {
                endX = this.x + this.getWidth();
                endY = this.y + progressHeight;
            } else if (this.direction == ProgressDirection.TO_TOP) {
                endX = this.x + this.getWidth();
                startY = endY - progressHeight;
            }
            fill(pose, startX, startY, endX, endY, RenderUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_NORMAL * this.getAlphaMultiplier())));

            UIBase.resetShaderColor();
        }

        protected int getWidth() {
            return this.width;
        }

        protected int getHeight() {
            return this.height;
        }

        protected float getAlphaMultiplier() {
            if (AnchorPointOverlay.this.leftMouseDown && AnchorPointOverlay.this.isElementPressed()) {
                return AREA_ALPHA_MULTIPLIER;
            }
            return 1.0F;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return UIBase.isXYInArea((int) mouseX, (int) mouseY, this.x, this.y, this.getWidth(), this.getWidth());
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
