package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
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
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.source_mode.desc")))
                .setIcon(MaterialIcons.TUNE);

        this.addTextResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_text_resource",
                        TextEditorElement.class,
                        null,
                        consumes -> consumes.element.textResourceSupplier,
                        (element, supplier) -> element.element.setSource(TextElement.SourceMode.RESOURCE, supplier.getSourceWithPrefix()),
                        Component.translatable("fancymenu.elements.text.v2.source.choose"),
                        false, null, true, true, true)
                .setIcon(MaterialIcons.FILE_OPEN)
                .addIsVisibleSupplier((menu, entry) -> this.element.sourceMode == TextElement.SourceMode.RESOURCE)
                .setStackable(false);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_text_content",
                        TextEditorElement.class,
                        consumes -> consumes.element.source,
                        (textEditorElement, s) -> textEditorElement.element.setSource(TextElement.SourceMode.DIRECT, s),
                        null, true, true, Component.translatable("fancymenu.elements.text.v2.source.input"),
                        false, null, null, null)
                .setIcon(MaterialIcons.TEXT_FIELDS)
                .addIsVisibleSupplier((menu, entry) -> this.element.sourceMode == TextElement.SourceMode.DIRECT)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("text_separator_1");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "remove_html_breaks", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isRemoveHtmlBreaks(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setRemoveHtmlBreaks(aBoolean),
                        "fancymenu.elements.text.remove_html_breaks")
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_TEXT_WRAP);

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_case_mode",
                ListUtils.of(MarkdownRenderer.TextCase.NORMAL, MarkdownRenderer.TextCase.ALL_UPPER, MarkdownRenderer.TextCase.ALL_LOWER),
                consumes -> (consumes instanceof TextEditorElement),
                consumes -> (consumes.element).markdownRenderer.getTextCase(),
                (element1, caseMode) -> (element1.element).markdownRenderer.setTextCase(caseMode),
                (menu, entry, switcherValue) -> {
                    if (switcherValue == MarkdownRenderer.TextCase.NORMAL) {
                        return Component.translatable("fancymenu.elements.text.case_mode.normal");
                    }
                    if (switcherValue == MarkdownRenderer.TextCase.ALL_LOWER) {
                        return Component.translatable("fancymenu.elements.text.case_mode.lower");
                    }
                    return Component.translatable("fancymenu.elements.text.case_mode.upper");
                }).setIcon(MaterialIcons.TEXT_FORMAT);

        this.element.textScale.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.FORMAT_SIZE)
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_shadow", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isTextShadow(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setTextShadow(aBoolean),
                        "fancymenu.elements.text.shadow")
                .setStackable(true)
                .setIcon(MaterialIcons.SHADOW);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_base_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> (consumes.element).markdownRenderer.getTextBaseColor().getHex(),
                        (element, colorHex) -> (element.element).markdownRenderer.setTextBaseColor(DrawableColor.of(colorHex)), null, false, false, Component.translatable("fancymenu.elements.text.base_color"),
                        true, DrawableColor.WHITE.getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.base_color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        this.element.textBorder.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.text_border.desc")))
                .setIcon(MaterialIcons.BORDER_OUTER);

        this.element.lineSpacing.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_LINE_SPACING);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_line_wrapping", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isAutoLineBreakingEnabled(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setAutoLineBreakingEnabled(aBoolean),
                        "fancymenu.elements.text.auto_line_wrapping")
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_TEXT_WRAP);

        this.rightClickMenu.addSeparatorEntry("separator_after_line_wrapping").setStackable(true);

        ContextMenu markdownMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("markdown", Component.translatable("fancymenu.elements.text.markdown"), markdownMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.MENU_BOOK);

        this.addToggleContextMenuEntryTo(markdownMenu, "parse_markdown",
                        TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isParseMarkdown(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setParseMarkdown(aBoolean),
                        "fancymenu.elements.text.markdown.toggle")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.toggle.desc")))
                .setIcon(MaterialIcons.CODE);

        markdownMenu.addSeparatorEntry("separator_after_parse_markdown");

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_single_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getCodeBlockSingleLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setCodeBlockSingleLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.code_block.single.color"),
                        true, DrawableColor.of(115, 115, 115).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_multi_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getCodeBlockMultiLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setCodeBlockMultiLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.code_block.multi.color"),
                        true, DrawableColor.of(86, 86, 86).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        markdownMenu.addSeparatorEntry("separator_after_code_block");

        this.addStringInputContextMenuEntryTo(markdownMenu, "headline_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getHeadlineUnderlineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setHeadlineLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.headline.line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        markdownMenu.addSeparatorEntry("separator_after_headline");

        this.addStringInputContextMenuEntryTo(markdownMenu, "separation_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getSeparationLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setSeparationLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.separation_line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        markdownMenu.addSeparatorEntry("separator_after_separation_line");

        this.addStringInputContextMenuEntryTo(markdownMenu, "hyperlink_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getHyperlinkColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setHyperlinkColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.hyperlink.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.addStringInputContextMenuEntryTo(markdownMenu, "click_event_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTextClickEventColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTextClickEventColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.click_event.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.addStringInputContextMenuEntryTo(markdownMenu, "hover_event_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTextHoverEventColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTextHoverEventColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.hover_event.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        markdownMenu.addSeparatorEntry("separator_after_hyperlink");

        this.addStringInputContextMenuEntryTo(markdownMenu, "quote_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getQuoteColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setQuoteColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.quote.color"),
                        true, DrawableColor.of(129, 129, 129).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.element.quoteIndent.buildContextMenuEntryAndAddTo(markdownMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_INDENT_INCREASE);

        this.addToggleContextMenuEntryTo(markdownMenu, "quote_italic", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isQuoteItalic(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setQuoteItalic(aBoolean),
                        "fancymenu.elements.text.markdown.quote.italic")
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_ITALIC);

        markdownMenu.addSeparatorEntry("separator_after_quote");

        this.addStringInputContextMenuEntryTo(markdownMenu, "bullet_list_dot_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getBulletListDotColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setBulletListDotColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.bullet_list.dot.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.element.bulletListIndent.buildContextMenuEntryAndAddTo(markdownMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_INDENT_INCREASE);

        this.element.bulletListSpacing.buildContextMenuEntryAndAddTo(markdownMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_LINE_SPACING);

        markdownMenu.addSeparatorEntry("separator_after_bullet_list");

        // Table customization submenu
        ContextMenu tableMenu = new ContextMenu();
        markdownMenu.addSubMenuEntry("tables", Component.translatable("fancymenu.elements.text.markdown.tables"), tableMenu)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.desc")))
                .setStackable(true)
                .setIcon(MaterialIcons.TABLE_CHART);

        this.addToggleContextMenuEntryTo(tableMenu, "table_show_header", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isTableShowHeader(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setTableShowHeader(aBoolean),
                        "fancymenu.elements.text.markdown.tables.show_header")
                .setStackable(true)
                .setIcon(MaterialIcons.TABLE_VIEW);

        this.addToggleContextMenuEntryTo(tableMenu, "table_alternate_row_colors", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isTableAlternateRowColors(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setTableAlternateRowColors(aBoolean),
                        "fancymenu.elements.text.markdown.tables.alternate_row_colors")
                .setStackable(true)
                .setIcon(MaterialIcons.VIEW_LIST);

        tableMenu.addSeparatorEntry("separator_after_table_toggles");

        this.addStringInputContextMenuEntryTo(tableMenu, "table_line_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.line_color"),
                        true, DrawableColor.of(120, 120, 120).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_header_background_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableHeaderBackgroundColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableHeaderBackgroundColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.header_background_color"),
                        true, DrawableColor.of(50, 50, 50).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_row_background_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableRowBackgroundColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableRowBackgroundColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.row_background_color"),
                        true, DrawableColor.of(40, 40, 40).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_alternate_row_color", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.getTableAlternateRowColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.element.markdownRenderer.setTableAlternateRowColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.elements.text.markdown.tables.alternate_row_color"),
                        true, DrawableColor.of(60, 60, 60).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.alternate_row_color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        tableMenu.addSeparatorEntry("separator_after_table_colors");

        this.element.tableLineThickness.buildContextMenuEntryAndAddTo(tableMenu, this)
                .setStackable(true)
                .setIcon(MaterialIcons.LINE_WEIGHT);

        this.element.tableCellPadding.buildContextMenuEntryAndAddTo(tableMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.cell_padding.desc")))
                .setIcon(MaterialIcons.PADDING_ICON);

        this.element.tableMargin.buildContextMenuEntryAndAddTo(tableMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.markdown.tables.margin.desc")))
                .setIcon(MaterialIcons.MARGIN);

        this.rightClickMenu.addSeparatorEntry("separator_after_markdown");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_scrolling", TextEditorElement.class,
                        consumes -> consumes.element.enableScrolling,
                        (textEditorElement, aBoolean) -> textEditorElement.element.enableScrolling = aBoolean,
                        "fancymenu.elements.text.scrolling")
                .setStackable(true)
                .setIcon(MaterialIcons.SWIPE_VERTICAL);

        this.rightClickMenu.addSeparatorEntry("separator_after_scrolling");

        ContextMenu grabberTextureMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_texture", Component.translatable("fancymenu.elements.text.scroll_grabber_texture"), grabberTextureMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "vertical_normal_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.verticalScrollGrabberTextureNormal,
                (element, supplier) -> element.element.verticalScrollGrabberTextureNormal = supplier,
                Component.translatable("fancymenu.elements.text.vertical_scroll_grabber_texture.normal"),
                true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "vertical_hover_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.verticalScrollGrabberTextureHover,
                (element, supplier) -> element.element.verticalScrollGrabberTextureHover = supplier,
                Component.translatable("fancymenu.elements.text.vertical_scroll_grabber_texture.hover"),
                true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        grabberTextureMenu.addSeparatorEntry("separator_after_vertical_textures");

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_normal_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.horizontalScrollGrabberTextureNormal,
                (element, supplier) -> element.element.horizontalScrollGrabberTextureNormal = supplier,
                Component.translatable("fancymenu.elements.text.horizontal_scroll_grabber_texture.normal"),
                true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_hover_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.element.horizontalScrollGrabberTextureHover,
                (element, supplier) -> element.element.horizontalScrollGrabberTextureHover = supplier,
                Component.translatable("fancymenu.elements.text.horizontal_scroll_grabber_texture.hover"),
                true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenu grabberColorMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_color", Component.translatable("fancymenu.elements.text.scroll_grabber_color"), grabberColorMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.scroll_grabber_color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        this.element.scrollGrabberColorHexNormal.buildContextMenuEntryAndAddTo(grabberColorMenu, this)
                .setIcon(MaterialIcons.PALETTE);

        this.element.scrollGrabberColorHexHover.buildContextMenuEntryAndAddTo(grabberColorMenu, this)
                .setIcon(MaterialIcons.PALETTE);

        this.rightClickMenu.addSeparatorEntry("separator_after_hover_grabber_color");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "interactable", TextEditorElement.class,
                        consumes -> consumes.element.interactable,
                        (textEditorElement, aBoolean) -> textEditorElement.element.interactable = aBoolean,
                        "fancymenu.elements.text.v2.interactable")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.interactable.desc")))
                .setIcon(MaterialIcons.TOUCH_APP);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.editor.children().contains(this.element.scrollArea)) {
            ((IMixinScreen)this.editor).getChildrenFancyMenu().add(this.element.scrollArea);
        }

        super.render(graphics, mouseX, mouseY, partial);

    }


}
