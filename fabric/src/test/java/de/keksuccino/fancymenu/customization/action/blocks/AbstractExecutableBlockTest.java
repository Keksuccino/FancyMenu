package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractExecutableBlockTest {

    @Test
    void addExecutableAppendsToTheEnd() {
        GenericExecutableBlock block = new GenericExecutableBlock();
        ActionInstance first = newAction();
        ActionInstance second = newAction();

        block.addExecutable(first).addExecutable(second);

        assertEquals(List.of(first, second), block.getExecutables());
    }

    @Test
    void addExecutableAtIndexInsertsAtThatIndex() {
        GenericExecutableBlock block = new GenericExecutableBlock();
        ActionInstance first = newAction();
        ActionInstance second = newAction();
        ActionInstance atStart = newAction();
        ActionInstance inMiddle = newAction();
        ActionInstance atEnd = newAction();
        block.addExecutable(first).addExecutable(second);

        block.addExecutable(0, atStart);
        block.addExecutable(2, inMiddle);
        block.addExecutable(4, atEnd);

        assertEquals(List.of(atStart, first, inMiddle, second, atEnd), block.getExecutables());
    }

    @Test
    void addExecutableAtIndexHandsTheBlockValuePlaceholdersToTheExecutable() {
        GenericExecutableBlock block = new GenericExecutableBlock();
        block.addExecutable(newAction());
        block.addValuePlaceholder("test_placeholder", () -> "test_value");
        ActionInstance inserted = newAction();

        block.addExecutable(0, inserted);

        assertTrue(inserted.getValuePlaceholders().containsKey("test_placeholder"));
    }

    @Test
    void addExecutableAtInvalidIndexLeavesTheBlockUnchanged() {
        GenericExecutableBlock block = new GenericExecutableBlock();
        ActionInstance existing = newAction();
        block.addExecutable(existing);

        assertThrows(IndexOutOfBoundsException.class, () -> block.addExecutable(2, newAction()));
        assertThrows(IndexOutOfBoundsException.class, () -> block.addExecutable(-1, newAction()));
        assertEquals(List.of(existing), block.getExecutables());
    }

    private static ActionInstance newAction() {
        return new ActionInstance(new SetVariableAction(), "test_variable:test_value");
    }

}
