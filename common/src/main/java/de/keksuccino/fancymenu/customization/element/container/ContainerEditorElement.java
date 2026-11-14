package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
     * Renders a child of this container the way the container shows it, but without cutting anything of it off.
     * The editor shows children like this while the user drags them, so they can be seen leaving the container.
     */
    void extractChildRenderStateUncut(@NotNull AbstractEditorElement<?, ?> child, @NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

    /**
     * Returns the X position of the area of this container on the screen that elements get dropped on to become its children.
     */
    int getDropAreaX();

    int getDropAreaY();

    int getDropAreaWidth();

    int getDropAreaHeight();

}
