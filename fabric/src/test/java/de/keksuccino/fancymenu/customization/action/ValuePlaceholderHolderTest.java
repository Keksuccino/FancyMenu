package de.keksuccino.fancymenu.customization.action;

import org.junit.jupiter.api.Test;

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

        assertEquals("LOCAL|clip.mp4", ValuePlaceholderHolder.applyValuePlaceholders(text, shortFirst, List.of()));
        assertEquals("LOCAL|clip.mp4", ValuePlaceholderHolder.applyValuePlaceholders(text, longFirst, List.of()));
    }

    @Test
    void referenceWithLongerVariableNameStaysForTheVariable() {
        Map<String, Supplier<String>> placeholders = Map.of("value", () -> "true");

        assertEquals("$$value_backup / true", ValuePlaceholderHolder.applyValuePlaceholders("$$value_backup / $$value", placeholders, List.of("value_backup")));
    }

    @Test
    void valuePlaceholderWinsOverVariableWithTheSameOrAShorterName() {
        Map<String, Supplier<String>> placeholders = Map.of("value_backup", () -> "backup");

        assertEquals("backup", ValuePlaceholderHolder.applyValuePlaceholders("$$value_backup", placeholders, List.of("value_backup")));
        assertEquals("backup", ValuePlaceholderHolder.applyValuePlaceholders("$$value_backup", placeholders, List.of("value")));
    }

    @Test
    void nullValueBecomesEmptyText() {
        Map<String, Supplier<String>> placeholders = Map.of("value", () -> null);

        assertEquals("[]", ValuePlaceholderHolder.applyValuePlaceholders("[$$value]", placeholders, List.of()));
    }

    @Test
    void onlyValuesOfUsedReferencesGetRead() {
        AtomicInteger unusedReads = new AtomicInteger();
        Map<String, Supplier<String>> placeholders = new LinkedHashMap<>();
        placeholders.put("used", () -> "1");
        placeholders.put("unused", () -> String.valueOf(unusedReads.incrementAndGet()));

        assertEquals("1", ValuePlaceholderHolder.applyValuePlaceholders("$$used", placeholders, List.of()));
        assertEquals(0, unusedReads.get());
    }

    @Test
    void textWithoutReferencesStaysTheSameInstance() {
        String text = "value without references";

        assertSame(text, ValuePlaceholderHolder.applyValuePlaceholders(text, Map.of("value", () -> "true")));
    }

}
