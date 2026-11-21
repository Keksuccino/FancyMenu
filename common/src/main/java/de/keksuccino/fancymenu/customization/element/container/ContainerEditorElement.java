package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * What an {@link ElementContainer} is in the layout editor. The editor renders its children through it,
 * so they show the way the container shows them, with everything the editor adds to elements.
 */
public interface ContainerEditorElement {

    /**
     * Gives the container its children as what they are in the editor, in the order they render in. Gets called whenever
     * the elements of the editor changed. The list belongs to the caller, so the container has to copy what it keeps.
     */
    void setChildEditorElements(@NotNull List<AbstractEditorElement<?, ?>> children);

    /**
     * Returns if the element can become a child of this container.
     */
    boolean canHoldChild(@NotNull AbstractEditorElement<?, ?> element);

    /**
     * Returns the place elements dropped at the given position get among the children of this container: the number of children
     * that come before them, not counting the dragged ones. Returns -1 for a container that leaves the place of its children to them.
     *
     * @param dragged The elements the user drags right now, which can be children of this container already.
     */
    int getDropIndex(int mouseX, int mouseY, @NotNull List<AbstractEditorElement<?, ?>> dragged);

    /**
     * Works out the line that shows where dropped elements would go.
     *
     * @param dropIndex The place from {@link #getDropIndex(int, int, List)}.
     * @param marker Gets filled with the X, Y, width and height of the line on the screen.
     * @return If there is a line to show.
     */
    boolean getDropMarker(int dropIndex, @NotNull List<AbstractEditorElement<?, ?>> dragged, int @NotNull [] marker);

    /**
     * Returns the children of this container as what they are in the editor, in the order they render in.
     * The list belongs to the container and must not be changed.
     */
    @NotNull
    List<AbstractEditorElement<?, ?>> getChildEditorElements();

    /**
     * Returns the X position of the area of this container on the screen that elements get dropped on to become its children.
     */
    int getDropAreaX();

    int getDropAreaY();

    int getDropAreaWidth();

    int getDropAreaHeight();

}
