package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link ShortReferenceResolver} on its own, with lookups that find names through {@link ShortReferenceResolver#findLongestName(String, int, Iterable, java.util.function.Function)}
 * like the lookups of value placeholders do. The resolution of FancyMenu's variables through their name index is covered by {@code VariableStoreReferenceResolutionTest}.
 */
class ShortReferenceResolverTest {

    @Test
    void longestNameWinsInEveryOrder() {
        String text = "Record: $$coins_record, now: $$coins";

        assertEquals("Record: 80, now: 25", resolve(text, values("coins", "25", "coins_record", "80")));
        assertEquals("Record: 80, now: 25", resolve(text, values("coins_record", "80", "coins", "25")));
    }

    @Test
    void findLongestNameFindsTheLongestNameThatStartsAtTheIndexInEveryOrder() {
        List<String> shortFirst = List.of("c", "coins", "coins_record", "money");
        List<String> longFirst = List.of("money", "coins_record", "coins", "c");
        String text = "$$coins_record|$$coins|$$cash|$$nothing";

        for (List<String> names : List.of(shortFirst, longFirst)) {
            assertEquals("coins_record", ShortReferenceResolver.findLongestName(text, 2, names, name -> name));
            assertEquals("coins", ShortReferenceResolver.findLongestName(text, text.indexOf("coins|"), names, name -> name));
            assertEquals("c", ShortReferenceResolver.findLongestName(text, text.indexOf("cash"), names, name -> name));
            assertNull(ShortReferenceResolver.findLongestName(text, text.indexOf("nothing"), names, name -> name));
            assertNull(ShortReferenceResolver.findLongestName(text, text.length(), names, name -> name));
        }
    }

    @Test
    void everyReferenceOfANameGetsReplaced() {
        assertEquals("25/25/80", resolve("$$coins/$$coins/$$coins_record", values("coins", "25", "coins_record", "80")));
    }

    @Test
    void referenceDirectlyAfterADollarSignStillResolves() {
        assertEquals("$25", resolve("$$$coins", values("coins", "25")));
    }

    @Test
    void textsWithoutReplacedReferencesStayTheSameInstance() {
        String unknownReference = "$$unknown and $$ alone";
        String noReference = "coins_record";
        String keptReference = "$$coins stays";

        assertSame(unknownReference, resolve(unknownReference, values("coins", "25", "coins_record", "80")));
        assertSame(noReference, resolve(noReference, values("coins", "25", "coins_record", "80")));
        assertSame(unknownReference, resolve(unknownReference, values()));
        assertSame(keptReference, ShortReferenceResolver.resolve(keptReference, (text, nameStart) -> new ShortReferenceResolver.Reference(5, null)));
    }

    @Test
    void emptyNameNeverMatches() {
        String text = "$$coins and $$";

        assertNull(ShortReferenceResolver.findLongestName(text, 2, List.of(""), name -> name));
        assertSame(text, resolve(text, values("", "empty")));
        assertEquals("25 and $$", resolve(text, values("", "empty", "coins", "25")));
    }

    @Test
    void replacementsAreNotSearchedForReferencesAgain() {
        assertEquals("$$coins|25", resolve("$$pointer|$$coins", values("pointer", "$$coins", "coins", "25")));
    }

    @Test
    void lookupGetsAskedOnceAtEveryPrefixThatIsNoPartOfAnEarlierReference() {
        List<Integer> askedAt = new ArrayList<>();
        AtomicInteger replaced = new AtomicInteger();

        String resolved = ShortReferenceResolver.resolve("$$a $$b$$a $$", (text, nameStart) -> {
            askedAt.add(nameStart);
            if (!text.startsWith("a", nameStart)) return null;
            return new ShortReferenceResolver.Reference(1, "A" + replaced.incrementAndGet());
        });

        assertEquals("A1 $$bA2 $$", resolved);
        assertEquals(List.of(2, 6, 9, 13), askedAt);
    }

    @Test
    void keptReferenceGetsSkippedCompletely() {
        // The lookup keeps "$$a$$b" as one reference, so the "$$b" inside of it must not get asked for.
        String resolved = ShortReferenceResolver.resolve("$$a$$b $$b", (text, nameStart) -> {
            if (text.startsWith("a$$b", nameStart)) return new ShortReferenceResolver.Reference(4, null);
            if (text.startsWith("b", nameStart)) return new ShortReferenceResolver.Reference(1, "B");
            return null;
        });

        assertEquals("$$a$$b B", resolved);
    }

    @NotNull
    private static String resolve(@NotNull String text, @NotNull Map<String, String> values) {
        return ShortReferenceResolver.resolve(text, (in, nameStart) -> {
            String name = ShortReferenceResolver.findLongestName(in, nameStart, values.keySet(), n -> n);
            return (name != null) ? new ShortReferenceResolver.Reference(name.length(), values.get(name)) : null;
        });
    }

    /** Names and values in the given order, as name, value, name, value and so on. */
    @NotNull
    private static Map<String, String> values(@NotNull String... namesAndValues) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) values.put(namesAndValues[i], namesAndValues[i + 1]);
        return values;
    }

}
