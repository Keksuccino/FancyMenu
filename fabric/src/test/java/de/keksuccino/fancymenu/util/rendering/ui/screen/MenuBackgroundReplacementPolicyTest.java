package de.keksuccino.fancymenu.util.rendering.ui.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuBackgroundReplacementPolicyTest {

    @Test
    void screenMenuBackgroundsSuppressTheGlobalBaseOnEveryRenderingPath() {
        assertAll(() -> assertTrue(MenuBackgroundReplacementPolicy.shouldRenderGlobalBase(false)), () -> assertFalse(MenuBackgroundReplacementPolicy.shouldRenderGlobalBase(true)));
    }

    @Test
    void rendersGlobalBaseOnlyForOffWorldScreensWithoutScreenMenuBackgrounds() {
        assertAll(() -> assertTrue(MenuBackgroundReplacementPolicy.shouldRenderListBase(false, false)), () -> assertFalse(MenuBackgroundReplacementPolicy.shouldRenderListBase(false, true)), () -> assertFalse(MenuBackgroundReplacementPolicy.shouldRenderListBase(true, false)), () -> assertFalse(MenuBackgroundReplacementPolicy.shouldRenderListBase(true, true)));
    }

    @Test
    void retainsLegacyBoundedPanelOnlyWithoutAnyFullScreenMenuBackground() {
        assertAll(() -> assertTrue(MenuBackgroundReplacementPolicy.shouldRenderLegacyBoundedPanel(false, false)), () -> assertFalse(MenuBackgroundReplacementPolicy.shouldRenderLegacyBoundedPanel(true, false)), () -> assertFalse(MenuBackgroundReplacementPolicy.shouldRenderLegacyBoundedPanel(false, true)), () -> assertFalse(MenuBackgroundReplacementPolicy.shouldRenderLegacyBoundedPanel(true, true)));
    }

}
