package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.CommentExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.DelayExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ExecuteLaterExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ForEachExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Moves the entries of an action script one step up or down, for the Move Up and Move Down features of the {@link ActionScriptEditorWindowBody}.<br>
 * It changes the script itself, so the editor only needs to rebuild its entry list afterwards.<br><br>
 *
 * Every step moves an entry to the next spot it can take in the editor's entry list, and a step down always undoes a step up:
 * <ul>
 *     <li>Entries swap places with a neighbor that shows no content: actions, comments and collapsed blocks.</li>
 *     <li>Entries enter an expanded neighbor block. Moving up puts them at the end of the block's last statement
 *     (the last ELSE-IF or ELSE of an IF chain), moving down puts them at the start of the block.</li>
 *     <li>Entries at the start or end of a block leave it, or move to the neighboring statement of their IF chain.</li>
 *     <li>ELSE-IF statements swap places with neighboring ELSE-IF statements, but never move above their IF or below an ELSE.</li>
 * </ul>
 * Blocks move together with their content and the rest of their IF chain. Entries that move to another block get the value placeholders
 * of that block, like the ones of a FOR EACH loop they move into.
 */
public final class ActionScriptEntryMover {

    private ActionScriptEntryMover() {
    }

    /**
     * If the executable can get moved anywhere in a script.<br>
     * ELSE-IF and ELSE statements belong to their IF chain, so they can't.
     */
    public static boolean canMoveFreely(@NotNull Executable executable) {
        if (!(executable instanceof AbstractExecutableBlock)) {
            return true;
        }
        return (executable instanceof IfExecutableBlock) || (executable instanceof WhileExecutableBlock) || (executable instanceof ForEachExecutableBlock) || (executable instanceof DelayExecutableBlock) || (executable instanceof ExecuteLaterExecutableBlock) || (executable instanceof FolderExecutableBlock) || (executable instanceof CommentExecutableBlock);
    }

    /**
     * If the editor hides the content of the block.<br>
     * Collapsing an IF also hides the rest of its chain.
     */
    public static boolean isCollapsed(@NotNull AbstractExecutableBlock block) {
        if (block instanceof FolderExecutableBlock folder) return folder.isCollapsed();
        if (block instanceof IfExecutableBlock ifBlock) return ifBlock.isCollapsed();
        if (block instanceof WhileExecutableBlock whileBlock) return whileBlock.isCollapsed();
        if (block instanceof ForEachExecutableBlock forEachBlock) return forEachBlock.isCollapsed();
        if (block instanceof DelayExecutableBlock delayBlock) return delayBlock.isCollapsed();
        if (block instanceof ExecuteLaterExecutableBlock executeLaterBlock) return executeLaterBlock.isCollapsed();
        return false;
    }

    /**
     * Moves the executable one step up.
     *
     * @param script The script that contains the executable.
     * @param beforeMove Runs right before the script changes, and only if the executable can move.
     * @return If the executable moved.
     */
    public static boolean moveUp(@NotNull AbstractExecutableBlock script, @NotNull Executable executable, @NotNull Runnable beforeMove) {
        if (executable instanceof ElseIfExecutableBlock elseIf) {
            return moveElseIfUp(script, elseIf, beforeMove);
        }
        if (!canMoveFreely(executable)) return false;
        AbstractExecutableBlock parent = findParentOf(script, executable);
        if (parent == null) return false;
        List<Executable> siblings = parent.getExecutables();
        int index = siblings.indexOf(executable);
        if (index > 0) {
            AbstractExecutableBlock enteredStatement = getLastStatementIfExpanded(siblings.get(index - 1));
            beforeMove.run();
            siblings.remove(index);
            if (enteredStatement != null) {
                enteredStatement.addExecutable(executable);
            } else {
                siblings.add(index - 1, executable);
            }
            return true;
        }
        if ((index < 0) || (parent == script)) return false;
        AbstractExecutableBlock previousStatement = findPreviousStatementOf(script, parent);
        if (previousStatement != null) {
            beforeMove.run();
            siblings.remove(0);
            previousStatement.addExecutable(executable);
            return true;
        }
        AbstractExecutableBlock grandparent = findParentOf(script, parent);
        if (grandparent == null) return false;
        beforeMove.run();
        siblings.remove(0);
        List<Executable> parentSiblings = grandparent.getExecutables();
        grandparent.addExecutable(parentSiblings.indexOf(parent), executable);
        return true;
    }

    /**
     * Moves the executable one step down.
     *
     * @param script The script that contains the executable.
     * @param beforeMove Runs right before the script changes, and only if the executable can move.
     * @return If the executable moved.
     */
    public static boolean moveDown(@NotNull AbstractExecutableBlock script, @NotNull Executable executable, @NotNull Runnable beforeMove) {
        if (executable instanceof ElseIfExecutableBlock elseIf) {
            return moveElseIfDown(script, elseIf, beforeMove);
        }
        if (!canMoveFreely(executable)) return false;
        AbstractExecutableBlock parent = findParentOf(script, executable);
        if (parent == null) return false;
        List<Executable> siblings = parent.getExecutables();
        int index = siblings.indexOf(executable);
        if (index < 0) return false;
        if (index < (siblings.size() - 1)) {
            Executable next = siblings.get(index + 1);
            beforeMove.run();
            siblings.remove(index);
            if ((next instanceof AbstractExecutableBlock nextBlock) && isExpandedContainer(nextBlock)) {
                nextBlock.addExecutable(0, executable);
            } else {
                siblings.add(index + 1, executable);
            }
            return true;
        }
        if (parent == script) return false;
        AbstractExecutableBlock nextStatement = parent.getAppendedBlock();
        if (nextStatement != null) {
            beforeMove.run();
            siblings.remove(index);
            nextStatement.addExecutable(0, executable);
            return true;
        }
        AbstractExecutableBlock chainStart = getChainStartOf(script, parent);
        AbstractExecutableBlock grandparent = findParentOf(script, chainStart);
        if (grandparent == null) return false;
        beforeMove.run();
        siblings.remove(index);
        List<Executable> parentSiblings = grandparent.getExecutables();
        grandparent.addExecutable(parentSiblings.indexOf(chainStart) + 1, executable);
        return true;
    }

    private static boolean moveElseIfUp(@NotNull AbstractExecutableBlock script, @NotNull ElseIfExecutableBlock elseIf, @NotNull Runnable beforeMove) {
        AbstractExecutableBlock previous = findPreviousStatementOf(script, elseIf);
        if (previous == null) return false;
        // The IF that starts the chain has no previous statement, so an ELSE-IF right below it can't move up
        AbstractExecutableBlock beforePrevious = findPreviousStatementOf(script, previous);
        if (beforePrevious == null) return false;
        beforeMove.run();
        previous.setAppendedBlock(elseIf.getAppendedBlock());
        elseIf.setAppendedBlock(previous);
        beforePrevious.setAppendedBlock(elseIf);
        return true;
    }

    private static boolean moveElseIfDown(@NotNull AbstractExecutableBlock script, @NotNull ElseIfExecutableBlock elseIf, @NotNull Runnable beforeMove) {
        if (!(elseIf.getAppendedBlock() instanceof ElseIfExecutableBlock next)) return false;
        AbstractExecutableBlock previous = findPreviousStatementOf(script, elseIf);
        if (previous == null) return false;
        beforeMove.run();
        elseIf.setAppendedBlock(next.getAppendedBlock());
        next.setAppendedBlock(elseIf);
        previous.setAppendedBlock(next);
        return true;
    }

    /**
     * If entries can get moved into the block, because it can hold entries and shows them.
     */
    private static boolean isExpandedContainer(@NotNull AbstractExecutableBlock block) {
        return !(block instanceof CommentExecutableBlock) && !isCollapsed(block);
    }

    /**
     * Returns the last statement of the block's chain (the block itself if it has no ELSE-IF or ELSE),
     * or NULL if the executable is no block or entries can't get moved into it.
     */
    @Nullable
    private static AbstractExecutableBlock getLastStatementIfExpanded(@NotNull Executable executable) {
        if (!(executable instanceof AbstractExecutableBlock block) || !isExpandedContainer(block)) return null;
        AbstractExecutableBlock last = block;
        while (last.getAppendedBlock() != null) {
            last = last.getAppendedBlock();
        }
        return last;
    }

    @NotNull
    private static AbstractExecutableBlock getChainStartOf(@NotNull AbstractExecutableBlock script, @NotNull AbstractExecutableBlock statement) {
        AbstractExecutableBlock start = statement;
        AbstractExecutableBlock previous = findPreviousStatementOf(script, start);
        while (previous != null) {
            start = previous;
            previous = findPreviousStatementOf(script, start);
        }
        return start;
    }

    /**
     * Returns the block that holds the executable in its list of executables.<br>
     * Returns NULL for ELSE-IF and ELSE statements, because they are appended to the previous statement of their chain instead.
     */
    @Nullable
    private static AbstractExecutableBlock findParentOf(@NotNull AbstractExecutableBlock block, @NotNull Executable executable) {
        for (Executable child : block.getExecutables()) {
            if (child == executable) return block;
            if (child instanceof AbstractExecutableBlock childBlock) {
                AbstractExecutableBlock parent = findParentOf(childBlock, executable);
                if (parent != null) return parent;
            }
        }
        AbstractExecutableBlock appended = block.getAppendedBlock();
        return (appended != null) ? findParentOf(appended, executable) : null;
    }

    /**
     * Returns the statement the given ELSE-IF or ELSE statement is appended to.
     */
    @Nullable
    private static AbstractExecutableBlock findPreviousStatementOf(@NotNull AbstractExecutableBlock block, @NotNull AbstractExecutableBlock statement) {
        AbstractExecutableBlock appended = block.getAppendedBlock();
        if (appended == statement) return block;
        for (Executable child : block.getExecutables()) {
            if (child instanceof AbstractExecutableBlock childBlock) {
                AbstractExecutableBlock previous = findPreviousStatementOf(childBlock, statement);
                if (previous != null) return previous;
            }
        }
        return (appended != null) ? findPreviousStatementOf(appended, statement) : null;
    }

}
