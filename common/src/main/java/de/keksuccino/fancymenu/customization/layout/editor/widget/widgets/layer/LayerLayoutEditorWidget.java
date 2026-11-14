package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.mojang.blaze3d.platform.InputConstants;
import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorHistory;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.UIIconButton;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class LayerLayoutEditorWidget extends AbstractLayoutEditorWidget {

    protected ScrollArea scrollArea;

    // Added fields for drag and drop functionality
    protected ScrollAreaEntry draggedEntry = null;
    /** If the dragged layers have somewhere to go where the mouse is. */
    private boolean dragTargetValid = false;
    /** The container the dragged layers get dropped into, or NULL if they get dropped into the main layout. */
    @Nullable
    private AbstractEditorElement<?, ?> dragTargetContainer = null;
    /** The layer the dragged ones get dropped next to, or NULL if they get dropped at the top or the bottom of everything in what they get dropped into. */
    @Nullable
    private AbstractEditorElement<?, ?> dragTargetReference = null;
    /** If the dragged layers end up above the reference, or at the top if there is none. Otherwise they end up below it, or at the bottom. */
    private boolean dragTargetAbove = false;
    private float dragTargetIndicatorY = Float.NaN;
    private float dragTargetIndicatorIndent = 0.0F;
    @Nullable
    private Layout.LayerGroup dragTargetGroup = null;
    private boolean isDragging = false;
    private static final int DROP_INDICATOR_THICKNESS = 3;
    private static final int TITLE_BAR_ICON_BASE_SIZE = 8;
    private static final MaterialIcon TITLE_BAR_HIDE_ICON = MaterialIcons.CLOSE;
    private static final MaterialIcon TITLE_BAR_EXPAND_ICON = MaterialIcons.EXPAND_MORE;
    private static final MaterialIcon TITLE_BAR_COLLAPSE_ICON = MaterialIcons.EXPAND_LESS;
    private static final MaterialIcon GROUP_EXPAND_ICON = MaterialIcons.EXPAND_MORE;
    private static final MaterialIcon GROUP_COLLAPSE_ICON = MaterialIcons.EXPAND_LESS;
    private static final MaterialIcon LAYER_ICON = MaterialIcons.LAYERS;
    private static final MaterialIcon GROUP_ICON = MaterialIcons.FOLDER;
    private static final MaterialIcon VANILLA_ICON = MaterialIcons.CONTEXTUAL_TOKEN;
    private static final MaterialIcon EYE_ICON = MaterialIcons.VISIBILITY;
    private static final MaterialIcon MOVE_TO_TOP_ICON = MaterialIcons.VERTICAL_ALIGN_TOP;
    private static final MaterialIcon MOVE_BEHIND_ICON = MaterialIcons.VERTICAL_ALIGN_BOTTOM;
    private static final float LAYER_ENTRY_ICON_PADDING = 6.0f;
    private static final float LAYER_ICON_TEXT_GAP = 4.0f;
    private static final float RIGHT_SIDE_BUTTON_OFFSET = 7.0f;
    private static final float GROUP_INDENT = 12.0f;
    private static final float GROUP_NAME_LEFT_PADDING = 0.0f;
    private static final float ENTRY_ICON_BUTTON_SHRINK = 2.0f;
    private static final float LAYER_EYE_BUTTON_WIDTH = 30.0f - (ENTRY_ICON_BUTTON_SHRINK * 2.0f);
    private static final float LAYER_EYE_BUTTON_HEIGHT = 28.0f - (ENTRY_ICON_BUTTON_SHRINK * 2.0f);
    private static final float GROUP_COLLAPSE_BUTTON_WIDTH = 24.0f - (ENTRY_ICON_BUTTON_SHRINK * 2.0f);
    private static final float GROUP_EYE_BUTTON_WIDTH = 30.0f - (ENTRY_ICON_BUTTON_SHRINK * 2.0f);
    private static final float GROUP_BUTTON_HEIGHT = 28.0f - (ENTRY_ICON_BUTTON_SHRINK * 2.0f);
    private static final float VANILLA_MOVE_BUTTON_WIDTH = 30.0f - (ENTRY_ICON_BUTTON_SHRINK * 2.0f);
    private static final float VANILLA_MOVE_BUTTON_HEIGHT = 28.0f - (ENTRY_ICON_BUTTON_SHRINK * 2.0f);

    public LayerLayoutEditorWidget(LayoutEditorScreen editor, AbstractLayoutEditorWidgetBuilder<?> builder) {

        super(editor, builder);

        this.displayLabel = Component.translatable("fancymenu.editor.widgets.layers");

        this.scrollArea = new ScrollArea(0, 0, 0, 0) {
            @Override
            public void updateScrollArea() {
                int grabberOffset = 5;
                boolean verticalVisible = this.verticalScrollBar.active && (this.getTotalScrollHeight() > 0.0F);
                boolean horizontalVisible = this.horizontalScrollBar.active && (this.getTotalScrollWidth() > 0.0F);
                float horizontalReserve = horizontalVisible ? this.horizontalScrollBar.grabberHeight : 0.0F;
                float verticalReserve = verticalVisible ? this.verticalScrollBar.grabberWidth : 0.0F;
                this.verticalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.verticalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.verticalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - grabberOffset;
                this.verticalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - horizontalReserve - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.horizontalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.horizontalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - verticalReserve - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - grabberOffset;
            }
        };

        this.scrollArea.backgroundColor = () -> null;
        this.scrollArea.borderColor = () -> null;
        this.scrollArea.setBorderThickness(0.0F);
        this.scrollArea.setScissorEnabled(false);
        this.scrollArea.setRenderOnlyEntriesInArea(true);
        this.scrollArea.setSetupForBlurInterface(true);
        this.scrollArea.setRoundedStyleEnabled(true);

        this.updateList(false);

    }

    @Override
    protected void init() {
        this.children.clear();
        this.titleBarButtons.clear();

        this.addTitleBarButton(new MaterialTitleBarButton(this, button -> TITLE_BAR_HIDE_ICON, button -> {
            this.setVisible(false);
        }));

        this.addTitleBarButton(new MaterialTitleBarButton(this, button -> this.isExpanded() ? TITLE_BAR_COLLAPSE_ICON : TITLE_BAR_EXPAND_ICON, button -> {
            this.setExpanded(!this.isExpanded());
        }));
    }

    @Override
    public void refresh() {
        super.refresh();
        this.updateList(false);
    }

    public void updateList(boolean keepScroll) {
        float scrollOffsetY = 0.0F;
        if (keepScroll) {
            float totalScrollHeight = this.scrollArea.getTotalScrollHeight();
            if (totalScrollHeight > 0.0F) {
                scrollOffsetY = totalScrollHeight * this.scrollArea.verticalScrollBar.getScroll();
            }
        }
        this.scrollArea.clearEntries();
        if (this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        Set<Layout.LayerGroup> handledGroups = new HashSet<>();
        for (AbstractEditorElement<?, ?> e : Lists.reverse(new ArrayList<>(this.editor.normalEditorElements))) {
            // What is in a container shows under the layer of its container
            if (e.element.getParentContainer() != null) continue;
            Layout.LayerGroup group = this.editor.getLayerGroupForElement(e);
            if ((group != null) && !handledGroups.contains(group)) {
                LayerGroupEntry groupEntry = new LayerGroupEntry(this.scrollArea, this, group);
                this.scrollArea.addEntry(groupEntry);
                this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
                handledGroups.add(group);
            }
            if (group != null && group.collapsed) {
                continue;
            }
            this.addLayerEntries(e, group, 0);
        }
        for (Layout.LayerGroup group : this.editor.layout.layerGroups) {
            if (!handledGroups.contains(group)) {
                LayerGroupEntry groupEntry = new LayerGroupEntry(this.scrollArea, this, group);
                this.scrollArea.addEntry(groupEntry);
                this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
            }
        }
        if (!this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        if (keepScroll) {
            float totalScrollHeight = this.scrollArea.getTotalScrollHeight();
            if (totalScrollHeight > 0.0F) {
                this.scrollArea.verticalScrollBar.setScroll(scrollOffsetY / totalScrollHeight);
            } else {
                this.scrollArea.verticalScrollBar.setScroll(0.0F);
            }
        }

        // Reset drag state when list is updated
        this.draggedEntry = null;
        this.resetDragTarget();
        this.isDragging = false;
    }

    /**
     * Adds the layer of the element, and under it the layers of everything that is in the element, unless it shows collapsed.
     * The topmost child comes first, like the topmost element of the layout does.
     *
     * @param depth How many containers the element is in.
     */
    private void addLayerEntries(@NotNull AbstractEditorElement<?, ?> element, @Nullable Layout.LayerGroup group, int depth) {
        this.scrollArea.addEntry(new LayerElementEntry(this.scrollArea, this, element, group, depth));
        this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        if (!(element instanceof ContainerEditorElement container) || !(element.element instanceof ElementContainer elementContainer) || elementContainer.isCollapsedInLayerList()) return;
        List<AbstractEditorElement<?, ?>> children = container.getChildEditorElements();
        for (int i = children.size() - 1; i >= 0; i--) {
            this.addLayerEntries(children.get(i), group, depth + 1);
        }
    }

    private void resetDragTarget() {
        this.dragTargetValid = false;
        this.dragTargetContainer = null;
        this.dragTargetReference = null;
        this.dragTargetAbove = false;
        this.dragTargetIndicatorY = Float.NaN;
        this.dragTargetIndicatorIndent = 0.0F;
        this.dragTargetGroup = null;
    }

    @Override
    protected void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.scrollArea.setX(0);
        this.scrollArea.setY(0);
        this.scrollArea.setWidth(this.getBodyWidth());
        this.scrollArea.setHeight(this.getBodyHeight());
        this.scrollArea.horizontalScrollBar.active = false;
        this.scrollArea.makeEntriesWidthOfArea = true;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;

        this.scrollArea.extractRenderState(graphics, mouseX, mouseY, partial);

        // Render the drop indicator if currently dragging
        if (isDragging && dragTargetValid && Float.isFinite(dragTargetIndicatorY)) {
            float indicatorY = dragTargetIndicatorY;
            // The line starts where the layers start it would put the dragged ones between, which shows what they would end up in
            UIBase.fillF(graphics, this.scrollArea.getInnerX() + this.dragTargetIndicatorIndent, indicatorY - DROP_INDICATOR_THICKNESS / 2f,
                    this.scrollArea.getInnerX() + this.scrollArea.getInnerWidth(),
                    indicatorY + DROP_INDICATOR_THICKNESS / 2f,
                    UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_widget_border_color.getColorInt() : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt());
        }

    }

    @Override
    protected @Nullable ResizingEdge updateHoveredResizingEdge(double localMouseX, double localMouseY) {
        if (this.scrollArea.isMouseInteractingWithGrabbers()) return null;
        return super.updateHoveredResizingEdge(localMouseX, localMouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isVisible()) {
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseClickedBody(double mouseX, double mouseY, int button) {
        if (super.mouseClickedBody(mouseX, mouseY, button)) return true;
        if (this.isExpanded()) {
            if (this.scrollArea.verticalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
            if (this.scrollArea.horizontalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
            for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
                if (entry.mouseClicked(mouseX, mouseY, button)) return true;
            }
            if (button == InputConstants.MOUSE_BUTTON_RIGHT && this.scrollArea.isMouseOverInnerArea(mouseX, mouseY)) {
                this.openContextMenuForBackground();
                return true;
            }
        }

        return true;

    }

    @Override
    protected boolean mouseReleasedBody(double mouseX, double mouseY, int button) {

        // Handle drop operation when mouse button is released
        if (button == InputConstants.MOUSE_BUTTON_LEFT && isDragging && dragTargetValid) {
            finishDragOperation();
        }

        // Always inform scroll bars about mouse release so they can reset their grabber state
        this.scrollArea.verticalScrollBar.mouseReleased(mouseX, mouseY, button);
        this.scrollArea.horizontalScrollBar.mouseReleased(mouseX, mouseY, button);

        // Reset drag state
        isDragging = false;
        draggedEntry = null;
        this.resetDragTarget();

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                if (l.layerMouseReleased(mouseX, mouseY, button)) return true;
            } else if (e instanceof LayerGroupEntry g) {
                if (g.groupMouseReleased(mouseX, mouseY, button)) return true;
            }
        }

        return super.mouseReleasedBody(mouseX, mouseY, button);

    }

    @Override
    protected boolean mouseDraggedBody(double mouseX, double mouseY, int button, double d1, double d2) {

        // Give scroll bars a chance to handle dragging their grabbers
        if (this.scrollArea.verticalScrollBar.mouseDragged(mouseX, mouseY, button, d1, d2) ||
                this.scrollArea.horizontalScrollBar.mouseDragged(mouseX, mouseY, button, d1, d2)) {
            return true;
        }

        if (isDragging && button == InputConstants.MOUSE_BUTTON_LEFT) {
            updateDragTarget(mouseX, mouseY);
        }

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                if (l.layerMouseDragged(mouseX, mouseY, button, d1, d2)) return true;
            } else if (e instanceof LayerGroupEntry g) {
                if (g.groupMouseDragged(mouseX, mouseY, button, d1, d2)) return true;
            }
        }

        return super.mouseDraggedBody(mouseX, mouseY, button, d1, d2);

    }

    /**
     * Works out where the dragged layers would go if the user dropped them where the mouse is.<br>
     * A layer the mouse is on takes them above or below itself, in whatever it is in. The lower half of a container takes them into the container instead.
     */
    private void updateDragTarget(double mouseX, double mouseY) {

        if (!isDragging || draggedEntry == null) return;
        this.resetDragTarget();

        List<AbstractEditorElement<?, ?>> movingElements = this.getMovingElementsForDrag();
        if (movingElements.isEmpty()) return;
        boolean groupDrag = this.draggedEntry instanceof LayerGroupEntry;
        List<LayerElementEntry> entries = this.getLayerEntriesNotMoving(movingElements);

        // Layers dropped on a group become its bottom layers
        LayerGroupEntry hoveredGroup = groupDrag ? null : this.getHoveredGroupEntry(mouseX, mouseY);
        if (hoveredGroup != null) {
            this.dragTargetGroup = hoveredGroup.group;
            for (int i = entries.size() - 1; i >= 0; i--) {
                LayerElementEntry entry = entries.get(i);
                if ((entry.group == hoveredGroup.group) && (entry.depth == 0)) {
                    this.setDragTarget(null, entry.element, false, entries);
                    return;
                }
            }
            // A group without layers that show sits where its header is, which nothing can be put next to but the header itself
            this.dragTargetValid = true;
            this.dragTargetIndicatorY = hoveredGroup.getY() + hoveredGroup.getHeight();
            this.dragTargetIndicatorIndent = GROUP_INDENT;
            List<AbstractEditorElement<?, ?>> groupElements = this.editor.getElementsInGroup(hoveredGroup.group);
            groupElements.removeAll(movingElements);
            if (!groupElements.isEmpty()) this.dragTargetReference = groupElements.get(0);
            return;
        }

        if (entries.isEmpty() || (mouseY < this.scrollArea.getInnerY())) {
            this.setDragTarget(null, null, true, entries);
            return;
        }
        if (mouseY > (this.scrollArea.getInnerY() + this.scrollArea.getInnerHeight())) {
            this.setDragTarget(null, null, false, entries);
            return;
        }

        // Below the last layer is below all layers, also when the last one is in a container: the lowest layer of the main layout
        LayerElementEntry lastEntry = entries.get(entries.size() - 1);
        if (mouseY >= (lastEntry.getY() + lastEntry.getHeight())) {
            this.setDragTarget(null, null, false, entries);
            return;
        }

        // The layer the mouse is on, or the one it is in front of when it is between two layers
        LayerElementEntry target = lastEntry;
        for (LayerElementEntry entry : entries) {
            if (mouseY < (entry.getY() + entry.getHeight())) {
                target = entry;
                break;
            }
        }
        boolean upperHalf = mouseY < (target.getY() + (target.getHeight() / 2.0F));

        if (groupDrag) {
            // Groups hold layers of the main layout, so a group goes next to one of those and never into a container
            AbstractEditorElement<?, ?> root = this.getRootOf(target.element);
            boolean above = (root == target.element) && upperHalf;
            Layout.LayerGroup rootGroup = this.editor.getLayerGroupForElement(root);
            if (rootGroup != null) {
                // Next to another group means next to all of it, which keeps the layers of both groups together
                List<AbstractEditorElement<?, ?>> groupElements = this.editor.getElementsInGroup(rootGroup);
                if (!groupElements.isEmpty()) root = above ? groupElements.get(groupElements.size() - 1) : groupElements.get(0);
            }
            this.setDragTarget(null, root, above, entries);
            return;
        }

        if (!upperHalf && (target.element instanceof ContainerEditorElement)) {
            this.setDragTarget(target.element, null, true, entries);
        } else {
            this.setDragTarget(this.getContainerOf(target.element), target.element, upperHalf, entries);
        }
        if (this.dragTargetContainer == null) this.dragTargetGroup = target.group;

    }

    /**
     * Makes the given place the one the dragged layers would go to, if all of them can go there, and works out where the line shows that marks it.
     *
     * @param entries The layers that are not getting dragged, in the order they show in.
     */
    private void setDragTarget(@Nullable AbstractEditorElement<?, ?> container, @Nullable AbstractEditorElement<?, ?> reference, boolean above, @NotNull List<LayerElementEntry> entries) {
        if (container instanceof ContainerEditorElement containerEditorElement) {
            for (AbstractEditorElement<?, ?> moving : this.getMovingElementsForDrag()) {
                if (!containerEditorElement.canHoldChild(moving)) return;
            }
        }
        this.dragTargetValid = true;
        this.dragTargetContainer = container;
        this.dragTargetReference = reference;
        this.dragTargetAbove = above;
        LayerElementEntry first = entries.isEmpty() ? null : entries.get(0);
        LayerElementEntry last = entries.isEmpty() ? null : entries.get(entries.size() - 1);
        if (reference != null) {
            LayerElementEntry referenceEntry = null;
            LayerElementEntry lastOfReference = null;
            for (LayerElementEntry entry : entries) {
                if (entry.element == reference) referenceEntry = entry;
                // What is in the reference shows under it, and what goes below the reference goes below all of that
                if ((entry.element == reference) || ((reference.element instanceof ElementContainer referenceContainer) && ElementContainerHierarchy.isInContainer(entry.element.element, referenceContainer))) lastOfReference = entry;
            }
            if ((referenceEntry == null) || (lastOfReference == null)) return;
            this.dragTargetIndicatorY = above ? referenceEntry.getY() : (lastOfReference.getY() + lastOfReference.getHeight());
            this.dragTargetIndicatorIndent = referenceEntry.getIndentOffset();
        } else if (container != null) {
            for (LayerElementEntry entry : entries) {
                if (entry.element != container) continue;
                this.dragTargetIndicatorY = entry.getY() + entry.getHeight();
                this.dragTargetIndicatorIndent = entry.getIndentOffset() + GROUP_INDENT;
            }
        } else if (above) {
            this.dragTargetIndicatorY = (first != null) ? first.getY() : this.scrollArea.getInnerY();
        } else {
            this.dragTargetIndicatorY = (last != null) ? (last.getY() + last.getHeight()) : this.scrollArea.getInnerY();
        }
    }

    /**
     * Returns the layers that are not getting dragged, in the order they show in: neither the dragged ones nor what is in them.
     */
    @NotNull
    private List<LayerElementEntry> getLayerEntriesNotMoving(@NotNull List<AbstractEditorElement<?, ?>> movingElements) {
        List<LayerElementEntry> entries = new ArrayList<>();
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (!(entry instanceof LayerElementEntry layerEntry)) continue;
            if (movingElements.contains(layerEntry.element) || this.editor.containerDragDropHandler.movesWithDraggedContainer(layerEntry.element, movingElements)) continue;
            entries.add(layerEntry);
        }
        return entries;
    }

    @Nullable
    private AbstractEditorElement<?, ?> getContainerOf(@NotNull AbstractEditorElement<?, ?> element) {
        ElementContainer container = element.element.getParentContainer();
        return (container != null) ? this.editor.getElementByInstanceIdentifier(container.getContainerElement().getInstanceIdentifier()) : null;
    }

    /**
     * Returns the element of the main layout the given one is in, or the given one if it is one itself.
     */
    @NotNull
    private AbstractEditorElement<?, ?> getRootOf(@NotNull AbstractEditorElement<?, ?> element) {
        AbstractEditorElement<?, ?> root = element;
        for (AbstractEditorElement<?, ?> container = this.getContainerOf(root); container != null; container = this.getContainerOf(root)) {
            root = container;
        }
        return root;
    }

    @Nullable
    private LayerGroupEntry getHoveredGroupEntry(double mouseX, double mouseY) {
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry instanceof LayerGroupEntry groupEntry) {
                if (groupEntry.isMouseOver(mouseX, mouseY)) {
                    return groupEntry;
                }
            }
        }
        return null;
    }

    /**
     * Returns the layers the user drags: the ones of the dragged group, or the selected ones. Layers that are in another dragged
     * layer are not among them, because they move along with it.
     */
    @NotNull
    private List<AbstractEditorElement<?, ?>> getMovingElementsForDrag() {
        List<AbstractEditorElement<?, ?>> movingElements = new ArrayList<>();
        if (this.draggedEntry instanceof LayerGroupEntry groupEntry) {
            movingElements.addAll(this.editor.getElementsInGroup(groupEntry.group));
            return movingElements;
        }
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry instanceof LayerElementEntry layerElement) {
                if (layerElement.element.isSelected()) {
                    movingElements.add(layerElement.element);
                }
            }
        }
        if (movingElements.isEmpty() && this.draggedEntry instanceof LayerElementEntry layerEntry) {
            movingElements.add(layerEntry.element);
        }
        List<AbstractEditorElement<?, ?>> selected = new ArrayList<>(movingElements);
        movingElements.removeIf(element -> this.editor.containerDragDropHandler.movesWithDraggedContainer(element, selected));
        return movingElements;
    }

    private float resolveScrollAreaBottomRadius() {
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        if (radius <= 0.0F) {
            return 0.0F;
        }
        float innerWidth = this.scrollArea.getInnerWidth();
        float innerHeight = this.scrollArea.getInnerHeight();
        float maxRadius = Math.min(innerWidth, innerHeight) * 0.5F;
        if (maxRadius <= 0.0F) {
            return 0.0F;
        }
        return Math.min(radius, maxRadius);
    }

    private float getBottomCornerInset(float y, float areaBottom, float radius) {
        float roundingStart = areaBottom - radius;
        if (y <= roundingStart) {
            return 0.0F;
        }
        float dy = y - roundingStart;
        if (dy >= radius) {
            return radius;
        }
        float inside = (radius * radius) - (dy * dy);
        if (inside <= 0.0F) {
            return radius;
        }
        return radius - (float) Math.sqrt(inside);
    }

    private void fillClippedToRoundedBottom(@NotNull GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float innerX = this.scrollArea.getInnerX();
        float innerY = this.scrollArea.getInnerY();
        float innerWidth = this.scrollArea.getInnerWidth();
        float innerHeight = this.scrollArea.getInnerHeight();
        float left = Math.max(x, innerX);
        float right = Math.min(x + width, innerX + innerWidth);
        if (right <= left) {
            return;
        }
        if (!this.scrollArea.isRoundedStyle()) {
            UIBase.fillF(graphics, left, y, right, y + height, color);
            return;
        }
        float radius = this.resolveScrollAreaBottomRadius();
        if (radius <= 0.0F) {
            UIBase.fillF(graphics, left, y, right, y + height, color);
            return;
        }
        float areaBottom = innerY + innerHeight;
        float roundingStart = areaBottom - radius;
        float fullTop = y;
        float fullBottom = Math.min(y + height, roundingStart);
        if (fullBottom > fullTop) {
            UIBase.fillF(graphics, left, fullTop, right, fullBottom, color);
        }
        float roundedTop = Math.max(y, roundingStart);
        float roundedBottom = y + height;
        if (roundedBottom <= roundedTop) {
            return;
        }
        int yStart = (int) Math.floor(roundedTop);
        int yEnd = (int) Math.ceil(roundedBottom);
        for (int yi = yStart; yi < yEnd; yi++) {
            float lineTop = Math.max(roundedTop, yi);
            float lineBottom = Math.min(roundedBottom, yi + 1.0F);
            if (lineBottom <= lineTop) {
                continue;
            }
            float lineCenter = (lineTop + lineBottom) * 0.5F;
            float inset = this.getBottomCornerInset(lineCenter, areaBottom, radius);
            float lineLeft = left + inset;
            float lineRight = right - inset;
            if (lineRight > lineLeft) {
                UIBase.fillF(graphics, lineLeft, lineTop, lineRight, lineBottom, color);
            }
        }
    }

    private void fillClippedToRoundedBottomSmooth(@NotNull GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float innerX = this.scrollArea.getInnerX();
        float innerY = this.scrollArea.getInnerY();
        float innerWidth = this.scrollArea.getInnerWidth();
        float innerHeight = this.scrollArea.getInnerHeight();
        float left = Math.max(x, innerX);
        float right = Math.min(x + width, innerX + innerWidth);
        if (right <= left) {
            return;
        }
        float top = y;
        float bottom = y + height;
        if (!this.scrollArea.isRoundedStyle()) {
            UIBase.fillF(graphics, left, top, right, bottom, color);
            return;
        }
        float radius = this.resolveScrollAreaBottomRadius();
        if (radius <= 0.0F) {
            UIBase.fillF(graphics, left, top, right, bottom, color);
            return;
        }
        float areaBottom = innerY + innerHeight;
        float roundingStart = areaBottom - radius;
        if (bottom <= roundingStart) {
            UIBase.fillF(graphics, left, top, right, bottom, color);
            return;
        }
        graphics.enableScissor((int) Math.floor(left), (int) Math.floor(top), (int) Math.ceil(right), (int) Math.ceil(bottom));
        SmoothRectangleRenderer.renderSmoothRectRoundBottomCornersScaled(graphics, innerX, innerY, innerWidth, innerHeight, radius, color, 1.0F);
        graphics.disableScissor();
    }

    /**
     * Completes the drag operation by moving the dragged elements to the new position.
     */
    private void finishDragOperation() {

        if (!this.dragTargetValid || this.draggedEntry == null) {
            return;
        }

        List<AbstractEditorElement<?, ?>> movingElements = this.getMovingElementsForDrag();
        if (movingElements.isEmpty()) {
            return;
        }

        List<AbstractEditorElement<?, ?>> orderBefore = new ArrayList<>(this.editor.normalEditorElements);
        movingElements.sort(Comparator.comparingInt(orderBefore::indexOf));
        boolean groupDrag = this.draggedEntry instanceof LayerGroupEntry;
        LayoutEditorHistory.Snapshot snapshot = this.editor.history.createSnapshot();
        boolean changed = false;

        // Into what the layers got dropped on first, so their place in the order is one among the layers there
        ElementContainer targetContainer = (this.dragTargetContainer != null) ? (ElementContainer) this.dragTargetContainer.element : null;
        for (AbstractEditorElement<?, ?> element : movingElements) {
            if (element.element.getParentContainer() == targetContainer) continue;
            this.editor.containerOperations.moveToContainerVisibly(element, targetContainer);
            changed = true;
        }
        if (changed) this.editor.containerOperations.detachAnchorsAcrossContainers();

        List<AbstractEditorElement<?, ?>> reordered = new ArrayList<>(orderBefore);
        reordered.removeAll(movingElements);
        int targetIndex;
        if (this.dragTargetReference != null) {
            // Directly behind or in front of the layer they got dropped next to. The hierarchy puts what is in that layer between them again.
            targetIndex = reordered.indexOf(this.dragTargetReference) + (this.dragTargetAbove ? 1 : 0);
        } else {
            // The topmost layers are the last elements, also among the children of a container
            targetIndex = this.dragTargetAbove ? reordered.size() : 0;
        }
        reordered.addAll(Math.min(Math.max(targetIndex, 0), reordered.size()), movingElements);
        this.editor.normalEditorElements = reordered;

        if (!groupDrag) {
            for (AbstractEditorElement<?, ?> element : movingElements) {
                // Layers in a container belong to no group, and the hierarchy takes them out of the one they had
                Layout.LayerGroup newGroup = (targetContainer == null) ? this.dragTargetGroup : null;
                if (this.editor.getLayerGroupForElement(element) == newGroup) continue;
                changed = true;
                List<String> identifier = List.of(element.element.getInstanceIdentifier());
                if (newGroup != null) {
                    this.editor.addElementsToLayerGroup(identifier, newGroup);
                } else {
                    this.editor.removeElementsFromLayerGroups(identifier);
                }
            }
        }

        this.editor.updateContainerHierarchy();
        this.editor.updateLayerGroupElementOrder();

        if (!changed && orderBefore.equals(this.editor.normalEditorElements)) {
            this.editor.history.discardSnapshot(snapshot);
            return;
        }
        this.editor.history.saveSnapshot(snapshot);

        for (AbstractEditorElement<?, ?> element : movingElements) {
            boolean movedUp = this.editor.normalEditorElements.indexOf(element) > orderBefore.indexOf(element);
            this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(element, movedUp));
        }

        MainThreadTaskExecutor.executeInMainThread(() -> this.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

    @Override
    protected boolean mouseScrolledBody(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (super.mouseScrolledBody(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;

        // Handle scroll wheel manually to support the widget's translated coordinate system
        if (scrollDeltaY != 0.0D && this.scrollArea.isVerticalScrollBarVisible() && this.scrollArea.verticalScrollBar.isScrollWheelAllowed()) {
            boolean hoveringContent = this.scrollArea.isMouseOverInnerArea(mouseX, mouseY);
            boolean hoveringBar = this.scrollArea.verticalScrollBar.isMouseInsideScrollArea(mouseX, mouseY, true);
            if (hoveringContent || hoveringBar) {
                float scrollOffset = 0.1F * this.scrollArea.verticalScrollBar.getWheelScrollSpeed();
                if (scrollDeltaY > 0.0D) {
                    scrollOffset = -scrollOffset;
                }
                this.scrollArea.verticalScrollBar.setScroll(this.scrollArea.verticalScrollBar.getScroll() + scrollOffset);
                return true;
            }
        }

        return this.scrollArea.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
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
        this.updateList(true);
    }

    private static float getIconSize(float areaWidth, float areaHeight, float padding) {
        float size = Math.min(areaWidth, areaHeight) - (padding * 2.0f);
        return Math.max(1.0f, size);
    }

    private void openContextMenuForBackground() {
        this.openLayerContextMenu(null, null);
    }

    private void openContextMenuForLayer(@NotNull LayerElementEntry entry) {
        this.openLayerContextMenu(entry, null);
    }

    private void openContextMenuForGroup(@NotNull LayerGroupEntry entry) {
        this.openLayerContextMenu(null, entry);
    }

    private void openLayerContextMenu(@Nullable LayerElementEntry layerEntry, @Nullable LayerGroupEntry groupEntry) {
        final ContextMenu[] attachedElementSettingsMenu = new ContextMenu[1];
        ContextMenu menu = new ContextMenu() {
            private void detachElementSettingsMenu_FancyMenu() {
                if (attachedElementSettingsMenu[0] != null) {
                    attachedElementSettingsMenu[0].detachFromParentEntry();
                    attachedElementSettingsMenu[0] = null;
                }
            }

            @Override
            public ContextMenu closeMenu() {
                ContextMenu closed = super.closeMenu();
                this.detachElementSettingsMenu_FancyMenu();
                return closed;
            }

            @Override
            public ContextMenu closeMenuChain() {
                ContextMenu closed = super.closeMenuChain();
                this.detachElementSettingsMenu_FancyMenu();
                return closed;
            }
        };

        this.addAddGroupEntry(menu);

        if (layerEntry != null) {
            AbstractEditorElement<?, ?> element = layerEntry.element;
            menu.addSeparatorEntry("separator_layer");
            this.addOpenElementSettingsSubMenuEntry(menu, element, attachedElementSettingsMenu);
            this.addToggleLayerVisibilityEntry(menu, element);
            this.addRenameLayerEntry(menu, layerEntry);
            this.addMoveLayerEntries(menu, element);
        } else if (groupEntry != null) {
            menu.addSeparatorEntry("separator_group");
            this.addToggleGroupVisibilityEntry(menu, groupEntry.group);
            this.addToggleGroupCollapseEntry(menu, groupEntry.group);
            this.addRenameGroupEntry(menu, groupEntry);
            this.addMoveGroupEntries(menu, groupEntry.group);
            this.addDeleteGroupEntry(menu, groupEntry.group);
        }

        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(menu);
    }

    private void addAddGroupEntry(@NotNull ContextMenu menu) {
        menu.addClickableEntry("add_group", Component.translatable("fancymenu.editor.widgets.layers.context.add_group"), (menu1, entry) -> {
            this.editor.history.saveSnapshot();
            this.editor.layout.layerGroups.add(new Layout.LayerGroup());
            menu1.closeMenuChain();
            MainThreadTaskExecutor.executeInMainThread(() -> this.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setIcon(MaterialIcons.ADD);
    }

    private void addOpenElementSettingsSubMenuEntry(@NotNull ContextMenu menu, @NotNull AbstractEditorElement<?, ?> element, @NotNull ContextMenu[] attachedElementSettingsMenu) {
        if (!element.isSelected()) {
            this.editor.deselectAllElements();
            element.setSelected(true);
        }
        ContextMenu elementSettingsMenu = element.rightClickMenu;
        elementSettingsMenu.detachFromParentEntry();
        attachedElementSettingsMenu[0] = elementSettingsMenu;
        menu.addSubMenuEntry("open_element_settings", Component.translatable("fancymenu.editor.widgets.layers.context.element_settings"), elementSettingsMenu)
                .setIcon(MaterialIcons.SETTINGS);
    }

    private void addToggleLayerVisibilityEntry(@NotNull ContextMenu menu, @NotNull AbstractEditorElement<?, ?> element) {
        boolean hidden = element.element.layerHiddenInEditor;
        Component label = Component.translatable(hidden
                ? "fancymenu.editor.widgets.layers.context.show_layer"
                : "fancymenu.editor.widgets.layers.context.hide_layer");
        menu.addClickableEntry("toggle_layer_visibility", label, (menu1, entry) -> {
            this.editor.history.saveSnapshot();
            element.element.layerHiddenInEditor = !element.element.layerHiddenInEditor;
            menu1.closeMenu();
        }).setIcon(EYE_ICON);
    }

    private void addToggleGroupVisibilityEntry(@NotNull ContextMenu menu, @NotNull Layout.LayerGroup group) {
        boolean hidden = this.isGroupHidden(group);
        Component label = Component.translatable(hidden
                ? "fancymenu.editor.widgets.layers.context.show_group"
                : "fancymenu.editor.widgets.layers.context.hide_group");
        menu.addClickableEntry("toggle_group_visibility", label, (menu1, entry) -> {
            this.editor.history.saveSnapshot();
            this.toggleGroupVisibility(group);
            menu1.closeMenu();
        }).setIcon(EYE_ICON);
    }

    private void addToggleGroupCollapseEntry(@NotNull ContextMenu menu, @NotNull Layout.LayerGroup group) {
        Component label = Component.translatable(group.collapsed
                ? "fancymenu.editor.widgets.layers.context.expand_group"
                : "fancymenu.editor.widgets.layers.context.collapse_group");
        menu.addClickableEntry("toggle_group_collapse", label, (menu1, entry) -> {
            this.editor.history.saveSnapshot();
            group.collapsed = !group.collapsed;
            menu1.closeMenu();
            MainThreadTaskExecutor.executeInMainThread(() -> this.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setIcon(group.collapsed ? GROUP_EXPAND_ICON : GROUP_COLLAPSE_ICON);
    }

    private void addRenameLayerEntry(@NotNull ContextMenu menu, @NotNull LayerElementEntry entry) {
        menu.addClickableEntry("rename_layer", Component.translatable("fancymenu.editor.widgets.layers.context.rename_layer"), (menu1, menuEntry) -> {
            TextInputWindowBody inputScreen = new TextInputWindowBody(null, value -> this.applyLayerName(entry.element, value));
            Dialogs.openGeneric(inputScreen,
                    Component.translatable("fancymenu.editor.widgets.layers.context.rename_layer"),
                    null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                    .getSecond().setIcon(MaterialIcons.DRIVE_FILE_RENAME_OUTLINE);
            inputScreen.setText(entry.getLayerName());
            menu1.closeMenuChain();
        }).setIcon(MaterialIcons.DRIVE_FILE_RENAME_OUTLINE);
    }

    private void addRenameGroupEntry(@NotNull ContextMenu menu, @NotNull LayerGroupEntry entry) {
        menu.addClickableEntry("rename_group", Component.translatable("fancymenu.editor.widgets.layers.context.rename_group"), (menu1, menuEntry) -> {
            TextInputWindowBody inputScreen = new TextInputWindowBody(null, value -> this.applyGroupName(entry.group, value));
            Dialogs.openGeneric(inputScreen,
                    Component.translatable("fancymenu.editor.widgets.layers.context.rename_group"),
                    null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                    .getSecond().setIcon(MaterialIcons.DRIVE_FILE_RENAME_OUTLINE);
            inputScreen.setText(entry.getGroupName());
            menu1.closeMenuChain();
        }).setIcon(MaterialIcons.DRIVE_FILE_RENAME_OUTLINE);
    }

    private void addMoveLayerEntries(@NotNull ContextMenu menu, @NotNull AbstractEditorElement<?, ?> element) {
        menu.addSeparatorEntry("separator_layer_move");
        menu.addClickableEntry("move_layer_up", Component.translatable("fancymenu.editor.object.moveup"), (menu1, entry) -> {
            this.editor.moveLayerUp(element);
            this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(element, true));
            menu1.closeMenu();
        }).addIsActiveSupplier((menu1, entry) -> this.editor.canMoveLayerUp(element))
                .setIcon(MaterialIcons.ARROW_UPWARD);
        menu.addClickableEntry("move_layer_down", Component.translatable("fancymenu.editor.object.movedown"), (menu1, entry) -> {
            this.editor.moveLayerDown(element);
            this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(element, false));
            menu1.closeMenu();
        }).addIsActiveSupplier((menu1, entry) -> this.editor.canMoveLayerDown(element))
                .setIcon(MaterialIcons.ARROW_DOWNWARD);
    }

    private void addMoveGroupEntries(@NotNull ContextMenu menu, @NotNull Layout.LayerGroup group) {
        menu.addSeparatorEntry("separator_group_move");
        menu.addClickableEntry("move_group_up", Component.translatable("fancymenu.editor.object.moveup"), (menu1, entry) -> {
            if (this.moveLayerGroup(group, true)) {
                menu1.closeMenu();
            }
        }).addIsActiveSupplier((menu1, entry) -> this.canMoveLayerGroup(group, true))
                .setIcon(MaterialIcons.ARROW_UPWARD);
        menu.addClickableEntry("move_group_down", Component.translatable("fancymenu.editor.object.movedown"), (menu1, entry) -> {
            if (this.moveLayerGroup(group, false)) {
                menu1.closeMenu();
            }
        }).addIsActiveSupplier((menu1, entry) -> this.canMoveLayerGroup(group, false))
                .setIcon(MaterialIcons.ARROW_DOWNWARD);
    }

    private void addDeleteGroupEntry(@NotNull ContextMenu menu, @NotNull Layout.LayerGroup group) {
        menu.addSeparatorEntry("separator_group_delete");
        menu.addClickableEntry("delete_group", Component.translatable("fancymenu.editor.widgets.layers.context.delete_group"), (menu1, entry) -> {
            menu1.closeMenuChain();
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.editor.widgets.layers.group.delete.confirm"), MessageDialogStyle.WARNING, call -> {
                if (call) {
                    this.editor.history.saveSnapshot();
                    this.editor.layout.layerGroups.remove(group);
                    MainThreadTaskExecutor.executeInMainThread(() -> this.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            });
        }).setIcon(MaterialIcons.DELETE);
    }

    private void applyLayerName(@NotNull AbstractEditorElement<?, ?> element, @Nullable String value) {
        String oldLayerName = (element.element.customElementLayerName != null)
                ? element.element.customElementLayerName
                : element.element.getBuilder().getDisplayName(element.element).getString();
        element.element.customElementLayerName = value;
        if (Objects.equals(oldLayerName, element.element.customElementLayerName)) {
            element.element.customElementLayerName = null;
        }
        if ((element.element.customElementLayerName != null) && element.element.customElementLayerName.replace(" ", "").isEmpty()) {
            element.element.customElementLayerName = null;
        }
    }

    private void applyGroupName(@NotNull Layout.LayerGroup group, @Nullable String value) {
        if (value != null) {
            value = value.trim();
        }
        if ((value == null) || value.isEmpty()) {
            group.name = null;
        } else {
            String defaultName = Component.translatable("fancymenu.editor.widgets.layers.group.default_name").getString();
            if (value.equals(defaultName)) {
                group.name = null;
            } else {
                group.name = value;
            }
        }
    }

    private void toggleGroupVisibility(@NotNull Layout.LayerGroup group) {
        List<AbstractEditorElement<?, ?>> elements = this.editor.getElementsInGroup(group);
        if (elements.isEmpty()) {
            return;
        }
        boolean hideElements = false;
        for (AbstractEditorElement<?, ?> element : elements) {
            if (!element.element.layerHiddenInEditor) {
                hideElements = true;
                break;
            }
        }
        for (AbstractEditorElement<?, ?> element : elements) {
            element.element.layerHiddenInEditor = hideElements;
        }
    }

    private boolean isGroupHidden(@NotNull Layout.LayerGroup group) {
        List<AbstractEditorElement<?, ?>> elements = this.editor.getElementsInGroup(group);
        if (elements.isEmpty()) {
            return false;
        }
        for (AbstractEditorElement<?, ?> element : elements) {
            if (!element.element.layerHiddenInEditor) {
                return false;
            }
        }
        return true;
    }

    private boolean canMoveLayerGroup(@NotNull Layout.LayerGroup group, boolean moveUp) {
        List<AbstractEditorElement<?, ?>> groupElements = this.editor.getElementsInGroup(group);
        if (groupElements.isEmpty()) {
            return false;
        }
        Set<AbstractEditorElement<?, ?>> groupSet = new HashSet<>(groupElements);
        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;
        for (AbstractEditorElement<?, ?> element : groupElements) {
            int idx = this.editor.normalEditorElements.indexOf(element);
            if (idx >= 0) {
                minIndex = Math.min(minIndex, idx);
                maxIndex = Math.max(maxIndex, idx);
            }
        }
        if (moveUp) {
            for (int i = maxIndex + 1; i < this.editor.normalEditorElements.size(); i++) {
                if (this.canGroupMovePast(this.editor.normalEditorElements.get(i), groupSet)) {
                    return true;
                }
            }
            return false;
        }
        for (int i = minIndex - 1; i >= 0; i--) {
            if (this.canGroupMovePast(this.editor.normalEditorElements.get(i), groupSet)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns if the element is a layer a group can move past: a layer of the main layout that is not in the group.
     * What is in a container is no layer of the main layout, and moves with its container.
     */
    private boolean canGroupMovePast(@NotNull AbstractEditorElement<?, ?> element, @NotNull Set<AbstractEditorElement<?, ?>> groupSet) {
        return !groupSet.contains(element) && (element.element.getParentContainer() == null);
    }

    private boolean moveLayerGroup(@NotNull Layout.LayerGroup group, boolean moveUp) {
        List<AbstractEditorElement<?, ?>> groupElements = this.editor.getElementsInGroup(group);
        if (groupElements.isEmpty()) {
            return false;
        }
        Set<AbstractEditorElement<?, ?>> groupSet = new HashSet<>(groupElements);
        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;
        for (AbstractEditorElement<?, ?> element : groupElements) {
            int idx = this.editor.normalEditorElements.indexOf(element);
            if (idx >= 0) {
                minIndex = Math.min(minIndex, idx);
                maxIndex = Math.max(maxIndex, idx);
            }
        }
        AbstractEditorElement<?, ?> anchor = null;
        if (moveUp) {
            for (int i = maxIndex + 1; i < this.editor.normalEditorElements.size(); i++) {
                AbstractEditorElement<?, ?> candidate = this.editor.normalEditorElements.get(i);
                if (this.canGroupMovePast(candidate, groupSet)) {
                    anchor = candidate;
                    break;
                }
            }
        } else {
            for (int i = minIndex - 1; i >= 0; i--) {
                AbstractEditorElement<?, ?> candidate = this.editor.normalEditorElements.get(i);
                if (this.canGroupMovePast(candidate, groupSet)) {
                    anchor = candidate;
                    break;
                }
            }
        }
        if (anchor == null) {
            return false;
        }
        List<AbstractEditorElement<?, ?>> remaining = new ArrayList<>(this.editor.normalEditorElements);
        remaining.removeIf(groupSet::contains);
        int anchorIndex = remaining.indexOf(anchor);
        if (anchorIndex < 0) {
            return false;
        }
        int insertIndex = moveUp ? anchorIndex + 1 : anchorIndex;
        if (insertIndex < 0) {
            insertIndex = 0;
        }
        if (insertIndex > remaining.size()) {
            insertIndex = remaining.size();
        }
        remaining.addAll(insertIndex, groupElements);
        this.editor.normalEditorElements = remaining;
        // What is in the containers of the group and of the layer it moved past follows its container
        this.editor.updateContainerHierarchy();
        this.editor.updateLayerGroupElementOrder();
        for (AbstractEditorElement<?, ?> element : groupElements) {
            this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(element, moveUp));
        }
        MainThreadTaskExecutor.executeInMainThread(() -> this.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        return true;
    }

    private class MaterialTitleBarButton extends TitleBarButton {

        @NotNull
        private final ConsumingSupplier<MaterialTitleBarButton, MaterialIcon> materialIconSupplier;

        private MaterialTitleBarButton(@NotNull AbstractLayoutEditorWidget parent, @NotNull ConsumingSupplier<MaterialTitleBarButton, MaterialIcon> materialIconSupplier, @NotNull Consumer<TitleBarButton> clickAction) {
            super(parent, button -> null, clickAction);
            this.materialIconSupplier = materialIconSupplier;
        }

        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, float partial, double localMouseX, double localMouseY) {
            this.hovered = this.isMouseOver(localMouseX, localMouseY);

            this.renderHoverBackground(graphics, partial);

            MaterialIcon icon = this.materialIconSupplier.get(this);
            if (icon == null) {
                return;
            }
            float iconPadding = Math.max(0.0F, 4.0F);
            float maxIconSize = Math.max(1.0F, TITLE_BAR_ICON_BASE_SIZE);
            float iconSize = Math.max(1.0F, Math.min(this.width - iconPadding, maxIconSize));
            iconSize = Math.min(iconSize, Math.min(this.width, this.parent.getTitleBarHeight()));
            float iconX = this.x + (this.width - iconSize) * 0.5F;
            float iconY = this.y + (this.parent.getTitleBarHeight() - iconSize) * 0.5F;
            icon.render(graphics, iconX, iconY, iconSize, iconSize, UIBase.getUITheme().getUIIconColor(1.0F));
        }

        @Override
        protected void renderHoverBackground(GuiGraphicsExtractor graphics, float partial) {
            if (!this.isHovered()) {
                return;
            }
            UIBase.resetShaderColor(graphics);
            float radius = UIBase.getInterfaceCornerRoundingRadius();
            float topLeft = 0.0F;
            float topRight = 0.0F;
            float bottomRight = 0.0F;
            float bottomLeft = 0.0F;
            float rightEdge = this.parent.getBorderThickness() + this.parent.getBodyWidth();
            boolean isRightmost = Math.abs((this.x + this.width) - rightEdge) <= 0.01F;
            if (isRightmost) {
                topRight = radius;
                if (!this.parent.isExpanded()) {
                    bottomRight = radius;
                }
            }
            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                    graphics,
                    this.x,
                    this.y,
                    this.width,
                    this.parent.getTitleBarHeight(),
                    topLeft,
                    topRight,
                    bottomRight,
                    bottomLeft,
                    getElementHoverColor().getColorInt(),
                    partial
            );
            UIBase.resetShaderColor(graphics);
        }
    }

    private class LayerElementEntry extends ScrollAreaEntry {

        protected AbstractEditorElement<?, ?> element;
        protected LayerLayoutEditorWidget layerWidget;
        @Nullable
        protected Layout.LayerGroup group;
        /** How many containers the element of the layer is in, which is how far the layer is indented. */
        protected final int depth;
        protected final UIIconButton eyeButton;
        /** Shows or hides the layers of what is in the element. Only layers of containers have it. */
        @Nullable
        protected final UIIconButton collapseButton;
        protected Font font = Minecraft.getInstance().font;
        protected boolean dragStarted = false; // Flag to track if drag has been initiated
        protected double dragStartX;
        protected double dragStartY;
        private static final int DRAG_THRESHOLD = 3; // Minimum pixels to move before initiating drag

        public LayerElementEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget, @NotNull AbstractEditorElement<?, ?> element, @Nullable Layout.LayerGroup group, int depth) {
            super(parent, 50, 28);
            this.element = element;
            this.layerWidget = layerWidget;
            this.group = group;
            this.depth = depth;
            this.collapseButton = !(element.element instanceof ElementContainer container) ? null : new UIIconButton(0.0F, 0.0F, GROUP_COLLAPSE_BUTTON_WIDTH, GROUP_BUTTON_HEIGHT, GROUP_COLLAPSE_ICON, button -> {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.layerWidget.editor.history.saveSnapshot();
                container.setCollapsedInLayerList(!container.isCollapsedInLayerList());
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            });
            this.eyeButton = new UIIconButton(0.0F, 0.0F, LAYER_EYE_BUTTON_WIDTH, LAYER_EYE_BUTTON_HEIGHT, EYE_ICON, button -> {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.layerWidget.editor.history.saveSnapshot();
                if (!this.element.isSelected()) {
                    this.layerWidget.editor.deselectAllElements();
                }
                this.element.setSelected(true);
                this.element.element.layerHiddenInEditor = !this.element.element.layerHiddenInEditor;
            });
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.backgroundColorNormal = null;
            this.backgroundColorHover = null;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

            if (this.element.isSelected() || this.element.isMultiSelected() || this.isHovered()) {
                this.layerWidget.fillClippedToRoundedBottomSmooth(graphics, this.x, this.y, this.getWidth(), this.getHeight(), getElementHoverColor().getColorInt());
                
            }

            float layerIconSize = getIconSize(this.getLayerIconWidth(), this.getLayerIconHeight(), LAYER_ENTRY_ICON_PADDING);
            float layerIconX = this.getLayerIconX() + (this.getLayerIconWidth() - layerIconSize) * 0.5f;
            float layerIconY = this.getLayerIconY() + (this.getLayerIconHeight() - layerIconSize) * 0.5f;
            LAYER_ICON.render(graphics, layerIconX, layerIconY, layerIconSize, layerIconSize, UIBase.getUITheme().getUIIconColor(1.0F));

            this.updateEyeButtonLayout();
            this.updateCollapseButtonLayout();
            int renderMouseX = this.isIconButtonInteractionAllowed() ? mouseX : Integer.MIN_VALUE;
            int renderMouseY = this.isIconButtonInteractionAllowed() ? mouseY : Integer.MIN_VALUE;
            if (this.collapseButton != null) this.collapseButton.extractRenderState(graphics, renderMouseX, renderMouseY, partial);
            this.eyeButton.extractRenderState(graphics, renderMouseX, renderMouseY, partial);

            float labelX = this.getLayerNameX();
            float labelY = this.getLayerNameY();
            float labelWidth = this.getMaxLayerNameWidth();
            float labelHeight = this.getHeight();
            if (labelWidth > 0.0f) {
                graphics.enableScissor((int) labelX, (int) this.getY(), (int) (labelX + labelWidth), (int) (this.getY() + labelHeight));
                UIBase.renderText(graphics, Component.literal(this.getLayerName()), (int) labelX, (int) labelY);
                graphics.disableScissor();
            }

        }

        @SuppressWarnings("all")
        public String getLayerName() {
            if (this.element.element.customElementLayerName != null) return this.element.element.customElementLayerName;
            return this.element.element.getBuilder().getDisplayName(this.element.element).getString();
        }

        public float getLayerNameX() {
            float iconSize = getIconSize(this.getLayerIconWidth(), this.getLayerIconHeight(), LAYER_ENTRY_ICON_PADDING);
            float iconX = this.getLayerIconX() + (this.getLayerIconWidth() - iconSize) * 0.5f;
            return iconX + iconSize + LAYER_ICON_TEXT_GAP;
        }

        public float getLayerNameY() {
            return this.getY() + (this.getHeight() / 2f) - (UIBase.getUITextHeightNormal() / 2f);
        }

        public float getMaxLayerNameWidth() {
            float rightEdge = this.x + this.getWidth() - RIGHT_SIDE_BUTTON_OFFSET;
            float eyeWidth = this.eyeButton.getWidth();
            float collapseWidth = (this.collapseButton != null) ? this.collapseButton.getWidth() : 0.0F;
            return rightEdge - eyeWidth - collapseWidth - this.getLayerNameX() - 3f;
        }

        public boolean isEyeButtonMouseOver(double mouseX, double mouseY) {
            if (!this.isIconButtonInteractionAllowed()) return false;
            return this.eyeButton.isMouseOver(mouseX, mouseY);
        }

        public boolean isCollapseButtonMouseOver(double mouseX, double mouseY) {
            if ((this.collapseButton == null) || !this.isIconButtonInteractionAllowed()) return false;
            return this.collapseButton.isMouseOver(mouseX, mouseY);
        }

        /** The button sits where the one of layer groups sits: in front of the eye. */
        private void updateCollapseButtonLayout() {
            if ((this.collapseButton == null) || !(this.element.element instanceof ElementContainer container)) return;
            float collapseWidth = this.collapseButton.getWidth();
            float collapseHeight = this.collapseButton.getHeight();
            this.collapseButton.setX(this.x + this.getWidth() - this.eyeButton.getWidth() - collapseWidth - RIGHT_SIDE_BUTTON_OFFSET)
                    .setY(this.y + (this.getHeight() - collapseHeight) * 0.5f)
                    .setWidth(collapseWidth)
                    .setHeight(collapseHeight)
                    .setIcon(container.isCollapsedInLayerList() ? GROUP_EXPAND_ICON : GROUP_COLLAPSE_ICON)
                    .setIconAlpha(1.0f);
        }

        private boolean isIconButtonInteractionAllowed() {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            return this.parent.isInnerAreaHovered();
        }

        private void updateEyeButtonLayout() {
            float eyeAlpha = !this.element.element.layerHiddenInEditor ? 1.0f : 0.3f;
            float eyeWidth = this.eyeButton.getWidth();
            float eyeHeight = this.eyeButton.getHeight();
            this.eyeButton.setX(this.x + this.getWidth() - eyeWidth - RIGHT_SIDE_BUTTON_OFFSET)
                    .setY(this.y + (this.getHeight() - eyeHeight) * 0.5f)
                    .setWidth(eyeWidth)
                    .setHeight(eyeHeight)
                    .setIcon(EYE_ICON)
                    .setIconAlpha(eyeAlpha);
        }

        public float getLayerIconWidth() {
            return 30f;
        }

        public float getLayerIconHeight() {
            return 28f;
        }

        public float getIndentOffset() {
            return ((this.group != null) ? GROUP_INDENT : 0.0f) + (this.depth * GROUP_INDENT);
        }

        public float getLayerIconX() {
            return this.x + this.getIndentOffset();
        }

        public float getLayerIconY() {
            return this.y;
        }

        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                this.updateEyeButtonLayout();
                this.updateCollapseButtonLayout();
                if (this.isMouseOver(mouseX, mouseY) && !this.isEyeButtonMouseOver(mouseX, mouseY) && !this.isCollapseButtonMouseOver(mouseX, mouseY)) {
                    // Store initial position for drag threshold checking
                    this.dragStartX = mouseX;
                    this.dragStartY = mouseY;
                    this.dragStarted = true;

                    // Don't handle selection logic here - let it be handled in the onClick method
                    // which is called by the parent widget with proper event sequencing
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        public boolean layerMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT && this.dragStarted) {
                // Check if we've dragged past the threshold to start a real drag operation
                double deltaX = mouseX - this.dragStartX;
                double deltaY = mouseY - this.dragStartY;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                if (distance > DRAG_THRESHOLD && !this.layerWidget.isDragging) {
                    // Only start drag if this element is selected
                    if (this.element.isSelected()) {
                        // Start drag operation
                        this.layerWidget.draggedEntry = this;
                        this.layerWidget.isDragging = true;
                        this.layerWidget.updateDragTarget(mouseX, mouseY);

                        return true;
                    }
                }
                return true;
            }
            return false;
        }

        public boolean layerMouseReleased(double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
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
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                this.updateEyeButtonLayout();
                this.updateCollapseButtonLayout();
                if (this.isEyeButtonMouseOver(mouseX, mouseY)) {
                    this.eyeButton.mouseClicked(mouseX, mouseY, button);
                } else if ((this.collapseButton != null) && this.isCollapseButtonMouseOver(mouseX, mouseY)) {
                    this.collapseButton.mouseClicked(mouseX, mouseY, button);
                } else {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));

                    // Proper multi-selection with the platform GUI shortcut modifier
                    boolean shortcutModifierDown = InputUtils.isGuiShortcutModifierDown();

                    if (!shortcutModifierDown) {
                        // Without the shortcut modifier, deselect all others first
                        this.layerWidget.editor.deselectAllElements();
                    } else if (this.element.isSelected()) {
                        // With the shortcut modifier and already selected, toggle this element (deselect it)
                        this.element.setSelected(false);
                        return;
                    }

                    // Select this element unless the shortcut modifier was down and we just deselected it
                    if (!this.element.isSelected()) {
                        this.element.setSelected(true);
                    }

                }
            }
            if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                this.element.setSelected(true);
                this.layerWidget.openContextMenuForLayer(this);
            }
        }

    }

    private class LayerGroupEntry extends ScrollAreaEntry {

        protected final Layout.LayerGroup group;
        protected final LayerLayoutEditorWidget layerWidget;
        protected Font font = Minecraft.getInstance().font;
        protected final UIIconButton eyeButton;
        protected final UIIconButton collapseButton;
        protected boolean dragStarted = false;
        protected double dragStartX;
        protected double dragStartY;
        private static final int DRAG_THRESHOLD = 3;

        public LayerGroupEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget, @NotNull Layout.LayerGroup group) {
            super(parent, 50, 28);
            this.group = group;
            this.layerWidget = layerWidget;
            this.eyeButton = new UIIconButton(0.0F, 0.0F, GROUP_EYE_BUTTON_WIDTH, GROUP_BUTTON_HEIGHT, EYE_ICON, button -> {
                List<AbstractEditorElement<?, ?>> elements = this.layerWidget.editor.getElementsInGroup(this.group);
                if (elements.isEmpty()) {
                    return;
                }
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.layerWidget.editor.history.saveSnapshot();
                boolean hideElements = false;
                for (AbstractEditorElement<?, ?> element : elements) {
                    if (!element.element.layerHiddenInEditor) {
                        hideElements = true;
                        break;
                    }
                }
                for (AbstractEditorElement<?, ?> element : elements) {
                    element.element.layerHiddenInEditor = hideElements;
                }
            });
            this.collapseButton = new UIIconButton(0.0F, 0.0F, GROUP_COLLAPSE_BUTTON_WIDTH, GROUP_BUTTON_HEIGHT, GROUP_COLLAPSE_ICON, button -> {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.layerWidget.editor.history.saveSnapshot();
                this.group.collapsed = !this.group.collapsed;
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            });
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.backgroundColorNormal = null;
            this.backgroundColorHover = null;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            if (this.isGroupFullySelected()) {
                this.layerWidget.fillClippedToRoundedBottomSmooth(graphics, this.x, this.y, this.getWidth(), this.getHeight(), getElementHoverColor().getColorInt());
            } else if (this.isMouseOver(mouseX, mouseY)) {
                this.layerWidget.fillClippedToRoundedBottomSmooth(graphics, this.x, this.y, this.getWidth(), this.getHeight(), getElementHoverColor().getColorInt());
            }

            float groupIconSize = getIconSize(this.getGroupIconWidth(), this.getGroupIconHeight(), LAYER_ENTRY_ICON_PADDING);
            float groupIconX = this.getGroupIconX() + (this.getGroupIconWidth() - groupIconSize) * 0.5f;
            float groupIconY = this.getGroupIconY() + (this.getGroupIconHeight() - groupIconSize) * 0.5f;
            GROUP_ICON.render(graphics, groupIconX, groupIconY, groupIconSize, groupIconSize, UIBase.getUITheme().getUIIconColor(1.0F));

            this.updateEyeButtonLayout();
            this.updateCollapseButtonLayout();
            int renderMouseX = this.isIconButtonInteractionAllowed() ? mouseX : Integer.MIN_VALUE;
            int renderMouseY = this.isIconButtonInteractionAllowed() ? mouseY : Integer.MIN_VALUE;
            this.collapseButton.extractRenderState(graphics, renderMouseX, renderMouseY, partial);
            this.eyeButton.extractRenderState(graphics, renderMouseX, renderMouseY, partial);

            float labelX = this.getGroupNameX();
            float labelY = this.getGroupNameY();
            float labelWidth = this.getMaxGroupNameWidth();
            float labelHeight = this.getHeight();
            if (labelWidth > 0.0f) {
                graphics.enableScissor((int) labelX, (int) this.getY(), (int) (labelX + labelWidth), (int) (this.getY() + labelHeight));
                UIBase.renderText(graphics, Component.literal(this.getGroupName()), (int) labelX, (int) labelY);
                graphics.disableScissor();
            }
        }

        public boolean isEyeButtonMouseOver(double mouseX, double mouseY) {
            if (!this.isIconButtonInteractionAllowed()) return false;
            return this.eyeButton.isMouseOver(mouseX, mouseY);
        }

        public boolean isCollapseButtonMouseOver(double mouseX, double mouseY) {
            if (!this.isIconButtonInteractionAllowed()) return false;
            return this.collapseButton.isMouseOver(mouseX, mouseY);
        }

        private boolean isIconButtonInteractionAllowed() {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            return this.parent.isInnerAreaHovered();
        }

        private void updateEyeButtonLayout() {
            float eyeAlpha = this.isGroupHidden() ? 0.3f : 1.0f;
            float eyeWidth = this.eyeButton.getWidth();
            float eyeHeight = this.eyeButton.getHeight();
            this.eyeButton.setX(this.x + this.getWidth() - eyeWidth - RIGHT_SIDE_BUTTON_OFFSET)
                    .setY(this.y + (this.getHeight() - eyeHeight) * 0.5f)
                    .setWidth(eyeWidth)
                    .setHeight(eyeHeight)
                    .setIcon(EYE_ICON)
                    .setIconAlpha(eyeAlpha);
        }

        private void updateCollapseButtonLayout() {
            float eyeWidth = this.eyeButton.getWidth();
            float collapseWidth = this.collapseButton.getWidth();
            float collapseHeight = this.collapseButton.getHeight();
            this.collapseButton.setX(this.x + this.getWidth() - eyeWidth - collapseWidth - RIGHT_SIDE_BUTTON_OFFSET)
                    .setY(this.y + (this.getHeight() - collapseHeight) * 0.5f)
                    .setWidth(collapseWidth)
                    .setHeight(collapseHeight)
                    .setIcon(this.group.collapsed ? GROUP_EXPAND_ICON : GROUP_COLLAPSE_ICON)
                    .setIconAlpha(1.0f);
        }

        public float getGroupNameX() {
            float iconSize = getIconSize(this.getGroupIconWidth(), this.getGroupIconHeight(), LAYER_ENTRY_ICON_PADDING);
            float iconX = this.getGroupIconX() + (this.getGroupIconWidth() - iconSize) * 0.5f;
            return iconX + iconSize + LAYER_ICON_TEXT_GAP;
        }

        public float getGroupNameY() {
            return this.getY() + (this.getHeight() / 2f) - (UIBase.getUITextHeightNormal() / 2f);
        }

        public float getMaxGroupNameWidth() {
            float rightEdge = this.x + this.getWidth() - RIGHT_SIDE_BUTTON_OFFSET;
            float eyeWidth = this.eyeButton.getWidth();
            float collapseWidth = this.collapseButton.getWidth();
            return rightEdge - eyeWidth - collapseWidth - this.getGroupNameX() - 3f;
        }

        public float getGroupIconWidth() {
            return 30f;
        }

        public float getGroupIconHeight() {
            return 28f;
        }

        public float getGroupIconX() {
            return this.x + GROUP_NAME_LEFT_PADDING;
        }

        public float getGroupIconY() {
            return this.y;
        }

        public String getGroupName() {
            if (this.group.name != null && !this.group.name.replace(" ", "").isEmpty()) {
                return this.group.name;
            }
            return Component.translatable("fancymenu.editor.widgets.layers.group.default_name").getString();
        }

        protected boolean isGroupHidden() {
            List<AbstractEditorElement<?, ?>> elements = this.layerWidget.editor.getElementsInGroup(this.group);
            if (elements.isEmpty()) {
                return false;
            }
            for (AbstractEditorElement<?, ?> element : elements) {
                if (!element.element.layerHiddenInEditor) {
                    return false;
                }
            }
            return true;
        }

        protected boolean isGroupFullySelected() {
            List<AbstractEditorElement<?, ?>> elements = this.layerWidget.editor.getElementsInGroup(this.group);
            if (elements.isEmpty()) {
                return false;
            }
            for (AbstractEditorElement<?, ?> element : elements) {
                if (!element.isSelected()) {
                    return false;
                }
            }
            return true;
        }

        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                this.updateEyeButtonLayout();
                this.updateCollapseButtonLayout();
                if (this.isMouseOver(mouseX, mouseY) && !this.isCollapseButtonMouseOver(mouseX, mouseY) && !this.isEyeButtonMouseOver(mouseX, mouseY)) {
                    this.dragStartX = mouseX;
                    this.dragStartY = mouseY;
                    this.dragStarted = true;
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        public boolean groupMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT && this.dragStarted) {
                double deltaX = mouseX - this.dragStartX;
                double deltaY = mouseY - this.dragStartY;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                if (distance > DRAG_THRESHOLD && !this.layerWidget.isDragging) {
                    this.layerWidget.draggedEntry = this;
                    this.layerWidget.isDragging = true;
                    this.layerWidget.updateDragTarget(mouseX, mouseY);
                    return true;
                }
                return true;
            }
            return false;
        }

        public boolean groupMouseReleased(double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                if (this.dragStarted && !this.layerWidget.isDragging) {
                    this.onClick(this, mouseX, mouseY, button);
                }
                this.dragStarted = false;
            }
            return false;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                this.layerWidget.openContextMenuForGroup(this);
                return;
            }
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                this.updateEyeButtonLayout();
                this.updateCollapseButtonLayout();
                if (this.isEyeButtonMouseOver(mouseX, mouseY)) {
                    this.eyeButton.mouseClicked(mouseX, mouseY, button);
                } else if (this.isCollapseButtonMouseOver(mouseX, mouseY)) {
                    this.collapseButton.mouseClicked(mouseX, mouseY, button);
                } else {
                    List<AbstractEditorElement<?, ?>> elements = this.layerWidget.editor.getElementsInGroup(this.group);
                    if (!elements.isEmpty()) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        boolean shortcutModifierDown = InputUtils.isGuiShortcutModifierDown();
                        boolean allSelected = this.isGroupFullySelected();
                        if (!shortcutModifierDown) {
                            this.layerWidget.editor.deselectAllElements();
                        } else if (allSelected) {
                            for (AbstractEditorElement<?, ?> element : elements) {
                                element.setSelected(false);
                            }
                            return;
                        }
                        for (AbstractEditorElement<?, ?> element : elements) {
                            element.setSelected(true);
                        }
                    }
                }
            }
        }

    }

    private class VanillaLayerElementEntry extends ScrollAreaEntry {

        protected LayerLayoutEditorWidget layerWidget;
        protected final UIIconButton moveTopBottomButton;
        protected Font font = Minecraft.getInstance().font;

        public VanillaLayerElementEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget) {
            super(parent, 50, 28);
            this.layerWidget = layerWidget;
            this.moveTopBottomButton = new UIIconButton(0.0F, 0.0F, VANILLA_MOVE_BUTTON_WIDTH, VANILLA_MOVE_BUTTON_HEIGHT, MOVE_TO_TOP_ICON, button -> {
                if (FancyMenu.getOptions().playUiClickSounds.getValue()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                this.layerWidget.editor.history.saveSnapshot();
                this.layerWidget.editor.layout.renderElementsBehindVanilla = !this.layerWidget.editor.layout.renderElementsBehindVanilla;
                this.layerWidget.editor.deselectAllElements();
                MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            });
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.backgroundColorNormal = null;
            this.backgroundColorHover = null;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

            this.updateMoveTopBottomButtonLayout();
            int renderMouseX = this.isIconButtonInteractionAllowed() ? mouseX : Integer.MIN_VALUE;
            int renderMouseY = this.isIconButtonInteractionAllowed() ? mouseY : Integer.MIN_VALUE;
            this.moveTopBottomButton.extractRenderState(graphics, renderMouseX, renderMouseY, partial);

            float iconSize = getIconSize(30.0f, this.getHeight(), LAYER_ENTRY_ICON_PADDING);
            float iconBaseX = this.x + (30.0f - iconSize) * 0.5f;
            float iconBaseY = this.y + (this.getHeight() - iconSize) * 0.5f;
            VANILLA_ICON.render(graphics, iconBaseX, iconBaseY, iconSize, iconSize, UIBase.getUITheme().getUIIconColor(1.0F));
            float labelX = iconBaseX + iconSize + LAYER_ICON_TEXT_GAP;
            float rightEdge = this.x + this.getWidth() - RIGHT_SIDE_BUTTON_OFFSET;
            float labelWidth = rightEdge - this.moveTopBottomButton.getWidth() - labelX - 3f;
            float labelY = this.getY() + (this.getHeight() / 2f) - (UIBase.getUITextHeightNormal() / 2f);
            if (labelWidth > 0.0f) {
                graphics.enableScissor((int) labelX, (int) this.getY(), (int) (labelX + labelWidth), (int) (this.getY() + this.getHeight()));
                UIBase.renderText(graphics, Component.translatable("fancymenu.editor.widgets.layers.vanilla_elements").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())), (int) labelX, (int) labelY);
                graphics.disableScissor();
            }

        }

        public boolean isMoveTopBottomButtonHovered(double mouseX, double mouseY) {
            if (!this.isIconButtonInteractionAllowed()) return false;
            return this.moveTopBottomButton.isMouseOver(mouseX, mouseY);
        }

        private boolean isIconButtonInteractionAllowed() {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            return this.parent.isInnerAreaHovered();
        }

        private void updateMoveTopBottomButtonLayout() {
            MaterialIcon icon = this.layerWidget.editor.layout.renderElementsBehindVanilla ? MOVE_BEHIND_ICON : MOVE_TO_TOP_ICON;
            float buttonWidth = this.moveTopBottomButton.getWidth();
            float buttonHeight = this.moveTopBottomButton.getHeight();
            this.moveTopBottomButton.setX(this.x + this.getWidth() - buttonWidth - RIGHT_SIDE_BUTTON_OFFSET)
                    .setY(this.y + (this.getHeight() - buttonHeight) * 0.5f)
                    .setWidth(buttonWidth)
                    .setHeight(buttonHeight)
                    .setIcon(icon)
                    .setIconAlpha(1.0f);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                this.updateMoveTopBottomButtonLayout();
                if (this.isMoveTopBottomButtonHovered(mouseX, mouseY)) {
                    this.moveTopBottomButton.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

    }

    private class SeparatorEntry extends ScrollAreaEntry {

        public SeparatorEntry(ScrollArea parent) {
            super(parent, 50f, 1f);
            this.selectable = false;
            this.selectOnClick = false;
            this.backgroundColorNormal = null;
            this.backgroundColorHover = null;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            LayerLayoutEditorWidget.this.fillClippedToRoundedBottom(graphics, this.x, this.y, this.getWidth(), this.getHeight(), getBorderColor().getColorInt());
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
