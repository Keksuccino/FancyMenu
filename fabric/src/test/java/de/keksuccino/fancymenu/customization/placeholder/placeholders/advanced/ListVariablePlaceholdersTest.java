package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.testing.TestLocalizations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The list placeholders read the entries through a lookup that answers like {@code VariableHandler#getListEntries(String)}: the entries of
 * list variables, and null for missing variables and text variables. The lists here stand in for the variables, so the tests never touch
 * the variables of the game directory.
 */
class ListVariablePlaceholdersTest {

    private static final List<String> FRUITS = List.of("apple", "Banana", "cherry", "banana");
    private static final Map<String, List<String>> LISTS = Map.of("fruits", FRUITS, "empty", List.of(), "single", List.of("only"), "with_empty", List.of("a", ""), "some_list", List.of("some_entry", "other"));
    /** Missing variables and text variables both have no entries, so the lookup answers both with null. */
    private static final Function<String, List<String>> LOOKUP = LISTS::get;

    private static final ListGetPlaceholder GET = new ListGetPlaceholder(LOOKUP);
    private static final ListSizePlaceholder SIZE = new ListSizePlaceholder(LOOKUP);
    private static final ListIndexOfPlaceholder INDEX_OF = new ListIndexOfPlaceholder(LOOKUP);
    private static final ListJoinPlaceholder JOIN = new ListJoinPlaceholder(LOOKUP);

    /**
     * @param values Value names and values, alternating. Values that are left out are missing, like in a placeholder that doesn't have them.
     */
    private static String replace(@NotNull AbstractListVariablePlaceholder placeholder, @NotNull String... values) {
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i], values[i + 1]);
        return placeholder.getReplacementFor(new DeserializedPlaceholderString(placeholder.getIdentifier(), map, ""));
    }

    @Test
    void identifiersStayTheSame() {
        assertAll(() -> assertEquals("list_get", GET.getIdentifier()), () -> assertEquals("list_size", SIZE.getIdentifier()), () -> assertEquals("list_index_of", INDEX_OF.getIdentifier()), () -> assertEquals("list_join", JOIN.getIdentifier()));
    }

    @Test
    void getReturnsTheEntryAtTheIndexStartingAtZero() {
        assertAll(() -> assertEquals("apple", replace(GET, "name", "fruits", "index", "0")), () -> assertEquals("Banana", replace(GET, "name", "fruits", "index", "1")), () -> assertEquals("banana", replace(GET, "name", "fruits", "index", "3")));
    }

    @Test
    void getCountsNegativeIndexesFromTheEnd() {
        assertAll(() -> assertEquals("banana", replace(GET, "name", "fruits", "index", "-1")), () -> assertEquals("cherry", replace(GET, "name", "fruits", "index", "-2")), () -> assertEquals("apple", replace(GET, "name", "fruits", "index", "-4")));
    }

    @Test
    void getReadsIndexesFromCalculatingPlaceholdersAndIgnoresWhitespace() {
        assertAll(() -> assertEquals("cherry", replace(GET, "name", "fruits", "index", "2.0")), () -> assertEquals("Banana", replace(GET, "name", "fruits", "index", " 1 ")), () -> assertEquals("banana", replace(GET, "name", "fruits", "index", "-1.0")));
    }

    @Test
    void getReturnsTheFallbackForIndexesWithoutAnEntry() {
        assertAll(() -> assertEquals("none", replace(GET, "name", "fruits", "index", "4", "fallback", "none")), () -> assertEquals("none", replace(GET, "name", "fruits", "index", "-5", "fallback", "none")),
                () -> assertEquals("none", replace(GET, "name", "fruits", "index", "2147483647", "fallback", "none")), () -> assertEquals("none", replace(GET, "name", "fruits", "index", "-2147483648", "fallback", "none")),
                () -> assertEquals("none", replace(GET, "name", "empty", "index", "0", "fallback", "none")), () -> assertEquals("none", replace(GET, "name", "empty", "index", "-1", "fallback", "none")));
    }

    @Test
    void getReturnsTheFallbackForInvalidOrMissingIndexes() {
        assertAll(() -> assertEquals("none", replace(GET, "name", "fruits", "index", "abc", "fallback", "none")), () -> assertEquals("none", replace(GET, "name", "fruits", "index", "1.5", "fallback", "none")),
                () -> assertEquals("none", replace(GET, "name", "fruits", "index", "", "fallback", "none")), () -> assertEquals("none", replace(GET, "name", "fruits", "index", "99999999999", "fallback", "none")),
                () -> assertEquals("none", replace(GET, "name", "fruits", "fallback", "none")));
    }

    @Test
    void getReturnsTheFallbackForMissingAndTextVariables() {
        assertAll(() -> assertEquals("none", replace(GET, "name", "text_or_missing", "index", "0", "fallback", "none")), () -> assertEquals("none", replace(GET, "index", "0", "fallback", "none")), () -> assertEquals("none", replace(GET, "name", "  ", "index", "0", "fallback", "none")));
    }

    @Test
    void getFallsBackToEmptyTextWithoutAFallbackValue() {
        assertAll(() -> assertEquals("", replace(GET, "name", "fruits", "index", "9")), () -> assertEquals("", replace(GET, "name", "missing", "index", "0")));
    }

    @Test
    void getKeepsTheFallbackForEmptyEntries() {
        // An empty entry is an entry, so it doesn't get replaced with the fallback
        assertEquals("", replace(GET, "name", "with_empty", "index", "1", "fallback", "none"));
    }

    @Test
    void namesGetTrimmedLikeEverywhereElse() {
        assertAll(() -> assertEquals("apple", replace(GET, "name", " fruits ", "index", "0")), () -> assertEquals("4", replace(SIZE, "name", "\tfruits ")), () -> assertEquals("1", replace(INDEX_OF, "name", " fruits", "value", "Banana")), () -> assertEquals("only", replace(JOIN, "name", "single ")));
    }

    @Test
    void entriesComeStraightFromTheListWithoutCopies() {
        assertAll(() -> assertSame(FRUITS.get(2), replace(GET, "name", "fruits", "index", "2")), () -> assertSame(LISTS.get("single").get(0), replace(JOIN, "name", "single")));
    }

    @Test
    void sizeCountsTheEntries() {
        assertAll(() -> assertEquals("4", replace(SIZE, "name", "fruits")), () -> assertEquals("1", replace(SIZE, "name", "single")), () -> assertEquals("2", replace(SIZE, "name", "with_empty")), () -> assertEquals("0", replace(SIZE, "name", "empty")));
    }

    @Test
    void sizeIsZeroForMissingAndTextVariables() {
        assertAll(() -> assertEquals("0", replace(SIZE, "name", "text_or_missing")), () -> assertEquals("0", replace(SIZE)), () -> assertEquals("0", replace(SIZE, "name", "")));
    }

    @Test
    void indexOfFindsTheFirstMatchingEntry() {
        List<String> duplicates = List.of("x", "y", "x");
        ListIndexOfPlaceholder placeholder = new ListIndexOfPlaceholder(name -> duplicates);
        assertAll(() -> assertEquals("0", replace(INDEX_OF, "name", "fruits", "value", "apple")), () -> assertEquals("3", replace(INDEX_OF, "name", "fruits", "value", "banana")), () -> assertEquals("1", replace(INDEX_OF, "name", "fruits", "value", "Banana")),
                () -> assertEquals("0", replace(placeholder, "name", "duplicates", "value", "x")));
    }

    @Test
    void indexOfOnlyMatchesWholeEntries() {
        assertAll(() -> assertEquals("-1", replace(INDEX_OF, "name", "fruits", "value", "app")), () -> assertEquals("-1", replace(INDEX_OF, "name", "fruits", "value", "apple ")), () -> assertEquals("-1", replace(INDEX_OF, "name", "fruits", "value", "grape")));
    }

    @Test
    void indexOfCanIgnoreCase() {
        assertAll(() -> assertEquals("1", replace(INDEX_OF, "name", "fruits", "value", "BANANA", "ignore_case", "true")), () -> assertEquals("1", replace(INDEX_OF, "name", "fruits", "value", "banana", "ignore_case", " TRUE ")),
                () -> assertEquals("-1", replace(INDEX_OF, "name", "fruits", "value", "BANANA", "ignore_case", "false")), () -> assertEquals("-1", replace(INDEX_OF, "name", "fruits", "value", "BANANA")),
                () -> assertEquals("-1", replace(INDEX_OF, "name", "fruits", "value", "BANANA", "ignore_case", "maybe")));
    }

    @Test
    void indexOfLooksForAnEmptyEntryWithoutAValue() {
        assertAll(() -> assertEquals("1", replace(INDEX_OF, "name", "with_empty")), () -> assertEquals("-1", replace(INDEX_OF, "name", "fruits")));
    }

    @Test
    void indexOfIsMinusOneForMissingTextAndEmptyLists() {
        assertAll(() -> assertEquals("-1", replace(INDEX_OF, "name", "text_or_missing", "value", "apple")), () -> assertEquals("-1", replace(INDEX_OF, "name", "empty", "value", "")), () -> assertEquals("-1", replace(INDEX_OF, "value", "apple")));
    }

    @Test
    void joinUsesTheSeparatorOrACommaByDefault() {
        assertAll(() -> assertEquals("apple, Banana, cherry, banana", replace(JOIN, "name", "fruits")), () -> assertEquals("apple | Banana | cherry | banana", replace(JOIN, "name", "fruits", "separator", " | ")),
                () -> assertEquals("appleBananacherrybanana", replace(JOIN, "name", "fruits", "separator", "")), () -> assertEquals("a;", replace(JOIN, "name", "with_empty", "separator", ";")));
    }

    @Test
    void joinTurnsLineBreakCodesOfTheSeparatorIntoLineBreaks() {
        assertAll(() -> assertEquals("apple\nBanana\ncherry\nbanana", replace(JOIN, "name", "fruits", "separator", "\\n")), () -> assertEquals("apple -\n- Banana -\n- cherry -\n- banana", replace(JOIN, "name", "fruits", "separator", " -\\n- ")));
    }

    @Test
    void joinNeedsNoSeparatorForOneEntry() {
        assertEquals("only", replace(JOIN, "name", "single", "separator", " | "));
    }

    @Test
    void joinIsEmptyForMissingTextAndEmptyLists() {
        assertAll(() -> assertEquals("", replace(JOIN, "name", "text_or_missing")), () -> assertEquals("", replace(JOIN, "name", "empty")), () -> assertEquals("", replace(JOIN)));
    }

    @Test
    void defaultPlaceholderStringsOnlyUseKnownValuesAndWork() {
        List<AbstractListVariablePlaceholder> placeholders = List.of(GET, SIZE, INDEX_OF, JOIN);
        List<String> expected = List.of("some_entry", "2", "0", "some_entry, other");
        List<String> actual = new ArrayList<>();
        for (AbstractListVariablePlaceholder placeholder : placeholders) {
            DeserializedPlaceholderString defaults = placeholder.getDefaultPlaceholderString();
            assertEquals(placeholder.getIdentifier(), defaults.placeholderIdentifier);
            assertEquals("name", placeholder.getValueNames().get(0));
            assertTrue(placeholder.getValueNames().containsAll(defaults.values.keySet()), placeholder.getIdentifier() + " has default values it doesn't know: " + defaults.values.keySet());
            actual.add(placeholder.getReplacementFor(defaults));
        }
        assertEquals(expected, actual);
    }

    @Test
    void everyTextIsLocalizedInEveryLanguage() throws Exception {
        List<String> keys = new ArrayList<>();
        for (String identifier : List.of("list_get", "list_size", "list_index_of", "list_join")) {
            keys.add("fancymenu.placeholders." + identifier);
            keys.add("fancymenu.placeholders." + identifier + ".desc");
        }
        TestLocalizations.assertLocalizedEverywhere(keys);
    }

}
