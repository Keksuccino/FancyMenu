package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
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
    protected final int[] box = new int[4];

    public ContainerDragDropHandler(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    /**
     * Marks the container the dragged elements would go into if the user dropped them right now, and the dragged elements it would not take.
     */
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.editor.isUserMovingElements()) return;
        List<AbstractEditorElement<?, ?>> dragged = this.editor.getCurrentlyDraggedElements();

        AbstractEditorElement<?, ?> target = this.findDropTarget(dragged, mouseX, mouseY);
        if (target == null) return;
        ContainerEditorElement container = (ContainerEditorElement) target;

        this.refused.clear();
        boolean takesAny = false;
        boolean placesAny = false;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if (this.movesWithDraggedContainer(e, dragged)) continue;
            if (!container.canHoldChild(e)) {
                this.refused.add(e);
            } else if (this.findDestination(e, target) == target) {
                placesAny = true;
                if (e.element.getParentContainer() != target.element) takesAny = true;
            }
        }

        if (!this.refused.isEmpty()) {
            long until = System.currentTimeMillis() + NOT_ALLOWED_MARK_MS;
            for (AbstractEditorElement<?, ?> e : this.refused) {
                e.renderMovingNotAllowedTime = Math.max(e.renderMovingNotAllowedTime, until);
            }
            // Vanilla elements never go into a container. Everything else only stays out of a container that sits where it sits because of that element.
            boolean onlyVanilla = true;
            for (AbstractEditorElement<?, ?> e : this.refused) {
                if (!(e instanceof VanillaWidgetEditorElement)) onlyVanilla = false;
            }
            TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines(onlyVanilla ? "fancymenu.editor.containers.drop.not_allowed" : "fancymenu.editor.containers.drop.not_allowed.dependency")), () -> true);
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
        if (placesAny) {
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
     * Puts the elements the user just dropped into what they got dropped on.<br>
     * The mouse picks the container. Every element goes into it if it reaches into what the container shows, and into the first
     * of the containers around it that it reaches into otherwise, so nothing ever ends up where it can't be seen.
     *
     * @param dragged The elements the user dragged until now.
     */
    public void onElementsDropped(@NotNull List<AbstractEditorElement<?, ?>> dragged, int mouseX, int mouseY) {

        if (!this.editor.isUserMovingElements()) return;

        AbstractEditorElement<?, ?> target = this.findDropTarget(dragged, mouseX, mouseY);

        // The place among the children has to be asked for before anything moves, while the dragged elements still are where the user sees them
        int dropIndex = (target != null) ? ((ContainerEditorElement) target).getDropIndex(mouseX, mouseY, dragged) : -1;

        // Where every dropped element ends up, in the order the elements have now. Has to be known for all of them before the first one moves.
        List<AbstractEditorElement<?, ?>> dropped = new ArrayList<>();
        List<AbstractEditorElement<?, ?>> destinations = new ArrayList<>();
        for (AbstractEditorElement<?, ?> e : this.editor.normalEditorElements) {
            if (!dragged.contains(e) || this.movesWithDraggedContainer(e, dragged)) continue;
            dropped.add(e);
            destinations.add(this.findDestination(e, target));
        }

        // What gets a place among the children of the container the mouse is over, and what lies on top of what it got dropped on
        List<AbstractEditorElement<?, ?>> placed = new ArrayList<>();
        List<AbstractEditorElement<?, ?>> onTop = new ArrayList<>();
        boolean anyChangedContainer = false;
        for (int i = 0; i < dropped.size(); i++) {
            AbstractEditorElement<?, ?> e = dropped.get(i);
            AbstractEditorElement<?, ?> destination = destinations.get(i);
            ElementContainer destinationContainer = toContainer(destination);
            boolean changesContainer = e.element.getParentContainer() != destinationContainer;
            if (changesContainer) {
                e.moveToContainer(destinationContainer);
                anyChangedContainer = true;
            }
            if ((dropIndex >= 0) && (destination == target)) {
                placed.add(e);
            } else if (changesContainer) {
                onTop.add(e);
            }
        }
        if (placed.isEmpty() && onTop.isEmpty()) return;

        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        List<AbstractEditorElement<?, ?>> orderBefore = anyChangedContainer ? null : new ArrayList<>(elements);
        if (!placed.isEmpty()) {
            AbstractEditorElement<?, ?> next = this.findChildAtDropIndex((ContainerEditorElement) target, dropIndex, dragged);
            elements.removeAll(placed);
            // In front of the child that comes after them in the container, or behind everything, from where the hierarchy puts them behind the last child
            elements.addAll((next != null) ? elements.indexOf(next) : elements.size(), placed);
        }
        // The hierarchy puts these behind their new container from the end of the list, as its last children
        elements.removeAll(onTop);
        elements.addAll(onTop);
        // Children that got dropped on the place they had already change nothing
        if ((orderBefore != null) && orderBefore.equals(elements)) return;

        this.editor.containerOperations.refreshAfterChange();

    }

    @Nullable
    private static ElementContainer toContainer(@Nullable AbstractEditorElement<?, ?> containerEditorElement) {
        return (containerEditorElement != null) ? (ElementContainer) containerEditorElement.element : null;
    }

    /**
     * Returns the container a dropped element ends up in when the mouse is over the given one, or NULL if it ends up in the main layout:
     * the first container, from the one the mouse is over outwards, that holds the element already, or that can hold it and that it reaches into.
     */
    @Nullable
    protected AbstractEditorElement<?, ?> findDestination(@NotNull AbstractEditorElement<?, ?> element, @Nullable AbstractEditorElement<?, ?> target) {
        AbstractEditorElement<?, ?> candidate = target;
        while ((candidate instanceof ContainerEditorElement candidateContainer) && (candidate.element instanceof ElementContainer container)) {
            // An element stays in the container it is in already, also where the container does not show it, like a child that is
            // scrolled out of sight while the user drags it along with others
            if (element.element.getParentContainer() == container) return candidate;
            if (candidateContainer.canHoldChild(element)) {
                this.box[0] = element.getX();
                this.box[1] = element.getY();
                this.box[2] = this.box[0] + element.getWidth();
                this.box[3] = this.box[1] + element.getHeight();
                if (container.clipToShownArea(this.box)) return candidate;
            }
            ElementContainer parent = candidate.element.getParentContainer();
            candidate = (parent != null) ? this.editor.getElementByInstanceIdentifier(parent.getContainerElement().getInstanceIdentifier()) : null;
        }
        return null;
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

}
