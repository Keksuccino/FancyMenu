package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
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
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scripts get written as text here: blocks show their content in brackets, and the statements of an IF chain get joined with "+".<br>
 * For example, "a, F(b), X(c) + Z()" is the action "a", the folder "F" holding the action "b", and the IF "X" holding the action "c" with the empty ELSE "Z".
 */
class ActionScriptEntryMoverTest {

    private final Map<Executable, String> names = new IdentityHashMap<>();
    private final AtomicInteger undoPoints = new AtomicInteger();

    @Test
    void commentsMoveUpAndDownLikeActions() {
        CommentExecutableBlock comment = this.comment("C");
        GenericExecutableBlock script = this.script(this.action("a"), comment, this.action("b"));

        assertTrue(this.moveUp(script, comment));
        assertEquals("C, a, b", this.describe(script));
        assertTrue(this.moveDown(script, comment));
        assertTrue(this.moveDown(script, comment));
        assertEquals("a, b, C", this.describe(script));
        assertEquals(3, this.undoPoints.get());
    }

    @Test
    void foldersMoveUpAndDownTogetherWithTheirContent() {
        FolderExecutableBlock folder = this.folder("F", this.action("b"), this.comment("c"));
        GenericExecutableBlock script = this.script(this.action("a"), folder, this.action("d"));

        assertTrue(this.moveUp(script, folder));
        assertEquals("F(b, c), a, d", this.describe(script));
        assertTrue(this.moveDown(script, folder));
        assertTrue(this.moveDown(script, folder));
        assertEquals("a, d, F(b, c)", this.describe(script));
    }

    @Test
    void foldersMoveIntoAndOutOfOtherBlocks() {
        FolderExecutableBlock folder = this.folder("F", this.action("b"));
        GenericExecutableBlock script = this.script(this.ifBlock("X", this.action("a")), folder);

        assertTrue(this.moveUp(script, folder));
        assertEquals("X(a, F(b))", this.describe(script));
        assertTrue(this.moveDown(script, folder));
        assertEquals("X(a), F(b)", this.describe(script));
    }

    @Test
    void entriesMoveUpPastCommentsInsteadOfStayingInPlace() {
        TestAction action = this.action("b");
        GenericExecutableBlock script = this.script(this.action("a"), this.comment("C"), action);

        assertTrue(this.moveUp(script, action));
        assertEquals("a, b, C", this.describe(script));
    }

    @Test
    void entriesSwapWithTheEntryAboveThemInsteadOfJumpingIntoTheBlockAboveThat() {
        TestAction action = this.action("c");
        GenericExecutableBlock script = this.script(this.ifBlock("X", this.action("a")), this.action("b"), action);

        assertTrue(this.moveUp(script, action));
        assertEquals("X(a), c, b", this.describe(script));
    }

    @Test
    void entriesSwapWithTheEntryAboveThemWhenAnEmptyBlockIsAboveThat() {
        TestAction action = this.action("b");
        GenericExecutableBlock script = this.script(this.folder("F"), this.action("a"), action);

        assertTrue(this.moveUp(script, action));
        assertEquals("F(), b, a", this.describe(script));
    }

    @Test
    void entriesEnterExpandedBlocksOneLevelAtATime() {
        TestAction movingUp = this.action("b");
        GenericExecutableBlock upScript = this.script(this.folder("F", this.ifBlock("X", this.action("a"))), movingUp);

        assertTrue(this.moveUp(upScript, movingUp));
        assertEquals("F(X(a), b)", this.describe(upScript));
        assertTrue(this.moveUp(upScript, movingUp));
        assertEquals("F(X(a, b))", this.describe(upScript));
        assertTrue(this.moveUp(upScript, movingUp));
        assertEquals("F(X(b, a))", this.describe(upScript));

        TestAction movingDown = this.action("d");
        GenericExecutableBlock downScript = this.script(movingDown, this.folder("G", this.folder("H", this.action("c"))));

        assertTrue(this.moveDown(downScript, movingDown));
        assertEquals("G(d, H(c))", this.describe(downScript));
        assertTrue(this.moveDown(downScript, movingDown));
        assertEquals("G(H(d, c))", this.describe(downScript));
    }

    @Test
    void entriesEnterEmptyBlocks() {
        TestAction action = this.action("a");
        GenericExecutableBlock script = this.script(this.folder("F"), action, this.folder("G"));

        assertTrue(this.moveUp(script, action));
        assertEquals("F(a), G()", this.describe(script));
        assertTrue(this.moveDown(script, action));
        assertTrue(this.moveDown(script, action));
        assertEquals("F(), G(a)", this.describe(script));
    }

    @Test
    void entriesMovingUpIntoAnIfChainEndUpInItsLastStatement() {
        TestAction action = this.action("d");
        GenericExecutableBlock script = this.script(this.chain(this.ifBlock("X", this.action("a")), this.elseIf("Y", this.action("b")), this.elseBlock("Z", this.action("c"))), action);

        assertTrue(this.moveUp(script, action));
        assertEquals("X(a) + Y(b) + Z(c, d)", this.describe(script));
    }

    @Test
    void entriesSkipCollapsedBlocks() {
        TestAction action = this.action("b");
        FolderExecutableBlock folder = this.folder("F", this.action("a"));
        folder.setCollapsed(true);
        IfExecutableBlock ifBlock = this.chain(this.ifBlock("X", this.action("c")), this.elseBlock("Z", this.action("d")));
        ifBlock.setCollapsed(true);
        GenericExecutableBlock script = this.script(folder, action, ifBlock);

        assertTrue(this.moveUp(script, action));
        assertEquals("b, F(a), X(c) + Z(d)", this.describe(script));
        assertTrue(this.moveDown(script, action));
        assertTrue(this.moveDown(script, action));
        assertEquals("F(a), X(c) + Z(d), b", this.describe(script));
    }

    @Test
    void entriesLeaveBlocksAtTheirStartAndEnd() {
        TestAction first = this.action("b");
        TestAction last = this.action("c");
        GenericExecutableBlock script = this.script(this.action("a"), this.folder("F", first, last), this.action("d"));

        assertTrue(this.moveUp(script, first));
        assertEquals("a, b, F(c), d", this.describe(script));
        assertTrue(this.moveDown(script, last));
        assertEquals("a, b, F(), c, d", this.describe(script));
    }

    @Test
    void entriesLeaveNestedBlocksOneLevelAtATime() {
        TestAction action = this.action("a");
        GenericExecutableBlock script = this.script(this.ifBlock("X", this.folder("F", action)));

        assertTrue(this.moveDown(script, action));
        assertEquals("X(F(), a)", this.describe(script));
        assertTrue(this.moveDown(script, action));
        assertEquals("X(F()), a", this.describe(script));
        assertFalse(this.moveDown(script, action));
    }

    @Test
    void entriesMoveBetweenTheStatementsOfAnIfChain() {
        TestAction inElseIf = this.action("b");
        TestAction inElse = this.action("c");
        GenericExecutableBlock script = this.script(this.chain(this.ifBlock("X", this.action("a")), this.elseIf("Y", inElseIf), this.elseBlock("Z", inElse)), this.action("d"));

        assertTrue(this.moveUp(script, inElseIf));
        assertEquals("X(a, b) + Y() + Z(c), d", this.describe(script));
        assertTrue(this.moveDown(script, inElseIf));
        assertEquals("X(a) + Y(b) + Z(c), d", this.describe(script));
        assertTrue(this.moveDown(script, inElseIf));
        assertEquals("X(a) + Y() + Z(b, c), d", this.describe(script));
        assertTrue(this.moveDown(script, inElse));
        assertEquals("X(a) + Y() + Z(b), c, d", this.describe(script));
    }

    @Test
    void ifBlocksMoveTogetherWithTheirChain() {
        IfExecutableBlock ifBlock = this.chain(this.ifBlock("X", this.action("b")), this.elseBlock("Z", this.action("c")));
        GenericExecutableBlock script = this.script(this.action("a"), ifBlock);

        assertTrue(this.moveUp(script, ifBlock));
        assertEquals("X(b) + Z(c), a", this.describe(script));
    }

    @Test
    void elseIfStatementsOnlySwapWithNeighboringElseIfStatements() {
        ElseIfExecutableBlock second = this.elseIf("Y2", this.action("b"));
        GenericExecutableBlock script = this.script(this.chain(this.ifBlock("X"), this.elseIf("Y1", this.action("a")), second, this.elseBlock("Z")));

        assertTrue(this.moveUp(script, second));
        assertEquals("X() + Y2(b) + Y1(a) + Z()", this.describe(script));
        assertFalse(this.moveUp(script, second));
        assertTrue(this.moveDown(script, second));
        assertEquals("X() + Y1(a) + Y2(b) + Z()", this.describe(script));
        assertFalse(this.moveDown(script, second));
        assertEquals("X() + Y1(a) + Y2(b) + Z()", this.describe(script));
        assertEquals(2, this.undoPoints.get());
    }

    @Test
    void entriesThatCantMoveChangeNothingAndCreateNoUndoPoint() {
        TestAction first = this.action("a");
        TestAction last = this.action("b");
        ElseExecutableBlock elseBlock = this.elseBlock("Z", this.action("c"));
        GenericExecutableBlock script = this.script(first, this.chain(this.ifBlock("X"), elseBlock), last);
        String before = this.describe(script);

        assertFalse(this.moveUp(script, first));
        assertFalse(this.moveDown(script, last));
        assertFalse(this.moveUp(script, elseBlock));
        assertFalse(this.moveDown(script, elseBlock));
        assertFalse(this.moveUp(script, this.action("not in script")));
        assertFalse(this.moveDown(script, this.action("not in script")));
        assertEquals(before, this.describe(script));
        assertEquals(0, this.undoPoints.get());
    }

    @Test
    void movingDownUndoesMovingUpAndTheOtherWayAround() {
        FolderExecutableBlock collapsedFolder = this.folder("H", this.action("h"));
        collapsedFolder.setCollapsed(true);
        IfExecutableBlock collapsedIf = this.chain(this.ifBlock("V", this.action("v")), this.elseBlock("W"));
        collapsedIf.setCollapsed(true);
        GenericExecutableBlock script = this.script(
                this.action("a"),
                this.comment("C"),
                this.folder("F", this.action("b"), this.folder("G", this.action("c")), this.folder("E")),
                collapsedFolder,
                this.chain(this.ifBlock("X", this.action("d"), this.comment("D")), this.elseIf("Y1", this.action("e")), this.elseIf("Y2"), this.elseBlock("Z", this.action("f"))),
                collapsedIf,
                this.action("g"));
        String original = this.describe(script);

        int checkedMoves = 0;
        for (Executable entry : this.collectShownEntries(script)) {
            if (this.moveUp(script, entry)) {
                assertTrue(this.moveDown(script, entry), this.names.get(entry));
                assertEquals(original, this.describe(script), this.names.get(entry));
                checkedMoves++;
            }
            if (this.moveDown(script, entry)) {
                assertTrue(this.moveUp(script, entry), this.names.get(entry));
                assertEquals(original, this.describe(script), this.names.get(entry));
                checkedMoves++;
            }
        }
        // Every shown entry can move both ways, except "a" (first), "g" (last), "Y1" (right below its IF), "Y2" (right above the ELSE) and the ELSE "Z"
        assertEquals(30, checkedMoves);
        assertEquals(checkedMoves * 2, this.undoPoints.get());
    }

    @Test
    void onlyStatementsOfIfChainsCantMoveFreely() {
        assertTrue(ActionScriptEntryMover.canMoveFreely(this.action("a")));
        assertTrue(ActionScriptEntryMover.canMoveFreely(new CommentExecutableBlock()));
        assertTrue(ActionScriptEntryMover.canMoveFreely(new FolderExecutableBlock()));
        assertTrue(ActionScriptEntryMover.canMoveFreely(new IfExecutableBlock()));
        assertTrue(ActionScriptEntryMover.canMoveFreely(new WhileExecutableBlock()));
        assertTrue(ActionScriptEntryMover.canMoveFreely(new ForEachExecutableBlock()));
        assertTrue(ActionScriptEntryMover.canMoveFreely(new DelayExecutableBlock()));
        assertTrue(ActionScriptEntryMover.canMoveFreely(new ExecuteLaterExecutableBlock()));
        assertFalse(ActionScriptEntryMover.canMoveFreely(new ElseIfExecutableBlock()));
        assertFalse(ActionScriptEntryMover.canMoveFreely(new ElseExecutableBlock()));
    }

    @Test
    void isCollapsedReadsEveryCollapsibleBlock() {
        FolderExecutableBlock folder = new FolderExecutableBlock();
        IfExecutableBlock ifBlock = new IfExecutableBlock();
        WhileExecutableBlock whileBlock = new WhileExecutableBlock();
        ForEachExecutableBlock forEachBlock = new ForEachExecutableBlock();
        DelayExecutableBlock delayBlock = new DelayExecutableBlock();
        ExecuteLaterExecutableBlock executeLaterBlock = new ExecuteLaterExecutableBlock();
        List<AbstractExecutableBlock> collapsible = List.of(folder, ifBlock, whileBlock, forEachBlock, delayBlock, executeLaterBlock);

        collapsible.forEach(block -> assertFalse(ActionScriptEntryMover.isCollapsed(block)));
        folder.setCollapsed(true);
        ifBlock.setCollapsed(true);
        whileBlock.setCollapsed(true);
        forEachBlock.setCollapsed(true);
        delayBlock.setCollapsed(true);
        executeLaterBlock.setCollapsed(true);
        collapsible.forEach(block -> assertTrue(ActionScriptEntryMover.isCollapsed(block)));
        assertFalse(ActionScriptEntryMover.isCollapsed(new CommentExecutableBlock()));
        assertFalse(ActionScriptEntryMover.isCollapsed(new ElseIfExecutableBlock()));
        assertFalse(ActionScriptEntryMover.isCollapsed(new ElseExecutableBlock()));
    }

    private boolean moveUp(@NotNull AbstractExecutableBlock script, @NotNull Executable executable) {
        return ActionScriptEntryMover.moveUp(script, executable, this.undoPoints::incrementAndGet);
    }

    private boolean moveDown(@NotNull AbstractExecutableBlock script, @NotNull Executable executable) {
        return ActionScriptEntryMover.moveDown(script, executable, this.undoPoints::incrementAndGet);
    }

    /**
     * Returns the entries the editor shows for the script, in the order it shows them.
     */
    @NotNull
    private List<Executable> collectShownEntries(@NotNull AbstractExecutableBlock script) {
        List<Executable> entries = new ArrayList<>();
        this.collectShownContent(script, entries);
        return entries;
    }

    private void collectShownContent(@NotNull AbstractExecutableBlock block, @NotNull List<Executable> entries) {
        for (Executable child : block.getExecutables()) {
            entries.add(child);
            if ((child instanceof AbstractExecutableBlock childBlock) && !ActionScriptEntryMover.isCollapsed(childBlock)) {
                this.collectShownContent(childBlock, entries);
                AbstractExecutableBlock statement = childBlock.getAppendedBlock();
                while (statement != null) {
                    entries.add(statement);
                    this.collectShownContent(statement, entries);
                    statement = statement.getAppendedBlock();
                }
            }
        }
    }

    @NotNull
    private String describe(@NotNull AbstractExecutableBlock block) {
        List<String> entries = new ArrayList<>();
        for (Executable child : block.getExecutables()) {
            entries.add(this.describeEntry(child));
        }
        return String.join(", ", entries);
    }

    @NotNull
    private String describeEntry(@NotNull Executable executable) {
        String name = this.names.get(executable);
        if (!(executable instanceof AbstractExecutableBlock block) || (block instanceof CommentExecutableBlock)) {
            return name;
        }
        String description = name + "(" + this.describe(block) + ")";
        AbstractExecutableBlock appended = block.getAppendedBlock();
        return (appended != null) ? (description + " + " + this.describeEntry(appended)) : description;
    }

    @NotNull
    private GenericExecutableBlock script(@NotNull Executable... content) {
        return this.fill(new GenericExecutableBlock(), "script", content);
    }

    @NotNull
    private TestAction action(@NotNull String name) {
        TestAction action = new TestAction();
        this.names.put(action, name);
        return action;
    }

    @NotNull
    private CommentExecutableBlock comment(@NotNull String name) {
        return this.fill(new CommentExecutableBlock(), name);
    }

    @NotNull
    private FolderExecutableBlock folder(@NotNull String name, @NotNull Executable... content) {
        return this.fill(new FolderExecutableBlock(), name, content);
    }

    @NotNull
    private IfExecutableBlock ifBlock(@NotNull String name, @NotNull Executable... content) {
        return this.fill(new IfExecutableBlock(), name, content);
    }

    @NotNull
    private ElseIfExecutableBlock elseIf(@NotNull String name, @NotNull Executable... content) {
        return this.fill(new ElseIfExecutableBlock(), name, content);
    }

    @NotNull
    private ElseExecutableBlock elseBlock(@NotNull String name, @NotNull Executable... content) {
        return this.fill(new ElseExecutableBlock(), name, content);
    }

    @NotNull
    private IfExecutableBlock chain(@NotNull IfExecutableBlock ifBlock, @NotNull AbstractExecutableBlock... statements) {
        AbstractExecutableBlock last = ifBlock;
        for (AbstractExecutableBlock statement : statements) {
            last.setAppendedBlock(statement);
            last = statement;
        }
        return ifBlock;
    }

    @NotNull
    private <T extends AbstractExecutableBlock> T fill(@NotNull T block, @NotNull String name, @NotNull Executable... content) {
        for (Executable executable : content) {
            block.addExecutable(executable);
        }
        this.names.put(block, name);
        return block;
    }

    private static final class TestAction implements Executable {

        private final String identifier = UUID.randomUUID().toString();

        @Override
        public void execute() {
        }

        @Override
        public @NotNull String getIdentifier() {
            return this.identifier;
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return new TestAction();
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("test_action");
        }

    }

}
