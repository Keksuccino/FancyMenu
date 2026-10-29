package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextElementContentHeightPlaceholderTest {

    @Test
    void missingOrBlankIdentifierReturnsZeroWithoutLookup() {
        List<String> lookups = new ArrayList<>();
        TextElementContentHeightPlaceholder placeholder = new TextElementContentHeightPlaceholder(recordingLookup(lookups, null));

        assertEquals("0", replacement(placeholder, null));
        assertEquals("0", replacement(placeholder, ""));
        assertEquals("0", replacement(placeholder, "   "));
        assertTrue(lookups.isEmpty());
    }

    @Test
    void unknownElementReturnsZero() {
        List<String> lookups = new ArrayList<>();
        TextElementContentHeightPlaceholder placeholder = new TextElementContentHeightPlaceholder(recordingLookup(lookups, null));

        assertEquals("0", replacement(placeholder, "missing.element"));
        assertEquals(List.of("missing.element"), lookups);
    }

    @Test
    void nonTextElementReturnsZero() {
        AnimationControllerElement element = new AnimationControllerElement(new AnimationControllerElementBuilder());
        element.baseHeight = 50;
        List<String> lookups = new ArrayList<>();
        TextElementContentHeightPlaceholder placeholder = new TextElementContentHeightPlaceholder(recordingLookup(lookups, element));

        assertEquals("0", replacement(placeholder, "animation.controller"));
        assertEquals(List.of("animation.controller"), lookups);
    }

    @Test
    void readsLiveElementStateOnlyOnTheMainThread() {
        assertFalse(new TextElementContentHeightPlaceholder(id -> null).canRunAsync());
    }

    private static Function<String, AbstractElement> recordingLookup(List<String> lookups, AbstractElement result) {
        return id -> {
            lookups.add(id);
            return result;
        };
    }

    private static String replacement(TextElementContentHeightPlaceholder placeholder, String elementIdentifier) {
        HashMap<String, String> values = new HashMap<>();
        if (elementIdentifier != null) values.put("element_identifier", elementIdentifier);
        return placeholder.getReplacementFor(new DeserializedPlaceholderString("text_element_content_height", values, ""));
    }

}
