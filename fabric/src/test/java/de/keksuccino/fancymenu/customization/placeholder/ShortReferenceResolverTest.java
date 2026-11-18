package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ShortReferenceResolverTest {

    private static final UserVariableSnapshot COINS = variable("coins", "25");
    private static final UserVariableSnapshot COINS_RECORD = variable("coins_record", "80");

    @Test
    void longestVariableNameWinsInEveryOrder() {
        String text = "Record: $$coins_record, now: $$coins";

        assertEquals("Record: 80, now: 25", PlaceholderParser.resolveVariableReferences(text, List.of(COINS, COINS_RECORD)));
        assertEquals("Record: 80, now: 25", PlaceholderParser.resolveVariableReferences(text, List.of(COINS_RECORD, COINS)));
    }

    @Test
    void everyReferenceOfAVariableGetsReplaced() {
        assertEquals("25/25/80", PlaceholderParser.resolveVariableReferences("$$coins/$$coins/$$coins_record", List.of(COINS, COINS_RECORD)));
    }

    @Test
    void referenceDirectlyAfterADollarSignStillResolves() {
        assertEquals("$25", PlaceholderParser.resolveVariableReferences("$$$coins", List.of(COINS)));
    }

    @Test
    void textsWithoutKnownReferencesStayTheSameInstance() {
        String unknownReference = "$$unknown and $$ alone";
        String noReference = "coins_record";

        assertSame(unknownReference, PlaceholderParser.resolveVariableReferences(unknownReference, List.of(COINS, COINS_RECORD)));
        assertSame(noReference, PlaceholderParser.resolveVariableReferences(noReference, List.of(COINS, COINS_RECORD)));
        assertSame(unknownReference, PlaceholderParser.resolveVariableReferences(unknownReference, List.of()));
    }

    @Test
    void emptyVariableNameNeverMatches() {
        String text = "$$coins and $$";

        assertSame(text, PlaceholderParser.resolveVariableReferences(text, List.of(variable("", "empty"))));
    }

    @Test
    void replacementsAreNotSearchedForReferencesAgain() {
        List<UserVariableSnapshot> variables = List.of(variable("pointer", "$$coins"), COINS);

        assertEquals("$$coins|25", PlaceholderParser.resolveVariableReferences("$$pointer|$$coins", variables));
    }

    @Test
    void elementValueLeavesLongerVariableReferenceForTheVariable() {
        Map<String, Supplier<String>> checkboxValues = Map.of("value", () -> "true");
        List<UserVariableSnapshot> variables = List.of(variable("value_backup", "hello"));
        List<String> variableNames = variables.stream().map(UserVariableSnapshot::name).toList();

        String withValues = ValuePlaceholderHolder.applyValuePlaceholders("$$value_backup|$$value", checkboxValues, variableNames);

        assertEquals("$$value_backup|true", withValues);
        assertEquals("hello|true", PlaceholderParser.resolveVariableReferences(withValues, variables));
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

    private static UserVariableSnapshot variable(String name, String value) {
        return new UserVariableSnapshot(name, value, false);
    }

}
