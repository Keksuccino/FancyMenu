package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * An element that holds other elements as its children and shows them in a space of its own: the content.<br>
 * The content is what the anchors, the stretching and the stay-on-screen logic of children refer to instead of the screen.
 * It can be bigger than what the container shows of it, it can be moved (scrolled) and it can be scaled.<br><br>
 *
 * Positions and sizes of children that leave {@link AbstractElement} are always in screen space, so everything that works
 * with elements on the screen keeps working for children. Children calculate their position in the units of the content
 * and convert it with the values the container gives them here.<br><br>
 *
 * All of these get called many times per frame for every child, so implementations have to answer from values they
 * worked out once per frame.
 */
public interface ElementContainer {

    /**
     * Returns the element this container is.
     */
    @NotNull
    AbstractElement getContainerElement();

    /**
     * Gives the container its children, in the order they render in. Gets called whenever the elements of a screen got built
     * or the children of a container changed. The list belongs to the caller, so the container has to copy what it keeps.
     */
    void setContainerChildren(@NotNull List<? extends AbstractElement> children);

    /**
     * Returns the width of the content, in the units of the content.
     */
    int getContentWidth();

    /**
     * Returns the height of the content, in the units of the content.
     */
    int getContentHeight();

    /**
     * Returns the X position the left edge of the content has on the screen right now.
     */
    int getContentScreenX();

    /**
     * Returns the Y position the top edge of the content has on the screen right now.
     */
    int getContentScreenY();

    /**
     * Returns how many screen pixels one unit of the content is. Includes the scale of all containers this container is in.
     */
    float getContentScale();

    /**
     * Returns if the content scale also scales what children draw, like a picture of the content getting scaled.
     * If not, only the positions and sizes of children follow the scale.
     */
    boolean scalesChildVisuals();

    /**
     * Returns if this container decides where its children are and how big they are, instead of the children themselves.<br>
     * A container that does gives every child its box through {@link ElementContainerLink#setManagedBox(int, int, int, int, int, int)}.
     */
    boolean managesChildBoxes();

    /**
     * Returns if this container decides where its children are in a way that leaves the user no place to put them:
     * it arranges them one after another, in the order they have. Children of such a container can only be put in another order.
     */
    boolean arrangesChildren();

    /**
     * Returns if the point on the screen lies in the part of the content the container shows, and in the shown part of
     * every container this container is in. Everything of a child outside of it is cut off.
     */
    boolean isScreenPointVisible(double screenX, double screenY);

    /**
     * Cuts a rectangle on the screen down to what the container shows of its content, in this container and in every container it is in.
     *
     * @param area The left, top, right and bottom edge of the rectangle, which get changed to the edges of the part that shows.
     * @return If any part of the rectangle shows.
     */
    boolean clipToShownArea(int @NotNull [] area);

    /**
     * Returns if the container shows its children right now. Children of a container that does not show them
     * do not get rendered and must not react to input.
     */
    boolean isShowingChildren();

    /**
     * Returns the opacity children get multiplied with, between 0 and 1.
     */
    float getChildOpacity();

}
