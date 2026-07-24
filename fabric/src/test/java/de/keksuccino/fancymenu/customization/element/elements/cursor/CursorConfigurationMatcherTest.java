package de.keksuccino.fancymenu.customization.element.elements.cursor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorConfigurationMatcherTest {

    @Test
    void equalResourceLocationDoesNotHideNewTextureAllocation() {
        Object previousTexture = new Object();
        Object reloadedTextureAtSameLocation = new Object();

        assertFalse(CursorConfigurationMatcher.matches(previousTexture, 1, 2, reloadedTextureAtSameLocation, 1, 2));
    }

    @Test
    void sameTextureAndHotspotMatches() {
        Object texture = new Object();

        assertTrue(CursorConfigurationMatcher.matches(texture, 1, 2, texture, 1, 2));
        assertFalse(CursorConfigurationMatcher.matches(texture, 1, 2, texture, 2, 2));
    }

}
