package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.LegacyMarkdownSettings;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextElementBuilder extends ElementBuilder<TextElement, TextEditorElement> {

    public TextElementBuilder() {
        super("text_v2");
    }

    @Override
    public @NotNull TextElement buildDefaultInstance() {
        TextElement i = new TextElement(this);
        i.baseWidth = 200;
        i.baseHeight = 40;
        i.setSource(TextElement.SourceMode.DIRECT, I18n.get("fancymenu.elements.text.placeholder"));
        i.markdownSettings.applyDefaultsOfNewElements();
        return i;
    }

    @Override
    public TextElement deserializeElement(@NotNull SerializedElement serialized) {

        LegacyMarkdownSettings.upgradeTextElement(serialized);

        //Don't use buildDefaultInstance() here, because updateContent() runs asynchronously and could override the deserialized content with the default one
        TextElement element = new TextElement(this);
        element.baseWidth = 200;
        element.baseHeight = 40;

        element.interactable = deserializeBoolean(element.interactable, serialized.getValue("interactable"));

        element.source = serialized.getValue("source");
        if (element.source != null) element.source = element.source.replace("%n%", "\n");

        String sourceModeString = serialized.getValue("source_mode");
        if (sourceModeString != null) {
            if (sourceModeString.equals("local") || sourceModeString.equals("web")) {
                element.sourceMode = TextElement.SourceMode.RESOURCE;
            } else {
                TextElement.SourceMode s = TextElement.SourceMode.getByName(sourceModeString);
                if (s != null) {
                    element.sourceMode = s;
                }
            }
        }

        String caseModeString = serialized.getValue("case_mode");
        if (caseModeString != null) {
            if (caseModeString.equals("lower")) {
                element.markdownRenderer.setTextCase(MarkdownRenderer.TextCase.ALL_LOWER);
            }
            if (caseModeString.equals("upper")) {
                element.markdownRenderer.setTextCase(MarkdownRenderer.TextCase.ALL_UPPER);
            }
        }

        element.verticalScrollGrabberTextureNormal = deserializeImageResourceSupplier(serialized.getValue("grabber_texture_normal"));
        element.verticalScrollGrabberTextureHover = deserializeImageResourceSupplier(serialized.getValue("grabber_texture_hover"));
        element.horizontalScrollGrabberTextureNormal = deserializeImageResourceSupplier(serialized.getValue("horizontal_grabber_texture_normal"));
        element.horizontalScrollGrabberTextureHover = deserializeImageResourceSupplier(serialized.getValue("horizontal_grabber_texture_hover"));

        String enableScrollingString = serialized.getValue("enable_scrolling");
        if ((enableScrollingString != null) && enableScrollingString.equals("false")) {
            element.enableScrolling = false;
        }

        String autoLineWrapping = serialized.getValue("auto_line_wrapping");
        if (autoLineWrapping != null) {
            if (autoLineWrapping.equals("true")) element.markdownRenderer.setAutoLineBreakingEnabled(true);
            if (autoLineWrapping.equals("false")) element.markdownRenderer.setAutoLineBreakingEnabled(false);
        }

        String removeHtmlBreaks = serialized.getValue("remove_html_breaks");
        if (removeHtmlBreaks != null) {
            if (removeHtmlBreaks.equals("true")) element.markdownRenderer.setRemoveHtmlBreaks(true);
            if (removeHtmlBreaks.equals("false")) element.markdownRenderer.setRemoveHtmlBreaks(false);
        }

        element.markdownRenderer.refreshRenderer();

        element.setSource(element.sourceMode, element.source);

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull TextElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("interactable", "" + element.interactable);

        if (element.source != null) {
            serializeTo.putProperty("source", element.source.replace("\n", "%n%"));
        }
        serializeTo.putProperty("source_mode", element.sourceMode.name);
        if (element.markdownRenderer.getTextCase() == MarkdownRenderer.TextCase.ALL_LOWER) {
            serializeTo.putProperty("case_mode", "lower");
        } else if (element.markdownRenderer.getTextCase() == MarkdownRenderer.TextCase.ALL_UPPER) {
            serializeTo.putProperty("case_mode", "upper");
        }
        if (element.verticalScrollGrabberTextureNormal != null) {
            serializeTo.putProperty("grabber_texture_normal", element.verticalScrollGrabberTextureNormal.getSourceWithPrefix());
        }
        if (element.verticalScrollGrabberTextureHover != null) {
            serializeTo.putProperty("grabber_texture_hover", element.verticalScrollGrabberTextureHover.getSourceWithPrefix());
        }
        if (element.horizontalScrollGrabberTextureNormal != null) {
            serializeTo.putProperty("horizontal_grabber_texture_normal", element.horizontalScrollGrabberTextureNormal.getSourceWithPrefix());
        }
        if (element.horizontalScrollGrabberTextureHover != null) {
            serializeTo.putProperty("horizontal_grabber_texture_hover", element.horizontalScrollGrabberTextureHover.getSourceWithPrefix());
        }
        serializeTo.putProperty("enable_scrolling", "" + element.enableScrolling);
        serializeTo.putProperty("auto_line_wrapping", "" + element.markdownRenderer.isAutoLineBreakingEnabled());
        serializeTo.putProperty("remove_html_breaks", "" + element.markdownRenderer.isRemoveHtmlBreaks());
        return serializeTo;
        
    }

    @Override
    public @NotNull TextEditorElement wrapIntoEditorElement(@NotNull TextElement element, @NotNull LayoutEditorScreen editor) {
        return new TextEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.text");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.desc");
    }

}
