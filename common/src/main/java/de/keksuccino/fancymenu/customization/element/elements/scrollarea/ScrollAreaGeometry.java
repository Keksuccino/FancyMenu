package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

/**
 * The geometry of a scrollable area for one frame: what it shows (the viewport), how big its content is,
 * how far the content can be scrolled and where the content is on the screen.<br>
 * Plain math on the values it gets, so it knows nothing about elements or the game.<br><br>
 *
 * The viewport and the scroll are in screen pixels. The content size is in the units of the content,
 * which are screen pixels times the scale.
 */
public final class ScrollAreaGeometry {

    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private float scale = 1.0F;
    private int contentWidth;
    private int contentHeight;
    private int maxScrollX;
    private int maxScrollY;
    private int scrollX;
    private int scrollY;

    /**
     * Sets the part of the screen the area shows its content in, and how many screen pixels one unit of the content is.
     */
    public void setViewport(int x, int y, int width, int height, float scale) {
        this.viewportX = x;
        this.viewportY = y;
        this.viewportWidth = Math.max(0, width);
        this.viewportHeight = Math.max(0, height);
        this.scale = (scale > 0.0F) ? scale : 1.0F;
    }

    /**
     * Sets the size of the content, in the units of the content. Has to be called after {@link #setViewport(int, int, int, int, float)}.<br>
     * The content is never smaller than the viewport, and on an axis that does not scroll it is exactly as big.
     *
     * @param scrollsX If the content can be wider than the viewport.
     * @param scrollsY If the content can be higher than the viewport.
     */
    public void setContentSize(int wantedWidth, int wantedHeight, boolean scrollsX, boolean scrollsY) {
        int viewportContentWidth = this.toContentUnits(this.viewportWidth);
        int viewportContentHeight = this.toContentUnits(this.viewportHeight);
        this.contentWidth = scrollsX ? Math.max(viewportContentWidth, wantedWidth) : viewportContentWidth;
        this.contentHeight = scrollsY ? Math.max(viewportContentHeight, wantedHeight) : viewportContentHeight;
        // The range comes from the content the way it shows on the screen, so the last pixel of it can always be scrolled to
        this.maxScrollX = (this.contentWidth > viewportContentWidth) ? Math.max(0, Math.round(this.contentWidth * this.scale) - this.viewportWidth) : 0;
        this.maxScrollY = (this.contentHeight > viewportContentHeight) ? Math.max(0, Math.round(this.contentHeight * this.scale) - this.viewportHeight) : 0;
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

    private int toContentUnits(int screenPixels) {
        return (this.scale == 1.0F) ? screenPixels : Math.max(0, Math.round(screenPixels / this.scale));
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

    public float getScale() {
        return this.scale;
    }

    /**
     * Returns how many units of the content the viewport shows on the X axis.
     */
    public int getViewportContentWidth() {
        return this.toContentUnits(this.viewportWidth);
    }

    /**
     * Returns how many units of the content the viewport shows on the Y axis.
     */
    public int getViewportContentHeight() {
        return this.toContentUnits(this.viewportHeight);
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
