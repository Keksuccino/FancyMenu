package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnchorPointOverlay extends GuiComponent implements Renderable, GuiEventListener {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final int AREA_HOVER_CHARGING_TIME_MS = 1000;
    protected static final int AREA_ALPHA_NORMAL = 40;
    protected static final int AREA_ALPHA_BORDER = 70;
    protected static final float AREA_ALPHA_MULTIPLIER = 2.0F;

    protected final LayoutEditorScreen editor;
    protected AnchorPointArea lastTickHoveredArea = null;
    protected AnchorPointArea currentlyHoveredArea = null;
    protected AnchorPointArea lastCompletedHoverArea = null;
    protected boolean lastTickDraggedEmpty = true;
    protected long areaHoverStartTime = -1;

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
        this.editor = Objects.requireNonNull(editor);
    }

    public void resetAreaHoverCache() {
        this.currentlyHoveredArea = null;
        this.lastTickHoveredArea = null;
        this.areaHoverStartTime = -1;
    }

    public void resetOverlay() {
        this.resetAreaHoverCache();
        this.setLastCompletedHoverArea(null);
        this.lastTickDraggedEmpty = true;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!FancyMenu.getOptions().showAnchorOverlay.getValue()) {
            this.resetOverlay();
            return;
        }

        //Don't render if overlay not always visible and no elements currently dragged
        if (!FancyMenu.getOptions().alwaysShowAnchorOverlay.getValue() && this.editor.getCurrentlyDraggedElements().isEmpty()) return;

        this.tickAreaMouseOver(mouseX, mouseY);

        RenderingUtils.resetShaderColor();
        RenderSystem.enableBlend();
        //Invert color of overlay based on what's rendered behind it
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        this.renderAreas(pose, mouseX, mouseY, partial);
        this.renderConnectionLines(pose);

        RenderSystem.defaultBlendFunc();
        RenderingUtils.resetShaderColor();

    }

    protected void renderAreas(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        int menuBarHeight = ((this.editor.menuBar != null) ? (int)((float)this.editor.menuBar.getHeight() * UIBase.calculateFixedScale(this.editor.menuBar.getScale())) : 0);
        if ((this.editor.menuBar != null) && !this.editor.menuBar.isExpanded()) menuBarHeight = 0;

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

        if ((this.currentlyHoveredArea != null) && FancyMenu.getOptions().changeAnchorOnHover.getValue()) {
            this.currentlyHoveredArea.renderMouseOverProgress(pose, this.calculateMouseOverProgress());
        }

    }

    protected float calculateMouseOverProgress() {
        if (this.currentlyHoveredArea != null) {
            long now = System.currentTimeMillis();
            if ((this.areaHoverStartTime + AREA_HOVER_CHARGING_TIME_MS) > now) {
                long diff = (this.areaHoverStartTime + AREA_HOVER_CHARGING_TIME_MS) - now;
                float f = Math.max(0.0F, Math.min(1.0F, Math.max(1F, (float)diff) / (float) AREA_HOVER_CHARGING_TIME_MS));
                return 1.0F - f;
            }
            return 1.0F;
        }
        return 0.0F;
    }

    protected void renderConnectionLines(@NotNull PoseStack pose) {
        List<AbstractEditorElement> elements = FancyMenu.getOptions().showAllAnchorConnections.getValue() ? this.editor.getAllElements() : this.editor.getCurrentlyDraggedElements();
        for (AbstractEditorElement e : elements) {
            boolean hidden = (e instanceof HideableElement h) && h.isHidden();
            if (!hidden) this.renderConnectionLineFor(pose, e);
        }
    }

    protected void renderConnectionLineFor(@NotNull PoseStack pose, @NotNull AbstractEditorElement e) {
        AnchorPointArea a = this.getParentAreaOfElement(e);
        if (a != null) {
            int xElement = e.getX() + (e.getWidth() / 2);
            int yElement = e.getY() + (e.getHeight() / 2);
            int xArea = a.getX() + (a.getWidth() / 2);
            int yArea = a.getY() + (a.getHeight() / 2);
            this.renderSquareLine(pose, xElement, yElement, xArea, yArea, 2, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorTheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_BORDER * a.getAlphaMultiplier())));
        }
    }

    @SuppressWarnings("all")
    protected void renderSquareLine(@NotNull PoseStack pose, int xElement, int yElement, int xArea, int yArea, int lineThickness, int color) {

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

    protected void tickAreaMouseOver(int mouseX, int mouseY) {
        boolean draggedEmpty = this.editor.getCurrentlyDraggedElements().isEmpty();
        if (!draggedEmpty) {
            this.currentlyHoveredArea = FancyMenu.getOptions().changeAnchorOnHover.getValue() ? this.getMouseOverArea(mouseX, mouseY) : null;
            //If just started dragging, set lastCompleted to current, to "ignore" the initially hovered area
            if (this.lastTickDraggedEmpty) {
                this.setLastCompletedHoverArea(this.currentlyHoveredArea);
            }
            //Reset lastCompleted if current is NULL
            if (this.currentlyHoveredArea == null) this.setLastCompletedHoverArea(null);
            //Set current to NULL if it was the last area that changed anchors of elements
            if (this.isSameArea(this.currentlyHoveredArea, this.lastCompletedHoverArea)) this.currentlyHoveredArea = null;
            //Update hoverStartTime if new area got hovered
            if ((this.currentlyHoveredArea != null) && !this.isSameArea(this.currentlyHoveredArea, this.lastTickHoveredArea)) {
                this.areaHoverStartTime = System.currentTimeMillis();
            }
            this.lastTickHoveredArea = this.currentlyHoveredArea;
            if (this.currentlyHoveredArea != null) {
                //Change anchor of dragged elements if area hovered long enough
                if ((this.areaHoverStartTime + AREA_HOVER_CHARGING_TIME_MS) <= System.currentTimeMillis()) {
                    for (AbstractEditorElement e : this.editor.getCurrentlyDraggedElements()) {
                        if (this.canChangeAnchorTo(e, this.currentlyHoveredArea)) {
                            e.setAnchorPointViaOverlay(this.currentlyHoveredArea, mouseX, mouseY);
                        }
                    }
                    this.setLastCompletedHoverArea(this.currentlyHoveredArea);
                    this.resetAreaHoverCache();
                }
            }
        } else {
            this.resetAreaHoverCache();
        }
        this.lastTickDraggedEmpty = draggedEmpty;
    }

    /**
     * Compares two {@link AnchorPointArea}s.<br>
     * Returns FALSE if one or both areas are NULL.
     */
    protected boolean isSameArea(@Nullable AnchorPointArea firstArea, @Nullable AnchorPointArea secondArea) {
        if ((firstArea == null) && (secondArea == null)) return true;
        if ((firstArea == null) || (secondArea == null)) return false;
        if ((firstArea instanceof ElementAnchorPointArea a1) && (secondArea instanceof ElementAnchorPointArea a2)) {
            return StringUtils.equals(a1.elementIdentifier, a2.elementIdentifier);
        }
        return firstArea.anchorPoint == secondArea.anchorPoint;
    }

    protected boolean canChangeAnchorTo(@NotNull AbstractEditorElement element, @NotNull AnchorPointArea area) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(area);
        if (this.isAttachedToAnchor(element, area)) return false;
        //Check if area is ElementAnchorPointArea and if so, check if area's element is child of the given element parameter
        if (area instanceof ElementAnchorPointArea a) {
            String parentOfElement = element.element.anchorPointElementIdentifier;
            if ((parentOfElement != null) && parentOfElement.equals(a.elementIdentifier)) return false;
        }
        return true;
    }

    /**
     * Returns NULL if there was an error while trying to get all child elements.
     */
    @Nullable
    protected List<AbstractEditorElement> getChildElementsOfDraggedElements() {
        List<AbstractEditorElement> currentlyDragged = this.editor.getCurrentlyDraggedElements();
        List<AbstractEditorElement> children = new ArrayList<>();
        for (AbstractEditorElement e : currentlyDragged) {
            List<AbstractEditorElement> childChainOfE = this.editor.getElementChildChainOfExcluding(e);
            if (childChainOfE == null) return null;
            childChainOfE.forEach(element -> {
                if (!currentlyDragged.contains(element)) children.add(element);
            });
        }
        return children;
    }

    protected boolean isAttachedToAnchor(@NotNull AbstractEditorElement element, @NotNull AnchorPointArea area) {
        if (area instanceof ElementAnchorPointArea ae) {
            String parentOfElement = element.element.anchorPointElementIdentifier;
            if (parentOfElement != null) {
                return ae.elementIdentifier.equals(parentOfElement);
            }
        }
        return element.element.anchorPoint == area.anchorPoint;
    }

    @Nullable
    protected AnchorPointArea getParentAreaOfElement(@NotNull AbstractEditorElement element) {
        if (element.element.anchorPoint == ElementAnchorPoints.ELEMENT) {
            if (element.element.anchorPointElementIdentifier != null) {
                //Safety check to lower the change to construct a broken ElementAnchorPointArea instance
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
    protected AbstractEditorElement getTopHoveredNotDraggedElement() {
        List<AbstractEditorElement> childrenOfDragged = this.getChildElementsOfDraggedElements();
        if (childrenOfDragged == null) {
            LOGGER.error("[FANCYMENU] Failed to get hovered element! Error while getting children of dragged elements!", new IllegalStateException());
            return null;
        }
        List<AbstractEditorElement> draggedElements = this.editor.getCurrentlyDraggedElements();
        List<AbstractEditorElement> notDraggedElements = this.editor.getHoveredElements();
        notDraggedElements.removeIf(draggedElements::contains);
        notDraggedElements.removeIf(childrenOfDragged::contains);
        return notDraggedElements.isEmpty() ? null : ListUtils.getLast(notDraggedElements);
    }

    @Nullable
    protected AnchorPointArea getMouseOverArea(int mouseX, int mouseY) {
        for (AnchorPointArea a : this.anchorPointAreas) {
            if (a.isMouseOver(mouseX, mouseY)) return a;
        }
        AbstractEditorElement e = this.getTopHoveredNotDraggedElement();
        if ((e != null) && !e.isSelected() && !e.isMultiSelected()) return new ElementAnchorPointArea(e.element.getInstanceIdentifier());
        return null;
    }

    /**
     * Has its own setter method for easier debugging.
     */
    protected void setLastCompletedHoverArea(@Nullable AnchorPointArea area) {
        this.lastCompletedHoverArea = area;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        this.resetAreaHoverCache();
        this.lastTickDraggedEmpty = true;
        this.setLastCompletedHoverArea(null);

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

        @NotNull
        public final String elementIdentifier;

        private ElementAnchorPointArea(@NotNull String elementIdentifier) {
            super(ElementAnchorPoints.ELEMENT, 0, 0, ProgressDirection.TO_TOP);
            this.elementIdentifier = Objects.requireNonNull(elementIdentifier);
        }

        @Override
        public String toString() {
            return "[element_anchor_point_area;id=" + this.elementIdentifier + ";x=" + this.getX() + ";y=" + this.getY() + ";w=" + this.getWidth() + ";h=" + this.getHeight() + "]";
        }

        @Nullable
        public AbstractEditorElement getElement() {
            AbstractEditorElement element = AnchorPointOverlay.this.editor.getElementByInstanceIdentifier(this.elementIdentifier);
            if (element == null) LOGGER.error("[FANCYMENU] Failed to get element instance of ElementAnchorPointArea! Element was NULL!", new NullPointerException());
            return element;
        }

        @Override
        protected int getX() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getX() : -100000;
        }

        @Override
        protected int getY() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getY() : -100000;
        }

        @Override
        protected int getWidth() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getWidth() : 1;
        }

        @Override
        protected int getHeight() {
            AbstractEditorElement element = this.getElement();
            return (element != null) ? element.getHeight() : 1;
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
            fill(pose, this.getX(), this.getY(), endX, endY, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorTheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_NORMAL * this.getAlphaMultiplier())));
            UIBase.renderBorder(pose, this.getX(), this.getY(), endX, endY, 1, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorTheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_BORDER * this.getAlphaMultiplier())), true, true, true, true);
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
            fill(pose, startX, startY, endX, endY, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorTheme().layout_editor_anchor_point_overlay_color.getColorInt(), (int)((float)AREA_ALPHA_NORMAL * this.getAlphaMultiplier())));
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
            if (!AnchorPointOverlay.this.editor.getCurrentlyDraggedElements().isEmpty()) {
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
