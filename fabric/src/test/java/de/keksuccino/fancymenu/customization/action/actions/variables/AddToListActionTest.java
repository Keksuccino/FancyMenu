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

class AddToListActionTest {

    @NotNull
    private static String value(@NotNull String listName, @NotNull String index, boolean skipIfInList, boolean ignoreCase, @NotNull String entry) {
        AddToListAction.Config config = new AddToListAction.Config();
        config.listName = listName;
        config.index = index;
        config.skipIfInList = skipIfInList;
        config.ignoreCase = ignoreCase;
        config.entry = entry;
        return config.serialize();
    }

    @Test
    void valuesSurviveSavingAndLoading() {
        AddToListAction.Config config = AddToListAction.Config.parse(value("fruits", "{\"placeholder\":\"index\"}", true, true, "a ||| b|||"));
        assertNotNull(config);
        assertAll(() -> assertEquals("fruits", config.listName), () -> assertEquals("{\"placeholder\":\"index\"}", config.index), () -> assertTrue(config.skipIfInList), () -> assertTrue(config.ignoreCase), () -> assertEquals("a ||| b|||", config.entry));
    }

    @Test
    void aValueWithPartsMissingOrBrokenKeepsWhatANewActionStartsWith() {
        AddToListAction.Config onlyName = AddToListAction.Config.parse("fruits");
        assertNotNull(onlyName);
        assertAll(() -> assertEquals("fruits", onlyName.listName), () -> assertEquals("", onlyName.index), () -> assertFalse(onlyName.skipIfInList), () -> assertFalse(onlyName.ignoreCase), () -> assertEquals("", onlyName.entry));
        AddToListAction.Config broken = AddToListAction.Config.parse("fruits||||||maybe|||nope|||apple");
        assertNotNull(broken);
        assertAll(() -> assertFalse(broken.skipIfInList), () -> assertFalse(broken.ignoreCase), () -> assertEquals("apple", broken.entry));
        assertNull(AddToListAction.Config.parse(null));
        assertNotNull(AddToListAction.Config.parse(new AddToListAction().getValuePreset()));
    }

    @Test
    void entriesGetAddedAtTheEndOrAtTheIndex() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "apple", "banana");
        AddToListAction action = new AddToListAction(variables);

        action.execute(value(" fruits ", "", false, false, "cherry"));
        action.execute(value("fruits", "0", false, false, "kiwi"));
        action.execute(value("fruits", "-1", false, false, "lime"));
        // What a calculating placeholder answers with has a point in it, and indexes past the end add at the end
        action.execute(value("fruits", " 1.0 ", false, false, "mango"));
        action.execute(value("fruits", "100", false, false, "peach"));

        assertEquals(List.of("kiwi", "mango", "apple", "banana", "cherry", "lime", "peach"), variables.lists.get("fruits"));
    }

    @Test
    void entriesAlreadyInTheListGetSkippedIfWanted() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "Apple");
        AddToListAction action = new AddToListAction(variables);

        action.execute(value("fruits", "", true, false, "Apple"));
        action.execute(value("fruits", "", true, true, "APPLE"));
        action.execute(value("fruits", "", true, false, "apple"));
        action.execute(value("fruits", "", false, false, "Apple"));

        assertEquals(List.of("Apple", "apple", "Apple"), variables.lists.get("fruits"));
    }

    @Test
    void missingListsGetCreatedAndTextWithAValueStaysUntouched() {
        FakeListVariables variables = new FakeListVariables().withText("empty", "").withText("text", "hello");
        AddToListAction action = new AddToListAction(variables);

        action.execute(value("new", "", false, false, "a"));
        action.execute(value("empty", "", false, false, "b"));
        action.execute(value("text", "", false, false, "c"));

        assertAll(() -> assertEquals(List.of("a"), variables.lists.get("new")), () -> assertEquals(List.of("b"), variables.lists.get("empty")), () -> assertEquals("hello", variables.texts.get("text")), () -> assertNull(variables.lists.get("text")));
    }

    @Test
    void nothingHappensWithoutAListNameOrWithAnIndexThatIsNoWholeNumber() {
        FakeListVariables variables = new FakeListVariables();
        AddToListAction action = new AddToListAction(variables);

        action.execute(null);
        action.execute(value("  ", "", false, false, "a"));
        action.execute(value("fruits", "abc", false, false, "a"));
        action.execute(value("fruits", "1.5", false, false, "a"));

        assertTrue(variables.editedNames.isEmpty());
    }

}
