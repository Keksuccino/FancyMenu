package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.customization.placeholder.ShortReferenceResolver;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports texts of the text editor for AI chats, with the placeholders and variables they use and what they show right now.
 */
public final class TextAiExporter {

    private static final int MAX_PLACEHOLDER_DEPTH = 8;

    private TextAiExporter() {
    }

    @NotNull
    public static AiExportDocument export(@NotNull String text) {
        return AiExports.create("Text", "text", writer -> {
            AiExportReferences references = new AiExportReferences();
            writer.heading(2, "Text");
            writer.paragraph("A text from FancyMenu's text editor. Placeholders are JSON-like snippets like `{\"placeholder\":\"playername\"}` that get replaced with a value, "
                    + "and `$$name` gets replaced with the value of the FancyMenu variable with that name (list variables give their entries as JSON array of strings, like `[\"apple\",\"banana\"]`). "
                    + "Placeholders can be inside the values of other placeholders.");
            writer.heading(3, "Raw Text");
            writer.codeBlock(null, text);
            writer.heading(3, "What It Shows Right Now");
            writer.paragraph("The text with all placeholders replaced, at the moment of the export. Formatting codes like `&a` are still in it.");
            writer.codeBlock(null, AiExportText.safe(() -> PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(text), "(failed to replace the placeholders)"));
            writer.heading(3, "Placeholders in the Text");
            int found = writePlaceholders(writer, text, references, 0);
            if (found == 0) writer.paragraph("The text has no placeholders.");
            writeVariableReferences(writer, text);
            references.write(writer);
        });
    }

    /**
     * @return How many placeholders got found.
     */
    private static int writePlaceholders(@NotNull AiExportWriter writer, @NotNull String text, @NotNull AiExportReferences references, int depth) {
        if (depth > MAX_PLACEHOLDER_DEPTH) return 0;
        List<PlaceholderParser.ParsedPlaceholder> placeholders = PlaceholderParser.findPlaceholders(text, new HashMap<>());
        for (PlaceholderParser.ParsedPlaceholder parsed : placeholders) {
            String identifier = parsed.getIdentifier();
            Placeholder placeholder = (identifier != null) ? PlaceholderRegistry.getPlaceholder(identifier) : null;
            if (placeholder == null) {
                writer.field("Unknown placeholder", parsed.placeholderString, (identifier != null) ? ("no placeholder with the identifier " + AiExportWriter.inlineCode(identifier) + " exists, maybe a typo or a missing add-on") : "its identifier could not be read, so it is probably broken");
                continue;
            }
            references.addPlaceholder(placeholder);
            String name = AiExportText.safe(() -> AiExportText.plain(placeholder.getDisplayName()), identifier);
            writer.bullet("**" + name + "** (" + AiExportWriter.inlineCode(identifier) + ")");
            writer.nested(() -> {
                Map<String, String> values = parsed.getValues();
                if (values == null) {
                    writer.fieldText("Values", "could not be read, so the placeholder is probably broken");
                } else if (!values.isEmpty()) {
                    for (Map.Entry<String, String> value : new LinkedHashMap<>(values).entrySet()) {
                        writer.field("Value " + value.getKey(), value.getValue());
                        writer.nested(() -> writePlaceholders(writer, value.getValue(), references, depth + 1));
                    }
                }
            });
        }
        return placeholders.size();
    }

    private static void writeVariableReferences(@NotNull AiExportWriter writer, @NotNull String text) {
        List<UserVariableSnapshot> variables = VariableHandler.getVariableSnapshots();
        Map<String, UserVariableSnapshot> found = new LinkedHashMap<>();
        int prefixIndex = text.indexOf(ShortReferenceResolver.PREFIX);
        while (prefixIndex >= 0) {
            int nameStart = prefixIndex + ShortReferenceResolver.PREFIX.length();
            UserVariableSnapshot variable = ShortReferenceResolver.findLongestName(text, nameStart, variables, UserVariableSnapshot::name);
            if (variable != null) {
                found.put(variable.name(), variable);
                prefixIndex = text.indexOf(ShortReferenceResolver.PREFIX, nameStart + variable.name().length());
            } else {
                int nameEnd = nameStart;
                while ((nameEnd < text.length()) && isNameChar(text.charAt(nameEnd))) nameEnd++;
                if (nameEnd > nameStart) found.putIfAbsent(text.substring(nameStart, nameEnd), null);
                prefixIndex = text.indexOf(ShortReferenceResolver.PREFIX, Math.max(nameEnd, nameStart));
            }
        }
        if (found.isEmpty()) return;
        writer.heading(3, "$$ References in the Text");
        found.forEach((name, variable) -> {
            if ((variable != null) && variable.isList()) {
                int size = variable.listEntries().size();
                writer.field("$$" + name, variable.value(), "a FancyMenu list variable with " + size + ((size == 1) ? " entry" : " entries") + " right now, which `$$` gives as JSON array like this");
            } else if (variable != null) {
                writer.field("$$" + name, variable.value(), "a FancyMenu variable with this value right now");
            } else {
                writer.fieldText("$$" + name, "no FancyMenu variable starts with this name, so it stays as it is, unless it is a value placeholder of an action script or listener");
            }
        });
    }

    private static boolean isNameChar(char c) {
        return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')) || (c == '_') || (c == '-');
    }

}
