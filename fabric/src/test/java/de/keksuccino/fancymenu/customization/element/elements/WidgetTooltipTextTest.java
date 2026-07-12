package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.util.properties.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetTooltipTextTest {

    @Test
    void nullRemainsAbsentAcrossConversions() {
        assertFalse(WidgetTooltipText.isEditorValueValid(null));
        assertNull(WidgetTooltipText.normalizeStoredValue(null));
        assertNull(WidgetTooltipText.toEditorValue(null));
        assertNull(WidgetTooltipText.fromEditorValue(null));
    }

    @Test
    void exactlyEmptyEditorValueIsValidAndClearsStorage() {
        assertTrue(WidgetTooltipText.isEditorValueValid(""));
        assertNull(WidgetTooltipText.normalizeStoredValue(""));
        assertNull(WidgetTooltipText.toEditorValue(""));
        assertNull(WidgetTooltipText.fromEditorValue(""));
    }

    @Test
    void whitespaceOnlyValueRemainsInvalidWithoutBeingNormalizedAway() {
        String whitespace = "   ";

        assertFalse(WidgetTooltipText.isEditorValueValid(whitespace));
        assertEquals(whitespace, WidgetTooltipText.normalizeStoredValue(whitespace));
        assertEquals(whitespace, WidgetTooltipText.toEditorValue(whitespace));
        assertEquals(whitespace, WidgetTooltipText.fromEditorValue(whitespace));
    }

    @Test
    void convertsStoredLineMarkersToEditorNewlines() {
        assertEquals("First line\nSecond line", WidgetTooltipText.toEditorValue("First line%n%Second line"));
    }

    @Test
    void multilineEditorAndStoredValuesRoundTrip() {
        String stored = "First%n%%n%Third%n%";
        String editor = "First\n\nThird\n";

        assertEquals(editor, WidgetTooltipText.toEditorValue(stored));
        assertEquals(stored, WidgetTooltipText.fromEditorValue(editor));
    }

    @Test
    void nonemptyTextStaysValidAndUnchanged() {
        String text = "Useful tooltip";

        assertTrue(WidgetTooltipText.isEditorValueValid(text));
        assertEquals(text, WidgetTooltipText.normalizeStoredValue(text));
        assertEquals(text, WidgetTooltipText.toEditorValue(text));
        assertEquals(text, WidgetTooltipText.fromEditorValue(text));
    }

    @Test
    void normalizedBuilderWritesRemoveStaleEmptyProperties() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "stale button tooltip");
        serialized.putProperty("tooltip", "stale slider tooltip");

        serialized.putProperty("description", WidgetTooltipText.normalizeStoredValue(""));
        serialized.putProperty("tooltip", WidgetTooltipText.normalizeStoredValue(""));

        assertFalse(serialized.hasProperty("description"));
        assertFalse(serialized.hasProperty("tooltip"));
    }

    @Test
    void normalizedBuilderReadsTreatEmptyAsAbsentWithoutDiscardingSpaces() {
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "");
        serialized.putProperty("tooltip", "   ");

        assertNull(WidgetTooltipText.normalizeStoredValue(serialized.getValue("description")));
        assertEquals("   ", WidgetTooltipText.normalizeStoredValue(serialized.getValue("tooltip")));
    }

    @Test
    void checkboxPropertyRemovesStaleEmptyValueAfterRoundTrip() {
        Property<String> tooltip = Property.stringProperty("description", null, true, true, "unused").setUserInputTextValidator(WidgetTooltipText::isEditorValueValid).setValueGetProcessor(WidgetTooltipText::toEditorValue).setValueSetProcessor(WidgetTooltipText::fromEditorValue);
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "");

        tooltip.deserialize(serialized);
        assertNull(tooltip.get());

        tooltip.serialize(serialized);
        assertFalse(serialized.hasProperty("description"));
    }

    @Test
    void checkboxPropertyPreservesMultilineStorageRoundTrip() {
        Property<String> tooltip = Property.stringProperty("description", null, true, true, "unused").setUserInputTextValidator(WidgetTooltipText::isEditorValueValid).setValueGetProcessor(WidgetTooltipText::toEditorValue).setValueSetProcessor(WidgetTooltipText::fromEditorValue);
        SerializedElement serialized = new SerializedElement();
        serialized.putProperty("description", "First%n%Second");

        tooltip.deserialize(serialized);
        assertEquals("First\nSecond", tooltip.get());

        tooltip.set("First\nChanged");
        tooltip.serialize(serialized);
        assertEquals("First%n%Changed", serialized.getValue("description"));
    }

}
