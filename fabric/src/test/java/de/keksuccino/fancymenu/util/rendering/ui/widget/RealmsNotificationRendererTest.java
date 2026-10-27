package de.keksuccino.fancymenu.util.rendering.ui.widget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmsNotificationRendererTest {

    // The vanilla Title screen centers its 200 pixels wide Realms button horizontally
    private static final int REALMS_BUTTON_HALF_WIDTH = 100;
    private static final int MAX_ICON_COUNT = 4;

    @ParameterizedTest
    @ValueSource(ints = {320, 427, 480, 853, 1000, 1001})
    void widgetStaysInsideRealmsButtonForEveryIconCount(int screenWidth) {
        for (int iconCount = 0; iconCount <= MAX_ICON_COUNT; iconCount++) {
            int widgetWidth = getWidgetWidth(iconCount);
            int widgetX = RealmsNotificationRenderer.getWidgetX(screenWidth, widgetWidth);
            assertTrue(widgetX >= screenWidth / 2 - REALMS_BUTTON_HALF_WIDTH, "icon count " + iconCount);
            assertTrue(widgetX + widgetWidth <= screenWidth / 2 + REALMS_BUTTON_HALF_WIDTH, "icon count " + iconCount);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {320, 427, 480, 853, 1000, 1001})
    void widgetRightEdgeDoesNotDependOnIconCount(int screenWidth) {
        // The layout editor always shows all icons, while the normal Title screen often shows none
        int editorRightEdge = getWidgetRightEdge(screenWidth, MAX_ICON_COUNT);
        for (int iconCount = 0; iconCount < MAX_ICON_COUNT; iconCount++) {
            assertEquals(editorRightEdge, getWidgetRightEdge(screenWidth, iconCount), "icon count " + iconCount);
        }
    }

    @Test
    void widgetMatchesIconAreaWhileIconsAreShown() {
        int screenWidth = 480;
        for (int iconCount = 1; iconCount <= MAX_ICON_COUNT; iconCount++) {
            int iconsWidth = RealmsNotificationRenderer.getIconsWidth(iconCount);
            assertEquals(iconsWidth, RealmsNotificationRenderer.getWidgetWidth(iconsWidth));
            assertEquals(RealmsNotificationRenderer.getIconAreaRightX(screenWidth) - iconsWidth, RealmsNotificationRenderer.getWidgetX(screenWidth, iconsWidth));
        }
    }

    @Test
    void widgetKeepsUsableSizeWhileNoIconIsShown() {
        assertEquals(0, RealmsNotificationRenderer.getIconsWidth(0));
        assertTrue(RealmsNotificationRenderer.getWidgetWidth(0) > 0);
    }

    private static int getWidgetWidth(int iconCount) {
        return RealmsNotificationRenderer.getWidgetWidth(RealmsNotificationRenderer.getIconsWidth(iconCount));
    }

    private static int getWidgetRightEdge(int screenWidth, int iconCount) {
        int widgetWidth = getWidgetWidth(iconCount);
        return RealmsNotificationRenderer.getWidgetX(screenWidth, widgetWidth) + widgetWidth;
    }

}
