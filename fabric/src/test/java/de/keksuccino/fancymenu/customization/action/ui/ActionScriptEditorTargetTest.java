package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.CommentExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.DelayExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ExecuteLaterExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ActionScriptEditorTargetTest {

    @Test
    void targetBelowABlockIsTheStartOfTheBlock() {
        GenericExecutableBlock root = new GenericExecutableBlock();
        List<AbstractExecutableBlock> blocks = List.of(new IfExecutableBlock(), new ElseIfExecutableBlock(), new ElseExecutableBlock(), new WhileExecutableBlock(), new DelayExecutableBlock(), new ExecuteLaterExecutableBlock(), new FolderExecutableBlock());
        for (AbstractExecutableBlock block : blocks) {
            block.addExecutable(newAction()).addExecutable(newAction()).addExecutable(newAction());
            root.addExecutable(block);

            ActionScriptEditorWindowBody.MoveTarget target = ActionScriptEditorWindowBody.getTargetBelow(block, root);

            assertSame(block, target.parent(), block.getBlockType());
            assertEquals(0, target.index(), block.getBlockType());
        }
    }

    @Test
    void targetBelowACommentIsRightAfterTheComment() {
        GenericExecutableBlock root = new GenericExecutableBlock();
        CommentExecutableBlock comment = new CommentExecutableBlock();
        root.addExecutable(newAction()).addExecutable(comment).addExecutable(newAction());

        ActionScriptEditorWindowBody.MoveTarget target = ActionScriptEditorWindowBody.getTargetBelow(comment, root);

        assertSame(root, target.parent());
        assertEquals(2, target.index());
    }

    @Test
    void targetBelowAnActionIsRightAfterTheActionInItsParentBlock() {
        FolderExecutableBlock folder = new FolderExecutableBlock();
        ActionInstance first = newAction();
        ActionInstance last = newAction();
        folder.addExecutable(first).addExecutable(newAction()).addExecutable(last);

        ActionScriptEditorWindowBody.MoveTarget afterFirst = ActionScriptEditorWindowBody.getTargetBelow(first, folder);
        ActionScriptEditorWindowBody.MoveTarget afterLast = ActionScriptEditorWindowBody.getTargetBelow(last, folder);

        assertSame(folder, afterFirst.parent());
        assertEquals(1, afterFirst.index());
        assertSame(folder, afterLast.parent());
        assertEquals(3, afterLast.index());
    }

    @Test
    void targetBelowAnActionOutsideTheGivenParentIsTheEndOfTheParent() {
        GenericExecutableBlock root = new GenericExecutableBlock();
        root.addExecutable(newAction()).addExecutable(newAction());

        ActionScriptEditorWindowBody.MoveTarget target = ActionScriptEditorWindowBody.getTargetBelow(newAction(), root);

        assertSame(root, target.parent());
        assertEquals(2, target.index());
    }

    private static ActionInstance newAction() {
        return new ActionInstance(new SetVariableAction(), "test_variable:test_value");
    }

}
