package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import org.jetbrains.annotations.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Collects the types an export uses, like the actions of an action script, and describes them at the end of the export,
 * so the AI knows what each of them does without every use repeating the description.
 */
public class AiExportReferences {

    private final Map<String, Action> actions = new LinkedHashMap<>();
    private final Map<String, Requirement> requirements = new LinkedHashMap<>();
    private final Map<String, Placeholder> placeholders = new LinkedHashMap<>();
    private final Map<String, AbstractListener> listeners = new LinkedHashMap<>();
    private final Map<String, ElementBuilder<?, ?>> elementTypes = new LinkedHashMap<>();
    private final Map<String, MenuBackgroundBuilder<?>> backgroundTypes = new LinkedHashMap<>();
    private final Map<String, AbstractDecorationOverlayBuilder<?>> decorationOverlayTypes = new LinkedHashMap<>();
    private boolean usesScriptBlocks = false;

    public void addAction(@NotNull Action action) {
        this.actions.putIfAbsent(action.getIdentifier(), action);
    }

    public void addRequirement(@NotNull Requirement requirement) {
        this.requirements.putIfAbsent(requirement.getIdentifier(), requirement);
    }

    public void addPlaceholder(@NotNull Placeholder placeholder) {
        this.placeholders.putIfAbsent(placeholder.getIdentifier(), placeholder);
    }

    public void addListener(@NotNull AbstractListener listener) {
        this.listeners.putIfAbsent(listener.getIdentifier(), listener);
    }

    public void addElementType(@NotNull ElementBuilder<?, ?> builder) {
        this.elementTypes.putIfAbsent(builder.getIdentifier(), builder);
    }

    public void addBackgroundType(@NotNull MenuBackgroundBuilder<?> builder) {
        this.backgroundTypes.putIfAbsent(builder.getIdentifier(), builder);
    }

    public void addDecorationOverlayType(@NotNull AbstractDecorationOverlayBuilder<?> builder) {
        this.decorationOverlayTypes.putIfAbsent(builder.getIdentifier(), builder);
    }

    /**
     * Marks that the export contains action scripts, so the reference explains the blocks scripts can have.
     */
    public void markScriptBlocksUsed() {
        this.usesScriptBlocks = true;
    }

    public boolean isEmpty() {
        return this.actions.isEmpty() && this.requirements.isEmpty() && this.placeholders.isEmpty() && this.listeners.isEmpty()
                && this.elementTypes.isEmpty() && this.backgroundTypes.isEmpty() && this.decorationOverlayTypes.isEmpty() && !this.usesScriptBlocks;
    }

    /**
     * Writes the reference section. Writes nothing if the export uses no types.
     */
    public void write(@NotNull AiExportWriter writer) {
        if (this.isEmpty()) return;
        writer.heading(2, "Reference: Types Used in This Export");
        writer.paragraph("What the types of things used above are and do, in FancyMenu's own words.");
        if (this.usesScriptBlocks) writeScriptBlocks(writer);
        writeSection(writer, "Element Types", this.elementTypes, AiExportTypeWriter::writeElementType);
        writeSection(writer, "Menu Background Types", this.backgroundTypes, AiExportTypeWriter::writeBackgroundType);
        writeSection(writer, "Decoration Overlay Types", this.decorationOverlayTypes, AiExportTypeWriter::writeDecorationOverlayType);
        writeSection(writer, "Listener Types", this.listeners, AiExportTypeWriter::writeListener);
        writeSection(writer, "Actions", this.actions, AiExportTypeWriter::writeAction);
        writeSection(writer, "Requirements", this.requirements, AiExportTypeWriter::writeRequirement);
        writeSection(writer, "Placeholders", this.placeholders, AiExportTypeWriter::writePlaceholder);
    }

    private static <T> void writeSection(@NotNull AiExportWriter writer, @NotNull String title, @NotNull Map<String, T> types, @NotNull BiConsumer<AiExportWriter, T> typeWriter) {
        if (types.isEmpty()) return;
        writer.heading(3, title);
        types.values().forEach(type -> typeWriter.accept(writer, type));
    }

    private static void writeScriptBlocks(@NotNull AiExportWriter writer) {
        writer.heading(3, "Action Script Blocks");
        writer.bullet("**IF / ELSE-IF / ELSE:** Runs the actions in a block only when its requirements are met. ELSE-IF and ELSE blocks only get checked when the blocks before them in the same chain did not run.");
        writer.bullet("**WHILE:** Runs the actions in it again and again for as long as its requirements are met. FancyMenu stops loops that run for too long, so they can't freeze the game.");
        writer.bullet("**FOR EACH ENTRY:** Runs the actions in it once for every entry of a list variable, from the first entry to the last. Inside the loop, `$$list_entry` is the current entry and `$$list_index` its index (starting at 0); in nested loops they belong to the innermost loop. It reads the list once when it starts, so editing the list inside the loop doesn't change which entries it goes through. A missing variable or a text variable makes it run zero times. Like WHILE, it stops when it runs for too long.");
        writer.bullet("**DELAY GATE:** " + AiExportText.localized("fancymenu.actions.blocks.add.delay.desc"));
        writer.bullet("**EXECUTE LATER:** " + AiExportText.localized("fancymenu.actions.blocks.add.execute_later.desc"));
        writer.bullet("**FOLDER:** Only groups actions to keep the script tidy. Runs everything in it as if it was not in a folder.");
        writer.bullet("**COMMENT:** A note of the user. Does nothing.");
        writer.bullet("**$$name:** Value placeholders. Listeners and some elements give their action scripts values like `$$key`, which get replaced in action values and requirement values when the script runs. In normal texts, `$$name` gets replaced with the value of the FancyMenu variable with that name, which is a JSON array of strings for list variables.");
    }

}
