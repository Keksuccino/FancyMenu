package de.keksuccino.fancymenu.customization.element.elements.slider;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.utils.ListUtils;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;

public class SliderEditorElement extends AbstractEditorElement {

    public SliderEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    //TODO inputfield + playerentity + splash -> editor element entries fertig machen !!
    //TODO inputfield + playerentity + splash -> editor element entries fertig machen !!
    //TODO inputfield + playerentity + splash -> editor element entries fertig machen !!
    //TODO inputfield + playerentity + splash -> editor element entries fertig machen !!

    @Override
    public void init() {

        super.init();

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_variable", null,
                        consumes -> (consumes instanceof SliderEditorElement),
                        null,
                        consumes -> ((SliderElement)consumes.element).linkedVariable,
                        (element, varName) -> ((SliderElement)element.element).linkedVariable = varName,
                        false, false, Component.translatable("fancymenu.customization.items.slider.editor.set_variable"))
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.set_variable.desc")));

        this.rightClickMenu.addSeparatorEntry("slider_separator_1");

        this.addSwitcherContextMenuEntryTo(this.rightClickMenu, "set_slider_type",
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
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.type.desc")));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_list_values", null, consumes -> (consumes instanceof SliderEditorElement), "example_value_1\nexample_value_2\nexample_value_3", consumes ->
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
                        }, true, true,
                        Component.translatable("fancymenu.customization.items.slider.editor.list.set_list_values"))
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.list.set_list_values.desc")))
                .setIsVisibleSupplier((menu, entry) -> ((SliderElement)this.element).type == SliderElement.SliderType.LIST);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_min_range_value",
                        consumes -> (consumes instanceof SliderEditorElement),
                        0,
                        consumes -> ((SliderElement)consumes.element).minRangeValue,
                        (element, range) -> {
                            ((SliderElement)element.element).minRangeValue = range;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        Component.translatable("fancymenu.customization.items.slider.editor.range.set_min_range_value"))
                .setIsVisibleSupplier((menu, entry) -> ((SliderElement)this.element).type == SliderElement.SliderType.RANGE);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_max_range_value",
                        consumes -> (consumes instanceof SliderEditorElement),
                        0,
                        consumes -> ((SliderElement)consumes.element).maxRangeValue,
                        (element, range) -> {
                            ((SliderElement)element.element).maxRangeValue = range;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        Component.translatable("fancymenu.customization.items.slider.editor.range.set_max_range_value"))
                .setIsVisibleSupplier((menu, entry) -> ((SliderElement)this.element).type == SliderElement.SliderType.RANGE);

        this.rightClickMenu.addSeparatorEntry("slider_separator_2");

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_label_prefix", null,
                        consumes -> (consumes instanceof SliderEditorElement),
                        null,
                        consumes -> ((SliderElement)consumes.element).labelPrefix,
                        (element, label) -> {
                            ((SliderElement)element.element).labelPrefix = label;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        false, false, Component.translatable("fancymenu.customization.items.slider.editor.set_label_prefix"))
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.set_label_prefix.desc")));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_label_suffix", null,
                        consumes -> (consumes instanceof SliderEditorElement),
                        null,
                        consumes -> ((SliderElement)consumes.element).labelSuffix,
                        (element, label) -> {
                            ((SliderElement)element.element).labelSuffix = label;
                            ((SliderElement)element.element).initializeSlider();
                        },
                        false, false, Component.translatable("fancymenu.customization.items.slider.editor.set_label_suffix"))
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.editor.set_label_suffix.desc")));

        this.rightClickMenu.addSeparatorEntry("slider_separator_3").setStackable(true);

    }

}
