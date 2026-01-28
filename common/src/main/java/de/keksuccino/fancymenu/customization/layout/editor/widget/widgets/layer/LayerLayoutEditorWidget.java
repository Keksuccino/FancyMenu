package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class LayerLayoutEditorWidget extends AbstractLayoutEditorWidget {

    protected ScrollArea scrollArea;

    // Added fields for drag and drop functionality
    protected ScrollAreaEntry draggedEntry = null;
    private int dragTargetUiIndex = -1;
    private float dragTargetIndicatorY = Float.NaN;
    @Nullable
    private Layout.LayerGroup dragTargetGroup = null;
    private boolean isDragging = false;
    private static final int DROP_INDICATOR_THICKNESS = 3;
    private static final int TITLE_BAR_ICON_BASE_SIZE = 8;
    private static final MaterialIcon TITLE_BAR_HIDE_ICON = MaterialIcons.CLOSE;
    private static final MaterialIcon TITLE_BAR_EXPAND_ICON = MaterialIcons.EXPAND_MORE;
    private static final MaterialIcon TITLE_BAR_COLLAPSE_ICON = MaterialIcons.EXPAND_LESS;
    private static final MaterialIcon GROUP_ADD_ICON = MaterialIcons.ADD;
    private static final MaterialIcon GROUP_DELETE_ICON = MaterialIcons.CLOSE;
    private static final MaterialIcon GROUP_EXPAND_ICON = MaterialIcons.EXPAND_MORE;
    private static final MaterialIcon GROUP_COLLAPSE_ICON = MaterialIcons.EXPAND_LESS;
    private static final MaterialIcon EYE_ICON = MaterialIcons.VISIBILITY;
    private static final MaterialIcon MOVE_TO_TOP_ICON = MaterialIcons.VERTICAL_ALIGN_TOP;
    private static final MaterialIcon MOVE_BEHIND_ICON = MaterialIcons.VERTICAL_ALIGN_BOTTOM;
    private static final float LAYER_EYE_ICON_PADDING = 4.0f;
    private static final float LAYER_ICON_TEXT_GAP = 4.0f;
    private static final float GROUP_INDENT = 12.0f;
    private static final float GROUP_NAME_LEFT_PADDING = 0.0f;
    private static final float GROUP_DELETE_BUTTON_WIDTH = 24.0f;
    private static final float GROUP_COLLAPSE_BUTTON_WIDTH = 24.0f;

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

        this.addTitleBarButton(new MaterialTitleBarButton(this, button -> GROUP_ADD_ICON, button -> {
            this.editor.history.saveSnapshot();
            this.editor.layout.layerGroups.add(new Layout.LayerGroup());
            MainThreadTaskExecutor.executeInMainThread(() -> this.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }));
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
            } else if (e instanceof LayerGroupEntry g) {
                this.children.remove(g.editGroupNameBox);
            }
        }
        this.scrollArea.clearEntries();
        if (this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        Set<Layout.LayerGroup> handledGroups = new HashSet<>();
        for (AbstractEditorElement e : Lists.reverse(new ArrayList<>(this.editor.normalEditorElements))) {
            Layout.LayerGroup group = this.editor.getLayerGroupForElement(e);
            if ((group != null) && !handledGroups.contains(group)) {
                LayerGroupEntry groupEntry = new LayerGroupEntry(this.scrollArea, this, group);
                this.children.add(groupEntry.editGroupNameBox);
                this.scrollArea.addEntry(groupEntry);
                this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
                handledGroups.add(group);
            }
            if (group != null && group.collapsed) {
                continue;
            }
            LayerElementEntry layer = new LayerElementEntry(this.scrollArea, this, e, group);
            this.children.add(layer.editLayerNameBox);
            this.scrollArea.addEntry(layer);
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        for (Layout.LayerGroup group : this.editor.layout.layerGroups) {
            if (!handledGroups.contains(group)) {
                LayerGroupEntry groupEntry = new LayerGroupEntry(this.scrollArea, this, group);
                this.children.add(groupEntry.editGroupNameBox);
                this.scrollArea.addEntry(groupEntry);
                this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
            }
        }
        if (!this.editor.layout.renderElementsBehindVanilla) {
            this.scrollArea.addEntry(new VanillaLayerElementEntry(this.scrollArea, this));
            this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea));
        }
        if (keepScroll) this.scrollArea.verticalScrollBar.setScroll(scroll);

        // Reset drag state when list is updated
        this.draggedEntry = null;
        this.dragTargetUiIndex = -1;
        this.dragTargetIndicatorY = Float.NaN;
        this.dragTargetGroup = null;
        this.isDragging = false;
    }

    @Override
    protected void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.scrollArea.setX(0);
        this.scrollArea.setY(0);
        this.scrollArea.setWidth(this.getBodyWidth());
        this.scrollArea.setHeight(this.getBodyHeight());
        this.scrollArea.horizontalScrollBar.active = false;
        this.scrollArea.makeEntriesWidthOfArea = true;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;

        this.scrollArea.render(graphics, mouseX, mouseY, partial);

        // Render the drop indicator if currently dragging
        if (isDragging && Float.isFinite(dragTargetIndicatorY)) {
            float indicatorY = dragTargetIndicatorY;
            UIBase.fillF(graphics, this.scrollArea.getInnerX(), indicatorY - DROP_INDICATOR_THICKNESS / 2f,
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isVisible()) {
            return false;
        }
        this.stopEditingLayerNames();
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
        }

        return true;

    }

    private void stopEditingLayerNames() {
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof LayerElementEntry l) {
                if (!l.isLayerNameHovered()) {
                    l.stopEditingLayerName();
                }
            } else if (e instanceof LayerGroupEntry g) {
                if (!g.isGroupNameHovered()) {
                    g.stopEditingGroupName();
                }
            }
        }
    }

    @Override
    protected boolean mouseReleasedBody(double mouseX, double mouseY, int button) {

        // Handle drop operation when mouse button is released
        if (button == 0 && isDragging && dragTargetUiIndex >= 0) {
            finishDragOperation();
        }

        // Always inform scroll bars about mouse release so they can reset their grabber state
        this.scrollArea.verticalScrollBar.mouseReleased(mouseX, mouseY, button);
        this.scrollArea.horizontalScrollBar.mouseReleased(mouseX, mouseY, button);

        // Reset drag state
        isDragging = false;
        draggedEntry = null;
        dragTargetUiIndex = -1;
        dragTargetIndicatorY = Float.NaN;
        dragTargetGroup = null;

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

        if (isDragging && button == 0) {
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
     * Updates the drag target index based on current mouse position
     */
    private void updateDragTarget(double mouseX, double mouseY) {
        if (!isDragging || draggedEntry == null) return;

        List<AbstractEditorElement<?, ?>> movingElements = this.getMovingElementsForDrag();
        List<LayerElementEntry> dragEntries = this.getLayerEntriesInUiOrderExcluding(movingElements);
        this.dragTargetUiIndex = -1;
        this.dragTargetIndicatorY = Float.NaN;
        this.dragTargetGroup = null;

        if (!(draggedEntry instanceof LayerGroupEntry)) {
            LayerGroupEntry hoveredGroup = this.getHoveredGroupEntry(mouseX, mouseY);
            if (hoveredGroup != null) {
                this.dragTargetGroup = hoveredGroup.group;
                this.dragTargetUiIndex = this.getGroupInsertionIndex(hoveredGroup.group, dragEntries);
                this.dragTargetIndicatorY = this.getIndicatorYForUiIndex(dragEntries, this.dragTargetUiIndex);
                return;
            }
        }

        if (dragEntries.isEmpty()) {
            this.dragTargetUiIndex = 0;
            this.dragTargetIndicatorY = this.scrollArea.getInnerY();
            return;
        }

        LayerElementEntry hoveredLayer = this.getHoveredLayerEntry(mouseX, mouseY);
        if ((hoveredLayer != null) && dragEntries.contains(hoveredLayer)) {
            float entryMidpoint = hoveredLayer.getY() + hoveredLayer.getHeight() / 2f;
            int uiIndex = dragEntries.indexOf(hoveredLayer);
            if (mouseY < entryMidpoint) {
                this.dragTargetUiIndex = uiIndex;
                this.dragTargetIndicatorY = hoveredLayer.getY();
            } else {
                this.dragTargetUiIndex = uiIndex + 1;
                this.dragTargetIndicatorY = hoveredLayer.getY() + hoveredLayer.getHeight();
            }
            if (!(draggedEntry instanceof LayerGroupEntry)) {
                this.dragTargetGroup = hoveredLayer.group;
            }
            return;
        }

        if (mouseY < this.scrollArea.getInnerY()) {
            this.dragTargetUiIndex = 0;
            this.dragTargetIndicatorY = dragEntries.get(0).getY();
        } else if (mouseY > this.scrollArea.getInnerY() + this.scrollArea.getInnerHeight()) {
            LayerElementEntry lastEntry = dragEntries.get(dragEntries.size() - 1);
            this.dragTargetUiIndex = dragEntries.size();
            this.dragTargetIndicatorY = lastEntry.getY() + lastEntry.getHeight();
        } else {
            for (int i = 0; i < dragEntries.size(); i++) {
                LayerElementEntry entry = dragEntries.get(i);
                float entryMidpoint = entry.getY() + entry.getHeight() / 2f;
                if (mouseY < entryMidpoint) {
                    this.dragTargetUiIndex = i;
                    this.dragTargetIndicatorY = entry.getY();
                    break;
                }
            }
            if (this.dragTargetUiIndex == -1) {
                LayerElementEntry lastEntry = dragEntries.get(dragEntries.size() - 1);
                this.dragTargetUiIndex = dragEntries.size();
                this.dragTargetIndicatorY = lastEntry.getY() + lastEntry.getHeight();
            }
        }

        if (!(draggedEntry instanceof LayerGroupEntry)) {
            this.dragTargetGroup = this.resolveDragTargetGroup(mouseX, mouseY, this.dragTargetIndicatorY);
        }
    }

    @NotNull
    private List<LayerElementEntry> getLayerEntriesInUiOrder() {
        List<LayerElementEntry> entries = new ArrayList<>();
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry instanceof LayerElementEntry layerEntry) {
                entries.add(layerEntry);
            }
        }
        return entries;
    }

    @NotNull
    private List<LayerElementEntry> getLayerEntriesInUiOrderExcluding(@NotNull Collection<AbstractEditorElement<?, ?>> excluded) {
        if (excluded.isEmpty()) {
            return this.getLayerEntriesInUiOrder();
        }
        List<LayerElementEntry> entries = new ArrayList<>();
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry instanceof LayerElementEntry layerEntry) {
                if (!excluded.contains(layerEntry.element)) {
                    entries.add(layerEntry);
                }
            }
        }
        return entries;
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

    @Nullable
    private LayerElementEntry getHoveredLayerEntry(double mouseX, double mouseY) {
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry instanceof LayerElementEntry layerEntry) {
                if (layerEntry.isMouseOver(mouseX, mouseY)) {
                    return layerEntry;
                }
            }
        }
        return null;
    }

    @Nullable
    private LayerGroupEntry getGroupEntry(@NotNull Layout.LayerGroup group) {
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry instanceof LayerGroupEntry groupEntry) {
                if (groupEntry.group == group) {
                    return groupEntry;
                }
            }
        }
        return null;
    }

    private int getGroupInsertionIndex(@NotNull Layout.LayerGroup group, @NotNull List<LayerElementEntry> entries) {
        int lastIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).group == group) {
                lastIndex = i;
            }
        }
        if (lastIndex >= 0) {
            return lastIndex + 1;
        }
        LayerGroupEntry groupEntry = this.getGroupEntry(group);
        if (groupEntry != null) {
            float groupY = groupEntry.getY();
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getY() > groupY) {
                    return i;
                }
            }
        }
        return entries.size();
    }

    private float getIndicatorYForUiIndex(@NotNull List<LayerElementEntry> entries, int uiIndex) {
        if (entries.isEmpty()) {
            return this.scrollArea.getInnerY();
        }
        if (uiIndex <= 0) {
            return entries.get(0).getY();
        }
        if (uiIndex >= entries.size()) {
            LayerElementEntry lastEntry = entries.get(entries.size() - 1);
            return lastEntry.getY() + lastEntry.getHeight();
        }
        return entries.get(uiIndex).getY();
    }

    @Nullable
    private Layout.LayerGroup resolveDragTargetGroup(double mouseX, double mouseY, float indicatorY) {
        LayerGroupEntry hoveredGroup = this.getHoveredGroupEntry(mouseX, mouseY);
        if (hoveredGroup != null) {
            return hoveredGroup.group;
        }
        LayerElementEntry hoveredLayer = this.getHoveredLayerEntry(mouseX, mouseY);
        if (hoveredLayer != null) {
            return hoveredLayer.group;
        }
        List<LayerElementEntry> entries = this.getLayerEntriesInUiOrder();
        if (entries.isEmpty() || !Float.isFinite(indicatorY)) {
            return null;
        }
        int uiIndex = 0;
        for (; uiIndex < entries.size(); uiIndex++) {
            if (indicatorY <= entries.get(uiIndex).getY()) {
                break;
            }
        }
        Layout.LayerGroup before = (uiIndex > 0) ? entries.get(uiIndex - 1).group : null;
        Layout.LayerGroup after = (uiIndex < entries.size()) ? entries.get(uiIndex).group : null;
        if ((before != null) && (before == after)) {
            return before;
        }
        return null;
    }

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
        return movingElements;
    }

    /**
     * Completes the drag operation by moving the dragged elements to the new position.
     */
    private void finishDragOperation() {

        if (this.dragTargetUiIndex < 0 || this.draggedEntry == null) {
            return;
        }

        List<AbstractEditorElement<?, ?>> movingElements = this.getMovingElementsForDrag();
        if (movingElements.isEmpty()) {
            return;
        }

        Map<AbstractEditorElement<?, ?>, Integer> oldIndices = new HashMap<>();
        for (int i = 0; i < this.editor.normalEditorElements.size(); i++) {
            oldIndices.put(this.editor.normalEditorElements.get(i), i);
        }

        movingElements.sort(Comparator.comparingInt(element -> oldIndices.getOrDefault(element, -1)));

        List<AbstractEditorElement<?, ?>> remaining = new ArrayList<>(this.editor.normalEditorElements);
        remaining.removeAll(movingElements);

        int remainingCount = remaining.size();
        int targetUiIndex = Math.min(Math.max(this.dragTargetUiIndex, 0), remainingCount);
        int targetIndex = remainingCount - targetUiIndex;
        targetIndex = Math.min(Math.max(targetIndex, 0), remainingCount);

        List<AbstractEditorElement<?, ?>> reordered = new ArrayList<>(remaining);
        reordered.addAll(targetIndex, movingElements);
        boolean groupChangeRequired = false;
        if (!(this.draggedEntry instanceof LayerGroupEntry)) {
            if (this.dragTargetGroup != null) {
                for (AbstractEditorElement<?, ?> element : movingElements) {
                    Layout.LayerGroup currentGroup = this.editor.getLayerGroupForElement(element);
                    if (currentGroup != this.dragTargetGroup) {
                        groupChangeRequired = true;
                        break;
                    }
                }
            } else {
                for (AbstractEditorElement<?, ?> element : movingElements) {
                    if (this.editor.getLayerGroupForElement(element) != null) {
                        groupChangeRequired = true;
                        break;
                    }
                }
            }
        }
        if (reordered.equals(this.editor.normalEditorElements) && !groupChangeRequired) {
            return;
        }

        this.editor.history.saveSnapshot();

        this.editor.normalEditorElements = reordered;

        if (!(this.draggedEntry instanceof LayerGroupEntry)) {
            List<String> movingIds = new ArrayList<>();
            for (AbstractEditorElement<?, ?> element : movingElements) {
                movingIds.add(element.element.getInstanceIdentifier());
            }
            if (this.dragTargetGroup != null) {
                this.editor.addElementsToLayerGroup(movingIds, this.dragTargetGroup);
            } else {
                this.editor.removeElementsFromLayerGroups(movingIds);
            }
        }

        this.editor.updateLayerGroupElementOrder();

        for (AbstractEditorElement<?, ?> element : movingElements) {
            Integer oldIndex = oldIndices.get(element);
            Integer newIndex = this.editor.normalEditorElements.indexOf(element);
            if ((oldIndex != null) && (newIndex != null)) {
                boolean movedUp = newIndex > oldIndex;
                this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(element, movedUp));
            }
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
        this.updateList(false);
    }

    @Nullable
    private static IconRenderData resolveMaterialIconData(@Nullable MaterialIcon icon) {
        if (icon == null) {
            return null;
        }
        int size = UIBase.getUIMaterialIconTextureSizeNormal();
        ResourceLocation location = icon.getTextureLocation(size);
        if (location == null) {
            return null;
        }
        int width = icon.getWidth(size);
        int height = icon.getHeight(size);
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new IconRenderData(location, width, height);
    }

    private static void blitScaledIcon(@NotNull GuiGraphics graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
        if (areaWidth <= 0.0F || areaHeight <= 0.0F || iconData.width <= 0 || iconData.height <= 0) {
            return;
        }
        float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            return;
        }
        float scaledWidth = iconData.width * scale;
        float scaledHeight = iconData.height * scale;
        float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
        float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
        graphics.pose().popPose();
    }

    private void renderMaterialIcon(@NotNull GuiGraphics graphics, @NotNull MaterialIcon icon, float x, float y, float width, float height, float alpha) {
        IconRenderData iconData = resolveMaterialIconData(icon);
        if (iconData == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        UIBase.getUITheme().setUITextureShaderColor(graphics, alpha);
        blitScaledIcon(graphics, iconData, x, y, width, height);
        UIBase.resetShaderColor(graphics);
    }

    private static float getIconSize(float areaWidth, float areaHeight, float padding) {
        float size = Math.min(areaWidth, areaHeight) - (padding * 2.0f);
        return Math.max(1.0f, size);
    }

    private static final class IconRenderData {
        private final ResourceLocation texture;
        private final int width;
        private final int height;

        private IconRenderData(@NotNull ResourceLocation texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    private class MaterialTitleBarButton extends TitleBarButton {

        @NotNull
        private final ConsumingSupplier<MaterialTitleBarButton, MaterialIcon> materialIconSupplier;

        private MaterialTitleBarButton(@NotNull AbstractLayoutEditorWidget parent, @NotNull ConsumingSupplier<MaterialTitleBarButton, MaterialIcon> materialIconSupplier, @NotNull Consumer<TitleBarButton> clickAction) {
            super(parent, button -> null, clickAction);
            this.materialIconSupplier = materialIconSupplier;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, float partial, double localMouseX, double localMouseY) {
            this.hovered = this.isMouseOver(localMouseX, localMouseY);

            this.renderHoverBackground(graphics, partial);

            MaterialIcon icon = this.materialIconSupplier.get(this);
            if (icon == null) {
                return;
            }
            IconRenderData iconData = resolveMaterialIconData(icon);
            if (iconData == null) {
                return;
            }
            float iconPadding = Math.max(0.0F, 4.0F);
            float maxIconSize = Math.max(1.0F, TITLE_BAR_ICON_BASE_SIZE);
            float iconSize = Math.max(1.0F, Math.min(this.width - iconPadding, maxIconSize));
            iconSize = Math.min(iconSize, Math.min(this.width, this.parent.getTitleBarHeight()));
            float iconX = this.x + (this.width - iconSize) * 0.5F;
            float iconY = this.y + (this.parent.getTitleBarHeight() - iconSize) * 0.5F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
            blitScaledIcon(graphics, iconData, iconX, iconY, iconSize, iconSize);
            UIBase.resetShaderColor(graphics);
        }

        @Override
        protected void renderHoverBackground(GuiGraphics graphics, float partial) {
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

        protected AbstractEditorElement element;
        protected LayerLayoutEditorWidget layerWidget;
        @Nullable
        protected Layout.LayerGroup group;
        protected boolean eyeButtonHovered = false;
        protected Font font = Minecraft.getInstance().font;
        protected ExtendedEditBox editLayerNameBox;
        protected boolean displayEditLayerNameBox = false;
        protected boolean layerNameHovered = false;
        protected long lastLeftClick = -1L;
        protected boolean dragStarted = false; // Flag to track if drag has been initiated
        protected double dragStartX;
        protected double dragStartY;
        private static final int DRAG_THRESHOLD = 3; // Minimum pixels to move before initiating drag

        public LayerElementEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget, @NotNull AbstractEditorElement element, @Nullable Layout.LayerGroup group) {
            super(parent, 50, 28);
            this.element = element;
            this.layerWidget = layerWidget;
            this.group = group;
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
            this.backgroundColorNormal = null;
            this.backgroundColorHover = null;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.eyeButtonHovered = this.isEyeButtonMouseOver(mouseX, mouseY);
            this.layerNameHovered = this.isLayerNameMouseOver(mouseX, mouseY);

            RenderSystem.enableBlend();

            if (this.element.isSelected() || this.element.isMultiSelected()) {
                UIBase.fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), getElementHoverColor().getColorInt());
                graphics.flush();
            }

            float eyeIconSize = getIconSize(this.getEyeButtonWidth(), this.getEyeButtonHeight(), LAYER_EYE_ICON_PADDING);
            float eyeIconX = this.getEyeButtonX() + (this.getEyeButtonWidth() - eyeIconSize) * 0.5f;
            float eyeIconY = this.getEyeButtonY() + (this.getEyeButtonHeight() - eyeIconSize) * 0.5f;
            float eyeAlpha = !this.element.element.layerHiddenInEditor ? 1.0f : 0.3f;
            this.layerWidget.renderMaterialIcon(graphics, EYE_ICON, eyeIconX, eyeIconY, eyeIconSize, eyeIconSize, eyeAlpha);

            if (!this.displayEditLayerNameBox) {
                UIBase.renderText(graphics, Component.literal(this.getLayerName()), (int)this.getLayerNameX(), (int)this.getLayerNameY());
            } else {
                UIBase.applyDefaultWidgetSkinTo(this.editLayerNameBox);
                this.editLayerNameBox.setX((int)this.getLayerNameX());
                this.editLayerNameBox.setY((int)this.getLayerNameY() - 1);
                this.editLayerNameBox.setWidth((int) Math.min(this.getMaxLayerNameWidth(), UIBase.getUITextWidth(this.editLayerNameBox.getValue() + 13)));
                if (this.editLayerNameBox.getWidth() < this.getMaxLayerNameWidth()) {
                    this.editLayerNameBox.setDisplayPosition(0);
                }
                ((IMixinAbstractWidget)this.editLayerNameBox).setHeightFancyMenu((int)(UIBase.getUITextHeightNormal() + 2));
                this.editLayerNameBox.render(graphics, mouseX, mouseY, partial);
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
            return this.element.element.getBuilder().getDisplayName(this.element.element).getString();
        }

        public float getLayerNameX() {
            float eyeIconSize = getIconSize(this.getEyeButtonWidth(), this.getEyeButtonHeight(), LAYER_EYE_ICON_PADDING);
            float eyeIconX = this.getEyeButtonX() + (this.getEyeButtonWidth() - eyeIconSize) * 0.5f;
            return eyeIconX + eyeIconSize + LAYER_ICON_TEXT_GAP;
        }

        public float getLayerNameY() {
            return this.getY() + (this.getHeight() / 2f) - (UIBase.getUITextHeightNormal() / 2f);
        }

        public float getMaxLayerNameWidth() {
            return (this.getX() + this.getWidth() - 3f) - this.getLayerNameX();
        }

        public boolean isEyeButtonHovered() {
            return this.eyeButtonHovered;
        }

        public boolean isLayerNameHovered() {
            return this.layerNameHovered;
        }

        public boolean isEyeButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return UIBase.isXYInArea(mouseX, mouseY, this.getEyeButtonX(), this.getEyeButtonY(), this.getEyeButtonWidth(), this.getEyeButtonHeight());
        }

        public boolean isLayerNameMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return UIBase.isXYInArea(mouseX, mouseY, this.getLayerNameX(), this.getLayerNameY(), this.getMaxLayerNameWidth(), UIBase.getUITextHeightNormal());
        }

        public float getEyeButtonWidth() {
            return 30f;
        }

        public float getEyeButtonHeight() {
            return 28f;
        }

        public float getIndentOffset() {
            return (this.group != null) ? GROUP_INDENT : 0.0f;
        }

        public float getEyeButtonX() {
            return this.x + this.getIndentOffset();
        }

        public float getEyeButtonY() {
            return this.y;
        }

        public float getButtonWidth() {
            return 30f;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (this.isMouseOver(mouseX, mouseY) && !this.eyeButtonHovered) {
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
            if (button == 0 && this.dragStarted) {
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

                        // Important: Set initial target index to the same as the dragged entry
                        // This ensures no jump at the start
                        List<LayerElementEntry> entries = this.layerWidget.getLayerEntriesInUiOrder();
                        int currentIndex = entries.indexOf(this);
                        if (currentIndex < 0) {
                            currentIndex = 0;
                        }
                        this.layerWidget.dragTargetUiIndex = currentIndex;
                        this.layerWidget.dragTargetIndicatorY = this.getY();

                        return true;
                    }
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
                if (this.isEyeButtonHovered()) {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.layerWidget.editor.history.saveSnapshot();
                    if (!this.element.isSelected()) this.layerWidget.editor.deselectAllElements();
                    this.element.setSelected(true);
                    this.element.element.layerHiddenInEditor = !this.element.element.layerHiddenInEditor;
                } else {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));

                    // Proper multi-selection with CTRL handling
                    boolean ctrlDown = Screen.hasControlDown();

                    if (!ctrlDown) {
                        // Without CTRL, deselect all others first
                        this.layerWidget.editor.deselectAllElements();
                    } else if (this.element.isSelected()) {
                        // With CTRL and already selected, toggle this element (deselect it)
                        this.element.setSelected(false);
                        return;
                    }

                    // Select this element (unless CTRL was down and we just deselected it)
                    if (!this.element.isSelected()) {
                        this.element.setSelected(true);
                    }

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

    private class LayerGroupEntry extends ScrollAreaEntry {

        protected final Layout.LayerGroup group;
        protected final LayerLayoutEditorWidget layerWidget;
        protected Font font = Minecraft.getInstance().font;
        protected ExtendedEditBox editGroupNameBox;
        protected boolean displayEditGroupNameBox = false;
        protected boolean groupNameHovered = false;
        protected boolean eyeButtonHovered = false;
        protected boolean collapseButtonHovered = false;
        protected boolean deleteButtonHovered = false;
        protected long lastLeftClick = -1L;
        protected boolean dragStarted = false;
        protected double dragStartX;
        protected double dragStartY;
        private static final int DRAG_THRESHOLD = 3;

        public LayerGroupEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget, @NotNull Layout.LayerGroup group) {
            super(parent, 50, 28);
            this.group = group;
            this.layerWidget = layerWidget;
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.editGroupNameBox = new ExtendedEditBox(this.font, 0, 0, 0, 0, Component.empty()) {
                @Override
                public boolean keyPressed(int keycode, int scancode, int modifiers) {
                    if (this.isVisible() && LayerGroupEntry.this.displayEditGroupNameBox) {
                        if (keycode == InputConstants.KEY_ENTER) {
                            LayerGroupEntry.this.stopEditingGroupName();
                            return true;
                        }
                    }
                    return super.keyPressed(keycode, scancode, modifiers);
                }
            };
            this.editGroupNameBox.setVisible(false);
            this.editGroupNameBox.setMaxLength(10000);
            this.backgroundColorNormal = null;
            this.backgroundColorHover = null;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.groupNameHovered = this.isGroupNameMouseOver(mouseX, mouseY);
            this.eyeButtonHovered = this.isEyeButtonMouseOver(mouseX, mouseY);
            this.collapseButtonHovered = this.isCollapseButtonMouseOver(mouseX, mouseY);
            this.deleteButtonHovered = this.isDeleteButtonMouseOver(mouseX, mouseY);

            if (this.isGroupFullySelected()) {
                UIBase.fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), getElementHoverColor().getColorInt());
            } else if (this.isMouseOver(mouseX, mouseY)) {
                UIBase.fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), getElementHoverColor().getColorInt());
            }

            float eyeIconSize = getIconSize(this.getEyeButtonWidth(), this.getEyeButtonHeight(), LAYER_EYE_ICON_PADDING);
            float eyeIconX = this.getEyeButtonX() + (this.getEyeButtonWidth() - eyeIconSize) * 0.5f;
            float eyeIconY = this.getEyeButtonY() + (this.getEyeButtonHeight() - eyeIconSize) * 0.5f;
            float eyeAlpha = this.isGroupHidden() ? 0.3f : 1.0f;
            this.layerWidget.renderMaterialIcon(graphics, EYE_ICON, eyeIconX, eyeIconY, eyeIconSize, eyeIconSize, eyeAlpha);

            float collapseIconSize = getIconSize(this.getCollapseButtonWidth(), this.getCollapseButtonHeight(), LAYER_EYE_ICON_PADDING);
            float collapseIconX = this.getCollapseButtonX() + (this.getCollapseButtonWidth() - collapseIconSize) * 0.5f;
            float collapseIconY = this.getCollapseButtonY() + (this.getCollapseButtonHeight() - collapseIconSize) * 0.5f;
            float collapseAlpha = this.collapseButtonHovered ? 1.0f : 0.7f;
            this.layerWidget.renderMaterialIcon(graphics, this.group.collapsed ? GROUP_EXPAND_ICON : GROUP_COLLAPSE_ICON, collapseIconX, collapseIconY, collapseIconSize, collapseIconSize, collapseAlpha);

            float deleteIconSize = getIconSize(this.getDeleteButtonWidth(), this.getDeleteButtonHeight(), LAYER_EYE_ICON_PADDING);
            float deleteIconX = this.getDeleteButtonX() + (this.getDeleteButtonWidth() - deleteIconSize) * 0.5f;
            float deleteIconY = this.getDeleteButtonY() + (this.getDeleteButtonHeight() - deleteIconSize) * 0.5f;
            float deleteAlpha = this.deleteButtonHovered ? 1.0f : 0.7f;
            this.layerWidget.renderMaterialIcon(graphics, GROUP_DELETE_ICON, deleteIconX, deleteIconY, deleteIconSize, deleteIconSize, deleteAlpha);

            if (!this.displayEditGroupNameBox) {
                UIBase.renderText(graphics, Component.literal(this.getGroupName()), (int)this.getGroupNameX(), (int)this.getGroupNameY());
            } else {
                UIBase.applyDefaultWidgetSkinTo(this.editGroupNameBox);
                this.editGroupNameBox.setX((int)this.getGroupNameX());
                this.editGroupNameBox.setY((int)this.getGroupNameY() - 1);
                this.editGroupNameBox.setWidth((int)Math.min(this.getMaxGroupNameWidth(), UIBase.getUITextWidth(this.editGroupNameBox.getValue() + 13)));
                if (this.editGroupNameBox.getWidth() < this.getMaxGroupNameWidth()) {
                    this.editGroupNameBox.setDisplayPosition(0);
                }
                ((IMixinAbstractWidget)this.editGroupNameBox).setHeightFancyMenu((int)(UIBase.getUITextHeightNormal() + 2));
                this.editGroupNameBox.render(graphics, mouseX, mouseY, partial);
            }
        }

        public boolean isGroupNameHovered() {
            return this.groupNameHovered;
        }

        public boolean isEyeButtonHovered() {
            return this.eyeButtonHovered;
        }

        public boolean isCollapseButtonHovered() {
            return this.collapseButtonHovered;
        }

        public boolean isGroupNameMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return UIBase.isXYInArea(mouseX, mouseY, this.getGroupNameX(), this.getGroupNameY(), this.getMaxGroupNameWidth(), UIBase.getUITextHeightNormal());
        }

        public boolean isEyeButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return UIBase.isXYInArea(mouseX, mouseY, this.getEyeButtonX(), this.getEyeButtonY(), this.getEyeButtonWidth(), this.getEyeButtonHeight());
        }

        public boolean isCollapseButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return UIBase.isXYInArea(mouseX, mouseY, this.getCollapseButtonX(), this.getCollapseButtonY(), this.getCollapseButtonWidth(), this.getCollapseButtonHeight());
        }

        public boolean isDeleteButtonMouseOver(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return UIBase.isXYInArea(mouseX, mouseY, this.getDeleteButtonX(), this.getDeleteButtonY(), this.getDeleteButtonWidth(), this.getDeleteButtonHeight());
        }

        public float getGroupNameX() {
            float eyeIconSize = getIconSize(this.getEyeButtonWidth(), this.getEyeButtonHeight(), LAYER_EYE_ICON_PADDING);
            float eyeIconX = this.getEyeButtonX() + (this.getEyeButtonWidth() - eyeIconSize) * 0.5f;
            return eyeIconX + eyeIconSize + LAYER_ICON_TEXT_GAP;
        }

        public float getGroupNameY() {
            return this.getY() + (this.getHeight() / 2f) - (UIBase.getUITextHeightNormal() / 2f);
        }

        public float getMaxGroupNameWidth() {
            return (this.getX() + this.getWidth() - 3f) - this.getGroupNameX() - (this.getDeleteButtonWidth() + this.getCollapseButtonWidth());
        }

        public float getEyeButtonWidth() {
            return 30f;
        }

        public float getEyeButtonHeight() {
            return 28f;
        }

        public float getEyeButtonX() {
            return this.x + GROUP_NAME_LEFT_PADDING;
        }

        public float getEyeButtonY() {
            return this.y;
        }

        public float getCollapseButtonWidth() {
            return GROUP_COLLAPSE_BUTTON_WIDTH;
        }

        public float getCollapseButtonHeight() {
            return this.getHeight();
        }

        public float getCollapseButtonX() {
            return this.getDeleteButtonX() - this.getCollapseButtonWidth();
        }

        public float getCollapseButtonY() {
            return this.y;
        }

        public float getDeleteButtonWidth() {
            return GROUP_DELETE_BUTTON_WIDTH;
        }

        public float getDeleteButtonHeight() {
            return this.getHeight();
        }

        public float getDeleteButtonX() {
            return this.x + this.getWidth() - this.getDeleteButtonWidth();
        }

        public float getDeleteButtonY() {
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

        protected void startEditingGroupName() {
            this.editGroupNameBox.setVisible(true);
            this.editGroupNameBox.setFocused(true);
            this.editGroupNameBox.setValue(this.getGroupName());
            this.editGroupNameBox.moveCursorToEnd(false);
            this.displayEditGroupNameBox = true;
        }

        protected void stopEditingGroupName() {
            if (this.displayEditGroupNameBox) {
                String newName = this.editGroupNameBox.getValue();
                if (newName != null) {
                    newName = newName.trim();
                }
                if ((newName == null) || newName.isEmpty()) {
                    this.group.name = null;
                } else {
                    String defaultName = Component.translatable("fancymenu.editor.widgets.layers.group.default_name").getString();
                    if (newName.equals(defaultName)) {
                        this.group.name = null;
                    } else {
                        this.group.name = newName;
                    }
                }
            }
            this.editGroupNameBox.setFocused(false);
            this.editGroupNameBox.setVisible(false);
            this.displayEditGroupNameBox = false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (this.isMouseOver(mouseX, mouseY) && !this.deleteButtonHovered && !this.collapseButtonHovered && !this.eyeButtonHovered) {
                    this.dragStartX = mouseX;
                    this.dragStartY = mouseY;
                    this.dragStarted = true;
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        public boolean groupMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button == 0 && this.dragStarted) {
                double deltaX = mouseX - this.dragStartX;
                double deltaY = mouseY - this.dragStartY;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                if (distance > DRAG_THRESHOLD && !this.layerWidget.isDragging) {
                    this.layerWidget.draggedEntry = this;
                    this.layerWidget.isDragging = true;
                    this.layerWidget.dragTargetUiIndex = 0;
                    this.layerWidget.dragTargetIndicatorY = this.getY();
                    return true;
                }
                return true;
            }
            return false;
        }

        public boolean groupMouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) {
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
                if (this.eyeButtonHovered) {
                    List<AbstractEditorElement<?, ?>> elements = this.layerWidget.editor.getElementsInGroup(this.group);
                    if (!elements.isEmpty()) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
                    }
                } else if (this.collapseButtonHovered) {
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.layerWidget.editor.history.saveSnapshot();
                    this.group.collapsed = !this.group.collapsed;
                    MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                } else if (this.deleteButtonHovered) {
                    Dialogs.openMessageWithCallback(Component.translatable("fancymenu.editor.widgets.layers.group.delete.confirm"), MessageDialogStyle.WARNING, call -> {
                        if (call) {
                            this.layerWidget.editor.history.saveSnapshot();
                            this.layerWidget.editor.layout.layerGroups.remove(this.group);
                            MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                        }
                    });
                } else {
                    List<AbstractEditorElement<?, ?>> elements = this.layerWidget.editor.getElementsInGroup(this.group);
                    if (!elements.isEmpty()) {
                        if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        boolean ctrlDown = Screen.hasControlDown();
                        boolean allSelected = this.isGroupFullySelected();
                        if (!ctrlDown) {
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
                    if (!this.isGroupNameHovered()) {
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if ((this.lastLeftClick + 400) > now) {
                        this.startEditingGroupName();
                    }
                    this.lastLeftClick = now;
                }
            }
        }

    }

    private class VanillaLayerElementEntry extends ScrollAreaEntry {

        protected LayerLayoutEditorWidget layerWidget;
        protected boolean moveTopBottomButtonHovered = false;
        protected Font font = Minecraft.getInstance().font;

        public VanillaLayerElementEntry(ScrollArea parent, LayerLayoutEditorWidget layerWidget) {
            super(parent, 50, 28);
            this.layerWidget = layerWidget;
            this.playClickSound = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.backgroundColorNormal = null;
            this.backgroundColorHover = null;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.moveTopBottomButtonHovered = this.isMoveTopBottomButtonHovered(mouseX, mouseY);

            RenderSystem.enableBlend();

            MaterialIcon icon = this.layerWidget.editor.layout.renderElementsBehindVanilla ? MOVE_BEHIND_ICON : MOVE_TO_TOP_ICON;
            float vanillaIconSize = getIconSize(this.getButtonWidth(), this.getButtonHeight(), LAYER_EYE_ICON_PADDING);
            float vanillaIconX = this.x + (this.getButtonWidth() - vanillaIconSize) * 0.5f;
            float vanillaIconY = this.y + (this.getButtonHeight() - vanillaIconSize) * 0.5f;
            this.layerWidget.renderMaterialIcon(graphics, icon, vanillaIconX, vanillaIconY, vanillaIconSize, vanillaIconSize, 1.0f);

            UIBase.renderText(graphics, Component.translatable("fancymenu.editor.widgets.layers.vanilla_elements").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt())), (int)(vanillaIconX + vanillaIconSize + LAYER_ICON_TEXT_GAP), (int)(this.getY() + (this.getHeight() / 2f) - (UIBase.getUITextHeightNormal() / 2f)));

        }

        public boolean isMoveTopBottomButtonHovered() {
            return this.moveTopBottomButtonHovered;
        }

        public boolean isMoveTopBottomButtonHovered(double mouseX, double mouseY) {
            if (this.parent.isMouseInteractingWithGrabbers()) return false;
            if (!this.parent.isInnerAreaHovered()) return false;
            return UIBase.isXYInArea(mouseX, mouseY, this.x, this.y, this.getButtonWidth(), this.getButtonHeight());
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
                    if (FancyMenu.getOptions().playUiClickSounds.getValue()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.layerWidget.editor.history.saveSnapshot();
                    this.layerWidget.editor.layout.renderElementsBehindVanilla = !this.layerWidget.editor.layout.renderElementsBehindVanilla;
                    this.layerWidget.editor.deselectAllElements();
                    MainThreadTaskExecutor.executeInMainThread(() -> this.layerWidget.updateList(true), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
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
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderSystem.enableBlend();
            UIBase.fillF(graphics, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), getBorderColor().getColorInt());
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
