package de.keksuccino.fancymenu.customization.element.elements.tooltip;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.editor.EditorElementSettings;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class TooltipEditorElement extends AbstractEditorElement {

    public TooltipEditorElement(@NotNull TooltipElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor, new EditorElementSettings() {{
            // Tooltips are not resizable
            this.setResizeable(false);
            this.setResizeableX(false);
            this.setResizeableY(false);
        }});
    }

    @Override
    public void init() {
        super.init();

        TooltipElement tooltipElement = (TooltipElement) this.element;

        // Content source submenu
        ContextMenu contentMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("content", Component.translatable("fancymenu.customization.items.tooltip.content"), contentMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("notes"));

        // Source mode selector - using Text element localization
        this.addCycleContextMenuEntryTo(contentMenu, "source_mode",
                        List.of(TooltipElement.SourceMode.DIRECT, TooltipElement.SourceMode.RESOURCE),
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).sourceMode,
                        (e, mode) -> ((TooltipElement)e.element).setSource(mode, ((TooltipElement)e.element).source),
                        (menu, entry, value) -> value.getCycleComponent())
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.elements.text.v2.source_mode.desc")));

        // Text input for direct mode - using Text element localization
        contentMenu.addClickableEntry("set_text", Component.translatable("fancymenu.elements.text.v2.source.input"), 
                (menu, entry) -> {
                    if (entry.getStackMeta().isFirstInStack()) {
                        List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(e -> e instanceof TooltipEditorElement);
                        String defaultText = tooltipElement.source;
                        TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.elements.text.v2.source.input"), null, (call) -> {
                            if (call != null) {
                                this.editor.history.saveSnapshot();
                                for (AbstractEditorElement e : selectedElements) {
                                    ((TooltipElement)e.element).setSource(TooltipElement.SourceMode.DIRECT, call);
                                }
                            }
                            Minecraft.getInstance().setScreen(this.editor);
                        });
                        s.setText(defaultText);
                        s.setMultilineMode(true);
                        s.setPlaceholdersAllowed(true);
                        Minecraft.getInstance().setScreen(s);
                    }
                })
                .setStackable(true)
                .setIsActiveSupplier((menu, entry) -> tooltipElement.sourceMode == TooltipElement.SourceMode.DIRECT)
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

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
                .setIsActiveSupplier((menu, entry) -> tooltipElement.sourceMode == TooltipElement.SourceMode.RESOURCE);

        this.rightClickMenu.addSeparatorEntry("separator_after_content");

        // Appearance submenu
        ContextMenu appearanceMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("appearance", Component.translatable("fancymenu.customization.items.tooltip.appearance"), appearanceMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("palette"));

        // Background texture
        this.addImageResourceChooserContextMenuEntryTo(appearanceMenu, "background_texture",
                        TooltipEditorElement.class,
                        null,
                        e -> ((TooltipElement)e.element).backgroundTexture,
                        (e, texture) -> ((TooltipElement)e.element).backgroundTexture = texture,
                        Component.translatable("fancymenu.customization.items.tooltip.background_texture"),
                        true, null, true, true, true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.customization.items.tooltip.background_texture.desc"),
                        Component.translatable("fancymenu.customization.items.tooltip.background_texture.nine_slice_info")));

        // Nine-slice submenu
        ContextMenu nineSliceMenu = new ContextMenu();
        appearanceMenu.addSubMenuEntry("nine_slice", Component.translatable("fancymenu.customization.items.tooltip.nine_slice"), nineSliceMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.customization.items.tooltip.nine_slice.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("measure"));

        // Nine-slice border top
        this.addIntegerInputContextMenuEntryTo(nineSliceMenu, "nine_slice_border_top",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).nineSliceBorderTop,
                        (e, value) -> ((TooltipElement)e.element).nineSliceBorderTop = value,
                        Component.translatable("fancymenu.customization.items.tooltip.nine_slice.border.top"),
                        true, 5, null, null);

        // Nine-slice border right
        this.addIntegerInputContextMenuEntryTo(nineSliceMenu, "nine_slice_border_right",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).nineSliceBorderRight,
                        (e, value) -> ((TooltipElement)e.element).nineSliceBorderRight = value,
                        Component.translatable("fancymenu.customization.items.tooltip.nine_slice.border.right"),
                        true, 5, null, null);

        // Nine-slice border bottom
        this.addIntegerInputContextMenuEntryTo(nineSliceMenu, "nine_slice_border_bottom",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).nineSliceBorderBottom,
                        (e, value) -> ((TooltipElement)e.element).nineSliceBorderBottom = value,
                        Component.translatable("fancymenu.customization.items.tooltip.nine_slice.border.bottom"),
                        true, 5, null, null);

        // Nine-slice border left
        this.addIntegerInputContextMenuEntryTo(nineSliceMenu, "nine_slice_border_left",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).nineSliceBorderLeft,
                        (e, value) -> ((TooltipElement)e.element).nineSliceBorderLeft = value,
                        Component.translatable("fancymenu.customization.items.tooltip.nine_slice.border.left"),
                        true, 5, null, null);

        // Max width
        this.addIntegerInputContextMenuEntryTo(appearanceMenu, "max_width",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).maxWidth,
                        (e, width) -> ((TooltipElement)e.element).maxWidth = width,
                        Component.translatable("fancymenu.customization.items.tooltip.max_width"),
                        true, 200, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.customization.items.tooltip.max_width.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_appearance");

        // Behavior submenu
        ContextMenu behaviorMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("behavior", Component.translatable("fancymenu.customization.items.tooltip.behavior"), behaviorMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("settings"));

        // Mouse following toggle
        this.addToggleContextMenuEntryTo(behaviorMenu, "mouse_following",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).mouseFollowing,
                        (e, value) -> ((TooltipElement)e.element).mouseFollowing = value,
                        "fancymenu.customization.items.tooltip.mouse_following")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.customization.items.tooltip.mouse_following.desc")));

        // Interactable toggle - using Text element localization
        this.addToggleContextMenuEntryTo(behaviorMenu, "interactable",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).interactable,
                        (e, value) -> ((TooltipElement)e.element).interactable = value,
                        "fancymenu.elements.text.v2.interactable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(
                        Component.translatable("fancymenu.elements.text.v2.interactable.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_behavior");

        // Markdown submenu - using Text element localization
        ContextMenu markdownMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("markdown", Component.translatable("fancymenu.customization.items.text.markdown"), markdownMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        // Text shadow - using Text element localization
        this.addToggleContextMenuEntryTo(markdownMenu, "text_shadow",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).markdownRenderer.isTextShadow(),
                        (e, value) -> ((TooltipElement)e.element).markdownRenderer.setTextShadow(value),
                        "fancymenu.customization.items.text.shadow");

        // Text case mode - using Text element localization
        this.addCycleContextMenuEntryTo(markdownMenu, "text_case",
                        List.of(MarkdownRenderer.TextCase.NORMAL, MarkdownRenderer.TextCase.ALL_LOWER, MarkdownRenderer.TextCase.ALL_UPPER),
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).markdownRenderer.getTextCase(),
                        (e, caseMode) -> ((TooltipElement)e.element).markdownRenderer.setTextCase(caseMode),
                        (menu, entry, value) -> {
                            if (value == MarkdownRenderer.TextCase.NORMAL) {
                                return Component.translatable("fancymenu.customization.items.text.case_mode.normal");
                            }
                            if (value == MarkdownRenderer.TextCase.ALL_LOWER) {
                                return Component.translatable("fancymenu.customization.items.text.case_mode.lower");
                            }
                            return Component.translatable("fancymenu.customization.items.text.case_mode.upper");
                        });

        // Text scale - using Text element localization
        this.addFloatInputContextMenuEntryTo(markdownMenu, "text_scale",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).markdownRenderer.getTextBaseScale(),
                        (e, scale) -> ((TooltipElement)e.element).markdownRenderer.setTextBaseScale(scale),
                        Component.translatable("fancymenu.customization.items.text.scale"),
                        true, 1.0F, null, null);

        // Text color - using Text element localization
        this.addStringInputContextMenuEntryTo(markdownMenu, "text_color",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).markdownRenderer.getTextBaseColor().getHex(),
                        (e, color) -> ((TooltipElement)e.element).markdownRenderer.setTextBaseColor(DrawableColor.of(color)),
                        null, false, false,
                        Component.translatable("fancymenu.customization.items.text.base_color"),
                        true, "#FFFFFF", null, null);

        // Text border - using Text element localization
        this.addIntegerInputContextMenuEntryTo(markdownMenu, "text_border",
                        TooltipEditorElement.class,
                        e -> (int)((TooltipElement)e.element).markdownRenderer.getBorder(),
                        (e, border) -> ((TooltipElement)e.element).markdownRenderer.setBorder(border),
                        Component.translatable("fancymenu.customization.items.text.text_border"),
                        true, 0, null, null);

        // Line spacing - using Text element localization
        this.addIntegerInputContextMenuEntryTo(markdownMenu, "line_spacing",
                        TooltipEditorElement.class,
                        e -> (int)((TooltipElement)e.element).markdownRenderer.getLineSpacing(),
                        (e, spacing) -> ((TooltipElement)e.element).markdownRenderer.setLineSpacing(spacing),
                        Component.translatable("fancymenu.customization.items.text.line_spacing"),
                        true, 1, null, null);

        // Auto line wrapping - using Text element localization
        this.addToggleContextMenuEntryTo(markdownMenu, "auto_line_wrapping",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).markdownRenderer.isAutoLineBreakingEnabled(),
                        (e, value) -> ((TooltipElement)e.element).markdownRenderer.setAutoLineBreakingEnabled(value),
                        "fancymenu.customization.items.text.auto_line_wrapping");

        // Parse markdown toggle - using Text element localization
        this.addToggleContextMenuEntryTo(markdownMenu, "parse_markdown",
                        TooltipEditorElement.class,
                        e -> ((TooltipElement)e.element).markdownRenderer.isParseMarkdown(),
                        (e, value) -> ((TooltipElement)e.element).markdownRenderer.setParseMarkdown(value),
                        "fancymenu.customization.items.text.markdown.toggle");
    }
}
