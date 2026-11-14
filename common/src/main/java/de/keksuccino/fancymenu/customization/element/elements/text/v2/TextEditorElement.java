package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettingsMenu;
import de.keksuccino.fancymenu.customization.element.style.ElementStyleMenus;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
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
                        (element, supplier) -> element.element.setSource(TextElement.SourceMode.RESOURCE, (supplier != null) ? supplier.getSourceWithPrefix() : null),
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

        this.element.markdownSettings.textScale.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.FORMAT_SIZE)
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_line_wrapping", TextEditorElement.class,
                        consumes -> consumes.element.markdownRenderer.isAutoLineBreakingEnabled(),
                        (textEditorElement, aBoolean) -> textEditorElement.element.markdownRenderer.setAutoLineBreakingEnabled(aBoolean),
                        "fancymenu.elements.text.auto_line_wrapping")
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_TEXT_WRAP);

        this.rightClickMenu.addSeparatorEntry("separator_after_line_wrapping").setStackable(true);

        MarkdownSettingsMenu.addTo(this.rightClickMenu, this, editorElement -> editorElement.element.markdownSettings);

        ElementStyleMenus.addPanelBackgroundMenu(this.rightClickMenu, this, this.element.background);

        this.rightClickMenu.addSeparatorEntry("separator_after_markdown");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_scrolling", TextEditorElement.class,
                        consumes -> consumes.element.enableScrolling,
                        (textEditorElement, aBoolean) -> textEditorElement.element.enableScrolling = aBoolean,
                        "fancymenu.elements.text.scrolling")
                .setStackable(true)
                .setIcon(MaterialIcons.SWIPE_VERTICAL);

        this.rightClickMenu.addSeparatorEntry("separator_after_scrolling");

        this.element.scrollGrabberDesign.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.scroll_grabber_design.desc")))
                .setIcon(MaterialIcons.DRAG_INDICATOR);

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

        ElementStyleMenus.addScrollBarBackgroundMenu(this.rightClickMenu, this, this.element.scrollBarBackground);

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

        if (!this.editor.children().contains(this.element.getScrollAreaInput())) {
            ((IMixinScreen)this.editor).getChildrenFancyMenu().add(this.element.getScrollAreaInput());
        }

        super.render(graphics, mouseX, mouseY, partial);

    }

}
