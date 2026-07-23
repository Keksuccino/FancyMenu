package de.keksuccino.fancymenu.platform.services;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleScreenBrandingLineCollectorTest {

    @Test
    void requestsMinecraftAndNaturalOrderForMultipleLines() {
        List<String> naturalLines = List.of("Minecraft 26.2", "NeoForge 26.2.0.7-beta (2 mods)");
        AtomicBoolean includedMinecraft = new AtomicBoolean();
        AtomicBoolean reversed = new AtomicBoolean(true);

        List<String> collected = TitleScreenBrandingLineCollector.collectTopToBottom((includeMinecraft, reverse, consumer) -> {
            includedMinecraft.set(includeMinecraft);
            reversed.set(reverse);
            emitLikeReversibleSource(naturalLines, includeMinecraft, reverse, consumer);
        }, Function.identity());

        assertTrue(includedMinecraft.get());
        assertFalse(reversed.get());
        assertEquals(naturalLines, collected);
    }

    @Test
    void preservesSingleLine() {
        List<String> collected = TitleScreenBrandingLineCollector.collectTopToBottom((includeMinecraft, reverse, consumer) -> consumer.accept(0, "Minecraft 26.2"), Function.identity());

        assertEquals(List.of("Minecraft 26.2"), collected);
    }

    @Test
    void preservesEmptySource() {
        List<String> collected = TitleScreenBrandingLineCollector.collectTopToBottom((includeMinecraft, reverse, consumer) -> {}, Function.identity());

        assertEquals(List.of(), collected);
    }

    private static void emitLikeReversibleSource(List<String> naturalLines, boolean includeMinecraft, boolean reverse, BiConsumer<Integer, String> consumer) {
        List<String> selectedLines = new ArrayList<>(includeMinecraft ? naturalLines : naturalLines.subList(1, naturalLines.size()));
        if (reverse) Collections.reverse(selectedLines);
        for (int index = 0; index < selectedLines.size(); index++) consumer.accept(index, selectedLines.get(index));
    }

}
