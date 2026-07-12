package de.keksuccino.fancymenu.customization.element.elements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetTooltipTextTest {

    @Test
    void validatesClearableAndPopulatedEditorValuesWithoutAllowingSpaceOnlyText() {
        assertFalse(WidgetTooltipText.isEditorValueValid(null));
        assertTrue(WidgetTooltipText.isEditorValueValid(""));
        assertFalse(WidgetTooltipText.isEditorValueValid(" "));
        assertFalse(WidgetTooltipText.isEditorValueValid("     "));
        assertTrue(WidgetTooltipText.isEditorValueValid("Tooltip"));
        assertTrue(WidgetTooltipText.isEditorValueValid("  Tooltip  "));
    }

    @Test
    void normalizesOnlyMissingAndExactlyEmptyStoredValues() {
        assertNull(WidgetTooltipText.normalizeStoredValue(null));
        assertNull(WidgetTooltipText.normalizeStoredValue(""));
        assertEquals(" ", WidgetTooltipText.normalizeStoredValue(" "));
        assertEquals("Tooltip", WidgetTooltipText.normalizeStoredValue("Tooltip"));
    }

    @Test
    void convertsStoredNewlineTokensToEditorLineBreaks() {
        assertNull(WidgetTooltipText.toEditorValue(null));
        assertNull(WidgetTooltipText.toEditorValue(""));
        assertEquals("First\n\nSecond", WidgetTooltipText.toEditorValue("First%n%%n%Second"));
        assertEquals("Already\nMultiline", WidgetTooltipText.toEditorValue("Already\nMultiline"));
    }

    @Test
    void convertsEditorLineBreaksToStoredNewlineTokens() {
        assertNull(WidgetTooltipText.fromEditorValue(null));
        assertNull(WidgetTooltipText.fromEditorValue(""));
        assertEquals("First%n%%n%Second", WidgetTooltipText.fromEditorValue("First\n\nSecond"));
        assertEquals("Windows\r%n%Line", WidgetTooltipText.fromEditorValue("Windows\r\nLine"));
    }

    @Test
    void preservesCanonicalStoredValuesAcrossEditorRoundTrips() {
        String stored = "First%n%%n%Second%n%Third";

        assertEquals(stored, WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue(stored)));
        assertNull(WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue("")));
        assertNull(WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue(null)));
    }

}
