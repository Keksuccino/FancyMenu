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
    @NotNull
    private SizeSpace sizeSpace = SizeSpace.SCREEN;
    /** Screen offset X and Y from the content, screen width and height, content width and height. */
    private final int[] managedBox = new int[6];
    private boolean hasManagedBox = false;
    private int managedDragOffsetX = 0;
    private int managedDragOffsetY = 0;

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

    private boolean wouldHoldItself(@NotNull ElementContainer container) {
        AbstractElement current = container.getContainerElement();
        // Longer chains than this do not exist in layouts made in the editor, so reaching the limit means a loop
        for (int depth = 0; depth < 64; depth++) {
            if (current == this.element) return true;
            ElementContainer parent = current.getParentContainer();
            if (parent == null) return false;
            current = parent.getContainerElement();
        }
        return true;
    }

    /**
     * Gives the element the box its container worked out for it. Only containers that manage the boxes of their children call this.
     *
     * @param screenOffsetX How far the left edge of the element is from the left edge of the content, in screen pixels.
     * @param screenOffsetY How far the top edge of the element is from the top edge of the content, in screen pixels.
     * @param screenWidth The width of the element on the screen.
     * @param screenHeight The height of the element on the screen.
     * @param contentWidth The width of the element in the units of the content.
     * @param contentHeight The height of the element in the units of the content.
     */
    public void setManagedBox(int screenOffsetX, int screenOffsetY, int screenWidth, int screenHeight, int contentWidth, int contentHeight) {
        this.managedBox[0] = screenOffsetX;
        this.managedBox[1] = screenOffsetY;
        this.managedBox[2] = screenWidth;
        this.managedBox[3] = screenHeight;
        this.managedBox[4] = contentWidth;
        this.managedBox[5] = contentHeight;
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

    /**
     * Returns the width the container gave the element, in the space the element reports its size in right now.
     */
    public int getManagedWidth() {
        return (this.sizeSpace == SizeSpace.SCREEN) ? this.managedBox[2] : this.managedBox[4];
    }

    /**
     * Returns the height the container gave the element, in the space the element reports its size in right now.
     */
    public int getManagedHeight() {
        return (this.sizeSpace == SizeSpace.SCREEN) ? this.managedBox[3] : this.managedBox[5];
    }

    @NotNull
    public SizeSpace getSizeSpace() {
        return this.sizeSpace;
    }

    /**
     * Sets the space the element reports its width and height in and returns the one it was in before, to set it back afterwards.
     */
    @NotNull
    public SizeSpace setSizeSpace(@NotNull SizeSpace sizeSpace) {
        SizeSpace previous = this.sizeSpace;
        this.sizeSpace = sizeSpace;
        return previous;
    }

    /**
     * Returns if the element draws scaled by its container: the container scales what children draw, there is a scale to apply,
     * and the element draws in a way that follows the scale.
     */
    public static boolean drawsScaled(@NotNull AbstractElement element, @NotNull ElementContainer container) {
        return container.scalesChildVisuals() && (container.getContentScale() != 1.0F) && element.supportsPoseScaling();
    }

    public static int toScreenX(@NotNull ElementContainer container, int contentX) {
        return container.getContentScreenX() + Math.round(contentX * container.getContentScale());
    }

    public static int toScreenY(@NotNull ElementContainer container, int contentY) {
        return container.getContentScreenY() + Math.round(contentY * container.getContentScale());
    }

    public static int toScreenSize(@NotNull ElementContainer container, int contentSize) {
        float scale = container.getContentScale();
        return (scale == 1.0F) ? contentSize : Math.round(contentSize * scale);
    }

    public static int toContentX(@NotNull ElementContainer container, int screenX) {
        return Math.round((screenX - container.getContentScreenX()) / safeScale(container));
    }

    public static int toContentY(@NotNull ElementContainer container, int screenY) {
        return Math.round((screenY - container.getContentScreenY()) / safeScale(container));
    }

    public static int toContentSize(@NotNull ElementContainer container, int screenSize) {
        float scale = container.getContentScale();
        return (scale == 1.0F) ? screenSize : Math.round(screenSize / safeScale(container));
    }

    private static float safeScale(@NotNull ElementContainer container) {
        float scale = container.getContentScale();
        return (scale > 0.0F) ? scale : 1.0F;
    }

    /**
     * The spaces an element in a container can report its width and height in. Its position is the same in all of them.
     */
    public enum SizeSpace {

        /** The size the element has on the screen. This is what everything outside of the element works with. */
        SCREEN,
        /**
         * The size the element has in the units of the content, with what its container made of it.
         * Elements that draw scaled by their container are in this space while they render, so they draw
         * the way they do without a scale, and the scale of the container makes it the size on the screen.
         */
        CONTENT,
        /** The size the element has on its own, in the units of the content, before its container changes it. Containers lay out their children with it. */
        NATURAL

    }

}
