package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Exports listeners for AI chats.
 */
public final class ListenersAiExporter {

    private static final String LISTENERS_INTRO = "Listeners run an action script every time something happens in the game, like a key press or opening a screen. "
            + "Listeners give their script values about what happened as `$$` value placeholders, like `$$key`, which only have the right values while the script runs. "
            + ActionScriptAiExporter.SCRIPT_INTRO;

    private ListenersAiExporter() {
    }

    /**
     * Exports all saved listeners.
     */
    @NotNull
    public static AiExportDocument exportAll() {
        return exportAll(ListenerHandler.getInstances());
    }

    /**
     * @param listeners The listeners to export, like the ones of the listener manager, which can have unsaved changes.
     */
    @NotNull
    public static AiExportDocument exportAll(@NotNull List<ListenerInstance> listeners) {
        return AiExports.create("All Listeners", "listeners", writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Listeners (" + listeners.size() + ")");
            writer.paragraph(LISTENERS_INTRO);
            if (listeners.isEmpty()) writer.paragraph("There are no listeners.");
            int index = 1;
            for (ListenerInstance listener : listeners) {
                writeListener(writer, listener, index, references);
                index++;
            }
            references.write(writer);
        });
    }

    @NotNull
    public static AiExportDocument exportSingle(@NotNull ListenerInstance listener) {
        String name = (listener.getDisplayName() != null) ? listener.getDisplayName() : listener.parent.getIdentifier();
        return AiExports.create("Listener " + name, "listener_" + name, writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Listener");
            writer.paragraph(LISTENERS_INTRO);
            writeListener(writer, listener, 0, references);
            references.write(writer);
        });
    }

    private static void writeListener(@NotNull AiExportWriter writer, @NotNull ListenerInstance listener, int index, @NotNull AiExportReferences references) {
        references.addListener(listener.parent);
        String typeName = AiExportText.safe(() -> AiExportText.plain(listener.parent.getDisplayName()), listener.parent.getIdentifier());
        String name = (listener.getDisplayName() != null) ? listener.getDisplayName() : typeName;
        writer.heading(3, ((index > 0) ? ("Listener " + index + ": ") : "") + name);
        writer.fieldText("Type", typeName + " (" + AiExportWriter.inlineCode(listener.parent.getIdentifier()) + ")");
        writer.field("Name set by the user", listener.getDisplayName());
        writer.field("Identifier", listener.instanceIdentifier);
        writer.bullet("**Action script:**");
        writer.nested(() -> ActionScriptAiExporter.writeScript(writer, listener.getActionScript(), references));
    }

}
