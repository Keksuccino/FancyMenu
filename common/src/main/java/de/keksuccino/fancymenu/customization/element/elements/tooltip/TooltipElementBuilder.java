package de.keksuccino.fancymenu.customization.element.elements.tooltip;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.LegacyMarkdownSettings;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TooltipElementBuilder extends ElementBuilder<TooltipElement, TooltipEditorElement> {

    public TooltipElementBuilder() {
        super("tooltip");
    }

    @Override
    public @NotNull TooltipElement buildDefaultInstance() {
        TooltipElement element = new TooltipElement(this);
        element.baseWidth = 200;
        element.baseHeight = 40;
        element.setSource(TooltipElement.SourceMode.DIRECT, "------------------");
        element.markdownSettings.applyDefaultsOfNewElements();
        return element;
    }

    @Override
    public TooltipElement deserializeElement(@NotNull SerializedElement serialized) {

        LegacyMarkdownSettings.upgradeTooltipElement(serialized);

        // Don't use buildDefaultInstance() here, because updateContent() runs asynchronously and could override the deserialized content with the default one
        TooltipElement element = new TooltipElement(this);
        element.baseWidth = 200;
        element.baseHeight = 40;

        element.interactable = deserializeBoolean(element.interactable, serialized.getValue("interactable"));
        element.mouseFollowing = deserializeBoolean(element.mouseFollowing, serialized.getValue("mouse_following"));

        element.source = serialized.getValue("source");
        if (element.source != null) element.source = element.source.replace("%n%", "\n");

        String sourceModeString = serialized.getValue("source_mode");
        if (sourceModeString != null) {
            TooltipElement.SourceMode s = TooltipElement.SourceMode.getByName(sourceModeString);
            if (s != null) {
                element.sourceMode = s;
            }
        }

        element.backgroundTexture = deserializeImageResourceSupplier(serialized.getValue("background_texture"));
        
        String caseModeString = serialized.getValue("case_mode");
        if (caseModeString != null) {
            if (caseModeString.equals("lower")) {
                element.markdownRenderer.setTextCase(MarkdownRenderer.TextCase.ALL_LOWER);
            }
            if (caseModeString.equals("upper")) {
                element.markdownRenderer.setTextCase(MarkdownRenderer.TextCase.ALL_UPPER);
            }
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
    protected SerializedElement serializeElement(@NotNull TooltipElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("interactable", "" + element.interactable);
        serializeTo.putProperty("mouse_following", "" + element.mouseFollowing);

        if (element.source != null) {
            serializeTo.putProperty("source", element.source.replace("\n", "%n%"));
        }
        serializeTo.putProperty("source_mode", element.sourceMode.name);

        if (element.backgroundTexture != null) {
            serializeTo.putProperty("background_texture", element.backgroundTexture.getSourceWithPrefix());
        }
        
        if (element.markdownRenderer.getTextCase() == MarkdownRenderer.TextCase.ALL_LOWER) {
            serializeTo.putProperty("case_mode", "lower");
        } else if (element.markdownRenderer.getTextCase() == MarkdownRenderer.TextCase.ALL_UPPER) {
            serializeTo.putProperty("case_mode", "upper");
        }
        serializeTo.putProperty("auto_line_wrapping", "" + element.markdownRenderer.isAutoLineBreakingEnabled());
        serializeTo.putProperty("remove_html_breaks", "" + element.markdownRenderer.isRemoveHtmlBreaks());

        return serializeTo;

    }

    @Override
    public @NotNull TooltipEditorElement wrapIntoEditorElement(@NotNull TooltipElement element, @NotNull LayoutEditorScreen editor) {
        return new TooltipEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.tooltip");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.tooltip.desc");
    }

}
