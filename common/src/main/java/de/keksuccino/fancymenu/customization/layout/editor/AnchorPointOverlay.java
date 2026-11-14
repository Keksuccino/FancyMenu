package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnchorPointOverlay implements Renderable, GuiEventListener {

    private static final Logger LOGGER = LogManager.getLogger();

    protected final LayoutEditorScreen editor;
    protected AnchorPointArea lastTickHoveredArea = null;
    protected AnchorPointArea currentlyHoveredArea = null;
    protected AnchorPointArea lastCompletedHoverArea = null;
    protected boolean lastTickDraggedEmpty = true;
    protected long areaHoverStartTime = -1;
    protected boolean overlayVisibilityKeybindPressed = false;
    /**
     * The container whose content the anchor areas stand for right now, or NULL if they stand for the screen.<br>
     * Anchors of children refer to the content of their container, so while the user drags children of one container,
     * the areas sit along what that container shows of its content.
     */
    @Nullable
    protected AbstractEditorElement<?, ?> anchorSpaceContainer = null;
    /** If the dragged elements have anchors the overlay can change. Children of a container that places them itself have none. */
    protected boolean draggedElementsHaveAnchors = true;

    protected static final int SCREEN_ANCHOR_AREA_SIZE = 30;
    protected static final int MIN_CONTAINER_ANCHOR_AREA_SIZE = 6;

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
        this.overlayVisibilityKeybindPressed = false;
    }

    public double getOverlayHoverChargingTimeSeconds() {
        return FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.getValue();
    }

    public long getOverlayHoverChargingTimeMs() {
        return (long) (this.getOverlayHoverChargingTimeSeconds() * 1000.0F);
    }

    @NotNull
    public AnchorOverlayVisibilityMode getVisibilityMode() {
        AnchorOverlayVisibilityMode m = AnchorOverlayVisibilityMode.getByName(FancyMenu.getOptions().anchorOverlayVisibilityMode.getValue());
        return (m != null) ? m : AnchorOverlayVisibilityMode.DRAGGING;
    }

    public boolean isOverlayVisible() {
        if (this.getVisibilityMode() == AnchorOverlayVisibilityMode.DISABLED) return false;
        if (this.getVisibilityMode() == AnchorOverlayVisibilityMode.DRAGGING) return !this.editor.getCurrentlyDraggedElements().isEmpty();
        if (this.getVisibilityMode() == AnchorOverlayVisibilityMode.KEYBIND) return this.overlayVisibilityKeybindPressed;
        return true; //mode == ALWAYS
    }

    public boolean invertOverlayColors() {
        return FancyMenu.getOptions().invertAnchorOverlayColor.getValue();
    }

    @Nullable
    public DrawableColor getOverlayColorBaseOverride() {
        String override = FancyMenu.getOptions().anchorOverlayColorBaseOverride.getValue();
        if (override.trim().isEmpty()) return null;
        if (!TextValidators.HEX_COLOR_TEXT_VALIDATOR.get(override)) return null;
        return DrawableColor.of(override);
    }

    @Nullable
    public DrawableColor getOverlayColorBorderOverride() {
        String override = FancyMenu.getOptions().anchorOverlayColorBorderOverride.getValue();
        if (override.trim().isEmpty()) return null;
        if (!TextValidators.HEX_COLOR_TEXT_VALIDATOR.get(override)) return null;
        return DrawableColor.of(override);
    }

    @NotNull
    public DrawableColor getOverlayColorBase() {
        if (this.invertOverlayColors()) return DrawableColor.WHITE;
        DrawableColor override = this.getOverlayColorBaseOverride();
        return (override != null) ? override : UIBase.getUITheme().layout_editor_anchor_point_overlay_color_base;
    }

    @NotNull
    public DrawableColor getOverlayColorBorder() {
        if (this.invertOverlayColors()) return DrawableColor.WHITE;
        DrawableColor override = this.getOverlayColorBorderOverride();
        return (override != null) ? override : UIBase.getUITheme().layout_editor_anchor_point_overlay_color_border;
    }

    public boolean isOverlayBusy() {
        return !this.editor.getCurrentlyDraggedElements().isEmpty();
    }

    public float getOverlayOpacityNormal() {
        return FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.getValue();
    }

    public float getOverlayOpacityBusy() {
        return FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.getValue();
    }

    /**
     * Value between 0 and 255.
     */
    public int getOverlayOpacity() {
        if (this.invertOverlayColors()) return 255;
        float percentage = this.isOverlayBusy() ? this.getOverlayOpacityBusy() : this.getOverlayOpacityNormal();
        if (percentage > 1.0F) percentage = 1.0F;
        if (percentage < 0.0F) percentage = 0.0F;
        return Math.min(255, Math.max(0, (int)(percentage * 255.0F)));
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (!this.isOverlayVisible()) {
            this.resetOverlay();
            return;
        }

        this.updateAnchorSpace();
        if (!this.draggedElementsHaveAnchors) {
            this.resetAreaHoverCache();
            return;
        }

        // The areas have to be where they are in this frame before the mouse gets checked against them
        this.updateAreaBounds();
        this.tickAreaMouseOver(mouseX, mouseY);

        RenderingUtils.resetShaderColor(graphics);
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        //Invert color of overlay based on what's rendered behind it
        if (this.invertOverlayColors()) {
            GlStateManager._blendFuncSeparate(775, 769, 1, 0);
        }

        this.renderAreas(graphics, mouseX, mouseY, partial);
        this.renderConnectionLines(graphics);

        de.keksuccino.fancymenu.util.rendering.RenderingUtils.defaultBlendFunc();
        RenderingUtils.resetShaderColor(graphics);

    }

    /**
     * Works out what the anchor areas stand for in this frame: the screen, or the content of the container the dragged elements are in.
     */
    protected void updateAnchorSpace() {
        this.anchorSpaceContainer = null;
        this.draggedElementsHaveAnchors = true;
        List<AbstractEditorElement<?, ?>> dragged = this.editor.getCurrentlyDraggedElements();
        ElementContainer shared = null;
        boolean first = true;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if (this.editor.containerDragDropHandler.movesWithDraggedContainer(e, dragged)) continue;
            ElementContainer container = e.element.getParentContainer();
            if (first) {
                shared = container;
                first = false;
            } else if (shared != container) {
                // Elements of different spaces have no anchor areas in common, so the areas stay the ones of the screen
                return;
            }
        }
        if (shared == null) return;
        if (shared.managesChildBoxes()) {
            this.draggedElementsHaveAnchors = false;
            return;
        }
        AbstractEditorElement<?, ?> containerEditorElement = this.editor.getElementByInstanceIdentifier(shared.getContainerElement().getInstanceIdentifier());
        if (containerEditorElement instanceof ContainerEditorElement) this.anchorSpaceContainer = containerEditorElement;
    }

    /**
     * Returns if the anchors of the element refer to what the anchor areas stand for right now.
     */
    protected boolean isInAnchorSpace(@NotNull AbstractEditorElement<?, ?> element) {
        ElementContainer container = element.element.getParentContainer();
        if (container == null) return this.anchorSpaceContainer == null;
        return (this.anchorSpaceContainer != null) && (this.anchorSpaceContainer.element == container.getContainerElement());
    }

    /**
     * Puts the anchor areas at the edges and the middle of what they stand for in this frame: the screen, or the container the dragged elements are in.
     */
    protected void updateAreaBounds() {

        int spaceX = 0;
        int spaceY = 0;
        int spaceWidth = ScreenUtils.getScreenWidth();
        int spaceHeight = ScreenUtils.getScreenHeight();
        int areaSize = SCREEN_ANCHOR_AREA_SIZE;
        // On the screen, the areas reach a pixel beyond its edges, which hides the part of their border that would lie on the edge
        int overhang = 1;
        if (this.anchorSpaceContainer != null) {
            ContainerEditorElement container = (ContainerEditorElement) this.anchorSpaceContainer;
            spaceX = container.getDropAreaX();
            spaceY = container.getDropAreaY();
            spaceWidth = container.getDropAreaWidth();
            spaceHeight = container.getDropAreaHeight();
            // Three areas have to fit next to each other with room between them, in containers of every size
            areaSize = Math.max(MIN_CONTAINER_ANCHOR_AREA_SIZE, Math.min(SCREEN_ANCHOR_AREA_SIZE, Math.min(spaceWidth, spaceHeight) / 5));
            overhang = 0;
        } else {
            MenuBar menuBar = this.editor.getCurrentMenuBar();
            int menuBarHeight = ((menuBar != null) ? (int)((float)MenuBar.PIXEL_SIZE * MenuBar.getRenderScale()) : 0);
            if ((menuBar != null) && !menuBar.isExpanded()) menuBarHeight = 0;
            spaceY = menuBarHeight;
            spaceHeight -= menuBarHeight;
        }

        int left = spaceX - overhang;
        int centerX = spaceX + (spaceWidth / 2) - (areaSize / 2);
        int right = spaceX + spaceWidth - areaSize + overhang;
        int top = spaceY - overhang;
        // The middle of the screen stays the middle of the screen, also with the menu bar taking some of it
        int centerY = ((this.anchorSpaceContainer != null) ? (spaceY + (spaceHeight / 2)) : (ScreenUtils.getScreenHeight() / 2)) - (areaSize / 2);
        int bottom = spaceY + spaceHeight - areaSize + overhang;

        this.topLeftArea.setBounds(left, top, areaSize);
        this.midLeftArea.setBounds(left, centerY, areaSize);
        this.bottomLeftArea.setBounds(left, bottom, areaSize);
        this.topCenteredArea.setBounds(centerX, top, areaSize);
        this.midCenteredArea.setBounds(centerX, centerY, areaSize);
        this.bottomCenteredArea.setBounds(centerX, bottom, areaSize);
        this.topRightArea.setBounds(right, top, areaSize);
        this.midRightArea.setBounds(right, centerY, areaSize);
        this.bottomRightArea.setBounds(right, bottom, areaSize);

    }

    protected void renderAreas(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        for (AnchorPointArea area : this.anchorPointAreas) {
            area.extractRenderState(graphics, mouseX, mouseY, partial);
        }

        if (this.currentlyHoveredArea != null) {
            this.currentlyHoveredArea.renderMouseOverProgress(graphics, this.calculateMouseOverProgress());
        }

    }

    protected float calculateMouseOverProgress() {
        if (this.currentlyHoveredArea != null) {
            long now = System.currentTimeMillis();
            if ((this.areaHoverStartTime + this.getOverlayHoverChargingTimeMs()) > now) {
                long diff = (this.areaHoverStartTime + this.getOverlayHoverChargingTimeMs()) - now;
                float f = Math.max(0.0F, Math.min(1.0F, Math.max(1F, (float)diff) / (float)this.getOverlayHoverChargingTimeMs()));
                return 1.0F - f;
            }
            return 1.0F;
        }
        return 0.0F;
    }

    protected void renderConnectionLines(@NotNull GuiGraphicsExtractor graphics) {
        List<AbstractEditorElement<?, ?>> elements = FancyMenu.getOptions().showAllAnchorOverlayConnections.getValue() ? this.editor.getAllElements() : this.editor.getCurrentlyDraggedElements();
        for (AbstractEditorElement<?, ?> e : elements) {
            boolean hidden = (e instanceof HideableElement h) && h.isHidden();
            if (!hidden) this.renderConnectionLineFor(graphics, e);
        }
    }

    protected void renderConnectionLineFor(@NotNull GuiGraphicsExtractor graphics, @NotNull AbstractEditorElement<?, ?> e) {
        // The areas only stand for the anchors of elements whose anchors refer to the same space
        if ((e.element.anchorPoint != ElementAnchorPoints.ELEMENT) && !this.isInAnchorSpace(e)) return;
        AnchorPointArea a = this.getParentAreaOfElement(e);
        if (a != null) {
            int xElement = e.getX() + (e.getWidth() / 2);
            int yElement = e.getY() + (e.getHeight() / 2);
            int xArea = a.getX() + (a.getWidth() / 2);
            int yArea = a.getY() + (a.getHeight() / 2);
            this.renderSquareLine(graphics, xElement, yElement, xArea, yArea, 2, RenderingUtils.replaceAlphaInColor(this.getOverlayColorBase().getColorInt(), this.getOverlayOpacity()));
        }
    }

    @SuppressWarnings("all")
    protected void renderSquareLine(@NotNull GuiGraphicsExtractor graphics, int xElement, int yElement, int xArea, int yArea, int lineThickness, int color) {

        int horizontalWidth = Math.max(xElement, xArea) - Math.min(xElement, xArea);
        int verticalHeight = Math.max(yElement, yArea) - Math.min(yElement, yArea);
        int horizontalX = Math.min(xElement, xArea);
        int horizontalY = yArea;
        int verticalX = xElement;
        int verticalY = Math.min(yElement, yArea);
        if (xArea < xElement) {
            horizontalX += lineThickness;
        }

        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        UIBase.resetShaderColor(graphics);
        //Horizontal Line
        graphics.fill(horizontalX, horizontalY, horizontalX + horizontalWidth, horizontalY + lineThickness, color);
        //Vertical Line
        graphics.fill(verticalX, verticalY, verticalX + lineThickness, verticalY + verticalHeight, color);
        UIBase.resetShaderColor(graphics);

    }

    protected void tickAreaMouseOver(int mouseX, int mouseY) {
        boolean hoverChangeAllowed = this.editor.isUserMovingElements();
        if (hoverChangeAllowed) {
            this.currentlyHoveredArea = FancyMenu.getOptions().anchorOverlayChangeAnchorOnAreaHover.getValue() ? this.getMouseOverArea(mouseX, mouseY) : null;
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
                if ((this.areaHoverStartTime + this.getOverlayHoverChargingTimeMs()) <= System.currentTimeMillis()) {
                    for (AbstractEditorElement<?, ?> e : this.editor.getCurrentlyDraggedElements()) {
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
        this.lastTickDraggedEmpty = !hoverChangeAllowed;
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

    protected boolean canChangeAnchorTo(@NotNull AbstractEditorElement<?, ?> element, @NotNull AnchorPointArea area) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(area);
        if (this.isAttachedToAnchor(element, area)) return false;
        // Elements that move with a container the user drags keep their place in it
        if (this.editor.containerDragDropHandler.movesWithDraggedContainer(element, this.editor.getCurrentlyDraggedElements())) return false;
        ElementContainer container = element.element.getParentContainer();
        if ((container != null) && container.managesChildBoxes()) return false;
        if (area instanceof ElementAnchorPointArea elementArea) {
            // An element can only follow an element of the same space: its container, or the main layout
            AbstractEditorElement<?, ?> anchorElement = this.editor.getElementByInstanceIdentifier(elementArea.elementIdentifier);
            if ((anchorElement == null) || (anchorElement.element.getParentContainer() != container)) return false;
        } else if (!this.isInAnchorSpace(element)) {
            return false;
        }
        //Check if area is ElementAnchorPointArea and if so, check if area's element is child of the given element parameter
        if (area instanceof ElementAnchorPointArea) {
            AbstractEditorElement<?, ?> areaElement = this.editor.getElementByInstanceIdentifier(((ElementAnchorPointArea) area).elementIdentifier);
            AbstractElement parentOfAreaElement = (areaElement != null) ? areaElement.element.getElementAnchorPointParent() : null;
            if ((parentOfAreaElement != null) && parentOfAreaElement.getInstanceIdentifier().equals(element.element.getInstanceIdentifier())) return false;
        }
        return true;
    }

    /**
     * Returns NULL if there was an error while trying to get all child elements.
     */
    @Nullable
    protected List<AbstractEditorElement<?, ?>> getChildElementsOfDraggedElements() {
        List<AbstractEditorElement<?, ?>> currentlyDragged = this.editor.getCurrentlyDraggedElements();
        List<AbstractEditorElement<?, ?>> children = new ArrayList<>();
        for (AbstractEditorElement<?, ?> e : currentlyDragged) {
            List<AbstractEditorElement<?, ?>> childChainOfE = this.editor.getElementChildChainOfExcluding(e);
            if (childChainOfE == null) return null;
            childChainOfE.forEach(element -> {
                if (!currentlyDragged.contains(element)) children.add(element);
            });
        }
        return children;
    }

    protected boolean isAttachedToAnchor(@NotNull AbstractEditorElement<?, ?> element, @NotNull AnchorPointArea area) {
        if (area instanceof ElementAnchorPointArea ae) {
            String parentOfElement = element.element.getAnchorPointElementIdentifier();
            if (parentOfElement != null) {
                return ae.elementIdentifier.equals(parentOfElement);
            }
        }
        return element.element.anchorPoint == area.anchorPoint;
    }

    @Nullable
    protected AnchorPointArea getParentAreaOfElement(@NotNull AbstractEditorElement<?, ?> element) {
        if (element.element.anchorPoint == ElementAnchorPoints.ELEMENT) {
            if (element.element.getAnchorPointElementIdentifier() != null) {
                //Safety check to lower the change to construct a broken ElementAnchorPointArea instance
                AbstractEditorElement<?, ?> e = this.editor.getElementByInstanceIdentifier(element.element.getAnchorPointElementIdentifier());
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
    protected AbstractEditorElement<?, ?> getTopHoveredNotDraggedElement() {
        List<AbstractEditorElement<?, ?>> childrenOfDragged = this.getChildElementsOfDraggedElements();
        if (childrenOfDragged == null) {
            LOGGER.error("[FANCYMENU] Failed to get hovered element! Error while getting children of dragged elements!", new IllegalStateException());
            return null;
        }
        List<AbstractEditorElement<?, ?>> draggedElements = this.editor.getCurrentlyDraggedElements();
        List<AbstractEditorElement<?, ?>> notDraggedElements = this.editor.getHoveredElements();
        notDraggedElements.removeIf(draggedElements::contains);
        notDraggedElements.removeIf(childrenOfDragged::contains);
        return notDraggedElements.isEmpty() ? null : ListUtils.getLast(notDraggedElements);
    }

    @Nullable
    protected AnchorPointArea getMouseOverArea(int mouseX, int mouseY) {
        if (FancyMenu.getOptions().anchorOverlayChangeAnchorOnAreaHover.getValue()) {
            for (AnchorPointArea a : this.anchorPointAreas) {
                if (a.isMouseOver(mouseX, mouseY)) return a;
            }
        }
        if (FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.getValue()) {
            AbstractEditorElement<?, ?> e = this.getTopHoveredNotDraggedElement();
            if (e != null) {
                AbstractElement parentOfHovered = e.element.getElementAnchorPointParent();
                if (parentOfHovered != null) {
                    for (AbstractEditorElement<?, ?> dragged : this.editor.getCurrentlyDraggedElements()) {
                        if (parentOfHovered.getInstanceIdentifier().equals(dragged.element.getInstanceIdentifier())) return null;
                    }
                }
                // Hovering a container with something it can hold means dropping it in there, not anchoring to it
                if (e instanceof ContainerEditorElement container) {
                    for (AbstractEditorElement<?, ?> dragged : this.editor.getCurrentlyDraggedElements()) {
                        if ((dragged.element.getParentContainer() != e.element) && container.canHoldChild(dragged)) return null;
                    }
                }
                // An element can only follow one of the same space, so one that none of the dragged elements share theirs with is nothing to anchor to
                boolean sharesSpace = false;
                for (AbstractEditorElement<?, ?> dragged : this.editor.getCurrentlyDraggedElements()) {
                    if (dragged.element.getParentContainer() == e.element.getParentContainer()) sharesSpace = true;
                }
                if (!sharesSpace) return null;
                if (!e.isSelected() && !e.isMultiSelected()) return new ElementAnchorPointArea(e.element.getInstanceIdentifier());
            }
        }
        return null;
    }

    /**
     * Has its own setter method for easier debugging.
     */
    protected void setLastCompletedHoverArea(@Nullable AnchorPointArea area) {
        this.lastCompletedHoverArea = area;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        this.resetAreaHoverCache();
        this.lastTickDraggedEmpty = true;
        this.setLastCompletedHoverArea(null);

        return GuiEventListener.super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));

    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }
    
    public boolean keyPressed(int keycode, int scancode, int modifiers) {

        String key = GLFW.glfwGetKeyName(keycode, scancode);
        if (key == null) key = "";

        if (key.equals("o")) this.overlayVisibilityKeybindPressed = true;

        return GuiEventListener.super.keyPressed(new KeyEvent(keycode, scancode, modifiers));

    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return this.keyReleased(event.key(), event.scancode(), event.modifiers());
    }
    
    public boolean keyReleased(int keycode, int scancode, int modifiers) {

        String key = GLFW.glfwGetKeyName(keycode, scancode);
        if (key == null) key = "";

        if (key.equals("o")) this.overlayVisibilityKeybindPressed = false;

        return GuiEventListener.super.keyReleased(new KeyEvent(keycode, scancode, modifiers));

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
        public AbstractEditorElement<?, ?> getElement() {
            AbstractEditorElement<?, ?> element = AnchorPointOverlay.this.editor.getElementByInstanceIdentifier(this.elementIdentifier);
            if (element == null) LOGGER.error("[FANCYMENU] Failed to get element instance of ElementAnchorPointArea! Element was NULL!", new NullPointerException());
            return element;
        }

        @Override
        protected int getX() {
            AbstractEditorElement<?, ?> element = this.getElement();
            return (element != null) ? element.getX() : -100000;
        }

        @Override
        protected int getY() {
            AbstractEditorElement<?, ?> element = this.getElement();
            return (element != null) ? element.getY() : -100000;
        }

        @Override
        protected int getWidth() {
            AbstractEditorElement<?, ?> element = this.getElement();
            return (element != null) ? element.getWidth() : 1;
        }

        @Override
        protected int getHeight() {
            AbstractEditorElement<?, ?> element = this.getElement();
            return (element != null) ? element.getHeight() : 1;
        }

        @SuppressWarnings("all")
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            AbstractEditorElement<?, ?> element = this.getElement();
            return (element != null) ? element.isMouseOver(mouseX, mouseY) : false;
        }

    }

    public class AnchorPointArea implements Renderable, GuiEventListener {

        public final ElementAnchorPoint anchorPoint;
        private int x;
        private int y;
        private int width;
        private int height;
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
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            int endX = this.getX() + this.getWidth();
            int endY = this.getY() + this.getHeight();
            graphics.fill(this.getX(), this.getY(), endX, endY, RenderingUtils.replaceAlphaInColor(getOverlayColorBase().getColorInt(), getOverlayOpacity()));
            UIBase.renderBorder(graphics, this.getX(), this.getY(), endX, endY, 1, RenderingUtils.replaceAlphaInColor(getOverlayColorBorder().getColorInt(), getOverlayOpacity()), true, true, true, true);
            UIBase.resetShaderColor(graphics);
        }

        protected void renderMouseOverProgress(@NotNull GuiGraphicsExtractor graphics, float progress) {
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
            graphics.fill(startX, startY, endX, endY, RenderingUtils.replaceAlphaInColor(getOverlayColorBorder().getColorInt(), getOverlayOpacity()));
            UIBase.resetShaderColor(graphics);
        }

        protected void setBounds(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.width = size;
            this.height = size;
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

    public enum AnchorOverlayVisibilityMode implements LocalizedCycleEnum<AnchorOverlayVisibilityMode> {

        DISABLED("disabled"),
        ALWAYS("always"),
        DRAGGING("dragging"),
        KEYBIND("keybind");

        final String name;

        AnchorOverlayVisibilityMode(@NotNull String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.editor.anchor_overlay.visibility_mode";
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return WARNING_TEXT_STYLE.get();
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull AnchorOverlayVisibilityMode[] getValues() {
            return AnchorOverlayVisibilityMode.values();
        }

        @Override
        public @Nullable AnchorPointOverlay.AnchorOverlayVisibilityMode getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static AnchorOverlayVisibilityMode getByName(@NotNull String name) {
            for (AnchorOverlayVisibilityMode m : AnchorOverlayVisibilityMode.values()) {
                if (m.name.equals(name)) return m;
            }
            return null;
        }

    }

}
