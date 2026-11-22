package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;

/**
 * Where the parts of a {@link MessageDialogBody} go: the icon and the message at the top, the row of buttons at the bottom.
 * All positions and sizes are in the coordinates of the dialog's body.<br><br>
 *
 * The icon and the lines of the message stand together in the horizontal middle of the body, with the lines starting right of the icon,
 * so a dialog that is wider than its message keeps the same room on both sides of it.<br>
 * The lines of the message get the room between the top padding and the buttons, minus a margin at its top and bottom, so they never reach
 * the buttons. A message that fits this room stands in its middle. A longer one starts at the top of the room and scrolls in the message area,
 * with everything outside of the area cut off. Glyphs reach above the top of their line, so the message area also covers {@link #GLYPH_OVERHANG}
 * above the room, which keeps the accents of the first line whole when the message is scrolled to its start.<br>
 * {@link #getFittingBodyHeight(int, boolean)} is the body height that fits a message without scrolling, and {@link #getMinBodyHeight(int, boolean, float)}
 * the one a dialog gets on a screen of a given height.
 *
 * @param textX Where the lines of the message start horizontally.
 * @param textY Where the first line of the message starts when the message is not scrolled.
 * @param iconX Where the icon of the dialog starts horizontally, if it has one.
 * @param iconY Where the icon of the dialog starts vertically, if it has one.
 * @param messageAreaTop The top of the area a scrolling message gets drawn in.
 * @param messageAreaHeight The height of the area a scrolling message gets drawn in, which is zero when there is no room for its lines.
 * @param maxScroll How far the message can scroll, which is zero for a message that fits its room.
 * @param scrollBarCenterX The horizontal center of the scroll bar of a message that scrolls, in the middle of the padding right of the message.
 * @param buttonY Where the row of buttons starts.
 */
record MessageDialogLayout(int textX, int textY, int iconX, int iconY, int messageAreaTop, int messageAreaHeight, int maxScroll, int scrollBarCenterX, int buttonY) {

    static final int ICON_SIZE = 32;
    private static final int ICON_GAP = 12;
    static final int PADDING = 16;
    static final int LINE_SPACING = 14;
    static final int BUTTON_WIDTH = 120;
    static final int BUTTON_HEIGHT = 20;
    static final int BUTTON_GAP = 8;
    private static final int BUTTON_BOTTOM_PADDING = 20;
    /** The space between the lines of the message and the top and bottom of the room above the buttons. */
    private static final int MESSAGE_MARGIN = 8;
    /**
     * How far glyphs reach above the top of their line. The UI font draws its highest accents, like the ones of "Ḕ", 3.4 units above it at the
     * Micro UI scale, where its text is one size bigger, and up to half a unit more where it snaps its glyphs to whole pixels.
     * Minecraft's font draws accented capitals, like "Ä", 3 units above it.<br>
     * Below their line, the glyphs of both fonts end at least 2 units before the next line starts, so lines need no room below them.
     */
    static final int GLYPH_OVERHANG = 4;
    private static final int MIN_WRAP_WIDTH = 20;
    private static final int MIN_BUTTON_WIDTH = 20;
    /** How many lines one step of the mouse wheel scrolls, which keeps the lines at the top of the area whole. */
    private static final int WHEEL_SCROLL_LINES = 3;
    /** The share of the screen height a dialog grows to at most for a message that is much too long for it, which scrolls then. */
    private static final float MAX_SCREEN_SHARE = 0.75F;
    /**
     * A message that would scroll by less than this, one step of the mouse wheel, makes its dialog taller than {@link #MAX_SCREEN_SHARE} of the
     * screen instead, if the screen has room for the whole message.
     */
    private static final int MIN_SCROLL = WHEEL_SCROLL_LINES * LINE_SPACING;
    /** Keeps a dialog that is taller than {@link #MAX_SCREEN_SHARE} of the screen off the bottom of the screen, with room for its frame getting rounded to whole GUI pixels. */
    private static final int SCREEN_BOTTOM_GAP = 4;
    /**
     * The part of the screen height the body of a dialog can't use: FancyMenu's menu bar at the top, which windows always stay below,
     * the title bar and the border of the dialog's window, and {@link #SCREEN_BOTTOM_GAP}. The menu bar and the frame get drawn at the UI scale
     * like the body, so this is the same at every scale.<br>
     * The sizes of the menu bar and the frame are compile-time constants, which get copied into this class, so it still works without Minecraft.
     */
    private static final int SCREEN_RESERVED_HEIGHT = MenuBar.PIXEL_SIZE + PiPWindow.DEFAULT_TITLE_BAR_HEIGHT + (PiPWindow.DEFAULT_BORDER_THICKNESS * 2) + SCREEN_BOTTOM_GAP;

    /**
     * Lays out a body of the given size for a message that wraps into the given number of lines.
     *
     * @param textWidth The width of the widest line of the message.
     */
    static MessageDialogLayout of(int bodyWidth, int bodyHeight, int lineCount, int textWidth, boolean hasIcon) {
        int buttonY = bodyHeight - BUTTON_HEIGHT - BUTTON_BOTTOM_PADDING;
        int linesTop = PADDING + MESSAGE_MARGIN;
        int linesHeight = Math.max(0, buttonY - MESSAGE_MARGIN - linesTop);
        int textHeight = lineCount * LINE_SPACING;
        int messageHeight = getMessageHeight(lineCount, hasIcon);
        int textY;
        int iconY;
        int maxScroll = 0;
        if (messageHeight <= linesHeight) {
            int messageY = linesTop + ((linesHeight - messageHeight) / 2);
            textY = messageY + ((messageHeight - textHeight) / 2);
            iconY = messageY + ((messageHeight - ICON_SIZE) / 2);
        } else {
            // The icon stays in the middle of the room while the text scrolls next to it
            iconY = linesTop + ((linesHeight - ICON_SIZE) / 2);
            if (textHeight > linesHeight) {
                textY = linesTop;
                maxScroll = textHeight - linesHeight;
            } else {
                // Only the icon is taller than the room, so the text still fits
                textY = linesTop + ((linesHeight - textHeight) / 2);
            }
        }
        int messageAreaTop = linesTop - GLYPH_OVERHANG;
        int messageAreaHeight = (linesHeight > 0) ? (linesHeight + GLYPH_OVERHANG) : 0;
        int iconX = getContentX(bodyWidth, textWidth, hasIcon);
        return new MessageDialogLayout(iconX + getTextOffset(hasIcon), textY, iconX, iconY, messageAreaTop, messageAreaHeight, maxScroll, bodyWidth - (PADDING / 2), buttonY);
    }

    /**
     * Returns how far right of the icon the lines of the message start.
     */
    private static int getTextOffset(boolean hasIcon) {
        return hasIcon ? (ICON_SIZE + ICON_GAP) : 0;
    }

    /**
     * Returns where the icon and the lines of the message start horizontally, which puts them together in the middle of the body.
     * They never start left of the padding, so a line that is wider than the lines get wrapped at, like one long word, runs out at the right instead.
     */
    private static int getContentX(int bodyWidth, int textWidth, boolean hasIcon) {
        int contentWidth = (textWidth > 0) ? (getTextOffset(hasIcon) + textWidth) : (hasIcon ? ICON_SIZE : 0);
        return Math.max(PADDING, (bodyWidth - contentWidth) / 2);
    }

    /**
     * Returns the width the lines of the message get wrapped at in a body of the given width.
     */
    static int getWrapWidth(int bodyWidth, boolean hasIcon) {
        return Math.max(MIN_WRAP_WIDTH, bodyWidth - (PADDING * 2) - getTextOffset(hasIcon));
    }

    /**
     * Returns the smallest body height that shows a message of the given number of lines whole, without scrolling.
     */
    static int getFittingBodyHeight(int lineCount, boolean hasIcon) {
        return PADDING + MESSAGE_MARGIN + getMessageHeight(lineCount, hasIcon) + MESSAGE_MARGIN + BUTTON_HEIGHT + BUTTON_BOTTOM_PADDING;
    }

    /**
     * Returns the smallest body height a dialog gets for a message of the given number of lines.<br>
     * The dialog fits its message, but grows to at most {@link #MAX_SCREEN_SHARE} of the screen height for it, so a very long message scrolls
     * instead of making the dialog a column as tall as the screen. A message that is less than one step of the mouse wheel taller than that
     * still gets a dialog that fits it, if the screen has room for it, because the message would scroll by only a few units otherwise.
     *
     * @param screenHeight The height of the screen in the units of the body.
     */
    static int getMinBodyHeight(int lineCount, boolean hasIcon, float screenHeight) {
        int fittingHeight = getFittingBodyHeight(lineCount, hasIcon);
        int maxHeight = (int) (screenHeight * MAX_SCREEN_SHARE);
        if (fittingHeight <= maxHeight) return fittingHeight;
        boolean scrollWouldBeTiny = (fittingHeight - maxHeight) < MIN_SCROLL;
        boolean screenHasRoom = fittingHeight <= (screenHeight - SCREEN_RESERVED_HEIGHT);
        return (scrollWouldBeTiny && screenHasRoom) ? fittingHeight : maxHeight;
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
     * A line below the area counts as inside as soon as the glyphs reaching above it do.
     */
    int getVisibleLineEnd(int scrollOffset, int lineCount) {
        if (this.messageAreaHeight <= 0) return 0;
        int visibleBottom = scrollOffset + this.messageAreaTop + this.messageAreaHeight + GLYPH_OVERHANG - this.textY;
        int end = (visibleBottom + LINE_SPACING - 1) / LINE_SPACING;
        return Math.max(0, Math.min(lineCount, end));
    }

}
