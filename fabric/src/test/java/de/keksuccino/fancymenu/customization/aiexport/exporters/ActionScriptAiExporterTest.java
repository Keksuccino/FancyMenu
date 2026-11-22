package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ForEachExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionScriptAiExporterTest {

    @Test
    void nestedBlocksGetWrittenOneLevelDeeperThanTheirBlock() {
        TestAction action = new TestAction(true);
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(new ActionInstance(action, "top_level_value"));
        IfExecutableBlock ifBlock = new IfExecutableBlock();
        ifBlock.addExecutable(new ActionInstance(action, "then_branch_value"));
        ElseExecutableBlock elseBlock = new ElseExecutableBlock();
        elseBlock.addExecutable(new ActionInstance(action, "else_branch_value"));
        ifBlock.setAppendedBlock(elseBlock);
        script.addExecutable(ifBlock);
        FolderExecutableBlock folder = new FolderExecutableBlock();
        folder.setName("my_folder_name");
        folder.addExecutable(new ActionInstance(action, "folder_value"));
        script.addExecutable(folder);

        String[] lines = write(script, new AiExportReferences());

        int topLevel = indexOfLineContaining(lines, "top_level_value");
        int thenBranch = indexOfLineContaining(lines, "then_branch_value");
        int elseBranch = indexOfLineContaining(lines, "else_branch_value");
        int folderName = indexOfLineContaining(lines, "my_folder_name");
        int folderContent = indexOfLineContaining(lines, "folder_value");
        assertTrue(topLevel < thenBranch && thenBranch < elseBranch && elseBranch < folderName && folderName < folderContent);
        assertTrue(indentationOf(lines[thenBranch]) > indentationOf(lines[topLevel]));
        assertEquals(indentationOf(lines[thenBranch]), indentationOf(lines[elseBranch]));
        assertEquals(indentationOf(lines[topLevel]), indentationOf(lines[folderName]));
        assertTrue(indentationOf(lines[folderContent]) > indentationOf(lines[folderName]));
    }

    @Test
    void forEachLoopsGetWrittenWithTheirListAndTheirContentsOneLevelDeeper() {
        TestAction action = new TestAction(true);
        GenericExecutableBlock script = new GenericExecutableBlock();
        ForEachExecutableBlock loop = new ForEachExecutableBlock("my_fruit_list");
        loop.addExecutable(new ActionInstance(action, "loop_content_value"));
        script.addExecutable(loop);

        String[] lines = write(script, new AiExportReferences());

        int listName = indexOfLineContaining(lines, "my_fruit_list");
        int loopContent = indexOfLineContaining(lines, "loop_content_value");
        assertTrue(listName < loopContent);
        assertTrue(indentationOf(lines[loopContent]) > indentationOf(lines[listName]));
    }

    @Test
    void actionsWithoutValueDoNotShowTheirStoredValue() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(new ActionInstance(new TestAction(false), "stale_stored_value"));
        String text = String.join("\n", write(script, new AiExportReferences()));
        assertTrue(text.contains(TestAction.IDENTIFIER));
        assertFalse(text.contains("stale_stored_value"));
    }

    @Test
    void multiLineValuesStayComplete() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(new ActionInstance(new TestAction(true), "first_value_line\nsecond_value_line"));
        String[] lines = write(script, new AiExportReferences());
        assertTrue(indexOfLineContaining(lines, "second_value_line") == indexOfLineContaining(lines, "first_value_line") + 1);
    }

    @Test
    void valuePlaceholdersOfTheScriptGetListedWithTheirCurrentValue() {
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addValuePlaceholder("pressed_key", () -> "current_key_value");
        String text = String.join("\n", write(script, new AiExportReferences()));
        assertTrue(text.contains("$$pressed_key"));
        assertTrue(text.contains("current_key_value"));
    }

    @Test
    void usedActionsEndUpInTheReferences() {
        AiExportReferences references = new AiExportReferences();
        GenericExecutableBlock script = new GenericExecutableBlock();
        script.addExecutable(new ActionInstance(new TestAction(true), "value"));
        write(script, references);
        AiExportWriter referenceWriter = new AiExportWriter();
        references.write(referenceWriter);
        assertTrue(referenceWriter.build().contains(TestAction.IDENTIFIER));
    }

    private static String[] write(GenericExecutableBlock script, AiExportReferences references) {
        AiExportWriter writer = new AiExportWriter();
        ActionScriptAiExporter.writeScript(writer, script, references);
        return writer.build().split("\n");
    }

    private static int indexOfLineContaining(String[] lines, String token) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(token)) return i;
        }
        throw new AssertionError("No line contains " + token + ":\n" + String.join("\n", lines));
    }

    private static int indentationOf(String line) {
        return line.length() - line.stripLeading().length();
    }

    private static final class TestAction extends Action {

        private static final String IDENTIFIER = "test_ai_export_action";

        private final boolean hasValue;

        private TestAction(boolean hasValue) {
            super(IDENTIFIER);
            this.hasValue = hasValue;
        }

        @Override
        public boolean hasValue() {
            return this.hasValue;
        }

        @Override
        public void execute(@Nullable String value) {
        }

        @Override
        public boolean checkAsync() {
            return true;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Test Action");
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }

        @Override
        public Component getValueDisplayName() {
            return Component.empty();
        }

        @Override
        public String getValuePreset() {
            return "";
        }

    }

}
