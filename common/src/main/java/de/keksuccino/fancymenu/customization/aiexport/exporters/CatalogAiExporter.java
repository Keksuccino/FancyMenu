package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportTypeWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayRegistry;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Exports every type of thing FancyMenu has in this game, including the ones of add-ons, for AI chats.
 */
public final class CatalogAiExporter {

    private CatalogAiExporter() {
    }

    @NotNull
    public static AiExportDocument export() {
        return AiExports.create("Everything FancyMenu Can Do", "catalog", writer -> {
            writer.paragraph("This lists every element, background, decoration overlay, listener, action, requirement and placeholder FancyMenu has in the game of the user, "
                    + "with their identifiers, descriptions and value formats. It includes the ones add-ons add, and leaves out the ones that don't exist in this FancyMenu version.");
            writeSection(writer, "Element Types", ElementRegistry.getBuilders(), AiExportTypeWriter::writeElementType);
            writeSection(writer, "Menu Background Types", MenuBackgroundRegistry.getBuilders(), AiExportTypeWriter::writeBackgroundType);
            writeSection(writer, "Decoration Overlay Types", DecorationOverlayRegistry.getAll(), AiExportTypeWriter::writeDecorationOverlayType);
            writeSection(writer, "Listener Types", ListenerRegistry.getListeners(), AiExportTypeWriter::writeListener);
            writeSection(writer, "Actions", ActionRegistry.getActions().stream().sorted(Comparator.comparing(Action::getIdentifier)).toList(), AiExportTypeWriter::writeAction);
            writeSection(writer, "Requirements", RequirementRegistry.getRequirements().stream().sorted(Comparator.comparing((Requirement requirement) -> AiExportText.safe(requirement::getCategory, "")).thenComparing(Requirement::getIdentifier)).toList(), AiExportTypeWriter::writeRequirement);
            writeSection(writer, "Placeholders", PlaceholderRegistry.getPlaceholders().stream().sorted(Comparator.comparing((Placeholder placeholder) -> AiExportText.safe(placeholder::getCategory, "")).thenComparing(Placeholder::getIdentifier)).toList(), AiExportTypeWriter::writePlaceholder);
        });
    }

    private static <T> void writeSection(@NotNull AiExportWriter writer, @NotNull String title, @NotNull List<T> types, @NotNull BiConsumer<AiExportWriter, T> typeWriter) {
        writer.heading(2, title + " (" + types.size() + ")");
        types.forEach(type -> typeWriter.accept(writer, type));
    }

}
