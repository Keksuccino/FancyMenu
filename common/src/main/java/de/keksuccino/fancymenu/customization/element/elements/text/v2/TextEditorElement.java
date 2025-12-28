package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public class TextEditorElement extends AbstractEditorElement<TextEditorElement, TextElement> {

    public TextEditorElement(@NotNull TextElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "source_mode", Arrays.asList(TextElement.SourceMode.values()),
                        TextEditorElement.class,
                        consumes -> consumes.element.sourceMode,
                        (textEditorElement, sourceMode) -> {
                            textEditorElement.element.sourceMode = sourceMode;
                            textEditorElement.element.setSource(sourceMode, null);
                        },
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.source_mode.desc")));

        this.addTextResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_text_resource",
                        TextEditorElement.class,
                        null,
                        consumes -> consumes.element.textResourceSupplier,
                        (element, supplier) -> element.element.setSource(TextElement.SourceMode.RESOURCE, supplier.getSourceWithPrefix()),
                        Component.translatable("fancymenu.elements.text.v2.source.choose"),
                        false, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("text"))
                .setIsVisibleSupplier((menu, entry) -> this.element.sourceMode == TextElement.SourceMode.RESOURCE)
                .setStackable(false);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_text_content",
                        TextEditorElement.class,
                        consumes -> consumes.element.source,
                        (textEditorElement, s) -> textEditorElement.element.setSource(TextElement.SourceMode.DIRECT, s),
                        null, true, true, Component.translatable("fancymenu.elements.text.v2.source.input"),
                        false, null, null, null)
                .setIcon(ContextMenu.IconFactory.getIcon("text"))
                .setIsVisibleSupplier((menu, entry) -> this.element.sourceMode == TextElement.SourceMode.DIRECT)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("text_separator_1");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "remove_html_breaks", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isRemoveHtmlBreaks(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setRemoveHtmlBreaks(aBoolean),
                        "fancymenu.elements.text.remove_html_breaks")
                .setStackable(true);

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_case_mode",
                ListUtils.of(MarkdownRenderer.TextCase.NORMAL, MarkdownRenderer.TextCase.ALL_UPPER, MarkdownRenderer.TextCase.ALL_LOWER),
                consumes -> (consumes instanceof TextEditorElement),
                consumes -> ((TextElement)consumes.element).markdownRenderer.getTextCase(),
                (element1, caseMode) -> ((TextElement)element1.element).markdownRenderer.setTextCase(caseMode),
                (menu, entry, switcherValue) -> {
                    if (switcherValue == MarkdownRenderer.TextCase.NORMAL) {
                        return Component.translatable("fancymenu.elements.text.case_mode.normal");
                    }
                    if (switcherValue == MarkdownRenderer.TextCase.ALL_LOWER) {
                        return Component.translatable("fancymenu.elements.text.case_mode.lower");
                    }
                    return Component.translatable("fancymenu.elements.text.case_mode.upper");
                }).setIcon(ContextMenu.IconFactory.getIcon("text_size"));

        this.addGenericFloatInputContextMenuEntryTo(this.rightClickMenu, "set_scale",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).markdownRenderer.getTextBaseScale(),
                        (element1, aFloat) -> {
                            ((TextElement)element1.element).markdownRenderer.setTextBaseScale(Math.max(0.2F, aFloat));
                            ((TextElement)element1.element).updateContent();
                        },
                        Component.translatable("fancymenu.elements.text.scale"),
                        true, 1.0F, null, null)
                .setIcon(ContextMenu.IconFactory.getIcon("measure"))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_shadow", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isTextShadow(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setTextShadow(aBoolean),
                        "fancymenu.elements.text.shadow")
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("shadow"));

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_base_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).markdownRenderer.getTextBaseColor().getHex(),
                        (element, colorHex) -> ((TextElement)element.element).markdownRenderer.setTextBaseColor(DrawableColor.of(colorHex)), null, false, false, Component.translatable("fancymenu.elements.text.base_color"),
                        true, DrawableColor.WHITE.getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.base_color.desc")));

        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_border",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> (int)((TextElement)consumes.element).markdownRenderer.getBorder(),
                        (element, border) -> ((TextElement)element.element).markdownRenderer.setBorder(Math.max(0, border)),
                        Component.translatable("fancymenu.elements.text.text_border"),
                        true, 2, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.text_border.desc")));

        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_line_spacing",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> (int)((TextElement)consumes.element).markdownRenderer.getLineSpacing(),
                        (element, border) -> {
                            ((TextElement)element.element).markdownRenderer.setLineSpacing(Math.max(0, border));
                        },
                        Component.translatable("fancymenu.elements.text.line_spacing"),
                        true, 2, null, null)
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_line_wrapping", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isAutoLineBreakingEnabled(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setAutoLineBreakingEnabled(aBoolean),
                        "fancymenu.elements.text.auto_line_wrapping")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_line_wrapping").setStackable(true);

        ContextMenu markdownMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("markdown", Component.translatable("fancymenu.elements.text.markdown"), markdownMenu).setStackable(true);

        this.addToggleContextMenuEntryTo(markdownMenu, "parse_markdown",
                        TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isParseMarkdown(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setParseMarkdown(aBoolean),
                        "fancymenu.elements.text.markdown.toggle")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.toggle.desc")));

        markdownMenu.addSeparatorEntry("separator_after_parse_markdown");

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_single_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getCodeBlockSingleLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setCodeBlockSingleLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.code_block.single.color"),
                        true, DrawableColor.of(115, 115, 115).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_multi_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getCodeBlockMultiLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setCodeBlockMultiLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.code_block.multi.color"),
                        true, DrawableColor.of(86, 86, 86).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_code_block");

        this.addStringInputContextMenuEntryTo(markdownMenu, "headline_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getHeadlineUnderlineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setHeadlineLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.headline.line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_headline");

        this.addStringInputContextMenuEntryTo(markdownMenu, "separation_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getSeparationLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setSeparationLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.separation_line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_separation_line");

        this.addStringInputContextMenuEntryTo(markdownMenu, "hyperlink_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getHyperlinkColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setHyperlinkColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.hyperlink.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(markdownMenu, "click_event_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTextClickEventColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTextClickEventColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.click_event.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(markdownMenu, "hover_event_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTextHoverEventColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTextHoverEventColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.hover_event.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_hyperlink");

        this.addStringInputContextMenuEntryTo(markdownMenu, "quote_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getQuoteColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setQuoteColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.quote.color"),
                        true, DrawableColor.of(129, 129, 129).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "quote_indent", TextEditorElement.class,
                        consumes -> (int)consumes.element.markdownRenderer.getQuoteIndent(),
                        (textEditorElement, integer) -> textEditorElement.element.markdownRenderer.setQuoteIndent(integer),
                        Component.translatable("fancymenu.elements.text.markdown.quote.indent"), true, 8, null, null)
                .setStackable(true);

        this.addToggleContextMenuEntryTo(markdownMenu, "quote_italic", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isQuoteItalic(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setQuoteItalic(aBoolean),
                        "fancymenu.elements.text.markdown.quote.italic")
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_quote");

        this.addStringInputContextMenuEntryTo(markdownMenu, "bullet_list_dot_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getBulletListDotColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setBulletListDotColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.bullet_list.dot.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "bullet_list_indent", TextEditorElement.class,
                        consumes -> (int)consumes.element.markdownRenderer.getBulletListIndent(),
                        (textEditorElement, integer) -> textEditorElement.element.markdownRenderer.setBulletListIndent(integer),
                        Component.translatable("fancymenu.elements.text.markdown.bullet_list.indent"), true, 8, null, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "bullet_list_spacing", TextEditorElement.class,
                        consumes -> (int)consumes.element.markdownRenderer.getBulletListSpacing(),
                        (textEditorElement, integer) -> textEditorElement.element.markdownRenderer.setBulletListSpacing(integer),
                        Component.translatable("fancymenu.elements.text.markdown.bullet_list.spacing"), true, 3, null, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_bullet_list");

        // Table customization submenu
        ContextMenu tableMenu = new ContextMenu();
        markdownMenu.addSubMenuEntry("tables", Component.translatable("fancymenu.elements.text.markdown.tables"), tableMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.desc")))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(tableMenu, "table_show_header", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isTableShowHeader(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setTableShowHeader(aBoolean),
                        "fancymenu.elements.text.markdown.tables.show_header")
                .setStackable(true);

        this.addToggleContextMenuEntryTo(tableMenu, "table_alternate_row_colors", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isTableAlternateRowColors(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setTableAlternateRowColors(aBoolean),
                        "fancymenu.elements.text.markdown.tables.alternate_row_colors")
                .setStackable(true);

        tableMenu.addSeparatorEntry("separator_after_table_toggles");

        this.addStringInputContextMenuEntryTo(tableMenu, "table_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.line_color"),
                        true, DrawableColor.of(120, 120, 120).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_header_background_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableHeaderBackgroundColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableHeaderBackgroundColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.header_background_color"),
                        true, DrawableColor.of(50, 50, 50).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_row_background_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableRowBackgroundColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableRowBackgroundColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.row_background_color"),
                        true, DrawableColor.of(40, 40, 40).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_alternate_row_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableAlternateRowColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableAlternateRowColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.alternate_row_color"),
                        true, DrawableColor.of(60, 60, 60).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.alternate_row_color.desc")));

        tableMenu.addSeparatorEntry("separator_after_table_colors");

        this.addFloatInputContextMenuEntryTo(tableMenu, "table_line_thickness", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableLineThickness(),
                        (textEditorElement, aFloat) -> textEditorElement.element.markdownRenderer.setTableLineThickness(Math.max(0.5F, aFloat)),
                        Component.translatable("fancymenu.elements.text.markdown.tables.line_thickness"), true, 1.0F, null, null)
                .setStackable(true);

        this.addFloatInputContextMenuEntryTo(tableMenu, "table_cell_padding", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableCellPadding(),
                        (textEditorElement, aFloat) -> textEditorElement.element.markdownRenderer.setTableCellPadding(Math.max(0.0F, aFloat)),
                        Component.translatable("fancymenu.elements.text.markdown.tables.cell_padding"), true, 8.0F, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.cell_padding.desc")));

        this.addFloatInputContextMenuEntryTo(tableMenu, "table_margin", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableMargin(),
                        (textEditorElement, aFloat) -> textEditorElement.element.markdownRenderer.setTableMargin(Math.max(0.0F, aFloat)),
                        Component.translatable("fancymenu.elements.text.markdown.tables.margin"), true, 4.0F, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.margin.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_markdown");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_scrolling", TextEditorElement.class,
                        consumes -> consumes.element.enableScrolling,
                        (textEditorElement, aBoolean) -> textEditorElement.element.enableScrolling = aBoolean,
                        "fancymenu.elements.text.scrolling")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_scrolling");

        ContextMenu grabberTextureMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_texture", Component.translatable("fancymenu.elements.text.scroll_grabber_texture"), grabberTextureMenu)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("image"));

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "vertical_normal_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.verticalScrollGrabberTextureNormal,
                (element, supplier) -> element.element.verticalScrollGrabberTextureNormal = supplier,
                Component.translatable("fancymenu.elements.text.vertical_scroll_grabber_texture.normal"),
                true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "vertical_hover_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.verticalScrollGrabberTextureHover,
                (element, supplier) -> element.element.verticalScrollGrabberTextureHover = supplier,
                Component.translatable("fancymenu.elements.text.vertical_scroll_grabber_texture.hover"),
                true, null, true, true, true);

        grabberTextureMenu.addSeparatorEntry("separator_after_vertical_textures");

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_normal_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.horizontalScrollGrabberTextureNormal,
                (element, supplier) -> element.element.horizontalScrollGrabberTextureNormal = supplier,
                Component.translatable("fancymenu.elements.text.horizontal_scroll_grabber_texture.normal"),
                true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_hover_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.horizontalScrollGrabberTextureHover,
                (element, supplier) -> element.element.horizontalScrollGrabberTextureHover = supplier,
                Component.translatable("fancymenu.elements.text.horizontal_scroll_grabber_texture.hover"),
                true, null, true, true, true);

        ContextMenu grabberColorMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_color", Component.translatable("fancymenu.elements.text.scroll_grabber_color"), grabberColorMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.scroll_grabber_color.desc")));

        this.addGenericStringInputContextMenuEntryTo(grabberColorMenu, "normal_grabber_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).scrollGrabberColorHexNormal,
                        (element, colorHex) -> ((TextElement)element.element).scrollGrabberColorHexNormal = colorHex,
                        null, false, false, Component.translatable("fancymenu.elements.text.scroll_grabber_color.normal"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(grabberColorMenu, "hover_grabber_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).scrollGrabberColorHexHover,
                        (element, colorHex) -> ((TextElement)element.element).scrollGrabberColorHexHover = colorHex,
                        null, false, false, Component.translatable("fancymenu.elements.text.scroll_grabber_color.hover"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_hover_grabber_color");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "interactable", TextEditorElement.class,
                        consumes -> consumes.element.interactable,
                        (textEditorElement, aBoolean) -> textEditorElement.element.interactable = aBoolean,
                        "fancymenu.elements.text.v2.interactable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.interactable.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.editor.children().contains(this.element.scrollArea)) {
            ((IMixinScreen)this.editor).getChildrenFancyMenu().add(this.element.scrollArea);
        }

        super.render(graphics, mouseX, mouseY, partial);

    }


}
