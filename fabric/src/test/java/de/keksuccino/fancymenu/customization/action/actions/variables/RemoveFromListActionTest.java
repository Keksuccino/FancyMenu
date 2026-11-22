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

class RemoveFromListActionTest {

    @NotNull
    private static String value(@NotNull String listName, @NotNull RemoveFromListAction.Mode mode, @NotNull String index, boolean ignoreCase, @NotNull String entry) {
        RemoveFromListAction.Config config = new RemoveFromListAction.Config();
        config.listName = listName;
        config.mode = mode;
        config.index = index;
        config.ignoreCase = ignoreCase;
        config.entry = entry;
        return config.serialize();
    }

    @Test
    void valuesSurviveSavingAndLoading() {
        for (RemoveFromListAction.Mode mode : RemoveFromListAction.Mode.values()) {
            RemoveFromListAction.Config config = RemoveFromListAction.Config.parse(value("fruits", mode, "3", true, "a|||b"));
            assertNotNull(config);
            assertAll(mode.name(), () -> assertEquals("fruits", config.listName), () -> assertEquals(mode, config.mode), () -> assertEquals("3", config.index), () -> assertTrue(config.ignoreCase), () -> assertEquals("a|||b", config.entry));
        }
    }

    @Test
    void aValueWithPartsMissingOrBrokenKeepsWhatANewActionStartsWith() {
        RemoveFromListAction.Config onlyName = RemoveFromListAction.Config.parse("fruits");
        assertNotNull(onlyName);
        assertAll(() -> assertEquals(RemoveFromListAction.Mode.AT_INDEX, onlyName.mode), () -> assertEquals("", onlyName.index), () -> assertFalse(onlyName.ignoreCase), () -> assertEquals("", onlyName.entry));
        RemoveFromListAction.Config broken = RemoveFromListAction.Config.parse("fruits|||everything|||0|||sure|||x");
        assertNotNull(broken);
        assertAll(() -> assertEquals(RemoveFromListAction.Mode.AT_INDEX, broken.mode), () -> assertFalse(broken.ignoreCase));
        assertNull(RemoveFromListAction.Config.parse(null));
        assertNotNull(RemoveFromListAction.Config.parse(new RemoveFromListAction().getValuePreset()));
    }

    @Test
    void theEntryAtTheIndexGetsRemoved() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "apple", "banana", "cherry", "kiwi");
        RemoveFromListAction action = new RemoveFromListAction(variables);

        action.execute(value("fruits", RemoveFromListAction.Mode.AT_INDEX, "1", false, ""));
        action.execute(value("fruits", RemoveFromListAction.Mode.AT_INDEX, "-1", false, ""));
        // An index the list doesn't have changes nothing
        action.execute(value("fruits", RemoveFromListAction.Mode.AT_INDEX, "5", false, ""));

        assertEquals(List.of("apple", "cherry"), variables.lists.get("fruits"));
    }

    @Test
    void entriesWithTheValueGetRemoved() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "Apple", "apple", "kiwi", "apple", "APPLE");
        RemoveFromListAction action = new RemoveFromListAction(variables);

        action.execute(value("fruits", RemoveFromListAction.Mode.FIRST_MATCH, "", false, "apple"));
        assertEquals(List.of("Apple", "kiwi", "apple", "APPLE"), variables.lists.get("fruits"));
        action.execute(value("fruits", RemoveFromListAction.Mode.FIRST_MATCH, "", true, "apple"));
        assertEquals(List.of("kiwi", "apple", "APPLE"), variables.lists.get("fruits"));
        action.execute(value("fruits", RemoveFromListAction.Mode.ALL_MATCHES, "", true, "APPLE"));
        assertEquals(List.of("kiwi"), variables.lists.get("fruits"));
    }

    @Test
    void nothingHappensWithoutAListNameOrAWholeNumberAsIndex() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "apple");
        RemoveFromListAction action = new RemoveFromListAction(variables);

        action.execute(null);
        action.execute(value(" ", RemoveFromListAction.Mode.ALL_MATCHES, "", false, "apple"));
        action.execute(value("fruits", RemoveFromListAction.Mode.AT_INDEX, "", false, "apple"));
        action.execute(value("fruits", RemoveFromListAction.Mode.AT_INDEX, "last", false, "apple"));

        assertAll(() -> assertTrue(variables.editedNames.isEmpty()), () -> assertEquals(List.of("apple"), variables.lists.get("fruits")));
    }

}
