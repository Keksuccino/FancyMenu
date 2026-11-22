package de.keksuccino.fancymenu.customization.variables;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableListsTest {

    @Test
    void textFormIsACompactJsonArrayThatRoundTripsEveryCharacter() {
        List<String> entries = List.of("plain", "quote \" and backslash \\", "line\nbreak\r\nand\ttab", "control \u0001 \u001f", "Grüße 😀", "{\"placeholder\":\"calc\"} $$name", "", " spaced ", "  ");

        String text = VariableLists.toText(entries);

        assertAll(() -> assertEquals(entries, VariableLists.parseText(text)), () -> assertEquals(-1, text.indexOf('\n')), () -> assertEquals(-1, text.indexOf('\r')), () -> assertEquals("[]", VariableLists.toText(List.of())), () -> assertEquals("[\"a\",\"b\"]", VariableLists.toText(List.of("a", "b"))));
    }

    @Test
    void parsedJsonKeepsTheTextOfNumbersBooleansAndNestedValues() {
        List<String> entries = VariableLists.parseText(" [1.50, -3, true, null, {\"a\": 1}, [1, 2], \"x\"] ");

        assertEquals(List.of("1.50", "-3", "true", "", "{\"a\":1}", "[1,2]", "x"), entries);
    }

    @Test
    void parsingRejectsEverythingThatIsNotExactlyOneStrictJsonArray() {
        assertAll(() -> assertNull(VariableLists.parseText("")), () -> assertNull(VariableLists.parseText("   ")), () -> assertNull(VariableLists.parseText("apple")), () -> assertNull(VariableLists.parseText("[")),
                () -> assertNull(VariableLists.parseText("[\"a\",]")), () -> assertNull(VariableLists.parseText("[apple, banana]")), () -> assertNull(VariableLists.parseText("[\"a\"] [\"b\"]")),
                () -> assertNull(VariableLists.parseText("{\"a\":1}")), () -> assertNull(VariableLists.parseText("\"[1]\"")), () -> assertNull(VariableLists.parseText("[note] hello")), () -> assertNull(VariableLists.parseText("['single']")));
    }

    @Test
    void assignedTextBecomesEntriesAnEmptyListOrTheOnlyEntry() {
        assertAll(() -> assertEquals(List.of(), VariableLists.fromAssignedText(null)), () -> assertEquals(List.of(), VariableLists.fromAssignedText("")), () -> assertEquals(List.of(), VariableLists.fromAssignedText("  \n ")),
                () -> assertEquals(List.of("a", "b"), VariableLists.fromAssignedText("[\"a\",\"b\"]")), () -> assertEquals(List.of(" sword "), VariableLists.fromAssignedText(" sword ")),
                () -> assertEquals(List.of("[not, json"), VariableLists.fromAssignedText("[not, json")));
    }

    @Test
    void singleValueTextIsTheExactInverseOfAssigningText() {
        List<List<String>> lists = List.of(List.of(), List.of("hello"), List.of("true"), List.of(" spaced "), List.of(""), List.of("  "), List.of("[\"x\"]"), List.of("[]"), List.of("[not json"), List.of("a", "b"), List.of("line\nbreak"));

        for (List<String> entries : lists) {
            assertEquals(entries, VariableLists.fromAssignedText(VariableLists.toSingleValueText(entries)), "Round trip of " + entries);
        }
        assertAll(() -> assertEquals("", VariableLists.toSingleValueText(List.of())), () -> assertEquals("hello", VariableLists.toSingleValueText(List.of("hello"))),
                () -> assertEquals("[\"a\",\"b\"]", VariableLists.toSingleValueText(List.of("a", "b"))), () -> assertEquals("[\"[]\"]", VariableLists.toSingleValueText(List.of("[]"))));
    }

    @Test
    void splittingUsesPlainTextSeparatorsAndLineBreaksForAnEmptySeparator() {
        assertAll(() -> assertEquals(List.of("a", "b", "c"), VariableLists.split("a.b.c", ".", false, false)), () -> assertEquals(List.of("a", "b"), VariableLists.split("a||b", "||", false, false)),
                () -> assertEquals(List.of("a", "", "b", ""), VariableLists.split("a,,b,", ",", false, false)), () -> assertEquals(List.of("a", "b"), VariableLists.split(" a , , b ,", ",", true, true)),
                () -> assertEquals(List.of(" a ", " b"), VariableLists.split(" a , b", ",", false, true)), () -> assertEquals(List.of(), VariableLists.split("", ",", false, false)),
                () -> assertEquals(List.of("one", "two", "three", "four"), VariableLists.split("one\ntwo\r\nthree\rfour", "", false, false)), () -> assertEquals(List.of("no separator"), VariableLists.split("no separator", ";", false, false)));
    }

    @Test
    void indexesAcceptWholeNumbersInAnyNotation() {
        assertAll(() -> assertEquals(2, VariableLists.parseIndex("2")), () -> assertEquals(-1, VariableLists.parseIndex(" -1 ")), () -> assertEquals(2, VariableLists.parseIndex("2.0")), () -> assertEquals(3, VariableLists.parseIndex("+3")),
                () -> assertEquals(1000, VariableLists.parseIndex("1e3")), () -> assertNull(VariableLists.parseIndex("2.5")), () -> assertNull(VariableLists.parseIndex("abc")), () -> assertNull(VariableLists.parseIndex("")),
                () -> assertNull(VariableLists.parseIndex(null)), () -> assertNull(VariableLists.parseIndex("99999999999")), () -> assertNull(VariableLists.parseIndex("NaN")), () -> assertNull(VariableLists.parseIndex("Infinity")));
    }

    @Test
    void entryIndexesCountFromTheEndWhenNegative() {
        assertAll(() -> assertEquals(0, VariableLists.resolveEntryIndex(0, 3)), () -> assertEquals(2, VariableLists.resolveEntryIndex(2, 3)), () -> assertEquals(-1, VariableLists.resolveEntryIndex(3, 3)),
                () -> assertEquals(2, VariableLists.resolveEntryIndex(-1, 3)), () -> assertEquals(0, VariableLists.resolveEntryIndex(-3, 3)), () -> assertEquals(-1, VariableLists.resolveEntryIndex(-4, 3)),
                () -> assertEquals(-1, VariableLists.resolveEntryIndex(0, 0)), () -> assertEquals(-1, VariableLists.resolveEntryIndex(Integer.MIN_VALUE, 3)), () -> assertEquals(-1, VariableLists.resolveEntryIndex(Integer.MAX_VALUE, 3)));
    }

    @Test
    void insertIndexesGiveTheNewEntryThatIndexAndClampToTheEnds() {
        assertAll(() -> assertEquals(0, VariableLists.resolveInsertIndex(0, 3)), () -> assertEquals(3, VariableLists.resolveInsertIndex(3, 3)), () -> assertEquals(3, VariableLists.resolveInsertIndex(10, 3)),
                () -> assertEquals(3, VariableLists.resolveInsertIndex(-1, 3)), () -> assertEquals(2, VariableLists.resolveInsertIndex(-2, 3)), () -> assertEquals(0, VariableLists.resolveInsertIndex(-10, 3)),
                () -> assertEquals(0, VariableLists.resolveInsertIndex(Integer.MIN_VALUE, 3)), () -> assertEquals(0, VariableLists.resolveInsertIndex(-1, 0)));
    }

    @Test
    void addingAppendsInsertsAndCanSkipEntriesTheListHas() {
        List<String> entries = new ArrayList<>(List.of("a", "b"));

        assertAll(() -> assertEquals(ListEditResult.CHANGED, VariableLists.add(entries, "c", null, false, false)), () -> assertEquals(ListEditResult.CHANGED, VariableLists.add(entries, "first", 0, false, false)),
                () -> assertEquals(ListEditResult.CHANGED, VariableLists.add(entries, "last", -1, false, false)), () -> assertEquals(ListEditResult.CHANGED, VariableLists.add(entries, "far", 99, false, false)),
                () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.add(entries, "a", null, true, false)), () -> assertEquals(ListEditResult.CHANGED, VariableLists.add(entries, "A", null, true, false)),
                () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.add(entries, "B", null, true, true)), () -> assertEquals(List.of("first", "a", "b", "c", "last", "far", "A"), entries));
    }

    @Test
    void settingAndRemovingByIndexLeaveTheListAloneWhenTheIndexHasNoEntry() {
        List<String> entries = new ArrayList<>(List.of("a", "b", "c"));

        assertAll(() -> assertEquals(ListEditResult.INDEX_OUT_OF_RANGE, VariableLists.set(entries, 3, "x")), () -> assertEquals(ListEditResult.INDEX_OUT_OF_RANGE, VariableLists.removeAt(entries, -4)),
                () -> assertEquals(List.of("a", "b", "c"), entries), () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.set(entries, 1, "b")), () -> assertEquals(ListEditResult.CHANGED, VariableLists.set(entries, -1, "z")),
                () -> assertEquals(ListEditResult.CHANGED, VariableLists.removeAt(entries, 0)), () -> assertEquals(List.of("b", "z"), entries));
    }

    @Test
    void removingByValueRemovesTheFirstOrAllMatches() {
        List<String> first = new ArrayList<>(List.of("a", "B", "a", "b"));
        List<String> all = new ArrayList<>(List.of("a", "B", "a", "b"));

        assertAll(() -> assertEquals(ListEditResult.CHANGED, VariableLists.remove(first, "a", false, false)), () -> assertEquals(List.of("B", "a", "b"), first),
                () -> assertEquals(ListEditResult.CHANGED, VariableLists.remove(all, "b", true, true)), () -> assertEquals(List.of("a", "a"), all),
                () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.remove(all, "A", true, false)), () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.remove(all, "x", false, true)));
    }

    @Test
    void clearingAndReplacingOnlyReportChangesThatHappened() {
        List<String> entries = new ArrayList<>(List.of("a"));

        assertAll(() -> assertEquals(ListEditResult.UNCHANGED, VariableLists.replaceAll(entries, List.of("a"))), () -> assertEquals(ListEditResult.CHANGED, VariableLists.replaceAll(entries, List.of("x", "y"))),
                () -> assertEquals(List.of("x", "y"), entries), () -> assertEquals(ListEditResult.CHANGED, VariableLists.clear(entries)), () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.clear(entries)),
                () -> assertThrows(NullPointerException.class, () -> VariableLists.replaceAll(entries, Arrays.asList("ok", null))), () -> assertEquals(List.of(), entries));
    }

    @Test
    void alphabeticalSortsIgnoreCaseAndSortAccentsNextToTheirLetters() {
        List<String> ascending = new ArrayList<>(List.of("Zitrone", "Äpfel", "birne", "apfel", "Birne"));
        List<String> descending = new ArrayList<>(ascending);

        VariableLists.arrange(ascending, ListArrangement.SORT_ASCENDING, new Random(1));
        VariableLists.arrange(descending, ListArrangement.SORT_DESCENDING, new Random(1));

        assertAll(() -> assertEquals(List.of("apfel", "Äpfel", "birne", "Birne", "Zitrone"), ascending), () -> assertEquals(List.of("Zitrone", "Birne", "birne", "Äpfel", "apfel"), descending));
    }

    @Test
    void numberSortsPutEntriesThatAreNoNumbersAtTheEndInTheirOrder() {
        List<String> ascending = new ArrayList<>(List.of("10", "x", "2", "-1.5", "y", " 3 ", "NaN"));
        List<String> descending = new ArrayList<>(ascending);

        VariableLists.arrange(ascending, ListArrangement.SORT_NUMBERS_ASCENDING, new Random(1));
        VariableLists.arrange(descending, ListArrangement.SORT_NUMBERS_DESCENDING, new Random(1));

        assertAll(() -> assertEquals(List.of("-1.5", "2", " 3 ", "10", "x", "y", "NaN"), ascending), () -> assertEquals(List.of("10", " 3 ", "2", "-1.5", "x", "y", "NaN"), descending));
    }

    @Test
    void reversingShufflingAndRemovingDuplicatesKeepTheEntries() {
        List<String> original = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        List<String> reversed = new ArrayList<>(original);
        List<String> shuffled = new ArrayList<>(original);
        List<String> duplicates = new ArrayList<>(List.of("a", "b", "a", "c", "b"));

        VariableLists.arrange(reversed, ListArrangement.REVERSE, new Random(1));
        ListEditResult shuffleResult = VariableLists.arrange(shuffled, ListArrangement.SHUFFLE, new Random(42));
        VariableLists.arrange(duplicates, ListArrangement.REMOVE_DUPLICATES, new Random(1));

        assertAll(() -> assertEquals(List.of("j", "i", "h", "g", "f", "e", "d", "c", "b", "a"), reversed), () -> assertEquals(ListEditResult.CHANGED, shuffleResult), () -> assertNotEquals(original, shuffled),
                () -> assertEquals(original, shuffled.stream().sorted().toList()), () -> assertEquals(List.of("a", "b", "c"), duplicates));
    }

    @Test
    void arrangingReportsUnchangedListsAndNeverTouchesShortOnes() {
        List<String> sorted = new ArrayList<>(List.of("a", "b"));
        List<String> single = new ArrayList<>(List.of("only"));

        assertAll(() -> assertEquals(ListEditResult.UNCHANGED, VariableLists.arrange(sorted, ListArrangement.SORT_ASCENDING, new Random(1))), () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.arrange(sorted, ListArrangement.REMOVE_DUPLICATES, new Random(1))),
                () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.arrange(single, ListArrangement.SHUFFLE, new Random(1))), () -> assertEquals(ListEditResult.UNCHANGED, VariableLists.arrange(new ArrayList<>(), ListArrangement.REVERSE, new Random(1))));
    }

    @Test
    void snapshotsKnowTheirTypeAndRejectEntriesForText() {
        UserVariableSnapshot list = UserVariableSnapshot.ofList("fruits", List.of("apple", "banana"), true);
        UserVariableSnapshot text = new UserVariableSnapshot("name", "Steve", false);

        assertAll(() -> assertEquals(VariableType.LIST, list.type()), () -> assertEquals("[\"apple\",\"banana\"]", list.value()), () -> assertEquals(List.of("apple", "banana"), list.listEntries()),
                () -> assertEquals(VariableType.TEXT, text.type()), () -> assertEquals(List.of(), text.listEntries()),
                () -> assertThrows(IllegalArgumentException.class, () -> new UserVariableSnapshot("bad", "x", false, VariableType.TEXT, List.of("entry"))),
                () -> assertThrows(UnsupportedOperationException.class, () -> list.listEntries().add("x")));
    }

}
