package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageDialogLayoutTest {

    /** The body heights a dialog of the default size has, which differ a bit between GUI scales and UI scales because its frame takes whole GUI pixels. */
    private static final int[] DEFAULT_BODY_HEIGHTS = {152, 153, 154, 155, 156, 157};
    private static final int DEFAULT_BODY_WIDTH = 400;
    private static final int LINE = MessageDialogLayout.LINE_SPACING;
    private static final int OVERHANG = MessageDialogLayout.GLYPH_OVERHANG;
    /** What a dialog's window needs on the screen next to its body, in the units of the body: FancyMenu's menu bar (28), the title bar (21) and the border (2). */
    private static final int WINDOW_FRAME_AND_MENU_BAR_HEIGHT = 28 + 21 + 2;
    /** One step of the mouse wheel scrolls by three lines. */
    private static final int WHEEL_STEP = 3 * LINE;

    @Test
    void messageThatFitsStaysWhereTheDialogAlwaysPutIt() {
        for (int bodyHeight = 72; bodyHeight <= 400; bodyHeight++) {
            for (int lines = 0; lines <= 30; lines++) {
                for (boolean hasIcon : new boolean[] {false, true}) {
                    MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, lines, hasIcon);
                    if (MessageDialogLayout.getFittingBodyHeight(lines, hasIcon) > bodyHeight) continue;
                    int[] old = oldLayout(bodyHeight, lines, hasIcon);
                    String context = "body height " + bodyHeight + ", " + lines + " lines, icon " + hasIcon;
                    assertEquals(old[0], layout.textX(), context);
                    assertEquals(old[1], layout.textY(), context);
                    assertEquals(old[2], layout.iconY(), context);
                    assertEquals(old[3], layout.buttonY(), context);
                    assertFalse(layout.isScrollable(), context);
                }
            }
        }
    }

    @Test
    void shortMessagesKeepTheDefaultDialogSize() {
        for (int bodyHeight : DEFAULT_BODY_HEIGHTS) {
            for (int lines = 0; lines <= 5; lines++) {
                assertTrue(MessageDialogLayout.getFittingBodyHeight(lines, true) <= bodyHeight, lines + " lines make a body of height " + bodyHeight + " taller");
                assertTrue(MessageDialogLayout.getFittingBodyHeight(lines, false) <= bodyHeight, lines + " lines make a body of height " + bodyHeight + " taller");
            }
        }
    }

    @Test
    void asyncErrorMessageNoLongerRunsBehindTheButtons() {
        // The English "can't run async" error wraps into 8 lines in a dialog of the default size, which used to end behind the buttons
        int lines = 8;
        int fittingHeight = MessageDialogLayout.getFittingBodyHeight(lines, true);
        for (int bodyHeight : DEFAULT_BODY_HEIGHTS) {
            assertTrue(fittingHeight > bodyHeight, "the dialog has to grow for the message");
            // Where the screen has no room for a taller dialog, the message scrolls instead
            MessageDialogLayout defaultSize = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, lines, true);
            assertTrue(defaultSize.isScrollable());
            assertTrue(defaultSize.messageAreaTop() + defaultSize.messageAreaHeight() < defaultSize.buttonY());
        }
        MessageDialogLayout fitted = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, fittingHeight, lines, true);
        assertFalse(fitted.isScrollable());
        assertTrue(fitted.textY() + (lines * LINE) < fitted.buttonY());
    }

    @Test
    void messageNeverReachesTheButtons() {
        for (int bodyHeight = 0; bodyHeight <= 600; bodyHeight++) {
            for (int lines = 0; lines <= 100; lines++) {
                for (boolean hasIcon : new boolean[] {false, true}) {
                    MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, lines, hasIcon);
                    String context = "body height " + bodyHeight + ", " + lines + " lines, icon " + hasIcon;
                    int areaBottom = layout.messageAreaTop() + layout.messageAreaHeight();
                    int textHeight = lines * LINE;
                    assertTrue(layout.messageAreaHeight() >= 0, context);
                    if (layout.messageAreaHeight() > 0) {
                        assertTrue(areaBottom < layout.buttonY(), context);
                    }
                    if (layout.isScrollable() && (layout.messageAreaHeight() == 0)) {
                        // Without room for the lines, nothing of the message gets drawn
                        assertEquals(0, layout.getVisibleLineEnd(0, lines), context);
                    } else if (layout.isScrollable()) {
                        assertEquals(layout.messageAreaTop() + OVERHANG, layout.textY(), context);
                        assertEquals(OVERHANG + textHeight - layout.messageAreaHeight(), layout.maxScroll(), context);
                        // Scrolled to the end, the last line ends right at the bottom of the area
                        assertEquals(areaBottom, layout.textY() - layout.maxScroll() + textHeight, context);
                    } else {
                        assertEquals(0, layout.maxScroll(), context);
                        if (textHeight > 0) {
                            assertTrue(layout.textY() >= layout.messageAreaTop(), context);
                            assertTrue(layout.textY() + textHeight <= areaBottom, context);
                        }
                    }
                }
            }
        }
    }

    @Test
    void fittingBodyHeightIsTheSmallestOneThatShowsTheWholeMessage() {
        for (int lines = 0; lines <= 200; lines++) {
            for (boolean hasIcon : new boolean[] {false, true}) {
                int fittingHeight = MessageDialogLayout.getFittingBodyHeight(lines, hasIcon);
                MessageDialogLayout fitted = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, fittingHeight, lines, hasIcon);
                String context = lines + " lines, icon " + hasIcon;
                assertFalse(fitted.isScrollable(), context);
                int messageHeight = Math.max(lines * LINE, hasIcon ? 32 : 0);
                assertEquals((messageHeight > 0) ? (messageHeight + OVERHANG) : 0, fitted.messageAreaHeight(), context);
                if ((lines * LINE) > (hasIcon ? 32 : 0)) {
                    assertTrue(MessageDialogLayout.of(DEFAULT_BODY_WIDTH, fittingHeight - 1, lines, hasIcon).isScrollable(), context);
                }
            }
        }
    }

    @Test
    void wrapWidthLeavesRoomForTheIconAndThePadding() {
        for (int bodyWidth = 0; bodyWidth <= 800; bodyWidth++) {
            assertEquals(Math.max(20, bodyWidth - 32), MessageDialogLayout.getWrapWidth(bodyWidth, false));
            assertEquals(Math.max(20, bodyWidth - 32 - 44), MessageDialogLayout.getWrapWidth(bodyWidth, true));
        }
    }

    @Test
    void scrollBarSitsInThePaddingRightOfTheMessage() {
        for (int bodyWidth = 100; bodyWidth <= 800; bodyWidth++) {
            MessageDialogLayout layout = MessageDialogLayout.of(bodyWidth, 200, 40, true);
            int textRight = layout.textX() + MessageDialogLayout.getWrapWidth(bodyWidth, true);
            assertTrue(layout.scrollBarCenterX() > textRight);
            assertTrue(layout.scrollBarCenterX() < bodyWidth);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void buttonsKeepTheirWidthWhereTheyFit(int buttonCount) {
        for (int bodyWidth = 400; bodyWidth <= 900; bodyWidth++) {
            int width = MessageDialogLayout.getButtonWidth(bodyWidth, buttonCount);
            assertEquals(120, width);
            int rowWidth = (120 * buttonCount) + (8 * (buttonCount - 1));
            assertEquals((bodyWidth - rowWidth) / 2, MessageDialogLayout.getButtonRowX(bodyWidth, buttonCount, width));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void buttonsGetNarrowerInNarrowDialogsSoTheRowStaysInside(int buttonCount) {
        for (int bodyWidth = 0; bodyWidth < 400; bodyWidth++) {
            int width = MessageDialogLayout.getButtonWidth(bodyWidth, buttonCount);
            int x = MessageDialogLayout.getButtonRowX(bodyWidth, buttonCount, width);
            int rowRight = x + (width * buttonCount) + (8 * (buttonCount - 1));
            assertTrue((width >= 20) && (width <= 120));
            // Only buttons that already have their smallest width can be too wide for a dialog
            if (width > 20) {
                assertTrue(x >= 8, "row starts at " + x + " in a body of width " + bodyWidth);
                assertTrue(rowRight <= bodyWidth - 8, "row ends at " + rowRight + " in a body of width " + bodyWidth);
            }
        }
    }

    @Test
    void scrollOffsetCoversTheWholeMessage() {
        MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, 200, 30, true);
        assertTrue(layout.isScrollable());
        assertEquals(0, layout.getScrollOffset(0.0F));
        assertEquals(layout.maxScroll(), layout.getScrollOffset(1.0F));
        assertEquals(0, layout.getScrollOffset(-0.5F));
        assertEquals(layout.maxScroll(), layout.getScrollOffset(1.5F));
        MessageDialogLayout fitting = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, 200, 2, true);
        assertEquals(0, fitting.getScrollOffset(0.7F));
        assertEquals(0.0F, fitting.getScrollAfterWheel(0.7F, -1.0D));
    }

    @Test
    void wheelScrollsByWholeLinesAndStopsAtBothEnds() {
        MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, 200, 30, true);
        float scroll = 0.0F;
        int lastOffset = 0;
        while (lastOffset < layout.maxScroll()) {
            scroll = layout.getScrollAfterWheel(scroll, -1.0D);
            int offset = layout.getScrollOffset(scroll);
            assertTrue(offset > lastOffset);
            // Until the end, every step keeps the line at the top of the area whole
            if (offset < layout.maxScroll()) assertEquals(0, offset % LINE);
            lastOffset = offset;
        }
        assertEquals(1.0F, layout.getScrollAfterWheel(scroll, -1.0D));
        assertEquals(0.0F, layout.getScrollAfterWheel(0.0F, 1.0D));
        // Touchpads turn the wheel by fractions of a step
        assertEquals(LINE * 3 / 2, layout.getScrollOffset(layout.getScrollAfterWheel(0.0F, -0.5D)));
    }

    @Test
    void onlyLinesInsideTheAreaGetDrawn() {
        for (int bodyHeight = 90; bodyHeight <= 300; bodyHeight += 7) {
            int lines = 60;
            MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, lines, true);
            assertTrue(layout.isScrollable());
            int areaBottom = layout.messageAreaTop() + layout.messageAreaHeight();
            for (int offset = 0; offset <= layout.maxScroll(); offset++) {
                int first = layout.getFirstVisibleLine(offset);
                int end = layout.getVisibleLineEnd(offset, lines);
                for (int i = 0; i < lines; i++) {
                    int top = layout.textY() - offset + (i * LINE);
                    // The glyphs of a line reach up to the overhang above it, so a line right below the area shows the tips of its accents in it
                    boolean visible = (top + LINE > layout.messageAreaTop()) && (top - OVERHANG < areaBottom);
                    assertEquals(visible, (i >= first) && (i < end), "line " + i + " at offset " + offset + " in a body of height " + bodyHeight);
                }
            }
        }
        MessageDialogLayout fitting = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, 200, 4, true);
        assertEquals(0, fitting.getFirstVisibleLine(0));
        assertEquals(4, fitting.getVisibleLineEnd(0, 4));
    }

    @Test
    void scrollingMessageKeepsTheGlyphsOfItsFirstAndLastLineWhole() {
        for (int bodyHeight = 0; bodyHeight <= 600; bodyHeight++) {
            for (int lines = 1; lines <= 100; lines++) {
                for (boolean hasIcon : new boolean[] {false, true}) {
                    MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, lines, hasIcon);
                    if (!layout.isScrollable() || (layout.messageAreaHeight() == 0)) continue;
                    String context = "body height " + bodyHeight + ", " + lines + " lines, icon " + hasIcon;
                    int areaBottom = layout.messageAreaTop() + layout.messageAreaHeight();
                    // Scrolled to the start, the accents above the first line are inside the area, which still starts below the top padding
                    assertTrue(layout.textY() - OVERHANG >= layout.messageAreaTop(), context);
                    assertTrue(layout.messageAreaTop() >= MessageDialogLayout.PADDING, context);
                    assertEquals(0, layout.getFirstVisibleLine(0), context);
                    // Scrolled to the end, the last line (its descenders stay inside its own line) is inside the area and gets drawn
                    int lastLineBottom = layout.textY() - layout.maxScroll() + (lines * LINE);
                    assertTrue(lastLineBottom <= areaBottom, context);
                    assertEquals(lines, layout.getVisibleLineEnd(layout.maxScroll(), lines), context);
                }
            }
        }
    }

    @Test
    void noRoomForTheLinesDrawsNoLines() {
        // A body this small leaves no room between the top padding and the buttons
        MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, 60, 10, true);
        assertEquals(0, layout.messageAreaHeight());
        assertEquals(0, layout.getVisibleLineEnd(0, 10));
        assertEquals(0, layout.getVisibleLineEnd(layout.maxScroll(), 10));
    }

    @Test
    void messagesThatFitThreeQuartersOfTheScreenGetADialogThatFitsThem() {
        for (int screenHeight = 50; screenHeight <= 2400; screenHeight++) {
            for (int lines = 0; lines <= 200; lines++) {
                for (boolean hasIcon : new boolean[] {false, true}) {
                    int fittingHeight = MessageDialogLayout.getFittingBodyHeight(lines, hasIcon);
                    if (fittingHeight > (int) (screenHeight * 0.75F)) continue;
                    assertEquals(fittingHeight, MessageDialogLayout.getMinBodyHeight(lines, hasIcon, screenHeight), "screen height " + screenHeight + ", " + lines + " lines, icon " + hasIcon);
                }
            }
        }
    }

    @Test
    void messageScrollsByLessThanOneWheelStepOnlyWhenTheScreenHasNoRoomForIt() {
        for (int screenHeight = 50; screenHeight <= 2400; screenHeight++) {
            int threeQuarters = (int) (screenHeight * 0.75F);
            for (int lines = 0; lines <= 200; lines++) {
                for (boolean hasIcon : new boolean[] {false, true}) {
                    String context = "screen height " + screenHeight + ", " + lines + " lines, icon " + hasIcon;
                    int fittingHeight = MessageDialogLayout.getFittingBodyHeight(lines, hasIcon);
                    int bodyHeight = MessageDialogLayout.getMinBodyHeight(lines, hasIcon, screenHeight);
                    MessageDialogLayout layout = MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, lines, hasIcon);
                    // Its window fits below the menu bar with some gap to the bottom of the screen, or with a gap of at least 8 units
                    boolean windowFits = (fittingHeight + WINDOW_FRAME_AND_MENU_BAR_HEIGHT) < screenHeight;
                    boolean windowFitsEasily = (fittingHeight + WINDOW_FRAME_AND_MENU_BAR_HEIGHT + 8) <= screenHeight;
                    // A dialog never gets smaller than it got before it grew for slightly too long messages
                    assertTrue(bodyHeight >= Math.min(fittingHeight, threeQuarters), context);
                    if (layout.maxScroll() > 0) {
                        assertTrue((layout.maxScroll() >= WHEEL_STEP) || !windowFitsEasily, context + " scrolls by " + layout.maxScroll());
                    }
                    if (bodyHeight > threeQuarters) {
                        // Only to show the whole message, and only where its window fits on the screen
                        assertEquals(fittingHeight, bodyHeight, context);
                        assertFalse(layout.isScrollable(), context);
                        assertTrue(windowFits, context);
                    }
                }
            }
        }
    }

    @Test
    void veryLongMessagesStillScrollInThreeQuartersOfTheScreen() {
        for (int screenHeight = 50; screenHeight <= 2400; screenHeight++) {
            int threeQuarters = (int) (screenHeight * 0.75F);
            for (int lines = 0; lines <= 200; lines++) {
                for (boolean hasIcon : new boolean[] {false, true}) {
                    if (MessageDialogLayout.getFittingBodyHeight(lines, hasIcon) - threeQuarters < WHEEL_STEP) continue;
                    assertEquals(threeQuarters, MessageDialogLayout.getMinBodyHeight(lines, hasIcon, screenHeight), "screen height " + screenHeight + ", " + lines + " lines, icon " + hasIcon);
                }
            }
        }
        // 100 lines on a screen 1080 units tall, like at the Micro UI scale in a game window 1080 pixels tall
        int bodyHeight = MessageDialogLayout.getMinBodyHeight(100, true, 1080.0F);
        assertEquals(810, bodyHeight);
        assertTrue(MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, 100, true).maxScroll() >= 500);
    }

    @Test
    void asyncErrorAtTheColossalUiScaleFitsInsteadOfScrollingFourUnits() {
        // At the Colossal UI scale, a game window 960 pixels tall is 240 units tall, and three quarters of it are 180 units.
        // The 8 lines of the "can't run async" error need 184, which would make it scroll by 4 units.
        int lines = 8;
        assertEquals(184, MessageDialogLayout.getFittingBodyHeight(lines, true));
        int bodyHeight = MessageDialogLayout.getMinBodyHeight(lines, true, 240.0F);
        assertEquals(184, bodyHeight);
        assertFalse(MessageDialogLayout.of(DEFAULT_BODY_WIDTH, bodyHeight, lines, true).isScrollable());
        // A screen without room for the whole message keeps three quarters of its height for the dialog, like before
        assertEquals(165, MessageDialogLayout.getMinBodyHeight(lines, true, 220.0F));
    }

    /**
     * The layout of message dialogs before they fit their message: {text x, text y, icon y, button y}.
     */
    private static int[] oldLayout(int bodyHeight, int lines, boolean hasIcon) {
        int contentTop = 16;
        int contentBottom = Math.max(contentTop, bodyHeight - 40);
        int contentHeight = Math.max(0, contentBottom - contentTop);
        int textHeight = lines * 14;
        int iconHeight = hasIcon ? 32 : 0;
        int contentBlockHeight = Math.max(textHeight, iconHeight);
        int contentY = contentTop + Math.max(0, (contentHeight - contentBlockHeight) / 2);
        int textX = hasIcon ? (16 + 32 + 12) : 16;
        int iconY = contentY + (contentBlockHeight - 32) / 2;
        int textY = contentY + Math.max(0, (contentBlockHeight - textHeight) / 2);
        return new int[] {textX, textY, iconY, bodyHeight - 40};
    }

}
