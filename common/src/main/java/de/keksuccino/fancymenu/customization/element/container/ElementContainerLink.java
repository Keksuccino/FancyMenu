package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The connection of an element to the {@link ElementContainer} it is a child of.<br>
 * Elements only have one of these while they are in a container, so elements of the main layout pay nothing for it.
 */
public final class ElementContainerLink {

    /** How long a container that could not be found stays unsearched. Searching walks all elements of the screen. */
    private static final long CONTAINER_LOOKUP_RETRY_MS = 500L;

    @NotNull
    private final AbstractElement element;
    @NotNull
    private String containerIdentifier;
    @Nullable
    private ElementContainer container;
    /** If something told the link what its container is, or that there is none. Until then, the link looks for it on its own. */
    private boolean connected = false;
    private long nextLookupTime = 0L;
    /** If the element reports the size it has on its own right now, instead of the one its container gave it. */
    private boolean reportsNaturalSize = false;
    /** Offset X and Y from the top-left corner of the content, width and height. */
    private final int[] managedBox = new int[4];
    private boolean hasManagedBox = false;
    private int managedDragOffsetX = 0;
    private int managedDragOffsetY = 0;
    private boolean movedByUser = false;

    public ElementContainerLink(@NotNull AbstractElement element, @NotNull String containerIdentifier) {
        this.element = element;
        this.containerIdentifier = containerIdentifier;
    }

    @NotNull
    public String getContainerIdentifier() {
        return this.containerIdentifier;
    }

    /**
     * Returns the container of the element, or NULL if there is no container with the identifier the element remembers.
     * An element without a container behaves like an element of the main layout.<br>
     * Screens connect all of their elements when they build them. Only elements nobody connected look for their container themselves.
     */
    @Nullable
    public ElementContainer getContainer() {
        if (this.connected) return this.container;
        long now = System.currentTimeMillis();
        if (now < this.nextLookupTime) return null;
        this.nextLookupTime = now + CONTAINER_LOOKUP_RETRY_MS;
        if (AbstractElement.getElementByInstanceIdentifier(this.containerIdentifier) instanceof ElementContainer found) {
            this.setContainer(found);
        }
        return this.container;
    }

    /**
     * Sets the container the identifier of the element resolved to, or NULL if there is none with that identifier.
     * Containers that would hold themselves, directly or through their own containers, get refused,
     * because such an element could never work out where it is.
     */
    public void setContainer(@Nullable ElementContainer container) {
        this.container = ((container != null) && !this.wouldHoldItself(container)) ? container : null;
        this.connected = true;
    }

    /**
     * Returns if the container is the element, or sits where it sits because of where the element is: in the element,
     * or anchored to it, directly or through other elements.
     */
    private boolean wouldHoldItself(@NotNull ElementContainer container) {
        AbstractElement containerElement = container.getContainerElement();
        return (containerElement == this.element) || ElementContainerHierarchy.dependsOn(containerElement, this.element);
    }

    /**
     * Gives the element the box its container worked out for it. Only containers that arrange their children call this.
     *
     * @param offsetX How far the left edge of the element is from the left edge of the content.
     * @param offsetY How far the top edge of the element is from the top edge of the content.
     */
    public void setManagedBox(int offsetX, int offsetY, int width, int height) {
        this.managedBox[0] = offsetX;
        this.managedBox[1] = offsetY;
        this.managedBox[2] = width;
        this.managedBox[3] = height;
        this.hasManagedBox = true;
    }

    /**
     * Forgets the box of the container, so the element is as big as it is on its own until it gets a new one.
     */
    public void clearManagedBox() {
        this.hasManagedBox = false;
    }

    public boolean hasManagedBox() {
        return this.hasManagedBox;
    }

    public int getManagedScreenX(@NotNull ElementContainer container) {
        return container.getContentScreenX() + this.managedBox[0] + this.managedDragOffsetX;
    }

    public int getManagedScreenY(@NotNull ElementContainer container) {
        return container.getContentScreenY() + this.managedBox[1] + this.managedDragOffsetY;
    }

    /**
     * Moves the element away from the box its container gave it, by the given distance on the screen.<br>
     * The layout editor does this while the user drags a child of a container that arranges its children. Such a child has
     * no place of its own to move to, so it follows the mouse like this until it gets dropped.
     */
    public void setManagedDragOffset(int screenOffsetX, int screenOffsetY) {
        this.managedDragOffsetX = screenOffsetX;
        this.managedDragOffsetY = screenOffsetY;
    }

    public int getManagedDragOffsetX() {
        return this.managedDragOffsetX;
    }

    public int getManagedDragOffsetY() {
        return this.managedDragOffsetY;
    }

    /**
     * Tells the link if the user moves the element around in the layout editor right now.<br>
     * An element that stays on the screen stays in the content of its container, which would keep the user from dragging it
     * out of the container. So nothing holds a child the user moves, until it gets dropped.
     */
    public void setMovedByUser(boolean movedByUser) {
        this.movedByUser = movedByUser;
    }

    public boolean isMovedByUser() {
        return this.movedByUser;
    }

    public int getManagedWidth() {
        return this.managedBox[2];
    }

    public int getManagedHeight() {
        return this.managedBox[3];
    }

    /**
     * Returns if the element reports the size it has on its own right now, before its container changes it.
     * Containers arrange their children with that size.
     */
    public boolean reportsNaturalSize() {
        return this.reportsNaturalSize;
    }

    /**
     * Sets if the element reports the size it has on its own and returns what it did before, to set it back afterwards.
     */
    public boolean setReportsNaturalSize(boolean reportsNaturalSize) {
        boolean previous = this.reportsNaturalSize;
        this.reportsNaturalSize = reportsNaturalSize;
        return previous;
    }

    public static int toScreenX(@NotNull ElementContainer container, int contentX) {
        return container.getContentScreenX() + contentX;
    }

    public static int toScreenY(@NotNull ElementContainer container, int contentY) {
        return container.getContentScreenY() + contentY;
    }

    public static int toContentX(@NotNull ElementContainer container, int screenX) {
        return screenX - container.getContentScreenX();
    }

    public static int toContentY(@NotNull ElementContainer container, int screenY) {
        return screenY - container.getContentScreenY();
    }

}
