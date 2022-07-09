//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.ui.slider;

import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class ListSliderButton extends AdvancedSliderButton {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/ListSliderButton");

    public List<String> values;

    public ListSliderButton(int x, int y, int width, int height, boolean handleClick, @Nonnull List<String> values, double selectedIndex, Consumer<AdvancedSliderButton> applyValueCallback) {
        super(x, y, width, height, handleClick, 0, applyValueCallback);
        this.values = values;
        this.setSelectedIndex(selectedIndex);
        this.func_230979_b_();
    }

    @Override
    public String getSliderMessageWithoutPrefixSuffix() {
        return this.getSelectedListValue();
    }

    public String getSelectedListValue() {
        return this.values.get(this.getSelectedIndex());
    }

    public int getSelectedIndex() {
        if (!this.values.isEmpty()) {
            double minValue = 0;
            double maxValue = this.values.size()-1;
            return (int) MathHelper.lerp(MathHelper.clamp(this.sliderValue, 0.0D, 1.0D), minValue, maxValue);
        }
        return 0;
    }

    public void setSelectedIndex(double index) {
        if ((this.values != null) && !this.values.isEmpty()) {
            double minValue = 0;
            double maxValue = this.values.size()-1;
            this.setValue(((MathHelper.clamp(index, minValue, maxValue) - minValue) / (maxValue - minValue)));
        }
    }

}
