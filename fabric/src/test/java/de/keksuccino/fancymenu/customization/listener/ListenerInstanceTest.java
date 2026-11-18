package de.keksuccino.fancymenu.customization.listener;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerInstanceTest {

    @Test
    void copySerializesLikeTheOriginal() {
        ListenerInstance original = newInstance(new TestListener());

        ListenerInstance copy = original.copy();

        assertNotSame(original, copy);
        assertNotSame(original.getActionScript(), copy.getActionScript());
        assertSame(original.parent, copy.parent);
        assertEquals(original.instanceIdentifier, copy.instanceIdentifier);
        assertEquals(original.serialize().getProperties(), copy.serialize().getProperties());
    }

    @Test
    void editingTheCopyKeepsTheOriginalUnchanged() {
        ListenerInstance original = newInstance(new TestListener());
        Map<String, String> originalState = original.serialize().getProperties();
        ListenerInstance copy = original.copy();

        copy.setDisplayName("Renamed listener");
        ((ActionInstance) copy.getActionScript().getExecutables().get(0)).value = "edited_variable:edited_value";
        ((FolderExecutableBlock) copy.getActionScript().getExecutables().get(1)).addExecutable(new ActionInstance(new SetVariableAction(), "added_variable:added_value"));

        assertEquals(originalState, original.serialize().getProperties());
    }

    @Test
    void copyIsNotRegisteredToItsListener() {
        TestListener listener = new TestListener();
        ListenerInstance original = newInstance(listener);
        listener.registerInstance(original);
        long revision = listener.getActiveInstanceRevision();

        original.copy();

        assertTrue(listener.isActiveAtRevision(revision));
    }

    @Test
    void copiedActionScriptKeepsTheListenerVariables() {
        ListenerInstance copy = newInstance(new TestListener()).copy();

        assertTrue(copy.getActionScript().getValuePlaceholders().containsKey(TestListener.VARIABLE_NAME));
    }

    private static ListenerInstance newInstance(@NotNull TestListener listener) {
        ListenerInstance instance = listener.createFreshInstance();
        instance.setDisplayName("Test listener");
        FolderExecutableBlock folder = new FolderExecutableBlock();
        folder.addExecutable(new ActionInstance(new SetVariableAction(), "folder_variable:folder_value"));
        instance.getActionScript().addExecutable(new ActionInstance(new SetVariableAction(), "first_variable:first_value")).addExecutable(folder);
        return instance;
    }

    private static final class TestListener extends AbstractListener {

        private static final String VARIABLE_NAME = "test_listener_variable";

        private TestListener() {
            super("listener_instance_copy_test");
        }

        @Override
        protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
            list.add(new CustomVariable(VARIABLE_NAME, () -> "test_value"));
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.literal(this.identifier);
        }

        @Override
        public @NotNull List<Component> getDescription() {
            return List.of();
        }

    }

}
