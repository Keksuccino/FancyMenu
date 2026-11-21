package de.keksuccino.fancymenu.customization.element.scrolling;

/**
 * The geometry of a scrollable element for one frame: what it shows (the viewport), how big its content is,
 * how far the content can be scrolled and where the content is on the screen.<br>
 * Plain math on the values it gets, so it knows nothing about elements or the game. Everything is in screen pixels.
 */
public final class ScrollGeometry {

    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int contentWidth;
    private int contentHeight;
    private int maxScrollX;
    private int maxScrollY;
    private int scrollX;
    private int scrollY;

    /**
     * Sets the part of the screen the element shows its content in.
     */
    public void setViewport(int x, int y, int width, int height) {
        this.viewportX = x;
        this.viewportY = y;
        this.viewportWidth = Math.max(0, width);
        this.viewportHeight = Math.max(0, height);
    }

    /**
     * Sets the size of the content. Has to be called after {@link #setViewport(int, int, int, int)}.<br>
     * The content is never smaller than the viewport, and on an axis that does not scroll it is exactly as big.
     *
     * @param scrollsX If the content can be wider than the viewport.
     * @param scrollsY If the content can be higher than the viewport.
     */
    public void setContentSize(int wantedWidth, int wantedHeight, boolean scrollsX, boolean scrollsY) {
        this.contentWidth = scrollsX ? Math.max(this.viewportWidth, wantedWidth) : this.viewportWidth;
        this.contentHeight = scrollsY ? Math.max(this.viewportHeight, wantedHeight) : this.viewportHeight;
        this.maxScrollX = this.contentWidth - this.viewportWidth;
        this.maxScrollY = this.contentHeight - this.viewportHeight;
    }

    /**
     * Sets how far the content is scrolled, in screen pixels.<br>
     * The content moves by whole screen pixels, like the lists of the game do. Everything in it that cuts what it draws,
     * like a text or a progress bar, cuts at whole screen pixels, and would cut next to what it draws in a content that sits between two of them.
     */
    public void setScroll(double positionX, double positionY) {
        this.scrollX = (int) Math.round(positionX);
        this.scrollY = (int) Math.round(positionY);
    }

    public int getViewportX() {
        return this.viewportX;
    }

    public int getViewportY() {
        return this.viewportY;
    }

    public int getViewportWidth() {
        return this.viewportWidth;
    }

    public int getViewportHeight() {
        return this.viewportHeight;
    }

    public int getContentWidth() {
        return this.contentWidth;
    }

    public int getContentHeight() {
        return this.contentHeight;
    }

    public int getMaxScrollX() {
        return this.maxScrollX;
    }

    public int getMaxScrollY() {
        return this.maxScrollY;
    }

    /**
     * Returns the X position of the left edge of the content on the screen.
     */
    public int getContentScreenX() {
        return this.viewportX - this.scrollX;
    }

    /**
     * Returns the Y position of the top edge of the content on the screen.
     */
    public int getContentScreenY() {
        return this.viewportY - this.scrollY;
    }

    public boolean isInViewport(double screenX, double screenY) {
        return (screenX >= this.viewportX) && (screenX < (this.viewportX + this.viewportWidth)) && (screenY >= this.viewportY) && (screenY < (this.viewportY + this.viewportHeight));
    }

}
