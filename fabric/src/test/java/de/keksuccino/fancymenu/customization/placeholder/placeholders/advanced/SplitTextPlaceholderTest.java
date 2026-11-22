package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SplitTextPlaceholderTest {

    private static final SplitTextPlaceholder SPLIT_TEXT = new SplitTextPlaceholder();

    @Test
    void returnsThePartAtSplitIndex() {
        assertEquals("a", replacement("a b c", " ", null, "0"));
        assertEquals("b", replacement("a b c", " ", null, "1"));
        assertEquals("c", replacement("a b c", " ", null, "2"));
    }

    @Test
    void keepsTheRestOfTheInputInTheLastPartWhenMaxPartsIsSet() {
        assertEquals("a", replacement("a b c", " ", "2", "0"));
        assertEquals("b c", replacement("a b c", " ", "2", "1"));
    }

    @Test
    void returnsEmptyTextWhenSplitIndexIsOutsideOfMaxParts() {
        assertEquals("", replacement("a b", " ", "2", "2"));
        assertEquals("", replacement("a b", " ", "2", "5"));
    }

    @Test
    void returnsEmptyTextWhenThereIsNoPartAtSplitIndex() {
        assertEquals("", replacement("a b", " ", null, "2"));
        assertEquals("", replacement("a b", " ", "5", "2"));
        assertEquals("", replacement("abc", " ", null, "1"));
    }

    @Test
    void returnsNullWithoutInputOrRegex() {
        assertNull(replacement(null, " ", null, "0"));
        assertNull(replacement("a b", null, null, "0"));
    }

    @Nullable
    private static String replacement(@Nullable String input, @Nullable String regex, @Nullable String maxParts, @Nullable String splitIndex) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        if (input != null) values.put("input", input);
        if (regex != null) values.put("regex", regex);
        if (maxParts != null) values.put("max_parts", maxParts);
        if (splitIndex != null) values.put("split_index", splitIndex);
        String placeholderString = new DeserializedPlaceholderString(SPLIT_TEXT.getIdentifier(), values, "").toString();
        return SPLIT_TEXT.getReplacementFor(new DeserializedPlaceholderString(SPLIT_TEXT.getIdentifier(), values, placeholderString));
    }

}
