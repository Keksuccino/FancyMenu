package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Keeps lists of elements in the order containers need: every child sits behind its container, with all children of
 * one container in a row, in the order they had before. Children of children follow their own container the same way.<br>
 * This one order is the order elements render in, the order they get input in, the order the layer list shows
 * and the order containers arrange their children in, so nothing has to work out the hierarchy on its own.
 */
public final class ElementContainerHierarchy {

    private ElementContainerHierarchy() {
    }

    /**
     * Connects every element that remembers a container to that container, if it is in the list.
     * Elements that remember a container which is not in the list stay unconnected and behave like elements of the main layout.
     */
    public static void connect(@NotNull List<? extends AbstractElement> elements) {
        Map<String, ElementContainer> containers = null;
        for (int i = 0; i < elements.size(); i++) {
            AbstractElement element = elements.get(i);
            if (element.getParentContainerIdentifier() == null) continue;
            if (containers == null) containers = collectContainers(elements, Function.identity());
            element.setParentContainer(containers.get(element.getParentContainerIdentifier()));
        }
    }

    /**
     * Gives every container in the list its children from the list, in the order of the list.
     * Containers without children get an empty list, so children they had before are gone for them.
     */
    public static void assignChildren(@NotNull List<? extends AbstractElement> elements) {
        List<AbstractElement> children = null;
        for (int i = 0; i < elements.size(); i++) {
            if (!(elements.get(i) instanceof ElementContainer container)) continue;
            if (children == null) {
                children = new ArrayList<>();
            } else {
                children.clear();
            }
            for (int j = 0; j < elements.size(); j++) {
                AbstractElement element = elements.get(j);
                if (element.getParentContainer() == container) children.add(element);
            }
            container.setContainerChildren(children);
        }
    }

    /**
     * Moves every child directly behind its container. Does nothing to lists without children.
     *
     * @param elementOf Returns the element of an item of the list, for lists that hold something that wraps elements.
     */
    public static <T> void sortChildrenBehindContainers(@NotNull List<T> items, @NotNull Function<T, AbstractElement> elementOf) {

        boolean hasChildren = false;
        for (int i = 0; i < items.size(); i++) {
            if (elementOf.apply(items.get(i)).getParentContainerIdentifier() != null) {
                hasChildren = true;
                break;
            }
        }
        if (!hasChildren) return;

        Map<String, ElementContainer> containers = collectContainers(items, elementOf);
        Map<String, List<T>> childrenByContainer = new HashMap<>();
        List<T> roots = new ArrayList<>(items.size());
        for (T item : items) {
            String containerId = elementOf.apply(item).getParentContainerIdentifier();
            if ((containerId != null) && containers.containsKey(containerId)) {
                childrenByContainer.computeIfAbsent(containerId, key -> new ArrayList<>()).add(item);
            } else {
                roots.add(item);
            }
        }

        List<T> sorted = new ArrayList<>(items.size());
        for (T root : roots) {
            addWithChildren(root, elementOf, childrenByContainer, sorted);
        }
        // Containers that hold each other never get reached from a root. They stay in the list, behind everything else.
        if (sorted.size() < items.size()) {
            for (T item : items) {
                if (!sorted.contains(item)) sorted.add(item);
            }
        }

        items.clear();
        items.addAll(sorted);

    }

    private static <T> void addWithChildren(@NotNull T item, @NotNull Function<T, AbstractElement> elementOf, @NotNull Map<String, List<T>> childrenByContainer, @NotNull List<T> sorted) {
        sorted.add(item);
        List<T> children = childrenByContainer.remove(elementOf.apply(item).getInstanceIdentifier());
        if (children == null) return;
        for (T child : children) {
            addWithChildren(child, elementOf, childrenByContainer, sorted);
        }
    }

    @NotNull
    private static <T> Map<String, ElementContainer> collectContainers(@NotNull List<? extends T> items, @NotNull Function<T, AbstractElement> elementOf) {
        Map<String, ElementContainer> containers = new HashMap<>();
        for (T item : items) {
            AbstractElement element = elementOf.apply(item);
            if (element instanceof ElementContainer container) containers.put(element.getInstanceIdentifier(), container);
        }
        return containers;
    }

    /**
     * Collects the direct children of the container from the list, in the order of the list.
     *
     * @param target The list the children get added to.
     */
    public static <T> void collectChildren(@NotNull ElementContainer container, @NotNull List<T> items, @NotNull Function<T, AbstractElement> elementOf, @NotNull List<T> target) {
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            if (elementOf.apply(item).getParentContainer() == container) target.add(item);
        }
    }

    /**
     * Returns if the element is in the container, directly or as a child of one of its children.
     */
    public static boolean isInContainer(@NotNull AbstractElement element, @NotNull ElementContainer container) {
        ElementContainer parent = element.getParentContainer();
        for (int depth = 0; (parent != null) && (depth < 64); depth++) {
            if (parent == container) return true;
            parent = parent.getContainerElement().getParentContainer();
        }
        return false;
    }

    /**
     * Returns the container that holds the element and is in no container itself, or NULL if the element is in no container.
     */
    @Nullable
    public static ElementContainer getOutermostContainer(@NotNull AbstractElement element) {
        ElementContainer outermost = null;
        ElementContainer parent = element.getParentContainer();
        for (int depth = 0; (parent != null) && (depth < 64); depth++) {
            outermost = parent;
            parent = parent.getContainerElement().getParentContainer();
        }
        return outermost;
    }

}
