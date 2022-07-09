//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.ui.slider;

import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ListSliderButton extends AdvancedSliderButton {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/ListSliderButton");

    public List<String> values;

    public ListSliderButton(int x, int y, int width, int height, boolean handleClick, @NotNull List<String> values, double selectedIndex, Consumer<AdvancedSliderButton> applyValueCallback) {
        super(x, y, width, height, handleClick, 0, applyValueCallback);
        this.values = values;
        this.setSelectedIndex(selectedIndex);
        this.updateMessage();
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
            return (int) Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), minValue, maxValue);
        }
        return 0;
    }

//    public int getSelectedIndex() {
//        if (!this.values.isEmpty()) {
//            int valueCount = this.values.size();
//            return ((int) Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), 1, valueCount))-1;
//        }
//        return 0;
//    }

    public void setSelectedIndex(double index) {
        if ((this.values != null) && !this.values.isEmpty()) {
            double minValue = 0;
            double maxValue = this.values.size()-1;
            this.setValue(((Mth.clamp(index, minValue, maxValue) - minValue) / (maxValue - minValue)));
        }
    }

//    public void setSelectedIndex(double index) {
//        if ((this.values != null) && !this.values.isEmpty()) {
//            double minValue = 0;
//            double maxValue = this.values.size();
//            if (index < 0) {
//                index = 0;
//            }
//            if (index > this.values.size()-1) {
//                index = this.values.size()-1;
//            }
//            this.setValue(((Mth.clamp((float)(index + 1), 1, this.values.size()) - 1) / (this.values.size() - 1)));
//        }
//    }

}
