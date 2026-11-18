package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
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
import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Describes action scripts in AI exports, with all their actions, blocks and conditions.
 */
public final class ActionScriptAiExporter {

    static final String SCRIPT_INTRO = "Action scripts run their actions from top to bottom. Blocks like IF contain more actions, which are listed one level deeper. "
            + "The \"right now\" results of conditions were checked at the moment of the export.";

    private ActionScriptAiExporter() {
    }

    /**
     * Exports the script of the action script editor.
     */
    @NotNull
    public static AiExportDocument export(@NotNull GenericExecutableBlock script) {
        return AiExports.create("Action Script", "action_script", writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Action Script");
            writer.paragraph(SCRIPT_INTRO);
            writeScript(writer, script, references);
            references.write(writer);
        });
    }

    /**
     * Writes the value placeholders the script gets and all its actions and blocks, at the current indentation.
     */
    public static void writeScript(@NotNull AiExportWriter writer, @NotNull AbstractExecutableBlock script, @NotNull AiExportReferences references) {
        references.markScriptBlocksUsed();
        Map<String, Supplier<String>> valuePlaceholders = script.getValuePlaceholders();
        if (!valuePlaceholders.isEmpty()) {
            writer.bullet("**Value placeholders this script gets:**");
            writer.nested(() -> valuePlaceholders.forEach((name, valueSupplier) -> {
                String lastValue = AiExportText.safe(valueSupplier, "");
                writer.field("$$" + name, lastValue, "its value right now, which is usually the value of the last time the script ran");
            }));
        }
        writer.bullet("**Actions:**");
        writer.nested(() -> writeExecutables(writer, script.getExecutables(), references));
    }

    private static void writeExecutables(@NotNull AiExportWriter writer, @NotNull List<Executable> executables, @NotNull AiExportReferences references) {
        if (executables.isEmpty()) {
            writer.bullet("(nothing)");
            return;
        }
        for (Executable executable : executables) {
            writeExecutable(writer, executable, references);
        }
    }

    private static void writeExecutable(@NotNull AiExportWriter writer, @NotNull Executable executable, @NotNull AiExportReferences references) {
        if (executable instanceof ActionInstance instance) {
            writeAction(writer, instance, references);
        } else if (executable instanceof IfExecutableBlock ifBlock) {
            writer.bullet("**IF**");
            writer.nested(() -> {
                RequirementAiExporter.writeContainer(writer, "Condition", ifBlock.condition, references, true);
                writeBlockContent(writer, "Then", ifBlock, references);
            });
            writeAppendedBlocks(writer, ifBlock.getAppendedBlock(), references);
        } else if (executable instanceof WhileExecutableBlock whileBlock) {
            writer.bullet("**WHILE**");
            writer.nested(() -> {
                RequirementAiExporter.writeContainer(writer, "Condition", whileBlock.condition, references, true);
                writeBlockContent(writer, "Repeats", whileBlock, references);
            });
        } else if (executable instanceof DelayExecutableBlock delayBlock) {
            writer.bullet("**DELAY GATE** of " + AiExportWriter.inlineCode(delayBlock.getDelayMsRaw()) + " milliseconds");
            writer.nested(() -> writeBlockContent(writer, "Contains", delayBlock, references));
        } else if (executable instanceof ExecuteLaterExecutableBlock executeLaterBlock) {
            writer.bullet("**EXECUTE LATER** after " + AiExportWriter.inlineCode(executeLaterBlock.getDelayMsRaw()) + " milliseconds");
            writer.nested(() -> writeBlockContent(writer, "Contains", executeLaterBlock, references));
        } else if (executable instanceof FolderExecutableBlock folderBlock) {
            writer.bullet("**FOLDER** " + AiExportWriter.inlineCode(folderBlock.getName()));
            writer.nested(() -> writeBlockContent(writer, "Contains", folderBlock, references));
        } else if (executable instanceof CommentExecutableBlock commentBlock) {
            writer.field("COMMENT", commentBlock.getComment());
        } else if (executable instanceof AbstractExecutableBlock block) {
            writer.bullet("**Block** of type " + AiExportWriter.inlineCode(block.getBlockType()));
            writer.nested(() -> writeBlockContent(writer, "Contains", block, references));
        }
    }

    private static void writeAction(@NotNull AiExportWriter writer, @NotNull ActionInstance instance, @NotNull AiExportReferences references) {
        references.addAction(instance.action);
        String name = AiExportText.safe(() -> AiExportText.plain(instance.action.getDisplayName()), instance.action.getIdentifier());
        String title = "**Action:** " + name + " (" + AiExportWriter.inlineCode(instance.action.getIdentifier()) + ")";
        if (instance.action.isDeprecated()) title += " (deprecated)";
        String value = instance.value;
        if (!instance.action.hasValue()) {
            writer.bullet(title);
        } else if ((value != null) && (value.indexOf('\n') < 0) && (value.indexOf('\r') < 0)) {
            writer.bullet(title + ", value: " + AiExportWriter.inlineCode(value));
        } else {
            writer.bullet(title);
            writer.nested(() -> writer.field("Value", value));
        }
    }

    private static void writeBlockContent(@NotNull AiExportWriter writer, @NotNull String label, @NotNull AbstractExecutableBlock block, @NotNull AiExportReferences references) {
        writer.bullet("**" + label + ":**");
        writer.nested(() -> writeExecutables(writer, block.getExecutables(), references));
    }

    private static void writeAppendedBlocks(@NotNull AiExportWriter writer, @Nullable AbstractExecutableBlock appended, @NotNull AiExportReferences references) {
        AbstractExecutableBlock current = appended;
        while (current != null) {
            AbstractExecutableBlock block = current;
            if (block instanceof ElseIfExecutableBlock elseIfBlock) {
                writer.bullet("**ELSE-IF**");
                writer.nested(() -> {
                    RequirementAiExporter.writeContainer(writer, "Condition", elseIfBlock.condition, references, true);
                    writeBlockContent(writer, "Then", elseIfBlock, references);
                });
            } else if (block instanceof ElseExecutableBlock) {
                writer.bullet("**ELSE**");
                writer.nested(() -> writeBlockContent(writer, "Then", block, references));
            } else {
                writer.bullet("**Appended block** of type " + AiExportWriter.inlineCode(block.getBlockType()));
                writer.nested(() -> writeBlockContent(writer, "Contains", block, references));
            }
            current = block.getAppendedBlock();
        }
    }

}
