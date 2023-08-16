
package de.keksuccino.fancymenu.customization.element.elements.text.v1;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TextEditorElementOLD extends AbstractEditorElement {

    public TextEditorElementOLD(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_mode",
                        ListUtils.build(TextElementOLD.SourceMode.DIRECT, TextElementOLD.SourceMode.LOCAL_SOURCE, TextElementOLD.SourceMode.WEB_SOURCE),
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).sourceMode,
                        (element1, sourceMode) -> {
                            ((TextElementOLD)element1.element).sourceMode = sourceMode;
                            ((TextElementOLD)element1.element).source = null;
                            ((TextElementOLD)element1.element).updateContent();
                        },
                        (menu, entry, switcherValue) -> {
                            if (switcherValue == TextElementOLD.SourceMode.LOCAL_SOURCE) {
                                return Component.translatable("fancymenu.customization.items.text.source_mode.mode.local");
                            }
                            if (switcherValue == TextElementOLD.SourceMode.WEB_SOURCE) {
                                return Component.translatable("fancymenu.customization.items.text.source_mode.mode.web");
                            }
                            return Component.translatable("fancymenu.customization.items.text.source_mode.mode.direct");
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.source_mode.desc")));

        this.rightClickMenu.addClickableEntry("set_source", Component.literal(""), (menu, entry) ->
                {
                    if (this.getTextElement().sourceMode == TextElementOLD.SourceMode.LOCAL_SOURCE) {
                        ChooseFileScreen s = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, LayoutHandler.ASSETS_DIR, call -> {
                            if (call != null) {
                                this.editor.history.saveSnapshot();
                                this.getTextElement().source = ScreenCustomization.getPathWithoutGameDirectory(call.getAbsolutePath());
                                this.getTextElement().updateContent();
                            }
                            Minecraft.getInstance().setScreen(this.editor);
                        });
                        Minecraft.getInstance().setScreen(s);
                    }
                    if ((this.getTextElement().sourceMode == TextElementOLD.SourceMode.DIRECT) || (this.getTextElement().sourceMode == TextElementOLD.SourceMode.WEB_SOURCE)) {
                        TextEditorScreen s = new TextEditorScreen(entry.getLabel(), null, (call) -> {
                            if (call != null) {
                                this.editor.history.saveSnapshot();
                                call = call.replace("\n", "%n%");
                                if (call.length() == 0) {
                                    this.getTextElement().source = null;
                                } else {
                                    this.getTextElement().source = call;
                                }
                                this.getTextElement().updateContent();
                            }
                            Minecraft.getInstance().setScreen(this.editor);
                        });
                        if (this.getTextElement().sourceMode != TextElementOLD.SourceMode.DIRECT) {
                            s.setMultilineMode(false);
                        }
                        if (this.getTextElement().source != null) {
                            if (this.getTextElement().sourceMode == TextElementOLD.SourceMode.DIRECT) {
                                s.setText(this.getTextElement().source.replace("%n%", "\n").replace("\\n", "\n"));
                            } else {
                                s.setText(this.getTextElement().source);
                            }
                        }
                        Minecraft.getInstance().setScreen(s);
                    }
                })
                .setTooltipSupplier((menu, entry) -> {
                    if (this.getTextElement().sourceMode == TextElementOLD.SourceMode.LOCAL_SOURCE) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.set_source.local.desc"));
                    }
                    if (this.getTextElement().sourceMode == TextElementOLD.SourceMode.WEB_SOURCE) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.set_source.web.desc"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.set_source.direct.desc"));
                })
                .setLabelSupplier((menu, entry) -> {
                    if (this.getTextElement().sourceMode == TextElementOLD.SourceMode.LOCAL_SOURCE) {
                        return Component.translatable("fancymenu.customization.items.text.set_source.local");
                    }
                    if (this.getTextElement().sourceMode == TextElementOLD.SourceMode.WEB_SOURCE) {
                        return Component.translatable("fancymenu.customization.items.text.set_source.web");
                    }
                    return Component.translatable("fancymenu.customization.items.text.set_source.direct");
                });

        this.rightClickMenu.addSeparatorEntry("text_separator_1");

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_case_mode",
                ListUtils.build(TextElementOLD.CaseMode.NORMAL, TextElementOLD.CaseMode.ALL_LOWER, TextElementOLD.CaseMode.ALL_UPPER),
                consumes -> (consumes instanceof TextEditorElementOLD),
                consumes -> ((TextElementOLD)consumes.element).caseMode,
                (element1, caseMode) -> {
                    ((TextElementOLD)element1.element).caseMode = caseMode;
                    ((TextElementOLD)element1.element).updateContent();
                },
                (menu, entry, switcherValue) -> {
                    if (switcherValue == TextElementOLD.CaseMode.NORMAL) {
                        return Component.translatable("fancymenu.customization.items.text.case_mode.normal");
                    }
                    if (switcherValue == TextElementOLD.CaseMode.ALL_LOWER) {
                        return Component.translatable("fancymenu.customization.items.text.case_mode.lower");
                    }
                    return Component.translatable("fancymenu.customization.items.text.case_mode.upper");
                });

        this.addGenericFloatInputContextMenuEntryTo(this.rightClickMenu, "set_scale",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).scale,
                        (element1, aFloat) -> {
                            ((TextElementOLD)element1.element).scale = Math.min(0.2F, aFloat);
                            ((TextElementOLD)element1.element).updateContent();
                        },
                        Component.translatable("fancymenu.customization.items.text.scale"),
                        true, 1.0F, null, null)
                .setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "set_shadow",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).shadow,
                        (element1, aBoolean) -> {
                            ((TextElementOLD)element1.element).shadow = aBoolean;
                            ((TextElementOLD)element1.element).updateContent();

                        },
                        "fancymenu.customization.items.text.shadow")
                .setStackable(true);

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_alignment",
                        ListUtils.build(AbstractElement.Alignment.LEFT, AbstractElement.Alignment.CENTERED, AbstractElement.Alignment.RIGHT),
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).alignment,
                        (element1, alignment) -> {
                            ((TextElementOLD)element1.element).alignment = alignment;
                            ((TextElementOLD)element1.element).updateContent();
                        },
                        (menu, entry, switcherValue) -> {
                            if (switcherValue == AbstractElement.Alignment.LEFT) {
                                return Component.translatable("fancymenu.customization.items.text.alignment.left");
                            }
                            if (switcherValue == AbstractElement.Alignment.CENTERED) {
                                return Component.translatable("fancymenu.customization.items.text.alignment.center");
                            }
                            return Component.translatable("fancymenu.customization.items.text.alignment.right");
                        })
                .setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_base_color",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).baseColorHex,
                        (element, colorHex) -> {
                            ((TextElementOLD)element.element).baseColorHex = colorHex;
                            ((TextElementOLD)element.element).updateContent();
                        }, null, false, false, Component.translatable("fancymenu.customization.items.text.base_color"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.base_color.desc")));

        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_border",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).textBorder,
                        (element, border) -> {
                            ((TextElementOLD)element.element).textBorder = Math.min(0, border);
                            ((TextElementOLD)element.element).updateContent();
                        },
                        Component.translatable("fancymenu.customization.items.text.text_border"),
                        true, 10, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.text_border.desc")));

        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_line_spacing",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).lineSpacing,
                        (element, border) -> {
                            ((TextElementOLD)element.element).lineSpacing = Math.min(0, border);
                            ((TextElementOLD)element.element).updateContent();
                        },
                        Component.translatable("fancymenu.customization.items.text.line_spacing"),
                        true, 10, null, null)
                .setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "set_scrolling",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).enableScrolling,
                        (element1, aBoolean) -> {
                            ((TextElementOLD)element1.element).enableScrolling = aBoolean;
                            ((TextElementOLD)element1.element).updateContent();
                        },
                        "fancymenu.customization.items.text.scrolling")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("text_separator_2").setStackable(true);

        ContextMenu grabberTextureMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_texture", Component.translatable("fancymenu.customization.items.text.scroll_grabber_texture"), grabberTextureMenu)
                .setStackable(true);

        this.addGenericFileChooserContextMenuEntryTo(grabberTextureMenu, "normal_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        null,
                        consumes -> ((TextElementOLD)consumes.element).scrollGrabberTextureNormal,
                        (element1, s) -> {
                            ((TextElementOLD)element1.element).scrollGrabberTextureNormal = s;
                            ((TextElementOLD)element1.element).updateContent();
                        },
                        Component.translatable("fancymenu.customization.items.text.scroll_grabber_texture.normal"),
                        true,
                        FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true);

        this.addGenericFileChooserContextMenuEntryTo(grabberTextureMenu, "hover_grabber_texture",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        null,
                        consumes -> ((TextElementOLD)consumes.element).scrollGrabberTextureHover,
                        (element1, s) -> {
                            ((TextElementOLD)element1.element).scrollGrabberTextureHover = s;
                            ((TextElementOLD)element1.element).updateContent();
                        },
                        Component.translatable("fancymenu.customization.items.text.scroll_grabber_texture.hover"),
                        true,
                        FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true);

        ContextMenu grabberColorMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("grabber_color", Component.translatable("fancymenu.customization.items.text.scroll_grabber_color"), grabberColorMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.scroll_grabber_color.desc")));

        this.addGenericStringInputContextMenuEntryTo(grabberColorMenu, "normal_grabber_color",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).scrollGrabberColorHexNormal,
                        (element, colorHex) -> {
                            ((TextElementOLD)element.element).scrollGrabberColorHexNormal = colorHex;
                            ((TextElementOLD)element.element).updateContent();
                        }, null, false, false, Component.translatable("fancymenu.customization.items.text.scroll_grabber_color.normal"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(grabberColorMenu, "hover_grabber_color",
                        consumes -> (consumes instanceof TextEditorElementOLD),
                        consumes -> ((TextElementOLD)consumes.element).scrollGrabberColorHexHover,
                        (element, colorHex) -> {
                            ((TextElementOLD)element.element).scrollGrabberColorHexHover = colorHex;
                            ((TextElementOLD)element.element).updateContent();
                        }, null, false, false, Component.translatable("fancymenu.customization.items.text.scroll_grabber_color.hover"),
                        true, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true);

    }

    protected TextElementOLD getTextElement() {
        return ((TextElementOLD)this.element);
    }

}
