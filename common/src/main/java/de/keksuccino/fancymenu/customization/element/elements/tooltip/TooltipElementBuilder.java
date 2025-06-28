package de.keksuccino.fancymenu.customization.element.elements.tooltip;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.konkrete.math.MathUtils;
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
        return element;
    }

    @Override
    public TooltipElement deserializeElement(@NotNull SerializedElement serialized) {

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
        
        // Nine-slice borders
        String nineSliceBorderTopString = serialized.getValue("nine_slice_border_top");
        if ((nineSliceBorderTopString != null) && MathUtils.isInteger(nineSliceBorderTopString)) {
            element.nineSliceBorderTop = Integer.parseInt(nineSliceBorderTopString);
        }
        
        String nineSliceBorderRightString = serialized.getValue("nine_slice_border_right");
        if ((nineSliceBorderRightString != null) && MathUtils.isInteger(nineSliceBorderRightString)) {
            element.nineSliceBorderRight = Integer.parseInt(nineSliceBorderRightString);
        }
        
        String nineSliceBorderBottomString = serialized.getValue("nine_slice_border_bottom");
        if ((nineSliceBorderBottomString != null) && MathUtils.isInteger(nineSliceBorderBottomString)) {
            element.nineSliceBorderBottom = Integer.parseInt(nineSliceBorderBottomString);
        }
        
        String nineSliceBorderLeftString = serialized.getValue("nine_slice_border_left");
        if ((nineSliceBorderLeftString != null) && MathUtils.isInteger(nineSliceBorderLeftString)) {
            element.nineSliceBorderLeft = Integer.parseInt(nineSliceBorderLeftString);
        }

        // Markdown renderer settings
        String shadowString = serialized.getValue("shadow");
        if (shadowString != null) {
            if (shadowString.equals("false")) element.markdownRenderer.setTextShadow(false);
            if (shadowString.equals("true")) element.markdownRenderer.setTextShadow(true);
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

        String scaleString = serialized.getValue("scale");
        if (scaleString != null) {
            if (MathUtils.isFloat(scaleString)) {
                element.markdownRenderer.setTextBaseScale(Float.parseFloat(scaleString));
            }
        }

        String baseColorString = serialized.getValue("base_color");
        if (baseColorString != null) {
            DrawableColor c = DrawableColor.of(baseColorString);
            element.markdownRenderer.setTextBaseColor(c);
        }

        String textBorderString = serialized.getValue("text_border");
        if ((textBorderString != null) && MathUtils.isInteger(textBorderString)) {
            element.markdownRenderer.setBorder(Integer.parseInt(textBorderString));
        }

        String lineSpacingString = serialized.getValue("line_spacing");
        if ((lineSpacingString != null) && MathUtils.isInteger(lineSpacingString)) {
            element.markdownRenderer.setLineSpacing(Integer.parseInt(lineSpacingString));
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

        // Markdown colors
        String codeBlockSingleColor = serialized.getValue("code_block_single_color");
        if (codeBlockSingleColor != null) {
            element.markdownRenderer.setCodeBlockSingleLineColor(DrawableColor.of(codeBlockSingleColor));
        }
        String codeBlockMultiColor = serialized.getValue("code_block_multi_color");
        if (codeBlockMultiColor != null) {
            element.markdownRenderer.setCodeBlockMultiLineColor(DrawableColor.of(codeBlockMultiColor));
        }

        String headlineLineColor = serialized.getValue("headline_line_color");
        if (headlineLineColor != null) {
            element.markdownRenderer.setHeadlineLineColor(DrawableColor.of(headlineLineColor));
        }

        String separationLineColor = serialized.getValue("separation_line_color");
        if (separationLineColor != null) {
            element.markdownRenderer.setSeparationLineColor(DrawableColor.of(separationLineColor));
        }

        String hyperlinkColor = serialized.getValue("hyperlink_color");
        if (hyperlinkColor != null) {
            element.markdownRenderer.setHyperlinkColor(DrawableColor.of(hyperlinkColor));
        }

        String quoteColor = serialized.getValue("quote_color");
        if (quoteColor != null) {
            element.markdownRenderer.setQuoteColor(DrawableColor.of(quoteColor));
        }
        String quoteIndent = serialized.getValue("quote_indent");
        if ((quoteIndent != null) && MathUtils.isInteger(quoteIndent)) {
            element.markdownRenderer.setQuoteIndent(Integer.parseInt(quoteIndent));
        }
        String quoteItalic = serialized.getValue("quote_italic");
        if (quoteItalic != null) {
            if (quoteItalic.equals("true")) element.markdownRenderer.setQuoteItalic(true);
            if (quoteItalic.equals("false")) element.markdownRenderer.setQuoteItalic(false);
        }

        String bulletListDotColor = serialized.getValue("bullet_list_dot_color");
        if (bulletListDotColor != null) {
            element.markdownRenderer.setBulletListDotColor(DrawableColor.of(bulletListDotColor));
        }
        String bulletListIndent = serialized.getValue("bullet_list_indent");
        if ((bulletListIndent != null) && MathUtils.isInteger(bulletListIndent)) {
            element.markdownRenderer.setBulletListIndent(Integer.parseInt(bulletListIndent));
        }
        String bulletListSpacing = serialized.getValue("bullet_list_spacing");
        if ((bulletListSpacing != null) && MathUtils.isInteger(bulletListSpacing)) {
            element.markdownRenderer.setBulletListSpacing(Integer.parseInt(bulletListSpacing));
        }

        element.markdownRenderer.setParseMarkdown(deserializeBoolean(true, serialized.getValue("parse_markdown")));

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
        
        serializeTo.putProperty("nine_slice_border_top", "" + element.nineSliceBorderTop);
        serializeTo.putProperty("nine_slice_border_right", "" + element.nineSliceBorderRight);
        serializeTo.putProperty("nine_slice_border_bottom", "" + element.nineSliceBorderBottom);
        serializeTo.putProperty("nine_slice_border_left", "" + element.nineSliceBorderLeft);

        // Markdown renderer settings
        serializeTo.putProperty("shadow", "" + element.markdownRenderer.isTextShadow());
        if (element.markdownRenderer.getTextCase() == MarkdownRenderer.TextCase.ALL_LOWER) {
            serializeTo.putProperty("case_mode", "lower");
        } else if (element.markdownRenderer.getTextCase() == MarkdownRenderer.TextCase.ALL_UPPER) {
            serializeTo.putProperty("case_mode", "upper");
        }
        serializeTo.putProperty("scale", "" + element.markdownRenderer.getTextBaseScale());
        serializeTo.putProperty("base_color", element.markdownRenderer.getTextBaseColor().getHex());
        serializeTo.putProperty("text_border", "" + (int)element.markdownRenderer.getBorder());
        serializeTo.putProperty("line_spacing", "" + (int)element.markdownRenderer.getLineSpacing());
        serializeTo.putProperty("auto_line_wrapping", "" + element.markdownRenderer.isAutoLineBreakingEnabled());
        serializeTo.putProperty("remove_html_breaks", "" + element.markdownRenderer.isRemoveHtmlBreaks());
        serializeTo.putProperty("code_block_single_color", element.markdownRenderer.getCodeBlockSingleLineColor().getHex());
        serializeTo.putProperty("code_block_multi_color", element.markdownRenderer.getCodeBlockMultiLineColor().getHex());
        serializeTo.putProperty("headline_line_color", element.markdownRenderer.getHeadlineUnderlineColor().getHex());
        serializeTo.putProperty("separation_line_color", element.markdownRenderer.getSeparationLineColor().getHex());
        serializeTo.putProperty("hyperlink_color", element.markdownRenderer.getHyperlinkColor().getHex());
        serializeTo.putProperty("quote_color", element.markdownRenderer.getQuoteColor().getHex());
        serializeTo.putProperty("quote_indent", "" + element.markdownRenderer.getQuoteIndent());
        serializeTo.putProperty("quote_italic", "" + element.markdownRenderer.isQuoteItalic());
        serializeTo.putProperty("bullet_list_dot_color", element.markdownRenderer.getBulletListDotColor().getHex());
        serializeTo.putProperty("bullet_list_indent", "" + element.markdownRenderer.getBulletListIndent());
        serializeTo.putProperty("bullet_list_spacing", "" + element.markdownRenderer.getBulletListSpacing());
        serializeTo.putProperty("parse_markdown", "" + element.markdownRenderer.isParseMarkdown());

        return serializeTo;

    }

    @Override
    public @NotNull TooltipEditorElement wrapIntoEditorElement(@NotNull TooltipElement element, @NotNull LayoutEditorScreen editor) {
        return new TooltipEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Components.translatable("fancymenu.elements.tooltip");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.tooltip.desc");
    }

}
