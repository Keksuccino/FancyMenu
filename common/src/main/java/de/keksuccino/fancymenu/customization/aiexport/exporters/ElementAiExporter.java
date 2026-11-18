package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.customization.aiexport.AiExportDefaults;
import de.keksuccino.fancymenu.customization.aiexport.AiExportReferences;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Describes serialized elements in AI exports.
 */
public final class ElementAiExporter {

    static final String ELEMENTS_INTRO = "Elements are the things layouts add to screens or change on screens, like buttons, texts and images. "
            + "Elements get positioned relative to their anchor point (a corner, edge or the center of the screen, another element, or the original position of a vanilla widget), with `x` and `y` as offset. "
            + "Their properties only list what differs from the defaults of their type.";

    private static final String KEY_TYPE = "element_type";
    private static final String KEY_IDENTIFIER = "instance_identifier";
    private static final String KEY_CUSTOM_NAME = "custom_element_layer_name";
    private static final String KEY_ANCHOR_POINT = "anchor_point";
    private static final String KEY_ANCHOR_ELEMENT = "anchor_point_element";
    private static final Set<String> SHOWN_ELSEWHERE = Set.of(KEY_TYPE, KEY_IDENTIFIER, KEY_CUSTOM_NAME, KEY_ANCHOR_POINT, KEY_ANCHOR_ELEMENT, ElementBuilder.PARENT_CONTAINER_KEY, "layer_hidden_in_editor", "in_editor_color");
    private static final Set<String> SHOWN_ELSEWHERE_VANILLA_ANCHOR = Set.of(KEY_TYPE, KEY_IDENTIFIER, KEY_CUSTOM_NAME, KEY_ANCHOR_POINT, KEY_ANCHOR_ELEMENT, ElementBuilder.PARENT_CONTAINER_KEY, "layer_hidden_in_editor", "in_editor_color", "x", "y", "width", "height");

    private ElementAiExporter() {
    }

    /**
     * Writes each element as own section.
     *
     * @param names         Names of all elements of the layout, to name anchor and container elements.
     * @param runtimeLookup Finds the live element of an identifier, if there is one.
     * @param skipUntouched If elements without any changes get skipped, like the vanilla widgets of a screen nobody customized.
     * @return How many elements got skipped.
     */
    public static int writeElements(@NotNull AiExportWriter writer, @NotNull List<? extends PropertyContainer> elements, @NotNull Map<String, String> names, @NotNull AiExportDefaults defaults, @NotNull AiExportReferences references, @NotNull Function<String, Object> runtimeLookup, boolean skipUntouched, @NotNull String headingPrefix) {
        int skipped = 0;
        int index = 1;
        for (PropertyContainer element : elements) {
            ElementBuilder<?, ?> builder = getBuilder(element);
            SerializedObjectAiExporter.Analysis analysis = SerializedObjectAiExporter.analyze(element, (builder != null) ? defaults.forElement(builder) : AiExportDefaults.TypeDefaults.UNKNOWN, isAnchoredToVanilla(element) ? SHOWN_ELSEWHERE_VANILLA_ANCHOR : SHOWN_ELSEWHERE);
            if (skipUntouched && analysis.isUntouched() && !isAnchoredToOtherElement(element)) {
                skipped++;
                continue;
            }
            String identifier = element.getValue(KEY_IDENTIFIER);
            writer.heading(3, headingPrefix + " " + index + ": " + describeName(element, builder));
            writeElement(writer, element, builder, analysis, names, references, (identifier != null) ? runtimeLookup.apply(identifier) : null);
            index++;
        }
        return skipped;
    }

    /**
     * Writes the details of one element as bullets at the current indentation.
     */
    public static void writeElement(@NotNull AiExportWriter writer, @NotNull PropertyContainer element, @Nullable ElementBuilder<?, ?> builder, @NotNull SerializedObjectAiExporter.Analysis analysis, @NotNull Map<String, String> names, @NotNull AiExportReferences references, @Nullable Object runtimeObject) {
        String type = element.getValue(KEY_TYPE);
        if (builder != null) {
            references.addElementType(builder);
            writer.fieldText("Type", typeName(builder) + " (" + AiExportWriter.inlineCode(builder.getIdentifier()) + ")");
        } else {
            writer.field("Type", type, "this type is not registered, so the add-on that adds it is probably missing");
        }
        writer.field("Identifier", element.getValue(KEY_IDENTIFIER));
        String customName = element.getValue(KEY_CUSTOM_NAME);
        if (customName != null) writer.field("Name set by the user", customName);
        writeAnchor(writer, element, names);
        String parentContainer = element.getValue(ElementBuilder.PARENT_CONTAINER_KEY);
        if (parentContainer != null) writer.fieldText("Inside the container element", describeReference(parentContainer, names));
        SerializedObjectAiExporter.write(writer, analysis, references, runtimeObject);
    }

    /**
     * @return The names of all elements, for {@link #writeElements}, like {@code "Play" (Button, `id`)}.
     */
    @NotNull
    public static Map<String, String> buildNames(@NotNull List<? extends PropertyContainer> elements) {
        Map<String, String> names = new HashMap<>();
        for (PropertyContainer element : elements) {
            String identifier = element.getValue(KEY_IDENTIFIER);
            if (identifier != null) names.put(identifier, describeName(element, getBuilder(element)));
        }
        return names;
    }

    @Nullable
    public static ElementBuilder<?, ?> getBuilder(@NotNull PropertyContainer element) {
        String type = element.getValue(KEY_TYPE);
        if (type == null) return null;
        // Vanilla widget customizations have their own builder, which is not part of the registry because users can't add them
        if (VanillaWidgetElementBuilder.INSTANCE.getIdentifier().equals(type)) return VanillaWidgetElementBuilder.INSTANCE;
        return ElementRegistry.getBuilder(type);
    }

    private static void writeAnchor(@NotNull AiExportWriter writer, @NotNull PropertyContainer element, @NotNull Map<String, String> names) {
        String anchor = element.getValue(KEY_ANCHOR_POINT);
        if (anchor == null) return;
        if (ElementAnchorPoints.ELEMENT.getName().equals(anchor)) {
            String anchorElement = element.getValue(KEY_ANCHOR_ELEMENT);
            writer.field("Anchor point", anchor, "anchored to the element " + ((anchorElement != null) ? describeReference(anchorElement, names) : "(none set)"));
        } else if (ElementAnchorPoints.VANILLA.getName().equals(anchor)) {
            writer.field("Anchor point", anchor, "keeps the original position and size of the vanilla widget");
        } else {
            writer.field("Anchor point", anchor);
        }
    }

    @NotNull
    private static String describeReference(@NotNull String identifier, @NotNull Map<String, String> names) {
        String name = names.get(identifier);
        return (name != null) ? name : (AiExportWriter.inlineCode(identifier) + " (not in this layout)");
    }

    @NotNull
    private static String describeName(@NotNull PropertyContainer element, @Nullable ElementBuilder<?, ?> builder) {
        String identifier = element.getValue(KEY_IDENTIFIER);
        String customName = element.getValue(KEY_CUSTOM_NAME);
        String type = (builder != null) ? typeName(builder) : String.valueOf(element.getValue(KEY_TYPE));
        String idPart = (identifier != null) ? AiExportWriter.inlineCode(identifier) : "no identifier";
        if (customName != null) return "\"" + customName + "\" (" + type + ", " + idPart + ")";
        return type + " " + idPart;
    }

    @NotNull
    private static String typeName(@NotNull ElementBuilder<?, ?> builder) {
        return AiExportText.safe(() -> AiExportText.plain(builder.getDisplayName(null)), builder.getIdentifier());
    }

    private static boolean isAnchoredToVanilla(@NotNull PropertyContainer element) {
        return ElementAnchorPoints.VANILLA.getName().equals(element.getValue(KEY_ANCHOR_POINT));
    }

    private static boolean isAnchoredToOtherElement(@NotNull PropertyContainer element) {
        return ElementAnchorPoints.ELEMENT.getName().equals(element.getValue(KEY_ANCHOR_POINT));
    }

}
