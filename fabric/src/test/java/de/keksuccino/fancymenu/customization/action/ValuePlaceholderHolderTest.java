package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.placeholder.ShortReferenceResolver;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ValuePlaceholderHolderTest {

    @Test
    void longestValuePlaceholderNameWinsInEveryOrder() {
        Map<String, Supplier<String>> shortFirst = new LinkedHashMap<>();
        shortFirst.put("video_source", () -> "clip.mp4");
        shortFirst.put("video_source_type", () -> "LOCAL");
        Map<String, Supplier<String>> longFirst = new LinkedHashMap<>();
        longFirst.put("video_source_type", () -> "LOCAL");
        longFirst.put("video_source", () -> "clip.mp4");
        String text = "$$video_source_type|$$video_source";

        assertEquals("LOCAL|clip.mp4", ValuePlaceholderHolder.applyValuePlaceholders(text, shortFirst, names()));
        assertEquals("LOCAL|clip.mp4", ValuePlaceholderHolder.applyValuePlaceholders(text, longFirst, names()));
    }

    @Test
    void referenceWithLongerVariableNameStaysForTheVariable() {
        Map<String, Supplier<String>> placeholders = Map.of("value", () -> "true");

        assertEquals("$$value_backup / true", ValuePlaceholderHolder.applyValuePlaceholders("$$value_backup / $$value", placeholders, names("value_backup")));
    }

    @Test
    void valuePlaceholderWinsOverVariableWithTheSameOrAShorterName() {
        Map<String, Supplier<String>> placeholders = Map.of("value_backup", () -> "backup");

        assertEquals("backup", ValuePlaceholderHolder.applyValuePlaceholders("$$value_backup", placeholders, names("value_backup")));
        assertEquals("backup", ValuePlaceholderHolder.applyValuePlaceholders("$$value_backup", placeholders, names("value")));
    }

    @Test
    void nullValueBecomesEmptyText() {
        Map<String, Supplier<String>> placeholders = Map.of("value", () -> null);

        assertEquals("[]", ValuePlaceholderHolder.applyValuePlaceholders("[$$value]", placeholders, names()));
    }

    @Test
    void onlyValuesOfUsedReferencesGetRead() {
        AtomicInteger unusedReads = new AtomicInteger();
        Map<String, Supplier<String>> placeholders = new LinkedHashMap<>();
        placeholders.put("used", () -> "1");
        placeholders.put("unused", () -> String.valueOf(unusedReads.incrementAndGet()));

        assertEquals("1", ValuePlaceholderHolder.applyValuePlaceholders("$$used", placeholders, names()));
        assertEquals(0, unusedReads.get());
    }

    @Test
    void textWithoutReferencesStaysTheSameInstance() {
        String text = "value without references";

        assertSame(text, ValuePlaceholderHolder.applyValuePlaceholders(text, Map.of("value", () -> "true")));
    }

    @Test
    void variableNamesGetLookedUpAtEveryReferenceOnly() {
        List<Integer> lookedUpAt = new ArrayList<>();
        ShortReferenceResolver.NameLookup variableNames = (text, nameStart) -> {
            lookedUpAt.add(nameStart);
            return 0;
        };

        assertEquals("a true b true c", ValuePlaceholderHolder.applyValuePlaceholders("a $$value b $$value c", Map.of("value", () -> "true"), variableNames));
        assertEquals(List.of(4, 14), lookedUpAt);
        assertSame("no references", ValuePlaceholderHolder.applyValuePlaceholders("no references", Map.of("value", () -> "true"), variableNames));
        assertEquals(2, lookedUpAt.size());
    }

    /** Finds the longest of the given names, like the name index of FancyMenu's variables does it. */
    @NotNull
    private static ShortReferenceResolver.NameLookup names(@NotNull String... names) {
        List<String> nameList = List.of(names);
        return (text, nameStart) -> {
            String name = ShortReferenceResolver.findLongestName(text, nameStart, nameList, n -> n);
            return (name != null) ? name.length() : 0;
        };
    }

}
