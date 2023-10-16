package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TextEditorElement extends AbstractEditorElement {

    public TextEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_mode",
                        ListUtils.of(TextElement.SourceMode.DIRECT, TextElement.SourceMode.LOCAL_SOURCE, TextElement.SourceMode.WEB_SOURCE),
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).sourceMode,
                        (element1, sourceMode) -> ((TextElement)element1.element).setSource(sourceMode, ""),
                        (menu, entry, switcherValue) -> {
                            if (switcherValue == TextElement.SourceMode.LOCAL_SOURCE) {
                                return Component.translatable("fancymenu.customization.items.text.source_mode.mode.local");
                            }
                            if (switcherValue == TextElement.SourceMode.WEB_SOURCE) {
                                return Component.translatable("fancymenu.customization.items.text.source_mode.mode.web");
                            }
                            return Component.translatable("fancymenu.customization.items.text.source_mode.mode.direct");
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.source_mode.desc")));

        this.rightClickMenu.addClickableEntry("set_source", Component.literal(""), (menu, entry) ->
                {
                    if (this.getTextElement().sourceMode == TextElement.SourceMode.LOCAL_SOURCE) {
                        ChooseFileScreen s = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, LayoutHandler.ASSETS_DIR, call -> {
                            if (call != null) {
                                this.editor.history.saveSnapshot();
                                this.getTextElement().setSource(TextElement.SourceMode.LOCAL_SOURCE, ScreenCustomization.getPathWithoutGameDirectory(call.getAbsolutePath()));
                            }
                            Minecraft.getInstance().setScreen(this.editor);
                        });
                        Minecraft.getInstance().setScreen(s);
                    }
                    if ((this.getTextElement().sourceMode == TextElement.SourceMode.DIRECT) || (this.getTextElement().sourceMode == TextElement.SourceMode.WEB_SOURCE)) {
                        TextEditorScreen s = new TextEditorScreen(entry.getLabel(), null, (call) -> {
                            if (call != null) {
                                this.editor.history.saveSnapshot();
                                this.getTextElement().setSource(this.getTextElement().sourceMode, call);
                            }
                            Minecraft.getInstance().setScreen(this.editor);
                        });
                        if (this.getTextElement().sourceMode != TextElement.SourceMode.DIRECT) {
                            s.setMultilineMode(false);
                        }
                        if (this.getTextElement().sourceMode == TextElement.SourceMode.WEB_SOURCE) {
                            s.setTextValidator(consumes -> TextValidators.BASIC_URL_TEXT_VALIDATOR.get(consumes.getText()));
                        }
                        if (this.getTextElement().source != null) {
                            if (this.getTextElement().sourceMode == TextElement.SourceMode.DIRECT) {
                                s.setText(this.getTextElement().source.replace("%n%", "\n"));
                            } else {
                                s.setText(this.getTextElement().source);
                            }
                        }
                        Minecraft.getInstance().setScreen(s);
                    }
                })
                .setTooltipSupplier((menu, entry) -> {
                    if (this.getTextElement().sourceMode == TextElement.SourceMode.LOCAL_SOURCE) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.set_source.local.desc"));
                    }
                    if (this.getTextElement().sourceMode == TextElement.SourceMode.WEB_SOURCE) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.set_source.web.desc"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.set_source.direct.desc"));
                })
                .setLabelSupplier((menu, entry) -> {
                    if (this.getTextElement().sourceMode == TextElement.SourceMode.LOCAL_SOURCE) {
                        return Component.translatable("fancymenu.customization.items.text.set_source.local");
                    }
                    if (this.getTextElement().sourceMode == TextElement.SourceMode.WEB_SOURCE) {
                        return Component.translatable("fancymenu.customization.items.text.set_source.web");
                    }
                    return Component.translatable("fancymenu.customization.items.text.set_source.direct");
                })
                .setIcon(ContextMenu.IconFactory.getIcon("text"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("text_separator_1");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "remove_html_breaks", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.isRemoveHtmlBreaks(),
                        (textEditorElement, aBoolean) -> textEditorElement.getTextElement().markdownRenderer.setRemoveHtmlBreaks(aBoolean),
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
                        consumes -> consumes.getTextElement().markdownRenderer.isTextShadow(),
                        (textEditorElement, aBoolean) -> textEditorElement.getTextElement().markdownRenderer.setTextShadow(aBoolean),
                        "fancymenu.customization.items.text.shadow")
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("shadow"));

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_base_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).markdownRenderer.getTextBaseColor().getHex(),
                        (element, colorHex) -> ((TextElement)element.element).markdownRenderer.setTextBaseColor(DrawableColor.of(colorHex)), null, false, false, Component.translatable("fancymenu.customization.items.text.base_color"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
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
                        consumes -> consumes.getTextElement().enableScrolling,
                        (textEditorElement, aBoolean) -> textEditorElement.getTextElement().enableScrolling = aBoolean,
                        "fancymenu.customization.items.text.scrolling")
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_line_wrapping", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.isAutoLineBreakingEnabled(),
                        (textEditorElement, aBoolean) -> textEditorElement.getTextElement().markdownRenderer.setAutoLineBreakingEnabled(aBoolean),
                        "fancymenu.customization.items.text.auto_line_wrapping")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_line_wrapping").setStackable(true);

        ContextMenu markdownMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("markdown", Component.translatable("fancymenu.customization.items.text.markdown"), markdownMenu).setStackable(true);

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_single_line_color", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.getCodeBlockSingleLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getTextElement().markdownRenderer.setCodeBlockSingleLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.code_block.single.color"),
                        true, DrawableColor.of(115, 115, 115).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(markdownMenu, "code_block_multi_line_color", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.getCodeBlockMultiLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getTextElement().markdownRenderer.setCodeBlockMultiLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.code_block.multi.color"),
                        true, DrawableColor.of(86, 86, 86).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_code_block");

        this.addStringInputContextMenuEntryTo(markdownMenu, "headline_line_color", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.getHeadlineUnderlineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getTextElement().markdownRenderer.setHeadlineLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.headline.line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_headline");

        this.addStringInputContextMenuEntryTo(markdownMenu, "separation_line_color", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.getSeparationLineColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getTextElement().markdownRenderer.setSeparationLineColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.separation_line.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_separation_line");

        this.addStringInputContextMenuEntryTo(markdownMenu, "hyperlink_color", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.getHyperlinkColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getTextElement().markdownRenderer.setHyperlinkColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.hyperlink.color"),
                        true, DrawableColor.of(7, 113, 252).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_hyperlink");

        this.addStringInputContextMenuEntryTo(markdownMenu, "quote_color", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.getQuoteColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getTextElement().markdownRenderer.setQuoteColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.quote.color"),
                        true, DrawableColor.of(129, 129, 129).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "quote_indent", TextEditorElement.class,
                        consumes -> (int)consumes.getTextElement().markdownRenderer.getQuoteIndent(),
                        (textEditorElement, integer) -> textEditorElement.getTextElement().markdownRenderer.setQuoteIndent(integer),
                        Component.translatable("fancymenu.customization.items.text.markdown.quote.indent"), true, 8, null, null)
                .setStackable(true);

        this.addToggleContextMenuEntryTo(markdownMenu, "quote_italic", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.isQuoteItalic(),
                        (textEditorElement, aBoolean) -> textEditorElement.getTextElement().markdownRenderer.setQuoteItalic(aBoolean),
                        "fancymenu.customization.items.text.markdown.quote.italic")
                .setStackable(true);

        markdownMenu.addSeparatorEntry("separator_after_quote");

        this.addStringInputContextMenuEntryTo(markdownMenu, "bullet_list_dot_color", TextEditorElement.class,
                        consumes -> consumes.getTextElement().markdownRenderer.getBulletListDotColor().getHex(),
                        (textEditorElement, s) -> textEditorElement.getTextElement().markdownRenderer.setBulletListDotColor(DrawableColor.of(s)),
                        null, false, false, Component.translatable("fancymenu.customization.items.text.markdown.bullet_list.dot.color"),
                        true, DrawableColor.of(169, 169, 169).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "bullet_list_indent", TextEditorElement.class,
                        consumes -> (int)consumes.getTextElement().markdownRenderer.getBulletListIndent(),
                        (textEditorElement, integer) -> textEditorElement.getTextElement().markdownRenderer.setBulletListIndent(integer),
                        Component.translatable("fancymenu.customization.items.text.markdown.bullet_list.indent"), true, 8, null, null)
                .setStackable(true);

        this.addIntegerInputContextMenuEntryTo(markdownMenu, "bullet_list_spacing", TextEditorElement.class,
                        consumes -> (int)consumes.getTextElement().markdownRenderer.getBulletListSpacing(),
                        (textEditorElement, integer) -> textEditorElement.getTextElement().markdownRenderer.setBulletListSpacing(integer),
                        Component.translatable("fancymenu.customization.items.text.markdown.bullet_list.spacing"), true, 3, null, null)
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_markdown");

        ContextMenu grabberTextureMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_texture", Component.translatable("fancymenu.customization.items.text.scroll_grabber_texture"), grabberTextureMenu)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("image"));

        this.addGenericFileChooserContextMenuEntryTo(grabberTextureMenu, "vertical_normal_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).verticalScrollGrabberTextureNormal,
                        (element1, s) -> ((TextElement)element1.element).verticalScrollGrabberTextureNormal = s,
                        Component.translatable("fancymenu.customization.items.text.vertical_scroll_grabber_texture.normal"),
                        true,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        this.addGenericFileChooserContextMenuEntryTo(grabberTextureMenu, "vertical_hover_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).verticalScrollGrabberTextureHover,
                        (element1, s) -> ((TextElement)element1.element).verticalScrollGrabberTextureHover = s,
                        Component.translatable("fancymenu.customization.items.text.vertical_scroll_grabber_texture.hover"),
                        true,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        grabberTextureMenu.addSeparatorEntry("separator_after_vertical_textures");

        this.addGenericFileChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_normal_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).horizontalScrollGrabberTextureNormal,
                        (element1, s) -> ((TextElement)element1.element).horizontalScrollGrabberTextureNormal = s,
                        Component.translatable("fancymenu.customization.items.text.horizontal_scroll_grabber_texture.normal"),
                        true,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        this.addGenericFileChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_hover_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).horizontalScrollGrabberTextureHover,
                        (element1, s) -> ((TextElement)element1.element).horizontalScrollGrabberTextureHover = s,
                        Component.translatable("fancymenu.customization.items.text.horizontal_scroll_grabber_texture.hover"),
                        true,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

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

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.editor.children().contains(this.getTextElement().scrollArea)) {
            ((IMixinScreen)this.editor).getChildrenFancyMenu().add(this.getTextElement().scrollArea);
        }

        super.render(pose, mouseX, mouseY, partial);

    }

    protected TextElement getTextElement() {
        return ((TextElement)this.element);
    }

}
