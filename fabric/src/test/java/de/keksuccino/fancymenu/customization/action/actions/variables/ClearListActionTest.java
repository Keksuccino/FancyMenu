package de.keksuccino.fancymenu.customization.action.actions.variables;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearListActionTest {

    @Test
    void theValueIsOnlyTheListName() {
        assertEquals("my_list", new ClearListAction().getValuePreset());
    }

    @Test
    void allEntriesGetRemovedAndTheListStays() {
        FakeListVariables variables = new FakeListVariables().withList("fruits", "apple", "banana").withText("empty", "").withText("text", "hello");
        ClearListAction action = new ClearListAction(variables);

        action.execute(" fruits ");
        action.execute("empty");
        action.execute("text");

        assertAll(() -> assertEquals(List.of(), variables.lists.get("fruits")), () -> assertEquals(List.of(), variables.lists.get("empty")), () -> assertEquals("hello", variables.texts.get("text")), () -> assertNull(variables.lists.get("text")));
    }

    @Test
    void nothingHappensWithoutAListName() {
        FakeListVariables variables = new FakeListVariables();
        ClearListAction action = new ClearListAction(variables);

        action.execute(null);
        action.execute("");
        action.execute("   ");

        assertTrue(variables.editedNames.isEmpty());
    }

}
