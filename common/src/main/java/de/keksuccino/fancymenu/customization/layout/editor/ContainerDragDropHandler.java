package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Moves elements into and out of containers when the user drags them in the layout editor.<br>
 * An element that gets dropped on a container becomes its child, and a child that gets dropped anywhere else
 * becomes an element of what it got dropped on: another container or the main layout.
 * While the user drags, the container that would take the elements is marked.
 */
public class ContainerDragDropHandler {

    private static final int DROP_TARGET_FILL_ALPHA = 40;
    /** How long the mark of an element that can't go into a container stays after the last frame it got set in. Longer than a frame, so it never flickers. */
    private static final long NOT_ALLOWED_MARK_MS = 150L;

    private static final int DROP_MARKER_THICKNESS = 2;

    @NotNull
    protected final LayoutEditorScreen editor;
    protected final List<AbstractEditorElement<?, ?>> refused = new ArrayList<>();
    protected final int[] dropMarker = new int[4];

    public ContainerDragDropHandler(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    /**
     * Marks the container the dragged elements would go into if the user dropped them right now, and the dragged elements it would not take.
     */
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        List<AbstractEditorElement<?, ?>> dragged = this.editor.getCurrentlyDraggedElements();
        if (!this.isMoving(dragged)) return;

        AbstractEditorElement<?, ?> target = this.findDropTarget(dragged, mouseX, mouseY);
        if (target == null) return;
        ContainerEditorElement container = (ContainerEditorElement) target;

        this.refused.clear();
        boolean takesAny = false;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if (this.movesWithDraggedContainer(e, dragged)) continue;
            if (container.canHoldChild(e)) {
                if (e.element.getParentContainer() != target.element) takesAny = true;
            } else {
                this.refused.add(e);
            }
        }

        if (!this.refused.isEmpty()) {
            long until = System.currentTimeMillis() + NOT_ALLOWED_MARK_MS;
            for (AbstractEditorElement<?, ?> e : this.refused) {
                e.renderMovingNotAllowedTime = Math.max(e.renderMovingNotAllowedTime, until);
            }
            TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.containers.drop.not_allowed")), () -> true);
        }

        int color = UIBase.getUITheme().layout_editor_element_border_color_selected.getColorInt();
        if (takesAny) {
            int x = container.getDropAreaX();
            int y = container.getDropAreaY();
            int width = container.getDropAreaWidth();
            int height = container.getDropAreaHeight();
            graphics.fill(x, y, x + width, y + height, RenderingUtils.replaceAlphaInColor(color, DROP_TARGET_FILL_ALPHA));
            UIBase.renderBorder(graphics, x, y, x + width, y + height, 1, UIBase.getUITheme().layout_editor_element_border_color_selected.getColor(), true, true, true, true);
        }

        // A container that arranges its children shows where the dragged ones would go, also when they are its children already and only change their place
        if (this.refused.size() < dragged.size()) {
            int dropIndex = container.getDropIndex(mouseX, mouseY, dragged);
            if ((dropIndex >= 0) && container.getDropMarker(dropIndex, dragged, this.dropMarker)) {
                int x = this.dropMarker[0];
                int y = this.dropMarker[1];
                graphics.fill(x, y, x + Math.max(DROP_MARKER_THICKNESS, this.dropMarker[2]), y + Math.max(DROP_MARKER_THICKNESS, this.dropMarker[3]), color);
            }
        }

        RenderingUtils.resetShaderColor(graphics);

    }

    /**
     * Puts the elements the user just dropped into what they got dropped on.
     *
     * @param dragged The elements the user dragged until now.
     */
    public void onElementsDropped(@NotNull List<AbstractEditorElement<?, ?>> dragged, int mouseX, int mouseY) {

        if (!this.isMoving(dragged)) return;

        AbstractEditorElement<?, ?> target = this.findDropTarget(dragged, mouseX, mouseY);
        ElementContainer targetContainer = (target != null) ? (ElementContainer) target.element : null;

        // The place among the children has to be asked for before anything moves, while the dragged elements still are where the user sees them
        int dropIndex = (target != null) ? ((ContainerEditorElement) target).getDropIndex(mouseX, mouseY, dragged) : -1;

        // Everything that ends up in what the elements got dropped on, in the order it has now: what comes in new, and what only gets a new place in it
        List<AbstractEditorElement<?, ?>> placed = new ArrayList<>();
        boolean changed = false;
        for (AbstractEditorElement<?, ?> e : this.editor.normalEditorElements) {
            if (!dragged.contains(e) || this.movesWithDraggedContainer(e, dragged)) continue;
            if (e.element.getParentContainer() == targetContainer) {
                if (dropIndex >= 0) placed.add(e);
                continue;
            }
            if ((target != null) && !((ContainerEditorElement) target).canHoldChild(e)) continue;
            e.moveToContainer(targetContainer);
            placed.add(e);
            changed = true;
        }
        if (placed.isEmpty()) return;

        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        AbstractEditorElement<?, ?> next = (dropIndex >= 0) ? this.findChildAtDropIndex((ContainerEditorElement) target, dropIndex, dragged) : null;
        int oldFirstIndex = elements.indexOf(placed.get(0));
        elements.removeAll(placed);
        if (next != null) {
            // In front of the child that comes after them in the container
            elements.addAll(elements.indexOf(next), placed);
        } else {
            // What got dropped lies on top of what it got dropped on, and the hierarchy puts it behind its new container from there
            elements.addAll(placed);
        }
        if (!changed && (elements.indexOf(placed.get(0)) == oldFirstIndex)) return;
        this.editor.updateContainerHierarchy();
        this.editor.updateLayerGroupElementOrder();
        for (AbstractLayoutEditorWidget widget : this.editor.layoutEditorWidgets) {
            widget.refresh();
        }

    }

    /**
     * Returns the child of the container that has the given place among its children when the dragged ones are not counted,
     * or NULL if the place is behind the last child.
     */
    @Nullable
    protected AbstractEditorElement<?, ?> findChildAtDropIndex(@NotNull ContainerEditorElement container, int dropIndex, @NotNull List<AbstractEditorElement<?, ?>> dragged) {
        int index = 0;
        for (AbstractEditorElement<?, ?> child : container.getChildEditorElements()) {
            if (dragged.contains(child)) continue;
            if (index == dropIndex) return child;
            index++;
        }
        return null;
    }

    /**
     * Returns the container the mouse is over that could take the dragged elements, or NULL if they would go into the main layout.<br>
     * Children come behind their container in the order of the elements, so a container in another container gets found before the outer one.
     */
    @Nullable
    protected AbstractEditorElement<?, ?> findDropTarget(@NotNull List<AbstractEditorElement<?, ?>> dragged, int mouseX, int mouseY) {
        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        for (int i = elements.size() - 1; i >= 0; i--) {
            AbstractEditorElement<?, ?> e = elements.get(i);
            if (!(e instanceof ContainerEditorElement) || !(e.element instanceof ElementContainer container)) continue;
            if (e.element.layerHiddenInEditor) continue;
            if (dragged.contains(e) || this.movesWithDraggedContainer(e, dragged)) continue;
            if (container.isShowingChildren() && container.isScreenPointVisible(mouseX, mouseY)) return e;
        }
        return null;
    }

    /**
     * Returns if the element is in a container the user drags along with it. Such an element moves with its container and stays in it.
     */
    public boolean movesWithDraggedContainer(@NotNull AbstractEditorElement<?, ?> element, @NotNull List<AbstractEditorElement<?, ?>> dragged) {
        if (element.element.getParentContainer() == null) return false;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if ((e != element) && (e.element instanceof ElementContainer container) && ElementContainerHierarchy.isInContainer(element.element, container)) return true;
        }
        return false;
    }

    /**
     * Returns if the user moves the elements around, which is the only kind of dragging that drops them somewhere.
     */
    protected boolean isMoving(@NotNull List<AbstractEditorElement<?, ?>> dragged) {
        if (dragged.isEmpty()) return false;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if (e.isGettingResized() || e.isGettingRotated() || e.isGettingTilted()) return false;
        }
        return true;
    }

}
