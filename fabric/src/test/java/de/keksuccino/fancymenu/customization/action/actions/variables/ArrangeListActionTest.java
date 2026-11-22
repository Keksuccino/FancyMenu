package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.variables.ListArrangement;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrangeListActionTest {

    @NotNull
    private static String value(@NotNull String listName, @NotNull ListArrangement arrangement) {
        ArrangeListAction.Config config = new ArrangeListAction.Config();
        config.listName = listName;
        config.arrangement = arrangement;
        return config.serialize();
    }

    @Test
    void everyArrangementSurvivesSavingAndLoading() {
        for (ListArrangement arrangement : ListArrangement.values()) {
            ArrangeListAction.Config config = ArrangeListAction.Config.parse(value("fruits", arrangement));
            assertNotNull(config);
            assertAll(arrangement.name(), () -> assertEquals("fruits", config.listName), () -> assertEquals(arrangement, config.arrangement));
        }
    }

    @Test
    void aValueWithPartsMissingOrBrokenKeepsWhatANewActionStartsWith() {
        ArrangeListAction.Config onlyName = ArrangeListAction.Config.parse("fruits");
        assertNotNull(onlyName);
        assertEquals(ListArrangement.SORT_ASCENDING, onlyName.arrangement);
        ArrangeListAction.Config broken = ArrangeListAction.Config.parse("fruits|||upside_down");
        assertNotNull(broken);
        assertEquals(ListArrangement.SORT_ASCENDING, broken.arrangement);
        ArrangeListAction.Config padded = ArrangeListAction.Config.parse("fruits||| reverse ");
        assertNotNull(padded);
        assertEquals(ListArrangement.REVERSE, padded.arrangement);
        assertNull(ArrangeListAction.Config.parse(null));
        assertNotNull(ArrangeListAction.Config.parse(new ArrangeListAction().getValuePreset()));
    }

    @Test
    void theListGetsArranged() {
        FakeListVariables variables = new FakeListVariables().withList("numbers", "10", "2", "x", "2", "1.5");
        ArrangeListAction action = new ArrangeListAction(variables);

        action.execute(value("numbers", ListArrangement.SORT_NUMBERS_ASCENDING));
        assertEquals(List.of("1.5", "2", "2", "10", "x"), variables.lists.get("numbers"));
        action.execute(value(" numbers ", ListArrangement.REMOVE_DUPLICATES));
        assertEquals(List.of("1.5", "2", "10", "x"), variables.lists.get("numbers"));
        action.execute(value("numbers", ListArrangement.REVERSE));
        assertEquals(List.of("x", "10", "2", "1.5"), variables.lists.get("numbers"));
    }

    @Test
    void nothingHappensWithoutAListName() {
        FakeListVariables variables = new FakeListVariables();
        ArrangeListAction action = new ArrangeListAction(variables);

        action.execute(value("", ListArrangement.SHUFFLE));
        action.execute(null);

        assertTrue(variables.editedNames.isEmpty());
    }

}
