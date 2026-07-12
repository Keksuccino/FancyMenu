package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetTooltipTextTest {

    @Test
    void editorValidationAllowsExactEmptyToClearTooltip() {
        assertTrue(WidgetTooltipText.isEditorValueValid(""));
        assertTrue(WidgetTooltipText.isEditorValueValid("Tooltip"));
        assertTrue(WidgetTooltipText.isEditorValueValid("\n"));
        assertFalse(WidgetTooltipText.isEditorValueValid(null));
        assertFalse(WidgetTooltipText.isEditorValueValid("   "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void legacyMissingOrEmptyStoredValueNormalizesToNull(String value) {
        assertNull(WidgetTooltipText.normalizeStoredValue(value));
        assertNull(WidgetTooltipText.toEditorValue(value));
        assertNull(WidgetTooltipText.fromEditorValue(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "Tooltip", "%n%"})
    void normalizationPreservesEveryNonEmptyStoredValue(String value) {
        assertEquals(value, WidgetTooltipText.normalizeStoredValue(value));
    }

    @Test
    void storedMultilineTokensAreDecodedForEditing() {
        assertEquals("First\nSecond\nThird", WidgetTooltipText.toEditorValue("First%n%Second%n%Third"));
    }

    @Test
    void editorNewlinesAreEncodedForPersistentStorage() {
        assertEquals("First%n%Second%n%Third", WidgetTooltipText.fromEditorValue("First\nSecond\nThird"));
    }

    @Test
    void multilineConversionRoundTripsWithoutChangingOtherText() {
        String stored = "Prefix %value%&a%n%Suffix";
        assertEquals(stored, WidgetTooltipText.fromEditorValue(WidgetTooltipText.toEditorValue(stored)));
    }

    @Test
    void unconditionalNormalizedWritesRemoveStaleTooltipProperties() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "stale button tooltip");
        serialized.putProperty("tooltip", "stale slider tooltip");

        serialized.putProperty("description", WidgetTooltipText.normalizeStoredValue(""));
        serialized.putProperty("tooltip", WidgetTooltipText.normalizeStoredValue(null));

        assertFalse(serialized.hasProperty("description"));
        assertFalse(serialized.hasProperty("tooltip"));
    }

    @Test
    void whitespaceIsNotNormalizedIntoAValidClearOperation() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", WidgetTooltipText.normalizeStoredValue("   "));

        assertTrue(serialized.hasProperty("description"));
        assertEquals("   ", serialized.getValue("description"));
    }

}
