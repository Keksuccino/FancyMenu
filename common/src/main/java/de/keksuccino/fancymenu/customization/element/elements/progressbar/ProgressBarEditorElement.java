package de.keksuccino.fancymenu.customization.element.elements.progressbar;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
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
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.bar_color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        this.addImageResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_bar_texture",
                        ProgressBarEditorElement.class,
                        null,
                        consumes -> consumes.element.barTextureSupplier,
                        (progressBarEditorElement, iTextureResourceSupplier) -> progressBarEditorElement.element.barTextureSupplier = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.progress_bar.bar_texture"),
                        true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.bar_texture.desc")))
                .setIcon(MaterialIcons.IMAGE);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "bar_nine_slice",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.barNineSlice,
                        (element, aBoolean) -> element.element.barNineSlice = aBoolean,
                        "fancymenu.elements.progress_bar.bar_texture.nine_slice")
                .setIcon(MaterialIcons.GRID_GUIDES);

        this.element.barNineSliceBorderTop.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_TOP);

        this.element.barNineSliceBorderRight.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_RIGHT);

        this.element.barNineSliceBorderBottom.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_BOTTOM);

        this.element.barNineSliceBorderLeft.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_LEFT);

        this.rightClickMenu.addSeparatorEntry("separator_after_bar_entries");

        this.element.backgroundColor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.background_color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        this.addImageResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_background_texture",
                        ProgressBarEditorElement.class,
                        null,
                        consumes -> consumes.element.backgroundTextureSupplier,
                        (progressBarEditorElement, iTextureResourceSupplier) -> progressBarEditorElement.element.backgroundTextureSupplier = iTextureResourceSupplier,
                        Component.translatable("fancymenu.elements.progress_bar.background_texture"),
                        true, null, true, true, true)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.background_texture.desc")))
                .setIcon(MaterialIcons.IMAGE);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "background_nine_slice",
                        ProgressBarEditorElement.class,
                        consumes -> consumes.element.backgroundNineSlice,
                        (element, aBoolean) -> element.element.backgroundNineSlice = aBoolean,
                        "fancymenu.elements.progress_bar.background_texture.nine_slice")
                .setIcon(MaterialIcons.GRID_GUIDES);

        this.element.backgroundNineSliceBorderTop.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_TOP);

        this.element.backgroundNineSliceBorderRight.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_RIGHT);

        this.element.backgroundNineSliceBorderBottom.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_BOTTOM);

        this.element.backgroundNineSliceBorderLeft.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.BORDER_LEFT);

        this.rightClickMenu.addSeparatorEntry("separator_after_background_entries");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_progress_value_mode",
                        ListUtils.of(ProgressBarElement.ProgressValueMode.PERCENTAGE, ProgressBarElement.ProgressValueMode.FLOATING_POINT),
                        ProgressBarEditorElement.class,
                        element -> element.element.progressValueMode,
                        (element, progressValueMode) -> element.element.progressValueMode = progressValueMode,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setStackable(true)
                .setIcon(MaterialIcons.PERCENT);

        this.element.progressSource.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.source.desc")))
                .setIcon(MaterialIcons.DATA_OBJECT);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "smooth_filling_animation", ProgressBarEditorElement.class,
                consumes -> consumes.element.smoothFillingAnimation,
                (element, aBoolean) -> element.element.smoothFillingAnimation = aBoolean, "fancymenu.elements.progress_bar.smoothing")
                .setIcon(MaterialIcons.ANIMATION);

        this.rightClickMenu.addSeparatorEntry("separator_after_progress_source");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_direction",
                        ListUtils.of(ProgressBarElement.BarDirection.RIGHT, ProgressBarElement.BarDirection.LEFT, ProgressBarElement.BarDirection.UP, ProgressBarElement.BarDirection.DOWN),
                        ProgressBarEditorElement.class,
                        element -> element.element.direction,
                        (element, barDirection) -> element.element.direction = barDirection,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setStackable(true)
                .setIcon(MaterialIcons.ARROW_RIGHT_ALT);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_use_progress_for_element_anchor",
                        ProgressBarEditorElement.class,
                        element -> element.element.useProgressForElementAnchor,
                        (element, aBoolean) -> element.element.useProgressForElementAnchor = aBoolean,
                        "fancymenu.elements.progress_bar.progress_for_element_anchor")
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.progress_bar.progress_for_element_anchor.desc")))
                .setIcon(MaterialIcons.LINK);

    }

}
