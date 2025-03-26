package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("all")
public class LayerLayoutEditorWidgetWorkingCopy extends AbstractLayoutEditorWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    protected ScrollArea scrollArea;

    // Added fields for drag and drop functionality
    protected ScrollAreaEntry draggedEntry = null;
    private int dragTargetIndex = -1;
    private boolean isDragging = false;
    private static final int DROP_INDICATOR_THICKNESS = 3;

    public LayerLayoutEditorWidgetWorkingCopy(LayoutEditorScreen editor, AbstractLayoutEditorWidgetBuilder<?> builder) {
        super(editor, builder);

        this.displayLabel = Component.translatable("fancymenu.editor.widgets.layers");

        this.scrollArea = new ScrollArea(0, 0, 0, 0) {
            @Override
            public void updateScrollArea() {
                int grabberOffset = 5;
                this.verticalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.verticalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.verticalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - grabberOffset;
                this.verticalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - this.horizontalScrollBar.grabberHeight - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.horizontalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.horizontalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - this.verticalScrollBar.grabberWidth - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - grabberOffset;
            }
        };

        this.scrollArea.borderColor = () -> UIBase.getUIColorTheme().area_background_color;

        this.updateList(false);
    }

    @Override
    public void refresh() {
        super.refresh();
        this.updateList(false);
    }

    public void updateList(boolean keepScroll) {
        float scroll = this.scrollArea.verticalScrollBar.getScroll();
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                this.children.remove(l.editLayerNameBox);
            }
        }
        this.scrollArea.clearEntries();
        if (this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        for (AbstractEditorElement e : Lists.reverse(new ArrayList<>(this.editor.normalEditorElements))) {
            LayerElementEntry layer = new LayerElementEntry(this.scrollArea, this, e);
            this.children.add(layer.editLayerNameBox);
            this.scrollArea.addEntry(layer);
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        if (!this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        if (keepScroll) this.scrollArea.verticalScrollBar.setScroll(scroll);

        // Reset drag state when list is updated
        this.draggedEntry = null;
        this.dragTargetIndex = -1;
        this.isDragging = false;
    }

    @Override
    protected void renderBody(@NotNull GuiGraphics graphics, double mouseX, double mouseY, float partial) {
        fillF(graphics, this.getRealBodyX(), this.getRealBodyY(), this.getRealBodyX() + this.getBodyWidth(), this.getRealBodyY() + this.getBodyHeight(), UIBase.getUIColorTheme().area_background_color.getColorInt());

        this.scrollArea.setX(this.getRealBodyX());
        this.scrollArea.setY(this.getRealBodyY());
        this.scrollArea.setWidth(this.getBodyWidth());
        this.scrollArea.setHeight(this.getBodyHeight());
        this.scrollArea.setApplyScissor(false);
        this.scrollArea.horizontalScrollBar.active = false;
        this.scrollArea.makeEntriesWidthOfArea = true;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;

        graphics.enableScissor((int) this.getRealBodyX(), (int) this.getRealBodyY(), (int) (this.getRealBodyX() + this.getBodyWidth()), (int) (this.getRealBodyY() + this.getBodyHeight()));

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);

        this.scrollArea.render(graphics, (int) mouseX, (int) mouseY, partial);

        // Render the drop indicator if currently dragging
        if (isDragging && dragTargetIndex >= 0 && dragTargetIndex <= this.scrollArea.getEntries().size()) {
            float indicatorY;

            // This is the key change - make the indicator position clearer
            if (dragTargetIndex == this.scrollArea.getEntries().size()) {
                // If dropping at the end of the list, show indicator below the last entry
                if (!this.scrollArea.getEntries().isEmpty()) {
                    ScrollAreaEntry lastEntry = this.scrollArea.getEntries().get(this.scrollArea.getEntries().size() - 1);
                    indicatorY = lastEntry.getY() + lastEntry.getHeight();
                } else {
                    indicatorY = this.scrollArea.getInnerY();
                }
            } else {
                // This is important: We always draw the indicator at the TOP of the target entry
                // This ensures the visual position matches where the item will be placed
                ScrollAreaEntry targetEntry = this.scrollArea.getEntries().get(dragTargetIndex);
                indicatorY = targetEntry.getY();
            }

            // Draw thicker drop indicator line
            fillF(graphics, this.scrollArea.getInnerX(), indicatorY - DROP_INDICATOR_THICKNESS/2f,
                    this.scrollArea.getInnerX() + this.scrollArea.getInnerWidth(),
                    indicatorY + DROP_INDICATOR_THICKNESS/2f,
                    UIBase.getUIColorTheme().element_border_color_hover.getColorInt());
        }

        graphics.pose().popPose();

        graphics.disableScissor();
    }

    @Override
    protected @Nullable ResizingEdge updateHoveredResizingEdge() {
        if (this.scrollArea.isMouseInteractingWithGrabbers()) return null;
        return super.updateHoveredResizingEdge();
    }

    @Override
    protected boolean mouseClickedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                if (!l.isLayerNameHovered()) {
                    l.stopEditingLayerName();
                }
            }
        }

        if (this.isVisible()) {
            if (super.mouseClickedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button)) return true;
            if (this.isExpanded()) {
                //Override original mouseClicked of ScrollArea, to use a combination of real and translated mouse coordinates
                if (this.scrollArea.verticalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
                if (this.scrollArea.horizontalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
                for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
                    if (entry.mouseClicked(realMouseX, realMouseY, button)) return true;
                }
            }
        }

        return this.isVisible() && this.isMouseOver();

    }

    @Override
    protected boolean mouseReleasedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {

        // Handle drop operation when mouse button is released
        if (button == 0 && isDragging && draggedEntry instanceof LayerElementEntry && dragTargetIndex >= 0) {
            finishDragOperation();
        }

        // Reset drag state
        isDragging = false;
        draggedEntry = null;
        dragTargetIndex = -1;

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                if (l.layerMouseReleased(realMouseX, realMouseY, button)) return true;
            }
        }

        return super.mouseReleasedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button);

    }

    @Override
    protected boolean mouseDraggedComponent(double translatedMouseX, double translatedMouseY, int button, double d1, double d2) {

        if (isDragging && button == 0) {
            updateDragTarget(translatedMouseX, translatedMouseY, this.getRealMouseX(), this.getRealMouseY());
        }

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                if (l.layerMouseDragged(translatedMouseX, translatedMouseY, button, d1, d2)) return true;
            }
        }

        return super.mouseDraggedComponent(translatedMouseX, translatedMouseY, button, d1, d2);

    }

    /**
     * Updates the drag target index based on current mouse position
     */
    private void updateDragTarget(double translatedMouseX, double translatedMouseY, double realMouseX, double realMouseY) {
        if (!isDragging || draggedEntry == null) return;

        // Get the index of the entry being dragged
        int draggedIndex = this.scrollArea.getEntries().indexOf(draggedEntry);
        if (draggedIndex < 0) return;

        // Find exactly which entry the mouse is directly over
        int mouseOverIndex = -1;
        for (int i = 0; i < this.scrollArea.getEntries().size(); i++) {
            ScrollAreaEntry entry = this.scrollArea.getEntries().get(i);

            if (entry.isMouseOver(realMouseX, realMouseY)) {
                mouseOverIndex = i;
                break;
            }
        }

        // Special handling for when mouse is over the Vanilla entry at the top
        if (this.editor.layout.renderElementsBehindVanilla &&
                mouseOverIndex == 0 &&
                this.scrollArea.getEntries().get(0) instanceof VanillaLayerElementEntry) {

            // Get the Vanilla entry
            VanillaLayerElementEntry vanillaEntry = (VanillaLayerElementEntry)this.scrollArea.getEntries().get(0);
            float entryMidpoint = vanillaEntry.getY() + vanillaEntry.getHeight() / 2f;

            if (translatedMouseY < entryMidpoint) {
                // Mouse is in top half of Vanilla entry - position at top
                dragTargetIndex = 0;
            } else {
                // Mouse is in bottom half of Vanilla entry - position right below it
                // Index 2 skips Vanilla entry (0) and its separator (1)
                dragTargetIndex = 2;
            }
            return;
        }

        // Special case: mouse is above all entries
        if (mouseOverIndex == -1 && !this.scrollArea.getEntries().isEmpty() &&
                translatedMouseY < this.scrollArea.getEntries().get(0).getY()) {
            dragTargetIndex = 0;
            return;
        }

        // Special case: mouse is below all entries
        if (mouseOverIndex == -1 && !this.scrollArea.getEntries().isEmpty() &&
                translatedMouseY > this.scrollArea.getEntries().get(this.scrollArea.getEntries().size() - 1).getY() +
                        this.scrollArea.getEntries().get(this.scrollArea.getEntries().size() - 1).getHeight()) {
            dragTargetIndex = this.scrollArea.getEntries().size();
            return;
        }

        // Don't continue the logic here to avoid out-of-bounds errors
        if (mouseOverIndex == -1) {
            return;
        }

        // If we're over a separator, adjust to the nearest actual layer
        if (mouseOverIndex % 2 == 1) { // Separator entries have odd indices
            // Determine whether to go up or down based on mouse position
            ScrollAreaEntry separator = this.scrollArea.getEntries().get(mouseOverIndex);
            float separatorMidpoint = separator.getY() + separator.getHeight() / 2f;

            if (translatedMouseY < separatorMidpoint) {
                mouseOverIndex = Math.max(0, mouseOverIndex - 1);
            } else {
                mouseOverIndex = Math.min(this.scrollArea.getEntries().size() - 1, mouseOverIndex + 1);
            }
        }

        // If we're over the entry being dragged, don't change anything
        if (mouseOverIndex == draggedIndex) {
            return;
        }

        // Now determine where to place the indicator based on mouse position
        ScrollAreaEntry targetEntry = this.scrollArea.getEntries().get(mouseOverIndex);
        float entryMidpoint = targetEntry.getY() + targetEntry.getHeight() / 2f;

        if (translatedMouseY < entryMidpoint) {
            // If in top half, place before this entry
            dragTargetIndex = mouseOverIndex;
        } else {
            // If in bottom half, place after this entry
            dragTargetIndex = mouseOverIndex + 1;
        }

        // Ensure we don't place the indicator exactly at the dragged entry's position
        if (dragTargetIndex == draggedIndex) {
            if (translatedMouseY > this.scrollArea.getEntries().get(draggedIndex).getY() +
                    this.scrollArea.getEntries().get(draggedIndex).getHeight() / 2f) {
                dragTargetIndex = draggedIndex + 1;
            } else {
                dragTargetIndex = Math.max(0, draggedIndex - 1);
            }
        } else if (dragTargetIndex == draggedIndex + 1) {
            if (draggedIndex < this.scrollArea.getEntries().size() - 1) {
                ScrollAreaEntry nextEntry = this.scrollArea.getEntries().get(draggedIndex + 1);
                if (translatedMouseY < nextEntry.getY() + nextEntry.getHeight() / 2f) {
                    // No change needed
                } else {
                    dragTargetIndex = draggedIndex + 2;
                }
            }
        }
    }

    /**
     * Converts a UI list index to an actual element index in the editor
     * @param uiIndex The index in the UI list
     * @param forDropIndicator True if this conversion is for a drop indicator position
     * @return The corresponding index in the editor's element list
     */
    private int getElementIndexFromUIIndex(int uiIndex, boolean forDropIndicator) {
        int elementCount = this.editor.normalEditorElements.size();
        if (elementCount == 0) return -1;

        // Handle special cases
        if (uiIndex < 0) return -1;
        if (uiIndex > this.scrollArea.getEntries().size()) {
            return 0; // Drop at bottom = index 0 in elements list
        }

        // Handle vanilla entry special cases
        if (this.editor.layout.renderElementsBehindVanilla) { // Vanilla entry at TOP of scroll area
            if (uiIndex <= 1) {
                // If dropping at or above vanilla entry
                return elementCount - 1;
            }

            // Adjust for vanilla entry and separator
            int entryIndex = (uiIndex - 2) / 2;

            // Convert to element index (accounting for reverse order)
            int elementIndex = elementCount - 1 - entryIndex;

            // The drop indicator needs to be adjusted differently
            if (forDropIndicator && uiIndex % 2 == 0) {
                // If indicator is at TOP of an entry, element should go ABOVE it (one index higher)
                return elementIndex + 1;
            }

            return elementIndex;
        } else { // Vanilla entry at BOTTOM of scroll area
            // Regular case without vanilla at top
            int entryIndex = uiIndex / 2;

            // Convert to element index (accounting for reverse order)
            int elementIndex = elementCount - 1 - entryIndex;

            // Adjust for drop indicator
            if (forDropIndicator && uiIndex % 2 == 0) {
                // If indicator is at TOP of an entry, element should go ABOVE it (one index higher)
                return elementIndex + 1;
            }

            return elementIndex;
        }
    }

    /**
     * Completes the drag operation by moving the element to the new position
     */
    private void finishDragOperation() {

        LOGGER.info("------------------------------------- ");

        LOGGER.info("RENDER ELEMENTS BEHIND VANILLA (VANILLA ENTRY AT TOP OF SCROLL AREA LIST): " + this.editor.layout.renderElementsBehindVanilla);
        LOGGER.info("NORMAL ELEMENTS SIZE: " + this.editor.normalEditorElements.size());
        LOGGER.info("ENTRIES SIZE: " + this.scrollArea.getEntries().size());

        List<ScrollAreaEntry> normalEntries = new ArrayList<>(this.scrollArea.getEntries());
        normalEntries.removeIf(scrollAreaEntry -> !(scrollAreaEntry instanceof  LayerElementEntry));

        LOGGER.info("LAYER ENTRIES SIZE: " + normalEntries.size());
        LOGGER.info("DRAG TARGET INDEX: " + dragTargetIndex);
        if (draggedEntry instanceof LayerElementEntry l) {
            LOGGER.info("DRAGGED LAYER CURRENT ELEMENT INDEX: " + this.editor.normalEditorElements.indexOf(l.element));
        }

        if (!(draggedEntry instanceof LayerElementEntry layerEntry)) return;

        // Get the current index of the dragged entry in UI
        int currentUiIndex = this.scrollArea.getEntries().indexOf(draggedEntry);
        if (currentUiIndex < 0) {
            LOGGER.info("currentUiIndex < 0");
            return;
        }

        // Skip if no change
        if (dragTargetIndex == currentUiIndex) {
            LOGGER.info("no change!!!!!!!!!!");
            return;
        }

        // Get source element index
        int sourceElementIndex = getElementIndexFromUIIndex(currentUiIndex, false);

        LOGGER.info("sourceElementIndex: " + sourceElementIndex);

        // Determine if indicator is at top or bottom of an entry
        boolean isIndicatorAtTop = (dragTargetIndex < this.scrollArea.getEntries().size() &&
                dragTargetIndex % 2 == 0);

        // Get target element index with appropriate adjustment
        int targetElementIndex = getElementIndexFromUIIndex(dragTargetIndex, true);

        LOGGER.info("targetElementIndex: " + targetElementIndex);

        // Ensure valid indices
        if (sourceElementIndex < 0 || targetElementIndex < 0 ||
                targetElementIndex > this.editor.normalEditorElements.size() ||
                sourceElementIndex == targetElementIndex) {
            return;
        }

        LOGGER.info("SETTING NEW LAYER POSITION!!!");

        // Save history before modifying layout
        this.editor.history.saveSnapshot();

        // Move the element in the editor's element list
        AbstractEditorElement elementToMove = layerEntry.element;
        this.editor.moveLayerToPosition(elementToMove, targetElementIndex);

        LOGGER.info("DRAGGED LAYER NEW ELEMENT INDEX: " + this.editor.normalEditorElements.indexOf(elementToMove));

        // Refresh the UI
        MainThreadTaskExecutor.executeInMainThread(() -> {
            this.updateList(true);
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

    }

    @Override
    protected boolean mouseScrolledComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, double scrollDeltaX, double scrollDeltaY) {
        if (super.mouseScrolledComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, scrollDeltaX, scrollDeltaY)) return true;
        return this.scrollArea.mouseScrolled(realMouseX, realMouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public void editorElementAdded(@NotNull AbstractEditorElement element) {
        this.updateList(false);
    }

    @Override
    public void editorElementRemovedOrHidden(@NotNull AbstractEditorElement element) {
        this.updateList(false);
    }

    @Override
    public void editorElementOrderChanged(@NotNull AbstractEditorElement element, boolean movedUp) {
        this.updateList(false);
    }

    public static class LayerElementEntry extends ScrollAreaEntry {

        protected static final ResourceLocation MOVE_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_up.png");
        protected static final ResourceLocation MOVE_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_down.png");

        protected AbstractEditorElement element;
        protected LayerLayoutEditorWidgetWorkingCopy layerWidget;
        protected boolean moveUpButtonHovered = false;
        protected boolean moveDownButtonHovered = false;
        protected Font font = Minecraft.getInstance().font;
        protected ExtendedEditBox editLayerNameBox;
        protected boolean displayEditLayerNameBox = false;
        protected boolean layerNameHovered = false;
        protected long lastLeftClick = -1L;
        protected boolean dragStarted = false; // Flag to track if drag has been initiated
        protected double dragStartX;
        protected double dragStartY;
        private static final int DRAG_THRESHOLD = 3; // Minimum pixels to move before initiating drag

        public LayerElementEntry(ScrollArea parent, LayerLayoutEditorWidgetWorkingCopy layerWidget, @NotNull AbstractEditorElement element) {
            super(parent, 50, 28);
            this.element = element;
            this.layerWidget = layerWidget;
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.editLayerNameBox = new ExtendedEditBox(this.font, 0, 0, 0, 0, Component.empty()) {
                @Override
                public boolean keyPressed(int keycode, int scancode, int modifiers) {
                    if (this.isVisible() && LayerElementEntry.this.displayEditLayerNameBox) {
                        if (keycode == InputConstants.KEY_ENTER) {
                            LayerElementEntry.this.stopEditingLayerName();
                            return true;
                        }
                    }
                    return super.keyPressed(keycode, scancode, modifiers);
                }
            };
            this.editLayerNameBox.setVisible(false);
            this.editLayerNameBox.setMaxLength(10000);
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.moveUpButtonHovered = this.isMoveUpButtonMouseOver(mouseX, mouseY);
            this.moveDownButtonHovered = this.isMoveDownButtonMouseOver(mouseX, mouseY);
            this.layerNameHovered = this.isLayerNameMouseOver(mouseX, mouseY);

            RenderSystem.enableBlend();

            if (this.element.isSelected() || this.element.isMultiSelected()) {
                fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), UIBase.getUIColorTheme().element_background_color_hover.getColorInt());
            }

            blitF(graphics, RenderType::guiTextured, MOVE_UP_TEXTURE, this.x, this.y, 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight(), UIBase.getUIColorTheme().ui_texture_color.getColorIntWithAlpha(this.layerWidget.editor.canMoveLayerUp(this.element) ? 1.0f : 0.3f));

            blitF(graphics, RenderType::guiTextured, MOVE_DOWN_TEXTURE, this.x, this.y + this.getButtonHeight(), 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight(), UIBase.getUIColorTheme().ui_texture_color.getColorIntWithAlpha(this.layerWidget.editor.canMoveLayerUp(this.element) ? 1.0f : 0.3f));

            if (!this.displayEditLayerNameBox) {
                UIBase.drawElementLabel(graphics, this.font, Component.literal(this.getLayerName()), (int)this.getLayerNameX(), (int)this.getLayerNameY());
            } else {
                UIBase.applyDefaultWidgetSkinTo(this.editLayerNameBox);
                this.editLayerNameBox.setX((int)this.getLayerNameX());
                this.editLayerNameBox.setY((int)this.getLayerNameY() - 1);
                this.editLayerNameBox.setWidth((int) Math.min(this.getMaxLayerNameWidth(), this.font.width(this.editLayerNameBox.getValue() + 13)));
                if (this.editLayerNameBox.getWidth() < this.getMaxLayerNameWidth()) {
                    this.editLayerNameBox.setDisplayPosition(0);
                }
                ((IMixinAbstractWidget)this.editLayerNameBox).setHeightFancyMenu(this.font.lineHeight + 2);
                this.editLayerNameBox.render(graphics, mouseX, mouseY, partial);
            }

            // If this entry is being dragged, add a visual effect
            if (this.layerWidget.draggedEntry == this) {
                // Add a subtle highlight or border to indicate this entry is being dragged
                graphics.fill(
                        RenderType.guiOverlay(),
                        (int)this.x,
                        (int)this.y,
                        (int)(this.x + this.getWidth()),
                        (int)(this.y + this.getHeight()),
                        0x40FFFFFF  // Semi-transparent white overlay
                );
            }
        }

        protected void startEditingLayerName() {
            this.editLayerNameBox.setVisible(true);
            this.editLayerNameBox.setFocused(true);
            this.editLayerNameBox.setValue(this.getLayerName());
            this.editLayerNameBox.moveCursorToEnd(false);
            this.displayEditLayerNameBox = true;
        }

        protected void stopEditingLayerName() {
            if (this.displayEditLayerNameBox) {
                String oldLayerName = this.getLayerName();
                this.element.element.customElementLayerName = this.editLayerNameBox.getValue();
                if (Objects.equals(oldLayerName, this.element.element.customElementLayerName)) this.element.element.customElementLayerName = null;
                if ((this.element.element.customElementLayerName != null) && this.element.element.customElementLayerName.replace(" ", "").isEmpty()) this.element.element.customElementLayerName = null;
            }
            this.editLayerNameBox.setFocused(false);
            this.editLayerNameBox.setVisible(false);
            this.displayEditLayerNameBox = false;
        }

        @SuppressWarnings("all")
        public String getLayerName() {
            if (this.element.element.customElementLayerName != null) return this.element.element.customElementLayerName;
            return this.element.element.builder.getDisplayName(this.element.element).getString();
        }

        public float getLayerNameX() {
            return this.getX() + this.getButtonWidth() + 3f;
        }

        public float getLayerNameY() {
            return this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f);
        }

        public float getMaxLayerNameWidth() {
            return (this.getX() + this.getWidth() - 3f) - this.getLayerNameX();
        }

        public boolean isMoveUpButtonHovered() {
            return this.moveUpButtonHovered;
        }

        public boolean isMoveDownButtonHovered() {
            return this.moveDownButtonHovered;
        }

        public boolean isLayerNameHovered() {
            return this.layerNameHovered;
        }

        public boolean isMoveUpButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return isXYInArea(mouseX, mouseY, this.x, this.y, this.getButtonWidth(), this.getButtonHeight());
        }

        public boolean isMoveDownButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return isXYInArea(mouseX, mouseY, this.x, this.y + this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight());
        }

        public boolean isLayerNameMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return isXYInArea(mouseX, mouseY, this.getLayerNameX(), this.getLayerNameY(), this.getMaxLayerNameWidth(), this.font.lineHeight);
        }

        public float getButtonHeight() {
            return 14f;
        }

        public float getButtonWidth() {
            return 30f;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (this.isMouseOver(mouseX, mouseY) && !this.moveUpButtonHovered && !this.moveDownButtonHovered) {
                    // Store initial position for drag threshold checking
                    this.dragStartX = mouseX;
                    this.dragStartY = mouseY;
                    this.dragStarted = true;
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        public boolean layerMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == 0 && this.dragStarted) {
                // Check if we've dragged past the threshold to start a real drag operation
                double deltaX = mouseX - this.dragStartX;
                double deltaY = mouseY - this.dragStartY;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                if (distance > DRAG_THRESHOLD && !this.layerWidget.isDragging) {
                    // Start drag operation
                    this.layerWidget.draggedEntry = this;
                    this.layerWidget.isDragging = true;

                    // Important: Set initial target index to the same as the dragged entry
                    // This ensures no jump at the start
                    int currentIndex = this.parent.getEntries().indexOf(this);
                    this.layerWidget.dragTargetIndex = currentIndex;

                    return true;
                }
                return true;
            }
            return false;
        }

        public boolean layerMouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) {
                // If we haven't started a real drag, handle as a regular click
                if (this.dragStarted && !this.layerWidget.isDragging) {
                    this.onClick(this, mouseX, mouseY, button);
                }
                this.dragStarted = false;
            }
            return false;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (this.isMoveUpButtonHovered()) {
                    if (this.layerWidget.editor.canMoveLayerUp(this.element)) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.layerWidget.editor.history.saveSnapshot();
                        if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                        this.element.setSelected(true);
                        this.layerWidget.editor.moveLayerUp(this.element);
                        this.layerWidget.getAllWidgetsExceptThis().forEach(widget -> widget.editorElementOrderChanged(this.element, true));
                        MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    }
                } else if (this.isMoveDownButtonHovered()) {
                    if (this.layerWidget.editor.canMoveLayerDown(this.element)) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.layerWidget.editor.history.saveSnapshot();
                        if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                        this.element.setSelected(true);
                        this.layerWidget.editor.moveLayerDown(this.element);
                        this.layerWidget.getAllWidgetsExceptThis().forEach(widget -> widget.editorElementOrderChanged(this.element, false));
                        MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    }
                } else {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    if (!Screen.hasControlDown()) this.layerWidget.editor.deselectAllElements();
                    this.element.setSelected(!this.element.isSelected());
                    if (this.isLayerNameHovered()) {
                        long now = System.currentTimeMillis();
                        if ((this.lastLeftClick + 400) > now) {
                            this.startEditingLayerName();
                        }
                        this.lastLeftClick = now;
                    }
                }
            }
            if (button == 1) {
                if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                this.element.setSelected(true);
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.editor.openElementContextMenuAtMouseIfPossible(), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        }

    }

    public static class VanillaLayerElementEntry extends ScrollAreaEntry {

        protected static final ResourceLocation MOVE_TO_TOP_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_top.png");
        protected static final ResourceLocation MOVE_BEHIND_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/layout_editor/widgets/layers/move_bottom.png");

        protected LayerLayoutEditorWidgetWorkingCopy layerWidget;
        protected boolean moveTopBottomButtonHovered = false;
        protected Font font = Minecraft.getInstance().font;

        public VanillaLayerElementEntry(ScrollArea parent, LayerLayoutEditorWidgetWorkingCopy layerWidget) {
            super(parent, 50, 28);
            this.layerWidget = layerWidget;
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.moveTopBottomButtonHovered = this.isMoveTopBottomButtonHovered(mouseX, mouseY);

            RenderSystem.enableBlend();

            ResourceLocation loc = this.layerWidget.editor.layout.renderElementsBehindVanilla ? MOVE_BEHIND_TEXTURE : MOVE_TO_TOP_TEXTURE;
            blitF(graphics, RenderType::guiTextured, loc, this.x, this.y, 0.0F, 0.0F, this.getButtonWidth(), this.getButtonHeight(), this.getButtonWidth(), this.getButtonHeight(), UIBase.getUIColorTheme().ui_texture_color.getColorInt());

            UIBase.drawElementLabel(graphics, this.font, Component.translatable("fancymenu.editor.widgets.layers.vanilla_elements").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())), (int)(this.getX() + this.getButtonWidth() + 3f), (int)(this.getY() + (this.getHeight() / 2f) - (this.font.lineHeight / 2f)));

        }

        public boolean isMoveTopBottomButtonHovered() {
            return this.moveTopBottomButtonHovered;
        }

        public boolean isMoveTopBottomButtonHovered(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return isXYInArea(mouseX, mouseY, this.x, this.y, this.getButtonWidth(), this.getButtonHeight());
        }

        public float getButtonHeight() {
            return 28f;
        }

        public float getButtonWidth() {
            return 30f;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (this.isMoveTopBottomButtonHovered()) {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.layerWidget.editor.history.saveSnapshot();
                    this.layerWidget.editor.layout.renderElementsBehindVanilla = !this.layerWidget.editor.layout.renderElementsBehindVanilla;
                    this.layerWidget.editor.deselectAllElements();
                    MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            }
        }
    }

    public static class SeparatorEntry extends ScrollAreaEntry {

        public SeparatorEntry(ScrollArea parent) {
            super(parent, 50f, 1f);
            this.selectable = false;
            this.selectOnClick = false;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), UIBase.getUIColorTheme().element_border_color_normal.getColorInt());
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }
    }
}