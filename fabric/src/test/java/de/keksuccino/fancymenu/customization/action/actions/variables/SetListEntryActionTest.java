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

class SetListEntryActionTest {

    @NotNull
    private static String value(@NotNull String listName, @NotNull String index, @NotNull String entry) {
        SetListEntryAction.Config config = new SetListEntryAction.Config();
        config.listName = listName;
        config.index = index;
        config.entry = entry;
        return config.serialize();
    }

    @Test
    void valuesSurviveSavingAndLoading() {
        SetListEntryAction.Config config = SetListEntryAction.Config.parse(value("fruits", "-2", "|||x|||"));
        assertNotNull(config);
        assertAll(() -> assertEquals("fruits", config.listName), () -> assertEquals("-2", config.index), () -> assertEquals("|||x|||", config.entry));
        SetListEntryAction.Config onlyName = SetListEntryAction.Config.parse("fruits");
        assertNotNull(onlyName);
        assertAll(() -> assertEquals("", onlyName.index), () -> assertEquals("", onlyName.entry));
        assertNull(SetListEntryAction.Config.parse(null));
        assertNotNull(SetListEntryAction.Config.parse(new SetListEntryAction().getValuePreset()));
    }

    @Test
    void theEntryAtTheIndexGetsReplaced() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "apple", "banana", "cherry");
        SetListEntryAction action = new SetListEntryAction(variables);

        action.execute(value("fruits", "0", "kiwi"));
        action.execute(value("fruits", "-1", "lime"));
        action.execute(value("fruits", " 1.0 ", ""));

        assertEquals(List.of("kiwi", "", "lime"), variables.lists.get("fruits"));
    }

    @Test
    void indexesTheListDoesNotHaveChangeAndCreateNothing() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "apple");
        SetListEntryAction action = new SetListEntryAction(variables);

        action.execute(value("fruits", "1", "x"));
        action.execute(value("fruits", "-2", "x"));
        action.execute(value("missing", "0", "x"));

        assertAll(() -> assertEquals(List.of("apple"), variables.lists.get("fruits")), () -> assertFalse(variables.lists.containsKey("missing")));
    }

    @Test
    void nothingHappensWithoutAListNameOrAWholeNumberAsIndex() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "apple");
        SetListEntryAction action = new SetListEntryAction(variables);

        action.execute(null);
        action.execute(value("", "0", "x"));
        action.execute(value("fruits", "", "x"));
        action.execute(value("fruits", "first", "x"));

        assertTrue(variables.editedNames.isEmpty());
    }

}
