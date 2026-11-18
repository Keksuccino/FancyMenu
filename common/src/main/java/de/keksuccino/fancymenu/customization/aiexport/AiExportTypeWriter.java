package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Describes the types of things FancyMenu has (actions, requirements, placeholders, ...) in AI exports, with their names, descriptions and value formats.
 */
public final class AiExportTypeWriter {

    private static final String FANCYMENU_PACKAGE = "de.keksuccino.fancymenu.";

    private AiExportTypeWriter() {
    }

    public static void writeAction(@NotNull AiExportWriter writer, @NotNull Action action) {
        writer.bullet(title(action, AiExportText.safe(() -> AiExportText.plain(action.getDisplayName()), action.getIdentifier()), action.getIdentifier(), action.isDeprecated()));
        writer.nested(() -> {
            writeDescription(writer, AiExportText.safe(() -> AiExportText.plain(action.getDescription()), ""));
            if (action.hasValue()) {
                writeValueFormat(writer, AiExportText.safe(() -> AiExportText.plain(action.getValueDisplayName()), ""), AiExportText.safe(action::getValuePreset, ""));
            } else {
                writer.fieldText("Value", "none, this action has no value");
            }
        });
    }

    public static void writeRequirement(@NotNull AiExportWriter writer, @NotNull Requirement requirement) {
        writer.bullet(title(requirement, AiExportText.safe(() -> AiExportText.plain(requirement.getDisplayName()), requirement.getIdentifier()), requirement.getIdentifier(), false));
        writer.nested(() -> {
            writeDescription(writer, AiExportText.safe(() -> AiExportText.plain(requirement.getDescription()), ""));
            String category = AiExportText.safe(requirement::getCategory, "");
            if (!category.isBlank()) writer.fieldText("Category", AiExportText.plain(category));
            if (requirement.hasValue()) {
                writeValueFormat(writer, AiExportText.safe(() -> AiExportText.plain(requirement.getValueDisplayName()), ""), AiExportText.safe(requirement::getValuePreset, ""));
            } else {
                writer.fieldText("Value", "none, this requirement has no value");
            }
        });
    }

    public static void writePlaceholder(@NotNull AiExportWriter writer, @NotNull Placeholder placeholder) {
        writer.bullet(title(placeholder, AiExportText.safe(() -> AiExportText.plain(placeholder.getDisplayName()), placeholder.getIdentifier()), placeholder.getIdentifier(), false));
        writer.nested(() -> {
            writeDescription(writer, AiExportText.safe(() -> AiExportText.plainStringLines(placeholder.getDescription()), ""));
            String category = AiExportText.safe(placeholder::getCategory, "");
            if (!category.isBlank()) writer.fieldText("Category", AiExportText.plain(category));
            List<String> alternativeIdentifiers = placeholder.getAlternativeIdentifiers();
            if ((alternativeIdentifiers != null) && !alternativeIdentifiers.isEmpty()) writer.field("Also works with the identifiers", String.join(", ", alternativeIdentifiers));
            List<String> valueNames = placeholder.getValueNames();
            writer.fieldText("Values", ((valueNames == null) || valueNames.isEmpty()) ? "none" : AiExportWriter.inlineCode(String.join(", ", valueNames)));
            writer.field("Example", AiExportText.safe(() -> placeholder.getDefaultPlaceholderString().toString(), ""));
        });
    }

    public static void writeListener(@NotNull AiExportWriter writer, @NotNull AbstractListener listener) {
        writer.bullet(title(listener, AiExportText.safe(() -> AiExportText.plain(listener.getDisplayName()), listener.getIdentifier()), listener.getIdentifier(), false));
        writer.nested(() -> {
            writeDescription(writer, AiExportText.safe(() -> AiExportText.plainLines(listener.getDescription()), ""));
            String variables = AiExportText.safe(() -> String.join(", ", listener.getCustomVariables().stream().map(variable -> "$$" + variable.name()).toList()), "");
            writer.fieldText("Variables it gives its action script", variables.isEmpty() ? "none" : AiExportWriter.inlineCode(variables));
        });
    }

    public static void writeElementType(@NotNull AiExportWriter writer, @NotNull ElementBuilder<?, ?> builder) {
        writer.bullet(title(builder, AiExportText.safe(() -> AiExportText.plain(builder.getDisplayName(null)), builder.getIdentifier()), builder.getIdentifier(), builder.isDeprecated()));
        writer.nested(() -> writeDescription(writer, AiExportText.safe(() -> AiExportText.plainLines(builder.getDescription(null)), "")));
    }

    public static void writeBackgroundType(@NotNull AiExportWriter writer, @NotNull MenuBackgroundBuilder<?> builder) {
        writer.bullet(title(builder, AiExportText.safe(() -> AiExportText.plain(builder.getDisplayName()), builder.getIdentifier()), builder.getIdentifier(), builder.isDeprecated()));
        writer.nested(() -> writeDescription(writer, AiExportText.safe(() -> AiExportText.plain(builder.getDescription()), "")));
    }

    public static void writeDecorationOverlayType(@NotNull AiExportWriter writer, @NotNull AbstractDecorationOverlayBuilder<?> builder) {
        writer.bullet(title(builder, AiExportText.safe(() -> AiExportText.plain(builder.getDisplayName()), builder.getIdentifier()), builder.getIdentifier(), false));
        writer.nested(() -> writeDescription(writer, AiExportText.safe(() -> AiExportText.plain(builder.getDescription()), "")));
    }

    @NotNull
    private static String title(@NotNull Object type, @NotNull String displayName, @NotNull String identifier, boolean deprecated) {
        String title = "**" + displayName + "** (" + AiExportWriter.inlineCode(identifier) + ")";
        if (deprecated) title += " (deprecated, users should not add it anymore)";
        // Types of add-ons live outside of FancyMenu's packages, which helps the AI tell them apart from FancyMenu's own types
        String className = type.getClass().getName();
        if (!className.startsWith(FANCYMENU_PACKAGE)) title += " (added by an add-on, class " + AiExportWriter.inlineCode(className) + ")";
        return title;
    }

    private static void writeDescription(@NotNull AiExportWriter writer, @Nullable String description) {
        if ((description == null) || description.isBlank()) return;
        writer.fieldText("Description", description.replace("\n\n", "\n"));
    }

    private static void writeValueFormat(@NotNull AiExportWriter writer, @NotNull String valueName, @Nullable String example) {
        writer.fieldText("Value", valueName.isBlank() ? "yes" : valueName.replace("\n\n", "\n"));
        if ((example != null) && !example.isBlank()) writer.field("Example value", example);
    }

}
