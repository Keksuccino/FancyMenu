package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import de.keksuccino.fancymenu.customization.action.blocks.CommentExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.DelayExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ExecuteLaterExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ForEachExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionScriptEditorChangeDetectionTest {

    @Test
    void unchangedScriptCountsAsUnchanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        assertFalse(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void undoSnapshotCopiesOfTheUnchangedScriptCountAsUnchanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        // Undo and redo restore a copy of a copy of the script.
        assertFalse(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root.copy(false).copy(false)));
    }

    @Test
    void scriptCountsAsUnchangedAfterUndoingAnEdit() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);
        GenericExecutableBlock snapshot = script.root.copy(false);

        script.firstAction.value = "edited_variable:edited_value";
        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));

        assertFalse(ActionScriptEditorWindowBody.hasScriptChanged(initial, snapshot.copy(false)));
    }

    @Test
    void editedActionValueCountsAsChanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.firstAction.value = "edited_variable:edited_value";

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void addedEntryCountsAsChangedUntilItGetsRemovedAgain() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);
        ActionInstance added = newAction("added_variable:added_value");

        script.folder.addExecutable(added);
        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));

        script.folder.removeExecutable(added);
        assertFalse(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void movedEntryCountsAsChanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.root.removeExecutable(script.firstAction);
        script.root.addExecutable(script.firstAction);

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void renamedFolderCountsAsChanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.folder.setName("Renamed Folder");

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void editedCommentCountsAsChanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.comment.setComment("Edited comment");

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void editedConditionCountsAsChanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.ifBlock.condition.createAndAddGroup("added_group", RequirementGroup.GroupMode.OR);

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void editedDelayCountsAsChanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.delayBlock.setDelayMs("2500");

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void editedForEachListCountsAsChanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.forEachBlock.setListVariableName("other_list");

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void collapsingOrExpandingBlocksCountsAsUnchanged() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.setAllCollapsed(true);
        assertFalse(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));

        script.setAllCollapsed(false);
        assertFalse(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    @Test
    void editCountsAsChangedWhenBlocksGotCollapsedToo() {
        TestScript script = TestScript.create();
        Map<String, String> initial = ActionScriptEditorWindowBody.serializeScriptState(script.root);

        script.setAllCollapsed(true);
        script.firstAction.value = "edited_variable:edited_value";

        assertTrue(ActionScriptEditorWindowBody.hasScriptChanged(initial, script.root));
    }

    private static ActionInstance newAction(String value) {
        return new ActionInstance(new SetVariableAction(), value);
    }

    private static final class TestScript {

        private final GenericExecutableBlock root = new GenericExecutableBlock();
        private final ActionInstance firstAction = newAction("first_variable:first_value");
        private final FolderExecutableBlock folder = new FolderExecutableBlock();
        private final CommentExecutableBlock comment = new CommentExecutableBlock();
        private final IfExecutableBlock ifBlock = new IfExecutableBlock();
        private final WhileExecutableBlock whileBlock = new WhileExecutableBlock();
        private final ForEachExecutableBlock forEachBlock = new ForEachExecutableBlock("for_each_list");
        private final DelayExecutableBlock delayBlock = new DelayExecutableBlock("500");
        private final ExecuteLaterExecutableBlock executeLaterBlock = new ExecuteLaterExecutableBlock();

        private static TestScript create() {
            TestScript script = new TestScript();
            script.folder.addExecutable(script.comment).addExecutable(newAction("folder_variable:folder_value"));
            ElseIfExecutableBlock elseIfBlock = new ElseIfExecutableBlock();
            elseIfBlock.addExecutable(newAction("else_if_variable:else_if_value"));
            ElseExecutableBlock elseBlock = new ElseExecutableBlock();
            elseBlock.addExecutable(newAction("else_variable:else_value"));
            elseIfBlock.setAppendedBlock(elseBlock);
            script.ifBlock.addExecutable(newAction("if_variable:if_value"));
            script.ifBlock.setAppendedBlock(elseIfBlock);
            script.whileBlock.addExecutable(newAction("while_variable:while_value"));
            script.forEachBlock.addExecutable(newAction("for_each_variable:for_each_value"));
            script.delayBlock.addExecutable(newAction("delay_variable:delay_value"));
            script.executeLaterBlock.addExecutable(newAction("later_variable:later_value"));
            script.root.addExecutable(script.firstAction).addExecutable(script.folder).addExecutable(script.ifBlock).addExecutable(script.whileBlock).addExecutable(script.forEachBlock).addExecutable(script.delayBlock).addExecutable(script.executeLaterBlock);
            return script;
        }

        private void setAllCollapsed(boolean collapsed) {
            this.folder.setCollapsed(collapsed);
            this.ifBlock.setCollapsed(collapsed);
            this.whileBlock.setCollapsed(collapsed);
            this.forEachBlock.setCollapsed(collapsed);
            this.delayBlock.setCollapsed(collapsed);
            this.executeLaterBlock.setCollapsed(collapsed);
        }

    }

}
