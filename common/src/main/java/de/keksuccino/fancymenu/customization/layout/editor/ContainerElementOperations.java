package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What the layout editor does with elements that works differently once containers are involved: copying and deleting a container
 * takes its children along, children can be taken out of their container, and the order of elements is one of siblings.
 */
public class ContainerElementOperations {

    private static final String INSTANCE_IDENTIFIER_KEY = "instance_identifier";
    /** How far from the top left of what a container shows an element gets put that would not show in it otherwise. */
    private static final int VISIBLE_PLACEMENT_INSET = 6;

    @NotNull
    protected final LayoutEditorScreen editor;
    protected final int[] box = new int[4];

    public ContainerElementOperations(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    /**
     * Returns the elements with everything that is in the containers among them, each element only once.<br>
     * They come in the order the editor has its elements in, so what gets made from them, like copies, has the layers in the same order.
     * Elements the editor keeps somewhere else, like vanilla elements, come last.
     */
    @NotNull
    public List<AbstractEditorElement<?, ?>> withDescendants(@NotNull Collection<AbstractEditorElement<?, ?>> elements) {
        List<AbstractEditorElement<?, ?>> all = new ArrayList<>();
        for (AbstractEditorElement<?, ?> e : this.editor.normalEditorElements) {
            if (elements.contains(e) || this.isInAnyOf(e, elements)) all.add(e);
        }
        for (AbstractEditorElement<?, ?> e : elements) {
            if (!all.contains(e)) all.add(e);
        }
        return all;
    }

    protected boolean isInAnyOf(@NotNull AbstractEditorElement<?, ?> element, @NotNull Collection<AbstractEditorElement<?, ?>> containers) {
        if (element.element.getParentContainer() == null) return false;
        for (AbstractEditorElement<?, ?> e : containers) {
            if ((e.element instanceof ElementContainer container) && ElementContainerHierarchy.isInContainer(element.element, container)) return true;
        }
        return false;
    }

    /**
     * Returns the element that comes next to the given one among the elements of the same container, or of the main layout.
     *
     * @param above If the element one layer above is asked for. Otherwise it is the one a layer below.
     */
    @Nullable
    public AbstractEditorElement<?, ?> getSibling(@NotNull AbstractEditorElement<?, ?> element, boolean above) {
        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        ElementContainer container = element.element.getParentContainer();
        int index = elements.indexOf(element);
        // An element the editor does not have among these, like a vanilla element, has no place in their order
        if (index < 0) return null;
        int step = above ? 1 : -1;
        for (int i = index + step; (i >= 0) && (i < elements.size()); i += step) {
            if (elements.get(i).element.getParentContainer() == container) return elements.get(i);
        }
        return null;
    }

    /**
     * Gives copies of elements new identifiers, and makes copies of children the children of the copy of their container.
     * Children that got copied without their container stay children of the one they had.
     *
     * @param identifiers Makes the new identifiers, a new one every time it gets asked.
     */
    public static void assignNewIdentifiers(@NotNull Collection<SerializedElement> copies, @NotNull Supplier<String> identifiers) {
        Map<String, String> newIdentifiers = new HashMap<>();
        for (SerializedElement copy : copies) {
            String newIdentifier = identifiers.get();
            String oldIdentifier = copy.getValue(INSTANCE_IDENTIFIER_KEY);
            if (oldIdentifier != null) newIdentifiers.put(oldIdentifier, newIdentifier);
            copy.putProperty(INSTANCE_IDENTIFIER_KEY, newIdentifier);
        }
        for (SerializedElement copy : copies) {
            String container = copy.getValue(ElementBuilder.PARENT_CONTAINER_KEY);
            if ((container != null) && newIdentifiers.containsKey(container)) copy.putProperty(ElementBuilder.PARENT_CONTAINER_KEY, newIdentifiers.get(container));
        }
    }

    /**
     * Deletes the elements. Containers among them that still hold elements the user did not pick make the editor ask first
     * what happens to those: they go with their container, or they stay and move into what their container was in.
     */
    public void deleteElements(@NotNull List<AbstractEditorElement<?, ?>> elements) {
        if (elements.isEmpty()) return;
        List<AbstractEditorElement<?, ?>> contents = new ArrayList<>();
        for (AbstractEditorElement<?, ?> descendant : this.withDescendants(elements)) {
            if (!elements.contains(descendant)) contents.add(descendant);
        }
        if (contents.isEmpty()) {
            this.editor.history.saveSnapshot();
            this.delete(elements);
            return;
        }
        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.editor.containers.delete.confirm", String.valueOf(contents.size())), MessageDialogStyle.WARNING, deleteContents -> {
            if (!deleteContents) return;
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement<?, ?>> all = new ArrayList<>(elements);
            all.addAll(contents);
            this.delete(all);
        }).setAcceptLabel(Component.translatable("fancymenu.editor.containers.delete.contents")).setAlternative(Component.translatable("fancymenu.editor.containers.delete.keep_contents"), () -> {
            this.editor.history.saveSnapshot();
            // Only the children of what gets deleted have to move. What is in one of them moves along with it.
            for (AbstractEditorElement<?, ?> content : contents) {
                ElementContainer parent = content.element.getParentContainer();
                if ((parent == null) || !this.isAmong(parent, elements)) continue;
                this.moveToContainerVisibly(content, this.findSurvivingContainer(parent, elements));
            }
            this.delete(elements);
        });
    }

    protected void delete(@NotNull List<AbstractEditorElement<?, ?>> elements) {
        for (AbstractEditorElement<?, ?> e : elements) {
            e.deleteElement();
        }
        this.refreshAfterChange();
    }

    protected boolean isAmong(@NotNull ElementContainer container, @NotNull List<AbstractEditorElement<?, ?>> elements) {
        for (AbstractEditorElement<?, ?> e : elements) {
            if (e.element == container.getContainerElement()) return true;
        }
        return false;
    }

    /**
     * Returns the first container around the given one that is not one of the elements that get deleted, or NULL if there is none.
     */
    @Nullable
    protected ElementContainer findSurvivingContainer(@NotNull ElementContainer container, @NotNull List<AbstractEditorElement<?, ?>> deleted) {
        ElementContainer parent = container.getContainerElement().getParentContainer();
        while ((parent != null) && this.isAmong(parent, deleted)) {
            parent = parent.getContainerElement().getParentContainer();
        }
        return parent;
    }

    /**
     * Takes the element out of its container and puts it into what the container is in: another container or the main layout.
     */
    public void moveOutOfContainer(@NotNull AbstractEditorElement<?, ?> element) {
        ElementContainer container = element.element.getParentContainer();
        if (container == null) return;
        this.editor.history.saveSnapshot();
        this.moveToContainerVisibly(element, container.getContainerElement().getParentContainer());
        // What comes out of a container lies on top of it
        this.editor.normalEditorElements.remove(element);
        this.editor.normalEditorElements.add(element);
        this.refreshAfterChange();
    }

    /**
     * Makes the element a child of the given container, or an element of the main layout if it is NULL, for everything that
     * moves it there without the user having put it somewhere on the screen. The element stays where it is if any of it shows there.
     * If not, it moves to where it can be seen: to the top left of what its new container shows, or of the container it came out of.<br>
     * The editor has to update its container hierarchy afterwards.
     */
    public void moveToContainerVisibly(@NotNull AbstractEditorElement<?, ?> element, @Nullable ElementContainer container) {
        ElementContainer oldContainer = element.element.getParentContainer();
        if (oldContainer == container) return;
        // Where the element goes if nothing of it shows where it is: next to where the user saw it last, or where its new container is
        int fallbackX = (oldContainer != null) ? oldContainer.getContainerElement().getAbsoluteX() : 0;
        int fallbackY = (oldContainer != null) ? oldContainer.getContainerElement().getAbsoluteY() : 0;
        element.moveToContainer(container);
        if (this.editor.getElementByInstanceIdentifier((container != null) ? container.getContainerElement().getInstanceIdentifier() : "") instanceof ContainerEditorElement containerEditorElement) {
            this.box[0] = element.getX();
            this.box[1] = element.getY();
            this.box[2] = this.box[0] + element.getWidth();
            this.box[3] = this.box[1] + element.getHeight();
            if (!container.clipToShownArea(this.box)) element.moveToScreenPosition(containerEditorElement.getDropAreaX() + VISIBLE_PLACEMENT_INSET, containerEditorElement.getDropAreaY() + VISIBLE_PLACEMENT_INSET);
        } else if ((element.getX() >= this.editor.width) || (element.getY() >= this.editor.height) || ((element.getX() + element.getWidth()) <= 0) || ((element.getY() + element.getHeight()) <= 0)) {
            element.moveToScreenPosition(Math.max(0, fallbackX), Math.max(0, fallbackY));
        }
    }

    /**
     * Replaces the anchors of elements that are anchored to an element of another container, or of the main layout while they are in a container.
     * An element can only follow one that is in the same content, and elements end up like this when one of two that are anchored
     * to each other gets moved into or out of a container. Every element stays where it is on the screen.<br>
     * Has to be called after everything that changes its container in one go did, so elements that move together stay anchored to each other.
     */
    public void detachAnchorsAcrossContainers() {
        for (AbstractEditorElement<?, ?> e : this.editor.getAllElements()) {
            if (e.element.anchorPoint != ElementAnchorPoints.ELEMENT) continue;
            AbstractElement anchor = e.element.getElementAnchorPointParent();
            if ((anchor == null) || (anchor.getParentContainer() == e.element.getParentContainer())) continue;
            int screenX = e.getX();
            int screenY = e.getY();
            // The top left is the one anchor that never moves a child when the content of its container changes its size
            e.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
            e.element.setAnchorPointElementIdentifier(null);
            e.moveToScreenPosition(screenX, screenY);
        }
    }

    /**
     * Brings the containers and everything that shows the elements of the editor up to date, after elements got added, removed or moved between containers.
     */
    public void refreshAfterChange() {
        this.detachAnchorsAcrossContainers();
        this.editor.updateContainerHierarchy();
        this.editor.updateLayerGroupElementOrder();
        for (AbstractLayoutEditorWidget widget : this.editor.layoutEditorWidgets) {
            widget.refresh();
        }
    }

}
