package de.keksuccino.fancymenu.customization.action.actions.variables;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetListActionTest {

    @NotNull
    private static SetListAction.Config split(@NotNull String text, @NotNull String separator, boolean trimEntries, boolean skipEmptyEntries) {
        SetListAction.Config config = new SetListAction.Config();
        config.listName = "list";
        config.source = SetListAction.Source.SPLIT_TEXT;
        config.text = text;
        config.separator = separator;
        config.trimEntries = trimEntries;
        config.skipEmptyEntries = skipEmptyEntries;
        return config;
    }

    @NotNull
    private static SetListAction.Config json(@NotNull String text) {
        SetListAction.Config config = new SetListAction.Config();
        config.listName = "list";
        config.source = SetListAction.Source.JSON_ARRAY;
        config.text = text;
        return config;
    }

    @Test
    void valuesSurviveSavingAndLoadingWhateverTheSeparatorIs() {
        for (String separator : List.of(",", "|", "||", "|||", "a|", "|a", "%", "%7C", "%25", "%%7c|", "", "\\n", " - ")) {
            SetListAction.Config config = split("x|||y|", separator, false, true);
            SetListAction.Config loaded = SetListAction.Config.parse(config.serialize());
            assertNotNull(loaded);
            assertAll(separator, () -> assertEquals("list", loaded.listName), () -> assertEquals(SetListAction.Source.SPLIT_TEXT, loaded.source), () -> assertEquals(separator, loaded.separator), () -> assertFalse(loaded.trimEntries),
                    () -> assertTrue(loaded.skipEmptyEntries), () -> assertEquals("x|||y|", loaded.text));
        }
        SetListAction.Config loadedJson = SetListAction.Config.parse(json("[\"a\",\"b\"]").serialize());
        assertNotNull(loadedJson);
        assertAll(() -> assertEquals(SetListAction.Source.JSON_ARRAY, loadedJson.source), () -> assertEquals("[\"a\",\"b\"]", loadedJson.text));
    }

    @Test
    void separatorsOnlyGetEncodedWhereTheyHaveToBe() {
        assertAll(() -> assertEquals(",", SetListAction.Config.encodeSeparator(",")), () -> assertEquals("%7C", SetListAction.Config.encodeSeparator("|")), () -> assertEquals("%25", SetListAction.Config.encodeSeparator("%")),
                // A "%" that starts no code stays as it is
                () -> assertEquals("50%", SetListAction.Config.decodeSeparator("50%")), () -> assertEquals("|", SetListAction.Config.decodeSeparator("%7c")));
    }

    @Test
    void aValueWithPartsMissingOrBrokenKeepsWhatANewActionStartsWith() {
        SetListAction.Config onlyName = SetListAction.Config.parse("list");
        assertNotNull(onlyName);
        assertAll(() -> assertEquals(SetListAction.Source.SPLIT_TEXT, onlyName.source), () -> assertEquals(",", onlyName.separator), () -> assertTrue(onlyName.trimEntries), () -> assertTrue(onlyName.skipEmptyEntries), () -> assertEquals("", onlyName.text));
        SetListAction.Config broken = SetListAction.Config.parse("list|||xml|||;|||on|||off|||a;b");
        assertNotNull(broken);
        assertAll(() -> assertEquals(SetListAction.Source.SPLIT_TEXT, broken.source), () -> assertEquals(";", broken.separator), () -> assertTrue(broken.trimEntries), () -> assertTrue(broken.skipEmptyEntries), () -> assertEquals("a;b", broken.text));
        assertNull(SetListAction.Config.parse(null));
        SetListAction.Config preset = SetListAction.Config.parse(new SetListAction().getValuePreset());
        assertNotNull(preset);
        assertEquals(List.of("apple", "banana", "cherry"), preset.buildEntries());
    }

    @Test
    void textGetsSplitAtTheSeparator() {
        assertAll(() -> assertEquals(List.of("a", "b", "c"), split(" a, b ,,c ", ",", true, true).buildEntries()), () -> assertEquals(List.of(" a", " b ", "", "c "), split(" a, b ,,c ", ",", false, false).buildEntries()),
                () -> assertEquals(List.of("a", "b", "c"), split("a|b|c", "|", true, true).buildEntries()), () -> assertEquals(List.of("a.b", "c"), split("a.b||c", "||", true, true).buildEntries()),
                () -> assertEquals(List.of(), split("", ",", true, false).buildEntries()));
    }

    @Test
    void anEmptySeparatorSplitsAtLineBreaksAlsoTypedInOnes() {
        assertAll(() -> assertEquals(List.of("a", "b", "c"), split("a\nb\r\nc", "", true, true).buildEntries()), () -> assertEquals(List.of("a", "b", "c"), split("a\\nb\\n\\nc", "", true, true).buildEntries()),
                () -> assertEquals(List.of("a", "b"), split("a\\nb", "\\n", true, true).buildEntries()), () -> assertEquals(List.of("a\\nb"), split("a\\nb", ",", true, true).buildEntries()));
    }

    @Test
    void jsonArraysBecomeTheEntriesAndOtherTextTheOnlyEntry() {
        assertAll(() -> assertEquals(List.of("a", "b|||c"), json("[\"a\",\"b|||c\"]").buildEntries()), () -> assertEquals(List.of(), json("  ").buildEntries()), () -> assertEquals(List.of("[\"a\""), json("[\"a\"").buildEntries()),
                () -> assertEquals(List.of("hello"), json("hello").buildEntries()));
    }

    @Test
    void theListGetsAllNewEntriesAndTextWithAValueStaysUntouched() {
        FakeListVariables variables = new FakeListVariables().withList("list", "old").withText("text", "a,b");
        SetListAction action = new SetListAction(variables);

        action.execute(split("x, y", ",", true, true).serialize());
        SetListAction.Config text = split("a,b", ",", true, true);
        text.listName = "text";
        action.execute(text.serialize());
        SetListAction.Config created = json("[\"1\",\"2\"]");
        created.listName = " created ";
        action.execute(created.serialize());

        assertAll(() -> assertEquals(List.of("x", "y"), variables.lists.get("list")), () -> assertEquals("a,b", variables.texts.get("text")), () -> assertNull(variables.lists.get("text")), () -> assertEquals(List.of("1", "2"), variables.lists.get("created")));
    }

    @Test
    void nothingHappensWithoutAListName() {
        FakeListVariables variables = new FakeListVariables();
        SetListAction action = new SetListAction(variables);
        SetListAction.Config config = split("a,b", ",", true, true);
        config.listName = "  ";

        action.execute(config.serialize());
        action.execute(null);

        assertTrue(variables.editedNames.isEmpty());
    }

}
