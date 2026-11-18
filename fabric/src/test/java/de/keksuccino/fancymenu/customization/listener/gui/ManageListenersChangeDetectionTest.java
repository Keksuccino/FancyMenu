package de.keksuccino.fancymenu.customization.listener.gui;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManageListenersChangeDetectionTest {

    private final TestListener listener = new TestListener();

    @Test
    void unchangedCopiesCountAsUnchanged() {
        List<ListenerInstance> instances = this.newInstances();
        Map<String, ManageListenersWindowBody.ListenerState> initial = ManageListenersWindowBody.serializeListenersState(instances);

        List<ListenerInstance> copies = instances.stream().map(ListenerInstance::copy).toList();

        assertFalse(ManageListenersWindowBody.hasListenersChanged(initial, copies));
    }

    @Test
    void renamedListenerCountsAsChangedUntilItGetsItsOldNameBack() {
        List<ListenerInstance> instances = this.newInstances();
        Map<String, ManageListenersWindowBody.ListenerState> initial = ManageListenersWindowBody.serializeListenersState(instances);

        instances.get(0).setDisplayName("Renamed listener");
        assertTrue(ManageListenersWindowBody.hasListenersChanged(initial, instances));

        instances.get(0).setDisplayName("First listener");
        assertFalse(ManageListenersWindowBody.hasListenersChanged(initial, instances));
    }

    @Test
    void editedActionScriptCountsAsChanged() {
        List<ListenerInstance> instances = this.newInstances();
        Map<String, ManageListenersWindowBody.ListenerState> initial = ManageListenersWindowBody.serializeListenersState(instances);
        GenericExecutableBlock edited = instances.get(1).getActionScript().copy(false);

        ((ActionInstance) edited.getExecutables().get(0)).value = "edited_variable:edited_value";
        instances.get(1).setActionScript(edited);

        assertTrue(ManageListenersWindowBody.hasListenersChanged(initial, instances));
    }

    @Test
    void actionScriptSavedWithoutEditsCountsAsUnchanged() {
        List<ListenerInstance> instances = this.newInstances();
        Map<String, ManageListenersWindowBody.ListenerState> initial = ManageListenersWindowBody.serializeListenersState(instances);

        // The action script editor hands back a copy of the script when its Done button gets clicked.
        instances.get(1).setActionScript(instances.get(1).getActionScript().copy(false));

        assertFalse(ManageListenersWindowBody.hasListenersChanged(initial, instances));
    }

    @Test
    void collapsingBlocksInAnActionScriptCountsAsUnchanged() {
        List<ListenerInstance> instances = this.newInstances();
        Map<String, ManageListenersWindowBody.ListenerState> initial = ManageListenersWindowBody.serializeListenersState(instances);

        ((FolderExecutableBlock) instances.get(0).getActionScript().getExecutables().get(1)).setCollapsed(true);

        assertFalse(ManageListenersWindowBody.hasListenersChanged(initial, instances));
    }

    @Test
    void addedListenerCountsAsChanged() {
        List<ListenerInstance> instances = this.newInstances();
        Map<String, ManageListenersWindowBody.ListenerState> initial = ManageListenersWindowBody.serializeListenersState(instances);

        instances.add(this.listener.createFreshInstance());

        assertTrue(ManageListenersWindowBody.hasListenersChanged(initial, instances));
    }

    @Test
    void removedListenerCountsAsChanged() {
        List<ListenerInstance> instances = this.newInstances();
        Map<String, ManageListenersWindowBody.ListenerState> initial = ManageListenersWindowBody.serializeListenersState(instances);

        instances.remove(1);

        assertTrue(ManageListenersWindowBody.hasListenersChanged(initial, instances));
    }

    private List<ListenerInstance> newInstances() {
        List<ListenerInstance> instances = new ArrayList<>();
        instances.add(this.newInstance("First listener"));
        instances.add(this.newInstance(null));
        return instances;
    }

    private ListenerInstance newInstance(String displayName) {
        ListenerInstance instance = this.listener.createFreshInstance();
        instance.setDisplayName(displayName);
        FolderExecutableBlock folder = new FolderExecutableBlock();
        folder.addExecutable(new ActionInstance(new SetVariableAction(), "folder_variable:folder_value"));
        instance.getActionScript().addExecutable(new ActionInstance(new SetVariableAction(), "first_variable:first_value")).addExecutable(folder);
        return instance;
    }

    private static final class TestListener extends AbstractListener {

        private TestListener() {
            super("listener_change_detection_test");
        }

        @Override
        protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
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
