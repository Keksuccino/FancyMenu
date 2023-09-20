package de.keksuccino.fancymenu.customization.element.elements.progressbar;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

public class ProgressBarEditorElement extends AbstractEditorElement {

    public ProgressBarEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_bar_color",
                        ProgressBarEditorElement.class,
                        element -> ((ProgressBarElement)element.element).barColor.getHex(),
                        (element, colorHex) -> ((ProgressBarElement)element.element).barColor = DrawableColor.of(colorHex),
                        null, false, false, Component.translatable("fancymenu.editor.elements.progress_bar.bar_color"),
                        true, DrawableColor.of(new Color(82, 149, 255)).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.progress_bar.bar_color.desc")));

        this.addFileChooserContextMenuEntryTo(this.rightClickMenu, "set_bar_texture",
                        ProgressBarEditorElement.class,
                        null,
                        element -> ((ProgressBarElement)element.element).barTexturePath,
                        (element, s) -> ((ProgressBarElement)element.element).barTexturePath = s,
                        Component.translatable("fancymenu.editor.elements.progress_bar.bar_texture"), true, FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.progress_bar.bar_texture.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_bar_entries");

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_background_color",
                        ProgressBarEditorElement.class,
                        element -> ((ProgressBarElement)element.element).backgroundColor.getHex(),
                        (element, colorHex) -> ((ProgressBarElement)element.element).backgroundColor = DrawableColor.of(colorHex),
                        null, false, false, Component.translatable("fancymenu.editor.elements.progress_bar.background_color"),
                        true, DrawableColor.of(new Color(171, 200, 247)).getHex(), TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.progress_bar.background_color.desc")));

        this.addFileChooserContextMenuEntryTo(this.rightClickMenu, "set_background_texture",
                        ProgressBarEditorElement.class,
                        null,
                        element -> ((ProgressBarElement)element.element).backgroundTexturePath,
                        (element, s) -> ((ProgressBarElement)element.element).backgroundTexturePath = s,
                        Component.translatable("fancymenu.editor.elements.progress_bar.background_texture"), true, FileFilter.IMAGE_FILE_FILTER)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.progress_bar.background_texture.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_background_entries");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_progress_value_mode",
                        ListUtils.build(ProgressBarElement.ProgressValueMode.PERCENTAGE, ProgressBarElement.ProgressValueMode.FLOATING_POINT),
                        ProgressBarEditorElement.class,
                        element -> ((ProgressBarElement)element.element).progressValueMode,
                        (element, progressValueMode) -> ((ProgressBarElement)element.element).progressValueMode = progressValueMode,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_progress_source",
                        ProgressBarEditorElement.class,
                        consumes -> ((ProgressBarElement)consumes.element).progressSource,
                        (progressBarEditorElement, s) -> ((ProgressBarElement)progressBarEditorElement.element).progressSource = s,
                        null, false, true, Component.translatable("fancymenu.editor.elements.progress_bar.source"), false, "50", null, null)
                .setStackable(false)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.progress_bar.source.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_progress_source");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_direction",
                        ListUtils.build(ProgressBarElement.BarDirection.RIGHT, ProgressBarElement.BarDirection.LEFT, ProgressBarElement.BarDirection.UP, ProgressBarElement.BarDirection.DOWN),
                        ProgressBarEditorElement.class,
                        element -> ((ProgressBarElement)element.element).direction,
                        (element, barDirection) -> ((ProgressBarElement)element.element).direction = barDirection,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setStackable(true);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_use_progress_for_element_anchor",
                        ProgressBarEditorElement.class,
                        element -> ((ProgressBarElement)element.element).useProgressForElementAnchor,
                        (element, aBoolean) -> ((ProgressBarElement)element.element).useProgressForElementAnchor = aBoolean,
                        "fancymenu.editor.elements.progress_bar.progress_for_element_anchor")
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.progress_bar.progress_for_element_anchor.desc")));

    }

}
