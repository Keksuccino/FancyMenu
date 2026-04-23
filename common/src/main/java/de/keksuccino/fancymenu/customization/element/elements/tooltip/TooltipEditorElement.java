package de.keksuccino.fancymenu.customization.element.elements.tooltip;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.editor.EditorElementSettings;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class TooltipEditorElement extends AbstractEditorElement<TooltipEditorElement, TooltipElement> {

    public TooltipEditorElement(@NotNull TooltipElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor, new EditorElementSettings() {{
            this.setResizeableX(true);
            this.setResizeableY(false);
            this.setParallaxAllowed(false);
            this.setAutoSizingAllowed(false);
            this.setStretchable(false);
            this.setAdvancedSizingSupported(false);
            this.setDelayable(false);
            this.setFadeable(false);
            this.setStayOnScreenAllowed(false);
            this.setOpacityChangeable(false);
        }});
    }

    @Override
    public void init() {

        super.init();

        TooltipElement tooltipElement = (TooltipElement) this.element;

        // Content source submenu
        ContextMenu contentMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("content", Component.translatable("fancymenu.elements.tooltip.content"), contentMenu)
                .setIcon(MaterialIcons.TEXT_FIELDS);

        // Source mode selector - using Text element localization
        this.addCycleContextMenuEntryTo(contentMenu, "source_mode",
                        List.of(TooltipElement.SourceMode.DIRECT, TooltipElement.SourceMode.RESOURCE),
                        TooltipEditorElement.class,
                        e -> e.element.sourceMode,
                        (e, mode) -> e.element.setSource(mode, null),
                        (menu, entry, value) -> value.getCycleComponent())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.v2.source_mode.desc")))
                .setIcon(MaterialIcons.TUNE);

        // Text input for direct mode - using Text element localization
        contentMenu.addClickableEntry("set_text", Component.translatable("fancymenu.elements.text.v2.source.input"),
                        (menu, entry) -> {
                            if (entry.getStackMeta().isFirstInStack()) {
                                List<TooltipEditorElement> selectedElements = this.getFilteredSelectedElementList(e -> e instanceof TooltipEditorElement);
                                String defaultText = tooltipElement.source;
                                TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable("fancymenu.elements.text.v2.source.input"), null, (call) -> {
                                    if (call != null) {
                                        this.editor.history.saveSnapshot();
                                        for (AbstractEditorElement<?, ?> e : selectedElements) {
                                            ((TooltipElement)e.element).setSource(TooltipElement.SourceMode.DIRECT, call);
                                        }
                                    }
                                });
                                s.setText(defaultText);
                                s.setMultilineMode(true);
                                s.setPlaceholdersAllowed(true);
                                menu.closeMenuChain();
                                Dialogs.openGeneric(s, Component.translatable("fancymenu.elements.text.v2.source.input"), null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                            }
                        })
                .setStackable(true)
                .setIsActiveSupplier((menu, entry) -> tooltipElement.sourceMode == TooltipElement.SourceMode.DIRECT)
                .setIcon(MaterialIcons.TEXT_FIELDS);

        // Resource file input for resource mode - using Text element localization
        this.addTextResourceChooserContextMenuEntryTo(contentMenu, "set_resource",
                        TooltipEditorElement.class,
                        null,
                        e -> ((TooltipElement)e.element).textResourceSupplier,
                        (e, supplier) -> {
                            TooltipElement el = (TooltipElement)e.element;
                            el.textResourceSupplier = supplier;
                            el.setSource(TooltipElement.SourceMode.RESOURCE, supplier != null ? supplier.getSourceWithPrefix() : null);
                        },
                        Component.translatable("fancymenu.elements.text.v2.source.choose"),
                        true, null, true, true, true)
                .setIsActiveSupplier((menu, entry) -> tooltipElement.sourceMode == TooltipElement.SourceMode.RESOURCE)
                .setIcon(MaterialIcons.FILE_OPEN);

        // Markdown submenu - using Text element localization
        ContextMenu markdownMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("markdown", Component.translatable("fancymenu.elements.text.markdown"), markdownMenu)
                .setIcon(MaterialIcons.MENU_BOOK);

        // Text shadow - using Text element localization
        this.addToggleContextMenuEntryTo(markdownMenu, "text_shadow",
                TooltipEditorElement.class,
                e -> ((TooltipElement)e.element).markdownRenderer.isTextShadow(),
                (e, value) -> ((TooltipElement)e.element).markdownRenderer.setTextShadow(value),
                "fancymenu.elements.text.shadow")
                .setIcon(MaterialIcons.SHADOW);

        // Text case mode - using Text element localization
        this.addCycleContextMenuEntryTo(markdownMenu, "text_case",
                List.of(MarkdownRenderer.TextCase.NORMAL, MarkdownRenderer.TextCase.ALL_LOWER, MarkdownRenderer.TextCase.ALL_UPPER),
                TooltipEditorElement.class,
                e -> ((TooltipElement)e.element).markdownRenderer.getTextCase(),
                (e, caseMode) -> ((TooltipElement)e.element).markdownRenderer.setTextCase(caseMode),
                (menu, entry, value) -> {
                    if (value == MarkdownRenderer.TextCase.NORMAL) {
                        return Component.translatable("fancymenu.elements.text.case_mode.normal");
                    }
                    if (value == MarkdownRenderer.TextCase.ALL_LOWER) {
                        return Component.translatable("fancymenu.elements.text.case_mode.lower");
                    }
                    return Component.translatable("fancymenu.elements.text.case_mode.upper");
                })
                .setIcon(MaterialIcons.TEXT_FORMAT);

        // Text scale - using Text element localization
        this.element.textScale.buildContextMenuEntryAndAddTo(markdownMenu, this)
                .setIcon(MaterialIcons.FORMAT_SIZE);

        // Text color - using Text element localization
        this.addStringInputContextMenuEntryTo(markdownMenu, "text_color",
                TooltipEditorElement.class,
                e -> ((TooltipElement)e.element).markdownRenderer.getTextBaseColor().getHex(),
                (e, color) -> ((TooltipElement)e.element).markdownRenderer.setTextBaseColor(DrawableColor.of(color)),
                null, false, false,
                Component.translatable("fancymenu.elements.text.base_color"),
                true, "#FFFFFF", null, null)
                .setIcon(MaterialIcons.PALETTE);

        // Text border - using Text element localization
        this.element.textBorder.buildContextMenuEntryAndAddTo(markdownMenu, this)
                .setIcon(MaterialIcons.BORDER_OUTER);

        // Line spacing - using Text element localization
        this.element.lineSpacing.buildContextMenuEntryAndAddTo(markdownMenu, this)
                .setIcon(MaterialIcons.FORMAT_LINE_SPACING);

        // Auto line wrapping - using Text element localization
        this.addToggleContextMenuEntryTo(markdownMenu, "auto_line_wrapping",
                TooltipEditorElement.class,
                e -> ((TooltipElement)e.element).markdownRenderer.isAutoLineBreakingEnabled(),
                (e, value) -> ((TooltipElement)e.element).markdownRenderer.setAutoLineBreakingEnabled(value),
                "fancymenu.elements.text.auto_line_wrapping")
                .setIcon(MaterialIcons.FORMAT_TEXT_WRAP);

        // Parse markdown toggle - using Text element localization
        this.addToggleContextMenuEntryTo(markdownMenu, "parse_markdown",
                TooltipEditorElement.class,
                e -> ((TooltipElement)e.element).markdownRenderer.isParseMarkdown(),
                (e, value) -> ((TooltipElement)e.element).markdownRenderer.setParseMarkdown(value),
                "fancymenu.elements.text.markdown.toggle")
                .setIcon(MaterialIcons.CODE);

        this.rightClickMenu.addSeparatorEntry("separator_after_markdown");

        // Background sub menu
        ContextMenu backgroundMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("background", Component.translatable("fancymenu.elements.tooltip.background"), backgroundMenu)
                .setIcon(MaterialIcons.IMAGE);

        // Background texture
        this.addImageResourceChooserContextMenuEntryTo(backgroundMenu, "background_texture",
                        TooltipEditorElement.class,
                        null,
                        e -> ((TooltipElement)e.element).backgroundTexture,
                        (e, texture) -> ((TooltipElement)e.element).backgroundTexture = texture,
                        Component.translatable("fancymenu.elements.tooltip.background_texture"),
                        true, null, true, true, true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.tooltip.background_texture.desc")))
                .setIcon(MaterialIcons.IMAGE);

        // Nine-slice submenu
        ContextMenu nineSliceMenu = new ContextMenu();
        backgroundMenu.addSubMenuEntry("nine_slice", Component.translatable("fancymenu.elements.tooltip.nine_slice"), nineSliceMenu)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.tooltip.nine_slice.desc")))
                .setIcon(MaterialIcons.GRID_GUIDES);

        // Nine-slice border top
        this.element.nineSliceBorderTop.buildContextMenuEntryAndAddTo(nineSliceMenu, this)
                .setIcon(MaterialIcons.BORDER_TOP);

        // Nine-slice border right
        this.element.nineSliceBorderRight.buildContextMenuEntryAndAddTo(nineSliceMenu, this)
                .setIcon(MaterialIcons.BORDER_RIGHT);

        // Nine-slice border bottom
        this.element.nineSliceBorderBottom.buildContextMenuEntryAndAddTo(nineSliceMenu, this)
                .setIcon(MaterialIcons.BORDER_BOTTOM);

        // Nine-slice border left
        this.element.nineSliceBorderLeft.buildContextMenuEntryAndAddTo(nineSliceMenu, this)
                .setIcon(MaterialIcons.BORDER_LEFT);

        // Mouse following toggle
        this.addToggleContextMenuEntryTo(this.rightClickMenu, "mouse_following",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).mouseFollowing,
                        (e, value) -> ((TooltipElement)e.element).mouseFollowing = value,
                        "fancymenu.elements.tooltip.mouse_following")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.tooltip.mouse_following.desc")))
                .setIcon(MaterialIcons.MOUSE);

    }

}
