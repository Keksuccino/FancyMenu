package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
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

public class TextEditorElement extends AbstractEditorElement {

    public TextEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "source_mode", Arrays.asList(TextElement.SourceMode.values()),
                        TextEditorElement.class,
                        consumes -> consumes.getElement().sourceMode,
                        (textEditorElement, sourceMode) -> {
                            textEditorElement.getElement().sourceMode = sourceMode;
                            textEditorElement.getElement().setSource(sourceMode, null);
                        },
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.source_mode.desc")));

        this.addTextResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_text_resource",
                        TextEditorElement.class,
                        null,
                        consumes -> consumes.getElement().textResourceSupplier,
                        (element, supplier) -> element.getElement().setSource(TextElement.SourceMode.RESOURCE, supplier.getSourceWithPrefix()),
                        Component.translatable("fancymenu.elements.text.v2.source.choose"),
                        false, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("text"))
                .setIsVisibleSupplier((menu, entry) -> this.getElement().sourceMode == TextElement.SourceMode.RESOURCE)
                .setStackable(false);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_text_content",
                        TextEditorElement.class,
                        consumes -> consumes.getElement().source,
                        (textEditorElement, s) -> textEditorElement.getElement().setSource(TextElement.SourceMode.DIRECT, s),
                        null, true, true, Component.translatable("fancymenu.elements.text.v2.source.input"),
                        false, null, null, null)
                .setIcon(ContextMenu.IconFactory.getIcon("text"))
                .setIsVisibleSupplier((menu, entry) -> this.getElement().sourceMode == TextElement.SourceMode.DIRECT)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("text_separator_1");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "remove_html_breaks", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.isRemoveHtmlBreaks(),
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().markdownRenderer.setRemoveHtmlBreaks(aBoolean),
                        "fancymenu.customization.items.text.remove_html_breaks")
                .setStackable(true);

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_case_mode",
                ListUtils.of(MarkdownRenderer.TextCase.NORMAL, MarkdownRenderer.TextCase.ALL_UPPER, MarkdownRenderer.TextCase.ALL_LOWER),
                consumes -> (consumes instanceof TextEditorElement),
                consumes -> ((TextElement)consumes.element).markdownRenderer.getTextCase(),
                (element1, caseMode) -> ((TextElement)element1.element).markdownRenderer.setTextCase(caseMode),
                (menu, entry, switcherValue) -> {
                    if (switcherValue == MarkdownRenderer.TextCase.NORMAL) {
                        return Component.translatable("fancymenu.customization.items.text.case_mode.normal");
                    }
                    if (switcherValue == MarkdownRenderer.TextCase.ALL_LOWER) {
                        return Component.translatable("fancymenu.customization.items.text.case_mode.lower");
                    }
                    return Component.translatable("fancymenu.customization.items.text.case_mode.upper");
                }).setIcon(ContextMenu.IconFactory.getIcon("text_size"));

        this.addGenericFloatInputContextMenuEntryTo(this.rightClickMenu, "set_scale",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).markdownRenderer.getTextBaseScale(),
                        (element1, aFloat) -> {
                            ((TextElement)element1.element).markdownRenderer.setTextBaseScale(Math.max(0.2F, aFloat));
                            ((TextElement)element1.element).updateContent();
                        },
                        Component.translatable("fancymenu.customization.items.text.scale"),
                        true, 1.0F, null, null)
                .setIcon(ContextMenu.IconFactory.getIcon("measure"))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_shadow", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.isTextShadow(),
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().markdownRenderer.setTextShadow(aBoolean),
                        "fancymenu.customization.items.text.shadow")
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("shadow"));

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_base_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).markdownRenderer.getTextBaseColor().getHex(),
                        (element, colorHex) -> ((TextElement)element.element).markdownRenderer.setTextBaseColor(DrawableColor.of(colorHex)), null, false, false, Component.translatable("fancymenu.customization.items.text.base_color"),
                        true, DrawableColor.WHITE.getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.base_color.desc")));

        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_border",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> (int)((TextElement)consumes.element).markdownRenderer.getBorder(),
                        (element, border) -> ((TextElement)element.element).markdownRenderer.setBorder(Math.max(0, border)),
                        Component.translatable("fancymenu.customization.items.text.text_border"),
                        true, 2, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.text_border.desc")));

        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_line_spacing",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> (int)((TextElement)consumes.element).markdownRenderer.getLineSpacing(),
                        (element, border) -> {
                            ((TextElement)element.element).markdownRenderer.setLineSpacing(Math.max(0, border));
                        },
                        Component.translatable("fancymenu.customization.items.text.line_spacing"),
                        true, 2, null, null)
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_scrolling", TextEditorElement.class,
                        consumes -> consumes.getElement().enableScrolling,
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().enableScrolling = aBoolean,
                        "fancymenu.customization.items.text.scrolling")
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_line_wrapping", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.isAutoLineBreakingEnabled(),
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().markdownRenderer.setAutoLineBreakingEnabled(aBoolean),
                        "fancymenu.customization.items.text.auto_line_wrapping")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_line_wrapping").setStackable(true);

        ContextMenu markdownMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("markdown", Component.translatable("fancymenu.customization.items.text.markdown"), markdownMenu).setStackable(true);

        this.addToggleContextMenuEntryTo(markdownMenu, "parse_markdown",
                        TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.isParseMarkdown(),
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().markdownRenderer.setParseMarkdown(aBoolean),
                        "fancymenu.customization.items.text.markdown.toggle")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.markdown.toggle.desc")));

        markdownMenu.addSeparatorEntry("separator_after_parse_markdown");

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_single_line_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getCodeBlockSingleLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setCodeBlockSingleLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.code_block.single.color"),
                        true, DrawableColor.of(115, 115, 115).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_multi_line_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getCodeBlockMultiLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setCodeBlockMultiLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.code_block.multi.color"),
                        true, DrawableColor.of(86, 86, 86).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_code_block");

        this.addStringInputContextMenuEntryTo(markdownMenu, "headline_line_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getHeadlineUnderlineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setHeadlineLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.headline.line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_headline");

        this.addStringInputContextMenuEntryTo(markdownMenu, "separation_line_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getSeparationLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setSeparationLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.separation_line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_separation_line");

        this.addStringInputContextMenuEntryTo(markdownMenu, "hyperlink_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getHyperlinkColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setHyperlinkColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.hyperlink.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_hyperlink");

        this.addStringInputContextMenuEntryTo(markdownMenu, "quote_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getQuoteColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setQuoteColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.quote.color"),
                        true, DrawableColor.of(129, 129, 129).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "quote_indent", TextEditorElement.class,
                        consumes -> (int)consumes.getElement().markdownRenderer.getQuoteIndent(),
                        (textEditorElement, integer) -> textEditorElement.getElement().markdownRenderer.setQuoteIndent(integer),
                        Component.translatable("fancymenu.customization.items.text.markdown.quote.indent"), true, 8, null, null)
                .setStackable(true);

        this.addToggleContextMenuEntryTo(markdownMenu, "quote_italic", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.isQuoteItalic(),
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().markdownRenderer.setQuoteItalic(aBoolean),
                        "fancymenu.customization.items.text.markdown.quote.italic")
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_quote");

        this.addStringInputContextMenuEntryTo(markdownMenu, "bullet_list_dot_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getBulletListDotColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setBulletListDotColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.bullet_list.dot.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "bullet_list_indent", TextEditorElement.class,
                        consumes -> (int)consumes.getElement().markdownRenderer.getBulletListIndent(),
                        (textEditorElement, integer) -> textEditorElement.getElement().markdownRenderer.setBulletListIndent(integer),
                        Component.translatable("fancymenu.customization.items.text.markdown.bullet_list.indent"), true, 8, null, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "bullet_list_spacing", TextEditorElement.class,
                        consumes -> (int)consumes.getElement().markdownRenderer.getBulletListSpacing(),
                        (textEditorElement, integer) -> textEditorElement.getElement().markdownRenderer.setBulletListSpacing(integer),
                        Component.translatable("fancymenu.customization.items.text.markdown.bullet_list.spacing"), true, 3, null, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_bullet_list");

        // Table customization submenu
        ContextMenu tableMenu = new ContextMenu();
        markdownMenu.addSubMenuEntry("tables", Component.translatable("fancymenu.customization.items.text.markdown.tables"), tableMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.markdown.tables.desc")))
                .setStackable(true);

        this.addToggleContextMenuEntryTo(tableMenu, "table_show_header", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.isTableShowHeader(),
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().markdownRenderer.setTableShowHeader(aBoolean),
                        "fancymenu.customization.items.text.markdown.tables.show_header")
                .setStackable(true);

        this.addToggleContextMenuEntryTo(tableMenu, "table_alternate_row_colors", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.isTableAlternateRowColors(),
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().markdownRenderer.setTableAlternateRowColors(aBoolean),
                        "fancymenu.customization.items.text.markdown.tables.alternate_row_colors")
                .setStackable(true);

        tableMenu.addSeparatorEntry("separator_after_table_toggles");

        this.addStringInputContextMenuEntryTo(tableMenu, "table_line_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getTableLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setTableLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.tables.line_color"),
                        true, DrawableColor.of(120, 120, 120).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_header_background_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getTableHeaderBackgroundColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setTableHeaderBackgroundColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.tables.header_background_color"),
                        true, DrawableColor.of(50, 50, 50).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_row_background_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getTableRowBackgroundColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setTableRowBackgroundColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.tables.row_background_color"),
                        true, DrawableColor.of(40, 40, 40).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(tableMenu, "table_alternate_row_color", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getTableAlternateRowColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getElement().markdownRenderer.setTableAlternateRowColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.tables.alternate_row_color"),
                        true, DrawableColor.of(60, 60, 60).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.markdown.tables.alternate_row_color.desc")));

        tableMenu.addSeparatorEntry("separator_after_table_colors");

        this.addFloatInputContextMenuEntryTo(tableMenu, "table_line_thickness", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getTableLineThickness(),
                        (textEditorElement, aFloat) -> textEditorElement.getElement().markdownRenderer.setTableLineThickness(Math.max(0.5F, aFloat)),
                        Component.translatable("fancymenu.customization.items.text.markdown.tables.line_thickness"), true, 1.0F, null, null)
                .setStackable(true);

        this.addFloatInputContextMenuEntryTo(tableMenu, "table_cell_padding", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getTableCellPadding(),
                        (textEditorElement, aFloat) -> textEditorElement.getElement().markdownRenderer.setTableCellPadding(Math.max(0.0F, aFloat)),
                        Component.translatable("fancymenu.customization.items.text.markdown.tables.cell_padding"), true, 8.0F, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.markdown.tables.cell_padding.desc")));

        this.addFloatInputContextMenuEntryTo(tableMenu, "table_margin", TextEditorElement.class,
                        consumes -> consumes.getElement().markdownRenderer.getTableMargin(),
                        (textEditorElement, aFloat) -> textEditorElement.getElement().markdownRenderer.setTableMargin(Math.max(0.0F, aFloat)),
                        Component.translatable("fancymenu.customization.items.text.markdown.tables.margin"), true, 4.0F, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.markdown.tables.margin.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_markdown");

        ContextMenu grabberTextureMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_texture", Component.translatable("fancymenu.customization.items.text.scroll_grabber_texture"), grabberTextureMenu)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("image"));

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "vertical_normal_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.getElement().verticalScrollGrabberTextureNormal,
                (element, supplier) -> element.getElement().verticalScrollGrabberTextureNormal = supplier,
                Component.translatable("fancymenu.customization.items.text.vertical_scroll_grabber_texture.normal"),
                true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "vertical_hover_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.getElement().verticalScrollGrabberTextureHover,
                (element, supplier) -> element.getElement().verticalScrollGrabberTextureHover = supplier,
                Component.translatable("fancymenu.customization.items.text.vertical_scroll_grabber_texture.hover"),
                true, null, true, true, true);

        grabberTextureMenu.addSeparatorEntry("separator_after_vertical_textures");

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_normal_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.getElement().horizontalScrollGrabberTextureNormal,
                (element, supplier) -> element.getElement().horizontalScrollGrabberTextureNormal = supplier,
                Component.translatable("fancymenu.customization.items.text.horizontal_scroll_grabber_texture.normal"),
                true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_hover_grabber_texture",
                TextEditorElement.class,
                null,
                consumes -> consumes.getElement().horizontalScrollGrabberTextureHover,
                (element, supplier) -> element.getElement().horizontalScrollGrabberTextureHover = supplier,
                Component.translatable("fancymenu.customization.items.text.horizontal_scroll_grabber_texture.hover"),
                true, null, true, true, true);

        ContextMenu grabberColorMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_color", Component.translatable("fancymenu.customization.items.text.scroll_grabber_color"), grabberColorMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.scroll_grabber_color.desc")));

        this.addGenericStringInputContextMenuEntryTo(grabberColorMenu, "normal_grabber_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).scrollGrabberColorHexNormal,
                        (element, colorHex) -> ((TextElement)element.element).scrollGrabberColorHexNormal = colorHex,
                        null, false, false, Component.translatable("fancymenu.customization.items.text.scroll_grabber_color.normal"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(grabberColorMenu, "hover_grabber_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).scrollGrabberColorHexHover,
                        (element, colorHex) -> ((TextElement)element.element).scrollGrabberColorHexHover = colorHex,
                        null, false, false, Component.translatable("fancymenu.customization.items.text.scroll_grabber_color.hover"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_hover_grabber_color");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "interactable", TextEditorElement.class,
                        consumes -> consumes.getElement().interactable,
                        (textEditorElement, aBoolean) -> textEditorElement.getElement().interactable = aBoolean,
                        "fancymenu.elements.text.v2.interactable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.interactable.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.editor.children().contains(this.getElement().scrollArea)) {
            ((IMixinScreen)this.editor).getChildrenFancyMenu().add(this.getElement().scrollArea);
        }

        super.render(graphics, mouseX, mouseY, partial);

    }

    protected TextElement getElement() {
        return ((TextElement)this.element);
    }

}
