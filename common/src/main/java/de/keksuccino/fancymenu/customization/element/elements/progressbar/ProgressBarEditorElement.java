package de.keksuccino.fancymenu.customization.element.elements.progressbar;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ProgressBarEditorElement extends AbstractEditorElement<ProgressBarEditorElement, ProgressBarElement> {

    public ProgressBarEditorElement(@NotNull ProgressBarElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.element.barColor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.bar_color.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("color_palette"));

        this.addImageResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_bar_texture",
                        ProgressBarEditorElement.class,
                        null,
                        consumes -> consumes.element.barTextureSupplier,
                        (progressBarEditorElement, iTextureResourceSupplier) -> progressBarEditorElement.element.barTextureSupplier = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.progress_bar.bar_texture"),
                        true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.bar_texture.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "bar_nine_slice",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.barNineSlice,
                        (element, aBoolean) -> element.element.barNineSlice = aBoolean,
                        "fancymenu.elements.progress_bar.bar_texture.nine_slice");

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "bar_nine_slice_border_top",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.barNineSliceBorderTop,
                        (element, value) -> element.element.barNineSliceBorderTop = value,
                        Component.translatable("fancymenu.elements.progress_bar.bar_texture.nine_slice.border.top"),
                        true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "bar_nine_slice_border_right",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.barNineSliceBorderRight,
                        (element, value) -> element.element.barNineSliceBorderRight = value,
                        Component.translatable("fancymenu.elements.progress_bar.bar_texture.nine_slice.border.right"),
                        true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "bar_nine_slice_border_bottom",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.barNineSliceBorderBottom,
                        (element, value) -> element.element.barNineSliceBorderBottom = value,
                        Component.translatable("fancymenu.elements.progress_bar.bar_texture.nine_slice.border.bottom"),
                        true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "bar_nine_slice_border_left",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.barNineSliceBorderLeft,
                        (element, value) -> element.element.barNineSliceBorderLeft = value,
                        Component.translatable("fancymenu.elements.progress_bar.bar_texture.nine_slice.border.left"),
                        true, 5, null, null);

        this.rightClickMenu.addSeparatorEntry("separator_after_bar_entries");

        this.element.backgroundColor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.background_color.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("color_palette"));

        this.addImageResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_background_texture",
                        ProgressBarEditorElement.class,
                        null,
                        consumes -> consumes.element.backgroundTextureSupplier,
                        (progressBarEditorElement, iTextureResourceSupplier) -> progressBarEditorElement.element.backgroundTextureSupplier = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.progress_bar.background_texture"),
                        true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.background_texture.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "background_nine_slice",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.backgroundNineSlice,
                        (element, aBoolean) -> element.element.backgroundNineSlice = aBoolean,
                        "fancymenu.elements.progress_bar.background_texture.nine_slice");

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "background_nine_slice_border_top",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.backgroundNineSliceBorderTop,
                        (element, value) -> element.element.backgroundNineSliceBorderTop = value,
                        Component.translatable("fancymenu.elements.progress_bar.background_texture.nine_slice.border.top"),
                        true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "background_nine_slice_border_right",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.backgroundNineSliceBorderRight,
                        (element, value) -> element.element.backgroundNineSliceBorderRight = value,
                        Component.translatable("fancymenu.elements.progress_bar.background_texture.nine_slice.border.right"),
                        true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "background_nine_slice_border_bottom",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.backgroundNineSliceBorderBottom,
                        (element, value) -> element.element.backgroundNineSliceBorderBottom = value,
                        Component.translatable("fancymenu.elements.progress_bar.background_texture.nine_slice.border.bottom"),
                        true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "background_nine_slice_border_left",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.backgroundNineSliceBorderLeft,
                        (element, value) -> element.element.backgroundNineSliceBorderLeft = value,
                        Component.translatable("fancymenu.elements.progress_bar.background_texture.nine_slice.border.left"),
                        true, 5, null, null);

        this.rightClickMenu.addSeparatorEntry("separator_after_background_entries");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_progress_value_mode",
                        ListUtils.of(ProgressBarElement.ProgressValueMode.PERCENTAGE, ProgressBarElement.ProgressValueMode.FLOATING_POINT),
                        ProgressBarEditorElement.class,
                        element -> element.element.progressValueMode,
                        (element, progressValueMode) -> element.element.progressValueMode = progressValueMode,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_progress_source",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.progressSource,
                        (progressBarEditorElement, s) -> progressBarEditorElement.element.progressSource = s,
                        null, false, true, Component.translatable("fancymenu.elements.progress_bar.source"), false, "50", null, null)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.source.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "smooth_filling_animation", ProgressBarEditorElement.class,
                consumes -> consumes.element.smoothFillingAnimation,
                (element, aBoolean) -> element.element.smoothFillingAnimation = aBoolean, "fancymenu.elements.progress_bar.smoothing");

        this.rightClickMenu.addSeparatorEntry("separator_after_progress_source");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_direction",
                        ListUtils.of(ProgressBarElement.BarDirection.RIGHT, ProgressBarElement.BarDirection.LEFT, ProgressBarElement.BarDirection.UP, ProgressBarElement.BarDirection.DOWN),
                        ProgressBarEditorElement.class,
                        element -> element.element.direction,
                        (element, barDirection) -> element.element.direction = barDirection,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_use_progress_for_element_anchor",
                        ProgressBarEditorElement.class,
                        element -> element.element.useProgressForElementAnchor,
                        (element, aBoolean) -> element.element.useProgressForElementAnchor = aBoolean,
                        "fancymenu.elements.progress_bar.progress_for_element_anchor")
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.progress_for_element_anchor.desc")));

    }

}
