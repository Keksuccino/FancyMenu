package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class SliderElement extends AbstractElement {

    public static final String VALUE_PLACEHOLDER = "$$value";

    public AbstractExtendedSlider slider;
    @NotNull
    public SliderType type = SliderType.INTEGER_RANGE;
    @Nullable
    public String preSelectedValue;
    @NotNull
    public List<String> listValues = new ArrayList<>();
    public double minRangeValue = 1;
    public double maxRangeValue = 10;
    public String label;
    @NotNull
    public GenericExecutableBlock executableBlock = new GenericExecutableBlock();

    public SliderElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.buildSlider();
        this.prepareExecutableBlock();
    }

    public void prepareExecutableBlock() {
        this.executableBlock.addValuePlaceholder("slidervalue", () -> (this.slider != null) ? this.slider.getValueDisplayText() : "");
    }

    /**
     * This should only get called on init or in the editor, because the new slider will
     * not get registered as {@link Screen} widget by calling this method.
     */
    public void buildSlider() {

        if (this.type == SliderType.INTEGER_RANGE) {
            int min = (int) this.minRangeValue;
            int max = (int) this.maxRangeValue;
            int preSelected = min;
            String preSelectedString = (this.preSelectedValue != null) ? PlaceholderParser.replacePlaceholders(this.preSelectedValue) : null;
            if ((preSelectedString != null) && MathUtils.isDouble(preSelectedString)) {
                preSelected = (int) Double.parseDouble(preSelectedString);
            }
            this.slider = new RangeSlider(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight(), Component.empty(), min, max, preSelected);
        }
        if (this.type == SliderType.LIST) {

        }

    }

    @NotNull
    protected Component getSliderLabel(@NotNull AbstractExtendedSlider slider) {
        String labelValueReplaced = (this.label != null) ? this.label.replace(VALUE_PLACEHOLDER, slider.getValueDisplayText()) : "";
        return buildComponent(labelValueReplaced);
    }

    protected void onChangeSliderValue(@NotNull String value) {



    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return !isEditor() ? List.of(this.slider) : null;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.slider.updateMessage();

            this.slider.render(pose, mouseX, mouseY, partial);

        }

    }

    public enum SliderType {

        LIST("list"),
        INTEGER_RANGE("integer_range"),
        DECIMAL_RANGE("decimal_range");

        final String name;

        SliderType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SliderType getByName(String name) {
            for (SliderType i : SliderType.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }

}
