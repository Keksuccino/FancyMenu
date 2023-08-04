package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorScreen;
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

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_mode",
                        ListUtils.build(TextElement.SourceMode.DIRECT, TextElement.SourceMode.LOCAL_SOURCE, TextElement.SourceMode.WEB_SOURCE),
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
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("text_separator_1");

//        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_case_mode",
//                ListUtils.build(TextElement.CaseMode.NORMAL, TextElement.CaseMode.ALL_LOWER, TextElement.CaseMode.ALL_UPPER),
//                consumes -> (consumes instanceof TextEditorElement),
//                consumes -> ((TextElement)consumes.element).caseMode,
//                (element1, caseMode) -> {
//                    ((TextElement)element1.element).caseMode = caseMode;
//                    ((TextElement)element1.element).updateContent();
//                },
//                (menu, entry, switcherValue) -> {
//                    if (switcherValue == TextElement.CaseMode.NORMAL) {
//                        return Component.translatable("fancymenu.customization.items.text.case_mode.normal");
//                    }
//                    if (switcherValue == TextElement.CaseMode.ALL_LOWER) {
//                        return Component.translatable("fancymenu.customization.items.text.case_mode.lower");
//                    }
//                    return Component.translatable("fancymenu.customization.items.text.case_mode.upper");
//                });

//        this.addFloatInputContextMenuEntryTo(this.rightClickMenu, "set_scale",
//                        consumes -> (consumes instanceof TextEditorElement),
//                        consumes -> ((TextElement)consumes.element).scale,
//                        (element1, aFloat) -> {
//                            ((TextElement)element1.element).scale = Math.min(0.2F, aFloat);
//                            ((TextElement)element1.element).updateContent();
//                        },
//                        Component.translatable("fancymenu.customization.items.text.scale"),
//                        true, 1.0F, null, null)
//                .setStackable(true);

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "set_shadow",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).markdownRenderer.isTextShadow(),
                        (element1, aBoolean) -> ((TextElement)element1.element).markdownRenderer.setTextShadow(aBoolean),
                        "fancymenu.customization.items.text.shadow")
                .setStackable(true);

//        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_alignment",
//                        ListUtils.build(AbstractElement.Alignment.LEFT, AbstractElement.Alignment.CENTERED, AbstractElement.Alignment.RIGHT),
//                        consumes -> (consumes instanceof TextEditorElement),
//                        consumes -> ((TextElement)consumes.element).alignment,
//                        (element1, alignment) -> {
//                            ((TextElement)element1.element).alignment = alignment;
//                            ((TextElement)element1.element).updateContent();
//                        },
//                        (menu, entry, switcherValue) -> {
//                            if (switcherValue == AbstractElement.Alignment.LEFT) {
//                                return Component.translatable("fancymenu.customization.items.text.alignment.left");
//                            }
//                            if (switcherValue == AbstractElement.Alignment.CENTERED) {
//                                return Component.translatable("fancymenu.customization.items.text.alignment.center");
//                            }
//                            return Component.translatable("fancymenu.customization.items.text.alignment.right");
//                        })
//                .setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_base_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).markdownRenderer.getTextBaseColor().getHex(),
                        (element, colorHex) -> ((TextElement)element.element).markdownRenderer.setTextBaseColor(DrawableColor.of(colorHex)), null, false, false, Component.translatable("fancymenu.customization.items.text.base_color"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.base_color.desc")));

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_border",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> (int)((TextElement)consumes.element).markdownRenderer.getBorder(),
                        (element, border) -> ((TextElement)element.element).markdownRenderer.setBorder(Math.max(0, border)),
                        Component.translatable("fancymenu.customization.items.text.text_border"),
                        true, 10, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.text_border.desc")));

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_line_spacing",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> (int)((TextElement)consumes.element).markdownRenderer.getLineSpacing(),
                        (element, border) -> {
                            ((TextElement)element.element).markdownRenderer.setLineSpacing(Math.max(0, border));
                        },
                        Component.translatable("fancymenu.customization.items.text.line_spacing"),
                        true, 10, null, null)
                .setStackable(true);

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "set_scrolling",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).enableScrolling,
                        (element1, aBoolean) -> ((TextElement)element1.element).enableScrolling = aBoolean,
                        "fancymenu.customization.items.text.scrolling")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("text_separator_2").setStackable(true);

        ContextMenu grabberTextureMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_texture", Component.translatable("fancymenu.customization.items.text.scroll_grabber_texture"), grabberTextureMenu)
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(grabberTextureMenu, "vertical_normal_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).verticalScrollGrabberTextureNormal,
                        (element1, s) -> ((TextElement)element1.element).verticalScrollGrabberTextureNormal = s,
                        Component.translatable("fancymenu.customization.items.text.vertical_scroll_grabber_texture.normal"),
                        true,
                        FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(grabberTextureMenu, "vertical_hover_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).verticalScrollGrabberTextureHover,
                        (element1, s) -> ((TextElement)element1.element).verticalScrollGrabberTextureHover = s,
                        Component.translatable("fancymenu.customization.items.text.vertical_scroll_grabber_texture.hover"),
                        true,
                        FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true);

        grabberTextureMenu.addSeparatorEntry("separator_after_vertical_textures");

        this.addFileChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_normal_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).horizontalScrollGrabberTextureNormal,
                        (element1, s) -> ((TextElement)element1.element).horizontalScrollGrabberTextureNormal = s,
                        Component.translatable("fancymenu.customization.items.text.horizontal_scroll_grabber_texture.normal"),
                        true,
                        FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(grabberTextureMenu, "horizontal_hover_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElement),
                        null,
                        consumes -> ((TextElement)consumes.element).horizontalScrollGrabberTextureHover,
                        (element1, s) -> ((TextElement)element1.element).horizontalScrollGrabberTextureHover = s,
                        Component.translatable("fancymenu.customization.items.text.horizontal_scroll_grabber_texture.hover"),
                        true,
                        FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true);

        ContextMenu grabberColorMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_color", Component.translatable("fancymenu.customization.items.text.scroll_grabber_color"), grabberColorMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.scroll_grabber_color.desc")));

        this.addStringInputContextMenuEntryTo(grabberColorMenu, "normal_grabber_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).scrollGrabberColorHexNormal,
                        (element, colorHex) -> ((TextElement)element.element).scrollGrabberColorHexNormal = colorHex,
                        null, false, false, Component.translatable("fancymenu.customization.items.text.scroll_grabber_color.normal"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(grabberColorMenu, "hover_grabber_color",
                        consumes -> (consumes instanceof TextEditorElement),
                        consumes -> ((TextElement)consumes.element).scrollGrabberColorHexHover,
                        (element, colorHex) -> ((TextElement)element.element).scrollGrabberColorHexHover = colorHex,
                        null, false, false, Component.translatable("fancymenu.customization.items.text.scroll_grabber_color.hover"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

    }

    protected TextElement getTextElement() {
        return ((TextElement)this.element);
    }

}
