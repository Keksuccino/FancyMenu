package de.keksuccino.fancymenu.customization.element.elements.slider.v1;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SliderElementBuilder extends ElementBuilder<SliderElement, SliderEditorElement> {

    public SliderElementBuilder() {
        super("fancymenu_customization_item_slider");
    }

    @Override
    public boolean isDeprecated() {
        return true;
    }

    @Override
    public @NotNull SliderElement buildDefaultInstance() {
        SliderElement i = new SliderElement(this);
        i.baseWidth = 100;
        i.baseHeight = 20;
        return i;
    }

    @Override
    public SliderElement deserializeElement(@NotNull SerializedElement serialized) {

        SliderElement element = this.buildDefaultInstance();

        element.linkedVariable = serialized.getValue("linked_variable");

        String sliderTypeString = serialized.getValue("slider_type");
        if (sliderTypeString != null) {
            SliderElement.SliderType t = SliderElement.SliderType.getByName(sliderTypeString);
            if (t != null) {
                element.type = t;
            }
        }

        element.labelPrefix = serialized.getValue("label_prefix");

        element.labelSuffix = serialized.getValue("label_suffix");

        if (element.type == SliderElement.SliderType.RANGE) {
            String minRangeString = serialized.getValue("min_range_value");
            if (minRangeString != null) {
                if (MathUtils.isInteger(minRangeString)) {
                    element.minRangeValue = Integer.parseInt(minRangeString);
                }
            }
            String maxRangeString = serialized.getValue("max_range_value");
            if (maxRangeString != null) {
                if (MathUtils.isInteger(maxRangeString)) {
                    element.maxRangeValue = Integer.parseInt(maxRangeString);
                }
            }
        }
        if (element.type == SliderElement.SliderType.LIST) {
            String listValueString = serialized.getValue("list_values");
            if (listValueString != null) {
                element.listValues = deserializeValuesList(listValueString);
            }
        }
        if (element.listValues.isEmpty()) {
            element.listValues.add("some_value");
            element.listValues.add("another_value");
            element.listValues.add("yet_another_value");
        }
        if (element.listValues.size() == 1) {
            element.listValues.add("dummy_value");
        }

        element.initializeSlider();

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull SliderElement element, @NotNull SerializedElement serializeTo) {

        if (element.linkedVariable != null) {
            serializeTo.putProperty("linked_variable", element.linkedVariable);
        }
        serializeTo.putProperty("slider_type", element.type.getName());
        if (element.labelPrefix != null) {
            serializeTo.putProperty("label_prefix", element.labelPrefix);
        }
        if (element.labelSuffix != null) {
            serializeTo.putProperty("label_suffix", element.labelSuffix);
        }
        if (element.type == SliderElement.SliderType.RANGE) {
            serializeTo.putProperty("min_range_value", "" + element.minRangeValue);
            serializeTo.putProperty("max_range_value", "" + element.maxRangeValue);
        }
        if (element.type == SliderElement.SliderType.LIST) {
            serializeTo.putProperty("list_values", "" + serializeValuesList(element.listValues));
        }

        return serializeTo;
        
    }

    @Override
    public @NotNull SliderEditorElement wrapIntoEditorElement(@NotNull SliderElement element, @NotNull LayoutEditorScreen editor) {
        return new SliderEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.customization.items.slider");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.slider.desc");
    }

    protected static String serializeValuesList(List<String> list) {
        String s = "";
        for (String s2 : list) {
            s += s2 + ";";
        }
        return s;
    }

    protected static List<String> deserializeValuesList(String list) {
        List<String> l = new ArrayList<>();
        if (list.contains(";")) {
            for (String s : list.split(";")) {
                if (!s.replace(" ", "").equals("")) {
                    l.add(s);
                }
            }
        }
        return l;
    }

}
