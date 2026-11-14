package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
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

    /** The key elements save the identifier of their container under. */
    private static final String PARENT_CONTAINER_KEY = "parent_container";
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
     * Returns everything that is in the container, directly or in one of its children, in the order of the elements.
     */
    @NotNull
    public List<AbstractEditorElement<?, ?>> getDescendants(@NotNull AbstractEditorElement<?, ?> container) {
        List<AbstractEditorElement<?, ?>> descendants = new ArrayList<>();
        if (!(container.element instanceof ElementContainer elementContainer)) return descendants;
        for (AbstractEditorElement<?, ?> e : this.editor.normalEditorElements) {
            if (ElementContainerHierarchy.isInContainer(e.element, elementContainer)) descendants.add(e);
        }
        return descendants;
    }

    /**
     * Returns the elements with everything that is in the containers among them, each element only once.
     */
    @NotNull
    public List<AbstractEditorElement<?, ?>> withDescendants(@NotNull Collection<AbstractEditorElement<?, ?>> elements) {
        List<AbstractEditorElement<?, ?>> all = new ArrayList<>(elements);
        for (AbstractEditorElement<?, ?> e : elements) {
            for (AbstractEditorElement<?, ?> descendant : this.getDescendants(e)) {
                if (!all.contains(descendant)) all.add(descendant);
            }
        }
        return all;
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
        int step = above ? 1 : -1;
        for (int i = elements.indexOf(element) + step; (i >= 0) && (i < elements.size()); i += step) {
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
            String container = copy.getValue(PARENT_CONTAINER_KEY);
            if ((container != null) && newIdentifiers.containsKey(container)) copy.putProperty(PARENT_CONTAINER_KEY, newIdentifiers.get(container));
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
        element.moveToContainer(container, false);
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
     * Brings the containers and everything that shows the elements of the editor up to date, after elements got added, removed or moved between containers.
     */
    public void refreshAfterChange() {
        this.editor.updateContainerHierarchy();
        this.editor.updateLayerGroupElementOrder();
        for (AbstractLayoutEditorWidget widget : this.editor.layoutEditorWidgets) {
            widget.refresh();
        }
    }

}
