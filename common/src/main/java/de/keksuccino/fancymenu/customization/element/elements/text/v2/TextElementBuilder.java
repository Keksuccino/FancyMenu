package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.Components;
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
        return i;
    }

    @Override
    public TextElement deserializeElement(@NotNull SerializedElement serialized) {

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

        element.scrollGrabberColorHexNormal = serialized.getValue("grabber_color_normal");
        element.scrollGrabberColorHexHover = serialized.getValue("grabber_color_hover");

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

        // Table properties
        String tableShowHeader = serialized.getValue("table_show_header");
        if (tableShowHeader != null) {
            element.markdownRenderer.setTableShowHeader(deserializeBoolean(true, tableShowHeader));
        }

        String tableAlternateRowColors = serialized.getValue("table_alternate_row_colors");
        if (tableAlternateRowColors != null) {
            element.markdownRenderer.setTableAlternateRowColors(deserializeBoolean(true, tableAlternateRowColors));
        }

        String tableLineColor = serialized.getValue("table_line_color");
        if (tableLineColor != null) {
            element.markdownRenderer.setTableLineColor(DrawableColor.of(tableLineColor));
        }

        String tableHeaderBackgroundColor = serialized.getValue("table_header_background_color");
        if (tableHeaderBackgroundColor != null) {
            element.markdownRenderer.setTableHeaderBackgroundColor(DrawableColor.of(tableHeaderBackgroundColor));
        }

        String tableRowBackgroundColor = serialized.getValue("table_row_background_color");
        if (tableRowBackgroundColor != null) {
            element.markdownRenderer.setTableRowBackgroundColor(DrawableColor.of(tableRowBackgroundColor));
        }

        String tableAlternateRowColor = serialized.getValue("table_alternate_row_color");
        if (tableAlternateRowColor != null) {
            element.markdownRenderer.setTableAlternateRowColor(DrawableColor.of(tableAlternateRowColor));
        }

        String tableLineThickness = serialized.getValue("table_line_thickness");
        if ((tableLineThickness != null) && MathUtils.isFloat(tableLineThickness)) {
            element.markdownRenderer.setTableLineThickness(Float.parseFloat(tableLineThickness));
        }

        String tableCellPadding = serialized.getValue("table_cell_padding");
        if ((tableCellPadding != null) && MathUtils.isFloat(tableCellPadding)) {
            element.markdownRenderer.setTableCellPadding(Float.parseFloat(tableCellPadding));
        }

        String tableMargin = serialized.getValue("table_margin");
        if ((tableMargin != null) && MathUtils.isFloat(tableMargin)) {
            element.markdownRenderer.setTableMargin(Float.parseFloat(tableMargin));
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
        if (element.scrollGrabberColorHexNormal != null) {
            serializeTo.putProperty("grabber_color_normal", element.scrollGrabberColorHexNormal);
        }
        if (element.scrollGrabberColorHexHover != null) {
            serializeTo.putProperty("grabber_color_hover", element.scrollGrabberColorHexHover);
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

        // Table properties
        serializeTo.putProperty("table_show_header", "" + element.markdownRenderer.isTableShowHeader());
        serializeTo.putProperty("table_alternate_row_colors", "" + element.markdownRenderer.isTableAlternateRowColors());
        serializeTo.putProperty("table_line_color", element.markdownRenderer.getTableLineColor().getHex());
        serializeTo.putProperty("table_header_background_color", element.markdownRenderer.getTableHeaderBackgroundColor().getHex());
        serializeTo.putProperty("table_row_background_color", element.markdownRenderer.getTableRowBackgroundColor().getHex());
        serializeTo.putProperty("table_alternate_row_color", element.markdownRenderer.getTableAlternateRowColor().getHex());
        serializeTo.putProperty("table_line_thickness", "" + element.markdownRenderer.getTableLineThickness());
        serializeTo.putProperty("table_cell_padding", "" + element.markdownRenderer.getTableCellPadding());
        serializeTo.putProperty("table_margin", "" + element.markdownRenderer.getTableMargin());

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
