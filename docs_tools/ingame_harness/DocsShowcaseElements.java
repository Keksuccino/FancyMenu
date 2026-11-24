package de.keksuccino.fancymenu.util.window.preview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.text.v2.TextElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;
import java.util.Map;

/**
 * Dev-only docs tool (see docs_tools/README.md). Builds the elements of a docs scene or editor showcase from JSON, with the defaults of new elements.<br>
 * Keys of an element: {@code type} (element type identifier, default {@code text_v2}), {@code x}, {@code y}, {@code w}, {@code h}
 * (GUI pixels, anchored top-left), {@code id} (instance identifier), {@code parent} (the {@code id} of the Scrollable Area it is in,
 * then {@code x}/{@code y} are relative to the area's content), {@code props} (saved properties of the element, as strings, applied last).<br>
 * Text elements also take {@code content}, {@code look} ({@code pixel}, {@code classic}, {@code neutral}), {@code background} and {@code scale}.
 */
final class DocsShowcaseElements {

    private DocsShowcaseElements() {
    }

    static void addTo(@NotNull Layout layout, @NotNull Iterable<JsonElement> elements) {
        for (JsonElement entry : elements) {
            layout.serializedElements.add(build(entry.getAsJsonObject()));
        }
    }

    @NotNull
    static SerializedElement build(@NotNull JsonObject data) {
        String type = data.has("type") ? data.get("type").getAsString() : "text_v2";
        ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(type);
        if (builder == null) throw new IllegalStateException("No element builder " + type);
        AbstractElement element = builder.buildDefaultInstance();
        if (element instanceof TextElement text) {
            if (data.has("content")) text.setSource(TextElement.SourceMode.DIRECT, data.get("content").getAsString());
            text.markdownSettings.applyLook(MarkdownLook.valueOf(data.has("look") ? data.get("look").getAsString().toUpperCase(Locale.ROOT) : "PIXEL"));
            if (data.has("background")) text.background.enabled.set(data.get("background").getAsBoolean());
            if (data.has("scale")) text.markdownSettings.textScale.set(data.get("scale").getAsFloat());
        }
        element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        element.posOffsetX = data.get("x").getAsInt();
        element.posOffsetY = data.get("y").getAsInt();
        element.baseWidth = data.get("w").getAsInt();
        element.baseHeight = data.get("h").getAsInt();
        if (data.has("id")) element.setInstanceIdentifier(data.get("id").getAsString());
        if (data.has("parent")) element.setParentContainerIdentifier(data.get("parent").getAsString());
        SerializedElement serialized = builder.serializeElementInternal(element);
        if (data.has("props")) {
            for (Map.Entry<String, JsonElement> property : data.getAsJsonObject("props").entrySet()) {
                serialized.putProperty(property.getKey(), property.getValue().getAsString());
            }
        }
        return serialized;
    }

}
