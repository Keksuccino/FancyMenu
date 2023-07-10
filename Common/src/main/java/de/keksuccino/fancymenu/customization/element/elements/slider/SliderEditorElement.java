package de.keksuccino.fancymenu.customization.element.elements.slider;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;

public class SliderEditorElement extends AbstractEditorElement {

    public SliderEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_variable",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).linkedVariable,
                        (element, varName) -> ((SliderElement)element.element).linkedVariable = varName,
                        null, false, false, Component.translatable("fancymenu.customization.items.slider.editor.set_variable"),
                        false, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.set_variable.desc")));

        this.rightClickMenu.addSeparatorEntry("slider_separator_1");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "set_slider_type",
                        ListUtils.build(SliderElement.SliderType.LIST, SliderElement.SliderType.RANGE),
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).type,
                        (element, type) -> {
                            ((SliderElement)element.element).type = type;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        (menu, entry, switcherValue) -> {
                            if (switcherValue == SliderElement.SliderType.LIST) {
                                return Component.translatable("fancymenu.customization.items.slider.editor.type.list");
                            }
                            return Component.translatable("fancymenu.customization.items.slider.editor.type.range");
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.type.desc")));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_list_values",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes ->
                        {
                            List<String> values = ((SliderElement)consumes.element).listValues;
                            String s = "example_value_1\nexample_value_2\nexample_value_3";
                            if (values != null) {
                                s = "";
                                for (String v : values) {
                                    if (s.length() > 0) s += "\n";
                                    s += v;
                                }
                            }
                            return s;
                        }, (element1, s) -> {
                            if (s != null) {
                                ((SliderElement)element1.element).listValues = Arrays.asList(StringUtils.splitLines(s, "\n"));
                                ((SliderElement)element1.element).initializeSlider();
                            }
                        }, null, true, true,
                        Component.translatable("fancymenu.customization.items.slider.editor.list.set_list_values"),
                        false, "", TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.list.set_list_values.desc")))
                .setIsVisibleSupplier((menu, entry) -> ((SliderElement)this.element).type == SliderElement.SliderType.LIST);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_min_range_value",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).minRangeValue,
                        (element, range) -> {
                            ((SliderElement)element.element).minRangeValue = range;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        Component.translatable("fancymenu.customization.items.slider.editor.range.set_min_range_value"),
                        true, 0, null, null)
                .setIsVisibleSupplier((menu, entry) -> ((SliderElement)this.element).type == SliderElement.SliderType.RANGE);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_max_range_value",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).maxRangeValue,
                        (element, range) -> {
                            ((SliderElement)element.element).maxRangeValue = range;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        Component.translatable("fancymenu.customization.items.slider.editor.range.set_max_range_value"),
                        true, 0, null, null)
                .setIsVisibleSupplier((menu, entry) -> ((SliderElement)this.element).type == SliderElement.SliderType.RANGE);

        this.rightClickMenu.addSeparatorEntry("slider_separator_2");

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_label_prefix",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).labelPrefix,
                        (element, label) -> {
                            ((SliderElement)element.element).labelPrefix = label;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        null, false, false, Component.translatable("fancymenu.customization.items.slider.editor.set_label_prefix"),
                        true, null, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.set_label_prefix.desc")));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_label_suffix",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).labelSuffix,
                        (element, label) -> {
                            ((SliderElement)element.element).labelSuffix = label;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        null, false, false, Component.translatable("fancymenu.customization.items.slider.editor.set_label_suffix"),
                        true, null, null, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.set_label_suffix.desc")));

        this.rightClickMenu.addSeparatorEntry("slider_separator_3").setStackable(true);

    }

}
