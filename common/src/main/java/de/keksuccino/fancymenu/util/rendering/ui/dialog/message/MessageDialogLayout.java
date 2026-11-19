package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

/**
 * Where the parts of a {@link MessageDialogBody} go: the icon and the message at the top, the row of buttons at the bottom.
 * All positions and sizes are in the coordinates of the dialog's body.<br><br>
 *
 * The message gets the area between the top padding and the buttons, minus a margin at its top and bottom, so it never reaches the buttons.
 * A message that fits this area stands in its middle. A longer one starts at the top of the area and scrolls, with everything outside
 * of the area cut off. {@link #getFittingBodyHeight(int, boolean)} is the body height that fits a message without scrolling.
 *
 * @param textX Where the lines of the message start horizontally.
 * @param textY Where the first line of the message starts when the message is not scrolled.
 * @param iconY Where the icon of the dialog starts, if it has one. It always starts at {@link #PADDING} horizontally.
 * @param messageAreaTop The top of the area the message gets drawn in.
 * @param messageAreaHeight The height of the area the message gets drawn in.
 * @param maxScroll How far the message can scroll, which is zero for a message that fits its area.
 * @param scrollBarCenterX The horizontal center of the scroll bar of a message that scrolls, in the middle of the padding right of the message.
 * @param buttonY Where the row of buttons starts.
 */
record MessageDialogLayout(int textX, int textY, int iconY, int messageAreaTop, int messageAreaHeight, int maxScroll, int scrollBarCenterX, int buttonY) {

    static final int ICON_SIZE = 32;
    private static final int ICON_GAP = 12;
    static final int PADDING = 16;
    static final int LINE_SPACING = 14;
    static final int BUTTON_WIDTH = 120;
    static final int BUTTON_HEIGHT = 20;
    static final int BUTTON_GAP = 8;
    private static final int BUTTON_BOTTOM_PADDING = 20;
    /** The space between the message and the top and bottom of the area above the buttons. */
    private static final int MESSAGE_MARGIN = 8;
    private static final int MIN_WRAP_WIDTH = 20;
    private static final int MIN_BUTTON_WIDTH = 20;
    /** How many lines one step of the mouse wheel scrolls, which keeps the lines at the top of the area whole. */
    private static final int WHEEL_SCROLL_LINES = 3;

    /**
     * Lays out a body of the given size for a message that wraps into the given number of lines.
     */
    static MessageDialogLayout of(int bodyWidth, int bodyHeight, int lineCount, boolean hasIcon) {
        int buttonY = bodyHeight - BUTTON_HEIGHT - BUTTON_BOTTOM_PADDING;
        int messageAreaTop = PADDING + MESSAGE_MARGIN;
        int messageAreaHeight = Math.max(0, buttonY - MESSAGE_MARGIN - messageAreaTop);
        int textHeight = lineCount * LINE_SPACING;
        int messageHeight = getMessageHeight(lineCount, hasIcon);
        int textY;
        int iconY;
        int maxScroll = 0;
        if (messageHeight <= messageAreaHeight) {
            int messageY = messageAreaTop + ((messageAreaHeight - messageHeight) / 2);
            textY = messageY + ((messageHeight - textHeight) / 2);
            iconY = messageY + ((messageHeight - ICON_SIZE) / 2);
        } else {
            // The icon stays in the middle of the area while the text scrolls next to it
            iconY = messageAreaTop + ((messageAreaHeight - ICON_SIZE) / 2);
            if (textHeight > messageAreaHeight) {
                textY = messageAreaTop;
                maxScroll = textHeight - messageAreaHeight;
            } else {
                // Only the icon is taller than the area, so the text still fits
                textY = messageAreaTop + ((messageAreaHeight - textHeight) / 2);
            }
        }
        return new MessageDialogLayout(getTextX(hasIcon), textY, iconY, messageAreaTop, messageAreaHeight, maxScroll, bodyWidth - (PADDING / 2), buttonY);
    }

    private static int getTextX(boolean hasIcon) {
        return hasIcon ? (PADDING + ICON_SIZE + ICON_GAP) : PADDING;
    }

    /**
     * Returns the width the lines of the message get wrapped at in a body of the given width.
     */
    static int getWrapWidth(int bodyWidth, boolean hasIcon) {
        return Math.max(MIN_WRAP_WIDTH, bodyWidth - getTextX(hasIcon) - PADDING);
    }

    /**
     * Returns the smallest body height that shows a message of the given number of lines whole, without scrolling.
     */
    static int getFittingBodyHeight(int lineCount, boolean hasIcon) {
        return PADDING + MESSAGE_MARGIN + getMessageHeight(lineCount, hasIcon) + MESSAGE_MARGIN + BUTTON_HEIGHT + BUTTON_BOTTOM_PADDING;
    }

    /**
     * Returns the width of each button in a row of the given number of buttons. The buttons get narrower than {@link #BUTTON_WIDTH}
     * when the body is too narrow for them, so the row always fits the body.
     */
    static int getButtonWidth(int bodyWidth, int buttonCount) {
        int count = Math.max(1, buttonCount);
        int roomPerButton = (bodyWidth - (BUTTON_GAP * 2) - (BUTTON_GAP * (count - 1))) / count;
        return Math.max(MIN_BUTTON_WIDTH, Math.min(BUTTON_WIDTH, roomPerButton));
    }

    /**
     * Returns where the row of the given number of buttons starts, which centers it in the body.
     */
    static int getButtonRowX(int bodyWidth, int buttonCount, int buttonWidth) {
        int count = Math.max(1, buttonCount);
        return (bodyWidth - ((buttonWidth * count) + (BUTTON_GAP * (count - 1)))) / 2;
    }

    private static int getMessageHeight(int lineCount, boolean hasIcon) {
        return Math.max(lineCount * LINE_SPACING, hasIcon ? ICON_SIZE : 0);
    }

    boolean isScrollable() {
        return this.maxScroll > 0;
    }

    /**
     * Returns how far the message is scrolled, in whole units, for the given scroll between 0 and 1.
     */
    int getScrollOffset(float scroll) {
        if (this.maxScroll <= 0) return 0;
        return Math.round(Math.max(0.0F, Math.min(1.0F, scroll)) * this.maxScroll);
    }

    /**
     * Returns the scroll between 0 and 1 after the mouse wheel turned by the given amount, where a positive amount scrolls up.
     */
    float getScrollAfterWheel(float scroll, double wheelAmount) {
        if (this.maxScroll <= 0) return 0.0F;
        double offset = this.getScrollOffset(scroll) - (wheelAmount * WHEEL_SCROLL_LINES * LINE_SPACING);
        long clampedOffset = Math.round(Math.max(0.0D, Math.min(this.maxScroll, offset)));
        return (float) clampedOffset / (float) this.maxScroll;
    }

    /**
     * Returns the index of the first line that is at least partly inside the message area at the given scroll offset.
     */
    int getFirstVisibleLine(int scrollOffset) {
        int hiddenAbove = scrollOffset - (this.textY - this.messageAreaTop);
        return Math.max(0, hiddenAbove / LINE_SPACING);
    }

    /**
     * Returns the index after the last line that is at least partly inside the message area at the given scroll offset.
     */
    int getVisibleLineEnd(int scrollOffset, int lineCount) {
        int visibleBottom = scrollOffset + this.messageAreaTop + this.messageAreaHeight - this.textY;
        int end = (visibleBottom + LINE_SPACING - 1) / LINE_SPACING;
        return Math.max(0, Math.min(lineCount, end));
    }

}
