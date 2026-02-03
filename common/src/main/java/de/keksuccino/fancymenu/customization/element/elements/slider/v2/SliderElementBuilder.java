package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SliderElementBuilder extends ElementBuilder<SliderElement, SliderEditorElement> {

    public SliderElementBuilder() {
        super("slider_v2");
    }

    @Override
    public @NotNull SliderElement buildDefaultInstance() {
        SliderElement element = new SliderElement(this);
        element.shouldBeAffectedByDecorationOverlays.setDefault(true).set(true);
        element.baseWidth = 100;
        element.baseHeight = 20;
        element.label = "New Slider: $$value";
        element.listValues.addAll(List.of("some_value", "another_value", "third_value"));
        element.minRangeValue.set(0.0D);
        element.maxRangeValue.set(20.0D);
        return element;
    }

    @Override
    public SliderElement deserializeElement(@NotNull SerializedElement serialized) {

        SliderElement element = this.buildDefaultInstance();

        String sliderTypeString = serialized.getValue("slider_type");
        if (sliderTypeString != null) {
            SliderElement.SliderType t = SliderElement.SliderType.getByName(sliderTypeString);
            if (t != null) {
                element.type = t;
            }
        }

        element.preSelectedValue = serialized.getValue("pre_selected_value");

        element.label = serialized.getValue("slider_label");

        List<Pair<String, String>> listValueEntries = new ArrayList<>();
        serialized.getProperties().forEach((key, value) -> {
            if (key.startsWith("slider_list_value_")) {
                listValueEntries.add(Pair.of(key, value));
            }
        });
        listValueEntries.sort(Comparator.comparingInt(value -> {
            String key = value.getKey();
            key = new StringBuilder(key).reverse().toString();
            key = new StringBuilder(key.split("_", 2)[0]).reverse().toString();
            if (MathUtils.isInteger(key)) return Integer.parseInt(key);
            return 0;
        }));
        if (!listValueEntries.isEmpty()) element.listValues.clear();
        listValueEntries.forEach(pair -> element.listValues.add(pair.getValue()));
        if (element.listValues.size() < 2) element.listValues.add("placeholder_value");

        String executableBlockId = serialized.getValue("slider_element_executable_block_identifier");
        if (executableBlockId != null) {
            AbstractExecutableBlock b = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, executableBlockId);
            if (b instanceof GenericExecutableBlock g) {
                element.executableBlock = g;
            }
        }

        element.tooltip = serialized.getValue("tooltip");

        element.handleTextureNormal = deserializeImageResourceSupplier(serialized.getValue("handle_texture_normal"));
        element.handleTextureHover = deserializeImageResourceSupplier(serialized.getValue("handle_texture_hovered"));
        element.handleTextureInactive = deserializeImageResourceSupplier(serialized.getValue("handle_texture_inactive"));

        element.sliderBackgroundTextureNormal = deserializeImageResourceSupplier(serialized.getValue("slider_background_texture_normal"));
        element.sliderBackgroundTextureHighlighted = deserializeImageResourceSupplier(serialized.getValue("slider_background_texture_highlighted"));

        element.underlineLabelOnHover = deserializeBoolean(element.underlineLabelOnHover, serialized.getValue("underline_label_on_hover"));

        element.transparentBackground = deserializeBoolean(element.transparentBackground, serialized.getValue("transparent_background"));

        element.restartBackgroundAnimationsOnHover = deserializeBoolean(element.restartBackgroundAnimationsOnHover, serialized.getValue("restart_background_animations"));

        element.nineSliceCustomBackground = deserializeBoolean(element.nineSliceCustomBackground, serialized.getValue("nine_slice_custom_background"));
        element.nineSliceSliderHandle = deserializeBoolean(element.nineSliceSliderHandle, serialized.getValue("nine_slice_slider_handle"));

        element.navigatable = deserializeBoolean(element.navigatable, serialized.getValue("navigatable"));

        String activeStateRequirementContainerIdentifier = serialized.getValue("widget_active_state_requirement_container_identifier");
        if (activeStateRequirementContainerIdentifier != null) {
            RequirementContainer c = RequirementContainer.deserializeWithIdentifier(activeStateRequirementContainerIdentifier, serialized);
            if (c != null) {
                element.activeStateSupplier = c;
            }
        }

        element.hoverSound = deserializeAudioResourceSupplier(serialized.getValue("hoversound"));

        return element;

    }

    @Override
    public @Nullable SliderElement deserializeElementInternal(@NotNull SerializedElement serialized) {
        SliderElement element = super.deserializeElementInternal(serialized);
        if (element != null) element.prepareLoadingRequirementContainer();
        return element;
    }

    @Override
    protected SerializedElement serializeElement(@NotNull SliderElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("slider_type", element.type.getName());

        serializeTo.putProperty("pre_selected_value", element.preSelectedValue);

        serializeTo.putProperty("slider_label", element.label);

        int i = 0;
        for (String s : element.listValues) {
            serializeTo.putProperty("slider_list_value_" + i, s);
            i++;
        }

        serializeTo.putProperty("slider_element_executable_block_identifier", element.executableBlock.getIdentifier());
        element.executableBlock.serializeToExistingPropertyContainer(serializeTo);

        serializeTo.putProperty("tooltip", element.tooltip);

        if (element.handleTextureNormal != null) {
            serializeTo.putProperty("handle_texture_normal", element.handleTextureNormal.getSourceWithPrefix());
        }
        if (element.handleTextureHover != null) {
            serializeTo.putProperty("handle_texture_hovered", element.handleTextureHover.getSourceWithPrefix());
        }
        if (element.handleTextureInactive != null) {
            serializeTo.putProperty("handle_texture_inactive", element.handleTextureInactive.getSourceWithPrefix());
        }

        serializeTo.putProperty("underline_label_on_hover", "" + element.underlineLabelOnHover);
        serializeTo.putProperty("transparent_background", "" + element.transparentBackground);
        serializeTo.putProperty("restart_background_animations", "" + element.restartBackgroundAnimationsOnHover);

        if (element.sliderBackgroundTextureNormal != null) {
            serializeTo.putProperty("slider_background_texture_normal", element.sliderBackgroundTextureNormal.getSourceWithPrefix());
        }
        if (element.sliderBackgroundTextureHighlighted != null) {
            serializeTo.putProperty("slider_background_texture_highlighted", element.sliderBackgroundTextureHighlighted.getSourceWithPrefix());
        }

        serializeTo.putProperty("nine_slice_custom_background", "" + element.nineSliceCustomBackground);
        serializeTo.putProperty("nine_slice_slider_handle", "" + element.nineSliceSliderHandle);

        serializeTo.putProperty("navigatable", "" + element.navigatable);

        serializeTo.putProperty("widget_active_state_requirement_container_identifier", element.activeStateSupplier.identifier);
        element.activeStateSupplier.serializeToExistingPropertyContainer(serializeTo);

        if (element.hoverSound != null) {
            serializeTo.putProperty("hoversound", element.hoverSound.getSourceWithPrefix());
        }

        return serializeTo;

    }

    @Override
    public @NotNull SliderEditorElement wrapIntoEditorElement(@NotNull SliderElement element, @NotNull LayoutEditorScreen editor) {
        return new SliderEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.slider.v2");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.desc");
    }

}
